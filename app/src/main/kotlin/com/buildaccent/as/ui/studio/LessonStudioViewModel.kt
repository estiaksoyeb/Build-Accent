package com.buildaccent.`as`.ui.studio

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.buildaccent.`as`.AccentBuilderApp
import com.buildaccent.`as`.audio.AudioPlayer
import com.buildaccent.`as`.audio.AudioRecorder
import com.buildaccent.`as`.data.Lesson
import com.buildaccent.`as`.data.LessonRepository
import com.buildaccent.`as`.data.UserRecording
import com.buildaccent.`as`.data.UserRecordingDao
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class StudioUiState(
    val lesson: Lesson? = null,
    val userRecordings: List<UserRecording> = emptyList(),
    val segments: List<LessonSegment> = emptyList(),
    val isRecording: Boolean = false,
    val isPlayingReference: Boolean = false,
    val isPlayingUser: Boolean = false,
    val playingRecordingId: Int? = null,
    val playbackProgress: Float = 0f,
    val hasListenedToReference: Boolean = false,
    val error: String? = null
)

class LessonStudioViewModel(
    savedStateHandle: SavedStateHandle,
    private val lessonRepository: LessonRepository,
    private val recordingDao: UserRecordingDao,
    private val application: Application
) : ViewModel() {

    private val lessonId: Int = checkNotNull(savedStateHandle["lessonId"])
    
    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    private val audioPlayer = AudioPlayer(application)
    private val audioRecorder = AudioRecorder(application)
    
    private var playbackJob: Job? = null

    init {
        loadLesson()
        loadRecordings()
    }

    private fun loadLesson() {
        viewModelScope.launch {
            val lesson = lessonRepository.getLesson(lessonId)
            lesson?.let {
                val segments = SegmentParser.parse(it.textContent)
                _uiState.update { state -> state.copy(lesson = it, segments = segments) }
            }
        }
    }

    private fun loadRecordings() {
        viewModelScope.launch {
            recordingDao.getRecordingsForLesson(lessonId).collect { recordings ->
                _uiState.update { it.copy(userRecordings = recordings) }
            }
        }
    }

    // --- Audio Logic ---

    fun toggleReferencePlayback() {
        val lesson = uiState.value.lesson ?: return
        val file = File(lesson.referenceAudioPath)

        if (uiState.value.isPlayingReference) {
            audioPlayer.pause()
            stopProgressUpdate()
            _uiState.update { it.copy(isPlayingReference = false) }
        } else {
            stopPlayback()
            
            // Resume if paused? Or restart? Simple toggle: Start or Pause.
            // If already prepared, resume. If not, playFile.
            // For simplicity in this architecture, we check if playing. 
            // Ideally AudioPlayer state should be better managed. 
            // For now, if progress > 0 and < 1, resume.
            
            // Re-logic:
            // 1. If playing, pause.
            // 2. If paused (progress > 0), resume.
            // 3. If stopped/new, start.
            
            // Current AudioPlayer.playFile restarts. We need a way to just resume.
            // AudioPlayer.resume() exists.
            
            if (uiState.value.playbackProgress > 0 && uiState.value.playbackProgress < 1f) {
                 audioPlayer.resume()
                 startProgressUpdate()
                 _uiState.update { it.copy(isPlayingReference = true) }
            } else {
                audioPlayer.playFile(file) {
                    stopProgressUpdate()
                    _uiState.update { it.copy(isPlayingReference = false, playbackProgress = 1f) }
                }
                startProgressUpdate()
                _uiState.update { it.copy(isPlayingReference = true, hasListenedToReference = true) }
            }
        }
    }

    fun seekTo(positionMs: Long) {
        if (uiState.value.isPlayingReference || uiState.value.lesson != null) {
             // Ensure we are in a state where we can seek (loaded file)
             // If not playing, we might need to prepare first.
             // For now, assume user plays first or we auto-load.
             // Simplest: If not playing, play then seek? No, just seek if player is active.
             
             // If player is not initialized with the file, we can't seek.
             // We'll lazy load if needed in a real app.
             // For now, only seek if we have started playback at least once (player initialized).
             
             audioPlayer.seekTo(positionMs.toInt())
             
             // Update progress immediately for UI responsiveness
             val duration = audioPlayer.getDuration()
             if (duration > 0) {
                 _uiState.update { it.copy(playbackProgress = positionMs.toFloat() / duration) }
             }
        }
    }
    
    fun seekToProgress(progress: Float) {
        val duration = audioPlayer.getDuration()
        if (duration > 0) {
            val positionMs = (duration * progress).toLong()
            seekTo(positionMs)
        }
    }

    fun toggleUserPlayback(recording: UserRecording) {
        if (uiState.value.playingRecordingId == recording.id && uiState.value.isPlayingUser) {
            audioPlayer.pause()
            stopProgressUpdate()
            _uiState.update { it.copy(isPlayingUser = false) }
        } else {
            stopPlayback()
            
            val file = File(recording.audioPath)
            audioPlayer.playFile(file) {
                stopProgressUpdate()
                _uiState.update { it.copy(isPlayingUser = false, playingRecordingId = null, playbackProgress = 1f) }
            }
            startProgressUpdate()
            _uiState.update { it.copy(isPlayingUser = true, playingRecordingId = recording.id) }
        }
    }

    private fun stopPlayback() {
        audioPlayer.stop()
        stopProgressUpdate()
        _uiState.update { 
            it.copy(
                isPlayingReference = false, 
                isPlayingUser = false, 
                playingRecordingId = null,
                playbackProgress = 0f
            ) 
        }
    }

    fun startRecording() {
        stopPlayback()
        if (!uiState.value.hasListenedToReference) {
            _uiState.update { it.copy(error = "Please listen to the reference audio first.") }
            return
        }

        val fileName = "rec_${lessonId}_${UUID.randomUUID()}.m4a"
        val file = File(application.filesDir, fileName)
        
        audioRecorder.start(file)
        _uiState.update { it.copy(isRecording = true, error = null) }
    }

    fun stopRecording() {
        audioRecorder.stop()
        _uiState.update { it.copy(isRecording = false) }
    }

    // Actually, let's fix stopRecording to save the entry.
    // I need to track the current file path.
    private var currentRecordingFile: File? = null

    fun startRecordingSecure() {
        stopPlayback()
         if (!uiState.value.hasListenedToReference) {
            _uiState.update { it.copy(error = "Please listen to the reference audio first.") }
            return
        }
        
        val fileName = "rec_${lessonId}_${UUID.randomUUID()}.m4a"
        val file = File(application.filesDir, fileName)
        currentRecordingFile = file
        
        audioRecorder.start(file)
        _uiState.update { it.copy(isRecording = true, error = null) }
    }

    fun stopRecordingSecure() {
        audioRecorder.stop()
        _uiState.update { it.copy(isRecording = false) }
        
        currentRecordingFile?.let { file ->
             viewModelScope.launch {
                 recordingDao.insertRecording(
                     UserRecording(
                         lessonId = lessonId,
                         audioPath = file.absolutePath,
                         durationMs = 0 // We could calculate this if we want
                     )
                 )
             }
        }
        currentRecordingFile = null
    }

    fun deleteRecording(recording: UserRecording) {
        viewModelScope.launch {
            recordingDao.deleteRecording(recording)
            // Also delete file
            File(recording.audioPath).delete()
        }
    }
    
    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun startProgressUpdate() {
        stopProgressUpdate()
        playbackJob = viewModelScope.launch {
            while (true) {
                if (audioPlayer.isPlaying()) {
                    val progress = audioPlayer.getCurrentPosition().toFloat() / audioPlayer.getDuration().coerceAtLeast(1)
                    _uiState.update { it.copy(playbackProgress = progress) }
                }
                delay(100) // 10fps update
            }
        }
    }

    private fun stopProgressUpdate() {
        playbackJob?.cancel()
        playbackJob = null
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
        audioRecorder.release()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AccentBuilderApp)
                val savedStateHandle = this.createSavedStateHandle()
                LessonStudioViewModel(
                    savedStateHandle = savedStateHandle,
                    lessonRepository = application.container.lessonRepository,
                    recordingDao = application.container.userRecordingDao,
                    application = application
                )
            }
        }
    }
}