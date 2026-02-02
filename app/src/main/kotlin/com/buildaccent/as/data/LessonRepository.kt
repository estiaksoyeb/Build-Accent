package com.buildaccent.`as`.data

import kotlinx.coroutines.flow.Flow

interface LessonRepository {
    fun getAllLessonsStream(): Flow<List<Lesson>>
    suspend fun getAllLessonsSync(): List<Lesson>
    suspend fun getLesson(id: Int): Lesson?
    suspend fun insertLesson(lesson: Lesson): Long
    suspend fun updateLesson(lesson: Lesson)
    suspend fun deleteLesson(lesson: Lesson)
    suspend fun getCount(): Int
}

class OfflineLessonRepository(private val lessonDao: LessonDao) : LessonRepository {
    override fun getAllLessonsStream(): Flow<List<Lesson>> = lessonDao.getAllLessons()
    override suspend fun getAllLessonsSync(): List<Lesson> = lessonDao.getAllLessonsSync()
    override suspend fun getLesson(id: Int): Lesson? = lessonDao.getLesson(id)
    override suspend fun insertLesson(lesson: Lesson): Long = lessonDao.insertLesson(lesson)
    override suspend fun updateLesson(lesson: Lesson) = lessonDao.updateLesson(lesson)
    override suspend fun deleteLesson(lesson: Lesson) = lessonDao.deleteLesson(lesson)
    override suspend fun getCount(): Int = lessonDao.getCount()
}
