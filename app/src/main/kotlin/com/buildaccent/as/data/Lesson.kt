package com.buildaccent.`as`.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lessons")
data class Lesson(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val language: String,
    val accent: String,
    val textContent: String,
    val referenceAudioPath: String, // Relative path in internal storage or resource name
    val isBuiltIn: Boolean = false
)
