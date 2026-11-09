package com.example.ui.calendar

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
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
import com.example.data.model.Patient
import com.example.data.model.Session
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.dashboard.UiState
import com.example.ui.dialogs.PaymentDialog
import com.example.ui.dialogs.RecordAudioDialog
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerContainer
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessContainer
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningContainer

enum class CalendarViewMode {
  WEEK,
  DAY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
  viewModel: DashboardViewModel,
  onPatientSelected: (String) -> Unit
) {
  val context = LocalContext.current

  val patientsState by viewModel.patientsState.collectAsStateWithLifecycle()
  val sessionsState by viewModel.sessionsState.collectAsStateWithLifecycle()

  val allPatients = when (val state = patientsState) {
    is UiState.Success -> state.data
    else -> emptyList()
  }

  val allSessions = when (val state = sessionsState) {
    is UiState.Success -> state.data
    else -> emptyList()
  }

  var weekOffset by remember { mutableIntStateOf(0) }
  var viewMode by remember { mutableStateOf(CalendarViewMode.WEEK) }

  val weekDays = remember(weekOffset) {
    CalendarHelper.getDaysOfWeek(weekOffset)
  }

  val todayDateStr = remember { CalendarHelper.getTodayDateStr() }

  // Selected day defaults to today if within week, otherwise first day of week
  var selectedDayStr by remember(weekOffset) {
    val foundToday = weekDays.firstOrNull { it.isToday }
    mutableStateOf(foundToday?.dateStr ?: weekDays.first().dateStr)
  }

  val selectedDayInfo = remember(weekDays, selectedDayStr) {
    weekDays.firstOrNull { it.dateStr == selectedDayStr } ?: weekDays.first()
  }

  // Quick 1-touch session dialog
  var dateForQuickSession by remember { mutableStateOf<String?>(null) }
  var patientToPay by remember { mutableStateOf<Patient?>(null) }
  var sessionForAudio by remember { mutableStateOf<Session?>(null) }

  if (dateForQuickSession != null) {
    val displayDate = CalendarHelper.formatDisplayDay(dateForQuickSession!!)
    QuickAddSessionDialog(
      targetDateStr = dateForQuickSession!!,
      displayDate = displayDate,
      patients = allPatients,
      onDismiss = { dateForQuickSession = null },
      onSaveSession = { patient, notes ->
        viewModel.quickRecordSessionForDate(patient, dateForQuickSession!!, notes) { success ->
          if (success) {
            dateForQuickSession = null
            Toast.makeText(context, "¡Sesión registrada para ${patient.name}!", Toast.LENGTH_SHORT).show()
          }
        }
      }
    )
  }

  if (patientToPay != null) {
    PaymentDialog(
      patient = patientToPay!!,
      onDismiss = { patientToPay = null },
      onSavePayment = { payment ->
        viewModel.recordPayment(payment) { success ->
          if (success) patientToPay = null
        }
      }
    )
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("Calendario de Sesiones", fontWeight = FontWeight.Bold)
            Text(
              CalendarHelper.formatMonthYear(selectedDayInfo.date),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary
            )
          }
        },
        actions = {
          // View Mode Selector (Semana / Día)
          Row(
            modifier = Modifier.padding(end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            FilterChip(
              selected = viewMode == CalendarViewMode.WEEK,
              onClick = { viewMode = CalendarViewMode.WEEK },
              label = { Text("Semana") },
              modifier = Modifier.testTag("calendar_mode_week_chip")
            )
            FilterChip(
              selected = viewMode == CalendarViewMode.DAY,
              onClick = { viewMode = CalendarViewMode.DAY },
              label = { Text("Día") },
              modifier = Modifier.testTag("calendar_mode_day_chip")
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = { dateForQuickSession = selectedDayStr },
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text("Añadir Sesión", fontWeight = FontWeight.Bold) },
        modifier = Modifier.testTag("calendar_quick_add_fab")
      )
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      // 1. Navigation Bar (< Anterior | Hoy | Siguiente >)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedButton(
          onClick = {
            if (viewMode == CalendarViewMode.WEEK) {
              weekOffset -= 1
            } else {
              // Shift by 1 day
              val currentIndex = weekDays.indexOfFirst { it.dateStr == selectedDayStr }
              if (currentIndex > 0) {
                selectedDayStr = weekDays[currentIndex - 1].dateStr
              } else {
                weekOffset -= 1
              }
            }
          },
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
          modifier = Modifier.testTag("calendar_prev_button")
        ) {
          Icon(Icons.Default.ChevronLeft, contentDescription = null)
          Spacer(Modifier.width(2.dp))
          Text(if (viewMode == CalendarViewMode.WEEK) "Semana ant." else "Día ant.", fontSize = 12.sp)
        }

        FilledTonalButton(
          onClick = {
            weekOffset = 0
            selectedDayStr = todayDateStr
          },
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
          modifier = Modifier.testTag("calendar_today_button")
        ) {
          Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("Hoy", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        OutlinedButton(
          onClick = {
            if (viewMode == CalendarViewMode.WEEK) {
              weekOffset += 1
            } else {
              val currentIndex = weekDays.indexOfFirst { it.dateStr == selectedDayStr }
              if (currentIndex >= 0 && currentIndex < weekDays.size - 1) {
                selectedDayStr = weekDays[currentIndex + 1].dateStr
              } else {
                weekOffset += 1
              }
            }
          },
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
          modifier = Modifier.testTag("calendar_next_button")
        ) {
          Text(if (viewMode == CalendarViewMode.WEEK) "Sig. semana" else "Sig. día", fontSize = 12.sp)
          Spacer(Modifier.width(2.dp))
          Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
      }

      // 2. 7-Day Interactive Week Strip
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        weekDays.forEach { dayInfo ->
          val isSelected = (dayInfo.dateStr == selectedDayStr)

          // Check if there are sessions or scheduled patients on this date
          val sessionsCount = allSessions.count { it.date == dayInfo.dateStr }
          val scheduledCount = allPatients.count { it.active && it.dayOfWeek.equals(dayInfo.dayNameSpanish, ignoreCase = true) }
          val hasActivity = sessionsCount > 0 || scheduledCount > 0

          Column(
            modifier = Modifier
              .weight(1f)
              .padding(horizontal = 2.dp)
              .background(
                color = when {
                  isSelected -> MaterialTheme.colorScheme.primaryContainer
                  dayInfo.isToday -> MaterialTheme.colorScheme.surfaceVariant
                  else -> Color.Transparent
                },
                shape = RoundedCornerShape(12.dp)
              )
              .border(
                width = if (dayInfo.isToday && !isSelected) 1.5.dp else 0.dp,
                color = if (dayInfo.isToday && !isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
              )
              .clickable {
                selectedDayStr = dayInfo.dateStr
              }
              .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = dayInfo.dayAbbrSpanish,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = if (isSelected || dayInfo.isToday) FontWeight.Bold else FontWeight.Medium,
              color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
              text = dayInfo.dayNumber.toString(),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = if (isSelected || dayInfo.isToday) FontWeight.ExtraBold else FontWeight.Bold,
              color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            // Activity indicator dot
            Box(
              modifier = Modifier
                .size(6.dp)
                .background(
                  color = when {
                    sessionsCount > 0 -> StatusSuccess
                    scheduledCount > 0 -> MaterialTheme.colorScheme.primary
                    else -> Color.Transparent
                  },
                  shape = CircleShape
                )
            )
          }
        }
      }

      HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

      // 3. Main Calendar View Body
      if (viewMode == CalendarViewMode.WEEK) {
        // --- VISTA SEMANA ---
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
          contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          weekDays.forEach { dayInfo ->
            item(key = dayInfo.dateStr) {
              WeekDayCard(
                dayInfo = dayInfo,
                patients = allPatients,
                sessions = allSessions.filter { it.date == dayInfo.dateStr },
                onAddSessionClick = { dateForQuickSession = dayInfo.dateStr },
                on1ClickRecord = { patient ->
                  viewModel.quickRecordSessionForDate(patient, dayInfo.dateStr)
                },
                onPatientClick = onPatientSelected
              )
            }
          }
        }
      } else {
        // --- VISTA DÍA ---
        DayDetailedView(
          dayInfo = selectedDayInfo,
          patients = allPatients,
          sessions = allSessions.filter { it.date == selectedDayInfo.dateStr },
          onAddSessionClick = { dateForQuickSession = selectedDayInfo.dateStr },
          on1ClickRecord = { patient ->
            viewModel.quickRecordSessionForDate(patient, selectedDayInfo.dateStr)
          },
          onPatientClick = onPatientSelected,
          onCobrarClick = { patient -> patientToPay = patient }
        )
      }
    }
  }
}

