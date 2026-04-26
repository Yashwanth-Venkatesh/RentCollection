package com.rentcollection

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.rentcollection.ui.navigation.RentNavGraph
import com.rentcollection.ui.theme.LocalDarkTheme
import com.rentcollection.ui.theme.LocalThemeToggle
import com.rentcollection.ui.theme.RentCollectionTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDark by rememberSaveable { mutableStateOf(systemDark) }
            CompositionLocalProvider(
                LocalDarkTheme provides isDark,
                LocalThemeToggle provides { isDark = !isDark }
            ) {
                RentCollectionTheme(darkTheme = isDark) {
                    RentNavGraph()
                }
            }
        }
    }
}
