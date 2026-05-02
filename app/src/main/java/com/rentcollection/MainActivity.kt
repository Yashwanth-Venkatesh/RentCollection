package com.rentcollection

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rentcollection.data.preferences.ThemePreferences
import com.rentcollection.ui.navigation.RentNavGraph
import com.rentcollection.ui.theme.LocalDarkTheme
import com.rentcollection.ui.theme.LocalThemeToggle
import com.rentcollection.ui.theme.RentCollectionTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var themePreferences: ThemePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val systemDark = isSystemInDarkTheme()
            val savedDark by themePreferences.isDarkMode.collectAsStateWithLifecycle(initialValue = null)
            val isDark = savedDark ?: systemDark

            val scope = rememberCoroutineScope()

            CompositionLocalProvider(
                LocalDarkTheme provides isDark,
                LocalThemeToggle provides { scope.launch { themePreferences.setDarkMode(!isDark) } }
            ) {
                RentCollectionTheme(darkTheme = isDark) {
                    RentNavGraph()
                }
            }
        }
    }
}
