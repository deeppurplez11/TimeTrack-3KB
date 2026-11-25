package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AttendanceStatus
import com.example.data.AttendanceWithBreaks
import com.example.data.DevotionalAnalytics
import com.example.data.DevotionalCalendarDay
import com.example.data.DevotionalEntity
import com.example.data.DevotionalPrivacyPolicy
import com.example.data.DevotionalStatusEntity
import com.example.data.DevotionalStreakSummary
import com.example.data.EmployeeEntity
import com.example.data.PersonalDevotionalHistory
import com.example.ui.DevotionalPhotoUploadState
import com.example.ui.components.AppWidthClass
import com.example.ui.components.AttendanceDetailDialog
import com.example.ui.components.CalendarDayResultDialog
import com.example.ui.components.DailyDevotionalCheckInCard
import com.example.ui.components.DevotionalCalendarCard
import com.example.ui.components.DevotionalCompletedCard
import com.example.ui.components.DevotionalDetailDialog
import com.example.ui.components.DevotionalStreakCard
import com.example.ui.components.DevotionalStreakCompactBanner
import com.example.ui.components.LiveManilaClockPill
import com.example.ui.components.StatusBadge
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandCharcoal
import com.example.ui.theme.BrandEspresso
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalStatusPalette
import com.example.util.ManilaTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeePortalScreen(
    employee: EmployeeEntity,
    employeeRecords: List<AttendanceWithBreaks>,
    todayDevotionalStatus: DevotionalStatusEntity?,
    employeeDevotionalStatuses: List<DevotionalStatusEntity>,
    myDevotionals: List<DevotionalEntity>,
    photoUploadState: DevotionalPhotoUploadState,
    isSubmittingDevotional: Boolean,
    privacyPolicy: DevotionalPrivacyPolicy,
    nowMillis: Long,
    snackbarHostState: SnackbarHostState,
    onUploadDevotionalPhoto: (Uri) -> Unit,
    onClearDevotionalPhoto: () -> Unit,
    onSubmitDevotional: (String, String, String, String, Boolean) -> Unit,
    onNotify: (String, Boolean) -> Unit,
    onClockIn: () -> Unit,
    onStartBreak: () -> Unit,
    onEndBreak: () -> Unit,
    onClockOut: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var detailRecord by remember { mutableStateOf<AttendanceWithBreaks?>(null) }
    var devotionalDetail by remember { mutableStateOf<DevotionalEntity?>(null) }
    var selectedCalendarDay by remember { mutableStateOf<DevotionalCalendarDay?>(null) }

    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    val todayKey = ManilaTime.dateKey(nowMillis)
    val todayAttendance = remember(employeeRecords, todayKey) {
        employeeRecords.firstOrNull { it.record.dateKey == todayKey }
    }
    val todayMyDevotional = remember(myDevotionals, todayKey, employee.uid) {
        myDevotionals.firstOrNull { it.employeeUid == employee.uid && it.dateKey == todayKey }
    }
    val myEmployeeDevotionals = remember(myDevotionals, employee.uid) {
        myDevotionals.filter { it.employeeUid == employee.uid }
    }
    val personalHistory = remember(employee.uid, myEmployeeDevotionals, employeeDevotionalStatuses, todayKey) {
        DevotionalAnalytics.buildPersonalHistory(
            employeeUid = employee.uid,
            myDevotionals = myEmployeeDevotionals,
            myStatuses = employeeDevotionalStatuses,
            todayKey = todayKey
        )
    }
    val currentStatus = todayAttendance?.attendanceStatus ?: AttendanceStatus.NOT_CLOCKED_IN

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthClass = AppWidthClass.fromWidth(maxWidth)
        val useRail = widthClass != AppWidthClass.COMPACT

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets.navigationBars,
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.logo_3kb_brand),
                                    contentDescription = "3KB TimeTrack logo",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(34.dp)
                                )
                                Column {
                                    Text(
                                        text = "Hi, ${employee.fullName.substringBefore(" ")}",
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${employee.fullName} • ${employee.employeeId}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrandAmber,
                                        maxLines = 1
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = onSignOut,
                                modifier = Modifier.testTag("employee_sign_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "Sign Out",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = BrandCharcoal
                        )
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            },
            bottomBar = {
                if (!useRail) {
                    val navItemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandAmber,
                        selectedTextColor = BrandAmber,
                        indicatorColor = BrandEspresso,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                colors = navItemColors,
                                icon = {
                                    Icon(
                                        if (selectedTab == 0) Icons.Filled.Today else Icons.Outlined.Today,
                                        contentDescription = "Today"
                                    )
                                },
                                label = { Text("Today") },
                                modifier = Modifier.testTag("emp_nav_today")
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                colors = navItemColors,
                                icon = {
                                    Icon(
                                        if (selectedTab == 1) Icons.Filled.MenuBook else Icons.Outlined.MenuBook,
                                        contentDescription = "Devotional"
                                    )
                                },
                                label = { Text("Devotional") },
                                modifier = Modifier.testTag("emp_nav_devotional")
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                colors = navItemColors,
                                icon = {
                                    Icon(
                                        if (selectedTab == 2) Icons.Filled.History else Icons.Outlined.History,
                                        contentDescription = "History"
                                    )
                                },
                                label = { Text("History") },
                                modifier = Modifier.testTag("emp_nav_history")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    val railColors = NavigationRailItemDefaults.colors(
                        selectedIconColor = BrandAmber,
                        selectedTextColor = BrandAmber,
                        indicatorColor = BrandEspresso,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Spacer(Modifier.height(12.dp))
                        NavigationRailItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            colors = railColors,
                            icon = {
                                Icon(
                                    if (selectedTab == 0) Icons.Filled.Today else Icons.Outlined.Today,
                                    contentDescription = "Today"
                                )
                            },
                            label = { Text("Today") },
                            modifier = Modifier.testTag("emp_rail_today")
                        )
                        NavigationRailItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            colors = railColors,
                            icon = {
                                Icon(
                                    if (selectedTab == 1) Icons.Filled.MenuBook else Icons.Outlined.MenuBook,
                                    contentDescription = "Devotional"
                                )
                            },
                            label = { Text("Devotional") },
                            modifier = Modifier.testTag("emp_rail_devotional")
                        )
                        NavigationRailItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            colors = railColors,
                            icon = {
                                Icon(
                                    if (selectedTab == 2) Icons.Filled.History else Icons.Outlined.History,
                                    contentDescription = "History"
                                )
                            },
                            label = { Text("History") },
                            modifier = Modifier.testTag("emp_rail_history")
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.TopCenter
                ) {
                    when (selectedTab) {
                        0 -> {
                            EmployeeTodayContent(
                                employee = employee,
                                todayKey = todayKey,
                                todayAttendance = todayAttendance,
                                todayDevotionalStatus = todayDevotionalStatus,
                                todayMyDevotional = todayMyDevotional,
                                streakSummary = personalHistory.streak,
                                photoUploadState = photoUploadState,
                                isSubmittingDevotional = isSubmittingDevotional,
                                privacyPolicy = privacyPolicy,
                                currentStatus = currentStatus,
                                nowMillis = nowMillis,
                                onUploadDevotionalPhoto = onUploadDevotionalPhoto,
                                onClearDevotionalPhoto = onClearDevotionalPhoto,
                                onSubmitDevotional = onSubmitDevotional,
                                onViewTodayDevotional = {
                                    todayMyDevotional?.let { devotionalDetail = it }
                                },
                                onOpenDevotionalJourney = { selectedTab = 1 },
                                onNotify = onNotify,
                                onClockIn = onClockIn,
                                onStartBreak = onStartBreak,
                                onEndBreak = onEndBreak,
                                onClockOut = onClockOut
                            )
                        }
                        1 -> {
                            EmployeeDevotionalJourneyContent(
                                personalHistory = personalHistory,
                                todayKey = todayKey,
                                todayCompleted = todayDevotionalStatus != null,
                                onSelectDay = { day -> selectedCalendarDay = day }
                            )
                        }
                        else -> {
                            EmployeeHistoryContent(
                                records = employeeRecords,
                                devotionals = myEmployeeDevotionals,
                                nowMillis = nowMillis,
                                onSelectRecord = { detailRecord = it },
                                onSelectDevotional = { devotionalDetail = it }
                            )
                        }
                    }
                }
            }
        }
    }

    detailRecord?.let { rec ->
        AttendanceDetailDialog(
            employee = employee,
            dateKey = rec.record.dateKey,
            attendance = rec,
            nowMillis = nowMillis,
            onDismiss = { detailRecord = null }
        )
    }

    devotionalDetail?.let { dev ->
        DevotionalDetailDialog(
            devotional = dev,
            onDismiss = { devotionalDetail = null }
        )
    }

    selectedCalendarDay?.let { day ->
        CalendarDayResultDialog(
            day = day,
            onDismiss = { selectedCalendarDay = null },
            onGoToTodayCheckIn = { selectedTab = 0 }
        )
    }
}

