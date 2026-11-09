package com.example.ui.templates

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ClinicalTemplate
import com.example.data.model.FieldType
import com.example.data.model.TemplateField
import java.util.UUID

@Composable
fun TemplateManagerDialog(
  templates: List<ClinicalTemplate>,
  onDismiss: () -> Unit,
  onSaveTemplate: (ClinicalTemplate) -> Unit,
  onDeleteTemplate: (String) -> Unit
) {
  var editingTemplate by remember { mutableStateOf<ClinicalTemplate?>(null) }
  var isCreatingNew by remember { mutableStateOf(false) }

  if (editingTemplate != null || isCreatingNew) {
    TemplateEditorDialog(
      template = editingTemplate,
      onDismiss = {
        editingTemplate = null
        isCreatingNew = false
      },
      onSave = { saved ->
        onSaveTemplate(saved)
        editingTemplate = null
        isCreatingNew = false
      }
    )
    return
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Text("Plantillas de Ficha", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Diseña modelos de ficha técnica con campos personalizados (texto, fecha o selección múltiple) para aplicar a tus pacientes.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
          onClick = { isCreatingNew = true },
          modifier = Modifier.fillMaxWidth().testTag("create_template_button")
        ) {
          Icon(Icons.Default.Add, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Crear Nueva Plantilla")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        if (templates.isEmpty()) {
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
          ) {
            Column(
              modifier = Modifier.padding(16.dp).fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "No tienes plantillas creadas",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
              )
              Text(
                text = "Crea una para estandarizar tus campos clínicos por tipo de paciente.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        } else {
          templates.forEach { tmpl ->
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(
                containerColor = if (tmpl.isDefault) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
              )
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                      Text(tmpl.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                      if (tmpl.isDefault) {
                        Surface(
                          color = MaterialTheme.colorScheme.primary,
                          shape = RoundedCornerShape(4.dp)
                        ) {
                          Text(
                            text = "POR DEFECTO",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                          )
                        }
                      }
                    }
                    if (!tmpl.description.isNullOrBlank()) {
                      Text(
                        tmpl.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                    Text(
                      text = "${tmpl.fields.size} campos personalizados",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.padding(top = 4.dp)
                    )
                  }

                  Row {
                    IconButton(onClick = { editingTemplate = tmpl }) {
                      Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { onDeleteTemplate(tmpl.id) }) {
                      Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                    }
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss) {
        Text("Listo")
      }
    }
  )
}

@Composable
fun TemplateEditorDialog(
  template: ClinicalTemplate?,
  onDismiss: () -> Unit,
  onSave: (ClinicalTemplate) -> Unit
) {
  var name by remember { mutableStateOf(template?.name ?: "") }
  var description by remember { mutableStateOf(template?.description ?: "") }
  var isDefault by remember { mutableStateOf(template?.isDefault ?: false) }

  // Custom fields
  val fields = remember {
    mutableStateListOf<TemplateField>().apply {
      if (template != null) {
        addAll(template.parsedFields())
      } else {
        // Pre-populate with typical clinical fields for ADHD friendly start
        add(TemplateField(id = UUID.randomUUID().toString(), label = "Medicación y Dosis", type = FieldType.TEXT.typeName))
        add(TemplateField(id = UUID.randomUUID().toString(), label = "Fecha Primera Consulta", type = FieldType.DATE.typeName))
        add(TemplateField(id = UUID.randomUUID().toString(), label = "Vía de Derivación", type = FieldType.MULTI_SELECT.typeName, options = listOf("Médico", "Familiar", "Escuela / Trabajo", "Iniciativa propia")))
      }
    }
  }

  var newFieldLabel by remember { mutableStateOf("") }
  var newFieldType by remember { mutableStateOf(FieldType.TEXT) }
  var newFieldOptionsText by remember { mutableStateOf("") }
  var nameError by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (template == null) "Nueva Plantilla de Ficha" else "Editar Plantilla",
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
          label = { Text("Nombre de la Plantilla *") },
          placeholder = { Text("Ej: TCC Adultos, Infanto-Juvenil...") },
          isError = nameError,
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("template_name_input")
        )

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Descripción (opcional)") },
          placeholder = { Text("Objetivos del modelo o población objetivo") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Usar por defecto para nuevos pacientes", style = MaterialTheme.typography.bodyMedium)
          Switch(checked = isDefault, onCheckedChange = { isDefault = it })
        }

        HorizontalDivider()

        Text(
          text = "Campos Personalizados (${fields.size})",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )

        // Existing fields list
        fields.forEachIndexed { index, field ->
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(field.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                val typeDesc = when (field.type) {
                  FieldType.DATE.typeName -> "Fecha (YYYY-MM-DD)"
                  FieldType.MULTI_SELECT.typeName -> "Selección Múltiple (${field.options.joinToString(", ")})"
                  else -> "Texto libre"
                }
                Text(typeDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              IconButton(onClick = { fields.removeAt(index) }) {
                Icon(Icons.Default.Close, contentDescription = "Quitar campo", tint = MaterialTheme.colorScheme.error)
              }
            }
          }
        }

        // Section to add a new field
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Agregar Campo a la Plantilla", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)

            OutlinedTextField(
              value = newFieldLabel,
              onValueChange = { newFieldLabel = it },
              label = { Text("Nombre del campo (ej: Medicación)") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("new_field_label_input")
            )

            Text("Tipo de campo:", style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              FieldType.entries.forEach { fType ->
                FilterChip(
                  selected = newFieldType == fType,
                  onClick = { newFieldType = fType },
                  label = { Text(fType.displayName) }
                )
              }
            }

            if (newFieldType == FieldType.MULTI_SELECT) {
              OutlinedTextField(
                value = newFieldOptionsText,
                onValueChange = { newFieldOptionsText = it },
                label = { Text("Opciones separadas por coma") },
                placeholder = { Text("Opción 1, Opción 2, Opción 3") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
            }

            Button(
              onClick = {
                if (newFieldLabel.isNotBlank()) {
                  val options = if (newFieldType == FieldType.MULTI_SELECT) {
                    newFieldOptionsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                  } else {
                    emptyList()
                  }
                  fields.add(
                    TemplateField(
                      id = UUID.randomUUID().toString(),
                      label = newFieldLabel.trim(),
                      type = newFieldType.typeName,
                      options = options
                    )
                  )
                  newFieldLabel = ""
                  newFieldOptionsText = ""
                }
              },
              enabled = newFieldLabel.isNotBlank(),
              modifier = Modifier.align(Alignment.End).testTag("add_field_button")
            ) {
              Icon(Icons.Default.Add, contentDescription = null)
              Spacer(Modifier.width(4.dp))
              Text("Añadir Campo")
            }
          }
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
          val result = (template ?: ClinicalTemplate()).copy(
            name = name.trim(),
            description = description.trim().ifBlank { null },
            isDefault = isDefault,
            fields = fields.map { it.toMap() }
          )
          onSave(result)
        },
        modifier = Modifier.testTag("save_template_button")
      ) {
        Text("Guardar Plantilla")
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss) {
        Text("Cancelar")
      }
    }
  )
}
