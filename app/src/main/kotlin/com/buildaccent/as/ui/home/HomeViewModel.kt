package com.buildaccent.`as`.ui.home

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.buildaccent.`as`.AccentBuilderApp
import com.buildaccent.`as`.data.Lesson
import com.buildaccent.`as`.data.LessonRepository
import com.buildaccent.`as`.data.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class HomeViewModel(
    private val lessonRepository: LessonRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val application: Application
) : ViewModel() {

    val homeUiState: StateFlow<HomeUiState> = 
        combine(
            lessonRepository.getAllLessonsStream(),
            userPreferencesRepository.isDeleteEnabled,
            userPreferencesRepository.isEditEnabled,
            userPreferencesRepository.preferredLanguage
        ) { lessons, isDeleteEnabled, isEditEnabled, preferredLanguage ->
            HomeUiState(lessons, isDeleteEnabled, isEditEnabled, preferredLanguage)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )

    init {
        initializeData()
    }

    private fun initializeData() {
        viewModelScope.launch {
            importBuiltInLessons()
        }
    }
    
    fun deleteLesson(lesson: Lesson) {
        viewModelScope.launch {
            if (!lesson.isBuiltIn) {
                try {
                    val file = File(lesson.referenceAudioPath)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            lessonRepository.deleteLesson(lesson)
        }
    }

    private suspend fun importBuiltInLessons() {
        withContext(Dispatchers.IO) {
            try {
                val lessonsInDb = lessonRepository.getAllLessonsStream().first().map { it.title }
                
                for (i in 1..20) {
                    val suffix = String.format("%02d", i)
                    val audioResId = application.resources.getIdentifier("lesson_$suffix", "raw", application.packageName)
                    val textResId = application.resources.getIdentifier("lesson_${suffix}_text", "raw", application.packageName)

                    if (audioResId != 0 && textResId != 0) {
                        val isAlreadyImported = lessonsInDb.any { 
                            it.contains("Lesson $i") 
                        }
                        
                        if (!isAlreadyImported) {
                            importLesson(suffix, audioResId, textResId, i)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun importLesson(suffix: String, audioResId: Int, textResId: Int, index: Int) {
        try {
            val audioFile = File(application.filesDir, "lesson_$suffix.opus")
            if (!audioFile.exists()) {
                application.resources.openRawResource(audioResId).use { input ->
                    FileOutputStream(audioFile).use { output -> input.copyTo(output) }
                }
            }

            val textContent = application.resources.openRawResource(textResId).bufferedReader().use { it.readText() }

            val lesson = Lesson(
                title = "Lesson $index",
                language = "English",
                accent = "Neutral",
                textContent = textContent,
                referenceAudioPath = audioFile.absolutePath, 
                isBuiltIn = true
            )
            lessonRepository.insertLesson(lesson)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AccentBuilderApp)
                HomeViewModel(
                    application.container.lessonRepository,
                    application.container.userPreferencesRepository,
                    application
                )
            }
        }
    }
}

data class HomeUiState(
    val lessonList: List<Lesson> = listOf(),
    val isDeleteEnabled: Boolean = false,
    val isEditEnabled: Boolean = false,
    val preferredLanguage: String? = null
)
