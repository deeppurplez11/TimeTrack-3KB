package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class DevotionalPrivacyPolicy(
    val title: String,
    val description: String
) {
    STATUS_ONLY(
        title = "Status & Timestamps Only",
        description = "Admin sees completion counts and submission timestamps. Photos and SOAP reflections stay completely private."
    ),
    OPT_IN_ONLY(
        title = "Employee Opt-In Sharing",
        description = "Admin can view photo previews and SOAP entries only when the employee explicitly opts in."
    ),
    ADMIN_PASTORAL_VIEW(
        title = "Authorized Admin Oversight",
        description = "Verified CEO/Admin accounts can view devotional photos and SOAP entries in the Devotionals tab (never in attendance reports or CSV)."
    );

    companion object {
        fun fromString(raw: String?): DevotionalPrivacyPolicy {
            return entries.firstOrNull { it.name == raw } ?: OPT_IN_ONLY
        }
    }
}

@Keep
@IgnoreExtraProperties
data class EmployeeEntity(
    @DocumentId val uid: String = "",
    val ownerUid: String = "",
    val workspaceId: String = "3kb_main",
    val employeeId: String = "",
    val fullName: String = "",
    val role: String = "",
    val pin: String = "",
    val active: Boolean = true,
    val admin: Boolean = false,
    val createdAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DevotionalPrivacySettingsEntity(
    @DocumentId val id: String = "devotional_privacy",
    val ownerUid: String = "",
    val workspaceId: String = "3kb_main",
    val policy: String = DevotionalPrivacyPolicy.OPT_IN_ONLY.name,
    val updatedByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val privacyPolicy: DevotionalPrivacyPolicy
        get() = DevotionalPrivacyPolicy.fromString(policy)
}

@Keep
@IgnoreExtraProperties
data class DevotionalStatusEntity(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val workspaceId: String = "3kb_main",
    val employeeUid: String = "",
    val employeeId: String = "",
    val employeeName: String = "",
    val dateKey: String = "",
    val status: String = "SUBMITTED",
    val hasPhoto: Boolean = true,
    val shareWithAdmin: Boolean = false,
    @ServerTimestamp val submittedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DevotionalEntity(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val workspaceId: String = "3kb_main",
    val employeeUid: String = "",
    val employeeId: String = "",
    val employeeName: String = "",
    val dateKey: String = "",
    val scripture: String = "",
    val observation: String = "",
    val application: String = "",
    val prayer: String = "",
    val photoStoragePath: String = "",
    val photoUrl: String = "",
    val status: String = "SUBMITTED",
    val shareWithAdmin: Boolean = false,
    @ServerTimestamp val submittedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AttendanceRecordEntity(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val workspaceId: String = "3kb_main",
    val employeeUid: String = "",
    val devotionalId: String = "",
    val dateKey: String = "",
    val clockInMillis: Long? = null,
    val clockOutMillis: Long? = null,
    val status: String = AttendanceStatus.NOT_CLOCKED_IN.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BreakRecordEntity(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val workspaceId: String = "3kb_main",
    val attendanceId: String = "",
    val employeeUid: String = "",
    val dateKey: String = "",
    val breakStartMillis: Long = 0L,
    val breakEndMillis: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class AttendanceWithBreaks(
    val record: AttendanceRecordEntity,
    val breaks: List<BreakRecordEntity>
) {
    val attendanceStatus: AttendanceStatus
        get() = AttendanceStatus.fromString(record.status)

    val sortedBreaks: List<BreakRecordEntity>
        get() = breaks.sortedBy { it.breakStartMillis }

    fun totalBreakMillis(nowMillis: Long): Long {
        return sortedBreaks.sumOf { brk ->
            val end = brk.breakEndMillis ?: nowMillis
            (end - brk.breakStartMillis).coerceAtLeast(0L)
        }
    }

    fun netWorkedMillis(nowMillis: Long): Long {
        val start = record.clockInMillis ?: return 0L
        val end = record.clockOutMillis ?: nowMillis
        val gross = (end - start).coerceAtLeast(0L)
        return (gross - totalBreakMillis(nowMillis)).coerceAtLeast(0L)
    }

    fun activeOpenBreak(): BreakRecordEntity? {
        return sortedBreaks.firstOrNull { it.breakEndMillis == null }
    }
}

data class EmployeeDaySummary(
    val employee: EmployeeEntity,
    val dateKey: String,
    val attendance: AttendanceWithBreaks?
) {
    val status: AttendanceStatus
        get() = attendance?.attendanceStatus ?: AttendanceStatus.NOT_CLOCKED_IN

    fun totalBreakMillis(nowMillis: Long): Long = attendance?.totalBreakMillis(nowMillis) ?: 0L

    fun netWorkedMillis(nowMillis: Long): Long = attendance?.netWorkedMillis(nowMillis) ?: 0L
}
