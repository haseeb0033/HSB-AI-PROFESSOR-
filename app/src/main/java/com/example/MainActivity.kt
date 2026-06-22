package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.screens.MainScreenContainer
import com.example.ui.screens.QuizScreen
import com.example.ui.viewmodel.ProfessorViewModel
import com.example.ui.viewmodel.ProfessorViewModelFactory

class MainActivity : ComponentActivity() {
    
    private val viewModel: ProfessorViewModel by viewModels {
        ProfessorViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentQuizTopic by remember { mutableStateOf<Pair<String, String>?>(null) }

                    if (currentQuizTopic == null) {
                        MainScreenContainer(
                            viewModel = viewModel,
                            onNavigateToQuiz = { subjectId, topicName ->
                                currentQuizTopic = Pair(subjectId, topicName)
                            }
                        )
                    } else {
                        val (subjectId, topicName) = currentQuizTopic!!
                        QuizScreen(
                            viewModel = viewModel,
                            subjectId = subjectId,
                            topicName = topicName,
                            onBackToLesson = {
                                currentQuizTopic = null
                            }
                        )
                    }
                }
            }
        }
    }
}
