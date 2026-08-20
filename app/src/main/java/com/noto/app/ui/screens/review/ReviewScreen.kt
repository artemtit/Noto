package com.noto.app.ui.screens.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noto.app.R
import com.noto.app.core.DateTimeUtils
import com.noto.app.di.NotoViewModelFactory
import com.noto.app.di.ServiceContainer
import com.noto.app.domain.model.Priority
import com.noto.app.domain.model.Recurrence
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

@Composable
fun ReviewScreen(
    container: ServiceContainer,
    transcript: String,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val vm: ReviewViewModel = viewModel(factory = NotoViewModelFactory(container))
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(transcript) { vm.start(transcript) }
    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.review_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null) }
                }
            )
        },
        bottomBar = {
            val tasksCount = state.items.size
            val actionsCount = state.actions.size
            val total = tasksCount + actionsCount
            val ru = java.util.Locale.getDefault().language == "ru"
            val label = when {
                total == 0 -> stringResource(R.string.create_n_tasks, 0)
                tasksCount == 0 -> if (ru) "Применить $actionsCount" else "Apply $actionsCount"
                actionsCount == 0 -> stringResource(R.string.create_n_tasks, tasksCount)
                else -> if (ru) "Готово ($total)" else "Done ($total)"
            }
            Button(
                onClick = vm::confirm,
                enabled = !state.loading && state.error == null && total > 0,
                modifier = Modifier.fillMaxWidth().padding(20.dp),
            ) { Text(label) }
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.error != null -> ErrorView(state.error!!, transcript)
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Text(
                            "\"$transcript\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    items(state.actions, key = { it.id }) { action ->
                        ActionRow(action = action, onRemove = { vm.removeAction(action.id) })
                    }
                    items(state.items, key = { it.id }) { item ->
                        ReviewRow(
                            item = item,
                            projects = state.projects,
                            onChange = { updated -> vm.updateItem(item.id) { updated } },
                            onPickSlot = { slot -> vm.pickSlot(item.id, slot) },
                            onRegenerateSlots = { vm.regenerateSlots(item.id) },
                            onAddChecklist = { text -> vm.addChecklistItem(item.id, text) },
                            onRemoveChecklist = { index -> vm.removeChecklistItem(item.id, index) },
                            onRemove = { vm.remove(item.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorView(err: com.noto.app.core.AppError, transcript: String) {
    val res = when (err) {
        com.noto.app.core.AppError.NoNetwork -> R.string.error_network
        com.noto.app.core.AppError.Timeout -> R.string.error_timeout
        com.noto.app.core.AppError.BadResponse -> R.string.error_bad_response
        com.noto.app.core.AppError.NoApiKey -> R.string.error_no_api_key
        com.noto.app.core.AppError.EmptySpeech -> R.string.error_empty_speech
        else -> R.string.error_api
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(res), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            transcript,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ReviewRow(
    item: ReviewItem,
    projects: List<com.noto.app.domain.model.Project>,
    onChange: (ReviewItem) -> Unit,
    onPickSlot: (LocalTime) -> Unit,
    onRegenerateSlots: () -> Unit,
    onAddChecklist: (String) -> Unit,
    onRemoveChecklist: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var showPriority by remember { mutableStateOf(false) }
    var showProject by remember { mutableStateOf(false) }
    var showRecurrence by remember { mutableStateOf(false) }
    var showDuration by remember { mutableStateOf(false) }
    var editingTitle by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cs.surface)
            .border(1.dp, cs.outline, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (editingTitle) {
                OutlinedTextField(
                    value = item.title,
                    onValueChange = { onChange(item.copy(title = it)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
            }
            TextButton(onClick = { editingTitle = !editingTitle }) {
                Text(if (editingTitle) stringResource(R.string.done) else "Edit")
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Rounded.Close, contentDescription = null)
            }
        }

        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AssistChip(onClick = { showDate = true }, label = {
                Text(item.dueDate?.let { DateTimeUtils.formatDateShort(it, LocalDate.now(), Locale.getDefault()) } ?: "Date")
            })
            AssistChip(onClick = { showTime = true }, label = {
                Text(item.dueTime?.let { DateTimeUtils.formatTime(it) } ?: "Time")
            })
            AssistChip(onClick = { showPriority = true }, label = { Text(item.priority.name.lowercase()) })
            AssistChip(onClick = { showProject = true }, label = {
                val name = projects.firstOrNull { it.id == item.projectId }?.name ?: stringResource(R.string.no_project)
                Text(name)
            })
            AssistChip(onClick = { showRecurrence = true }, label = { Text(recurrenceLabel(item.recurrence)) })
            AssistChip(onClick = { showDuration = true }, label = {
                Text(item.estimatedMinutes?.let { formatDuration(it) } ?: durationChipLabel())
            })
        }

        if (item.suggestedSlots.isNotEmpty()) {
            val ru = java.util.Locale.getDefault().language == "ru"
            Spacer(Modifier.height(2.dp))
            Text(
                if (ru) "Предложенное время:" else "Suggested time:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item.suggestedSlots.forEach { slot ->
                    AssistChip(
                        onClick = { onPickSlot(slot) },
                        label = { Text(DateTimeUtils.formatTime(slot)) },
                    )
                }
                AssistChip(
                    onClick = onRegenerateSlots,
                    label = { Text(if (ru) "↻ ещё" else "↻ more") },
                )
            }
        }

        ReviewChecklist(
            items = item.checklist,
            onAdd = onAddChecklist,
            onRemove = onRemoveChecklist,
        )
    }

    if (showDate) {
        val initial = item.dueDate ?: LocalDate.now()
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = initial.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = dateState.selectedDateMillis
                    if (millis != null) {
                        val chosen = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        onChange(item.copy(dueDate = chosen))
                    } else {
                        onChange(item.copy(dueDate = null))
                    }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    onChange(item.copy(dueDate = null)); showDate = false
                }) { Text("Clear") }
            },
        ) { DatePicker(state = dateState) }
    }

    if (showTime) {
        val initial = item.dueTime ?: LocalTime.of(9, 0)
        val timeState = rememberTimePickerState(
            initialHour = initial.hour,
            initialMinute = initial.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTime = false },
            title = { Text("Time") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    onChange(item.copy(dueTime = LocalTime.of(timeState.hour, timeState.minute))); showTime = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    onChange(item.copy(dueTime = null)); showTime = false
                }) { Text("Clear") }
            }
        )
    }

    if (showPriority) {
        AlertDialog(
            onDismissRequest = { showPriority = false },
            title = { Text("Priority") },
            text = {
                Column {
                    Priority.entries.forEach { p ->
                        TextButton(onClick = { onChange(item.copy(priority = p)); showPriority = false }) {
                            Text(p.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPriority = false }) { Text(stringResource(R.string.done)) } }
        )
    }

    if (showProject) {
        AlertDialog(
            onDismissRequest = { showProject = false },
            title = { Text("Project") },
            text = {
                Column {
                    TextButton(onClick = { onChange(item.copy(projectId = null)); showProject = false }) {
                        Text(stringResource(R.string.no_project))
                    }
                    projects.forEach { p ->
                        TextButton(onClick = { onChange(item.copy(projectId = p.id)); showProject = false }) {
                            Text(p.name)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showProject = false }) { Text(stringResource(R.string.done)) } }
        )
    }

    if (showDuration) {
        val ru = java.util.Locale.getDefault().language == "ru"
        val options = listOf(15, 30, 45, 60, 90, 120)
        AlertDialog(
            onDismissRequest = { showDuration = false },
            title = { Text(if (ru) "Длительность" else "Duration") },
            text = {
                Column {
                    options.forEach { m ->
                        TextButton(onClick = {
                            onChange(item.copy(estimatedMinutes = m)); showDuration = false
                        }) { Text(formatDuration(m)) }
                    }
                    TextButton(onClick = {
                        onChange(item.copy(estimatedMinutes = null)); showDuration = false
                    }) { Text(if (ru) "Не указано" else "Not set") }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDuration = false }) { Text(stringResource(R.string.done)) }
            }
        )
    }

    if (showRecurrence) {
        AlertDialog(
            onDismissRequest = { showRecurrence = false },
            title = { Text("Repeat") },
            text = {
                Column {
                    Recurrence.entries.forEach { r ->
                        TextButton(onClick = { onChange(item.copy(recurrence = r)); showRecurrence = false }) {
                            Text(recurrenceLabel(r))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showRecurrence = false }) { Text(stringResource(R.string.done)) } }
        )
    }
}

@Composable
private fun ActionRow(action: ReviewAction, onRemove: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cs.primaryContainer.copy(alpha = 0.5f))
            .border(1.dp, cs.primary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            action.description,
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurface,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Rounded.Close, contentDescription = null)
        }
    }
}

