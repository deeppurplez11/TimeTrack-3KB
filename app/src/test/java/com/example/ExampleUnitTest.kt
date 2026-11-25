package com.example

import com.example.data.AttendanceRecordEntity
import com.example.data.AttendanceStatus
import com.example.data.AttendanceWithBreaks
import com.example.data.BreakRecordEntity
import com.example.data.DevotionalPrivacyPolicy
import com.example.data.EmployeeEntity
import com.example.util.CsvExporter
import com.example.util.ManilaTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun manila_net_worked_excludes_breaks() {
        val dateKey = "2026-11-24"
        val clockIn = ManilaTime.millisAtManilaTime(dateKey, 9, 0)
        val breakStart = ManilaTime.millisAtManilaTime(dateKey, 12, 0)
        val breakEnd = ManilaTime.millisAtManilaTime(dateKey, 13, 0)
        val clockOut = ManilaTime.millisAtManilaTime(dateKey, 18, 0)

        val record = AttendanceWithBreaks(
            record = AttendanceRecordEntity(
                id = "emp-1_$dateKey",
                employeeUid = "emp-1",
                dateKey = dateKey,
                clockInMillis = clockIn,
                clockOutMillis = clockOut,
                status = AttendanceStatus.CLOCKED_OUT.name
            ),
            breaks = listOf(
                BreakRecordEntity(
                    id = "brk-1",
                    attendanceId = "emp-1_$dateKey",
                    employeeUid = "emp-1",
                    dateKey = dateKey,
                    breakStartMillis = breakStart,
                    breakEndMillis = breakEnd
                )
            )
        )

        // 9 hours gross - 1 hour break = 8 hours net
        assertEquals(3_600_000L, record.totalBreakMillis(clockOut))
        assertEquals(8L * 3_600_000L, record.netWorkedMillis(clockOut))
        assertEquals("08:00:00", ManilaTime.formatDurationHms(record.netWorkedMillis(clockOut)))
    }

    @Test
    fun csv_exporter_formats_columns_and_excludes_private_devotional_fields() {
        val dateKey = "2026-11-24"
        val emp = EmployeeEntity(
            uid = "emp-1",
            employeeId = "3KB-001",
            fullName = "Marco Santos",
            role = "Engineer",
            pin = "1234"
        )
        val clockIn = ManilaTime.millisAtManilaTime(dateKey, 9, 0)
        val b1s = ManilaTime.millisAtManilaTime(dateKey, 12, 0)
        val b1e = ManilaTime.millisAtManilaTime(dateKey, 12, 30)
        val b2s = ManilaTime.millisAtManilaTime(dateKey, 15, 0)
        val b2e = ManilaTime.millisAtManilaTime(dateKey, 15, 15)
        val clockOut = ManilaTime.millisAtManilaTime(dateKey, 18, 0)

        val att = AttendanceWithBreaks(
            record = AttendanceRecordEntity(
                id = "emp-1_$dateKey",
                employeeUid = emp.uid,
                devotionalId = "emp-1_$dateKey",
                dateKey = dateKey,
                clockInMillis = clockIn,
                clockOutMillis = clockOut,
                status = AttendanceStatus.CLOCKED_OUT.name
            ),
            breaks = listOf(
                BreakRecordEntity(
                    id = "brk-1",
                    attendanceId = "emp-1_$dateKey",
                    employeeUid = emp.uid,
                    dateKey = dateKey,
                    breakStartMillis = b1s,
                    breakEndMillis = b1e
                ),
                BreakRecordEntity(
                    id = "brk-2",
                    attendanceId = "emp-1_$dateKey",
                    employeeUid = emp.uid,
                    dateKey = dateKey,
                    breakStartMillis = b2s,
                    breakEndMillis = b2e
                )
            )
        )

        val csv = CsvExporter.generateCsv(
            employees = listOf(emp),
            records = listOf(att),
            dateKeys = listOf(dateKey),
            selectedEmployeeUid = null,
            nowMillis = clockOut
        )

        assertTrue(csv.startsWith("\uFEFF"))
        val lines = csv.removePrefix("\uFEFF").trim().lines()
        assertEquals(2, lines.size)
        assertEquals(
            "Employee Name,Employee ID,Date,Clock In,Break Start,Break End,Additional Breaks,Clock Out,Total Break Duration,Total Hours Worked,Attendance Status",
            lines[0]
        )
        assertFalse(lines[0].contains("Prayer", ignoreCase = true))
        assertFalse(lines[0].contains("Scripture", ignoreCase = true))
        assertTrue(lines[1].contains("Marco Santos"))
        assertTrue(lines[1].contains("3KB-001"))
        assertTrue(lines[1].contains("00:45:00"))
        assertTrue(lines[1].contains("08:15:00"))
        assertTrue(lines[1].contains("Clocked Out"))
    }

    @Test
    fun devotional_privacy_policy_defaults_safely_to_opt_in() {
        assertEquals(DevotionalPrivacyPolicy.STATUS_ONLY, DevotionalPrivacyPolicy.fromString("STATUS_ONLY"))
        assertEquals(DevotionalPrivacyPolicy.OPT_IN_ONLY, DevotionalPrivacyPolicy.fromString("OPT_IN_ONLY"))
        assertEquals(DevotionalPrivacyPolicy.ADMIN_PASTORAL_VIEW, DevotionalPrivacyPolicy.fromString("ADMIN_PASTORAL_VIEW"))
        assertEquals(DevotionalPrivacyPolicy.OPT_IN_ONLY, DevotionalPrivacyPolicy.fromString("UNKNOWN_VALUE"))
        assertEquals(DevotionalPrivacyPolicy.OPT_IN_ONLY, DevotionalPrivacyPolicy.fromString(null))
    }

    @Test
    fun photo_upload_helper_resolves_extensions_and_preview_models() {
        assertEquals("png", com.example.util.PhotoUploadHelper.resolveImageExtension("image/png"))
        assertEquals("webp", com.example.util.PhotoUploadHelper.resolveImageExtension("image/webp"))
        assertEquals("jpg", com.example.util.PhotoUploadHelper.resolveImageExtension("image/jpeg"))
        assertEquals("jpg", com.example.util.PhotoUploadHelper.resolveImageExtension("application/octet-stream"))

        assertEquals(null, com.example.util.PhotoUploadHelper.resolvePreviewModel(""))
        assertEquals(null, com.example.util.PhotoUploadHelper.resolvePreviewModel("inline://unreadable-image/123"))
        assertEquals(
            "https://firebasestorage.googleapis.com/example.jpg",
            com.example.util.PhotoUploadHelper.resolvePreviewModel("https://firebasestorage.googleapis.com/example.jpg")
        )
    }

    @Test
    fun streak_examples_a_b_c_d_and_multi_year_leap_year_match_rules() {
        // Example A: Mon, Tue, Wed, Thu completed -> Current = 4
        val exA = com.example.data.DevotionalAnalytics.calculateStreak(
            setOf("2026-11-23", "2026-11-24", "2026-11-25", "2026-11-26"),
            todayKey = "2026-11-26"
        )
        assertEquals(4, exA.currentStreak)
        assertEquals(4, exA.longestStreak)
        assertEquals(4, exA.totalDevotionals)
        assertEquals("2026-11-26", exA.lastCompletedDateKey)

        // Example B: Mon, Tue completed, Wed missed, Thu completed -> Current = 1, Longest = 2
        val exB = com.example.data.DevotionalAnalytics.calculateStreak(
            setOf("2026-11-23", "2026-11-24", "2026-11-26"),
            todayKey = "2026-11-26"
        )
        assertEquals(1, exB.currentStreak)
        assertEquals(2, exB.longestStreak)
        assertEquals(3, exB.totalDevotionals)

        // Example C: Mon, Tue, Wed completed, Thu not yet completed (today = Thu) -> Current = 3
        val exC = com.example.data.DevotionalAnalytics.calculateStreak(
            setOf("2026-11-23", "2026-11-24", "2026-11-25"),
            todayKey = "2026-11-26"
        )
        assertEquals(3, exC.currentStreak)
        assertEquals(3, exC.longestStreak)
        assertEquals("2026-11-25", exC.lastCompletedDateKey)

        // Example D: Mon, Tue, Wed completed, Thu missed, Fri not yet completed -> Current = 0, Longest = 3
        val exD = com.example.data.DevotionalAnalytics.calculateStreak(
            setOf("2026-11-23", "2026-11-24", "2026-11-25"),
            todayKey = "2026-11-27"
        )
        assertEquals(0, exD.currentStreak)
        assertEquals(3, exD.longestStreak)

        // Multi-year + leap-year consecutive streak across 2023 -> 2024
        val multiYearDates = mutableSetOf<String>()
        var cursor = "2023-12-28"
        for (i in 0 until 400) {
            multiYearDates.add(cursor)
            cursor = ManilaTime.addDays(cursor, 1)
        }
        val longStreak = com.example.data.DevotionalAnalytics.calculateStreak(
            completedDateKeys = multiYearDates,
            todayKey = ManilaTime.addDays("2023-12-28", 399)
        )
        assertEquals(400, longStreak.currentStreak)
        assertEquals(400, longStreak.longestStreak)
        assertEquals(400, longStreak.totalDevotionals)
    }

    @Test
    fun devotional_calendar_deduplicates_excludes_future_days_and_handles_empty_months() {
        val empUid = "emp-1"
        val todayKey = "2026-11-10"
        val devotionals = listOf(
            // Duplicate submissions on 2026-11-01 must count only once
            com.example.data.DevotionalEntity(
                id = "${empUid}_2026-11-01_a",
                employeeUid = empUid,
                dateKey = "2026-11-01",
                scripture = "Psalm 1:1",
                photoUrl = "", // Missing photo handled safely
                status = "SUBMITTED"
            ),
            com.example.data.DevotionalEntity(
                id = "${empUid}_2026-11-01_b",
                employeeUid = empUid,
                dateKey = "2026-11-01",
                scripture = "Psalm 1:2",
                photoUrl = "inline://unreadable-image/abc",
                status = "SUBMITTED"
            ),
            // Non-submitted record must NOT count
            com.example.data.DevotionalEntity(
                id = "${empUid}_2026-11-02",
                employeeUid = empUid,
                dateKey = "2026-11-02",
                scripture = "Draft",
                status = "DRAFT"
            ),
            // Another employee's record must NOT leak into emp-1 history
            com.example.data.DevotionalEntity(
                id = "emp-other_2026-11-03",
                employeeUid = "emp-other",
                dateKey = "2026-11-03",
                scripture = "Other",
                status = "SUBMITTED"
            ),
            // Future record (impossible normally) must NOT inflate stats
            com.example.data.DevotionalEntity(
                id = "${empUid}_2026-11-20",
                employeeUid = empUid,
                dateKey = "2026-11-20",
                scripture = "Future",
                status = "SUBMITTED"
            )
        )
        val statuses = listOf(
            com.example.data.DevotionalStatusEntity(
                id = "${empUid}_2026-11-09",
                employeeUid = empUid,
                dateKey = "2026-11-09",
                status = "SUBMITTED"
            ),
            com.example.data.DevotionalStatusEntity(
                id = "${empUid}_2026-11-10",
                employeeUid = empUid,
                dateKey = "2026-11-10",
                status = "SUBMITTED"
            )
        )

        val history = com.example.data.DevotionalAnalytics.buildPersonalHistory(
            employeeUid = empUid,
            myDevotionals = devotionals,
            myStatuses = statuses,
            todayKey = todayKey
        )

        // Completed unique dates: Nov 1, Nov 9, Nov 10 -> 3 total, current streak 2
        assertEquals(3, history.streak.totalDevotionals)
        assertEquals(2, history.streak.currentStreak)
        assertEquals(2, history.streak.longestStreak)

        val novCal = com.example.data.DevotionalAnalytics.buildMonthCalendar(history, 2026, 11, todayKey)
        assertEquals(30, novCal.days.size)
        assertEquals(6, novCal.leadingEmptyCells) // Nov 1, 2026 is Sunday -> 6 leading Monday..Saturday cells
        assertEquals(3, novCal.completedInMonth)
        assertEquals(10, novCal.elapsedDaysInMonth) // Nov 1..10 only
        assertEquals(30, novCal.completionPercent) // 3 / 10 = 30%
        assertEquals(com.example.data.DevotionalDayState.COMPLETED, novCal.days[0].state)
        assertEquals(com.example.data.DevotionalDayState.MISSED, novCal.days[1].state) // Nov 2 draft -> missed
        assertEquals(com.example.data.DevotionalDayState.TODAY_COMPLETED, novCal.days[9].state) // Nov 10
        assertEquals(com.example.data.DevotionalDayState.FUTURE, novCal.days[19].state) // Nov 20

        // Empty previous month (Oct 2026)
        val octCal = com.example.data.DevotionalAnalytics.buildMonthCalendar(history, 2026, 10, todayKey)
        assertEquals(0, octCal.completedInMonth)
        assertEquals(31, octCal.elapsedDaysInMonth)
        assertEquals(0, octCal.completionPercent)
        assertTrue(octCal.days.all { it.state == com.example.data.DevotionalDayState.MISSED })
    }

    @Test
    fun manila_calendar_and_streak_are_independent_of_device_timezone() {
        val originalTz = java.util.TimeZone.getDefault()
        try {
            // 2026-11-25 18:00 America/Los_Angeles (UTC-8) is 2026-11-26 10:00 Asia/Manila (UTC+8)
            val instantMillis = ManilaTime.millisAtManilaTime("2026-11-26", 10, 0)
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/Los_Angeles"))

            val todayInManila = ManilaTime.dateKey(instantMillis)
            assertEquals("2026-11-26", todayInManila)
            assertEquals("November 26, 2026", ManilaTime.formatFullMonthDate(todayInManila))
            assertEquals(2026 to 11, ManilaTime.yearMonthOf(todayInManila))

            val streak = com.example.data.DevotionalAnalytics.calculateStreak(
                setOf("2026-11-24", "2026-11-25", "2026-11-26"),
                todayKey = todayInManila
            )
            assertEquals(3, streak.currentStreak)
        } finally {
            java.util.TimeZone.setDefault(originalTz)
        }
    }
}
