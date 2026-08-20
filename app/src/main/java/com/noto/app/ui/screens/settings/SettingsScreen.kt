package com.noto.app.ui.screens.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noto.app.BuildConfig
import com.noto.app.R
import com.noto.app.data.prefs.SettingsRepository
import com.noto.app.di.NotoViewModelFactory
import com.noto.app.di.ServiceContainer

@Composable
fun SettingsScreen(container: ServiceContainer) {
    val vm: SettingsViewModel = viewModel(factory = NotoViewModelFactory(container))
    val state by vm.state.collectAsStateWithLifecycle()

    val ctx = LocalContext.current

    val calendarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.all { it }
        vm.setCalendarSync(granted)
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result ignored */ }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.displayMedium)

        SectionTitle(stringResource(R.string.settings_api))

        OutlinedTextField(
            value = state.apiKey,
            onValueChange = vm::setApiKey,
            label = { Text(stringResource(R.string.settings_api_key)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.baseUrl,
            onValueChange = vm::setBaseUrl,
            label = { Text(stringResource(R.string.settings_api_url)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.model,
            onValueChange = vm::setModel,
            label = { Text(stringResource(R.string.settings_model)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        SectionTitle(stringResource(R.string.settings_theme))
        ThemeRow(current = state.theme, onSelect = vm::setTheme)

        SectionTitle(stringResource(R.string.settings_notifications))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.settings_notif_permission),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            TextButton(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    openAppSettings(ctx)
                }
            }) { Text(stringResource(R.string.settings_open)) }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.settings_exact_alarms),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            TextButton(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${ctx.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    runCatching { ctx.startActivity(intent) }
                        .onFailure { openAppSettings(ctx) }
                } else {
                    openAppSettings(ctx)
                }
            }) { Text(stringResource(R.string.settings_open)) }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_calendar_sync), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.settings_calendar_sync_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = state.calendarSync,
                onCheckedChange = { on ->
                    if (on) {
                        calendarLauncher.launch(
                            arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
                        )
                    } else {
                        vm.setCalendarSync(false)
                    }
                },
            )
        }

        SectionTitle(stringResource(R.string.settings_rhythm))
        RhythmRow(
            workStart = state.rhythm.workStart,
            workEnd = state.rhythm.workEnd,
            onStart = vm::setWorkStart,
            onEnd = vm::setWorkEnd,
        )

        SectionTitle(stringResource(R.string.settings_about))
        Text("Noto ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RhythmRow(workStart: Int, workEnd: Int, onStart: (Int) -> Unit, onEnd: (Int) -> Unit) {
    val ru = java.util.Locale.getDefault().language == "ru"
    Column {
        Text(
            if (ru) "ИИ подбирает время только в этом окне" else "AI picks times only inside this window",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (ru) "С:" else "From:", modifier = Modifier.width(48.dp))
            HourStepper(hour = workStart, onChange = { onStart(it.coerceAtMost(workEnd - 1)) })
            Spacer(Modifier.width(24.dp))
            Text(if (ru) "До:" else "To:", modifier = Modifier.width(48.dp))
            HourStepper(hour = workEnd, onChange = { onEnd(it.coerceAtLeast(workStart + 1)) })
        }
    }
}

@Composable
private fun HourStepper(hour: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { onChange((hour - 1).coerceAtLeast(0)) }) { Text("−") }
        Text("%02d:00".format(hour), style = MaterialTheme.typography.bodyLarge)
        TextButton(onClick = { onChange((hour + 1).coerceAtMost(24)) }) { Text("+") }
    }
}

private fun openAppSettings(ctx: android.content.Context) {
    val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${ctx.packageName}")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    ctx.startActivity(i)
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun ThemeRow(current: SettingsRepository.Theme, onSelect: (SettingsRepository.Theme) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val options = listOf(
            SettingsRepository.Theme.SYSTEM to R.string.theme_system,
            SettingsRepository.Theme.LIGHT to R.string.theme_light,
            SettingsRepository.Theme.DARK to R.string.theme_dark,
        )
        options.forEach { (t, res) ->
            FilterChip(
                selected = current == t,
                onClick = { onSelect(t) },
                label = { Text(stringResource(res)) },
            )
        }
    }
}
