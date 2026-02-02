package com.buildaccent.`as`.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_recordings",
    foreignKeys = [
        ForeignKey(
            entity = Lesson::class,
            parentColumns = ["id"],
            childColumns = ["lessonId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("lessonId")]
)
data class UserRecording(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lessonId: Int,
    val audioPath: String,
    val createdAt: Long = System.currentTimeMillis(),
    val durationMs: Long = 0
)
