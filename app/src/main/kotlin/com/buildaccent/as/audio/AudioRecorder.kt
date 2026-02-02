package com.buildaccent.`as`.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.IOException

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    
    fun start(outputFile: File) {
        release() // Ensure clean state
        
        // Make sure parent dir exists
        outputFile.parentFile?.mkdirs()

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile.absolutePath)
            
            try {
                prepare()
                start()
            } catch (e: IOException) {
                e.printStackTrace()
                // Handle error
            }
        }
    }

    fun stop() {
        recorder?.apply {
            try {
                stop()
            } catch (e: RuntimeException) {
                // This can happen if stop is called immediately after start
                e.printStackTrace()
            }
            release()
        }
        recorder = null
    }

    fun release() {
        recorder?.release()
        recorder = null
    }
}
