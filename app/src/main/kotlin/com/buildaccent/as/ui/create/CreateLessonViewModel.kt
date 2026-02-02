package com.buildaccent.`as`.ui.create

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class CreateLessonViewModel(
    private val lessonRepository: LessonRepository,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateLessonUiState())
    val uiState = _uiState.asStateFlow()

    fun updateTitle(title: String) { _uiState.update { it.copy(title = title) } }
    fun updateLanguage(language: String) { _uiState.update { it.copy(language = language) } }
    fun updateAccent(accent: String) { _uiState.update { it.copy(accent = accent) } }
    fun updateText(text: String) { _uiState.update { it.copy(textContent = text) } }
    
    fun selectAudio(uri: Uri?) {
        _uiState.update { it.copy(selectedAudioUri = uri) }
    }

    fun saveLesson(onSuccess: () -> Unit) {
        val state = uiState.value
        if (state.title.isBlank() || state.textContent.isBlank() || state.selectedAudioUri == null) {
            // Show error
            return
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    // Copy audio file
                    val inputStream = application.contentResolver.openInputStream(state.selectedAudioUri!!)
                    val fileName = "custom_${UUID.randomUUID()}.audio"
                    val destFile = File(application.filesDir, fileName)
                    
                    inputStream?.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    val lesson = Lesson(
                        title = state.title,
                        language = state.language,
                        accent = state.accent,
                        textContent = state.textContent,
                        referenceAudioPath = destFile.absolutePath,
                        isBuiltIn = false
                    )

                    lessonRepository.insertLesson(lesson)
                    
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AccentBuilderApp)
                CreateLessonViewModel(application.container.lessonRepository, application)
            }
        }
    }
}

data class CreateLessonUiState(
    val title: String = "",
    val language: String = "English",
    val accent: String = "Neutral",
    val textContent: String = "",
    val selectedAudioUri: Uri? = null
)
