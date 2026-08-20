package com.noto.app.ui.screens.inbox

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noto.app.R
import com.noto.app.di.NotoViewModelFactory
import com.noto.app.di.ServiceContainer
import com.noto.app.ui.components.EmptyState
import com.noto.app.ui.components.MicButton
import com.noto.app.ui.components.TaskListItem

@Composable
fun InboxScreen(
    container: ServiceContainer,
    onOpenTask: (Long) -> Unit,
    onMic: () -> Unit,
) {
    val vm: InboxViewModel = viewModel(factory = NotoViewModelFactory(container))
    val state by vm.state.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp)) {
                Text(stringResource(R.string.inbox_title), style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${state.tasks.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.tasks.isEmpty()) {
                EmptyState(stringResource(R.string.empty_inbox), Modifier.weight(1f))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                ) {
                    items(state.tasks, key = { it.id }) { task ->
                        TaskListItem(
                            task = task,
                            projectName = state.projectsById[task.projectId ?: -1]?.name,
                            progress = state.progressById[task.id],
                            items = state.itemsByTask[task.id].orEmpty(),
                            onToggleTask = { vm.toggle(task) },
                            onOpen = { onOpenTask(task.id) },
                            onDelete = { vm.delete(task) },
                            onToggleItem = vm::toggleChecklistItem,
                        )
                    }
                    item { Spacer(Modifier.height(120.dp)) }
                }
            }
        }
        MicButton(
            onClick = onMic,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
        )
    }
}
