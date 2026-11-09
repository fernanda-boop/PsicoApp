package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

private const val TAG = "AudioRecordingManager"

class AudioRecordingManager {

  private var mediaRecorder: MediaRecorder? = null
  private var mediaPlayer: MediaPlayer? = null

  private var currentFilePath: String? = null
  private var recordingStartTime = 0L

  private val _isRecording = MutableStateFlow(false)
  val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

  private val _recordingDurationSec = MutableStateFlow(0)
  val recordingDurationSec: StateFlow<Int> = _recordingDurationSec.asStateFlow()

  private val _isPlaying = MutableStateFlow(false)
  val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

  private val _playingPositionSec = MutableStateFlow(0)
  val playingPositionSec: StateFlow<Int> = _playingPositionSec.asStateFlow()

  private val _playingTotalDurationSec = MutableStateFlow(0)
  val playingTotalDurationSec: StateFlow<Int> = _playingTotalDurationSec.asStateFlow()

  private var timerJob: Job? = null
  private var playbackJob: Job? = null
  private val scope = CoroutineScope(Dispatchers.Main)

  fun startRecording(context: Context, sessionId: String): Result<String> {
    return try {
      stopPlayback()
      stopRecording()

      val audioDir = File(context.filesDir, "session_audios").apply {
        if (!exists()) mkdirs()
      }
      val filename = "audio_${sessionId}_${System.currentTimeMillis()}.m4a"
      val outputFile = File(audioDir, filename)
      currentFilePath = "session_audios/$filename"

      val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(context)
      } else {
        @Suppress("DEPRECATION")
        MediaRecorder()
      }

      recorder.apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        setAudioEncodingBitRate(128000)
        setAudioSamplingRate(44100)
        setOutputFile(outputFile.absolutePath)
        prepare()
        start()
      }

      mediaRecorder = recorder
      recordingStartTime = System.currentTimeMillis()
      _isRecording.value = true
      _recordingDurationSec.value = 0

      timerJob = scope.launch {
        while (isActive && _isRecording.value) {
          delay(1000)
          _recordingDurationSec.value = ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt()
        }
      }

      Result.success(currentFilePath!!)
    } catch (e: Exception) {
      Log.e(TAG, "Error starting recording", e)
      cleanupRecorder()
      Result.failure(e)
    }
  }

  fun stopRecording(): Pair<String?, Long> {
    timerJob?.cancel()
    timerJob = null

    val relativePath = currentFilePath
    val durationSec = if (recordingStartTime > 0L) {
      ((System.currentTimeMillis() - recordingStartTime) / 1000).coerceAtLeast(1L)
    } else 0L

    try {
      mediaRecorder?.apply {
        try {
          stop()
        } catch (e: RuntimeException) {
          // Can happen if stop() is called immediately after start()
          Log.w(TAG, "Stop failed on recorder", e)
        }
        release()
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error releasing recorder", e)
    } finally {
      mediaRecorder = null
      _isRecording.value = false
      recordingStartTime = 0L
    }

    return Pair(relativePath, durationSec)
  }

  fun playAudio(context: Context, relativePath: String, onFinished: () -> Unit = {}) {
    stopPlayback()

    val file = File(context.filesDir, relativePath)
    if (!file.exists()) {
      Log.w(TAG, "Audio file does not exist: ${file.absolutePath}")
      return
    }

    try {
      val player = MediaPlayer().apply {
        setDataSource(file.absolutePath)
        prepare()
        start()
        setOnCompletionListener {
          stopPlayback()
          onFinished()
        }
      }

      mediaPlayer = player
      _isPlaying.value = true
      _playingTotalDurationSec.value = (player.duration / 1000).coerceAtLeast(1)

      playbackJob = scope.launch {
        while (isActive && _isPlaying.value) {
          mediaPlayer?.let { p ->
            if (p.isPlaying) {
              _playingPositionSec.value = (p.currentPosition / 1000)
            }
          }
          delay(500)
        }
      }
    } catch (e: IOException) {
      Log.e(TAG, "Error playing audio", e)
      stopPlayback()
    }
  }

  fun stopPlayback() {
    playbackJob?.cancel()
    playbackJob = null
    try {
      mediaPlayer?.apply {
        if (isPlaying) stop()
        release()
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error releasing mediaPlayer", e)
    } finally {
      mediaPlayer = null
      _isPlaying.value = false
      _playingPositionSec.value = 0
    }
  }

  private fun cleanupRecorder() {
    timerJob?.cancel()
    timerJob = null
    try {
      mediaRecorder?.release()
    } catch (_: Exception) {}
    mediaRecorder = null
    _isRecording.value = false
  }
}
