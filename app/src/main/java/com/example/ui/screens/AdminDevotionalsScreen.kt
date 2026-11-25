package com.example.ui.screens

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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.AdminDevotionalFeed
import com.example.data.DevotionalEntity
import com.example.data.DevotionalPrivacyPolicy
import com.example.data.DevotionalStatusEntity
import com.example.data.EmployeeEntity
import com.example.ui.components.DevotionalDetailDialog
import com.example.ui.components.EmployeeAvatar
import com.example.ui.components.LiveManilaClockPill
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalStatusPalette
import com.example.util.ManilaTime
import com.example.util.PhotoUploadHelper

private enum class AdminDevotionalDateFilter {
    TODAY,
    YESTERDAY,
    CUSTOM_DATE,
    ALL_HISTORY
}

private enum class AdminCompletionFilter(val label: String) {
    ALL("All"),
    COMPLETED("Completed"),
    PENDING("Pending")
}

private data class AdminDayDevotionalRow(
    val employee: EmployeeEntity?,
    val employeeUid: String,
    val employeeId: String,
    val fullName: String,
    val role: String,
    val dateKey: String,
    val status: DevotionalStatusEntity?,
    val detail: DevotionalEntity?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDevotionalsScreen(
    employees: List<EmployeeEntity>,
    devotionalStatuses: List<DevotionalStatusEntity>,
    adminDevotionalFeed: AdminDevotionalFeed,
    privacyPolicy: DevotionalPrivacyPolicy,
    nowMillis: Long,
    onUpdatePrivacyPolicy: (DevotionalPrivacyPolicy) -> Unit,
    modifier: Modifier = Modifier
) {
    val todayKey = ManilaTime.dateKey(nowMillis)
    val yesterdayKey = ManilaTime.addDays(todayKey, -1)

    var dateFilterMode by rememberSaveable { mutableStateOf(AdminDevotionalDateFilter.TODAY) }
    var customDateKey by rememberSaveable { mutableStateOf(todayKey) }
    var completionFilter by rememberSaveable { mutableStateOf(AdminCompletionFilter.ALL) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showPolicyDialog by remember { mutableStateOf(false) }
    var detailTarget by remember { mutableStateOf<DevotionalEntity?>(null) }

    val selectedDateKey = when (dateFilterMode) {
        AdminDevotionalDateFilter.TODAY -> todayKey
        AdminDevotionalDateFilter.YESTERDAY -> yesterdayKey
        AdminDevotionalDateFilter.CUSTOM_DATE -> customDateKey
        AdminDevotionalDateFilter.ALL_HISTORY -> null
    }

    val activeStaff = remember(employees) {
        employees.filter { !it.admin && it.active }.sortedBy { it.employeeId }
    }
    val empByUid = remember(employees) { employees.associateBy { it.uid } }
    val detailById = remember(adminDevotionalFeed.records) {
        adminDevotionalFeed.records.associateBy { it.id.ifBlank { "${it.employeeUid}_${it.dateKey}" } }
    }

    val overviewDateKey = selectedDateKey ?: todayKey
    val overviewStatusesForDate = remember(devotionalStatuses, overviewDateKey) {
        devotionalStatuses.filter { it.dateKey == overviewDateKey }.associateBy { it.employeeUid }
    }
    val completedCount = remember(activeStaff, overviewStatusesForDate) {
        activeStaff.count { overviewStatusesForDate.containsKey(it.uid) }
    }
    val pendingCount = (activeStaff.size - completedCount).coerceAtLeast(0)

    val rows: List<AdminDayDevotionalRow> = remember(
        dateFilterMode,
        selectedDateKey,
        activeStaff,
        empByUid,
        devotionalStatuses,
        detailById,
        completionFilter,
        searchQuery
    ) {
        val q = searchQuery.trim()
        val baseRows = if (selectedDateKey != null) {
            val statusMap = devotionalStatuses
                .filter { it.dateKey == selectedDateKey }
                .associateBy { it.employeeUid }

            activeStaff.map { emp ->
                val st = statusMap[emp.uid]
                val devId = "${emp.uid}_$selectedDateKey"
                AdminDayDevotionalRow(
                    employee = emp,
                    employeeUid = emp.uid,
                    employeeId = emp.employeeId,
                    fullName = emp.fullName,
                    role = emp.role,
                    dateKey = selectedDateKey,
                    status = st,
                    detail = detailById[devId]
                )
            } + devotionalStatuses
                .filter { st -> st.dateKey == selectedDateKey && activeStaff.none { it.uid == st.employeeUid } }
                .mapNotNull { st ->
                    val emp = empByUid[st.employeeUid] ?: return@mapNotNull null
                    val devId = "${st.employeeUid}_${st.dateKey}"
                    AdminDayDevotionalRow(
                        employee = emp,
                        employeeUid = st.employeeUid,
                        employeeId = emp.employeeId,
                        fullName = emp.fullName,
                        role = emp.role,
                        dateKey = st.dateKey,
                        status = st,
                        detail = detailById[devId]
                    )
                }
        } else {
            devotionalStatuses.mapNotNull { st ->
                val emp = empByUid[st.employeeUid] ?: return@mapNotNull null
                val devId = "${st.employeeUid}_${st.dateKey}"
                AdminDayDevotionalRow(
                    employee = emp,
                    employeeUid = st.employeeUid,
                    employeeId = emp.employeeId,
                    fullName = emp.fullName,
                    role = emp.role,
                    dateKey = st.dateKey,
                    status = st,
                    detail = detailById[devId]
                )
            }
        }

        baseRows.filter { row ->
            val matchesCompletion = when (completionFilter) {
                AdminCompletionFilter.ALL -> true
                AdminCompletionFilter.COMPLETED -> row.status != null
                AdminCompletionFilter.PENDING -> row.status == null
            }
            val matchesSearch = q.isEmpty() ||
                row.fullName.contains(q, ignoreCase = true) ||
                row.employeeId.contains(q, ignoreCase = true) ||
                row.role.contains(q, ignoreCase = true)
            matchesCompletion && matchesSearch
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
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Daily Devotionals",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Submission status & pastoral check-ins (${ManilaTime.formatShortDate(overviewDateKey)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LiveManilaClockPill(nowMillis = nowMillis)
            }
        }

        // Daily Overview KPI Cards (no scoring or ranking)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ElevatedCard(
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = palette.workingBg,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = palette.workingFg,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = completedCount.toString(),
                                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = JetBrainsMono),
                                color = palette.workingFg,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Completed",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                ElevatedCard(
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = palette.onBreakBg,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = palette.onBreakFg,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = pendingCount.toString(),
                                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = JetBrainsMono),
                                color = palette.onBreakFg,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Pending",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Privacy Policy Configuration Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PrivacyTip,
                                contentDescription = null,
                                tint = BrandAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Privacy Policy: ${privacyPolicy.title}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        OutlinedButton(
                            onClick = { showPolicyDialog = true },
                            modifier = Modifier.testTag("change_privacy_policy_button")
                        ) {
                            Text("Configure")
                        }
                    }
                    Text(
                        text = privacyPolicy.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!adminDevotionalFeed.isAuthorized && privacyPolicy != DevotionalPrivacyPolicy.STATUS_ONLY) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = BrandAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Sign in with the verified CEO/Admin Google account (374kingdombuilders@gmail.com) to inspect photos & shared SOAP entries.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Date Filter & Search Controls
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = dateFilterMode == AdminDevotionalDateFilter.TODAY,
                        onClick = { dateFilterMode = AdminDevotionalDateFilter.TODAY },
                        label = { Text("Today") }
                    )
                    FilterChip(
                        selected = dateFilterMode == AdminDevotionalDateFilter.YESTERDAY,
                        onClick = { dateFilterMode = AdminDevotionalDateFilter.YESTERDAY },
                        label = { Text("Yesterday") }
                    )
                    FilterChip(
                        selected = dateFilterMode == AdminDevotionalDateFilter.CUSTOM_DATE,
                        onClick = { showDatePicker = true },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                if (dateFilterMode == AdminDevotionalDateFilter.CUSTOM_DATE) {
                                    ManilaTime.formatShortDate(customDateKey)
                                } else {
                                    "Pick Date"
                                }
                            )
                        }
                    )
                    FilterChip(
                        selected = dateFilterMode == AdminDevotionalDateFilter.ALL_HISTORY,
                        onClick = {
                            dateFilterMode = AdminDevotionalDateFilter.ALL_HISTORY
                            completionFilter = AdminCompletionFilter.ALL
                        },
                        label = { Text("All History") }
                    )
                }

                if (dateFilterMode != AdminDevotionalDateFilter.ALL_HISTORY) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AdminCompletionFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = completionFilter == filter,
                                onClick = { completionFilter = filter },
                                label = { Text(filter.label) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search employee name or ID…") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Outlined.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_devotionals_search")
                )
            }
        }

        if (rows.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No devotional records match this filter.",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        } else {
            items(
                items = rows,
                key = { "${it.employeeUid}_${it.dateKey}" }
            ) { row ->
                val status = row.status
                val policyAllowsView = when (privacyPolicy) {
                    DevotionalPrivacyPolicy.STATUS_ONLY -> false
                    DevotionalPrivacyPolicy.OPT_IN_ONLY -> status?.shareWithAdmin == true
                    DevotionalPrivacyPolicy.ADMIN_PASTORAL_VIEW -> true
                }
                val canInspectSoap = status != null &&
                    policyAllowsView &&
                    adminDevotionalFeed.isAuthorized &&
                    row.detail != null

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .let { base ->
                            if (canInspectSoap && row.detail != null) {
                                base.clickable { detailTarget = row.detail }
                            } else {
                                base
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            EmployeeAvatar(fullName = row.fullName, isActive = true)
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = row.fullName,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${row.employeeId} • ${ManilaTime.formatShortDate(row.dateKey)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (status != null) {
                                Surface(
                                    color = palette.workingBg,
                                    shape = RoundedCornerShape(999.dp)
                                ) {
                                    Text(
                                        text = "Submitted",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.workingFg,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    color = palette.onBreakBg,
                                    shape = RoundedCornerShape(999.dp)
                                ) {
                                    Text(
                                        text = "Pending",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.onBreakFg,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        if (status != null) {
                            val timeText = status.submittedAt?.toDate()?.time?.let {
                                ManilaTime.formatTime12h(it)
                            } ?: "Verified"

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Submitted at $timeText PHT",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (canInspectSoap && row.detail != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = BrandAmber,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "View Photo & SOAP",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = BrandAmber,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                } else {
                                    val lockReason = when {
                                        privacyPolicy == DevotionalPrivacyPolicy.STATUS_ONLY -> "Private (Status Only)"
                                        privacyPolicy == DevotionalPrivacyPolicy.OPT_IN_ONLY && !status.shareWithAdmin -> "Private (Not Opted In)"
                                        !adminDevotionalFeed.isAuthorized -> "Admin Google Auth Required"
                                        else -> "Private"
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = lockReason,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (canInspectSoap && row.detail != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    var thumbFailed by remember(row.detail.photoUrl) { mutableStateOf(false) }
                                    val thumbModel = remember(row.detail.photoUrl) {
                                        PhotoUploadHelper.resolvePreviewModel(row.detail.photoUrl)
                                    }
                                    if (thumbModel != null && !thumbFailed) {
                                        AsyncImage(
                                            model = thumbModel,
                                            contentDescription = "Photo preview for ${row.fullName}",
                                            contentScale = ContentScale.Crop,
                                            onError = { thumbFailed = true },
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                        )
                                    } else {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.size(54.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Photo,
                                                    contentDescription = "Image attached",
                                                    tint = BrandAmber
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = row.detail.scripture,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }

    if (showDatePicker) {
        val initialMillis = ManilaTime.parseDateKey(customDateKey) ?: nowMillis
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { picked ->
                            customDateKey = ManilaTime.dateKey(picked)
                            dateFilterMode = AdminDevotionalDateFilter.CUSTOM_DATE
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Apply")
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

    if (showPolicyDialog) {
        var selected by remember(privacyPolicy) { mutableStateOf(privacyPolicy) }
        AlertDialog(
            onDismissRequest = { showPolicyDialog = false },
            title = { Text("Devotional Privacy Policy") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DevotionalPrivacyPolicy.entries.forEach { policy ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable { selected = policy }
                                .padding(8.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = selected == policy,
                                onClick = { selected = policy }
                            )
                            Column {
                                Text(
                                    text = policy.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = policy.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdatePrivacyPolicy(selected)
                        showPolicyDialog = false
                    }
                ) {
                    Text("Save Policy")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPolicyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    detailTarget?.let { dev ->
        DevotionalDetailDialog(
            devotional = dev,
            onDismiss = { detailTarget = null }
        )
    }
}
