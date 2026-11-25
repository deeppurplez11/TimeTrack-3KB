package com.example.data

import com.example.util.ManilaTime
import kotlin.math.roundToInt

enum class DevotionalDayState {
    COMPLETED,
    MISSED,
    TODAY_COMPLETED,
    TODAY_PENDING,
    FUTURE
}

data class DevotionalStreakSummary(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalDevotionals: Int = 0,
    val lastCompletedDateKey: String? = null
)

data class PersonalDevotionalHistory(
    val completedDateKeys: Set<String> = emptySet(),
    val devotionalsByDate: Map<String, DevotionalEntity> = emptyMap(),
    val statusesByDate: Map<String, DevotionalStatusEntity> = emptyMap(),
    val streak: DevotionalStreakSummary = DevotionalStreakSummary()
)

data class DevotionalCalendarDay(
    val dateKey: String,
    val dayOfMonth: Int,
    val state: DevotionalDayState,
    val devotional: DevotionalEntity? = null,
    val status: DevotionalStatusEntity? = null
)

data class DevotionalCalendarMonth(
    val year: Int,
    val month: Int,
    val monthLabel: String,
    val shortMonthLabel: String,
    val leadingEmptyCells: Int,
    val days: List<DevotionalCalendarDay>,
    val completedInMonth: Int,
    val elapsedDaysInMonth: Int,
    val completionPercent: Int,
    val completedInYear: Int,
    val totalCompletedDays: Int,
    val isCurrentMonth: Boolean
)

object DevotionalAnalytics {
    private val DATE_KEY_REGEX = Regex("""^\d{4}-\d{2}-\d{2}$""")

    fun resolveCanonicalDateKey(
        rawDateKey: String,
        submittedAtMillis: Long?
    ): String? {
        val trimmed = rawDateKey.trim()
        if (DATE_KEY_REGEX.matches(trimmed) && ManilaTime.parseDateKey(trimmed) != null) {
            return trimmed
        }
        return submittedAtMillis?.let { ManilaTime.dateKey(it) }
    }

    fun buildPersonalHistory(
        employeeUid: String,
        myDevotionals: List<DevotionalEntity>,
        myStatuses: List<DevotionalStatusEntity>,
        todayKey: String
    ): PersonalDevotionalHistory {
        val validDevotionalsByDate = mutableMapOf<String, DevotionalEntity>()
        myDevotionals
            .asSequence()
            .filter { it.employeeUid == employeeUid && it.status == "SUBMITTED" }
            .sortedByDescending { it.submittedAt?.toDate()?.time ?: 0L }
            .forEach { dev ->
                val dateKey = resolveCanonicalDateKey(dev.dateKey, dev.submittedAt?.toDate()?.time)
                if (dateKey != null && dateKey <= todayKey) {
                    validDevotionalsByDate.putIfAbsent(dateKey, dev)
                }
            }

        val validStatusesByDate = mutableMapOf<String, DevotionalStatusEntity>()
        myStatuses
            .asSequence()
            .filter { it.employeeUid == employeeUid && it.status == "SUBMITTED" }
            .sortedByDescending { it.submittedAt?.toDate()?.time ?: 0L }
            .forEach { st ->
                val dateKey = resolveCanonicalDateKey(st.dateKey, st.submittedAt?.toDate()?.time)
                if (dateKey != null && dateKey <= todayKey) {
                    validStatusesByDate.putIfAbsent(dateKey, st)
                }
            }

        val completedDates = (validDevotionalsByDate.keys + validStatusesByDate.keys).toSortedSet()
        val streak = calculateStreak(completedDates, todayKey)

        return PersonalDevotionalHistory(
            completedDateKeys = completedDates,
            devotionalsByDate = validDevotionalsByDate,
            statusesByDate = validStatusesByDate,
            streak = streak
        )
    }

