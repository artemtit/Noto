package com.noto.app.ui.screens.taskdetails

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noto.app.R
import com.noto.app.di.NotoViewModelFactory
import com.noto.app.di.ServiceContainer
import com.noto.app.domain.model.Priority
import com.noto.app.domain.model.Recurrence
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@Composable
fun TaskDetailsScreen(
    container: ServiceContainer,
    taskId: Long,
    onBack: () -> Unit,
) {
    val vm: TaskDetailsViewModel = viewModel(
        factory = NotoViewModelFactory(container, mapOf("taskId" to taskId))
    )
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.finished) { if (state.finished) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.task_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null) }
                },
                actions = {
                    if (state.task?.id != null && state.task?.id != 0L) {
                        IconButton(onClick = vm::delete) {
                            Icon(Icons.Rounded.DeleteOutline, null)
                        }
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = vm::save,
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                enabled = state.task?.title?.isNotBlank() == true,
            ) { Text(stringResource(R.string.save)) }
        }
    ) { inner ->
        val task = state.task
        if (state.loading || task == null) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                value = task.title,
                onValueChange = vm::onTitle,
                label = { Text(stringResource(R.string.create_task)) },
                singleLine = false,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = task.description.orEmpty(),
                onValueChange = vm::onDescription,
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 6,
            )

            DateTimeRow(
                startDate = task.startDate,
                date = task.dueDate,
                time = task.dueTime,
                onStartDate = vm::onStartDate,
                onDate = vm::onDate,
                onTime = vm::onTime,
            )

            DurationRow(current = task.estimatedMinutes, onSelect = vm::onDuration)

            val ru = java.util.Locale.getDefault().language == "ru"
            val today = LocalDate.now()
            val overdue = task.dueDate?.let { it.isBefore(today) || (it == today && task.dueTime?.isBefore(LocalTime.now()) == true) } == true && !task.completed
            if (overdue && task.id != 0L) {
                TextButton(onClick = vm::openReschedule) {
                    Text(if (ru) "Перепланировать" else "Reschedule")
                }
            }

            PriorityRow(current = task.priority, onSelect = vm::onPriority)

            ProjectRow(
                projects = state.projects,
                selectedId = task.projectId,
                onSelect = vm::onProject,
            )

            RecurrenceRow(current = task.recurrence, onSelect = vm::onRecurrence)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Reminder", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = task.reminderEnabled, onCheckedChange = vm::onReminder)
            }
        }

        state.conflict?.let { conflict ->
            val ru = java.util.Locale.getDefault().language == "ru"
            AlertDialog(
                onDismissRequest = vm::dismissConflict,
                title = { Text(if (ru) "Пересечение" else "Conflict") },
                text = {
                    Column {
                        Text(
                            if (ru) "Совпадает с «${conflict.other.title}» в ${conflict.other.dueTime?.toString()?.take(5)}."
                            else "Overlaps «${conflict.other.title}» at ${conflict.other.dueTime?.toString()?.take(5)}.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (conflict.alternatives.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (ru) "Сдвинуть на:" else "Shift to:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(6.dp))
                            conflict.alternatives.forEach { alt ->
                                TextButton(onClick = { vm.applyAlternative(alt) }) {
                                    Text(alt.toString().take(5))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = vm::saveDespiteConflict) {
                        Text(if (ru) "Всё равно сохранить" else "Save anyway")
                    }
                },
                dismissButton = {
                    TextButton(onClick = vm::dismissConflict) { Text(stringResource(R.string.cancel)) }
                }
            )
        }

        if (state.showReschedule) {
            val ru = java.util.Locale.getDefault().language == "ru"
            AlertDialog(
                onDismissRequest = vm::dismissReschedule,
                title = { Text(if (ru) "Куда перенести?" else "Move to?") },
                text = {
                    Column {
                        if (state.rescheduleOptions.isEmpty()) {
                            Text(if (ru) "Свободных окон не нашлось." else "No free slots today.")
                        } else {
                            state.rescheduleOptions.forEach { opt ->
                                TextButton(onClick = { vm.applyReschedule(opt) }) {
                                    Text(opt.toString().take(5))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = vm::dismissReschedule) { Text(stringResource(R.string.cancel)) }
                }
            )
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun DurationRow(current: Int?, onSelect: (Int?) -> Unit) {
    val ru = java.util.Locale.getDefault().language == "ru"
    val options = listOf(15, 30, 45, 60, 90, 120)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            if (ru) "Длительность" else "Duration",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = current == null,
                onClick = { onSelect(null) },
                label = { Text(if (ru) "Не указано" else "Not set") },
            )
            options.forEach { m ->
                FilterChip(
                    selected = current == m,
                    onClick = { onSelect(m) },
                    label = {
                        val label = when {
                            m < 60 -> if (ru) "$m мин" else "$m min"
                            m % 60 == 0 -> if (ru) "${m / 60} ч" else "${m / 60}h"
                            else -> if (ru) "${m / 60} ч ${m % 60} мин" else "${m / 60}h ${m % 60}m"
                        }
                        Text(label)
                    },
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun DateTimeRow(
    startDate: LocalDate?,
    date: LocalDate?,
    time: LocalTime?,
    onStartDate: (LocalDate?) -> Unit,
    onDate: (LocalDate?) -> Unit,
    onTime: (LocalTime?) -> Unit,
) {
    var showStart by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    val ru = java.util.Locale.getDefault().language == "ru"

    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AssistChip(onClick = { showStart = true }, label = {
            Text(startDate?.toString() ?: (if (ru) "С даты" else "Start"))
        })
        AssistChip(onClick = { showDate = true }, label = {
            Text(date?.toString() ?: (if (ru) "До даты" else "Due"))
        })
        AssistChip(onClick = { showTime = true }, label = { Text(time?.toString()?.substring(0, 5) ?: "Time") })
        if (startDate != null || date != null || time != null) {
            TextButton(onClick = { onStartDate(null); onDate(null); onTime(null) }) { Text("Clear") }
        }
    }

    if (showStart) {
        val initial = startDate ?: LocalDate.now()
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = initial.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showStart = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = dateState.selectedDateMillis
                    if (millis != null) {
                        val chosen = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        onStartDate(chosen)
                    }
                    showStart = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { onStartDate(null); showStart = false }) { Text("Clear") }
            },
        ) { DatePicker(state = dateState) }
    }

    if (showDate) {
        val initial = date ?: LocalDate.now()
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
                        onDate(chosen)
                    }
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDate = false }) { Text("Cancel") }
            },
        ) { DatePicker(state = dateState) }
    }

    if (showTime) {
        val initial = time ?: LocalTime.of(9, 0)
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
                    onTime(LocalTime.of(timeState.hour, timeState.minute))
                    showTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun PriorityRow(current: Priority, onSelect: (Priority) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Priority.entries.forEach { p ->
            FilterChip(
                selected = current == p,
                onClick = { onSelect(p) },
                label = { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }) },
            )
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun RecurrenceRow(current: Recurrence, onSelect: (Recurrence) -> Unit) {
    val ru = java.util.Locale.getDefault().language == "ru"
    val labels = mapOf(
        Recurrence.NONE to if (ru) "Без повтора" else "No repeat",
        Recurrence.DAILY to if (ru) "Каждый день" else "Daily",
        Recurrence.WEEKLY to if (ru) "Каждую неделю" else "Weekly",
        Recurrence.MONTHLY to if (ru) "Каждый месяц" else "Monthly",
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            if (ru) "Повтор" else "Repeat",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Recurrence.entries.forEach { r ->
                FilterChip(
                    selected = current == r,
                    onClick = { onSelect(r) },
                    label = { Text(labels[r] ?: r.name) },
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ProjectRow(
    projects: List<com.noto.app.domain.model.Project>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Project", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selectedId == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(R.string.no_project)) },
            )
            projects.forEach { p ->
                FilterChip(
                    selected = selectedId == p.id,
                    onClick = { onSelect(p.id) },
                    label = { Text(p.name) },
                )
            }
        }
    }
}
