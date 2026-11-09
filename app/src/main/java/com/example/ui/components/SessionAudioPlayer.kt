package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.AudioRecordingManager
import java.util.Locale

@Composable
fun SessionAudioPlayer(
  audioPath: String,
  audioDurationSec: Long?,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val audioManager = remember { AudioRecordingManager() }

  val isPlaying by audioManager.isPlaying.collectAsStateWithLifecycle()
  val positionSec by audioManager.playingPositionSec.collectAsStateWithLifecycle()

  DisposableEffect(Unit) {
    onDispose {
      audioManager.stopPlayback()
    }
  }

  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = {
            if (isPlaying) {
              audioManager.stopPlayback()
            } else {
              audioManager.playAudio(context, audioPath)
            }
          },
          modifier = Modifier
            .size(36.dp)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
            .testTag("play_session_audio_btn")
        ) {
          Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pausar audio" else "Reproducir audio",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(20.dp)
          )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Icon(
          imageVector = Icons.Default.GraphicEq,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
          text = if (isPlaying) "Reproduciendo..." else "Nota de voz de la sesión",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      val totalDur = audioDurationSec ?: 0L
      val displaySec = if (isPlaying) positionSec.toLong() else totalDur
      val minutes = displaySec / 60
      val seconds = displaySec % 60
      val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

      Text(
        text = formattedTime,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )
    }
  }
}
