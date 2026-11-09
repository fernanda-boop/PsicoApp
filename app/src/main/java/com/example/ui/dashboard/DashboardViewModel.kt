package com.example.ui.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ClinicalTemplate
import com.example.data.model.Patient
import com.example.data.model.Payment
import com.example.data.model.ReminderConfig
import com.example.data.model.Session
import com.example.data.repository.PsychologistRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

enum class DashboardFilter {
  ALL,
  TODAY,
  WITH_DEBT,
  ACTIVE
}

class DashboardViewModel(
  private val repository: PsychologistRepository,
  val currentUserId: String
) : ViewModel() {

  val searchQuery = MutableStateFlow("")
  val selectedFilter = MutableStateFlow(DashboardFilter.ALL)

  val patientsState: StateFlow<UiState<List<Patient>>> = repository.observePatients(currentUserId)
    .map<List<Patient>, UiState<List<Patient>>> { UiState.Success(it) }
    .catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      val rootError = firestoreError ?: error
      Log.w(TAG, "Error observing patients", rootError)
      emit(UiState.Error(rootError.message ?: "Error al cargar pacientes"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = UiState.Loading
    )

  val sessionsState: StateFlow<UiState<List<Session>>> = repository.observeSessions(currentUserId)
    .map<List<Session>, UiState<List<Session>>> { UiState.Success(it) }
    .catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      emit(UiState.Error(firestoreError?.message ?: "Error al cargar sesiones"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = UiState.Loading
    )

  val paymentsState: StateFlow<UiState<List<Payment>>> = repository.observePayments(currentUserId)
    .map<List<Payment>, UiState<List<Payment>>> { UiState.Success(it) }
    .catch { error ->
      val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      emit(UiState.Error(firestoreError?.message ?: "Error al cargar pagos"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = UiState.Loading
    )

  val templatesState: StateFlow<List<ClinicalTemplate>> = repository.observeTemplates(currentUserId)
    .catch { error ->
      Log.w(TAG, "Error observing templates", error)
      emit(emptyList())
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = emptyList()
    )

  val reminderConfigState: StateFlow<ReminderConfig> = repository.observeReminderConfig(currentUserId)
    .catch { error ->
      Log.w(TAG, "Error observing reminder config", error)
      emit(ReminderConfig(userId = currentUserId))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = ReminderConfig(userId = currentUserId)
    )

  private val _actionFeedback = MutableStateFlow<String?>(null)
  val actionFeedback: StateFlow<String?> = _actionFeedback.asStateFlow()

  fun clearFeedback() {
    _actionFeedback.value = null
  }

  fun getCurrentDayNameSpanish(): String {
    val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    return when (dayOfWeek) {
      Calendar.MONDAY -> "Lunes"
      Calendar.TUESDAY -> "Martes"
      Calendar.WEDNESDAY -> "Miércoles"
      Calendar.THURSDAY -> "Jueves"
      Calendar.FRIDAY -> "Viernes"
      Calendar.SATURDAY -> "Sábado"
      else -> "Domingo"
    }
  }

  /**
   * 1-Click Fast Action:
   * Adds a completed session for today, increments patient total sessions and updates debt.
   */
  fun quickRecordSession(patient: Patient, onCompleted: (Boolean) -> Unit = {}) {
    viewModelScope.launch {
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
      val result = repository.addSession(currentUserId, session, autoUpdateDebt = true)
      if (result.isSuccess) {
        _actionFeedback.value = "¡Sesión registrada para ${patient.name} (+1)!"
        onCompleted(true)
      } else {
        _actionFeedback.value = "Error al registrar la sesión"
        onCompleted(false)
      }
    }
  }

  fun createPatient(patient: Patient, onCompleted: (Boolean) -> Unit = {}) {
    viewModelScope.launch {
      val result = repository.addPatient(currentUserId, patient.copy(userId = currentUserId))
      onCompleted(result.isSuccess)
    }
  }

  fun updatePatient(patient: Patient, onCompleted: (Boolean) -> Unit = {}) {
    viewModelScope.launch {
      val result = repository.updatePatient(currentUserId, patient)
      onCompleted(result.isSuccess)
    }
  }

  fun recordPayment(payment: Payment, onCompleted: (Boolean) -> Unit = {}) {
    viewModelScope.launch {
      val result = repository.addPayment(currentUserId, payment.copy(userId = currentUserId))
      if (result.isSuccess) {
        _actionFeedback.value = "¡Pago de $${payment.amount.toInt()} registrado!"
        onCompleted(true)
      } else {
        _actionFeedback.value = "Error al guardar el pago"
        onCompleted(false)
      }
    }
  }

  fun saveTemplate(template: ClinicalTemplate) {
    viewModelScope.launch {
      val result = if (template.id.isBlank()) {
        repository.addTemplate(currentUserId, template.copy(userId = currentUserId))
      } else {
        repository.updateTemplate(currentUserId, template.copy(userId = currentUserId))
      }
      if (result.isSuccess) {
        _actionFeedback.value = "Plantilla guardada con éxito"
      } else {
        _actionFeedback.value = "Error al guardar la plantilla"
      }
    }
  }

  fun deleteTemplate(templateId: String) {
    viewModelScope.launch {
      val result = repository.deleteTemplate(currentUserId, templateId)
      if (result.isSuccess) {
        _actionFeedback.value = "Plantilla eliminada"
      }
    }
  }

  fun saveReminderConfig(config: ReminderConfig) {
    viewModelScope.launch {
      val result = repository.saveReminderConfig(currentUserId, config.copy(userId = currentUserId))
      if (result.isSuccess) {
        _actionFeedback.value = "Configuración de recordatorios guardada"
      } else {
        _actionFeedback.value = "Error al guardar recordatorios"
      }
    }
  }

  private companion object {
    const val TAG = "DashboardViewModel"
  }
}
