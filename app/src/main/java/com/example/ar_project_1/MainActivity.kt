package com.example.ar_project_1

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ar_project_1.ui.MainApp
import com.example.ar_project_1.ui.theme.AR_PROJECT_1Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Motyw jest zawsze jasny, więc ikony pasków systemowych muszą być ciemne.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        val container = (application as PatientCardApplication).container
        setContent {
            AR_PROJECT_1Theme {
                MainApp(container)
            }
        }
    }
}
