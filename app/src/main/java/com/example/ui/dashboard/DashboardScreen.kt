package com.example.ui.dashboard

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ui.dialogs.PatientEditDialog
import com.example.ui.dialogs.PaymentDialog
import com.example.ui.reminders.ReminderHelper
import com.example.ui.reminders.ReminderSettingsDialog
import com.example.ui.templates.TemplateManagerDialog
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerContainer
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessContainer
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
  viewModel: DashboardViewModel,
  onPatientSelected: (String) -> Unit,
  onSignOut: () -> Unit
) {
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }

  val patientsState by viewModel.patientsState.collectAsStateWithLifecycle()
  val sessionsState by viewModel.sessionsState.collectAsStateWithLifecycle()
  val paymentsState by viewModel.paymentsState.collectAsStateWithLifecycle()
  val templates by viewModel.templatesState.collectAsStateWithLifecycle()
  val reminderConfig by viewModel.reminderConfigState.collectAsStateWithLifecycle()

  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
  val actionFeedback by viewModel.actionFeedback.collectAsStateWithLifecycle()

  var showNewPatientDialog by remember { mutableStateOf(false) }
  var showTemplateManagerDialog by remember { mutableStateOf(false) }
  var showReminderSettingsDialog by remember { mutableStateOf(false) }
  var patientToPay by remember { mutableStateOf<Patient?>(null) }

  LaunchedEffect(actionFeedback) {
    actionFeedback?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearFeedback()
    }
  }

  if (showNewPatientDialog) {
    PatientEditDialog(
      patient = null,
      templates = templates,
      onDismiss = { showNewPatientDialog = false },
      onSave = { newPatient ->
        viewModel.createPatient(newPatient) { success ->
          if (success) {
            showNewPatientDialog = false
            Toast.makeText(context, "Paciente agregado exitosamente", Toast.LENGTH_SHORT).show()
          } else {
            Toast.makeText(context, "Error al crear paciente", Toast.LENGTH_SHORT).show()
          }
        }
      }
    )
  }

  if (showTemplateManagerDialog) {
    TemplateManagerDialog(
      templates = templates,
      onDismiss = { showTemplateManagerDialog = false },
      onSaveTemplate = { viewModel.saveTemplate(it) },
      onDeleteTemplate = { viewModel.deleteTemplate(it) }
    )
  }

  if (showReminderSettingsDialog) {
    ReminderSettingsDialog(
      currentConfig = reminderConfig,
      onDismiss = { showReminderSettingsDialog = false },
      onSave = { updated ->
        viewModel.saveReminderConfig(updated)
        showReminderSettingsDialog = false
      }
    )
  }

  if (patientToPay != null) {
    PaymentDialog(
      patient = patientToPay!!,
      onDismiss = { patientToPay = null },
      onSavePayment = { payment ->
        viewModel.recordPayment(payment) { success ->
          if (success) {
            patientToPay = null
          }
        }
      }
    )
  }

  val allPatients = when (val state = patientsState) {
    is UiState.Success -> state.data
    else -> emptyList()
  }

  val allSessions = when (val state = sessionsState) {
    is UiState.Success -> state.data
    else -> emptyList()
  }

  val allPayments = when (val state = paymentsState) {
    is UiState.Success -> state.data
    else -> emptyList()
  }

  val currentMonth = remember {
    SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
  }
  val currentDayName = remember { viewModel.getCurrentDayNameSpanish() }

  val filteredPatients = remember(allPatients, searchQuery, selectedFilter, currentDayName) {
    allPatients.filter { patient ->
      val matchesQuery = searchQuery.isBlank() ||
        patient.name.contains(searchQuery, ignoreCase = true) ||
        (patient.diagnosis?.contains(searchQuery, ignoreCase = true) == true) ||
        (patient.dayOfWeek?.contains(searchQuery, ignoreCase = true) == true)

      val matchesFilter = when (selectedFilter) {
        DashboardFilter.ALL -> true
        DashboardFilter.TODAY -> patient.dayOfWeek?.equals(currentDayName, ignoreCase = true) == true
        DashboardFilter.WITH_DEBT -> patient.debtAmount > 0.0
        DashboardFilter.ACTIVE -> patient.active
      }

      matchesQuery && matchesFilter
    }
  }

  val totalPendingDebt = remember(allPatients) {
    allPatients.sumOf { it.debtAmount }
  }

  val monthIncome = remember(allPayments, currentMonth) {
    allPayments.filter { it.date.startsWith(currentMonth) }.sumOf { it.amount }
  }

  val monthSessions = remember(allSessions, currentMonth) {
    allSessions.count { it.date.startsWith(currentMonth) }
  }

  val todayPatients = remember(allPatients, currentDayName) {
    allPatients.filter { it.active && it.dayOfWeek?.equals(currentDayName, ignoreCase = true) == true }
  }
  val todayCount = todayPatients.size

  val debtorPatients = remember(allPatients) {
    allPatients.filter { it.debtAmount > 0 }
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("PsicoGestión", fontWeight = FontWeight.Bold)
            Text(
              "Organización sin estrés • Enfoque TDAH",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary
            )
          }
        },
        actions = {
          IconButton(
            onClick = { showTemplateManagerDialog = true },
            modifier = Modifier.testTag("open_templates_button")
          ) {
            Icon(Icons.Default.Description, contentDescription = "Plantillas de Ficha")
          }
          IconButton(
            onClick = { showReminderSettingsDialog = true },
            modifier = Modifier.testTag("open_reminders_button")
          ) {
            Icon(Icons.Default.Notifications, contentDescription = "Configurar Recordatorios")
          }
          IconButton(
            onClick = onSignOut,
            modifier = Modifier.testTag("sign_out_button")
          ) {
            Icon(Icons.Default.Logout, contentDescription = "Cerrar sesión")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = { showNewPatientDialog = true },
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text("Nuevo Paciente", fontWeight = FontWeight.Bold) },
        modifier = Modifier.testTag("add_patient_fab")
      )
    }
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      contentPadding = PaddingValues(bottom = 88.dp)
    ) {
      // 1. ADHD Clean Financial & Today Summary
      item {
        MetricsHeaderSection(
          monthIncome = monthIncome,
          totalPendingDebt = totalPendingDebt,
          monthSessions = monthSessions,
          todayCount = todayCount,
          currentDayName = currentDayName
        )
      }

      // 2. ADHD High-Priority Debt Alert Card (Requested specifically for pending debt reminders)
      if (reminderConfig.notifyMePendingDebts && debtorPatients.isNotEmpty()) {
        item {
          PendingDebtsAlertCard(
            debtors = debtorPatients,
            config = reminderConfig,
            onSendReminder = { debtor ->
              ReminderHelper.sendDebtReminder(context, reminderConfig, debtor)
            },
            onCobrar = { debtor ->
              patientToPay = debtor
            }
          )
        }
      }

      // 3. Quick Upcoming Session Reminders (today's agenda)
      if (todayPatients.isNotEmpty()) {
        item {
          TodaySessionsReminderCard(
            todayPatients = todayPatients,
            config = reminderConfig,
            currentDayName = currentDayName,
            onSendReminder = { patient ->
              ReminderHelper.sendSessionReminder(context, reminderConfig, patient)
            },
            on1ClickSession = { patient ->
              viewModel.quickRecordSession(patient)
            }
          )
        }
      }

      // 4. Search & Fast Filter Chips
      item {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = { Text("Buscar por nombre, día o diagnóstico...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
              if (searchQuery.isNotBlank()) {
                IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                  Icon(Icons.Default.Clear, contentDescription = "Borrar búsqueda")
                }
              }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("search_patient_input")
          )

          Spacer(modifier = Modifier.height(10.dp))

          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            item {
              FilterChip(
                selected = selectedFilter == DashboardFilter.ALL,
                onClick = { viewModel.selectedFilter.value = DashboardFilter.ALL },
                label = { Text("Todos (${allPatients.size})") }
              )
            }
            item {
              FilterChip(
                selected = selectedFilter == DashboardFilter.TODAY,
                onClick = { viewModel.selectedFilter.value = DashboardFilter.TODAY },
                label = { Text("Hoy: $currentDayName ($todayCount)") },
                modifier = Modifier.testTag("filter_today_chip")
              )
            }
            item {
              val debtCount = allPatients.count { it.debtAmount > 0 }
              FilterChip(
                selected = selectedFilter == DashboardFilter.WITH_DEBT,
                onClick = { viewModel.selectedFilter.value = DashboardFilter.WITH_DEBT },
                label = { Text("Con Deuda ($debtCount)") },
                modifier = Modifier.testTag("filter_debt_chip")
              )
            }
            item {
              val activeCount = allPatients.count { it.active }
              FilterChip(
                selected = selectedFilter == DashboardFilter.ACTIVE,
                onClick = { viewModel.selectedFilter.value = DashboardFilter.ACTIVE },
                label = { Text("Activos ($activeCount)") }
              )
            }
          }
        }
      }

      // 5. Patient List Section
      if (patientsState is UiState.Loading) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(200.dp),
            contentAlignment = Alignment.Center
          ) {
            CircularProgressIndicator()
          }
        }
      } else if (filteredPatients.isEmpty()) {
        item {
          EmptyPatientsCard(
            hasSearch = searchQuery.isNotBlank() || selectedFilter != DashboardFilter.ALL,
            onAddClick = { showNewPatientDialog = true }
          )
        }
      } else {
        items(filteredPatients, key = { it.id }) { patient ->
          PatientItemCard(
            patient = patient,
            onClick = { onPatientSelected(patient.id) },
            on1ClickSession = {
              viewModel.quickRecordSession(patient)
            },
            onCobrarClick = {
              patientToPay = patient
            }
          )
        }
      }
    }
  }
}

