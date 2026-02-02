package com.buildaccent.`as`.ui.studio

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.buildaccent.`as`.data.UserRecording
import com.buildaccent.`as`.ui.studio.LessonStudioViewModel
import com.buildaccent.`as`.ui.studio.StudioUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonStudioScreen(
    onBackClick: () -> Unit,
    viewModel: LessonStudioViewModel = viewModel(factory = LessonStudioViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showRecordingsSheet by remember { mutableStateOf(false) }
    var recordingToDelete by remember { mutableStateOf<UserRecording?>(null) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecordingSecure()
        } else {
            // Optionally handle denial
        }
    }

    if (recordingToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordingToDelete = null },
            title = { Text("Delete Recording") },
            text = { Text("Are you sure you want to delete this recording? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        recordingToDelete?.let { viewModel.deleteRecording(it) }
                        recordingToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordingToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRecordingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRecordingsSheet = false },
            windowInsets = WindowInsets(0)
        ) {
            RecordingsSheetContent(
                recordings = uiState.userRecordings,
                currentPlayingId = uiState.playingRecordingId,
                isPlaying = uiState.isPlayingUser,
                onPlay = { viewModel.toggleUserPlayback(it) },
                onDelete = { recordingToDelete = it }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.lesson?.title ?: "Lesson") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            StudioBottomBar(
                isRecording = uiState.isRecording,
                isPlayingReference = uiState.isPlayingReference,
                referenceProgress = uiState.playbackProgress,
                onPlayReference = { viewModel.toggleReferencePlayback() },
                onSeekReference = { viewModel.seekToProgress(it) },
                onToggleRecord = {
                     if (uiState.isRecording) {
                         viewModel.stopRecordingSecure()
                     } else {
                         if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                             viewModel.startRecordingSecure()
                         } else {
                             permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                         }
                     }
                },
                onShowRecordings = { showRecordingsSheet = true }
            )
        },
        snackbarHost = {
             if (uiState.error != null) {
                 Snackbar(
                     action = {
                         TextButton(onClick = { viewModel.dismissError() }) {
                             Text("Dismiss")
                         }
                     }
                 ) {
                     Text(uiState.error!!)
                 }
             }
        }
    ) { innerPadding ->
        // Main Content Area with Segmented Text
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            if (uiState.segments.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.segments) { segment ->
                        SegmentCard(
                            text = segment.text,
                            startTimeMs = segment.startTimeMs,
                            onClick = { viewModel.seekTo(segment.startTimeMs) }
                        )
                    }
                }
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("Loading content...")
                }
            }
        }
    }
}

@Composable
fun SegmentCard(
    text: String,
    startTimeMs: Long,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = formatTime(startTimeMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2 // Reduced line height
            )
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun StudioBottomBar(
    isRecording: Boolean,
    isPlayingReference: Boolean,
    referenceProgress: Float,
    onPlayReference: () -> Unit,
    onSeekReference: (Float) -> Unit,
    onToggleRecord: () -> Unit,
    onShowRecordings: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp)
        ) {
            // Interactive Slider
            Slider(
                value = referenceProgress,
                onValueChange = onSeekReference,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Play Reference Button
                IconButton(
                    onClick = onPlayReference,
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlayingReference) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play Reference",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                // Record Button (Center, Large)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            CircleShape
                        )
                        .clickable(onClick = onToggleRecord)
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Record",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                // Show Recordings Button
                IconButton(
                    onClick = onShowRecordings,
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "My Recordings",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun RecordingsSheetContent(
    recordings: List<UserRecording>,
    currentPlayingId: Int?,
    isPlaying: Boolean,
    onPlay: (UserRecording) -> Unit,
    onDelete: (UserRecording) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(16.dp)
            .heightIn(min = 200.dp, max = 500.dp)
    ) {
        Text(
            text = "Audio Records",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (recordings.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No recordings yet.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn {
                items(recordings) { recording ->
                    RecordingItem(
                        recording = recording,
                        isPlaying = currentPlayingId == recording.id && isPlaying,
                        onPlay = { onPlay(recording) },
                        onDelete = { onDelete(recording) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun RecordingItem(
    recording: UserRecording,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPlay,
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play Take",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Take on ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(recording.createdAt))}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(recording.createdAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
        }
    }
}
