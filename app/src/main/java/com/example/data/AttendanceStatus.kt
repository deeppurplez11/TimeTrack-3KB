package com.example.data

enum class AttendanceStatus(val label: String) {
    NOT_CLOCKED_IN("Not Clocked In"),
    WORKING("Working"),
    ON_BREAK("On Break"),
    CLOCKED_OUT("Clocked Out");

    companion object {
        fun fromString(raw: String?): AttendanceStatus {
            return entries.firstOrNull { it.name == raw } ?: NOT_CLOCKED_IN
        }
    }
}
