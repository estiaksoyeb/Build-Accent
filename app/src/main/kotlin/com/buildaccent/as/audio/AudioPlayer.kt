package com.buildaccent.`as`.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import java.io.File
import java.io.IOException

class AudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    fun playFile(file: File, onCompletion: () -> Unit = {}) {
        stop()
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener { onCompletion() }
                start()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    // Play from Raw resource (for the built-in lesson if not copied yet)
    fun playRaw(resId: Int, onCompletion: () -> Unit = {}) {
        stop()
        mediaPlayer = MediaPlayer.create(context, resId).apply {
            setOnCompletionListener { onCompletion() }
            start()
        }
    }

    fun playUri(uri: Uri, onCompletion: () -> Unit = {}) {
        stop()
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(context, uri)
                prepare()
                setOnCompletionListener { onCompletion() }
                start()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
            }
        }
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.seekTo(positionMs)
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false

    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0
    
    fun getDuration(): Int = mediaPlayer?.duration ?: 0

    fun release() {
        stop()
    }
}
