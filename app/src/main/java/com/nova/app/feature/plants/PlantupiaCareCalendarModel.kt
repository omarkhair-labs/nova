package com.nova.app.feature.plants

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/**
 * One presentation of existing persisted personal follow-ups and reviewed inspections.
 * No inferred watering frequency, invented events, or second schedule data store.
 */
internal data class CareCalendarEntry(
    val date: LocalDate,
    val plantId: String,
    val plantName: String,
    val title: String,
    val origin: String,
)

internal fun parseCareCalendarDate(value: String?): LocalDate? =
    value?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

internal fun careCalendarMonthDays(month: YearMonth): List<LocalDate> {
    val first = month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val required = (month.atEndOfMonth().toEpochDay() - first.toEpochDay() + 1).toInt()
    val count = if (required > 35) 42 else 35
    return (0 until count).map { first.plusDays(it.toLong()) }
}

internal fun careCalendarWeekDays(selected: LocalDate): List<LocalDate> {
    val monday = selected.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (0 until 7).map { monday.plusDays(it.toLong()) }
}

internal fun careCalendarEntries(
    personal: List<CareCalendarEntry>,
    reviewed: List<CareCalendarEntry>,
): List<CareCalendarEntry> = (personal + reviewed)
    .filter { it.plantId.isNotBlank() }
    .distinctBy { listOf(it.date.toString(), it.plantId, it.title, it.origin) }
    .sortedWith(compareBy(CareCalendarEntry::date, CareCalendarEntry::plantName, CareCalendarEntry::title))
