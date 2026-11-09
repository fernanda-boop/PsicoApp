package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.ClinicalTemplate
import com.example.data.model.Patient
import com.example.data.model.Payment
import com.example.data.model.ReminderConfig
import com.example.data.model.Session
import com.example.data.model.TemplateField
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PsychologistRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun addPatient_validPayload_createsDocumentAndReturnsId() {
    runBlocking {
      val uid = signInTestUser(ALICE_EMAIL)
      val repo = PsychologistRepository(firestore)

      val patient = Patient(
        userId = uid,
        name = "Clara Gomez",
        phone = "+5491100112233",
        sessionFee = 16000.0,
        active = true,
        totalSessions = 0L,
        debtAmount = 0.0
      )

      val result = withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.addPatient(uid, patient)
      }
      assertTrue(result.isSuccess)
      val patientId = result.getOrThrow()
      assertTrue(patientId.isNotEmpty())
    }
  }

  @Test
  fun observePatients_authenticatedOwner_returnsMatchingPatients() {
    runBlocking {
      val uid = signInTestUser(ALICE_EMAIL)
      val repo = PsychologistRepository(firestore)

      val patient = Patient(
        userId = uid,
        name = "Bernardo Silva",
        sessionFee = 15000.0,
        active = true,
        totalSessions = 0L,
        debtAmount = 0.0
      )
      val patientId = withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.addPatient(uid, patient).getOrThrow()
      }

      val patients = withTimeout(FLOW_TIMEOUT_MS) {
        repo.observePatients(uid).first { list -> list.any { it.id == patientId } }
      }
      assertTrue(patients.any { it.id == patientId })
    }
  }

  @Test
  fun addSession_1Click_createsSessionAndUpdatesPatientDebt() {
    runBlocking {
      val uid = signInTestUser(ALICE_EMAIL)
      val repo = PsychologistRepository(firestore)

      val patient = Patient(
        userId = uid,
        name = "Martin Fierro",
        sessionFee = 20000.0,
        active = true,
        totalSessions = 0L,
        debtAmount = 0.0
      )
      val patientId = withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.addPatient(uid, patient).getOrThrow()
      }

      val session = Session(
        userId = uid,
        patientId = patientId,
        patientName = "Martin Fierro",
        date = "2026-11-09",
        fee = 20000.0,
        isPaid = false,
        notes = "Primera sesión de encuadre"
      )

      val sessionResult = withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.addSession(uid, session, autoUpdateDebt = true)
      }
      assertTrue(sessionResult.isSuccess)

      val updatedPatient = withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.getPatient(uid, patientId)
      }
      assertNotNull(updatedPatient)
      assertEquals(1L, updatedPatient!!.totalSessions)
      assertEquals(20000.0, updatedPatient.debtAmount, 0.001)
    }
  }

  @Test
  fun templatesCRUD_authenticatedOwner_succeeds() {
    runBlocking {
      val uid = signInTestUser(ALICE_EMAIL)
      val repo = PsychologistRepository(firestore)

      val template = ClinicalTemplate(
        userId = uid,
        name = "Ficha TCC Adultos",
        description = "Plantilla con campos personalizados",
        isDefault = true,
        fields = listOf(
          TemplateField("f1", "Medicación Actual", "text").toMap(),
          TemplateField("f2", "Fecha Inicio", "date").toMap()
        )
      )

      val addResult = withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.addTemplate(uid, template)
      }
      if (addResult.isFailure) {
        val ex = addResult.exceptionOrNull()
        System.err.println("DEBUG_ERROR_TRACE: ${ex?.message}")
        ex?.printStackTrace()
        throw AssertionError("addTemplate failed: ${ex?.message}", ex)
      }
      assertTrue(addResult.isSuccess)
      val templateId = addResult.getOrThrow()

      val templates = withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeTemplates(uid).first { list -> list.any { it.id == templateId } }
      }
      assertTrue(templates.any { it.id == templateId })
    }
  }

  @Test
  fun reminderConfig_authenticatedOwner_savesAndObserves() {
    runBlocking {
      val uid = signInTestUser(ALICE_EMAIL)
      val repo = PsychologistRepository(firestore)

      val config = ReminderConfig(
        userId = uid,
        sessionReminderChannel = "whatsapp",
        sessionReminderHoursBefore = 24L,
        debtReminderChannel = "sms",
        debtReminderFrequency = "weekly",
        notifyMePendingDebts = true
      )

      val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.saveReminderConfig(uid, config)
      }
      assertTrue(saveResult.isSuccess)

      val observed = withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeReminderConfig(uid).first()
      }
      assertEquals("whatsapp", observed.sessionReminderChannel)
      assertEquals("sms", observed.debtReminderChannel)
    }
  }

  @Test
  fun unauthenticatedUser_failsWithPermissionDenied() {
    runBlocking {
      auth.signOut()
      val repo = PsychologistRepository(firestore)

      var caughtException: Exception? = null
      try {
        withTimeout(FLOW_TIMEOUT_MS) {
          repo.observePatients("any_unauth_uid").first()
        }
      } catch (e: Exception) {
        caughtException = e
      }

      assertNotNull("Expected exception when unauthenticated", caughtException)
      val firestoreException = caughtException?.unwrapFirestoreException()
      assertNotNull("Expected nested FirebaseFirestoreException", firestoreException)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException!!.code)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice@psychologist.test"
    const val DEFAULT_TIMEOUT_MS = 6000L
    const val FLOW_TIMEOUT_MS = 5000L
  }
}
