package com.noto.app.ui.screens.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
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
import com.noto.app.di.NotoViewModelFactory
import com.noto.app.di.ServiceContainer
import com.noto.app.ui.components.EmptyState

@Composable
fun ProjectsScreen(
    container: ServiceContainer,
    onOpen: (Long) -> Unit,
) {
    val vm: ProjectsViewModel = viewModel(factory = NotoViewModelFactory(container))
    val items by vm.state.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) { Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_project)) }
        },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp)) {
                Text(stringResource(R.string.projects_title), style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(4.dp))
                Text("${items.size}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (items.isEmpty()) {
                EmptyState(stringResource(R.string.empty_projects), Modifier.weight(1f))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                ) {
                    items(items, key = { it.id }) { project ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                                .clickable { onOpen(project.id) }
                                .padding(horizontal = 16.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(project.name, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    item { Spacer(Modifier.height(100.dp)) }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false; newName = "" },
            title = { Text(stringResource(R.string.add_project)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.project_name)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.create(newName); showAdd = false; newName = ""
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false; newName = "" }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
