package com.noto.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.noto.app.core.DateTimeUtils
import com.noto.app.domain.model.Priority
import com.noto.app.domain.model.Task
import com.noto.app.ui.theme.PriorityHigh
import com.noto.app.ui.theme.PriorityLow
import com.noto.app.ui.theme.PriorityMedium
import com.noto.app.ui.theme.projectColor
import java.time.LocalDate
import java.util.Locale

@Composable
fun TaskRow(
    task: Task,
    projectName: String? = null,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val bg by animateColorAsState(cs.surface, label = "row_bg")
    val alpha by animateFloatAsState(if (task.completed) 0.55f else 1f, label = "row_alpha")
    val pColor = priorityColor(task.priority)
    val projColor = task.projectId?.let { projectColor(it) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(1.dp, cs.outline, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .alpha(alpha),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(56.dp)
                .background(pColor)
        )
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CheckCircle(
                checked = task.completed,
                accent = pColor,
                onClick = onToggle,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = cs.onSurface,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val meta = buildMeta(task)
                if (meta.isNotBlank() || projectName != null) {
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (projColor != null && !projectName.isNullOrBlank()) {
                            Box(
                                Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(projColor)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = projectName,
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (meta.isNotBlank()) {
                                Text(
                                    text = " · ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = cs.onSurfaceVariant,
                                )
                            }
                        }
                        if (meta.isNotBlank()) {
                            Text(
                                text = meta,
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckCircle(checked: Boolean, accent: Color, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (checked) accent else Color.Transparent)
            .border(1.5.dp, if (checked) accent else cs.outline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private fun priorityColor(p: Priority): Color = when (p) {
    Priority.HIGH -> PriorityHigh
    Priority.MEDIUM -> PriorityMedium
    Priority.LOW -> PriorityLow
}

private fun buildMeta(task: Task): String {
    val today = LocalDate.now()
    val parts = mutableListOf<String>()
    if (task.isRange) {
        val start = DateTimeUtils.formatDateShort(task.startDate!!, today, Locale.getDefault())
        val end = DateTimeUtils.formatDateShort(task.dueDate!!, today, Locale.getDefault())
        parts += "$start → $end"
    } else {
        task.dueDate?.let { parts += DateTimeUtils.formatDateShort(it, today, Locale.getDefault()) }
    }
    task.dueTime?.let { parts += DateTimeUtils.formatTime(it) }
    return parts.joinToString(" ")
}