@Composable
private fun PendingDebtsAlertCard(
  debtors: List<Patient>,
  config: ReminderConfig,
  onSendReminder: (Patient) -> Unit,
  onCobrar: (Patient) -> Unit
) {
  val totalDebt = debtors.sumOf { it.debtAmount }
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = StatusWarningContainer.copy(alpha = 0.8f))
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(Icons.Default.Warning, contentDescription = null, tint = StatusWarning)
          Text(
            text = "Cobros Pendientes (${debtors.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = StatusWarning
          )
        }
        Text(
          text = "Total: $${totalDebt.toInt()}",
          fontWeight = FontWeight.ExtraBold,
          color = StatusWarning,
          style = MaterialTheme.typography.titleMedium
        )
      }

      debtors.take(3).forEach { debtor ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(debtor.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(
              "Debe: $${debtor.debtAmount.toInt()}",
              color = StatusDanger,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.SemiBold
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilledTonalButton(
              onClick = { onSendReminder(debtor) },
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
              Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(Modifier.width(4.dp))
              Text(config.debtReminderChannel.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Button(
              onClick = { onCobrar(debtor) },
              colors = ButtonDefaults.buttonColors(containerColor = StatusWarning),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
              Text("Cobrar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TodaySessionsReminderCard(
  todayPatients: List<Patient>,
  config: ReminderConfig,
  currentDayName: String,
  onSendReminder: (Patient) -> Unit,
  on1ClickSession: (Patient) -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(Icons.Default.EventNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Text(
            text = "Agenda de Hoy ($currentDayName)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }
        Text(
          text = "${todayPatients.size} pacientes",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold
        )
      }

      todayPatients.forEach { patient ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(patient.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(
              "⏰ ${patient.sessionTime ?: "Horario a convenir"}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
              onClick = { onSendReminder(patient) },
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
              Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(Modifier.width(4.dp))
              Text("Recordar", fontSize = 11.sp)
            }
            Button(
              onClick = { on1ClickSession(patient) },
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
              Text("⚡ +1", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MetricsHeaderSection(
  monthIncome: Double,
  totalPendingDebt: Double,
  monthSessions: Int,
  todayCount: Int,
  currentDayName: String
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Metric 1: Month Income
      MetricCard(
        title = "Cobrado este Mes",
        value = "$${monthIncome.toInt()}",
        caption = "$monthSessions sesiones",
        icon = Icons.Default.TrendingUp,
        accentColor = StatusSuccess,
        modifier = Modifier.weight(1f)
      )

      // Metric 2: Pending Debts (High visibility for ADHD)
      MetricCard(
        title = "Total por Cobrar",
        value = "$${totalPendingDebt.toInt()}",
        caption = if (totalPendingDebt > 0) "⚠️ Pagos pendientes" else "🎉 Todo al día",
        icon = Icons.Default.AttachMoney,
        accentColor = if (totalPendingDebt > 0) StatusDanger else StatusSuccess,
        modifier = Modifier.weight(1f)
      )
    }
  }
}

@Composable
private fun MetricCard(
  title: String,
  value: String,
  caption: String,
  icon: ImageVector,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accentColor,
          modifier = Modifier.size(18.dp)
        )
      }
      Text(
        text = value,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = accentColor
      )
      Text(
        text = caption,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun PatientItemCard(
  patient: Patient,
  onClick: () -> Unit,
  on1ClickSession: () -> Unit,
  onCobrarClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
      .clickable(onClick = onClick)
      .testTag("patient_card_${patient.id}"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Top Row: Patient Name & Debt Traffic-Light Pill
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(42.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = patient.name.firstOrNull()?.uppercase() ?: "?",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 18.sp
              )
            }
          }

          Spacer(modifier = Modifier.width(10.dp))

          Column {
            Text(
              text = patient.name,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1
            )
            Text(
              text = "🗓️ ${patient.dayOfWeek ?: "Sin día"} ${patient.sessionTime ?: "16:00"}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Debt Traffic Light Badge
        val (badgeText, badgeBg, badgeFg) = when {
          patient.debtAmount <= 0.0 -> Triple("🟢 Al Día", StatusSuccessContainer, StatusSuccess)
          patient.debtAmount <= patient.sessionFee -> Triple("🟠 1 Pendiente", StatusWarningContainer, StatusWarning)
          else -> Triple("🔴 Debe $${patient.debtAmount.toInt()}", StatusDangerContainer, StatusDanger)
        }

        Surface(shape = RoundedCornerShape(10.dp), color = badgeBg) {
          Text(
            text = badgeText,
            color = badgeFg,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      // Middle: Diagnosis / Quick note snippet
      if (!patient.diagnosis.isNullOrBlank()) {
        Text(
          text = "🎯 ${patient.diagnosis}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1
        )
      }

      // Bottom Row: Golden 1-Click Action Buttons!
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "$${patient.sessionFee.toInt()}/sesión • ${patient.totalSessions} hechas",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.weight(1f)
        )

        // 1-Click Session Button (The user specifically stressed this!)
        Button(
          onClick = on1ClickSession,
          modifier = Modifier.testTag("one_click_session_btn_${patient.id}"),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
          Text("⚡ +1 Sesión", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        // Quick Cobrar button
        Button(
          onClick = onCobrarClick,
          modifier = Modifier.testTag("cobrar_btn_${patient.id}"),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (patient.debtAmount > 0) StatusWarning else MaterialTheme.colorScheme.secondary
          ),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Text("💰 Cobrar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
      }
    }
  }
}

@Composable
private fun EmptyPatientsCard(
  hasSearch: Boolean,
  onAddClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(24.dp),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Text(
        text = if (hasSearch) "No se encontraron pacientes" else "¡Bienvenida a PsicoGestión!",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = if (hasSearch) {
          "Prueba con otro término de búsqueda o limpia los filtros."
        } else {
          "Empieza agregando tu primer paciente para registrar sesiones en 1 clic y controlar tus honorarios."
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
      )
      if (!hasSearch) {
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onAddClick) {
          Icon(Icons.Default.Add, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Crear Primer Paciente")
        }
      }
    }
  }
}
