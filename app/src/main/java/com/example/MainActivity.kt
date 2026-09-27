package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.navigation.Air2CashNavGraph
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.Air2CashViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: Air2CashViewModel by viewModels {
        val app = application as Air2CashApplication
        Air2CashViewModel.provideFactory(app.container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Air2CashNavGraph(viewModel = viewModel)
                }
            }
        }
    }
}
