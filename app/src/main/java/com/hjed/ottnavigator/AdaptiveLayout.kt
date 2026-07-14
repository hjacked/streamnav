package com.hjed.ottnavigator

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class StreamNavLayout(
    val isCompact: Boolean,
    val isTelevision: Boolean,
    val leftDrawerWidth: Dp,
    val rightDrawerWidth: Dp,
    val dialogWidth: Dp,
    val channelCardWidth: Dp
)

@Composable
fun rememberStreamNavLayout(): StreamNavLayout {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val uiModeType = configuration.uiMode and Configuration.UI_MODE_TYPE_MASK
    val isTelevision = uiModeType == Configuration.UI_MODE_TYPE_TELEVISION
    val isCompact = screenWidthDp < 600 && !isTelevision
    val availableWidth = (screenWidthDp - 24).coerceAtLeast(280)

    return StreamNavLayout(
        isCompact = isCompact,
        isTelevision = isTelevision,
        leftDrawerWidth = minOf(if (isTelevision) 380 else 340, availableWidth).dp,
        rightDrawerWidth = minOf(if (isTelevision) 360 else 330, availableWidth).dp,
        dialogWidth = minOf(400, availableWidth).dp,
        channelCardWidth = minOf(if (isCompact) 232 else 280, availableWidth).dp
    )
}
