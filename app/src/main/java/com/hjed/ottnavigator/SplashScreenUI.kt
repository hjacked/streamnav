package com.hjed.ottnavigator

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(onTimeout: () -> Unit) {

    val ringProgress = remember { Animatable(0f) }
    val ringRotation = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleScale = remember { Animatable(0.6f) }
    val lineWidth = remember { Animatable(0f) }
    val signatureAlpha = remember { Animatable(0f) }
    val signatureOffsetY = remember { Animatable(12f) }
    val pulseAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            ringRotation.animateTo(
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 3000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        }

        launch {
            ringProgress.animateTo(
                targetValue = 0.75f,
                animationSpec = tween(
                    durationMillis = 800,
                    easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                )
            )
        }

        delay(200)

        launch {
            titleAlpha.animateTo(1f, animationSpec = tween(600))
        }
        launch {
            titleScale.animateTo(
                1f,
                animationSpec = tween(
                    durationMillis = 700,
                    easing = CubicBezierEasing(0.0f, 0.7f, 0.3f, 1f)
                )
            )
        }

        delay(300)

        launch {
            pulseAlpha.animateTo(
                targetValue = 0.4f,
                animationSpec = tween(400)
            )
            pulseAlpha.animateTo(
                targetValue = 0.08f,
                animationSpec = tween(600)
            )
        }

        launch {
            lineWidth.animateTo(
                targetValue = 0.5f,
                animationSpec = tween(
                    durationMillis = 900,
                    easing = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
                )
            )
        }

        delay(200)

        launch {
            signatureOffsetY.animateTo(0f, animationSpec = tween(600))
        }
        launch {
            signatureAlpha.animateTo(1f, animationSpec = tween(600))
        }

        delay(3500)
        onTimeout()
    }

    val accentCyan = MotifFocusNeon
    val accentViolet = MotifAccent

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020508))
    ) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentCyan.copy(alpha = pulseAlpha.value),
                        accentViolet.copy(alpha = pulseAlpha.value * 0.5f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = size.minDimension * 0.45f
                ),
                center = Offset(cx, cy),
                radius = size.minDimension * 0.45f
            )

            val orbitRadius = size.minDimension * 0.18f
            val dotCount = 12
            val progress = ringProgress.value
            val rotation = ringRotation.value

            for (i in 0 until dotCount) {
                val fraction = i.toFloat() / dotCount
                if (fraction > progress) break

                val angle = (fraction * 360f + rotation) * (PI.toFloat() / 180f)
                val dotX = cx + cos(angle) * orbitRadius
                val dotY = cy + sin(angle) * orbitRadius

                val fadeTail = ((progress - fraction) / progress).coerceIn(0.2f, 1f)
                val dotRadius = 2.5f + fadeTail * 2f

                drawCircle(
                    color = accentCyan.copy(alpha = fadeTail * 0.9f),
                    radius = dotRadius,
                    center = Offset(dotX, dotY)
                )

                if (fadeTail > 0.7f) {
                    drawCircle(
                        color = Color.White.copy(alpha = fadeTail * 0.3f),
                        radius = dotRadius * 2.5f,
                        center = Offset(dotX, dotY)
                    )
                }
            }

            val tickCount = 60
            val innerRadius = orbitRadius - 14f
            val outerRadius = orbitRadius - 8f
            for (i in 0 until tickCount) {
                val tickAngle = (i.toFloat() / tickCount * 360f + rotation * 0.3f) * (PI.toFloat() / 180f)
                val isLong = i % 5 == 0
                val r1 = if (isLong) innerRadius - 4f else innerRadius
                val r2 = outerRadius

                val x1 = cx + cos(tickAngle) * r1
                val y1 = cy + sin(tickAngle) * r1
                val x2 = cx + cos(tickAngle) * r2
                val y2 = cy + sin(tickAngle) * r2

                drawLine(
                    color = accentViolet.copy(alpha = if (isLong) 0.35f else 0.15f),
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = if (isLong) 1.5f else 0.8f,
                    cap = StrokeCap.Round
                )
            }
        }

        Text(
            text = "StreamNav",
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 3.sp,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-6).dp)
                .graphicsLayer(
                    alpha = titleAlpha.value,
                    scaleX = titleScale.value,
                    scaleY = titleScale.value
                )
        )

        Text(
            text = "I A M H J E D",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 6.sp,
            color = accentCyan.copy(alpha = titleAlpha.value * 0.6f),
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 26.dp)
                .graphicsLayer(alpha = titleAlpha.value)
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 52.dp)
                .fillMaxWidth(lineWidth.value)
                .height(1.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            accentCyan.copy(alpha = 0.7f),
                            Color.White.copy(alpha = 0.5f),
                            accentViolet.copy(alpha = 0.7f),
                            Color.Transparent
                        )
                    )
                )
        )

        Text(
            text = "Version 1.1.0",
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily.Monospace,
            color = accentCyan.copy(alpha = 0.55f),
            letterSpacing = 2.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .graphicsLayer(
                    alpha = signatureAlpha.value,
                    translationY = signatureOffsetY.value
                )
        )
    }
}