@Composable
private fun ReviewChecklist(
    items: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (Int) -> Unit,
) {
    val ru = java.util.Locale.getDefault().language == "ru"
    var draft by remember { mutableStateOf("") }
    var expanded by remember(items.isEmpty()) { mutableStateOf(items.isNotEmpty()) }

    if (!expanded && items.isEmpty()) {
        TextButton(onClick = { expanded = true }) {
            Text(if (ru) "＋ Чек-лист" else "＋ Checklist")
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            if (ru) "Чек-лист" else "Checklist",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        items.forEachIndexed { index, text ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("•  ", style = MaterialTheme.typography.bodyMedium)
                Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = { onRemove(index) }) {
                    Icon(Icons.Rounded.Close, contentDescription = null)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text(if (ru) "Пункт…" else "Item…") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = {
                    if (draft.isNotBlank()) { onAdd(draft); draft = "" }
                },
                enabled = draft.isNotBlank(),
            ) { Text("＋") }
        }
    }
}

private fun durationChipLabel(): String =
    if (java.util.Locale.getDefault().language == "ru") "Длит." else "Duration"

private fun formatDuration(minutes: Int): String {
    val ru = java.util.Locale.getDefault().language == "ru"
    return when {
        minutes < 60 -> if (ru) "$minutes мин" else "$minutes min"
        minutes % 60 == 0 -> if (ru) "${minutes / 60} ч" else "${minutes / 60}h"
        else -> if (ru) "${minutes / 60} ч ${minutes % 60} мин" else "${minutes / 60}h ${minutes % 60}m"
    }
}

private fun recurrenceLabel(r: Recurrence): String {
    val ru = java.util.Locale.getDefault().language == "ru"
    return when (r) {
        Recurrence.NONE -> if (ru) "Без повтора" else "No repeat"
        Recurrence.DAILY -> if (ru) "Каждый день" else "Daily"
        Recurrence.WEEKLY -> if (ru) "Каждую неделю" else "Weekly"
        Recurrence.MONTHLY -> if (ru) "Каждый месяц" else "Monthly"
    }
}
