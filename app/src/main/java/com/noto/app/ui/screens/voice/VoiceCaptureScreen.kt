package com.noto.app.ui.screens.voice

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noto.app.R
import com.noto.app.di.NotoViewModelFactory
import com.noto.app.di.ServiceContainer
import com.noto.app.ui.components.MicButton
import java.util.Locale

@Composable
fun VoiceCaptureScreen(
    container: ServiceContainer,
    onBack: () -> Unit,
    onRecognized: (String) -> Unit,
) {
    val vm: VoiceCaptureViewModel = viewModel(factory = NotoViewModelFactory(container))
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionGranted = granted
        if (granted) vm.start(Locale.getDefault().toLanguageTag())
    }

    LaunchedEffect(Unit) {
        if (permissionGranted) vm.start(Locale.getDefault().toLanguageTag())
        else permLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    LaunchedEffect(state.finalText) {
        state.finalText?.takeIf { it.isNotBlank() }?.let { onRecognized(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.voice_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null) }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Spacer(Modifier.weight(1f))

            val statusText = when {
                !permissionGranted -> stringResource(R.string.permission_mic_denied)
                state.status == VoiceUiState.Status.LISTENING -> stringResource(R.string.listening)
                state.status == VoiceUiState.Status.ERROR -> state.error ?: "error"
                state.status == VoiceUiState.Status.DONE -> stringResource(R.string.processing)
                else -> stringResource(R.string.start_listening)
            }

            Text(
                statusText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            if (state.partial.isNotBlank()) {
                Text(
                    state.partial,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }

            Spacer(Modifier.weight(1f))

            MicButton(
                listening = state.status == VoiceUiState.Status.LISTENING,
                onClick = {
                    when {
                        !permissionGranted -> permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        // While listening, tap finalizes: recognizer returns Final result via callback.
                        state.status == VoiceUiState.Status.LISTENING -> vm.finish()
                        else -> vm.start(Locale.getDefault().toLanguageTag())
                    }
                },
                modifier = Modifier.padding(bottom = 32.dp),
            )
            Text(
                text = if (Locale.getDefault().language == "ru")
                    "Нажми, чтобы завершить" else "Tap to finish",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