    fun calculateStreak(
        completedDateKeys: Set<String>,
        todayKey: String
    ): DevotionalStreakSummary {
        val validSorted = completedDateKeys
            .asSequence()
            .filter { DATE_KEY_REGEX.matches(it) && ManilaTime.epochDay(it) != null && it <= todayKey }
            .toSortedSet()

        if (validSorted.isEmpty()) {
            return DevotionalStreakSummary()
        }

        val todayCompleted = todayKey in validSorted
        val anchorKey = if (todayCompleted) {
            todayKey
        } else {
            ManilaTime.addDays(todayKey, -1)
        }

        var currentStreak = 0
        if (anchorKey in validSorted) {
            var cursor = anchorKey
            while (cursor in validSorted) {
                currentStreak++
                cursor = ManilaTime.addDays(cursor, -1)
            }
        }

        var longestStreak = 0
        var running = 0
        var prevEpochDay: Long? = null
        for (dateKey in validSorted) {
            val epoch = ManilaTime.epochDay(dateKey) ?: continue
            running = if (prevEpochDay != null && epoch == prevEpochDay + 1L) {
                running + 1
            } else {
                1
            }
            if (running > longestStreak) {
                longestStreak = running
            }
            prevEpochDay = epoch
        }

        return DevotionalStreakSummary(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            totalDevotionals = validSorted.size,
            lastCompletedDateKey = validSorted.lastOrNull()
        )
    }

    fun buildMonthCalendar(
        history: PersonalDevotionalHistory,
        year: Int,
        month: Int,
        todayKey: String
    ): DevotionalCalendarMonth {
        val safeMonth = month.coerceIn(1, 12)
        val totalDays = ManilaTime.daysInMonth(year, safeMonth)
        val leadingEmpty = (ManilaTime.firstDayOfWeekMondayOne(year, safeMonth) - 1).coerceIn(0, 6)

        val currentYm = ManilaTime.yearMonthOf(todayKey)
        val isCurrentMonth = currentYm?.first == year && currentYm.second == safeMonth

        val days = (1..totalDays).map { day ->
            val key = ManilaTime.dateKeyFor(year, safeMonth, day)
            val isCompleted = key in history.completedDateKeys
            val state = when {
                key == todayKey && isCompleted -> DevotionalDayState.TODAY_COMPLETED
                key == todayKey && !isCompleted -> DevotionalDayState.TODAY_PENDING
                key > todayKey -> DevotionalDayState.FUTURE
                isCompleted -> DevotionalDayState.COMPLETED
                else -> DevotionalDayState.MISSED
            }
            DevotionalCalendarDay(
                dateKey = key,
                dayOfMonth = day,
                state = state,
                devotional = history.devotionalsByDate[key],
                status = history.statusesByDate[key]
            )
        }

        val completedInMonth = days.count {
            it.state == DevotionalDayState.COMPLETED || it.state == DevotionalDayState.TODAY_COMPLETED
        }
        val elapsedDaysInMonth = days.count { it.state != DevotionalDayState.FUTURE }
        val completionPercent = if (elapsedDaysInMonth > 0) {
            ((completedInMonth * 100.0) / elapsedDaysInMonth).roundToInt().coerceIn(0, 100)
        } else {
            0
        }

        val yearPrefix = String.format(java.util.Locale.US, "%04d-", year)
        val completedInYear = history.completedDateKeys.count { it.startsWith(yearPrefix) }

        return DevotionalCalendarMonth(
            year = year,
            month = safeMonth,
            monthLabel = ManilaTime.formatMonthYear(year, safeMonth),
            shortMonthLabel = ManilaTime.formatMonthShort(year, safeMonth),
            leadingEmptyCells = leadingEmpty,
            days = days,
            completedInMonth = completedInMonth,
            elapsedDaysInMonth = elapsedDaysInMonth,
            completionPercent = completionPercent,
            completedInYear = completedInYear,
            totalCompletedDays = history.completedDateKeys.size,
            isCurrentMonth = isCurrentMonth
        )
    }
}
