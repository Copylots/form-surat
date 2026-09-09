package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.LetterDraftRepository
import com.example.ui.screens.MainFormScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LetterViewModel
import com.example.viewmodel.LetterViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Room Database and Repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = LetterDraftRepository(database.letterDraftDao())
        
        // Instantiate ViewModel
        val viewModel = ViewModelProvider(
            this, 
            LetterViewModelFactory(repository)
        )[LetterViewModel::class.java]

        setContent {
            MyApplicationTheme {
                MainFormScreen(viewModel = viewModel)
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun Greeting(name: String, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    androidx.compose.material3.Text(text = "Hello $name!", modifier = modifier)
}

