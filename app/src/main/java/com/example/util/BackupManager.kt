package com.example.util

import com.example.data.AttendanceRecordEntity
import com.example.data.BreakRecordEntity
import com.example.data.EmployeeEntity
import org.json.JSONArray
import org.json.JSONObject

data class BackupSnapshot(
    val employees: List<EmployeeEntity>,
    val attendance: List<AttendanceRecordEntity>,
    val breaks: List<BreakRecordEntity>
)

object BackupManager {
    private const val SCHEMA_VERSION = 2

    fun serialize(
        employees: List<EmployeeEntity>,
        attendance: List<AttendanceRecordEntity>,
        breaks: List<BreakRecordEntity>
    ): String {
        val root = JSONObject().apply {
            put("app", "3KB TimeTrack")
            put("company", "374 Kingdom Builders")
            put("timezone", "Asia/Manila")
            put("schemaVersion", SCHEMA_VERSION)
            put("exportedAtMillis", System.currentTimeMillis())

            val empArr = JSONArray()
            employees.forEach { e ->
                empArr.put(JSONObject().apply {
                    put("uid", e.uid)
                    put("employeeId", e.employeeId)
                    put("fullName", e.fullName)
                    put("role", e.role)
                    put("pin", e.pin)
                    put("active", e.active)
                    put("admin", e.admin)
                    put("createdAtMillis", e.createdAtMillis)
                })
            }
            put("employees", empArr)

            val attArr = JSONArray()
            attendance.forEach { a ->
                attArr.put(JSONObject().apply {
                    put("id", a.id)
                    put("employeeUid", a.employeeUid)
                    put("devotionalId", a.devotionalId.ifBlank { "${a.employeeUid}_${a.dateKey}" })
                    put("dateKey", a.dateKey)
                    put("clockInMillis", a.clockInMillis ?: JSONObject.NULL)
                    put("clockOutMillis", a.clockOutMillis ?: JSONObject.NULL)
                    put("status", a.status)
                })
            }
            put("attendance", attArr)

            val brkArr = JSONArray()
            breaks.forEach { b ->
                brkArr.put(JSONObject().apply {
                    put("id", b.id)
                    put("attendanceId", b.attendanceId)
                    put("employeeUid", b.employeeUid)
                    put("dateKey", b.dateKey)
                    put("breakStartMillis", b.breakStartMillis)
                    put("breakEndMillis", b.breakEndMillis ?: JSONObject.NULL)
                })
            }
            put("breaks", brkArr)
        }
        return root.toString(2)
    }

    fun parse(jsonText: String): BackupSnapshot {
        val root = JSONObject(jsonText)
        val empArr = root.getJSONArray("employees")
        val employees = buildList {
            for (i in 0 until empArr.length()) {
                val o = empArr.getJSONObject(i)
                add(
                    EmployeeEntity(
                        uid = o.getString("uid"),
                        employeeId = o.getString("employeeId"),
                        fullName = o.getString("fullName"),
                        role = o.optString("role", "Team Member"),
                        pin = o.getString("pin"),
                        active = if (o.has("active")) o.getBoolean("active") else o.optBoolean("isActive", true),
                        admin = if (o.has("admin")) o.getBoolean("admin") else o.optBoolean("isAdmin", false),
                        createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis())
                    )
                )
            }
        }

        val attArr = root.optJSONArray("attendance") ?: JSONArray()
        val attendance = buildList {
            for (i in 0 until attArr.length()) {
                val o = attArr.getJSONObject(i)
                val empUid = o.getString("employeeUid")
                val dateKey = o.getString("dateKey")
                add(
                    AttendanceRecordEntity(
                        id = o.opt("id")?.toString().orEmpty(),
                        employeeUid = empUid,
                        devotionalId = o.optString("devotionalId", "${empUid}_${dateKey}"),
                        dateKey = dateKey,
                        clockInMillis = if (o.isNull("clockInMillis")) null else o.getLong("clockInMillis"),
                        clockOutMillis = if (o.isNull("clockOutMillis")) null else o.getLong("clockOutMillis"),
                        status = o.getString("status")
                    )
                )
            }
        }

        val brkArr = root.optJSONArray("breaks") ?: JSONArray()
        val breaks = buildList {
            for (i in 0 until brkArr.length()) {
                val o = brkArr.getJSONObject(i)
                add(
                    BreakRecordEntity(
                        id = o.opt("id")?.toString().orEmpty(),
                        attendanceId = o.opt("attendanceId")?.toString().orEmpty(),
                        employeeUid = o.getString("employeeUid"),
                        dateKey = o.getString("dateKey"),
                        breakStartMillis = o.getLong("breakStartMillis"),
                        breakEndMillis = if (o.isNull("breakEndMillis")) null else o.getLong("breakEndMillis")
                    )
                )
            }
        }

        return BackupSnapshot(employees, attendance, breaks)
    }
}
