package com.example.ui.dialogs

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.AudioRecordingManager
import com.example.ui.theme.StatusDanger
import java.util.Locale

@Composable
fun RecordAudioDialog(
  sessionId: String,
  onDismiss: () -> Unit,
  onAudioRecorded: (filePath: String, durationSec: Long) -> Unit
) {
  val context = LocalContext.current
  val audioManager = remember { AudioRecordingManager() }

  val isRecording by audioManager.isRecording.collectAsStateWithLifecycle()
  val recordingDurationSec by audioManager.recordingDurationSec.collectAsStateWithLifecycle()
  val isPlaying by audioManager.isPlaying.collectAsStateWithLifecycle()
  val playingPositionSec by audioManager.playingPositionSec.collectAsStateWithLifecycle()
  val playingTotalDurationSec by audioManager.playingTotalDurationSec.collectAsStateWithLifecycle()

  var hasPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
      ) == PackageManager.PERMISSION_GRANTED
    )
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    hasPermission = granted
    if (granted) {
      audioManager.startRecording(context, sessionId)
    }
  }

  var recordedPath by remember { mutableStateOf<String?>(null) }
  var recordedDuration by remember { mutableStateOf(0L) }

  // Clean up on exit
  DisposableEffect(Unit) {
    onDispose {
      audioManager.stopPlayback()
      if (isRecording) {
        audioManager.stopRecording()
      }
    }
  }

  val pulseTransition = rememberInfiniteTransition(label = "pulse")
  val pulseScale by pulseTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.25f,
    animationSpec = infiniteRepeatable(
      animation = tween(700),
      repeatMode = RepeatMode.Reverse
    ),
    label = "scale"
  )

  AlertDialog(
    onDismissRequest = {
      audioManager.stopPlayback()
      if (isRecording) audioManager.stopRecording()
      onDismiss()
    },
    title = {
      Text(
        text = "Grabar Audio de la Sesión",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "Grabación confidencial guardada localmente en tu dispositivo.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Timer display
        val displayedSeconds = if (isRecording) {
          recordingDurationSec
        } else if (recordedPath != null) {
          if (isPlaying) playingPositionSec else recordedDuration.toInt()
        } else {
          0
        }

        val minutes = displayedSeconds / 60
        val seconds = displayedSeconds % 60
        val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

        Text(
          text = timeFormatted,
          fontSize = 36.sp,
          fontWeight = FontWeight.Bold,
          color = if (isRecording) StatusDanger else MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Recording / Playback Controls
        if (recordedPath == null) {
          // Recording in progress or not started
          Box(contentAlignment = Alignment.Center) {
            if (isRecording) {
              Box(
                modifier = Modifier
                  .size(90.dp)
                  .scale(pulseScale)
                  .background(StatusDanger.copy(alpha = 0.2f), CircleShape)
              )
            }

            IconButton(
              onClick = {
                if (!hasPermission) {
                  permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                } else {
                  if (isRecording) {
                    val (path, dur) = audioManager.stopRecording()
                    recordedPath = path
                    recordedDuration = dur
                  } else {
                    audioManager.startRecording(context, sessionId)
                  }
                }
              },
              modifier = Modifier
                .size(72.dp)
                .background(
                  if (isRecording) StatusDanger else MaterialTheme.colorScheme.primary,
                  CircleShape
                )
                .testTag("record_audio_button")
            ) {
              Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isRecording) "Detener grabación" else "Comenzar a grabar",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = if (isRecording) "Grabando... Toca para detener" else "Toca el micrófono para comenzar",
            style = MaterialTheme.typography.labelLarge,
            color = if (isRecording) StatusDanger else MaterialTheme.colorScheme.onSurfaceVariant
          )
        } else {
          // Audio recorded, playback controls available
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
              ) {
                FilledTonalButton(
                  onClick = {
                    if (isPlaying) {
                      audioManager.stopPlayback()
                    } else {
                      recordedPath?.let { audioManager.playAudio(context, it) }
                    }
                  },
                  modifier = Modifier.testTag("play_recorded_audio_button")
                ) {
                  Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir"
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(if (isPlaying) "Pausar" else "Escuchar audio")
                }
              }

              if (isPlaying && playingTotalDurationSec > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                  progress = { (playingPositionSec.toFloat() / playingTotalDurationSec).coerceIn(0f, 1f) },
                  modifier = Modifier.fillMaxWidth()
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          OutlinedButton(
            onClick = {
              audioManager.stopPlayback()
              recordedPath = null
              recordedDuration = 0L
            }
          ) {
            Text("Descartar y volver a grabar")
          }
        }
      }
    },
    confirmButton = {
      AnimatedVisibility(visible = recordedPath != null) {
        Button(
          onClick = {
            val path = recordedPath
            if (path != null) {
              audioManager.stopPlayback()
              onAudioRecorded(path, recordedDuration)
            }
          },
          modifier = Modifier.testTag("save_audio_button")
        ) {
          Text("Guardar audio")
        }
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = {
          audioManager.stopPlayback()
          if (isRecording) audioManager.stopRecording()
          onDismiss()
        }
      ) {
        Text("Cancelar")
      }
    }
  )
}
