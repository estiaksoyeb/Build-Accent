package com.buildaccent.`as`.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val backupState by viewModel.backupState.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val scrollState = rememberScrollState()
    
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { viewModel.exportBackup(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importBackup(it) }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Select Primary Language") },
            text = {
                LazyColumn {
                    items(uiState.availableLanguages) { language ->
                         Row(
                             modifier = Modifier
                                 .fillMaxWidth()
                                 .clickable {
                                     viewModel.setPreferredLanguage(language)
                                     showLanguageDialog = false
                                 }
                                 .padding(vertical = 12.dp, horizontal = 8.dp),
                             verticalAlignment = Alignment.CenterVertically
                         ) {
                             RadioButton(
                                 selected = language == uiState.preferredLanguage,
                                 onClick = null 
                             )
                             Spacer(modifier = Modifier.width(8.dp))
                             Text(text = language)
                         }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
        snackbarHost = {
            val state = backupState
            if (state is BackupUiState.Success) {
                Snackbar(action = { 
                    TextButton(onClick = { viewModel.clearBackupState() }) { Text("OK") }
                }) { Text(state.message) }
            } else if (state is BackupUiState.Error) {
                Snackbar(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    action = { 
                        TextButton(onClick = { viewModel.clearBackupState() }) { Text("Dismiss") }
                    }
                ) { Text(state.message) }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars) // Handle bottom inset
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Preferences",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                
                SettingsSwitchItem(
                    title = "Enable Lesson Deletion",
                    description = "Show delete buttons on lesson cards.",
                    checked = uiState.isDeleteEnabled,
                    onCheckedChange = { viewModel.toggleDeleteEnabled(it) }
                )
                
                HorizontalDivider()

                SettingsSwitchItem(
                    title = "Enable Lesson Editing",
                    description = "Show edit buttons on lesson cards.",
                    checked = uiState.isEditEnabled,
                    onCheckedChange = { viewModel.toggleEditEnabled(it) }
                )
                
                HorizontalDivider()
                
                SettingsClickableItem(
                    title = "Preferred Language",
                    description = uiState.preferredLanguage ?: "None selected",
                    onClick = { showLanguageDialog = true }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Data Management",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                
                SettingsActionItem(
                    title = "Export Backup",
                    description = "Save all lessons and recordings to a ZIP file.",
                    icon = Icons.Default.Download,
                    onClick = {
                        val fileName = "AccentBuilder_Backup_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.zip"
                        exportLauncher.launch(fileName)
                    }
                )
                
                SettingsActionItem(
                    title = "Import Backup",
                    description = "Restore lessons and recordings from a ZIP file.",
                    icon = Icons.Default.Upload,
                    onClick = {
                        importLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed"))
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "About",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                SettingsClickableItem(
                    title = "Developer",
                    description = "Ahmmed Soyeb",
                    onClick = {
                        uriHandler.openUri("https://github.com/estiaksoyeb")
                    }
                )
                
                // Extra spacer at bottom for comfortable scrolling
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            if (backupState is BackupUiState.Loading) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = (backupState as BackupUiState.Loading).message,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsClickableItem(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SettingsActionItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon, 
            contentDescription = null, 
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}