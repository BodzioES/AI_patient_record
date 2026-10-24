package com.example.ar_project_1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ar_project_1.ui.MainApp
import com.example.ar_project_1.ui.theme.AR_PROJECT_1Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as PatientCardApplication).container
        setContent {
            AR_PROJECT_1Theme {
                MainApp(container)
            }
        }
    }
}
