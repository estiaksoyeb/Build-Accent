package com.buildaccent.`as`.ui.create

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.buildaccent.`as`.AccentBuilderApp
import com.buildaccent.`as`.data.Lesson
import com.buildaccent.`as`.data.LessonRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditLessonViewModel(
    savedStateHandle: SavedStateHandle,
    private val lessonRepository: LessonRepository,
    private val application: Application
) : ViewModel() {

    private val lessonId: Int = checkNotNull(savedStateHandle["lessonId"])
    
    private val _uiState = MutableStateFlow(EditLessonUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadLesson()
    }

    private fun loadLesson() {
        viewModelScope.launch {
            val lesson = lessonRepository.getLesson(lessonId)
            lesson?.let {
                _uiState.update { state ->
                    state.copy(
                        title = it.title,
                        language = it.language,
                        accent = it.accent,
                        textContent = it.textContent,
                        originalLesson = it
                    )
                }
            }
        }
    }

    fun updateTitle(title: String) { _uiState.update { it.copy(title = title) } }
    fun updateLanguage(language: String) { _uiState.update { it.copy(language = language) } }
    fun updateAccent(accent: String) { _uiState.update { it.copy(accent = accent) } }
    fun updateText(text: String) { _uiState.update { it.copy(textContent = text) } }

    fun saveLesson(onSuccess: () -> Unit) {
        val state = uiState.value
        val original = state.originalLesson ?: return
        
        if (state.title.isBlank() || state.textContent.isBlank()) {
            return
        }

        viewModelScope.launch {
            val updatedLesson = original.copy(
                title = state.title,
                language = state.language,
                accent = state.accent,
                textContent = state.textContent
            )
            lessonRepository.updateLesson(updatedLesson)
            onSuccess()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AccentBuilderApp)
                val savedStateHandle = this.createSavedStateHandle()
                EditLessonViewModel(
                    savedStateHandle,
                    application.container.lessonRepository,
                    application
                )
            }
        }
    }
}

data class EditLessonUiState(
    val title: String = "",
    val language: String = "",
    val accent: String = "",
    val textContent: String = "",
    val originalLesson: Lesson? = null
)
