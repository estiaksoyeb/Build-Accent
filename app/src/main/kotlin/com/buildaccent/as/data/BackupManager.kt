package com.buildaccent.`as`.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupData(
    val lessons: List<Lesson>,
    val recordings: List<UserRecording>
)

class BackupManager(
    private val context: Context,
    private val lessonRepository: LessonRepository,
    private val recordingDao: UserRecordingDao
) {
    private val gson = Gson()

    suspend fun exportBackup(uri: Uri) {
        withContext(Dispatchers.IO) {
            val lessons = lessonRepository.getAllLessonsSync()
            val recordings = recordingDao.getAllRecordings()
            val backupData = BackupData(lessons, recordings)
            val jsonMetadata = gson.toJson(backupData)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                    // 1. Write Metadata
                    val metaEntry = ZipEntry("metadata.json")
                    zipOut.putNextEntry(metaEntry)
                    zipOut.write(jsonMetadata.toByteArray())
                    zipOut.closeEntry()

                    // 2. Write Audio Files
                    // Collect all unique file paths from lessons (references) and recordings
                    val filesToBackup = mutableSetOf<String>()
                    lessons.forEach { filesToBackup.add(it.referenceAudioPath) }
                    recordings.forEach { filesToBackup.add(it.audioPath) }

                    for (path in filesToBackup) {
                        val file = File(path)
                        if (file.exists()) {
                            // entry name: audio/filename.ext
                            val entryName = "audio/${file.name}"
                            zipOut.putNextEntry(ZipEntry(entryName))
                            
                            FileInputStream(file).use { fis ->
                                fis.copyTo(zipOut)
                            }
                            zipOut.closeEntry()
                        }
                    }
                }
            }
        }
    }

    suspend fun importBackup(uri: Uri) {
        withContext(Dispatchers.IO) {
            var metadata: BackupData? = null
            // Map old audio filenames to new local paths if needed (though we copy to same name usually)
            // But to avoid overwriting existing files with same name but different content?
            // Strategy: We will extract to a temp dir first, then verify, then move.
            
            val tempDir = File(context.cacheDir, "restore_temp")
            tempDir.mkdirs()
            val audioDir = File(tempDir, "audio")
            audioDir.mkdirs()

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        if (entry.name == "metadata.json") {
                            val jsonString = zipIn.bufferedReader().readText()
                            metadata = gson.fromJson(jsonString, BackupData::class.java)
                        } else if (entry.name.startsWith("audio/")) {
                            val fileName = File(entry.name).name
                            val targetFile = File(audioDir, fileName)
                            FileOutputStream(targetFile).use { fos ->
                                zipIn.copyTo(fos)
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            }

            metadata?.let { data ->
                restoreData(data, audioDir)
            }
            
            // Cleanup
            tempDir.deleteRecursively()
        }
    }

    private suspend fun restoreData(data: BackupData, tempAudioDir: File) {
        // Map Old Lesson ID -> New Lesson ID
        val lessonIdMap = mutableMapOf<Int, Int>()

        // 1. Restore Lessons
        for (oldLesson in data.lessons) {
            // Copy audio file to app storage
            val oldAudioFile = File(oldLesson.referenceAudioPath)
            val tempFile = File(tempAudioDir, oldAudioFile.name)
            
            var newAudioPath = oldLesson.referenceAudioPath
            if (tempFile.exists()) {
                val newFile = File(context.filesDir, oldAudioFile.name)
                // If file exists, maybe generate unique name? 
                // For simplicity, overwrite or skip if exact same name?
                // Let's generate unique name to be safe if it's not built-in
                if (!oldLesson.isBuiltIn) {
                     val uniqueName = "restored_${System.currentTimeMillis()}_${oldAudioFile.name}"
                     val finalFile = File(context.filesDir, uniqueName)
                     tempFile.copyTo(finalFile, overwrite = true)
                     newAudioPath = finalFile.absolutePath
                } else {
                    // Built-in: Check if exists, if not copy.
                    val finalFile = File(context.filesDir, oldAudioFile.name)
                    if (!finalFile.exists()) {
                        tempFile.copyTo(finalFile)
                    }
                    newAudioPath = finalFile.absolutePath
                }
            }

            val newLesson = oldLesson.copy(
                id = 0, // Auto-generate
                referenceAudioPath = newAudioPath
            )
            
            val newId = lessonRepository.insertLesson(newLesson).toInt()
            lessonIdMap[oldLesson.id] = newId
        }

        // 2. Restore Recordings
        for (recording in data.recordings) {
            val newLessonId = lessonIdMap[recording.lessonId]
            if (newLessonId != null) {
                val oldAudioFile = File(recording.audioPath)
                val tempFile = File(tempAudioDir, oldAudioFile.name)
                
                var newAudioPath = recording.audioPath
                if (tempFile.exists()) {
                     val uniqueName = "restored_rec_${System.currentTimeMillis()}_${oldAudioFile.name}"
                     val finalFile = File(context.filesDir, uniqueName)
                     tempFile.copyTo(finalFile, overwrite = true)
                     newAudioPath = finalFile.absolutePath
                }

                val newRecording = recording.copy(
                    id = 0,
                    lessonId = newLessonId,
                    audioPath = newAudioPath
                )
                recordingDao.insertRecording(newRecording)
            }
        }
    }
}
