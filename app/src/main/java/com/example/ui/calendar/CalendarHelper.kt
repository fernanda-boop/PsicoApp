package com.example.ui.calendar

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CalendarDayInfo(
  val date: Date,
  val dateStr: String, // YYYY-MM-DD
  val dayNameSpanish: String, // Lunes, Martes, etc.
  val dayAbbrSpanish: String, // Lun, Mar, etc.
  val dayNumber: Int, // 1 - 31
  val isToday: Boolean
)

object CalendarHelper {

  private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
  private val displayDayFormat = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "ES"))
  private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))

  fun getTodayDateStr(): String {
    return dateFormat.format(Date())
  }

  fun formatDisplayDay(dateStr: String): String {
    return try {
      val parsed = dateFormat.parse(dateStr)
      if (parsed != null) {
        val formatted = displayDayFormat.format(parsed)
        formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }
      } else {
        dateStr
      }
    } catch (e: Exception) {
      dateStr
    }
  }

  fun formatMonthYear(date: Date): String {
    val formatted = monthYearFormat.format(date)
    return formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }
  }

  /**
   * Returns the 7 days of the week (Monday through Sunday) for the week offset.
   * offsetWeeks = 0 is current week, -1 previous week, +1 next week.
   */
  fun getDaysOfWeek(offsetWeeks: Int = 0): List<CalendarDayInfo> {
    val cal = Calendar.getInstance()
    cal.firstDayOfWeek = Calendar.MONDAY

    // Shift to current week's Monday
    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)

    if (offsetWeeks != 0) {
      cal.add(Calendar.WEEK_OF_YEAR, offsetWeeks)
    }

    val todayStr = getTodayDateStr()
    val days = mutableListOf<CalendarDayInfo>()

    for (i in 0 until 7) {
      val d = cal.time
      val dateStr = dateFormat.format(d)
      val dayName = when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "Lunes"
        Calendar.TUESDAY -> "Martes"
        Calendar.WEDNESDAY -> "Miércoles"
        Calendar.THURSDAY -> "Jueves"
        Calendar.FRIDAY -> "Viernes"
        Calendar.SATURDAY -> "Sábado"
        else -> "Domingo"
      }
      val dayAbbr = when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "Lun"
        Calendar.TUESDAY -> "Mar"
        Calendar.WEDNESDAY -> "Mié"
        Calendar.THURSDAY -> "Jue"
        Calendar.FRIDAY -> "Vie"
        Calendar.SATURDAY -> "Sáb"
        else -> "Dom"
      }
      val dayNumber = cal.get(Calendar.DAY_OF_MONTH)

      days.add(
        CalendarDayInfo(
          date = d,
          dateStr = dateStr,
          dayNameSpanish = dayName,
          dayAbbrSpanish = dayAbbr,
          dayNumber = dayNumber,
          isToday = (dateStr == todayStr)
        )
      )

      cal.add(Calendar.DAY_OF_MONTH, 1)
    }

    return days
  }
}
