package com.buildaccent.`as`

import android.app.Application
import android.content.Context
import com.buildaccent.`as`.data.AppDatabase
import com.buildaccent.`as`.data.LessonRepository
import com.buildaccent.`as`.data.OfflineLessonRepository
import com.buildaccent.`as`.data.BackupManager
import com.buildaccent.`as`.data.UserPreferencesRepository
import com.buildaccent.`as`.data.UserRecordingDao
import com.buildaccent.`as`.data.dataStore

interface AppContainer {
    val lessonRepository: LessonRepository
    val userRecordingDao: UserRecordingDao
    val userPreferencesRepository: UserPreferencesRepository
    val backupManager: BackupManager
    val context: Context
}

class AppDataContainer(private val appContext: Context) : AppContainer {
    override val context: Context = appContext
    
    private val database: AppDatabase by lazy {
        AppDatabase.getDatabase(appContext)
    }
    
    override val lessonRepository: LessonRepository by lazy {
        OfflineLessonRepository(database.lessonDao())
    }
    
    override val userRecordingDao: UserRecordingDao by lazy {
        database.userRecordingDao()
    }

    override val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(appContext.dataStore)
    }
    
    override val backupManager: BackupManager by lazy {
        BackupManager(appContext, lessonRepository, userRecordingDao)
    }
}

class AccentBuilderApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)
    }
}