@Composable
private fun WeekDayCard(
  dayInfo: CalendarDayInfo,
  patients: List<Patient>,
  sessions: List<Session>,
  onAddSessionClick: () -> Unit,
  on1ClickRecord: (Patient) -> Unit,
  onPatientClick: (String) -> Unit
) {
  // Scheduled patients for this day from their profile
  val scheduledPatients = remember(patients, dayInfo.dayNameSpanish) {
    patients.filter { it.active && it.dayOfWeek.equals(dayInfo.dayNameSpanish, ignoreCase = true) }
  }

  // Those who haven't had a session recorded yet for this date
  val recordedPatientIds = sessions.map { it.patientId }.toSet()
  val pendingScheduled = scheduledPatients.filter { !recordedPatientIds.contains(it.id) }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (dayInfo.isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
      else MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      // Day Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "${dayInfo.dayNameSpanish} ${dayInfo.dayNumber}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (dayInfo.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
          )
          if (dayInfo.isToday) {
            Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(6.dp)) {
              Text(
                "HOY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }

        // Quick 1-touch + button for this day
        FilledTonalButton(
          onClick = onAddSessionClick,
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
          modifier = Modifier.testTag("add_session_day_${dayInfo.dateStr}")
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("Añadir", fontSize = 12.sp)
        }
      }

      // Recorded sessions
      if (sessions.isNotEmpty()) {
        sessions.forEach { session ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
              .clickable { onPatientClick(session.patientId) }
              .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(8.dp))
              Column {
                Text(session.patientName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(
                  text = "$${session.fee.toInt()} • ${if (session.isPaid) "Pagada" else "Pendiente de cobro"}",
                  style = MaterialTheme.typography.bodySmall,
                  color = if (session.isPaid) StatusSuccess else StatusWarning,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }

            if (!session.audioPath.isNullOrBlank()) {
              Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                Icon(
                  Icons.Default.Mic,
                  contentDescription = "Con audio",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(4.dp).size(16.dp)
                )
              }
            }
          }
        }
      }

      // Scheduled patients pending 1-touch confirmation for this day
      if (pendingScheduled.isNotEmpty()) {
        pendingScheduled.forEach { patient ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
              .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f).clickable { onPatientClick(patient.id) }) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Text(
                  text = "${patient.sessionTime ?: "16:00"} • ${patient.name}",
                  fontWeight = FontWeight.SemiBold,
                  style = MaterialTheme.typography.bodyMedium
                )
              }
              Text(
                "Programada recurrente • $${patient.sessionFee.toInt()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            // 1-Touch confirm button!
            Button(
              onClick = { on1ClickRecord(patient) },
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
              modifier = Modifier.testTag("one_touch_confirm_${patient.id}_${dayInfo.dateStr}")
            ) {
              Text("⚡ Registrar", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
          }
        }
      }

      // Empty day state
      if (sessions.isEmpty() && pendingScheduled.isEmpty()) {
        Text(
          text = "Sin sesiones programadas para este día",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
          modifier = Modifier.padding(vertical = 4.dp)
        )
      }
    }
  }
}

