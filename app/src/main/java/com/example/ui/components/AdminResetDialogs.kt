package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.EmployeeEntity
import com.example.util.ManilaTime

/**
 * Provides the two-option Reset Time / Attendance flow for a selected employee:
 * 1. Reset Today's Attendance
 * 2. Reset Selected Date
 * Always shows an explicit confirmation dialog naming the employee and affected date before deleting.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetAttendanceFlowDialogs(
    employee: EmployeeEntity,
    initialDateKey: String? = null,
    nowMillis: Long,
    onDismiss: () -> Unit,
    onConfirmReset: (employeeUid: String, dateKey: String) -> Unit
) {
    val todayKey = ManilaTime.dateKey(nowMillis)
    var showDatePicker by remember { mutableStateOf(false) }
    var pendingConfirmDateKey by remember { mutableStateOf(initialDateKey) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = nowMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            pendingConfirmDateKey = ManilaTime.dateKey(selectedMillis)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Select Date")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val confirmKey = pendingConfirmDateKey
    if (confirmKey != null) {
        AlertDialog(
            onDismissRequest = {
                if (initialDateKey != null) onDismiss() else pendingConfirmDateKey = null
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text("Confirm Attendance Reset")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Employee: ${employee.fullName} (${employee.employeeId})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Affected Date: ${ManilaTime.formatShortDate(confirmKey)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    Text(
                        text = "This will permanently delete the clock-in session and all break records for this employee on the selected date, and reset their status to Not Clocked In. Other employees' records will not be affected.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirmReset(employee.uid, confirmKey)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_reset_attendance_button")
                ) {
                    Text("Reset Attendance")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (initialDateKey != null) onDismiss() else pendingConfirmDateKey = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    } else {
        // Choose Option 1 (Today) or Option 2 (Selected Date)
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text("Reset Attendance Time")
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Select which attendance records to reset for ${employee.fullName} (${employee.employeeId}):",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    FilledTonalButton(
                        onClick = { pendingConfirmDateKey = todayKey },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_today_option")
                    ) {
                        Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Reset Today's Attendance (${ManilaTime.formatShortDate(todayKey)})")
                    }

                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_selected_date_option")
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Reset Selected Date...")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}
