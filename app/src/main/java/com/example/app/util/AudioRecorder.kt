package com.example.app.util

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun start(): Boolean {
        return try {
            val dir = File(context.cacheDir, "audio").apply { mkdirs() }
            val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            outputFile = File(dir, "REC_$name.aac")

            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile!!.absolutePath)
                prepare()
                start()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun stop(): Uri? {
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null

            outputFile?.let {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    it
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun isRecording() = recorder != null
}