package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AttendanceStatus
import com.example.data.AttendanceWithBreaks
import com.example.data.EmployeeDaySummary
import com.example.data.EmployeeEntity
import com.example.ui.components.AttendanceDetailDialog
import com.example.ui.components.EmployeeAvatar
import com.example.ui.components.LiveManilaClockPill
import com.example.ui.components.ResetAttendanceFlowDialogs
import com.example.ui.components.StatusBadge
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandCharcoal
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalStatusPalette
import com.example.util.ManilaTime

@Composable
fun AdminDashboardScreen(
    employees: List<EmployeeEntity>,
    records: List<AttendanceWithBreaks>,
    nowMillis: Long,
    onResetAttendance: (employeeUid: String, dateKey: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val todayKey = ManilaTime.dateKey(nowMillis)
    var statusFilter by remember { mutableStateOf<AttendanceStatus?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var detailTarget by remember { mutableStateOf<EmployeeDaySummary?>(null) }
    var resetTarget by remember { mutableStateOf<EmployeeEntity?>(null) }

    val activeEmployees = remember(employees) {
        employees.filter { !it.admin && it.active }.sortedBy { it.employeeId }
    }

    val todayMap = remember(records, todayKey) {
        records.filter { it.record.dateKey == todayKey }.associateBy { it.record.employeeUid }
    }

    val summaries = remember(activeEmployees, todayMap, todayKey) {
        activeEmployees.map { emp ->
            EmployeeDaySummary(
                employee = emp,
                dateKey = todayKey,
                attendance = todayMap[emp.uid]
            )
        }
    }

    val workingCount = summaries.count { it.status == AttendanceStatus.WORKING }
    val onBreakCount = summaries.count { it.status == AttendanceStatus.ON_BREAK }
    val notClockedInCount = summaries.count { it.status == AttendanceStatus.NOT_CLOCKED_IN }
    val clockedOutCount = summaries.count { it.status == AttendanceStatus.CLOCKED_OUT }

    val filteredSummaries = remember(summaries, statusFilter, searchQuery) {
        val q = searchQuery.trim()
        summaries.filter { summary ->
            val matchesStatus = statusFilter == null || summary.status == statusFilter
            val matchesQuery = q.isEmpty() ||
                summary.employee.fullName.contains(q, ignoreCase = true) ||
                summary.employee.employeeId.contains(q, ignoreCase = true) ||
                summary.employee.role.contains(q, ignoreCase = true)
            matchesStatus && matchesQuery
        }
    }

    val palette = LocalStatusPalette.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Today's Attendance",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "${activeEmployees.size} active staff members",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                LiveManilaClockPill(nowMillis = nowMillis, showDate = false)
            }
        }

        // KPI Summary Grid (2x2 on narrow, 4-across on wide via responsive Row pairs)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        title = "Working Now",
                        count = workingCount,
                        accentFg = palette.workingFg,
                        accentBg = palette.workingBg,
                        selected = statusFilter == AttendanceStatus.WORKING,
                        onClick = {
                            statusFilter = if (statusFilter == AttendanceStatus.WORKING) null else AttendanceStatus.WORKING
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_working")
                    )
                    KpiCard(
                        title = "On Break",
                        count = onBreakCount,
                        accentFg = palette.onBreakFg,
                        accentBg = palette.onBreakBg,
                        selected = statusFilter == AttendanceStatus.ON_BREAK,
                        onClick = {
                            statusFilter = if (statusFilter == AttendanceStatus.ON_BREAK) null else AttendanceStatus.ON_BREAK
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_on_break")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        title = "Not Clocked In",
                        count = notClockedInCount,
                        accentFg = palette.notClockedInFg,
                        accentBg = palette.notClockedInBg,
                        selected = statusFilter == AttendanceStatus.NOT_CLOCKED_IN,
                        onClick = {
                            statusFilter = if (statusFilter == AttendanceStatus.NOT_CLOCKED_IN) null else AttendanceStatus.NOT_CLOCKED_IN
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_not_clocked_in")
                    )
                    KpiCard(
                        title = "Clocked Out",
                        count = clockedOutCount,
                        accentFg = palette.clockedOutFg,
                        accentBg = palette.clockedOutBg,
                        selected = statusFilter == AttendanceStatus.CLOCKED_OUT,
                        onClick = {
                            statusFilter = if (statusFilter == AttendanceStatus.CLOCKED_OUT) null else AttendanceStatus.CLOCKED_OUT
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_clocked_out")
                    )
                }
            }
        }

        // Filter Chips + Quick Search
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = statusFilter == null,
                        onClick = { statusFilter = null },
                        label = { Text("All (${summaries.size})") }
                    )
                    AttendanceStatus.entries.forEach { st ->
                        val cnt = summaries.count { it.status == st }
                        FilterChip(
                            selected = statusFilter == st,
                            onClick = { statusFilter = if (statusFilter == st) null else st },
                            label = { Text("${st.label} ($cnt)") }
                        )
                    }
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search staff by name, ID, or role") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Outlined.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_search_input")
                )
            }
        }

        if (filteredSummaries.isEmpty()) {
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
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Groups,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "No employees match this filter",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        } else {
            items(filteredSummaries, key = { it.employee.uid }) { summary ->
                TodayEmployeeCard(
                    summary = summary,
                    nowMillis = nowMillis,
                    onClick = { detailTarget = summary },
                    onResetClick = { resetTarget = summary.employee }
                )
            }
        }
    }

    detailTarget?.let { target ->
        AttendanceDetailDialog(
            employee = target.employee,
            dateKey = target.dateKey,
            attendance = target.attendance,
            nowMillis = nowMillis,
            onDismiss = { detailTarget = null },
            onRequestReset = {
                val emp = target.employee
                detailTarget = null
                resetTarget = emp
            }
        )
    }

    resetTarget?.let { emp ->
        ResetAttendanceFlowDialogs(
            employee = emp,
            nowMillis = nowMillis,
            onDismiss = { resetTarget = null },
            onConfirmReset = onResetAttendance
        )
    }
}

@Composable
private fun KpiCard(
    title: String,
    count: Int,
    accentFg: Color,
    accentBg: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (selected) accentBg else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) accentFg else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    color = accentBg,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMono),
                        color = accentFg,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = if (count == 1) "1 member" else "$count members",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TodayEmployeeCard(
    summary: EmployeeDaySummary,
    nowMillis: Long,
    onClick: () -> Unit,
    onResetClick: () -> Unit
) {
    val emp = summary.employee
    val att = summary.attendance

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EmployeeAvatar(fullName = emp.fullName, isActive = emp.active, size = 40.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = emp.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${emp.employeeId} • ${emp.role}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                StatusBadge(status = summary.status, compact = true)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Work: ${ManilaTime.formatDurationCompact(summary.netWorkedMillis(nowMillis))}",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMono),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Coffee,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Break: ${ManilaTime.formatDurationCompact(summary.totalBreakMillis(nowMillis))}",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMono),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (att != null) {
                    IconButton(
                        onClick = onResetClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.RestartAlt,
                            contentDescription = "Reset attendance time",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (att?.record?.clockInMillis != null) {
                Text(
                    text = "In: ${ManilaTime.formatTime12h(att.record.clockInMillis)}  •  Out: ${ManilaTime.formatTime12h(att.record.clockOutMillis)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
