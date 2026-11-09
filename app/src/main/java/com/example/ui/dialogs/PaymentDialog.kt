package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Patient
import com.example.data.model.Payment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PaymentDialog(
  patient: Patient,
  onDismiss: () -> Unit,
  onSavePayment: (Payment) -> Unit
) {
  val todayStr = remember {
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
  }

  // Pre-fill with debt amount if any, or session fee
  val initialAmount = when {
    patient.debtAmount > 0 -> patient.debtAmount.toInt().toString()
    patient.sessionFee > 0 -> patient.sessionFee.toInt().toString()
    else -> ""
  }

  var amountText by remember { mutableStateOf(initialAmount) }
  var selectedMethod by remember { mutableStateOf("Transferencia") }
  var date by remember { mutableStateOf(todayStr) }
  var notes by remember { mutableStateOf("") }
  var amountError by remember { mutableStateOf(false) }

  val methods = listOf("Transferencia", "Efectivo", "Mercado Pago", "Tarjeta")

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Column {
        Text(
          text = "Registrar Pago",
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
        // Quick Amount Chips (ADHD friendly!)
        Text(
          text = "Montos rápidos:",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (patient.sessionFee > 0) {
            FilterChip(
              selected = amountText == patient.sessionFee.toInt().toString(),
              onClick = { amountText = patient.sessionFee.toInt().toString() },
              label = { Text("1 Sesión ($${patient.sessionFee.toInt()})") }
            )
          }
          if (patient.debtAmount > 0 && patient.debtAmount != patient.sessionFee) {
            FilterChip(
              selected = amountText == patient.debtAmount.toInt().toString(),
              onClick = { amountText = patient.debtAmount.toInt().toString() },
              label = { Text("Total Deuda ($${patient.debtAmount.toInt()})") }
            )
          }
        }

        OutlinedTextField(
          value = amountText,
          onValueChange = {
            amountText = it.filter { ch -> ch.isDigit() || ch == '.' }
            if (amountError && it.isNotBlank()) amountError = false
          },
          label = { Text("Monto cobrado ($) *") },
          leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          isError = amountError,
          supportingText = if (amountError) { { Text("Ingresa un monto válido") } } else null,
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("payment_amount_input")
        )

        Text(
          text = "Medio de pago:",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          methods.take(2).forEach { method ->
            FilterChip(
              selected = selectedMethod == method,
              onClick = { selectedMethod = method },
              label = { Text(method) },
              modifier = Modifier.weight(1f)
            )
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          methods.drop(2).forEach { method ->
            FilterChip(
              selected = selectedMethod == method,
              onClick = { selectedMethod = method },
              label = { Text(method) },
              modifier = Modifier.weight(1f)
            )
          }
        }

        OutlinedTextField(
          value = date,
          onValueChange = { date = it },
          label = { Text("Fecha de Cobro") },
          leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Notas / Nro Comprobante (Opcional)") },
          leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
          placeholder = { Text("Ej: Transferencia Banco Galicia...") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val parsedAmount = amountText.toDoubleOrNull()
          if (parsedAmount == null || parsedAmount <= 0.0) {
            amountError = true
            return@Button
          }

          val payment = Payment(
            userId = patient.userId,
            patientId = patient.id,
            patientName = patient.name,
            amount = parsedAmount,
            date = date.trim().ifBlank { todayStr },
            paymentMethod = selectedMethod,
            notes = notes.trim().ifBlank { null }
          )
          onSavePayment(payment)
        },
        modifier = Modifier.testTag("confirm_payment_button")
      ) {
        Text("Registrar Cobro")
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss) {
        Text("Cancelar")
      }
    }
  )
}