@Composable
private fun EmployeeTodayContent(
    employee: EmployeeEntity,
    todayKey: String,
    todayAttendance: AttendanceWithBreaks?,
    todayDevotionalStatus: DevotionalStatusEntity?,
    todayMyDevotional: DevotionalEntity?,
    streakSummary: DevotionalStreakSummary,
    photoUploadState: DevotionalPhotoUploadState,
    isSubmittingDevotional: Boolean,
    privacyPolicy: DevotionalPrivacyPolicy,
    currentStatus: AttendanceStatus,
    nowMillis: Long,
    onUploadDevotionalPhoto: (Uri) -> Unit,
    onClearDevotionalPhoto: () -> Unit,
    onSubmitDevotional: (String, String, String, String, Boolean) -> Unit,
    onViewTodayDevotional: () -> Unit,
    onOpenDevotionalJourney: () -> Unit,
    onNotify: (String, Boolean) -> Unit,
    onClockIn: () -> Unit,
    onStartBreak: () -> Unit,
    onEndBreak: () -> Unit,
    onClockOut: () -> Unit
) {
    val palette = LocalStatusPalette.current
    val workedMillis = todayAttendance?.netWorkedMillis(nowMillis) ?: 0L
    val breakMillis = todayAttendance?.totalBreakMillis(nowMillis) ?: 0L
    val hasDevotionalToday = todayDevotionalStatus != null

    Column(
        modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Live Asia/Manila Clock
        LiveManilaClockPill(
            nowMillis = nowMillis,
            showDate = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Daily Devotional Check-In Gate (mandatory before Clock In when not yet submitted today)
        if (!hasDevotionalToday) {
            DailyDevotionalCheckInCard(
                employeeUid = employee.uid,
                todayKey = todayKey,
                photoState = photoUploadState,
                isSubmitting = isSubmittingDevotional,
                privacyPolicy = privacyPolicy,
                onUploadPhoto = onUploadDevotionalPhoto,
                onClearPhoto = onClearDevotionalPhoto,
                onSubmitDevotional = onSubmitDevotional,
                onNotify = onNotify
            )
        } else {
            DevotionalCompletedCard(
                status = todayDevotionalStatus!!,
                myDevotional = todayMyDevotional,
                onViewDetails = onViewTodayDevotional
            )
        }

        // Quick Personal Devotional Streak & Calendar link inside the Devotional section
        DevotionalStreakCompactBanner(
            streak = streakSummary,
            todayCompleted = hasDevotionalToday,
            onOpenJourney = onOpenDevotionalJourney
        )

        // Signature Brand Charcoal & Espresso Hero Counter Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CURRENT STATUS",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                            Spacer(Modifier.height(6.dp))
                            StatusBadge(status = currentStatus)
                        }
                        Surface(
                            color = BrandAmber.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = BrandAmber,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(24.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = Color.White.copy(alpha = 0.08f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Hours Worked Today",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.72f)
                                )
                                Text(
                                    text = ManilaTime.formatDurationHms(workedMillis),
                                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMono),
                                    color = BrandAmber,
                                    modifier = Modifier.testTag("employee_worked_duration")
                                )
                                Text(
                                    text = "Excludes break time",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.55f)
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            color = Color.White.copy(alpha = 0.08f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Total Break Time",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.72f)
                                )
                                Text(
                                    text = ManilaTime.formatDurationHms(breakMillis),
                                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMono),
                                    color = Color.White,
                                    modifier = Modifier.testTag("employee_break_duration")
                                )
                                Text(
                                    text = "${todayAttendance?.breaks?.size ?: 0} break(s) logged",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.55f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Large Simple Action Buttons
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val canClockIn = currentStatus == AttendanceStatus.NOT_CLOCKED_IN && hasDevotionalToday
            val canStartBreak = currentStatus == AttendanceStatus.WORKING
            val isOnBreak = currentStatus == AttendanceStatus.ON_BREAK
            val canClockOut = currentStatus == AttendanceStatus.WORKING

            Button(
                onClick = onClockIn,
                enabled = canClockIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp)
                    .testTag("clock_in_button"),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.workingFg,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (!hasDevotionalToday && currentStatus == AttendanceStatus.NOT_CLOCKED_IN) {
                        "Submit Devotional to Clock In"
                    } else {
                        "Clock In"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (isOnBreak) {
                Button(
                    onClick = onEndBreak,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(66.dp)
                        .testTag("end_break_button"),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandAmber,
                        contentColor = BrandCharcoal
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Coffee,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "End Break",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                FilledTonalButton(
                    onClick = onStartBreak,
                    enabled = canStartBreak,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                        .testTag("start_break_button"),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Default.Coffee,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Start Break",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            OutlinedButton(
                onClick = onClockOut,
                enabled = canClockOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .testTag("clock_out_button"),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Clock Out",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            val ruleHint = when (currentStatus) {
                AttendanceStatus.NOT_CLOCKED_IN -> if (hasDevotionalToday) {
                    "Daily Devotional complete! Tap Clock In to begin your shift."
                } else {
                    "Complete today's Daily Devotional with photo & SOAP notes above to unlock Clock In."
                }
                AttendanceStatus.WORKING -> "Shift active. Tap Start Break before stepping away."
                AttendanceStatus.ON_BREAK -> "Break active. End your break first before clocking out."
                AttendanceStatus.CLOCKED_OUT -> "Shift complete for today. Ask Admin if a time reset is needed."
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = BrandAmber
                    )
                    Text(
                        text = ruleHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Today's Exact Timestamps Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Today's Shift Log",
                    style = MaterialTheme.typography.titleMedium
                )
                ShiftLogRow(
                    icon = Icons.AutoMirrored.Outlined.Login,
                    label = "Clock In",
                    time = ManilaTime.formatTime12h(todayAttendance?.record?.clockInMillis)
                )
                val breaks = todayAttendance?.sortedBreaks.orEmpty()
                if (breaks.isEmpty()) {
                    ShiftLogRow(
                        icon = Icons.Outlined.Coffee,
                        label = "Breaks",
                        time = "None yet"
                    )
                } else {
                    breaks.forEachIndexed { idx, brk ->
                        val startStr = ManilaTime.formatTime12h(brk.breakStartMillis)
                        val endStr = if (brk.breakEndMillis == null) "Active" else ManilaTime.formatTime12h(brk.breakEndMillis)
                        ShiftLogRow(
                            icon = Icons.Outlined.Coffee,
                            label = "Break #${idx + 1}",
                            time = "$startStr – $endStr"
                        )
                    }
                }
                ShiftLogRow(
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    label = "Clock Out",
                    time = ManilaTime.formatTime12h(todayAttendance?.record?.clockOutMillis)
                )
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ShiftLogRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    time: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = time,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMono),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmployeeDevotionalJourneyContent(
    personalHistory: PersonalDevotionalHistory,
    todayKey: String,
    todayCompleted: Boolean,
    onSelectDay: (DevotionalCalendarDay) -> Unit
) {
    val currentYm = ManilaTime.yearMonthOf(todayKey) ?: (2026 to 11)
    var viewedYear by rememberSaveable { mutableIntStateOf(currentYm.first) }
    var viewedMonth by rememberSaveable { mutableIntStateOf(currentYm.second) }

    val calendarMonth = remember(personalHistory, viewedYear, viewedMonth, todayKey) {
        DevotionalAnalytics.buildMonthCalendar(
            history = personalHistory,
            year = viewedYear,
            month = viewedMonth,
            todayKey = todayKey
        )
    }

    Column(
        modifier = Modifier
            .widthIn(max = 720.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DevotionalStreakCard(
            streak = personalHistory.streak,
            todayCompleted = todayCompleted
        )

        DevotionalCalendarCard(
            calendarMonth = calendarMonth,
            currentManilaYear = currentYm.first,
            onPrevMonth = {
                val (nextY, nextM) = ManilaTime.shiftMonth(viewedYear, viewedMonth, -1)
                viewedYear = nextY
                viewedMonth = nextM
            },
            onNextMonth = {
                val (nextY, nextM) = ManilaTime.shiftMonth(viewedYear, viewedMonth, 1)
                viewedYear = nextY
                viewedMonth = nextM
            },
            onJumpToday = {
                viewedYear = currentYm.first
                viewedMonth = currentYm.second
            },
            onSelectDay = onSelectDay
        )

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun EmployeeHistoryContent(
    records: List<AttendanceWithBreaks>,
    devotionals: List<DevotionalEntity>,
    nowMillis: Long,
    onSelectRecord: (AttendanceWithBreaks) -> Unit,
    onSelectDevotional: (DevotionalEntity) -> Unit
) {
    var historyMode by rememberSaveable { mutableIntStateOf(0) }

    LazyColumn(
        modifier = Modifier
            .widthIn(max = 680.dp)
            .fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "My History",
                    style = MaterialTheme.typography.titleLarge
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = historyMode == 0,
                        onClick = { historyMode = 0 },
                        label = { Text("Shifts (${records.size})") }
                    )
                    FilterChip(
                        selected = historyMode == 1,
                        onClick = { historyMode = 1 },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("My Devotionals (${devotionals.size})") }
                    )
                }
            }
        }

        if (historyMode == 0) {
            if (records.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "No Attendance History Yet",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Your daily clock-ins, breaks, and worked hours sync to Firestore automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(records, key = { it.record.id }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onSelectRecord(item) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = ManilaTime.formatShortDate(item.record.dateKey),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                StatusBadge(status = item.attendanceStatus, compact = true)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "In / Out",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${ManilaTime.formatTime12h(item.record.clockInMillis)} – ${ManilaTime.formatTime12h(item.record.clockOutMillis)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Net / Break",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${ManilaTime.formatDurationCompact(item.netWorkedMillis(nowMillis))} • ${ManilaTime.formatDurationCompact(item.totalBreakMillis(nowMillis))}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                                        color = BrandAmber,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (devotionals.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "No Personal Devotionals Visible on This Account",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Entries submitted while signed into this Google account appear privately here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(devotionals, key = { it.id.ifBlank { "${it.employeeUid}_${it.dateKey}" } }) { dev ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onSelectDevotional(dev) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = ManilaTime.formatShortDate(dev.dateKey),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = dev.submittedAt?.toDate()?.time?.let { ManilaTime.formatTime12h(it) } ?: "Submitted",
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMono),
                                    color = BrandAmber
                                )
                            }
                            Text(
                                text = dev.scripture,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Tap to view photo & full SOAP entry",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
