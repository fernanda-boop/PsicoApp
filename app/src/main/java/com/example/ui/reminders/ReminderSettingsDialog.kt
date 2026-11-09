package com.example.ui.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ReminderConfig

@Composable
fun ReminderSettingsDialog(
  currentConfig: ReminderConfig,
  onDismiss: () -> Unit,
  onSave: (ReminderConfig) -> Unit
) {
  var sessionChannel by remember { mutableStateOf(currentConfig.sessionReminderChannel) }
  var sessionHoursBefore by remember { mutableLongStateOf(currentConfig.sessionReminderHoursBefore) }
  var sessionTemplate by remember { mutableStateOf(currentConfig.sessionMessageTemplate) }

  var debtChannel by remember { mutableStateOf(currentConfig.debtReminderChannel) }
  var debtFrequency by remember { mutableStateOf(currentConfig.debtReminderFrequency) }
  var debtTemplate by remember { mutableStateOf(currentConfig.debtMessageTemplate) }
  var notifyMePendingDebts by remember { mutableStateOf(currentConfig.notifyMePendingDebts) }

  val channels = listOf("whatsapp" to "WhatsApp", "sms" to "SMS", "email" to "Email")
  val timingOptions = listOf(2L to "2h antes", 24L to "24h antes (1 día)", 48L to "48h antes (2 días)")
  val frequencyOptions = listOf("weekly" to "Semanal", "biweekly" to "Quincenal", "monthly" to "Fin de mes")

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
          text = "Configuración de Recordatorios",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // --- SESIONES PRÓXIMAS ---
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = "Recordatorio de Próximas Sesiones (Pacientes)",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.primary
            )

            Text("Canal preferido:", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              channels.forEach { (key, label) ->
                FilterChip(
                  selected = sessionChannel.equals(key, ignoreCase = true),
                  onClick = { sessionChannel = key },
                  label = { Text(label) }
                )
              }
            }

            Text("Anticipación:", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              timingOptions.forEach { (hours, label) ->
                FilterChip(
                  selected = sessionHoursBefore == hours,
                  onClick = { sessionHoursBefore = hours },
                  label = { Text(label) }
                )
              }
            }

            OutlinedTextField(
              value = sessionTemplate,
              onValueChange = { sessionTemplate = it },
              label = { Text("Mensaje automático para la sesión") },
              minLines = 3,
              modifier = Modifier.fillMaxWidth().testTag("session_reminder_template_input")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
              Text("Tokens:", style = MaterialTheme.typography.labelSmall)
              AssistChip(onClick = { sessionTemplate += " {nombre}" }, label = { Text("{nombre}") })
              AssistChip(onClick = { sessionTemplate += " {dia}" }, label = { Text("{dia}") })
              AssistChip(onClick = { sessionTemplate += " {hora}" }, label = { Text("{hora}") })
            }
          }
        }

        // --- RECORDATORIOS DE PAGOS PENDIENTES ---
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = "Recordatorio de Pagos y Deudas Pendientes",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.tertiary
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Alertarme en el Dashboard",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Medium
                )
                Text(
                  text = "Muestra tarjeta destacada con cobros pendientes para enviar 1-clic",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Switch(
                checked = notifyMePendingDebts,
                onCheckedChange = { notifyMePendingDebts = it }
              )
            }

            Text("Canal para recordatorio de pago:", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              channels.forEach { (key, label) ->
                FilterChip(
                  selected = debtChannel.equals(key, ignoreCase = true),
                  onClick = { debtChannel = key },
                  label = { Text(label) }
                )
              }
            }

            Text("Frecuencia sugerida de cobro:", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              frequencyOptions.forEach { (freqKey, label) ->
                FilterChip(
                  selected = debtFrequency.equals(freqKey, ignoreCase = true),
                  onClick = { debtFrequency = freqKey },
                  label = { Text(label) }
                )
              }
            }

            OutlinedTextField(
              value = debtTemplate,
              onValueChange = { debtTemplate = it },
              label = { Text("Mensaje automático de deuda") },
              minLines = 3,
              modifier = Modifier.fillMaxWidth().testTag("debt_reminder_template_input")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
              Text("Tokens:", style = MaterialTheme.typography.labelSmall)
              AssistChip(onClick = { debtTemplate += " {nombre}" }, label = { Text("{nombre}") })
              AssistChip(onClick = { debtTemplate += " \${monto}" }, label = { Text("\${monto}") })
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val updated = currentConfig.copy(
            sessionReminderChannel = sessionChannel,
            sessionReminderHoursBefore = sessionHoursBefore,
            sessionMessageTemplate = sessionTemplate.trim(),
            debtReminderChannel = debtChannel,
            debtReminderFrequency = debtFrequency,
            debtMessageTemplate = debtTemplate.trim(),
            notifyMePendingDebts = notifyMePendingDebts
          )
          onSave(updated)
        },
        modifier = Modifier.testTag("save_reminder_settings_button")
      ) {
        Text("Guardar Configuración")
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss) {
        Text("Cancelar")
      }
    }
  )
}
