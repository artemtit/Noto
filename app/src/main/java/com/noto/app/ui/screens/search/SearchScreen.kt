package com.noto.app.ui.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noto.app.R
import com.noto.app.di.NotoViewModelFactory
import com.noto.app.di.ServiceContainer
import com.noto.app.ui.components.EmptyState
import com.noto.app.ui.components.TaskListItem

@Composable
fun SearchScreen(
    container: ServiceContainer,
    onOpenTask: (Long) -> Unit,
    onBack: () -> Unit,
) {
    val vm: SearchViewModel = viewModel(factory = NotoViewModelFactory(container))
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null) }
                },
            )
        }
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::setQuery,
                placeholder = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            )
            when {
                state.query.isBlank() -> Spacer(Modifier.weight(1f))
                state.results.isEmpty() -> EmptyState(text = stringResource(R.string.search_empty), modifier = Modifier.weight(1f))
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(state.results, key = { it.id }) { t ->
                        TaskListItem(
                            task = t,
                            projectName = t.projectId?.let { state.projectsById[it]?.name },
                            progress = state.progressById[t.id],
                            items = state.itemsByTask[t.id].orEmpty(),
                            onToggleTask = { vm.toggle(t) },
                            onOpen = { onOpenTask(t.id) },
                            onDelete = { vm.delete(t) },
                            onToggleItem = vm::toggleChecklistItem,
                        )
                    }
                }
            }
        }
    }
}
