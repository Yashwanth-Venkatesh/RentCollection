package com.rentcollection.ui.theme

import androidx.compose.runtime.compositionLocalOf

val LocalDarkTheme = compositionLocalOf { false }
val LocalThemeToggle = compositionLocalOf<() -> Unit> { {} }
