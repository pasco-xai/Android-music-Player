package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.BrandGreen
import kotlin.math.cos
import kotlin.math.sin

enum class VisualizerStyle {
    BARS, WAVE_OSCILLATOR, COSMIC_RING
}

@Composable
fun MusicVisualizer(
    isPlaying: Boolean,
    style: VisualizerStyle,
    modifier: Modifier = Modifier,
    accentColor: Color = BrandGreen
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Visualizer")
    
    // Multiple animated phases to create compound wave patterns
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "phase1"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "phase2"
    )

    // Animated multiplier to smoothly scale audio waves on play/pause
    val animationMultiplier by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.05f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessVeryLow
        ), label = "amplitudeScaling"
    )

    Canvas(modifier = modifier.fillMaxWidth().height(160.dp)) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        when (style) {
            VisualizerStyle.BARS -> {
                // Renders columns of traditional audio graphic equalizer bars
                val barCount = 32
                val barSpacing = 4.dp.toPx()
                val totalSpacing = barSpacing * (barCount - 1)
                val barWidth = (width - totalSpacing) / barCount

                for (i in 0 until barCount) {
                    val multiplier = sin((i.toFloat() / barCount.toFloat()) * Math.PI.toFloat()).toFloat()
                    // Create simulated complex audio frequencies using sine of phase transitions
                    val fraction1 = sin(phase1 + (i * 0.45f))
                    val fraction2 = cos(phase2 + (i * 0.3f))
                    val combinedFraction = (fraction1 + fraction2) / 2f
                    
                    val rawBarHeight = (height * 0.85f) * multiplier * Math.abs(combinedFraction)
                    val activeHeight = rawBarHeight * animationMultiplier + 8.dp.toPx() // Keep a minimal resting state

                    val startX = i * (barWidth + barSpacing)
                    val startY = height - activeHeight

                    // Color gradient for active visualizer columns
                    val gradient = Brush.verticalGradient(
                        colors = listOf(
                            accentColor,
                            NeonCyan
                        ),
                        startY = startY,
                        endY = height
                    )

                    drawRect(
                        brush = gradient,
                        topLeft = Offset(startX, startY),
                        size = Size(barWidth, activeHeight)
                    )
                }
            }

            VisualizerStyle.WAVE_OSCILLATOR -> {
                // Renders compound fluid oscilloscope neon waveforms
                val pointsCount = 120
                val segmentWidth = width / pointsCount

                for (p in 0 until pointsCount - 1) {
                    val x1 = p * segmentWidth
                    val x2 = (p + 1) * segmentWidth

                    // First compound wave
                    val angle1_1 = (p.toFloat() / pointsCount.toFloat()) * 4f * Math.PI.toFloat() + phase1
                    val y1_1 = centerY + (sin(angle1_1) * 35.dp.toPx() * animationMultiplier)
                    val angle1_2 = ((p + 1).toFloat() / pointsCount.toFloat()) * 4f * Math.PI.toFloat() + phase1
                    val y1_2 = centerY + (sin(angle1_2) * 35.dp.toPx() * animationMultiplier)

                    // Second offsetting secondary harmonic wave
                    val angle2_1 = (p.toFloat() / pointsCount.toFloat()) * 8f * Math.PI.toFloat() + phase2
                    val y2_1 = centerY + (cos(angle2_1) * 20.dp.toPx() * animationMultiplier)
                    val angle2_2 = ((p + 1).toFloat() / pointsCount.toFloat()) * 8f * Math.PI.toFloat() + phase2
                    val y2_2 = centerY + (cos(angle2_2) * 20.dp.toPx() * animationMultiplier)

                    // Draw primary thick glowing wave
                    drawLine(
                        color = accentColor.copy(alpha = 0.9f),
                        start = Offset(x1, y1_1),
                        end = Offset(x2, y1_2),
                        strokeWidth = 4f
                    )

                    // Draw secondary neon cyan harmonic wave
                    drawLine(
                        color = NeonCyan.copy(alpha = 0.6f),
                        start = Offset(x1, y2_1),
                        end = Offset(x2, y2_2),
                        strokeWidth = 2.5f
                    )
                }
            }

            VisualizerStyle.COSMIC_RING -> {
                // Pulsing dynamic circular visualizer inside the player card
                val centerX = width / 2f
                val defaultRadius = 45.dp.toPx()
                val pulse = sin(phase1 * 1.5f) * 6.dp.toPx() * animationMultiplier
                val activeRadius = defaultRadius + pulse

                // Draw pulsing ambient radial neon glow
                drawCircle(
                    color = NeonMagenta.copy(alpha = 0.12f * animationMultiplier),
                    radius = activeRadius + 24.dp.toPx(),
                    center = Offset(centerX, centerY)
                )

                drawCircle(
                    color = accentColor.copy(alpha = 0.22f * animationMultiplier),
                    radius = activeRadius + 10.dp.toPx(),
                    center = Offset(centerX, centerY)
                )

                // Draw central visual ring outline
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(accentColor, NeonCyan)
                    ),
                    radius = activeRadius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 6f)
                )

                // Draw orbiting space particles around the circle
                val particlesCount = 12
                for (i in 0 until particlesCount) {
                    val angleOffset = (i.toFloat() / particlesCount.toLong()) * 2f * Math.PI.toFloat()
                    val orbitalAngle = phase2 + angleOffset
                    
                    val orbitDistance = activeRadius + 18.dp.toPx() + (sin(phase1 + i) * 4.dp.toPx())
                    val px = centerX + orbitDistance * cos(orbitalAngle)
                    val py = centerY + orbitDistance * sin(orbitalAngle)

                    drawCircle(
                        color = if (i % 2 == 0) NeonCyan else NeonMagenta,
                        radius = 4f + (sin(phase1 * 2f + i) * 1.5f),
                        center = Offset(px, py)
                    )
                }
            }
        }
    }
}
