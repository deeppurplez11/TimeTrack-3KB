package com.example.util

import com.example.data.AttendanceStatus
import com.example.data.AttendanceWithBreaks
import com.example.data.EmployeeEntity

enum class CsvExportScope(val label: String) {
    TODAY("Today's Attendance"),
    WEEKLY("Weekly Attendance"),
    MONTHLY("Monthly Attendance"),
    CUSTOM_RANGE("Selected Date Range")
}

object CsvExporter {

    private val HEADER = listOf(
        "Employee Name",
        "Employee ID",
        "Date",
        "Clock In",
        "Break Start",
        "Break End",
        "Additional Breaks",
        "Clock Out",
        "Total Break Duration",
        "Total Hours Worked",
        "Attendance Status"
    )

    fun generateCsv(
        employees: List<EmployeeEntity>,
        records: List<AttendanceWithBreaks>,
        dateKeys: List<String>,
        selectedEmployeeUid: String?,
        nowMillis: Long,
        includeNotClockedInForDates: Boolean = true
    ): String {
        val targetEmployees = employees
            .filter { !it.admin }
            .filter { selectedEmployeeUid == null || it.uid == selectedEmployeeUid }
            .sortedBy { it.employeeId }

        val recordLookup = records.associateBy { it.record.employeeUid to it.record.dateKey }

        val sb = StringBuilder()
        // UTF-8 BOM for Microsoft Excel & Google Sheets compatibility
        sb.append('\uFEFF')
        sb.appendLine(HEADER.joinToString(",") { escapeCsv(it) })

        val sortedDates = dateKeys.sortedDescending()

        for (dateKey in sortedDates) {
            for (emp in targetEmployees) {
                val att = recordLookup[emp.uid to dateKey]
                if (att == null && !includeNotClockedInForDates) continue

                val status = att?.attendanceStatus ?: AttendanceStatus.NOT_CLOCKED_IN
                val clockIn = ManilaTime.formatCsvTime(att?.record?.clockInMillis)
                val clockOut = ManilaTime.formatCsvTime(att?.record?.clockOutMillis)

                val breaks = att?.sortedBreaks.orEmpty()
                val firstBreak = breaks.firstOrNull()
                val breakStart = ManilaTime.formatCsvTime(firstBreak?.breakStartMillis)
                val breakEnd = when {
                    firstBreak == null -> ""
                    firstBreak.breakEndMillis == null -> "Active"
                    else -> ManilaTime.formatCsvTime(firstBreak.breakEndMillis)
                }

                val additionalBreaks = if (breaks.size > 1) {
                    breaks.drop(1).joinToString("; ") { brk ->
                        val s = ManilaTime.formatCsvTime(brk.breakStartMillis)
                        val e = if (brk.breakEndMillis == null) "Active" else ManilaTime.formatCsvTime(brk.breakEndMillis)
                        "$s - $e"
                    }
                } else {
                    ""
                }

                val totalBreakDur = if (att != null && breaks.isNotEmpty()) {
                    ManilaTime.formatDurationHms(att.totalBreakMillis(nowMillis))
                } else {
                    "00:00:00"
                }

                val totalWorkedDur = if (att?.record?.clockInMillis != null) {
                    ManilaTime.formatDurationHms(att.netWorkedMillis(nowMillis))
                } else {
                    "00:00:00"
                }

                val row = listOf(
                    emp.fullName,
                    emp.employeeId,
                    ManilaTime.formatShortDate(dateKey),
                    clockIn,
                    breakStart,
                    breakEnd,
                    additionalBreaks,
                    clockOut,
                    totalBreakDur,
                    totalWorkedDur,
                    status.label
                )
                sb.appendLine(row.joinToString(",") { escapeCsv(it) })
            }
        }

        return sb.toString()
    }

    fun buildSuggestedFilename(scope: CsvExportScope, employeeId: String?): String {
        val today = ManilaTime.dateKey()
        val empSuffix = employeeId?.let { "_$it" } ?: "_all"
        val scopeSlug = when (scope) {
            CsvExportScope.TODAY -> "today"
            CsvExportScope.WEEKLY -> "weekly"
            CsvExportScope.MONTHLY -> "monthly"
            CsvExportScope.CUSTOM_RANGE -> "range"
        }
        return "3KB_TimeTrack_${scopeSlug}${empSuffix}_${today}.csv"
    }

    private fun escapeCsv(value: String): String {
        val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
        return if (needsQuotes) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
