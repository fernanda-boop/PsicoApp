package com.example.ui.patient

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ClinicalTemplate
import com.example.data.model.Patient
import com.example.data.model.Payment
import com.example.data.model.ReminderConfig
import com.example.data.model.Session
import com.example.data.repository.PsychologistRepository
import com.example.ui.components.SessionAudioPlayer
import com.example.ui.dialogs.PatientEditDialog
import com.example.ui.dialogs.PaymentDialog
import com.example.ui.dialogs.RecordAudioDialog
import com.example.ui.dialogs.RecordSessionDialog
import com.example.ui.reminders.ReminderHelper
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerContainer
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessContainer
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningContainer
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
  currentUserId: String,
  patientId: String,
  repository: PsychologistRepository,
  onNavigateBack: () -> Unit
) {
  BackHandler(onBack = onNavigateBack)

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  val patientsList by repository.observePatients(currentUserId).collectAsStateWithLifecycle(emptyList())
  val patient = patientsList.firstOrNull { it.id == patientId }

  val allSessions by repository.observeSessions(currentUserId).collectAsStateWithLifecycle(emptyList())
  val patientSessions = remember(allSessions, patientId) {
    allSessions.filter { it.patientId == patientId }
  }

  val allPayments by repository.observePayments(currentUserId).collectAsStateWithLifecycle(emptyList())
  val patientPayments = remember(allPayments, patientId) {
    allPayments.filter { it.patientId == patientId }
  }

  val templates by repository.observeTemplates(currentUserId).collectAsStateWithLifecycle(emptyList())
  val reminderConfig by repository.observeReminderConfig(currentUserId).collectAsStateWithLifecycle(ReminderConfig(userId = currentUserId))

  var selectedTab by remember { mutableIntStateOf(0) } // 0: Ficha, 1: Sesiones, 2: Pagos

  var showEditPatientDialog by remember { mutableStateOf(false) }
  var showNewSessionDialog by remember { mutableStateOf(false) }
  var showPaymentDialog by remember { mutableStateOf(false) }

  // Dialog for adding audio note to an existing session
  var sessionForAudioRecording by remember { mutableStateOf<Session?>(null) }

  if (showEditPatientDialog && patient != null) {
    PatientEditDialog(
      patient = patient,
      templates = templates,
      onDismiss = { showEditPatientDialog = false },
      onSave = { updated ->
        coroutineScope.launch {
          repository.updatePatient(currentUserId, updated)
          showEditPatientDialog = false
          Toast.makeText(context, "Ficha actualizada", Toast.LENGTH_SHORT).show()
        }
      }
    )
  }

  if (showNewSessionDialog && patient != null) {
    RecordSessionDialog(
      patient = patient,
      onDismiss = { showNewSessionDialog = false },
      onSaveSession = { session ->
        coroutineScope.launch {
          repository.addSession(currentUserId, session, autoUpdateDebt = true)
          showNewSessionDialog = false
          Toast.makeText(context, "¡Sesión registrada!", Toast.LENGTH_SHORT).show()
        }
      }
    )
  }

  if (showPaymentDialog && patient != null) {
    PaymentDialog(
      patient = patient,
      onDismiss = { showPaymentDialog = false },
      onSavePayment = { payment ->
        coroutineScope.launch {
          repository.addPayment(currentUserId, payment)
          showPaymentDialog = false
          Toast.makeText(context, "¡Pago registrado correctamente!", Toast.LENGTH_SHORT).show()
        }
      }
    )
  }

  if (sessionForAudioRecording != null) {
    RecordAudioDialog(
      sessionId = sessionForAudioRecording!!.id,
      onDismiss = { sessionForAudioRecording = null },
      onAudioRecorded = { path, duration ->
        val sess = sessionForAudioRecording!!
        coroutineScope.launch {
          repository.updateSession(
            currentUserId,
            sess.copy(audioPath = path, audioDurationSec = duration)
          )
          sessionForAudioRecording = null
          Toast.makeText(context, "Audio adjuntado a la sesión", Toast.LENGTH_SHORT).show()
        }
      }
    )
  }

  if (patient == null) {
    Scaffold(
      topBar = {
        TopAppBar(
          title = { Text("Paciente") },
          navigationIcon = {
            IconButton(onClick = onNavigateBack) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
          }
        )
      }
    ) { padding ->
      Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Text("Cargando ficha del paciente...")
      }
    }
    return
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(patient.name, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(
              if (patient.active) "Paciente Activo" else "Paciente en Pausa / Alta",
              style = MaterialTheme.typography.labelSmall,
              color = if (patient.active) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("patient_detail_back_btn")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
          }
        },
        actions = {
          IconButton(onClick = { showEditPatientDialog = true }) {
            Icon(Icons.Default.Edit, contentDescription = "Editar ficha")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      // Top Patient Overview Header Card (ADHD visual summary)
      PatientHeaderCard(
        patient = patient,
        config = reminderConfig,
        onSendReminder = {
          ReminderHelper.sendSessionReminder(context, reminderConfig, patient)
        },
        onSendDebtReminder = {
          ReminderHelper.sendDebtReminder(context, reminderConfig, patient)
        },
        onCallClick = {
          patient.phone?.let {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${it.trim()}"))
            context.startActivity(intent)
          }
        },
        onQuick1ClickSession = {
          coroutineScope.launch {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val session = Session(
              userId = currentUserId,
              patientId = patient.id,
              patientName = patient.name,
              date = todayStr,
              fee = patient.sessionFee,
              isPaid = false,
              notes = "Sesión realizada (1-clic)"
            )
            repository.addSession(currentUserId, session, autoUpdateDebt = true)
            Toast.makeText(context, "¡Sesión realizada (+1) registrada para ${patient.name}!", Toast.LENGTH_SHORT).show()
          }
        },
        onCobrarClick = {
          showPaymentDialog = true
        }
      )

      // Tabs: Ficha Técnica / Sesiones / Cobros
      SecondaryTabRow(selectedTabIndex = selectedTab) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = { Text("Ficha Técnica") }
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = { Text("Sesiones (${patientSessions.size})") }
        )
        Tab(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          text = { Text("Cobros (${patientPayments.size})") }
        )
      }

      // Tab Content
      when (selectedTab) {
        0 -> PatientClinicalSheetTab(
          patient = patient,
          templates = templates,
          onEdit = { showEditPatientDialog = true }
        )
        1 -> PatientSessionsTab(
          sessions = patientSessions,
          onNewSessionClick = { showNewSessionDialog = true },
          onAddAudioClick = { session -> sessionForAudioRecording = session }
        )
        2 -> PatientPaymentsTab(
          payments = patientPayments,
          debtAmount = patient.debtAmount,
          onNewPaymentClick = { showPaymentDialog = true }
        )
      }
    }
  }
}

