package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Patient(
  @DocumentId val id: String = "",
  val userId: String = "",
  val name: String = "",
  val phone: String? = null,
  val email: String? = null,
  val sessionFee: Double = 0.0,
  val diagnosis: String? = null,
  val notes: String? = null,
  val emergencyContact: String? = null,
  val dayOfWeek: String? = null,
  val sessionTime: String? = null,
  val active: Boolean = true,
  val totalSessions: Long = 0L,
  val debtAmount: Double = 0.0,
  val templateId: String? = null,
  val customFieldValues: Map<String, String>? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Session(
  @DocumentId val id: String = "",
  val userId: String = "",
  val patientId: String = "",
  val patientName: String = "",
  val date: String = "",
  val fee: Double = 0.0,
  val isPaid: Boolean = false,
  val notes: String? = null,
  val audioPath: String? = null,
  val audioDurationSec: Long? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Payment(
  @DocumentId val id: String = "",
  val userId: String = "",
  val patientId: String = "",
  val patientName: String = "",
  val amount: Double = 0.0,
  val date: String = "",
  val paymentMethod: String = "",
  val notes: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class FieldType(val typeName: String, val displayName: String) {
  TEXT("text", "Texto"),
  DATE("date", "Fecha"),
  MULTI_SELECT("multi_select", "Selección Múltiple")
}

@IgnoreExtraProperties
data class TemplateField(
  val id: String = "",
  val label: String = "",
  val type: String = "text",
  val options: List<String> = emptyList()
) {
  fun toMap(): Map<String, Any> {
    return mapOf(
      "id" to id,
      "label" to label,
      "type" to type,
      "options" to options
    )
  }

  companion object {
    @Suppress("UNCHECKED_CAST")
    fun fromMap(map: Map<String, Any?>): TemplateField {
      return TemplateField(
        id = map["id"] as? String ?: "",
        label = map["label"] as? String ?: "",
        type = map["type"] as? String ?: "text",
        options = (map["options"] as? List<String>) ?: emptyList()
      )
    }
  }
}

@IgnoreExtraProperties
data class ClinicalTemplate(
  @DocumentId val id: String = "",
  val userId: String = "",
  val name: String = "",
  val description: String? = null,
  val isDefault: Boolean = false,
  val fields: List<Map<String, Any>> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun parsedFields(): List<TemplateField> {
    return fields.map { TemplateField.fromMap(it) }
  }
}

@IgnoreExtraProperties
data class ReminderConfig(
  @DocumentId val id: String = "default",
  val userId: String = "",
  val sessionReminderChannel: String = "whatsapp", // whatsapp, sms, email
  val sessionReminderHoursBefore: Long = 24L,
  val sessionMessageTemplate: String = "Hola {nombre}, te recuerdo nuestra sesión de terapia el {dia} a las {hora}.",
  val debtReminderChannel: String = "whatsapp",
  val debtReminderFrequency: String = "weekly", // weekly, biweekly, monthly
  val debtMessageTemplate: String = "Hola {nombre}, te comparto el estado de tu cuenta con un saldo pendiente de \${monto} correspondiente a tus sesiones. ¡Muchas gracias!",
  val notifyMePendingDebts: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)
