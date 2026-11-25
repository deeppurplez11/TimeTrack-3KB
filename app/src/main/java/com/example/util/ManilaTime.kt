package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object ManilaTime {
    val ZONE: TimeZone = TimeZone.getTimeZone("Asia/Manila")
    private val LOCALE: Locale = Locale.US

    private fun newCalendar(millis: Long = System.currentTimeMillis()): Calendar {
        return Calendar.getInstance(ZONE, LOCALE).apply {
            timeInMillis = millis
            firstDayOfWeek = Calendar.MONDAY
        }
    }

    fun dateKey(millis: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", LOCALE).apply { timeZone = ZONE }
        return sdf.format(Date(millis))
    }

    fun parseDateKey(dateKey: String): Long? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", LOCALE).apply {
                timeZone = ZONE
                isLenient = false
            }
            sdf.parse(dateKey)?.time
        } catch (_: Exception) {
            null
        }
    }

    fun formatFullDate(millis: Long): String {
        val sdf = SimpleDateFormat("EEE, MMM d, yyyy", LOCALE).apply { timeZone = ZONE }
        return sdf.format(Date(millis))
    }

    fun formatShortDate(dateKey: String): String {
        val millis = parseDateKey(dateKey) ?: return dateKey
        val sdf = SimpleDateFormat("MMM d, yyyy", LOCALE).apply { timeZone = ZONE }
        return sdf.format(Date(millis))
    }

    fun formatDayHeader(dateKey: String): Pair<String, String> {
        val millis = parseDateKey(dateKey) ?: return dateKey to ""
        val dayName = SimpleDateFormat("EEE", LOCALE).apply { timeZone = ZONE }.format(Date(millis))
        val monthDay = SimpleDateFormat("MMM d", LOCALE).apply { timeZone = ZONE }.format(Date(millis))
        return dayName to monthDay
    }

    fun formatTime12h(millis: Long?): String {
        if (millis == null) return "—"
        val sdf = SimpleDateFormat("hh:mm a", LOCALE).apply { timeZone = ZONE }
        return sdf.format(Date(millis))
    }

    fun formatTime12hWithSeconds(millis: Long): String {
        val sdf = SimpleDateFormat("hh:mm:ss a", LOCALE).apply { timeZone = ZONE }
        return sdf.format(Date(millis))
    }

    fun formatTime24hWithSeconds(millis: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", LOCALE).apply { timeZone = ZONE }
        return sdf.format(Date(millis))
    }

    fun formatCsvTime(millis: Long?): String {
        if (millis == null) return ""
        val sdf = SimpleDateFormat("hh:mm:ss a", LOCALE).apply { timeZone = ZONE }
        return sdf.format(Date(millis))
    }

    fun formatDurationHms(durationMillis: Long): String {
        val safe = durationMillis.coerceAtLeast(0L)
        val totalSeconds = safe / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return String.format(LOCALE, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun formatDurationCompact(durationMillis: Long): String {
        val safe = durationMillis.coerceAtLeast(0L)
        val totalMinutes = safe / 60_000L
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h 0m"
            totalMinutes > 0 -> "${minutes}m"
            else -> "0m"
        }
    }

    fun formatHoursDecimal(durationMillis: Long): String {
        val safe = durationMillis.coerceAtLeast(0L)
        val hours = safe / 3_600_000.0
        return String.format(LOCALE, "%.2f hrs", hours)
    }

    /**
     * Returns the 7 dateKeys (Monday to Sunday) for the week containing [anchorMillis] + [weekOffset]*7 days.
     */
    fun getWeekDateKeys(weekOffset: Int = 0, anchorMillis: Long = System.currentTimeMillis()): List<String> {
        val cal = newCalendar(anchorMillis)
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val diffFromMonday = when (dayOfWeek) {
            Calendar.SUNDAY -> -6
            else -> Calendar.MONDAY - dayOfWeek
        }
        cal.add(Calendar.DAY_OF_MONTH, diffFromMonday + (weekOffset * 7))

        val keys = mutableListOf<String>()
        for (i in 0..6) {
            keys.add(dateKey(cal.timeInMillis))
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return keys
    }

    fun getCurrentMonthRange(anchorMillis: Long = System.currentTimeMillis()): Pair<String, String> {
        val cal = newCalendar(anchorMillis)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val start = dateKey(cal.timeInMillis)
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        val end = dateKey(cal.timeInMillis)
        return start to end
    }

    fun addDays(dateKey: String, deltaDays: Int): String {
        val base = parseDateKey(dateKey) ?: System.currentTimeMillis()
        val cal = newCalendar(base)
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.add(Calendar.DAY_OF_MONTH, deltaDays)
        return dateKey(cal.timeInMillis)
    }

    fun millisAtManilaTime(dateKey: String, hour24: Int, minute: Int, second: Int = 0): Long {
        val base = parseDateKey(dateKey) ?: System.currentTimeMillis()
        val cal = newCalendar(base)
        cal.set(Calendar.HOUR_OF_DAY, hour24)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, second)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun formatFullMonthDate(dateKey: String): String {
        val millis = parseDateKey(dateKey) ?: return dateKey
        val sdf = SimpleDateFormat("MMMM d, yyyy", LOCALE).apply { timeZone = ZONE }
        return sdf.format(Date(millis))
    }

    fun yearMonthOf(dateKey: String): Pair<Int, Int>? {
        val millis = parseDateKey(dateKey) ?: return null
        val cal = newCalendar(millis)
        return cal.get(Calendar.YEAR) to (cal.get(Calendar.MONTH) + 1)
    }

    fun formatMonthYear(year: Int, month1to12: Int): String {
        val cal = newCalendar(millisAtManilaTime(dateKeyFor(year, month1to12, 1), 12, 0))
        val sdf = SimpleDateFormat("MMMM yyyy", LOCALE).apply { timeZone = ZONE }
        return sdf.format(Date(cal.timeInMillis))
    }

    fun formatMonthShort(year: Int, month1to12: Int): String {
        val cal = newCalendar(millisAtManilaTime(dateKeyFor(year, month1to12, 1), 12, 0))
        val sdf = SimpleDateFormat("MMM yyyy", LOCALE).apply { timeZone = ZONE }
        return sdf.format(Date(cal.timeInMillis))
    }

    fun dateKeyFor(year: Int, month1to12: Int, day: Int): String {
        return String.format(LOCALE, "%04d-%02d-%02d", year, month1to12.coerceIn(1, 12), day.coerceIn(1, 31))
    }

    fun daysInMonth(year: Int, month1to12: Int): Int {
        val cal = Calendar.getInstance(ZONE, LOCALE).apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month1to12 - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 12)
        }
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    /**
     * Returns 1 for Monday .. 7 for Sunday for the 1st day of the given Manila month.
     */
    fun firstDayOfWeekMondayOne(year: Int, month1to12: Int): Int {
        val cal = Calendar.getInstance(ZONE, LOCALE).apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month1to12 - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 12)
        }
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            else -> 7
        }
    }

    fun shiftMonth(year: Int, month1to12: Int, deltaMonths: Int): Pair<Int, Int> {
        val cal = Calendar.getInstance(ZONE, LOCALE).apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month1to12 - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 12)
            add(Calendar.MONTH, deltaMonths)
        }
        return cal.get(Calendar.YEAR) to (cal.get(Calendar.MONTH) + 1)
    }

    fun epochDay(dateKey: String): Long? {
        val parts = dateKey.split("-")
        if (parts.size != 3) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val d = parts[2].toIntOrNull() ?: return null
        if (m !in 1..12 || d !in 1..31) return null
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"), LOCALE).apply {
            clear()
            set(Calendar.YEAR, y)
            set(Calendar.MONTH, m - 1)
            set(Calendar.DAY_OF_MONTH, d)
            set(Calendar.HOUR_OF_DAY, 0)
        }
        return utc.timeInMillis / 86_400_000L
    }
}
