package com.noto.app.ai

import com.noto.app.data.prefs.SettingsRepository.RhythmProfile
import com.noto.app.domain.model.Task
import java.time.LocalDate
import java.time.LocalTime

object SlotSuggester {

    private val PICK_TIME_RE = Regex(
        "(подбери|выбери|подскажи|предложи).{0,15}врем|" +
            "во\\s?сколько|когда\\s(лучше|можно|мне|сделать)|" +
            "pick.{0,10}time|what\\s?time|when\\s?should",
        RegexOption.IGNORE_CASE,
    )

    fun wantsTimePool(transcript: String): Boolean = PICK_TIME_RE.containsMatchIn(transcript)

    /**
     * Local fallback / reschedule helper. Returns up to [count] slots on [date] that:
     * - fit within [rhythm] work window
     * - don't overlap any busy interval derived from [existing]
     * - start at least 30 min from [now] if [date] == today
     * - fit [durationMinutes]
     * Spaced evenly (~30 min buckets picked morning / midday / evening).
     */
    fun suggest(
        date: LocalDate,
        now: LocalTime,
        today: LocalDate,
        existing: List<Task>,
        rhythm: RhythmProfile,
        durationMinutes: Int = 30,
        count: Int = 3,
        excluding: Set<LocalTime> = emptySet(),
    ): List<LocalTime> {
        val windowStart = LocalTime.of(rhythm.workStart.coerceIn(0, 23), 0)
        val windowEnd = LocalTime.of(rhythm.workEnd.coerceIn(1, 23), 0)
        if (!windowEnd.isAfter(windowStart)) return emptyList()

        val earliest = if (date == today) maxOf(windowStart, roundUpQuarter(now.plusMinutes(30))) else windowStart

        val busy = existing.mapNotNull { t ->
            val s = t.dueTime ?: return@mapNotNull null
            val dur = t.effectiveDurationMinutes
            s to s.plusMinutes(dur.toLong())
        }.sortedBy { it.first }

        val candidates = mutableListOf<LocalTime>()
        var cur = earliest
        while (!cur.plusMinutes(durationMinutes.toLong()).isAfter(windowEnd)) {
            val end = cur.plusMinutes(durationMinutes.toLong())
            val clash = busy.any { (bs, be) -> cur.isBefore(be) && bs.isBefore(end) }
            if (!clash && cur !in excluding) candidates.add(cur)
            cur = cur.plusMinutes(30)
        }
        if (candidates.size <= count) return candidates
        // pick evenly spaced: first, middle, last
        val step = (candidates.size - 1).toDouble() / (count - 1)
        return (0 until count).map { i -> candidates[(i * step).toInt()] }.distinct()
    }

    private fun roundUpQuarter(t: LocalTime): LocalTime {
        val m = t.minute
        val add = ((15 - m % 15) % 15)
        return t.plusMinutes(add.toLong()).withSecond(0).withNano(0)
    }

    /**
     * Overlap check for a candidate task time on [date]. Returns first conflicting task or null.
     */
    fun findConflict(
        candidateStart: LocalTime,
        candidateDuration: Int,
        existing: List<Task>,
    ): Task? {
        val end = candidateStart.plusMinutes(candidateDuration.toLong())
        return existing.firstOrNull { t ->
            val bs = t.dueTime ?: return@firstOrNull false
            val be = bs.plusMinutes(t.effectiveDurationMinutes.toLong())
            candidateStart.isBefore(be) && bs.isBefore(end)
        }
    }
}
