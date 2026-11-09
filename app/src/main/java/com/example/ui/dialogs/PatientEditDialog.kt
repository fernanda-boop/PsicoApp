package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.ClinicalTemplate
import com.example.data.model.FieldType
import com.example.data.model.Patient

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PatientEditDialog(
  patient: Patient? = null,
  templates: List<ClinicalTemplate> = emptyList(),
  onDismiss: () -> Unit,
  onSave: (Patient) -> Unit
) {
  var name by remember { mutableStateOf(patient?.name ?: "") }
  var phone by remember { mutableStateOf(patient?.phone ?: "") }
  var email by remember { mutableStateOf(patient?.email ?: "") }
  var feeText by remember { mutableStateOf(if (patient != null && patient.sessionFee > 0) patient.sessionFee.toInt().toString() else "15000") }
  var diagnosis by remember { mutableStateOf(patient?.diagnosis ?: "") }
  var notes by remember { mutableStateOf(patient?.notes ?: "") }
  var emergencyContact by remember { mutableStateOf(patient?.emergencyContact ?: "") }
  var selectedDay by remember { mutableStateOf(patient?.dayOfWeek ?: "Lunes") }
  var sessionTime by remember { mutableStateOf(patient?.sessionTime ?: "16:00") }
  var active by remember { mutableStateOf(patient?.active ?: true) }

  // Initial template selection
  val defaultTemplate = templates.firstOrNull { it.isDefault } ?: templates.firstOrNull()
  var selectedTemplateId by remember {
    mutableStateOf(patient?.templateId ?: (if (patient == null) defaultTemplate?.id else null))
  }

  // Dynamic custom field values
  val customFieldValues = remember {
    mutableStateMapOf<String, String>().apply {
      patient?.customFieldValues?.forEach { (k, v) -> put(k, v) }
    }
  }

  val activeTemplate = templates.firstOrNull { it.id == selectedTemplateId }

  val days = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")
  var nameError by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (patient == null) "Nuevo Paciente" else "Editar Ficha Técnica",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = {
            name = it
            if (nameError && it.isNotBlank()) nameError = false
          },
          label = { Text("Nombre y Apellido *") },
          leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
          isError = nameError,
          supportingText = if (nameError) { { Text("El nombre es obligatorio") } } else null,
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("patient_name_input")
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = feeText,
            onValueChange = { feeText = it.filter { ch -> ch.isDigit() } },
            label = { Text("Honorario por Sesión") },
            leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .testTag("patient_fee_input")
          )

          OutlinedTextField(
            value = sessionTime,
            onValueChange = { sessionTime = it },
            label = { Text("Horario") },
            leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
            placeholder = { Text("16:00") },
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .testTag("patient_time_input")
          )
        }

        Text(
          text = "Día habitual de sesión:",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          days.take(3).forEach { day ->
            FilterChip(
              selected = selectedDay == day,
              onClick = { selectedDay = day },
              label = { Text(day.take(3)) },
              modifier = Modifier.weight(1f)
            )
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          days.drop(3).forEach { day ->
            FilterChip(
              selected = selectedDay == day,
              onClick = { selectedDay = day },
              label = { Text(day.take(3)) },
              modifier = Modifier.weight(1f)
            )
          }
        }

        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("Teléfono / WhatsApp") },
          leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          placeholder = { Text("Ej: +54911...") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("patient_phone_input")
        )

        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Correo Electrónico (opcional)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          placeholder = { Text("paciente@ejemplo.com") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = diagnosis,
          onValueChange = { diagnosis = it },
          label = { Text("Diagnóstico / Motivo de consulta") },
          leadingIcon = { Icon(Icons.Default.MedicalServices, contentDescription = null) },
          placeholder = { Text("Ej: TDAH, Ansiedad, Duelo, etc.") },
          minLines = 2,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("patient_diagnosis_input")
        )

        // --- PLANTILLAS DE FICHA TÉCNICA Y CAMPOS PERSONALIZADOS ---
        if (templates.isNotEmpty()) {
          HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

          Text(
            text = "Plantilla de Ficha Clínica:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )

          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilterChip(
              selected = selectedTemplateId == null,
              onClick = { selectedTemplateId = null },
              label = { Text("Básica / Libre") }
            )
            templates.forEach { tmpl ->
              FilterChip(
                selected = selectedTemplateId == tmpl.id,
                onClick = { selectedTemplateId = tmpl.id },
                label = { Text(tmpl.name) }
              )
            }
          }

          activeTemplate?.let { tmpl ->
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                  text = "Campos específicos de ${tmpl.name}:",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )

                tmpl.parsedFields().forEach { field ->
                  val currentValue = customFieldValues[field.id] ?: ""

                  when (field.type) {
                    FieldType.DATE.typeName -> {
                      OutlinedTextField(
                        value = currentValue,
                        onValueChange = { customFieldValues[field.id] = it },
                        label = { Text(field.label) },
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                        placeholder = { Text("AAAA-MM-DD") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                      )
                    }
                    FieldType.MULTI_SELECT.typeName -> {
                      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                          text = field.label,
                          style = MaterialTheme.typography.labelMedium,
                          fontWeight = FontWeight.SemiBold
                        )
                        val selectedOptions = currentValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()

                        FlowRow(
                          horizontalArrangement = Arrangement.spacedBy(6.dp),
                          verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                          field.options.forEach { opt ->
                            val isSelected = selectedOptions.contains(opt)
                            FilterChip(
                              selected = isSelected,
                              onClick = {
                                val newSet = if (isSelected) selectedOptions - opt else selectedOptions + opt
                                customFieldValues[field.id] = newSet.joinToString(",")
                              },
                              label = { Text(opt) }
                            )
                          }
                        }
                      }
                    }
                    else -> {
                      // FieldType.TEXT
                      OutlinedTextField(
                        value = currentValue,
                        onValueChange = { customFieldValues[field.id] = it },
                        label = { Text(field.label) },
                        placeholder = { Text("Escribe aquí...") },
                        modifier = Modifier.fillMaxWidth()
                      )
                    }
                  }
                }
              }
            }
          }
        }

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Ficha Técnica / Antecedentes Generales") },
          placeholder = { Text("Notas de encuadre, objetivos terapéuticos...") },
          minLines = 3,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("patient_notes_input")
        )

        OutlinedTextField(
          value = emergencyContact,
          onValueChange = { emergencyContact = it },
          label = { Text("Contacto de Emergencia") },
          placeholder = { Text("Ej: Mamá (11-4455-6677)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = if (active) "Paciente Activo" else "Paciente en Alta / Pausa",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
          )
          Switch(
            checked = active,
            onCheckedChange = { active = it }
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isBlank()) {
            nameError = true
            return@Button
          }
          val parsedFee = feeText.toDoubleOrNull() ?: 0.0
          val updatedPatient = (patient ?: Patient()).copy(
            name = name.trim(),
            phone = phone.trim().ifBlank { null },
            email = email.trim().ifBlank { null },
            sessionFee = parsedFee,
            diagnosis = diagnosis.trim().ifBlank { null },
            notes = notes.trim().ifBlank { null },
            emergencyContact = emergencyContact.trim().ifBlank { null },
            dayOfWeek = selectedDay,
            sessionTime = sessionTime.trim().ifBlank { null },
            active = active,
            templateId = selectedTemplateId,
            customFieldValues = customFieldValues.toMap()
          )
          onSave(updatedPatient)
        },
        modifier = Modifier.testTag("save_patient_button")
      ) {
        Text("Guardar Ficha")
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss) {
        Text("Cancelar")
      }
    }
  )
}
