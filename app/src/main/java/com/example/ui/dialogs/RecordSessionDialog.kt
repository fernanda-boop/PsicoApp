package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Patient
import com.example.data.model.Session
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordSessionDialog(
  patient: Patient,
  onDismiss: () -> Unit,
  onSaveSession: (Session) -> Unit
) {
  val todayStr = remember {
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
  }

  var date by remember { mutableStateOf(todayStr) }
  var feeText by remember { mutableStateOf(patient.sessionFee.toInt().toString()) }
  var isPaid by remember { mutableStateOf(false) }
  var notes by remember { mutableStateOf("") }

  var showAudioDialog by remember { mutableStateOf(false) }
  var audioPath by remember { mutableStateOf<String?>(null) }
  var audioDuration by remember { mutableStateOf<Long?>(null) }

  if (showAudioDialog) {
    RecordAudioDialog(
      sessionId = "sess_${System.currentTimeMillis()}",
      onDismiss = { showAudioDialog = false },
      onAudioRecorded = { path, duration ->
        audioPath = path
        audioDuration = duration
        showAudioDialog = false
      }
    )
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Column {
        Text(
          text = "Registrar Sesión",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Paciente: ${patient.name}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("Fecha") },
            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )

          OutlinedTextField(
            value = feeText,
            onValueChange = { feeText = it.filter { ch -> ch.isDigit() } },
            label = { Text("Valor ($)") },
            leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Notas de evolución clínica") },
          leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
          placeholder = { Text("Temas trabajados, tareas asignadas...") },
          minLines = 3,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("session_notes_input")
        )

        // Audio recording status / trigger
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            if (audioPath == null) {
              Column {
                Text(
                  text = "Audio de la sesión",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = "Graba notas de voz rápidas",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              FilledTonalButton(
                onClick = { showAudioDialog = true },
                modifier = Modifier.testTag("open_record_audio_dialog_btn")
              ) {
                Icon(Icons.Default.Mic, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Grabar")
              }
            } else {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Audio adjunto (${audioDuration ?: 0}s)",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
              }
              OutlinedButton(onClick = { showAudioDialog = true }) {
                Text("Cambiar")
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = if (isPaid) "Sesión Cobrada al momento" else "Sesión queda pendiente de pago",
            style = MaterialTheme.typography.bodyMedium
          )
          Switch(
            checked = isPaid,
            onCheckedChange = { isPaid = it }
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val parsedFee = feeText.toDoubleOrNull() ?: patient.sessionFee
          val session = Session(
            userId = patient.userId,
            patientId = patient.id,
            patientName = patient.name,
            date = date.trim().ifBlank { todayStr },
            fee = parsedFee,
            isPaid = isPaid,
            notes = notes.trim().ifBlank { null },
            audioPath = audioPath,
            audioDurationSec = audioDuration
          )
          onSaveSession(session)
        },
        modifier = Modifier.testTag("save_session_button")
      ) {
        Text("Guardar Sesión")
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss) {
        Text("Cancelar")
      }
    }
  )
}
