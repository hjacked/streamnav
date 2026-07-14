package com.hjed.ottnavigator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun StreamNavLogoOverlay(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(500)),
        modifier = Modifier
            .padding(top = 44.dp, start = 24.dp)
    ) {
        Text(
            text = "StreamNav",
            color = Color.White.copy(alpha = 0.35f),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PlaybackControlsOverlay(visible: Boolean) {
    val layout = rememberStreamNavLayout()

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(250)),
        exit = fadeOut(animationSpec = tween(350)),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(if (layout.isCompact) layout.dialogWidth else 440.dp)
                    .background(Color.Black.copy(alpha = 0.68f), RoundedCornerShape(8.dp))
                    .border(1.dp, MotifFocusNeon.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = if (layout.isCompact) 14.dp else 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(if (layout.isCompact) 8.dp else 10.dp)
            ) {
                Text(
                    text = "Playback Controls",
                    color = MotifFocusNeon,
                    fontSize = if (layout.isCompact) 16.sp else 18.sp,
                    fontWeight = FontWeight.Bold
                )

                ControlHelpRow(Icons.Default.KeyboardArrowUp, "Up / Channel+", "Previous channel")
                ControlHelpRow(Icons.Default.KeyboardArrowDown, "Down / Channel-", "Next channel")
                ControlHelpRow(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Left", "Open channel drawer")
                ControlHelpRow(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Right", "Open playback menu")
                ControlHelpRow(Icons.Default.Refresh, "Center / Enter", "Refresh stream")
                ControlHelpRow(Icons.AutoMirrored.Filled.List, "Mouse click", "Select focused item or channel")
                ControlHelpRow(Icons.Default.KeyboardArrowUp, "Swipe up/down", "Change channel")
                ControlHelpRow(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Swipe right/left", "Open channel drawer / playback menu")
            }
        }
    }
}

@Composable
private fun ControlHelpRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    input: String,
    action: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = input,
            tint = MotifFocusNeon.copy(alpha = 0.92f),
            modifier = Modifier.width(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = input,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(120.dp),
            maxLines = 1
        )
        Text(
            text = action,
            color = Color.White.copy(alpha = 0.76f),
            fontSize = 13.sp,
            maxLines = 1
        )
    }
}

@Composable
fun ChannelInfoOverlay(
    visible: Boolean,
    channel: Channel?,
    displayIndex: Int
) {
    val layout = rememberStreamNavLayout()

    AnimatedVisibility(
        visible = visible && channel != null,
        enter = fadeIn(animationSpec = tween(250)),
        exit = fadeOut(animationSpec = tween(400)),
        modifier = Modifier
            .fillMaxSize()
            .padding(end = if (layout.isCompact) 12.dp else 40.dp, start = 12.dp)
    ) {
        if (channel != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = if (layout.isCompact) Alignment.BottomCenter else Alignment.CenterEnd
            ) {
                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .height(IntrinsicSize.Max),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChannelLogo(channel = channel)
                    Spacer(modifier = Modifier.width(12.dp))
                    ChannelTextCard(
                        channelName = channel.name,
                        displayIndex = displayIndex,
                        cardWidth = layout.channelCardWidth,
                        compact = layout.isCompact
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelLogo(channel: Channel) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(1f)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!channel.logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(channel.logoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "External Overlay Logo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "TV Fallback",
                tint = Color.White,
                modifier = Modifier.fillMaxSize(0.6f)
            )
        }
    }
}

@Composable
private fun ChannelTextCard(
    channelName: String,
    displayIndex: Int,
    cardWidth: androidx.compose.ui.unit.Dp,
    compact: Boolean
) {
    Box(
        modifier = Modifier
            .width(cardWidth)
            .background(MotifDrawerGlass.copy(alpha = 0.72f), RoundedCornerShape(8.dp))
            .border(1.dp, MotifFocusNeon.copy(alpha = 0.72f), RoundedCornerShape(8.dp))
            .padding(horizontal = if (compact) 14.dp else 20.dp, vertical = if (compact) 12.dp else 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "",
                    color = MotifFocusNeon,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = channelName,
                    color = MotifFocusNeon,
                    fontSize = if (compact) 15.sp else 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "",
                    color = MotifFocusNeon,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = displayIndex.toString(),
                color = MotifFocusNeon,
                fontSize = if (compact) 42.sp else 60.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End
            )
        }
    }
}
