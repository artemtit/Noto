package com.noto.app.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noto.app.di.NotoViewModelFactory
import com.noto.app.di.ServiceContainer
import com.noto.app.domain.model.Priority
import com.noto.app.domain.model.Task
import com.noto.app.ui.components.EmptyState
import com.noto.app.ui.components.SwipeableTaskRow
import com.noto.app.ui.theme.PriorityHigh
import com.noto.app.ui.theme.PriorityLow
import com.noto.app.ui.theme.PriorityMedium
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    container: ServiceContainer,
    onOpenTask: (Long) -> Unit,
) {
    val vm: CalendarViewModel = viewModel(factory = NotoViewModelFactory(container))
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        MonthHeader(
            month = state.month,
            onPrev = vm::prevMonth,
            onNext = vm::nextMonth,
            onToday = vm::goToToday,
        )
        WeekdayLabels()
        MonthGrid(
            month = state.month,
            selected = state.selectedDate,
            tasksByDate = state.tasksByDate,
            onSelect = vm::select,
        )
        Spacer(Modifier.height(4.dp))
        DaySection(
            date = state.selectedDate,
            tasks = state.selectedTasks,
            projectNames = { id -> state.projectsById[id]?.name },
            onToggle = vm::toggle,
            onOpen = onOpenTask,
            onDelete = vm::delete,
        )
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val fmt = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault())
    val label = fmt.format(month).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev) { Icon(Icons.Rounded.ChevronLeft, null) }
        Text(
            text = label,
            style = MaterialTheme.typography.headlineSmall,
            color = cs.onSurface,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = onNext) { Icon(Icons.Rounded.ChevronRight, null) }
        TextButton(onClick = onToday) { Text(todayLabel()) }
    }
}

@Composable
private fun WeekdayLabels() {
    val cs = MaterialTheme.colorScheme
    val locale = Locale.getDefault()
    val firstDay = firstDayOfWeek(locale)
    val days = (0..6).map { firstDay.plus(it.toLong()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        days.forEach { d ->
            val short = d.getDisplayName(TextStyle.SHORT, locale)
                .replaceFirstChar { it.titlecase(locale) }
                .take(2)
            Text(
                text = short,
                style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selected: LocalDate,
    tasksByDate: Map<LocalDate, List<Task>>,
    onSelect: (LocalDate) -> Unit,
) {
    val locale = Locale.getDefault()
    val firstDay = firstDayOfWeek(locale)
    val firstOfMonth = month.atDay(1)
    val leadingBlanks = ((firstOfMonth.dayOfWeek.value - firstDay.value + 7) % 7)
    val start = firstOfMonth.minusDays(leadingBlanks.toLong())
    val totalCells = 42 // 6 weeks
    val today = LocalDate.now()

    Column(Modifier.padding(horizontal = 8.dp)) {
        for (row in 0 until 6) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val day = start.plusDays((row * 7 + col).toLong())
                    val inMonth = day.month == month.month
                    val tasks = tasksByDate[day].orEmpty()
                    DayCell(
                        day = day,
                        inMonth = inMonth,
                        isToday = day == today,
                        isSelected = day == selected,
                        tasks = tasks,
                        onClick = { onSelect(day) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    tasks: List<Task>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val bg = when {
        isSelected -> cs.primary
        isToday -> cs.primaryContainer
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> cs.onPrimary
        !inMonth -> cs.onSurfaceVariant.copy(alpha = 0.35f)
        isToday -> cs.onPrimaryContainer
        else -> cs.onSurface
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = day.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = if (isToday || isSelected) FontWeight.SemiBold else FontWeight.Normal,
            )
            Spacer(Modifier.weight(1f))
            if (tasks.isNotEmpty()) {
                val dots = tasks.take(3).map { priorityColor(it.priority) }
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    dots.forEach { c ->
                        Box(
                            Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) cs.onPrimary else c)
                        )
                    }
                }
                if (tasks.size > 3) {
                    Text(
                        "+${tasks.size - 3}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) cs.onPrimary else cs.onSurfaceVariant,
                        fontSize = 9.sp,
                    )
                } else {
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
    }
}

@Composable
private fun DaySection(
    date: LocalDate,
    tasks: List<Task>,
    projectNames: (Long) -> String?,
    onToggle: (Task) -> Unit,
    onOpen: (Long) -> Unit,
    onDelete: (Task) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val locale = Locale.getDefault()
    val label = DateTimeFormatter.ofPattern("EEEE, d MMMM", locale).format(date)
        .replaceFirstChar { it.titlecase(locale) }
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = cs.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${tasks.size}",
                style = MaterialTheme.typography.labelMedium,
                color = cs.onSurfaceVariant,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(cs.surfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        if (tasks.isEmpty()) {
            EmptyState(text = emptyDayLabel(), modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(tasks, key = { it.id }) { t ->
                    SwipeableTaskRow(
                        task = t,
                        projectName = t.projectId?.let(projectNames),
                        onToggle = { onToggle(t) },
                        onClick = { onOpen(t.id) },
                        onDelete = { onDelete(t) },
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

private fun priorityColor(p: Priority): Color = when (p) {
    Priority.HIGH -> PriorityHigh
    Priority.MEDIUM -> PriorityMedium
    Priority.LOW -> PriorityLow
}

private fun firstDayOfWeek(locale: Locale): DayOfWeek =
    if (locale.language == "ru") DayOfWeek.MONDAY else DayOfWeek.MONDAY

private fun todayLabel(): String = if (Locale.getDefault().language == "ru") "Сегодня" else "Today"
private fun emptyDayLabel(): String = if (Locale.getDefault().language == "ru") "На этот день ничего нет." else "Nothing planned for this day."
