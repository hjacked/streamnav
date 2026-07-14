package com.hjed.ottnavigator

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onTimeout: () -> Unit) {

    val bannerAlpha = 0.54f
    val lineWidth = remember { Animatable(0f) }
    val signatureAlpha = remember { Animatable(0f) }
    val signatureOffsetY = remember { Animatable(8f) }

    LaunchedEffect(Unit) {
        delay(120)

        launch {
            lineWidth.animateTo(
                targetValue = 0.42f,
                animationSpec = tween(
                    durationMillis = 900,
                    easing = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
                )
            )
        }

        delay(180)

        launch {
            signatureOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 700)
            )
        }

        launch {
            signatureAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 700)
            )
        }

        delay(1500)
        onTimeout()
    }

    val splashAccent = MotifFocusNeon

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            splashAccent.copy(alpha = 0.10f),
                            MotifAccent.copy(alpha = 0.08f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.70f)
                        ),
                        radius = 1300f
                    )
                )
        )

        Image(
            painter = painterResource(id = R.drawable.ic_tv_banner),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = bannerAlpha)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.08f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.28f)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 108.dp)
                .fillMaxWidth(lineWidth.value)
                .height(2.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            splashAccent.copy(alpha = 0.95f),
                            Color.White.copy(alpha = 0.85f),
                            MotifAccent.copy(alpha = 0.95f),
                            Color.Transparent
                        )
                    )
                )
        )

        Text(
            text = "CRAFTED BY HJED®",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = splashAccent.copy(alpha = 0.78f),
            letterSpacing = 1.4.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 55.dp)
                .graphicsLayer(
                    alpha = signatureAlpha.value,
                    translationY = signatureOffsetY.value
                )
        )
    }
}
