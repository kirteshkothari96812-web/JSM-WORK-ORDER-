package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.JsmDatabase
import com.example.data.preferences.AppPreferences
import com.example.data.remote.GoogleSheetsClient
import com.example.data.repository.WorkOrderRepository
import com.example.ui.screens.JsmAppRoot
import com.example.ui.theme.JsmIndustriesTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.parseColor("#0F172A"))
        )

        val database = JsmDatabase.getInstance(applicationContext)
        val preferences = AppPreferences(applicationContext)
        val sheetsClient = GoogleSheetsClient()
        val repository = WorkOrderRepository(
            orderDao = database.orderDao(),
            preferences = preferences,
            sheetsClient = sheetsClient
        )

        setContent {
            JsmIndustriesTheme {
                val mainViewModel: MainViewModel = viewModel(
                    factory = MainViewModel.Factory(repository)
                )
                JsmAppRoot(viewModel = mainViewModel)
            }
        }
    }
}
