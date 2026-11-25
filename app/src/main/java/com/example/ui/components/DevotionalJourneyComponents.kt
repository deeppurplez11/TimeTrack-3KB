package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DevotionalCalendarDay
import com.example.data.DevotionalCalendarMonth
import com.example.data.DevotionalDayState
import com.example.data.DevotionalStreakSummary
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandCharcoal
import com.example.ui.theme.BrandEspresso
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalStatusPalette
import com.example.util.ManilaTime

@Composable
fun DevotionalStreakCompactBanner(
    streak: DevotionalStreakSummary,
    todayCompleted: Boolean,
    onOpenJourney: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dayWord = if (streak.currentStreak == 1) "Day" else "Days"
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, BrandEspresso),
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onOpenJourney)
            .testTag("open_devotional_journey_button")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = BrandEspresso,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Personal devotional streak",
                        tint = BrandAmber,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Current Streak: ${streak.currentStreak} $dayWord",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val sub = if (!todayCompleted && streak.currentStreak > 0) {
                    "Longest: ${streak.longestStreak}d • Complete today to grow streak"
                } else {
                    "Longest: ${streak.longestStreak}d • ${streak.totalDevotionals} total completed"
                }
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                color = BrandAmber.copy(alpha = 0.14f),
                shape = RoundedCornerShape(999.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = BrandAmber,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Calendar",
                        style = MaterialTheme.typography.labelMedium,
                        color = BrandAmber,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun DevotionalStreakCard(
    streak: DevotionalStreakSummary,
    todayCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalStatusPalette.current
    val currentLabel = "${streak.currentStreak} ${if (streak.currentStreak == 1) "Day" else "Days"}"
    val longestLabel = "${streak.longestStreak} ${if (streak.longestStreak == 1) "Day" else "Days"}"
    val totalLabel = "${streak.totalDevotionals} ${if (streak.totalDevotionals == 1) "Day" else "Days"}"
    val lastCompletedLabel = streak.lastCompletedDateKey?.let { ManilaTime.formatFullMonthDate(it) } ?: "None yet"

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("devotional_streak_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(BrandCharcoal, BrandEspresso)
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        color = BrandAmber.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BrandAmber.copy(alpha = 0.45f)),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Devotional streak flame",
                                tint = BrandAmber,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "My Devotional Journey",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        val encouragement = when {
                            todayCompleted && streak.currentStreak >= 2 ->
                                "Today's reflection is complete. Keep walking faithfully!"
                            todayCompleted ->
                                "Great step today! Your daily devotional is recorded."
                            streak.currentStreak > 0 ->
                                "Yesterday's streak is preserved — submit today to reach ${streak.currentStreak + 1} days."
                            else ->
                                "Start a steady daily habit of Scripture and prayer."
                        }
                        Text(
                            text = encouragement,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StreakMetricBox(
                        label = "Current Streak",
                        value = currentLabel,
                        accentColor = BrandAmber,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("streak_current_value")
                    )
                    StreakMetricBox(
                        label = "Longest Streak",
                        value = longestLabel,
                        accentColor = Color.White,
                        icon = Icons.Default.EmojiEvents,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("streak_longest_value")
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Total Devotionals",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                        Text(
                            text = totalLabel,
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMono),
                            color = palette.workingFg,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("streak_total_value")
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Last Completed",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                        Text(
                            text = lastCompletedLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.testTag("streak_last_completed_value")
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Private to you • Calculated in Asia/Manila (PHT)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakMetricBox(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.08f),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = BrandAmber,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.72f)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = JetBrainsMono),
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DevotionalCalendarCard(
    calendarMonth: DevotionalCalendarMonth,
    currentManilaYear: Int,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToday: () -> Unit,
    onSelectDay: (DevotionalCalendarDay) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStatusPalette.current
    val weekdayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("devotional_calendar_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Month & Year Header + Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = BrandAmber,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = calendarMonth.monthLabel,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Tap any past or today date to inspect",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!calendarMonth.isCurrentMonth) {
                        TextButton(
                            onClick = onJumpToday,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("calendar_today_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Today,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Today")
                        }
                    }
                    IconButton(
                        onClick = onPrevMonth,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("calendar_prev_month_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous month"
                        )
                    }
                    IconButton(
                        onClick = onNextMonth,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("calendar_next_month_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next month"
                        )
                    }
                }
            }

            // Summary Metrics (Month, Year, Total Days, Elapsed Month %)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalendarStatChip(
                    label = if (calendarMonth.isCurrentMonth) "This Month" else calendarMonth.shortMonthLabel,
                    value = "${calendarMonth.completedInMonth}",
                    accentColor = palette.workingFg,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("calendar_month_count")
                )
                CalendarStatChip(
                    label = if (calendarMonth.year == currentManilaYear) "This Year" else "Year ${calendarMonth.year}",
                    value = "${calendarMonth.completedInYear}",
                    accentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("calendar_year_count")
                )
                CalendarStatChip(
                    label = "Total Days",
                    value = "${calendarMonth.totalCompletedDays}",
                    accentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("calendar_total_count")
                )
                CalendarStatChip(
                    label = "Completion",
                    value = "${calendarMonth.completionPercent}%",
                    accentColor = BrandAmber,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("calendar_completion_percent")
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Weekday Headers (Mon - Sun)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                weekdayLabels.forEach { dayName ->
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Monthly Grid with smooth month transition
            AnimatedContent(
                targetState = calendarMonth,
                transitionSpec = {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                },
                contentKey = { "${it.year}-${it.month}" },
                label = "calendar_month_transition"
            ) { monthData ->
                val cells = buildList<DevotionalCalendarDay?> {
                    repeat(monthData.leadingEmptyCells) { add(null) }
                    addAll(monthData.days)
                    while (size % 7 != 0) {
                        add(null)
                    }
                }
                val rows = cells.chunked(7)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    rows.forEach { weekCells ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            weekCells.forEach { cell ->
                                if (cell == null) {
                                    Spacer(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                    )
                                } else {
                                    CalendarDayCell(
                                        day = cell,
                                        onClick = { onSelectDay(cell) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Compact Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CalendarLegendItem(
                    dotColor = palette.workingFg,
                    label = "Completed"
                )
                CalendarLegendItem(
                    dotColor = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                    label = "Missed"
                )
                CalendarLegendItem(
                    dotColor = BrandAmber,
                    label = "Today"
                )
                CalendarLegendItem(
                    dotColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    label = "Future"
                )
            }
        }
    }
}

@Composable
private fun CalendarStatChip(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono),
                color = accentColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: DevotionalCalendarDay,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStatusPalette.current
    val isInteractive = day.state != DevotionalDayState.FUTURE

    val bgColor = when (day.state) {
        DevotionalDayState.COMPLETED -> palette.workingBg.copy(alpha = 0.85f)
        DevotionalDayState.TODAY_COMPLETED -> palette.workingBg
        DevotionalDayState.TODAY_PENDING -> BrandEspresso.copy(alpha = 0.85f)
        DevotionalDayState.MISSED -> MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
        DevotionalDayState.FUTURE -> Color.Transparent
    }
    val borderColor = when (day.state) {
        DevotionalDayState.TODAY_COMPLETED -> palette.workingFg
        DevotionalDayState.TODAY_PENDING -> BrandAmber
        DevotionalDayState.COMPLETED -> palette.workingFg.copy(alpha = 0.35f)
        DevotionalDayState.MISSED -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        DevotionalDayState.FUTURE -> Color.Transparent
    }
    val textColor = when (day.state) {
        DevotionalDayState.COMPLETED,
        DevotionalDayState.TODAY_COMPLETED -> Color.White
        DevotionalDayState.TODAY_PENDING -> BrandAmber
        DevotionalDayState.MISSED -> MaterialTheme.colorScheme.onSurfaceVariant
        DevotionalDayState.FUTURE -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    }

    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(bgColor)
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(
                        width = if (day.state == DevotionalDayState.TODAY_COMPLETED || day.state == DevotionalDayState.TODAY_PENDING) 2.dp else 1.dp,
                        color = borderColor,
                        shape = shape
                    )
                } else {
                    Modifier
                }
            )
            .then(
                if (isInteractive) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .testTag("calendar_day_${day.dateKey}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = day.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                fontWeight = if (day.state == DevotionalDayState.TODAY_COMPLETED || day.state == DevotionalDayState.TODAY_PENDING || day.state == DevotionalDayState.COMPLETED) {
                    FontWeight.Bold
                } else {
                    FontWeight.Medium
                },
                color = textColor
            )
            when (day.state) {
                DevotionalDayState.COMPLETED,
                DevotionalDayState.TODAY_COMPLETED -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = palette.workingFg,
                        modifier = Modifier.size(12.dp)
                    )
                }
                DevotionalDayState.TODAY_PENDING -> {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(BrandAmber)
                    )
                }
                DevotionalDayState.MISSED -> {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.65f))
                    )
                }
                DevotionalDayState.FUTURE -> {
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun CalendarLegendItem(
    dotColor: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CalendarDayResultDialog(
    day: DevotionalCalendarDay,
    onDismiss: () -> Unit,
    onGoToTodayCheckIn: () -> Unit
) {
    when (day.state) {
        DevotionalDayState.COMPLETED,
        DevotionalDayState.TODAY_COMPLETED -> {
            val detail = day.devotional
            if (detail != null) {
                DevotionalDetailDialog(
                    devotional = detail,
                    onDismiss = onDismiss
                )
            } else {
                val status = day.status
                val submittedMillis = status?.submittedAt?.toDate()?.time
                val timestampText = if (submittedMillis != null) {
                    "${ManilaTime.formatFullMonthDate(day.dateKey)} • ${ManilaTime.formatTime12h(submittedMillis)} PHT"
                } else {
                    ManilaTime.formatFullMonthDate(day.dateKey)
                }
                AlertDialog(
                    onDismissRequest = onDismiss,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = LocalStatusPalette.current.workingFg
                        )
                    },
                    title = { Text("Devotional Completed") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = timestampText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = BrandAmber,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "This devotional was completed under a different Google account. Private SOAP reflections and the uploaded photo remain restricted to the Google account used when submitting.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = onDismiss) {
                            Text("Close")
                        }
                    }
                )
            }
        }

        DevotionalDayState.MISSED -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        imageVector = Icons.Default.EventBusy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text(ManilaTime.formatFullMonthDate(day.dateKey)) },
                text = {
                    Text(
                        text = "No devotional was submitted on this date.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            )
        }

        DevotionalDayState.TODAY_PENDING -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = BrandAmber
                    )
                },
                title = { Text("Today's Devotional is Pending") },
                text = {
                    Text(
                        text = "You haven't submitted today's devotional yet (${ManilaTime.formatFullMonthDate(day.dateKey)}). Complete your photo and SOAP entry on the Today tab to continue your streak and unlock Clock In.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDismiss()
                            onGoToTodayCheckIn()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandAmber,
                            contentColor = BrandCharcoal
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Go to Check-In", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            )
        }

        DevotionalDayState.FUTURE -> {
            // Future dates are non-interactive and do not grant record access.
        }
    }
}