@Composable
private fun PatientHeaderCard(
  patient: Patient,
  config: ReminderConfig,
  onSendReminder: () -> Unit,
  onSendDebtReminder: () -> Unit,
  onCallClick: () -> Unit,
  onQuick1ClickSession: () -> Unit,
  onCobrarClick: () -> Unit
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Schedule Info
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "${patient.dayOfWeek ?: "Sin día"} a las ${patient.sessionTime ?: "16:00"}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
          )
        }

        // Debt Traffic Light Badge
        val (badgeText, badgeBg, badgeFg) = when {
          patient.debtAmount <= 0.0 -> Triple("🟢 Al Día ($0)", StatusSuccessContainer, StatusSuccess)
          patient.debtAmount <= patient.sessionFee -> Triple("🟠 Debe 1 sesión ($${patient.debtAmount.toInt()})", StatusWarningContainer, StatusWarning)
          else -> Triple("🔴 Deuda: $${patient.debtAmount.toInt()}", StatusDangerContainer, StatusDanger)
        }

        Surface(shape = RoundedCornerShape(12.dp), color = badgeBg) {
          Text(
            text = badgeText,
            color = badgeFg,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }
      }

      // Fast Contact + Reminders + 1-Click Action Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Reminder button
        FilledTonalButton(
          onClick = if (patient.debtAmount > 0) onSendDebtReminder else onSendReminder,
          modifier = Modifier.weight(1.1f).testTag("send_patient_reminder_btn"),
          contentPadding = PaddingValues(horizontal = 6.dp)
        ) {
          Icon(
            if (patient.debtAmount > 0) Icons.Default.AttachMoney else Icons.Default.Notifications,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            if (patient.debtAmount > 0) "Recordar Pago" else "Recordar Sesión",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }

        if (!patient.phone.isNullOrBlank()) {
          IconButton(
            onClick = onCallClick,
            modifier = Modifier
              .size(38.dp)
              .background(MaterialTheme.colorScheme.surface, CircleShape)
          ) {
            Icon(Icons.Default.Call, contentDescription = "Llamar", tint = MaterialTheme.colorScheme.primary)
          }
        }

        // 1-Click +1 Sesión Realizada Button!
        Button(
          onClick = onQuick1ClickSession,
          modifier = Modifier.weight(1.2f).testTag("quick_add_session_detail_btn"),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
          Text("⚡ +1 Sesión", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        // Cobrar Button
        Button(
          onClick = onCobrarClick,
          modifier = Modifier.weight(1f).testTag("quick_cobrar_detail_btn"),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (patient.debtAmount > 0) StatusWarning else MaterialTheme.colorScheme.secondary
          ),
          contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
          Text("💰 Cobrar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PatientClinicalSheetTab(
  patient: Patient,
  templates: List<ClinicalTemplate>,
  onEdit: () -> Unit
) {
  val currentTemplate = templates.firstOrNull { it.id == patient.templateId }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // 1. Diagnóstico / Motivo
    item {
      ClinicalCard(
        title = "Motivo de Consulta / Diagnóstico",
        icon = Icons.Default.MedicalServices,
        content = patient.diagnosis ?: "Sin diagnóstico cargado. Toca editar para agregar."
      )
    }

    // 2. Dynamic Clinical Template Custom Fields (if template or custom fields exist)
    val customFields = patient.customFieldValues ?: emptyMap()
    if (customFields.isNotEmpty() || currentTemplate != null) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Text(
                text = currentTemplate?.name ?: "Campos Personalizados de la Ficha",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
              )
            }

            if (customFields.isEmpty()) {
              Text(
                "Esta ficha no tiene valores guardados para la plantilla. Toca editar para completarla.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            } else {
              customFields.forEach { (fieldId, value) ->
                val fieldMeta = currentTemplate?.parsedFields()?.firstOrNull { it.id == fieldId }
                val label = fieldMeta?.label ?: fieldId

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                  if (value.contains(",")) {
                    FlowRow(
                      horizontalArrangement = Arrangement.spacedBy(4.dp),
                      verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                      value.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { opt ->
                        SuggestionChip(
                          onClick = {},
                          label = { Text(opt, fontSize = 11.sp) }
                        )
                      }
                    }
                  } else {
                    Text(
                      text = value.ifBlank { "Sin especificar" },
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // 3. Ficha Técnica y Antecedentes
    item {
      ClinicalCard(
        title = "Ficha Técnica y Antecedentes Generales",
        icon = Icons.Default.Notes,
        content = patient.notes ?: "Sin notas clínicas previas cargadas."
      )
    }

    // 4. Datos Administrativos
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Datos Administrativos", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
          InfoRow(label = "Honorario acordado:", value = "$${patient.sessionFee.toInt()} por sesión")
          InfoRow(label = "Sesiones realizadas:", value = "${patient.totalSessions} sesiones")
          InfoRow(label = "Teléfono:", value = patient.phone ?: "No registrado")
          InfoRow(label = "Email:", value = patient.email ?: "No registrado")
          InfoRow(label = "Contacto de emergencia:", value = patient.emergencyContact ?: "No registrado")
        }
      }
    }

    // 5. Botón de Edición
    item {
      OutlinedButton(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth().testTag("edit_clinical_sheet_btn")
      ) {
        Icon(Icons.Default.Edit, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Editar Datos de la Ficha")
      }
    }
  }
}

@Composable
private fun ClinicalCard(
  title: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  content: String
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
      }
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = content,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}

@Composable
private fun InfoRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
  }
}

@Composable
private fun PatientSessionsTab(
  sessions: List<Session>,
  onNewSessionClick: () -> Unit,
  onAddAudioClick: (Session) -> Unit
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Historial de Sesiones", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Button(
          onClick = onNewSessionClick,
          modifier = Modifier.testTag("new_detailed_session_btn"),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Sesión con Notas")
        }
      }
    }

    if (sessions.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
          Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text("No hay sesiones registradas", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              "Puedes registrarlas en 1 clic desde arriba o con notas y audio.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    } else {
      items(sessions, key = { it.id }) { session ->
        SessionDetailCard(
          session = session,
          onAddAudioClick = { onAddAudioClick(session) }
        )
      }
    }
  }
}

