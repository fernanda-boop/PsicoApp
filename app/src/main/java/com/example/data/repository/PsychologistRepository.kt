package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.error.OperationType
import com.example.data.error.handleFirestoreError
import com.example.data.model.ClinicalTemplate
import com.example.data.model.Patient
import com.example.data.model.Payment
import com.example.data.model.ReminderConfig
import com.example.data.model.Session
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PsychologistRepository(
  private val db: FirebaseFirestore
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  // --- PATIENTS ---

  fun observePatients(userId: String): Flow<List<Patient>> {
    return db.collection("users").document(userId).collection("patients")
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(Patient::class.java).sortedBy { it.name.lowercase() }
      }
  }

  suspend fun getPatient(userId: String, patientId: String): Patient? {
    val docRef = db.collection("users").document(userId).collection("patients").document(patientId)
    return try {
      val snap = docRef.get().await()
      snap.toObject(Patient::class.java)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      null
    }
  }

  suspend fun addPatient(userId: String, patient: Patient): Result<String> {
    val docRef = db.collection("users").document(userId).collection("patients").document()
    val payload = mutableMapOf<String, Any?>(
      "userId" to userId,
      "name" to patient.name,
      "phone" to patient.phone,
      "email" to patient.email,
      "sessionFee" to patient.sessionFee,
      "diagnosis" to patient.diagnosis,
      "notes" to patient.notes,
      "emergencyContact" to patient.emergencyContact,
      "dayOfWeek" to patient.dayOfWeek,
      "sessionTime" to patient.sessionTime,
      "active" to patient.active,
      "totalSessions" to patient.totalSessions,
      "debtAmount" to patient.debtAmount,
      "templateId" to patient.templateId,
      "customFieldValues" to patient.customFieldValues,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }

    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updatePatient(userId: String, patient: Patient): Result<Unit> {
    require(patient.id.isNotBlank()) { "Patient ID must not be blank" }
    val docRef = db.collection("users").document(userId).collection("patients").document(patient.id)
    val updates = mutableMapOf<String, Any?>(
      "name" to patient.name,
      "phone" to patient.phone,
      "email" to patient.email,
      "sessionFee" to patient.sessionFee,
      "diagnosis" to patient.diagnosis,
      "notes" to patient.notes,
      "emergencyContact" to patient.emergencyContact,
      "dayOfWeek" to patient.dayOfWeek,
      "sessionTime" to patient.sessionTime,
      "active" to patient.active,
      "totalSessions" to patient.totalSessions,
      "debtAmount" to patient.debtAmount,
      "templateId" to patient.templateId,
      "customFieldValues" to patient.customFieldValues,
      "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }

    return try {
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deletePatient(userId: String, patientId: String): Result<Unit> {
    val docRef = db.collection("users").document(userId).collection("patients").document(patientId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  // --- SESSIONS ---

  fun observeSessions(userId: String): Flow<List<Session>> {
    return db.collection("users").document(userId).collection("sessions")
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(Session::class.java).sortedByDescending { it.date }
      }
  }

  suspend fun addSession(userId: String, session: Session, autoUpdateDebt: Boolean = true): Result<String> {
    val docRef = db.collection("users").document(userId).collection("sessions").document()
    val payload = mutableMapOf<String, Any?>(
      "userId" to userId,
      "patientId" to session.patientId,
      "patientName" to session.patientName,
      "date" to session.date,
      "fee" to session.fee,
      "isPaid" to session.isPaid,
      "notes" to session.notes,
      "audioPath" to session.audioPath,
      "audioDurationSec" to session.audioDurationSec,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }

    return try {
      docRef.set(payload).await()

      if (autoUpdateDebt && session.patientId.isNotBlank()) {
        val patientRef = db.collection("users").document(userId).collection("patients").document(session.patientId)
        try {
          val patientSnap = patientRef.get().await()
          val currentTotal = patientSnap.getLong("totalSessions") ?: 0L
          val currentDebt = patientSnap.getDouble("debtAmount") ?: 0.0
          val additionalDebt = if (!session.isPaid) session.fee else 0.0
          patientRef.update(
            mapOf(
              "totalSessions" to currentTotal + 1L,
              "debtAmount" to (currentDebt + additionalDebt),
              "updatedAt" to FieldValue.serverTimestamp()
            )
          ).await()
        } catch (pe: Exception) {
          handleFirestoreError(pe, OperationType.UPDATE, patientRef.path)
        }
      }

      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateSession(userId: String, session: Session): Result<Unit> {
    require(session.id.isNotBlank()) { "Session ID must not be blank" }
    val docRef = db.collection("users").document(userId).collection("sessions").document(session.id)
    val updates = mutableMapOf<String, Any?>(
      "patientId" to session.patientId,
      "patientName" to session.patientName,
      "date" to session.date,
      "fee" to session.fee,
      "isPaid" to session.isPaid,
      "notes" to session.notes,
      "audioPath" to session.audioPath,
      "audioDurationSec" to session.audioDurationSec,
      "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }

    return try {
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteSession(userId: String, sessionId: String): Result<Unit> {
    val docRef = db.collection("users").document(userId).collection("sessions").document(sessionId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  // --- PAYMENTS ---

  fun observePayments(userId: String): Flow<List<Payment>> {
    return db.collection("users").document(userId).collection("payments")
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(Payment::class.java).sortedByDescending { it.date }
      }
  }

  suspend fun addPayment(userId: String, payment: Payment): Result<String> {
    val docRef = db.collection("users").document(userId).collection("payments").document()
    val payload = mutableMapOf<String, Any?>(
      "userId" to userId,
      "patientId" to payment.patientId,
      "patientName" to payment.patientName,
      "amount" to payment.amount,
      "date" to payment.date,
      "paymentMethod" to payment.paymentMethod,
      "notes" to payment.notes,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }

    return try {
      docRef.set(payload).await()

      if (payment.patientId.isNotBlank()) {
        val patientRef = db.collection("users").document(userId).collection("patients").document(payment.patientId)
        try {
          val patientSnap = patientRef.get().await()
          val currentDebt = patientSnap.getDouble("debtAmount") ?: 0.0
          val newDebt = maxOf(0.0, currentDebt - payment.amount)
          patientRef.update(
            mapOf(
              "debtAmount" to newDebt,
              "updatedAt" to FieldValue.serverTimestamp()
            )
          ).await()
        } catch (pe: Exception) {
          handleFirestoreError(pe, OperationType.UPDATE, patientRef.path)
        }
      }

      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  // --- CLINICAL TEMPLATES ---

  fun observeTemplates(userId: String): Flow<List<ClinicalTemplate>> {
    return db.collection("users").document(userId).collection("templates")
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(ClinicalTemplate::class.java).sortedBy { it.name.lowercase() }
      }
  }

  suspend fun getTemplate(userId: String, templateId: String): ClinicalTemplate? {
    val docRef = db.collection("users").document(userId).collection("templates").document(templateId)
    return try {
      val snap = docRef.get().await()
      snap.toObject(ClinicalTemplate::class.java)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      null
    }
  }

  suspend fun addTemplate(userId: String, template: ClinicalTemplate): Result<String> {
    val docRef = db.collection("users").document(userId).collection("templates").document()
    val payload = mutableMapOf<String, Any?>(
      "userId" to userId,
      "name" to template.name,
      "description" to template.description,
      "isDefault" to template.isDefault,
      "fields" to template.fields,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }

    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateTemplate(userId: String, template: ClinicalTemplate): Result<Unit> {
    require(template.id.isNotBlank()) { "Template ID must not be blank" }
    val docRef = db.collection("users").document(userId).collection("templates").document(template.id)
    val updates = mutableMapOf<String, Any?>(
      "name" to template.name,
      "description" to template.description,
      "isDefault" to template.isDefault,
      "fields" to template.fields,
      "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }

    return try {
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteTemplate(userId: String, templateId: String): Result<Unit> {
    val docRef = db.collection("users").document(userId).collection("templates").document(templateId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  // --- REMINDER CONFIGURATION ---

  fun observeReminderConfig(userId: String): Flow<ReminderConfig> {
    val docRef = db.collection("users").document(userId).collection("reminder_configs").document("default")
    return docRef.snapshots().map { snapshot ->
      snapshot.toObject(ReminderConfig::class.java) ?: ReminderConfig(userId = userId)
    }
  }

  suspend fun saveReminderConfig(userId: String, config: ReminderConfig): Result<Unit> {
    val docRef = db.collection("users").document(userId).collection("reminder_configs").document("default")
    val payload = mutableMapOf<String, Any?>(
      "userId" to userId,
      "sessionReminderChannel" to config.sessionReminderChannel,
      "sessionReminderHoursBefore" to config.sessionReminderHoursBefore,
      "sessionMessageTemplate" to config.sessionMessageTemplate,
      "debtReminderChannel" to config.debtReminderChannel,
      "debtReminderFrequency" to config.debtReminderFrequency,
      "debtMessageTemplate" to config.debtMessageTemplate,
      "notifyMePendingDebts" to config.notifyMePendingDebts,
      "createdAt" to (config.createdAt ?: FieldValue.serverTimestamp()),
      "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }

    return try {
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }
}
