package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.DeepCobalt
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.IceWhite
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.RoyalElectricBlue
import com.example.ui.theme.VoidBlack
import com.example.voice.AssistantVoiceState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ParmarGlowingOrb(
    voiceState: AssistantVoiceState,
    rmsLevel: Float,
    isOfflineMode: Boolean,
    orbSize: Dp = 220.dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_transition")

    val rotationSlow by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_slow"
    )

    val rotationFast by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_fast"
    )

    val breathing by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    val animatedRms by animateFloatAsState(
        targetValue = rmsLevel.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 90),
        label = "rms_smooth"
    )

    val coreAccent by animateColorAsState(
        targetValue = when (voiceState) {
            AssistantVoiceState.ACTIVE_LISTENING -> ElectricCyan
            AssistantVoiceState.SPEAKING -> EmeraldActive
            AssistantVoiceState.PROCESSING -> NeonBlue
            AssistantVoiceState.WAKE_WORD_STANDBY -> RoyalElectricBlue
            AssistantVoiceState.STOPPED -> AmberWarning
            AssistantVoiceState.IDLE -> if (isOfflineMode) AmberWarning else RoyalElectricBlue
        },
        animationSpec = tween(300),
        label = "core_accent"
    )

    val desc = stringResource(R.string.orb_content_description)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(orbSize)
            .clip(CircleShape)
            .semantics { contentDescription = desc }
            .testTag("parmar_central_orb")
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val minDim = minOf(w, h)
            val center = Offset(w / 2f, h / 2f)
            val pulseBoost = if (voiceState == AssistantVoiceState.ACTIVE_LISTENING ||
                voiceState == AssistantVoiceState.SPEAKING
            ) {
                1f + animatedRms * 0.20f
            } else {
                breathing
            }

            // 1. Volumetric outer aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreAccent.copy(alpha = 0.36f),
                        RoyalElectricBlue.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = (minDim * 0.50f) * pulseBoost.coerceAtMost(1.15f)
                ),
                radius = minDim * 0.50f,
                center = center
            )

            // 2. Outer rotating cybernetic dashed arc
            val outerArcRadius = minDim * 0.42f
            rotate(degrees = rotationSlow, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color.Transparent,
                            coreAccent.copy(alpha = 0.85f),
                            ElectricCyan,
                            Color.Transparent
                        )
                    ),
                    startAngle = 15f,
                    sweepAngle = 135f,
                    useCenter = false,
                    topLeft = Offset(center.x - outerArcRadius, center.y - outerArcRadius),
                    size = Size(outerArcRadius * 2f, outerArcRadius * 2f),
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = RoyalElectricBlue.copy(alpha = 0.55f),
                    startAngle = 200f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(center.x - outerArcRadius, center.y - outerArcRadius),
                    size = Size(outerArcRadius * 2f, outerArcRadius * 2f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 3. Secondary counter-rotating harmonic ring
            val midRadius = minDim * 0.35f * (0.96f + animatedRms * 0.08f)
            rotate(degrees = rotationFast, pivot = center) {
                drawArc(
                    color = coreAccent.copy(alpha = 0.7f),
                    startAngle = 0f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(center.x - midRadius, center.y - midRadius),
                    size = Size(midRadius * 2f, midRadius * 2f),
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = ElectricCyan.copy(alpha = 0.5f),
                    startAngle = 180f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(center.x - midRadius, center.y - midRadius),
                    size = Size(midRadius * 2f, midRadius * 2f),
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 4. Reactive audio ring bars when listening or speaking
            if (voiceState == AssistantVoiceState.ACTIVE_LISTENING ||
                voiceState == AssistantVoiceState.SPEAKING ||
                voiceState == AssistantVoiceState.WAKE_WORD_STANDBY
            ) {
                val barCount = 24
                val baseR = minDim * 0.29f
                for (i in 0 until barCount) {
                    val angleRad = Math.toRadians((i * (360.0 / barCount)) + rotationSlow * 0.4)
                    val wave = 0.35f + 0.65f * kotlin.math.abs(
                        sin((i * 0.9f + rotationFast * 0.05f).toDouble()).toFloat()
                    )
                    val len = minDim * (0.025f + 0.065f * animatedRms * wave)
                    val sx = center.x + (baseR * cos(angleRad)).toFloat()
                    val sy = center.y + (baseR * sin(angleRad)).toFloat()
                    val ex = center.x + ((baseR + len) * cos(angleRad)).toFloat()
                    val ey = center.y + ((baseR + len) * sin(angleRad)).toFloat()
                    drawLine(
                        color = coreAccent.copy(alpha = 0.85f),
                        start = Offset(sx, sy),
                        end = Offset(ex, ey),
                        strokeWidth = 2.4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // 5. Core obsidian sphere with diamond facet ring
            val coreRadius = minDim * 0.24f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreAccent.copy(alpha = 0.45f),
                        DeepCobalt.copy(alpha = 0.92f),
                        VoidBlack
                    ),
                    center = center,
                    radius = coreRadius
                ),
                radius = coreRadius,
                center = center
            )

            drawCircle(
                color = coreAccent.copy(alpha = 0.85f),
                radius = coreRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Subtle inner diamond silhouette representing wake word DIAMOND
            val diamondR = coreRadius * 0.78f
            val diamondPath = Path().apply {
                moveTo(center.x, center.y - diamondR)
                lineTo(center.x + diamondR * 0.78f, center.y)
                lineTo(center.x, center.y + diamondR)
                lineTo(center.x - diamondR * 0.78f, center.y)
                close()
            }
            drawPath(
                path = diamondPath,
                color = ElectricCyan.copy(alpha = 0.28f),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // Center interactive icon
        val icon = when (voiceState) {
            AssistantVoiceState.ACTIVE_LISTENING -> Icons.Filled.Stop
            AssistantVoiceState.SPEAKING -> Icons.Filled.GraphicEq
            AssistantVoiceState.STOPPED -> Icons.Filled.MicOff
            else -> Icons.Filled.Mic
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(VoidBlack.copy(alpha = 0.55f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = IceWhite,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}
