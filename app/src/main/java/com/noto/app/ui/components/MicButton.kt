package com.noto.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.noto.app.R

@Composable
fun MicButton(
    listening: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val infinite = rememberInfiniteTransition(label = "mic_pulse")
    val pulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = if (listening) 1.08f else 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "pulse",
    )
    val scale by animateFloatAsState(
        targetValue = if (listening) pulse else 1f,
        label = "scale",
    )
    Box(
        modifier = modifier
            .size(76.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(cs.primary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (listening) Icons.Rounded.Stop else Icons.Rounded.Mic,
            contentDescription = stringResource(R.string.mic_button_cd),
            tint = cs.onPrimary,
            modifier = Modifier.size(32.dp),
        )
    }
    // subtle glow (behind) — omitted for perf; MicButton stands on its own.
    @Suppress("UNUSED_VARIABLE") val ignored: Color = cs.primary
}
