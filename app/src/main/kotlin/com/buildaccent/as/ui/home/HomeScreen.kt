package com.buildaccent.`as`.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.buildaccent.`as`.data.Lesson

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navigateToLesson: (Int) -> Unit,
    navigateToCreate: () -> Unit,
    navigateToEdit: (Int) -> Unit,
    navigateToSettings: () -> Unit,
    navigateToWelcome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
) {
    val uiState by viewModel.homeUiState.collectAsState()
    var lessonToDelete by remember { mutableStateOf<Lesson?>(null) }

    if (lessonToDelete != null) {
        AlertDialog(
            onDismissRequest = { lessonToDelete = null },
            title = { Text("Delete Lesson") },
            text = { Text("Are you sure you want to delete this lesson and all its recordings? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        lessonToDelete?.let { viewModel.deleteLesson(it) }
                        lessonToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { lessonToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accent Builder") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = navigateToWelcome) {
                        Icon(Icons.Default.Help, contentDescription = "Help")
                    }
                    IconButton(onClick = navigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = navigateToCreate) {
                Icon(Icons.Default.Add, contentDescription = "Add Lesson")
            }
        }
    ) { innerPadding ->
        HomeBody(
            lessonList = uiState.lessonList,
            isDeleteEnabled = uiState.isDeleteEnabled,
            isEditEnabled = uiState.isEditEnabled,
            preferredLanguage = uiState.preferredLanguage,
            onLessonClick = navigateToLesson,
            onEditClick = navigateToEdit,
            onDeleteClick = { lessonToDelete = it },
            modifier = modifier.padding(innerPadding)
        )
    }
}

@Composable
fun HomeBody(
    lessonList: List<Lesson>,
    isDeleteEnabled: Boolean,
    isEditEnabled: Boolean,
    preferredLanguage: String?,
    onLessonClick: (Int) -> Unit,
    onEditClick: (Int) -> Unit,
    onDeleteClick: (Lesson) -> Unit,
    modifier: Modifier = Modifier
) {
    if (lessonList.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No lessons yet. Add one!", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        val languages = remember(lessonList, preferredLanguage) {
            val allLanguages = lessonList.map { it.language }.distinct().sorted()
            if (preferredLanguage != null && allLanguages.contains(preferredLanguage)) {
                val others = allLanguages.filter { it != preferredLanguage }
                listOf(preferredLanguage) + others
            } else {
                allLanguages
            }
        }
        var selectedTabIndex by remember { mutableIntStateOf(0) }

        // Adjust selection if it goes out of bounds
        LaunchedEffect(languages) {
            if (selectedTabIndex >= languages.size) {
                selectedTabIndex = 0.coerceAtLeast(languages.size - 1)
            }
        }

        Column(modifier = modifier.fillMaxSize()) {
            if (languages.size > 1) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    languages.forEachIndexed { index, language ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(language) }
                        )
                    }
                }
            }

            val filteredLessons = if (languages.isNotEmpty()) {
                if (languages.size > 1) {
                    val currentLang = languages.getOrElse(selectedTabIndex) { "" }
                    lessonList.filter { it.language == currentLang }
                } else {
                    lessonList
                }
            } else {
                emptyList()
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(filteredLessons) { lesson ->
                    LessonCard(
                        lesson = lesson,
                        isDeleteEnabled = isDeleteEnabled,
                        isEditEnabled = isEditEnabled,
                        onClick = { onLessonClick(lesson.id) },
                        onEdit = { onEditClick(lesson.id) },
                        onDelete = { onDeleteClick(lesson) }
                    )
                }
            }
        }
    }
}

@Composable
fun LessonCard(
    lesson: Lesson,
    isDeleteEnabled: Boolean,
    isEditEnabled: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Badge(lesson.accent, MaterialTheme.colorScheme.secondaryContainer)
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isEditEnabled) {
                     IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                if (isDeleteEnabled) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
                
                if (!isEditEnabled && !isDeleteEnabled) {
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun Badge(text: String, color: Color) {
    Surface(
        color = color,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}