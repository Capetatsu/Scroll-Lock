package com.scrolllock.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.scrolllock.app.ui.navigation.AppNavigation
import com.scrolllock.app.ui.theme.ScrollLockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScrollLockTheme {
                AppNavigation()
            }
        }
    }
}