@Composable
private fun SessionDetailCard(
  session: Session,
  onAddAudioClick: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Sesión: ${session.date}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (session.isPaid) StatusSuccessContainer else StatusWarningContainer
        ) {
          Text(
            text = if (session.isPaid) "Pagada" else "Pendiente de cobro",
            color = if (session.isPaid) StatusSuccess else StatusWarning,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
          )
        }
      }

      if (!session.notes.isNullOrBlank()) {
        Text(session.notes, style = MaterialTheme.typography.bodyMedium)
      }

      // Audio Section (Playing or recording)
      if (!session.audioPath.isNullOrBlank()) {
        SessionAudioPlayer(
          audioPath = session.audioPath,
          audioDurationSec = session.audioDurationSec
        )
      } else {
        OutlinedButton(
          onClick = onAddAudioClick,
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          modifier = Modifier.align(Alignment.End).testTag("add_audio_session_${session.id}")
        ) {
          Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Grabar Audio de Sesión", fontSize = 12.sp)
        }
      }
    }
  }
}

@Composable
private fun PatientPaymentsTab(
  payments: List<Payment>,
  debtAmount: Double,
  onNewPaymentClick: () -> Unit
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("Control de Pagos", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
          Text(
            text = if (debtAmount > 0) "Deuda pendiente: $${debtAmount.toInt()}" else "Cuenta al día (Sin deudas)",
            style = MaterialTheme.typography.bodySmall,
            color = if (debtAmount > 0) StatusDanger else StatusSuccess,
            fontWeight = FontWeight.SemiBold
          )
        }
        Button(
          onClick = onNewPaymentClick,
          modifier = Modifier.testTag("new_payment_record_btn"),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (debtAmount > 0) StatusWarning else MaterialTheme.colorScheme.primary
          )
        ) {
          Icon(Icons.Default.AttachMoney, contentDescription = null)
          Spacer(modifier = Modifier.width(4.dp))
          Text("Cobrar")
        }
      }
    }

    if (payments.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
          Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text("No hay pagos registrados aún", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              "Registra los honorarios cobrados para actualizar la deuda automáticamente.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    } else {
      items(payments, key = { it.id }) { payment ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "+$${payment.amount.toInt()}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = StatusSuccess
              )
              Text(
                text = "${payment.date} • ${payment.paymentMethod}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              if (!payment.notes.isNullOrBlank()) {
                Text(payment.notes, style = MaterialTheme.typography.bodySmall)
              }
            }
            Icon(Icons.Default.Check, contentDescription = null, tint = StatusSuccess)
          }
        }
      }
    }
  }
}