@Composable
private fun DayDetailedView(
  dayInfo: CalendarDayInfo,
  patients: List<Patient>,
  sessions: List<Session>,
  onAddSessionClick: () -> Unit,
  on1ClickRecord: (Patient) -> Unit,
  onPatientClick: (String) -> Unit,
  onCobrarClick: (Patient) -> Unit
) {
  val scheduledPatients = remember(patients, dayInfo.dayNameSpanish) {
    patients.filter { it.active && it.dayOfWeek.equals(dayInfo.dayNameSpanish, ignoreCase = true) }
  }

  val recordedPatientIds = sessions.map { it.patientId }.toSet()
  val pendingScheduled = scheduledPatients.filter { !recordedPatientIds.contains(it.id) }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 10.dp, bottom = 88.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Day Banner
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = CalendarHelper.formatDisplayDay(dayInfo.dateStr),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "${sessions.size} sesiones realizadas • ${pendingScheduled.size} programadas por realizar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Button(
            onClick = onAddSessionClick,
            modifier = Modifier.fillMaxWidth().testTag("day_view_add_session_btn")
          ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Añadir Sesión a este Día con 1 Toque")
          }
        }
      }
    }

    // Pending scheduled patients
    if (pendingScheduled.isNotEmpty()) {
      item {
        Text(
          text = "Citas Programadas de Hoy (${pendingScheduled.size})",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
      }

      items(pendingScheduled, key = { "sched_${it.id}" }) { patient ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f).clickable { onPatientClick(patient.id) }) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(patient.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
              }
              Spacer(Modifier.height(4.dp))
              Text("⏰ Horario: ${patient.sessionTime ?: "16:00"} • Honorario: $${patient.sessionFee.toInt()}", style = MaterialTheme.typography.bodySmall)
              if (!patient.diagnosis.isNullOrBlank()) {
                Text("🎯 ${patient.diagnosis}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }

            // 1-Touch record button!
            Button(
              onClick = { on1ClickRecord(patient) },
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              modifier = Modifier.testTag("day_record_btn_${patient.id}")
            ) {
              Text("⚡ Registrar (+1)")
            }
          }
        }
      }
    }

    // Recorded sessions
    if (sessions.isNotEmpty()) {
      item {
        Text(
          text = "Sesiones Realizadas (${sessions.size})",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = StatusSuccess
        )
      }

      items(sessions, key = { "rec_${it.id}" }) { session ->
        val patient = patients.firstOrNull { it.id == session.patientId }

        Card(
          modifier = Modifier.fillMaxWidth().clickable { onPatientClick(session.patientId) },
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess)
                Text(session.patientName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
              }

              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (session.isPaid) StatusSuccessContainer else StatusWarningContainer
              ) {
                Text(
                  text = if (session.isPaid) "Pagada ($${session.fee.toInt()})" else "Pendiente ($${session.fee.toInt()})",
                  color = if (session.isPaid) StatusSuccess else StatusWarning,
                  fontWeight = FontWeight.Bold,
                  style = MaterialTheme.typography.labelSmall,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
              }
            }

            if (!session.notes.isNullOrBlank()) {
              Text(session.notes, style = MaterialTheme.typography.bodyMedium)
            }

            if (!session.audioPath.isNullOrBlank()) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text("Audio de sesión grabado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
              }
            }

            if (!session.isPaid && patient != null) {
              Button(
                onClick = { onCobrarClick(patient) },
                colors = ButtonDefaults.buttonColors(containerColor = StatusWarning),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.align(Alignment.End)
              ) {
                Text("💰 Cobrar Sesión", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    if (sessions.isEmpty() && pendingScheduled.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text("Día sin citas programadas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
              "Añade una sesión con un solo toque usando el botón de arriba.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }
}
