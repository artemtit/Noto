package com.noto.app.ai

import android.util.Log
import com.noto.app.core.AppConfig
import com.noto.app.core.AppError
import com.noto.app.core.AppResult
import com.noto.app.data.prefs.SettingsRepository
import com.noto.app.domain.model.ExistingTaskRef
import com.noto.app.domain.model.ParsedTask
import com.noto.app.domain.model.Priority
import com.noto.app.domain.model.TaskAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

class OpenAiTaskParser(
    private val settings: SettingsRepository,
    private val clientBuilder: () -> OkHttpClient = ::defaultClient,
) : TaskParser {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun parse(
        transcript: String,
        now: ZonedDateTime,
        locale: Locale,
        knownProjects: List<String>,
        busyToday: List<BusySlot>,
        rhythm: SettingsRepository.RhythmProfile,
        existingTasks: List<ExistingTaskRef>,
    ): AppResult<ParseResult> = withContext(Dispatchers.IO) {
        val cfg = settings.currentAi()
        if (cfg.apiKey.isBlank()) return@withContext AppResult.Err(AppError.NoApiKey)

        val system = buildSystemPrompt(now, locale, knownProjects, busyToday, rhythm, existingTasks)
        val body = buildJsonObject {
            put("model", cfg.model)
            put("temperature", 0.0)
            put("response_format", buildJsonObject { put("type", "json_object") })
            put("messages", buildJsonArray {
                add(buildJsonObject { put("role", "system"); put("content", system) })
                add(buildJsonObject { put("role", "user"); put("content", transcript) })
            })
        }.toString()

        val url = cfg.baseUrl.trimEnd('/') + "/chat/completions"
        val req = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${cfg.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        Log.w(TAG, "AI call → url=$url model=${cfg.model} keyLen=${cfg.apiKey.length} keyTail=${cfg.apiKey.takeLast(4)}")
        try {
            clientBuilder().newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val bodyStr = resp.body?.string()
                    Log.w(TAG, "AI HTTP ${resp.code}: ${bodyStr?.take(500)}")
                    return@withContext AppResult.Err(AppError.Api(resp.code, bodyStr))
                }
                val raw = resp.body?.string().orEmpty()
                Log.w(TAG, "AI HTTP 200 body head: ${raw.take(300)}")
                val content = extractContent(raw) ?: run {
                    Log.w(TAG, "AI extract failed")
                    return@withContext AppResult.Err(AppError.BadResponse)
                }
                val parsed = runCatching { parseResponseJson(content, existingTasks.map { it.id }.toSet()) }
                    .getOrElse { e ->
                        Log.w(TAG, "AI parse failed: ${e.message} content=${content.take(300)}")
                        return@withContext AppResult.Err(AppError.BadResponse)
                    }
                AppResult.Ok(parsed)
            }
        } catch (e: UnknownHostException) {
            Log.w(TAG, "AI network: ${e.message}")
            AppResult.Err(AppError.NoNetwork)
        } catch (e: SocketTimeoutException) {
            Log.w(TAG, "AI timeout: ${e.message}")
            AppResult.Err(AppError.Timeout)
        } catch (e: IOException) {
            Log.w(TAG, "AI IO: ${e.message}", e)
            AppResult.Err(AppError.Unknown(e))
        }
    }

    /** Legacy JSON parser: creates ParseResult with tasks-only (no actions). Used by tests. */
    fun parseTasksJson(content: String): List<ParsedTask> =
        parseResponseJson(content, knownIds = emptySet()).tasks

    fun parseResponseJson(content: String, knownIds: Set<Long>): ParseResult {
        val root = json.parseToJsonElement(content).jsonObject
        val tasks = root["tasks"]?.jsonArray?.mapNotNull { el -> parseOne(el.jsonObject) }.orEmpty()
        val actions = root["actions"]?.jsonArray?.mapNotNull { el -> parseAction(el.jsonObject, knownIds) }.orEmpty()
        return ParseResult(tasks, actions)
    }

    private fun extractContent(raw: String): String? = runCatching {
        val root = json.parseToJsonElement(raw).jsonObject
        val choices = root["choices"]?.jsonArray ?: return null
        val first = choices.firstOrNull()?.jsonObject ?: return null
        val message = first["message"]?.jsonObject ?: return null
        message["content"]?.jsonPrimitive?.contentOrNull
    }.getOrNull()

    private fun parseOne(o: JsonObject): ParsedTask? {
        val title = o.str("title")?.trim().takeUnless { it.isNullOrBlank() } ?: return null
        val desc = o.str("description")?.takeUnless { it.isBlank() }
        val start = o.str("startDate")?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val date = o.str("dueDate")?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val time = o.str("dueTime")?.let { parseHm(it) }
        val estMin = (o["estimatedMinutes"] as? JsonPrimitive)?.intOrNull?.takeIf { it in 5..24 * 60 }
        val slots = o["suggestedSlots"]?.let { el ->
            runCatching {
                el.jsonArray.mapNotNull { s -> (s as? JsonPrimitive)?.contentOrNull?.let { parseHm(it) } }
            }.getOrNull()
        }.orEmpty()
        val checklist = o["checklist"]?.let { el ->
            runCatching {
                el.jsonArray.mapNotNull { s ->
                    (s as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
                }
            }.getOrNull()
        }.orEmpty()
        val priority = Priority.fromString(o.str("priority"))
        val project = o.str("project")?.takeUnless { it.isBlank() }
        val reminder = when (val r = o["reminder"]) {
            is JsonPrimitive -> r.contentOrNull?.lowercase()?.let { it != "false" && it != "0" } ?: true
            else -> true
        }
        return ParsedTask(
            title = title,
            description = desc,
            startDate = start,
            dueDate = date,
            dueTime = time,
            estimatedMinutes = estMin,
            suggestedSlots = slots,
            checklist = checklist,
            priority = priority,
            projectName = project,
            reminder = reminder,
        )
    }

    private fun parseAction(o: JsonObject, knownIds: Set<Long>): TaskAction? {
        val kind = when (o.str("action")?.lowercase()) {
            "complete", "done", "finish" -> TaskAction.Kind.COMPLETE
            "delete", "remove" -> TaskAction.Kind.DELETE
            "reschedule", "move", "postpone" -> TaskAction.Kind.RESCHEDULE
            else -> return null
        }
        val id = (o["taskId"] as? JsonPrimitive)?.longOrNull ?: return null
        // Guard against hallucinated ids — only accept those we passed in.
        if (knownIds.isNotEmpty() && id !in knownIds) return null
        val newDate = o.str("newDate")?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val newTime = o.str("newTime")?.let { parseHm(it) }
        return TaskAction(kind = kind, taskId = id, newDate = newDate, newTime = newTime)
    }

    private fun parseHm(s: String): LocalTime? =
        runCatching { LocalTime.parse(s, DateTimeFormatter.ofPattern("HH:mm")) }.getOrNull()
            ?: runCatching { LocalTime.parse(s) }.getOrNull()

    private fun JsonObject.str(k: String): String? = (this[k] as? JsonPrimitive)?.contentOrNull

    private fun buildSystemPrompt(
        now: ZonedDateTime,
        locale: Locale,
        knownProjects: List<String>,
        busy: List<BusySlot>,
        rhythm: SettingsRepository.RhythmProfile,
        existingTasks: List<ExistingTaskRef>,
    ): String {
        val today = now.toLocalDate()
        val time = now.toLocalTime().withNano(0)
        val zone = now.zone.id
        val lang = locale.language.ifBlank { "en" }
        val projects = if (knownProjects.isEmpty()) "(none)" else knownProjects.joinToString(", ")
        val busyStr = if (busy.isEmpty()) "(free)" else busy.joinToString(", ") {
            val end = it.start.plusMinutes(it.durationMinutes.toLong())
            "${it.start.toString().padStart(5, '0').take(5)}-${end.toString().padStart(5, '0').take(5)}"
        }
        val workRange = "${"%02d".format(rhythm.workStart)}:00-${"%02d".format(rhythm.workEnd)}:00"
        val tasksStr = if (existingTasks.isEmpty()) "(none)" else existingTasks.joinToString("\n") { t ->
            val w = t.whenLabel?.let { " ($it)" }.orEmpty()
            "  ${t.id} - ${t.title}$w"
        }
        return """
You extract structured actions from a short user voice note.
Return STRICT JSON only. No prose. No markdown fences. Shape:
{"tasks":[{"title":"...","description":null,"startDate":"YYYY-MM-DD" or null,"dueDate":"YYYY-MM-DD" or null,"dueTime":"HH:mm" or null,"estimatedMinutes":<int> or null,"suggestedSlots":["HH:mm",...] or [],"checklist":["step 1","step 2",...] or [],"priority":"low"|"medium"|"high","project":"..." or null,"reminder":true|false}],
 "actions":[{"action":"complete"|"delete"|"reschedule","taskId":<long>,"newDate":"YYYY-MM-DD" or null,"newTime":"HH:mm" or null}]}

Context:
- Today is $today. Current time is $time. Timezone: $zone. Language: $lang.
- User's active window today: $workRange.
- Slots already busy today (existing tasks/events): $busyStr.
- Currently open tasks (id, title, when):
$tasksStr

Decide first: does the user want to CREATE a new task, or MODIFY / COMPLETE / DELETE an existing one?
- If the utterance is a command on an existing task ("отметь стрижку выполненной", "убери молоко из списка", "перенеси стирку на завтра", "перекинь встречу на 19:00", "mark haircut done", "delete milk", "reschedule laundry to tomorrow") — emit an entry in `actions` targeting the matching task's id from the list above. Leave `tasks` empty for that utterance.
- If nothing matches confidently, do NOT invent an id. Leave `actions` empty and create a new task instead.
- For RESCHEDULE: fill `newDate` and/or `newTime` from the utterance.
- For COMPLETE: only `action` + `taskId`.
- For DELETE: only `action` + `taskId`. Prefer complete over delete when ambiguous.

Rules for `tasks` (new tasks):
- Resolve relative expressions (today, tomorrow, next Monday, in an hour, evening, morning, end of week, next week, "после школы", "через 3 дня", etc.) to concrete dueDate / dueTime using today's date and time.
- RANGE tasks: when the user says "к 1 сентября", "до пятницы", "by Sept 1" — startDate = today, dueDate = the deadline.
- If the user says "on Sept 1" / "1 сентября сделать X" — set only dueDate, leave startDate null.
- If time is not given, dueTime = null.
- If no date at all, both startDate and dueDate = null.
- Do NOT invent tasks, dates, or times not implied by the user.
- Default priority is medium unless the user emphasizes urgency (then high) or unimportance (then low).

DURATION:
- estimatedMinutes = your best guess of how long the task takes (haircut 60, quick call 15, dentist 45, workout 60, meeting 30, buy groceries 30, homework 45). Prefer null only when totally unclear.

CHECKLIST:
- If the user names ONE main task and lists sub-steps under it, produce ONE task object with the parent action as `title` and the sub-steps in `checklist`. Do NOT create a separate task per sub-step.
- If the user actually lists several independent tasks, then create multiple task objects and leave `checklist` empty.
- Each checklist item is a short imperative phrase (2-6 words), one action. Strip filler.
- If no explicit sub-steps, `checklist` = [].

EXAMPLES:
- Input: "Постричься чек-лист помыть голову взять деньги позвонить парикмахеру"
  → {"tasks":[{"title":"Постричься","checklist":["Помыть голову","Взять деньги","Позвонить парикмахеру"],...}],"actions":[]}
- Input: "Отметь стрижку как выполненную"
  → {"tasks":[],"actions":[{"action":"complete","taskId":<id-of-стрижка>}]}
- Input: "Перенеси стирку на завтра в 19"
  → {"tasks":[],"actions":[{"action":"reschedule","taskId":<id-of-стирка>,"newDate":"...","newTime":"19:00"}]}

SLOT SUGGESTIONS:
- If the user asked YOU to pick a time ("подбери время", "выбери время", "во сколько лучше", "когда сделать", "pick a time", "what time works") AND dueTime is null → return 3 concrete suggestedSlots for the given day.
- Slots MUST fit inside the active window ($workRange), MUST NOT overlap the busy list, MUST be at least (current time + 30 min) if the day is today, MUST fit the estimatedMinutes duration.
- Space suggestions out (e.g. morning / midday / evening). Round to :00, :15, :30, :45.
- If the user gave an explicit time or did NOT ask to pick — suggestedSlots = [].
- Never put a slot inside a busy interval.

TITLE POLISHING:
- Rewrite the title as a short, imperative action item ("Постричься", "Позвонить маме", "Купить хлеб"). 2–7 words. Start with a verb. Strip filler ("эээ", "ну", "короче"), self-corrections, reasons — move them into "description" if useful.
- Fix obvious speech-recognition typos and casing.
- Preserve the language of the title (Russian stays Russian, English stays English).

- Prefer matching an existing project when intent obviously fits. Known projects: $projects.
- reminder = true unless the user explicitly says no reminder.

If the note contains no actionable tasks and no commands, return {"tasks":[],"actions":[]}.
""".trimIndent()
    }

    companion object {
        private const val TAG = "NotoAI"
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(AppConfig.AI_REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(AppConfig.AI_REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(AppConfig.AI_REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }
}
