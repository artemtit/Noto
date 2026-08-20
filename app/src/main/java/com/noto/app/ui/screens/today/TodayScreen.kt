package com.noto.app.ui.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noto.app.R
import com.noto.app.di.NotoViewModelFactory
import com.noto.app.di.ServiceContainer
import com.noto.app.ui.components.EmptyState
import com.noto.app.ui.components.MicButton
import com.noto.app.ui.components.SectionHeader
import com.noto.app.ui.components.SwipeableTaskRow
import com.noto.app.ui.components.TaskRow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayScreen(
    container: ServiceContainer,
    onOpenTask: (Long) -> Unit,
    onMic: () -> Unit,
    onSearch: () -> Unit,
) {
    val vm: TodayViewModel = viewModel(factory = NotoViewModelFactory(container))
    val state by vm.state.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Header(date = state.date, totalCount = state.total, onSearch = onSearch)

            QuickAddBar(onAdd = vm::quickAdd)

            if (state.pending.isEmpty() && state.completed.isEmpty()) {
                EmptyState(text = stringResource(R.string.empty_today), modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    if (state.pending.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.section_pending)) }
                        items(state.pending, key = { it.id }) { task ->
                            SwipeableTaskRow(
                                task = task,
                                projectName = state.projectsById[task.projectId ?: -1]?.name,
                                progress = state.progressById[task.id],
                                onToggle = { vm.toggle(task) },
                                onClick = { onOpenTask(task.id) },
                                onDelete = { vm.delete(task) },
                            )
                        }
                    }
                    if (state.completed.isNotEmpty()) {
                        item { Spacer(Modifier.height(10.dp)) }
                        item { SectionHeader(stringResource(R.string.section_completed)) }
                        items(state.completed, key = { it.id }) { task ->
                            SwipeableTaskRow(
                                task = task,
                                projectName = state.projectsById[task.projectId ?: -1]?.name,
                                progress = state.progressById[task.id],
                                onToggle = { vm.toggle(task) },
                                onClick = { onOpenTask(task.id) },
                                onDelete = { vm.delete(task) },
                            )
                        }
                    }
                    item { Spacer(Modifier.height(120.dp)) }
                }
            }
        }

        MicButton(
            onClick = onMic,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
        )
    }
}

@Composable
private fun Header(date: LocalDate, totalCount: Int, onSearch: () -> Unit) {
    val fmt = DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.today_title), style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "${fmt.format(date)} · $totalCount",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onSearch) {
            Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.search_title))
        }
    }
}

@Composable
private fun QuickAddBar(onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cs.surfaceVariant)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(stringResource(R.string.quick_add_hint)) },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (text.isNotBlank()) { onAdd(text.trim()); text = "" }
            }),
        )
        IconButton(
            onClick = { if (text.isNotBlank()) { onAdd(text.trim()); text = "" } },
        ) {
            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.create_task))
        }
    }
}
