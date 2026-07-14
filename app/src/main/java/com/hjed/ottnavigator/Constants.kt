package com.hjed.ottnavigator

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star


val defaultSettingsCategories = listOf(
    RightMenuOption("Live TV", Icons.Default.PlayArrow),
    RightMenuOption("Movies", Icons.Default.Star),
    RightMenuOption("Series", Icons.Default.AccountBox),
    RightMenuOption("Favorites", Icons.Default.Favorite),
    RightMenuOption("All channels", Icons.AutoMirrored.Filled.List)
)
