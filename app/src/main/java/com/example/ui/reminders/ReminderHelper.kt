package com.example.ui.reminders

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.Patient
import com.example.data.model.ReminderConfig
import com.example.data.model.Session

object ReminderHelper {

  fun formatSessionMessage(template: String, patientName: String, day: String, time: String): String {
    return template
      .replace("{nombre}", patientName)
      .replace("{dia}", day)
      .replace("{hora}", time)
  }

  fun formatDebtMessage(template: String, patientName: String, amount: Double): String {
    val formattedAmount = amount.toInt().toString()
    return template
      .replace("{nombre}", patientName)
      .replace("{monto}", formattedAmount)
  }

  fun sendSessionReminder(
    context: Context,
    config: ReminderConfig,
    patient: Patient,
    sessionDay: String? = null,
    sessionTime: String? = null
  ) {
    val day = sessionDay ?: patient.dayOfWeek ?: "hoy"
    val time = sessionTime ?: patient.sessionTime ?: "tu horario habitual"
    val message = formatSessionMessage(config.sessionMessageTemplate, patient.name, day, time)

    when (config.sessionReminderChannel.lowercase()) {
      "sms" -> sendSms(context, patient.phone, message)
      "email" -> sendEmail(context, patient.email, "Recordatorio de Sesión de Terapia", message)
      else -> sendWhatsApp(context, patient.phone, message)
    }
  }

  fun sendDebtReminder(
    context: Context,
    config: ReminderConfig,
    patient: Patient
  ) {
    if (patient.debtAmount <= 0) {
      Toast.makeText(context, "${patient.name} no tiene deudas pendientes", Toast.LENGTH_SHORT).show()
      return
    }
    val message = formatDebtMessage(config.debtMessageTemplate, patient.name, patient.debtAmount)

    when (config.debtReminderChannel.lowercase()) {
      "sms" -> sendSms(context, patient.phone, message)
      "email" -> sendEmail(context, patient.email, "Estado de Cuenta - Sesiones de Psicología", message)
      else -> sendWhatsApp(context, patient.phone, message)
    }
  }

  fun sendWhatsApp(context: Context, phone: String?, message: String) {
    if (phone.isNullOrBlank()) {
      Toast.makeText(context, "El paciente no tiene número de teléfono registrado", Toast.LENGTH_SHORT).show()
      return
    }
    val cleanPhone = phone.replace(Regex("[^0-9]"), "")
    val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
      context.startActivity(intent)
    } catch (e: Exception) {
      // Fallback to generic share intent if WhatsApp is not installed
      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, message)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      try {
        context.startActivity(Intent.createChooser(shareIntent, "Enviar recordatorio vía:"))
      } catch (e2: Exception) {
        Toast.makeText(context, "No se pudo abrir WhatsApp ni compartir el mensaje", Toast.LENGTH_SHORT).show()
      }
    }
  }

  fun sendSms(context: Context, phone: String?, message: String) {
    if (phone.isNullOrBlank()) {
      Toast.makeText(context, "El paciente no tiene número de teléfono registrado", Toast.LENGTH_SHORT).show()
      return
    }
    val cleanPhone = phone.replace(Regex("[^0-9+]"), "")
    val smsUri = Uri.parse("smsto:$cleanPhone")
    val intent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
      putExtra("sms_body", message)
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
      context.startActivity(intent)
    } catch (e: Exception) {
      Toast.makeText(context, "No se encontró aplicación de SMS", Toast.LENGTH_SHORT).show()
    }
  }

  fun sendEmail(context: Context, email: String?, subject: String, message: String) {
    if (email.isNullOrBlank()) {
      Toast.makeText(context, "El paciente no tiene email registrado", Toast.LENGTH_SHORT).show()
      return
    }
    val intent = Intent(Intent.ACTION_SENDTO).apply {
      data = Uri.parse("mailto:$email")
      putExtra(Intent.EXTRA_SUBJECT, subject)
      putExtra(Intent.EXTRA_TEXT, message)
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
      context.startActivity(intent)
    } catch (e: Exception) {
      Toast.makeText(context, "No se encontró aplicación de correo", Toast.LENGTH_SHORT).show()
    }
  }
}
