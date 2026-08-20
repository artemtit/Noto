package com.noto.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.noto.app.domain.model.ChecklistItem
import com.noto.app.domain.model.ChecklistProgress
import com.noto.app.domain.model.Task

/**
 * Composes a [SwipeableTaskRow] with an inline, expandable checklist. The chevron on the row
 * toggles visibility; individual checkboxes here update their items directly, no navigation needed.
 */
@Composable
fun TaskListItem(
    task: Task,
    projectName: String?,
    progress: ChecklistProgress?,
    items: List<ChecklistItem>,
    onToggleTask: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onToggleItem: (ChecklistItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember(task.id) { mutableStateOf(false) }
    val cs = MaterialTheme.colorScheme

    Column(modifier = modifier) {
        SwipeableTaskRow(
            task = task,
            projectName = projectName,
            progress = progress,
            expanded = expanded,
            onExpandToggle = if ((progress?.total ?: 0) > 0) ({ expanded = !expanded }) else null,
            onToggle = onToggleTask,
            onClick = onOpen,
            onDelete = onDelete,
        )
        AnimatedVisibility(visible = expanded && items.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(cs.surfaceVariant.copy(alpha = 0.4f))
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, end = 12.dp),
                    ) {
                        Checkbox(checked = item.done, onCheckedChange = { onToggleItem(item) })
                        Text(
                            text = item.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (item.done) cs.onSurfaceVariant else cs.onSurface,
                            textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None,
                        )
                    }
                }
            }
        }
    }
}
