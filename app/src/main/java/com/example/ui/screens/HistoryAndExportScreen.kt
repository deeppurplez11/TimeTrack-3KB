package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AttendanceWithBreaks
import com.example.data.EmployeeDaySummary
import com.example.data.EmployeeEntity
import com.example.ui.components.AttendanceDetailDialog
import com.example.ui.components.ResetAttendanceFlowDialogs
import com.example.ui.components.StatusBadge
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandCharcoal
import com.example.ui.theme.JetBrainsMono
import com.example.util.CsvExportScope
import com.example.util.FileShareHelper
import com.example.util.ManilaTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryAndExportScreen(
    employees: List<EmployeeEntity>,
    records: List<AttendanceWithBreaks>,
    nowMillis: Long,
    onBuildCsv: (
        scope: CsvExportScope,
        weekOffset: Int,
        customStartKey: String,
        customEndKey: String,
        selectedEmployeeUid: String?
    ) -> Pair<String, String>,
    onNotify: (String, Boolean) -> Unit,
    onResetAttendance: (employeeUid: String, dateKey: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val todayKey = ManilaTime.dateKey(nowMillis)

    var exportScope by remember { mutableStateOf(CsvExportScope.TODAY) }
    var selectedEmpUid by remember { mutableStateOf<String?>(null) } // null = All Employees
    var customStartKey by remember { mutableStateOf(ManilaTime.addDays(todayKey, -6)) }
    var customEndKey by remember { mutableStateOf(todayKey) }
    var pickingStart by remember { mutableStateOf<Boolean?>(null) } // true=start, false=end

    var pendingCsvContent by remember { mutableStateOf("") }
    var previewDialogData by remember { mutableStateOf<Pair<String, String>?>(null) }

    var detailTarget by remember { mutableStateOf<EmployeeDaySummary?>(null) }
    var resetTarget by remember { mutableStateOf<Pair<EmployeeEntity, String>?>(null) }

    val staffList = remember(employees) {
        employees.filter { !it.admin }.sortedBy { it.employeeId }
    }
    val empByUid = remember(employees) {
        employees.associateBy { it.uid }
    }

    val saveCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(pendingCsvContent.toByteArray(Charsets.UTF_8))
                }
                onNotify("CSV exported successfully.", false)
            } catch (e: Exception) {
                onNotify("Failed to save CSV: ${e.message}", true)
            }
        }
    }

    val filteredHistory = remember(records, selectedEmpUid) {
        records.filter { rec ->
            val emp = empByUid[rec.record.employeeUid]
            emp != null && !emp.admin && (selectedEmpUid == null || emp.uid == selectedEmpUid)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "History & CSV Export",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Excel & Google Sheets compatible UTF-8 CSV (Asia/Manila)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // CSV Export Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Export Attendance CSV",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        text = "1. Date Scope",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CsvExportScope.entries.forEach { scope ->
                            FilterChip(
                                selected = exportScope == scope,
                                onClick = { exportScope = scope },
                                label = { Text(scope.label) },
                                modifier = Modifier.testTag("csv_scope_${scope.name.lowercase()}")
                            )
                        }
                    }

                    if (exportScope == CsvExportScope.CUSTOM_RANGE) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { pickingStart = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("From: ${ManilaTime.formatShortDate(customStartKey)}")
                            }
                            OutlinedButton(
                                onClick = { pickingStart = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("To: ${ManilaTime.formatShortDate(customEndKey)}")
                            }
                        }
                    }

                    Text(
                        text = "2. Employee Filter",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedEmpUid == null,
                            onClick = { selectedEmpUid = null },
                            label = { Text("All Employees") },
                            modifier = Modifier.testTag("csv_filter_all_employees")
                        )
                        staffList.forEach { emp ->
                            FilterChip(
                                selected = selectedEmpUid == emp.uid,
                                onClick = { selectedEmpUid = emp.uid },
                                label = { Text("${emp.fullName} (${emp.employeeId})") }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val (filename, csv) = onBuildCsv(
                                    exportScope,
                                    0,
                                    customStartKey,
                                    customEndKey,
                                    selectedEmpUid
                                )
                                pendingCsvContent = csv
                                saveCsvLauncher.launch(filename)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandAmber,
                                contentColor = BrandCharcoal
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("save_csv_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save CSV", fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = {
                                val (filename, csv) = onBuildCsv(
                                    exportScope,
                                    0,
                                    customStartKey,
                                    customEndKey,
                                    selectedEmpUid
                                )
                                FileShareHelper.shareTextFile(
                                    context = context,
                                    filename = filename,
                                    mimeType = "text/csv",
                                    content = csv,
                                    chooserTitle = "Share 3KB Attendance CSV"
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("share_csv_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share CSV")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val (filename, csv) = onBuildCsv(
                                exportScope,
                                0,
                                customStartKey,
                                customEndKey,
                                selectedEmpUid
                            )
                            previewDialogData = filename to csv
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("preview_csv_button")
                    ) {
                        Icon(Icons.Default.Preview, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Preview CSV Output")
                    }
                }
            }
        }

        // Attendance History Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Attendance History (${filteredHistory.size})",
                    style = MaterialTheme.typography.titleLarge
                )
                if (selectedEmpUid != null) {
                    TextButton(onClick = { selectedEmpUid = null }) {
                        Text("Show All")
                    }
                }
            }
        }

        if (filteredHistory.isEmpty()) {
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
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("No attendance records found", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        } else {
            items(filteredHistory, key = { it.record.id }) { item ->
                val emp = empByUid[item.record.employeeUid] ?: return@items
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable {
                            detailTarget = EmployeeDaySummary(
                                employee = emp,
                                dateKey = item.record.dateKey,
                                attendance = item
                            )
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                            Column {
                                Text(
                                    text = emp.fullName,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "${emp.employeeId} • ${ManilaTime.formatShortDate(item.record.dateKey)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusBadge(status = item.attendanceStatus, compact = true)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "In ${ManilaTime.formatTime12h(item.record.clockInMillis)} • Out ${ManilaTime.formatTime12h(item.record.clockOutMillis)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Net ${ManilaTime.formatDurationCompact(item.netWorkedMillis(nowMillis))} • Brk ${ManilaTime.formatDurationCompact(item.totalBreakMillis(nowMillis))}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    pickingStart?.let { isStart ->
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = ManilaTime.parseDateKey(if (isStart) customStartKey else customEndKey)
                ?: nowMillis
        )
        DatePickerDialog(
            onDismissRequest = { pickingStart = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ms = datePickerState.selectedDateMillis
                        if (ms != null) {
                            val key = ManilaTime.dateKey(ms)
                            if (isStart) customStartKey = key else customEndKey = key
                        }
                        pickingStart = null
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { pickingStart = null }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    previewDialogData?.let { (filename, csv) ->
        AlertDialog(
            onDismissRequest = { previewDialogData = null },
            title = { Text("CSV Preview") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = filename,
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMono),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    ) {
                        Text(
                            text = csv.removePrefix("\uFEFF"),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono),
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .horizontalScroll(rememberScrollState())
                                .padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { previewDialogData = null }) {
                    Text("Close")
                }
            }
        )
    }

    detailTarget?.let { target ->
        AttendanceDetailDialog(
            employee = target.employee,
            dateKey = target.dateKey,
            attendance = target.attendance,
            nowMillis = nowMillis,
            onDismiss = { detailTarget = null },
            onRequestReset = {
                val pair = target.employee to target.dateKey
                detailTarget = null
                resetTarget = pair
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
