package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

/**
 * High-craft animated processing visual (scanning loop / GIF effect)
 * Displays an animated digital card with a sweeping neon laser beam,
 * rotating scanner radar arcs, and glowing pulse rings.
 */
@Composable
fun ProcessingCardAnimation(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "card_processing_animation")

    // Laser beam vertical sweep
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_laser"
    )

    // Pulsing glowing ring
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Continuous rotating radar sweep
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_rotation"
    )

    val emeraldPrimary = Color(0xFF10B981)
    val tealAccent = Color(0xFF06B6D4)
    val lightBeam = Color(0xFFE0F2FE)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // 1. Radar glow & spinning scanner ring
        Canvas(modifier = Modifier.size(108.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2f) * 0.88f

            // Pulsing background aura
            drawCircle(
                color = emeraldPrimary.copy(alpha = 0.12f * pulseAlpha),
                radius = baseRadius * pulseScale,
                center = center
            )

            // Outer dashed/gradient spinning arc
            rotate(rotationAngle, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            tealAccent.copy(alpha = 0.15f),
                            emeraldPrimary.copy(alpha = 0.85f),
                            Color.Transparent
                        )
                    ),
                    startAngle = 0f,
                    sweepAngle = 210f,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // 2. High-tech Card silhouette
        Box(
            modifier = Modifier
                .size(width = 72.dp, height = 48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0F172A))
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(emeraldPrimary.copy(alpha = 0.8f), tealAccent.copy(alpha = 0.4f))
                    ),
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Simulated lines of business card
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 9.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top row with chip/logo mark
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 24.dp, height = 5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(emeraldPrimary.copy(alpha = 0.8f))
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 9.dp, height = 5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF64748B))
                    )
                }

                // Middle lines
                Box(
                    modifier = Modifier
                        .size(width = 38.dp, height = 3.5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF94A3B8).copy(alpha = 0.6f))
                )

                // Bottom line
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 28.dp, height = 3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF64748B).copy(alpha = 0.5f))
                    )
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(emeraldPrimary.copy(alpha = 0.9f))
                    )
                }
            }

            // 3. Animated Sweeping Laser Beam
            Canvas(modifier = Modifier.fillMaxSize()) {
                val beamY = size.height * scanProgress

                // Beam vertical gradient glow
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            emeraldPrimary.copy(alpha = 0.5f),
                            lightBeam.copy(alpha = 0.85f),
                            emeraldPrimary.copy(alpha = 0.5f),
                            Color.Transparent
                        ),
                        startY = beamY - 10.dp.toPx(),
                        endY = beamY + 10.dp.toPx()
                    ),
                    topLeft = Offset(0f, beamY - 10.dp.toPx()),
                    size = Size(size.width, 20.dp.toPx())
                )

                // Central laser beam line
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            emeraldPrimary.copy(alpha = 0.3f),
                            lightBeam,
                            Color.White,
                            lightBeam,
                            emeraldPrimary.copy(alpha = 0.3f)
                        )
                    ),
                    start = Offset(0f, beamY),
                    end = Offset(size.width, beamY),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
