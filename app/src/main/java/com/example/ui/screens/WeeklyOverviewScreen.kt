package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AttendanceStatus
import com.example.data.AttendanceWithBreaks
import com.example.data.EmployeeDaySummary
import com.example.data.EmployeeEntity
import com.example.ui.components.AttendanceDetailDialog
import com.example.ui.components.ResetAttendanceFlowDialogs
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalStatusPalette
import com.example.util.ManilaTime

private val EMP_COL_WIDTH = 144.dp
private val DAY_COL_WIDTH = 132.dp

@Composable
fun WeeklyOverviewScreen(
    employees: List<EmployeeEntity>,
    records: List<AttendanceWithBreaks>,
    nowMillis: Long,
    onResetAttendance: (employeeUid: String, dateKey: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var weekOffset by remember { mutableIntStateOf(0) }
    var selectedCell by remember { mutableStateOf<EmployeeDaySummary?>(null) }
    var resetTarget by remember { mutableStateOf<Pair<EmployeeEntity, String>?>(null) }

    val weekKeys = remember(weekOffset, nowMillis) {
        ManilaTime.getWeekDateKeys(weekOffset, nowMillis)
    }
    val todayKey = ManilaTime.dateKey(nowMillis)

    val displayEmployees = remember(employees) {
        employees.filter { !it.admin && it.active }.sortedBy { it.employeeId }
    }

    val recordLookup = remember(records) {
        records.associateBy { it.record.employeeUid to it.record.dateKey }
    }

    val sharedHorizontalScroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 12.dp)
    ) {
        // Week Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Weekly Attendance Matrix",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "${ManilaTime.formatShortDate(weekKeys.first())} – ${ManilaTime.formatShortDate(weekKeys.last())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { weekOffset -= 1 },
                    modifier = Modifier.testTag("week_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous week"
                    )
                }
                if (weekOffset != 0) {
                    TextButton(onClick = { weekOffset = 0 }) {
                        Icon(
                            imageVector = Icons.Outlined.Today,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("This Week")
                    }
                }
                IconButton(
                    onClick = { weekOffset += 1 },
                    modifier = Modifier.testTag("week_next_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next week"
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Matrix Card
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Row (Employee sticky left + 7 scrolling Day columns)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(EMP_COL_WIDTH)
                            .padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = "Employee",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(sharedHorizontalScroll)
                    ) {
                        weekKeys.forEach { dayKey ->
                            val (dayName, monthDay) = ManilaTime.formatDayHeader(dayKey)
                            val isToday = dayKey == todayKey
                            Column(
                                modifier = Modifier
                                    .width(DAY_COL_WIDTH)
                                    .padding(horizontal = 8.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = if (isToday) "$dayName • Today" else dayName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = monthDay,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Employee Rows
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(displayEmployees, key = { it.uid }) { emp ->
                        val weekTotalWorked = weekKeys.sumOf { dk ->
                            recordLookup[emp.uid to dk]?.netWorkedMillis(nowMillis) ?: 0L
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Sticky Left Employee Name + Weekly Total
                            Column(
                                modifier = Modifier
                                    .width(EMP_COL_WIDTH)
                                    .padding(horizontal = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = emp.fullName,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = emp.employeeId,
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Week: ${ManilaTime.formatDurationCompact(weekTotalWorked)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // 7 Day Columns
                            Row(
                                modifier = Modifier.horizontalScroll(sharedHorizontalScroll)
                            ) {
                                weekKeys.forEach { dayKey ->
                                    val att = recordLookup[emp.uid to dayKey]
                                    WeeklyDayCell(
                                        attendance = att,
                                        nowMillis = nowMillis,
                                        isToday = dayKey == todayKey,
                                        onClick = {
                                            selectedCell = EmployeeDaySummary(
                                                employee = emp,
                                                dateKey = dayKey,
                                                attendance = att
                                            )
                                        }
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }

    selectedCell?.let { cell ->
        AttendanceDetailDialog(
            employee = cell.employee,
            dateKey = cell.dateKey,
            attendance = cell.attendance,
            nowMillis = nowMillis,
            onDismiss = { selectedCell = null },
            onRequestReset = {
                val target = cell.employee to cell.dateKey
                selectedCell = null
                resetTarget = target
            }
        )
    }

    resetTarget?.let { (emp, dateKey) ->
        ResetAttendanceFlowDialogs(
            employee = emp,
            initialDateKey = dateKey,
            nowMillis = nowMillis,
            onDismiss = { resetTarget = null },
            onConfirmReset = onResetAttendance
        )
    }
}

@Composable
private fun WeeklyDayCell(
    attendance: AttendanceWithBreaks?,
    nowMillis: Long,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalStatusPalette.current
    val status = attendance?.attendanceStatus ?: AttendanceStatus.NOT_CLOCKED_IN
    val (fg, bg) = palette.colorsFor(status)
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = Modifier
            .width(DAY_COL_WIDTH)
            .padding(horizontal = 4.dp)
            .clip(shape)
            .background(bg.copy(alpha = if (attendance == null) 0.55f else 0.90f))
            .then(
                if (isToday) {
                    Modifier.border(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), shape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        if (attendance == null || attendance.record.clockInMillis == null) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Surface(
                    color = fg.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = AttendanceStatus.NOT_CLOCKED_IN.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = fg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = "In: —",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Out: —",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Brk: 0m • Net: 0m",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val inStr = ManilaTime.formatTime12h(attendance.record.clockInMillis)
            val outStr = ManilaTime.formatTime12h(attendance.record.clockOutMillis)
            val brkStr = ManilaTime.formatDurationCompact(attendance.totalBreakMillis(nowMillis))
            val netStr = ManilaTime.formatDurationCompact(attendance.netWorkedMillis(nowMillis))

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Surface(
                    color = fg.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = status.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = fg,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = "In: $inStr",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = "Out: $outStr",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = "Brk: $brkStr",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = "Net: $netStr",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono),
                    color = fg,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}
