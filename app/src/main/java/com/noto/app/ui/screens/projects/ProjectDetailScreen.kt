package com.noto.app.ui.screens.projects

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noto.app.di.NotoViewModelFactory
import com.noto.app.di.ServiceContainer
import com.noto.app.ui.components.SwipeableTaskRow

@Composable
fun ProjectDetailScreen(
    container: ServiceContainer,
    projectId: Long,
    onBack: () -> Unit,
    onOpenTask: (Long) -> Unit,
) {
    val vm: ProjectDetailViewModel = viewModel(
        factory = NotoViewModelFactory(container, mapOf("projectId" to projectId))
    )
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.project?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.tasks, key = { it.id }) { task ->
                SwipeableTaskRow(
                    task = task,
                    projectName = null,
                    progress = state.progressById[task.id],
                    onToggle = { vm.toggle(task) },
                    onClick = { onOpenTask(task.id) },
                    onDelete = { vm.delete(task) },
                )
            }
        }
    }
}
