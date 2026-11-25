package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.AttendanceStatus
import com.example.data.AttendanceWithBreaks
import com.example.data.EmployeeEntity
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalStatusPalette
import com.example.util.ManilaTime

enum class AppWidthClass {
    COMPACT,
    MEDIUM,
    EXPANDED;

    companion object {
        fun fromWidth(maxWidth: Dp): AppWidthClass = when {
            maxWidth < 600.dp -> COMPACT
            maxWidth < 840.dp -> MEDIUM
            else -> EXPANDED
        }
    }
}

@Composable
fun StatusBadge(
    status: AttendanceStatus,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val palette = LocalStatusPalette.current
    val (fg, bg) = palette.colorsFor(status)

    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 8.dp else 10.dp,
                vertical = if (compact) 4.dp else 5.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (compact) 7.dp else 8.dp)
                    .clip(CircleShape)
                    .background(fg)
            )
            Text(
                text = status.label,
                color = fg,
                style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LiveManilaClockPill(
    nowMillis: Long,
    modifier: Modifier = Modifier,
    showDate: Boolean = false
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = "Asia/Manila time",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Column {
                Text(
                    text = ManilaTime.formatTime12hWithSeconds(nowMillis),
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMono),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (showDate) {
                        "${ManilaTime.formatFullDate(nowMillis)} • PHT"
                    } else {
                        "Asia/Manila (PHT)"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmployeeAvatar(
    fullName: String,
    isActive: Boolean = true,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val initials = fullName
        .trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifEmpty { "3K" }

    val bg = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val fg = if (isActive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, fg.copy(alpha = 0.2f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleSmall,
            color = fg,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AttendanceDetailDialog(
    employee: EmployeeEntity,
    dateKey: String,
    attendance: AttendanceWithBreaks?,
    nowMillis: Long,
    onDismiss: () -> Unit,
    onRequestReset: (() -> Unit)? = null
) {
    val status = attendance?.attendanceStatus ?: AttendanceStatus.NOT_CLOCKED_IN
    val breaks = attendance?.sortedBreaks.orEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = employee.fullName,
                    style = MaterialTheme.typography.titleLarge
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${employee.employeeId} • ${ManilaTime.formatShortDate(dateKey)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusBadge(status = status)

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailTimeRow(
                            icon = Icons.AutoMirrored.Outlined.Login,
                            label = "Clock In",
                            value = ManilaTime.formatTime12h(attendance?.record?.clockInMillis)
                        )
                        DetailTimeRow(
                            icon = Icons.AutoMirrored.Outlined.Logout,
                            label = "Clock Out",
                            value = ManilaTime.formatTime12h(attendance?.record?.clockOutMillis)
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        DetailTimeRow(
                            icon = Icons.Outlined.Coffee,
                            label = "Total Break",
                            value = if (attendance != null) {
                                ManilaTime.formatDurationHms(attendance.totalBreakMillis(nowMillis))
                            } else "00:00:00"
                        )
                        DetailTimeRow(
                            icon = Icons.Outlined.AccessTime,
                            label = "Net Worked",
                            value = if (attendance?.record?.clockInMillis != null) {
                                ManilaTime.formatDurationHms(attendance.netWorkedMillis(nowMillis))
                            } else "00:00:00",
                            highlight = true
                        )
                    }
                }

                Text(
                    text = "Breaks (${breaks.size})",
                    style = MaterialTheme.typography.titleSmall
                )
                if (breaks.isEmpty()) {
                    Text(
                        text = "No breaks recorded for this date.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        breaks.forEachIndexed { idx, brk ->
                            val start = ManilaTime.formatTime12h(brk.breakStartMillis)
                            val end = if (brk.breakEndMillis == null) {
                                "Ongoing"
                            } else {
                                ManilaTime.formatTime12h(brk.breakEndMillis)
                            }
                            val dur = ManilaTime.formatDurationCompact(
                                ((brk.breakEndMillis ?: nowMillis) - brk.breakStartMillis).coerceAtLeast(0L)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Break #${idx + 1}: $start – $end",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = dur,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            if (onRequestReset != null && attendance != null) {
                TextButton(
                    onClick = onRequestReset,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("detail_reset_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Reset Date")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("detail_close_button")
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DetailTimeRow(
    icon: ImageVector,
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMono),
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium
        )
    }
}
