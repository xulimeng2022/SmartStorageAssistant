package com.example.smartstorage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.smartstorage.presentation.MainScreen
import com.example.smartstorage.presentation.theme.SmartStorageTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 主界面 Activity：承载 Compose 页面。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            SmartStorageTheme {
                MainScreen()
            }
        }
    }
}