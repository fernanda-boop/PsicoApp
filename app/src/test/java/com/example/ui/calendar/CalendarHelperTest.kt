package com.example.ui.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarHelperTest {

  @Test
  fun getDaysOfWeek_returnsExactlySevenDaysMondayThroughSunday() {
    val days = CalendarHelper.getDaysOfWeek(0)
    assertEquals(7, days.size)
    assertEquals("Lunes", days[0].dayNameSpanish)
    assertEquals("Lun", days[0].dayAbbrSpanish)
    assertEquals("Domingo", days[6].dayNameSpanish)
    assertEquals("Dom", days[6].dayAbbrSpanish)
  }

  @Test
  fun formatDisplayDay_validDate_returnsFormattedSpanishString() {
    val formatted = CalendarHelper.formatDisplayDay("2026-11-09")
    assertNotNull(formatted)
    assertTrue("Should contain day or month", formatted.contains("Noviembre", ignoreCase = true) || formatted.contains("9"))
  }

  @Test
  fun getDaysOfWeek_offsetPreviousAndNext_calculatesDifferentWeeks() {
    val thisWeek = CalendarHelper.getDaysOfWeek(0)
    val prevWeek = CalendarHelper.getDaysOfWeek(-1)
    val nextWeek = CalendarHelper.getDaysOfWeek(1)

    assertEquals(7, prevWeek.size)
    assertEquals(7, nextWeek.size)
    assertTrue(prevWeek.first().date.before(thisWeek.first().date))
    assertTrue(nextWeek.first().date.after(thisWeek.first().date))
  }
}
