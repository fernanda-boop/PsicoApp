package com.example.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Patient

@Composable
fun QuickAddSessionDialog(
  targetDateStr: String,
  displayDate: String,
  patients: List<Patient>,
  onDismiss: () -> Unit,
  onSaveSession: (Patient, String) -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedPatient by remember { mutableStateOf<Patient?>(null) }
  var notes by remember { mutableStateOf("Sesión realizada (1-toque)") }

  val activePatients = remember(patients) { patients.filter { it.active } }
  val filteredPatients = remember(activePatients, searchQuery) {
    if (searchQuery.isBlank()) activePatients
    else activePatients.filter { it.name.contains(searchQuery, ignoreCase = true) }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column {
          Text("Añadir Sesión (1 Toque)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
          Text(displayDate, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        if (selectedPatient == null) {
          Text(
            text = "Selecciona el paciente con un toque:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
          )

          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar paciente...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
              if (searchQuery.isNotBlank()) {
                IconButton(onClick = { searchQuery = "" }) {
                  Icon(Icons.Default.Clear, contentDescription = "Borrar")
                }
              }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("quick_search_patient_input")
          )

          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .height(240.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            items(filteredPatients, key = { it.id }) { patient ->
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { selectedPatient = patient }
                  .testTag("quick_patient_item_${patient.id}"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                      shape = CircleShape,
                      color = MaterialTheme.colorScheme.primaryContainer,
                      modifier = Modifier.size(36.dp)
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Text(
                          text = patient.name.firstOrNull()?.uppercase() ?: "?",
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                      }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                      Text(patient.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                      Text(
                        "${patient.dayOfWeek ?: ""} • $${patient.sessionFee.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  }

                  Text("Elegir ⚡", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
              }
            }
          }
        } else {
          // Patient selected - 1 touch confirmation
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column {
                Text("Paciente seleccionado:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(selectedPatient!!.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Honorario: $${selectedPatient!!.sessionFee.toInt()}", style = MaterialTheme.typography.bodySmall)
              }
              OutlinedButton(onClick = { selectedPatient = null }) {
                Text("Cambiar")
              }
            }
          }

          OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Nota rápida de sesión (opcional)") },
            modifier = Modifier.fillMaxWidth().testTag("quick_session_notes_input")
          )
        }
      }
    },
    confirmButton = {
      if (selectedPatient != null) {
        Button(
          onClick = {
            onSaveSession(selectedPatient!!, notes)
          },
          modifier = Modifier.testTag("confirm_quick_session_btn")
        ) {
          Text("⚡ Guardar Sesión (1 Toque)")
        }
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss) {
        Text("Cancelar")
      }
    }
  )
}
