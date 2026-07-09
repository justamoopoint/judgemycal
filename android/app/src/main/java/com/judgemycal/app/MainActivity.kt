package com.judgemycal.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.judgemycal.app.ui.JudgeMyCalRoot
import com.judgemycal.app.ui.JudgeMyCalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JudgeMyCalTheme {
                JudgeMyCalRoot()
            }
        }
    }
}
