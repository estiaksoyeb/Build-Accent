package com.buildaccent.`as`.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons ORDER BY id DESC")
    fun getAllLessons(): Flow<List<Lesson>>

    @Query("SELECT * FROM lessons ORDER BY id ASC")
    suspend fun getAllLessonsSync(): List<Lesson>

    @Query("SELECT * FROM lessons WHERE id = :id")
    suspend fun getLesson(id: Int): Lesson?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: Lesson): Long

    @Update
    suspend fun updateLesson(lesson: Lesson)

    @Delete
    suspend fun deleteLesson(lesson: Lesson)
    
    @Query("SELECT COUNT(*) FROM lessons")
    suspend fun getCount(): Int
}

@Dao
interface UserRecordingDao {
    @Query("SELECT * FROM user_recordings WHERE lessonId = :lessonId ORDER BY createdAt DESC")
    fun getRecordingsForLesson(lessonId: Int): Flow<List<UserRecording>>

    @Query("SELECT * FROM user_recordings")
    suspend fun getAllRecordings(): List<UserRecording>

    @Insert
    suspend fun insertRecording(recording: UserRecording)

    @Delete
    suspend fun deleteRecording(recording: UserRecording)
}

@Database(entities = [Lesson::class, UserRecording::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun lessonDao(): LessonDao
    abstract fun userRecordingDao(): UserRecordingDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "accent_builder_db")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
