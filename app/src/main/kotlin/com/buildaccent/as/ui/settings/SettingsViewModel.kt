package com.buildaccent.`as`.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.buildaccent.`as`.AccentBuilderApp
import android.net.Uri
import com.buildaccent.`as`.data.BackupManager
import com.buildaccent.`as`.data.LessonRepository
import com.buildaccent.`as`.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val lessonRepository: LessonRepository,
    private val backupManager: BackupManager
) : ViewModel() {

    private val _backupState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val backupState: StateFlow<BackupUiState> = _backupState

    val uiState: StateFlow<SettingsUiState> = 
        combine(
            userPreferencesRepository.isDeleteEnabled,
            userPreferencesRepository.isEditEnabled,
            userPreferencesRepository.preferredLanguage,
            lessonRepository.getAllLessonsStream()
        ) { deleteEnabled, editEnabled, preferredLang, lessons ->
            val languages = lessons.map { it.language }.distinct().sorted()
            SettingsUiState(
                isDeleteEnabled = deleteEnabled, 
                isEditEnabled = editEnabled,
                preferredLanguage = preferredLang,
                availableLanguages = languages
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState()
        )

    fun toggleDeleteEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.saveDeleteEnabled(enabled)
        }
    }

    fun toggleEditEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.saveEditEnabled(enabled)
        }
    }
    
    fun setPreferredLanguage(language: String) {
        viewModelScope.launch {
            userPreferencesRepository.savePreferredLanguage(language)
        }
    }
    
    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _backupState.value = BackupUiState.Loading("Exporting...")
            try {
                backupManager.exportBackup(uri)
                _backupState.value = BackupUiState.Success("Backup Exported Successfully")
            } catch (e: Exception) {
                e.printStackTrace()
                _backupState.value = BackupUiState.Error("Export Failed: ${e.localizedMessage}")
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _backupState.value = BackupUiState.Loading("Importing...")
            try {
                backupManager.importBackup(uri)
                _backupState.value = BackupUiState.Success("Backup Imported Successfully")
            } catch (e: Exception) {
                e.printStackTrace()
                _backupState.value = BackupUiState.Error("Import Failed: ${e.localizedMessage}")
            }
        }
    }
    
    fun clearBackupState() {
        _backupState.value = BackupUiState.Idle
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AccentBuilderApp)
                SettingsViewModel(
                    application.container.userPreferencesRepository,
                    application.container.lessonRepository,
                    application.container.backupManager
                )
            }
        }
    }
}

sealed class BackupUiState {
    object Idle : BackupUiState()
    data class Loading(val message: String) : BackupUiState()
    data class Success(val message: String) : BackupUiState()
    data class Error(val message: String) : BackupUiState()
}

data class SettingsUiState(
    val isDeleteEnabled: Boolean = false,
    val isEditEnabled: Boolean = false,
    val preferredLanguage: String? = null,
    val availableLanguages: List<String> = emptyList()
)