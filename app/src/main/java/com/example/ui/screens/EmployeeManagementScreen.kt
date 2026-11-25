package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.EmployeeEntity
import com.example.ui.components.EmployeeAvatar
import com.example.ui.components.ResetAttendanceFlowDialogs
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandCharcoal
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalStatusPalette
import com.example.util.FileShareHelper
import com.example.util.ManilaTime
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmployeeManagementScreen(
    employees: List<EmployeeEntity>,
    nowMillis: Long,
    suggestedNextId: () -> String,
    onAddEmployee: (fullName: String, employeeId: String, role: String, pin: String, isActive: Boolean) -> Unit,
    onUpdateEmployee: (uid: String, fullName: String, employeeId: String, role: String) -> Unit,
    onResetPin: (uid: String, newPin: String) -> Unit,
    onToggleActive: (uid: String, active: Boolean) -> Unit,
    onDeleteEmployee: (uid: String) -> Unit,
    onResetAttendance: (uid: String, dateKey: String) -> Unit,
    onCreateBackupJson: suspend () -> String,
    onRestoreBackupJson: (String) -> Unit,
    onNotify: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = LocalStatusPalette.current

    var searchQuery by remember { mutableStateOf("") }
    var showInactive by remember { mutableStateOf(true) }

    var showAddDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<EmployeeEntity?>(null) }
    var pinTarget by remember { mutableStateOf<EmployeeEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<EmployeeEntity?>(null) }
    var resetTimeTarget by remember { mutableStateOf<EmployeeEntity?>(null) }

    var pendingBackupJson by remember { mutableStateOf("") }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }

    val saveBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(pendingBackupJson.toByteArray(Charsets.UTF_8))
                }
                onNotify("Database backup saved successfully.", false)
            } catch (e: Exception) {
                onNotify("Failed to save backup: ${e.message}", true)
            }
        }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                if (!content.isNullOrBlank()) {
                    pendingRestoreJson = content
                } else {
                    onNotify("Selected backup file is empty.", true)
                }
            } catch (e: Exception) {
                onNotify("Unable to read backup file: ${e.message}", true)
            }
        }
    }

    val filteredEmployees = remember(employees, searchQuery, showInactive) {
        val q = searchQuery.trim()
        employees.filter { emp ->
            val matchesActive = showInactive || emp.active
            val matchesQuery = q.isEmpty() ||
                emp.fullName.contains(q, ignoreCase = true) ||
                emp.employeeId.contains(q, ignoreCase = true) ||
                emp.role.contains(q, ignoreCase = true)
            matchesActive && matchesQuery
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header + Add Employee Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Team & Data Management",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "${employees.count { !it.admin && it.active }} active • ${employees.count { !it.admin && !it.active }} deactivated",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandAmber,
                        contentColor = BrandCharcoal
                    ),
                    modifier = Modifier.testTag("add_employee_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Employee", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Cloud Firestore Sync & JSON Backup Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                            text = "Backup & Restore",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Surface(
                            color = palette.workingBg,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = palette.workingFg,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Auto-Saved",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.workingFg,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    Text(
                        text = "Attendance is saved automatically. Download or restore a backup file anytime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                scope.launch {
                                    pendingBackupJson = onCreateBackupJson()
                                    saveBackupLauncher.launch("3KB_TimeTrack_Backup_${ManilaTime.dateKey()}.json")
                                }
                            },
                            modifier = Modifier.testTag("backup_save_button")
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Backup (.json)")
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val json = onCreateBackupJson()
                                    FileShareHelper.shareTextFile(
                                        context = context,
                                        filename = "3KB_TimeTrack_Backup_${ManilaTime.dateKey()}.json",
                                        mimeType = "application/json",
                                        content = json,
                                        chooserTitle = "Share 3KB Database Backup"
                                    )
                                }
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share Backup")
                        }
                        OutlinedButton(
                            onClick = {
                                openBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                            modifier = Modifier.testTag("restore_backup_button")
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Restore Backup")
                        }
                    }
                }
            }
        }

        // Search & Filter Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search employees") },
                    placeholder = { Text("Name, ID, or role") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = showInactive,
                        onClick = { showInactive = true },
                        label = { Text("All Accounts") }
                    )
                    FilterChip(
                        selected = !showInactive,
                        onClick = { showInactive = false },
                        label = { Text("Active Only") }
                    )
                }
            }
        }

        // Employee Cards
        items(filteredEmployees, key = { it.uid }) { emp ->
            EmployeeManagementCard(
                employee = emp,
                onEdit = { editTarget = emp },
                onResetPin = { pinTarget = emp },
                onToggleActive = { onToggleActive(emp.uid, !emp.active) },
                onDelete = { deleteTarget = emp },
                onResetTime = { resetTimeTarget = emp }
            )
        }
    }

    if (showAddDialog) {
        EmployeeFormDialog(
            title = "Add New Employee",
            initialName = "",
            initialEmployeeId = suggestedNextId(),
            initialRole = "",
            includePinField = true,
            includeActiveToggle = true,
            onDismiss = { showAddDialog = false },
            onSubmit = { name, id, role, pin, active ->
                onAddEmployee(name, id, role, pin, active)
                showAddDialog = false
            }
        )
    }

    editTarget?.let { emp ->
        EmployeeFormDialog(
            title = "Edit Employee Info",
            initialName = emp.fullName,
            initialEmployeeId = emp.employeeId,
            initialRole = emp.role,
            includePinField = false,
            includeActiveToggle = false,
            onDismiss = { editTarget = null },
            onSubmit = { name, id, role, _, _ ->
                onUpdateEmployee(emp.uid, name, id, role)
                editTarget = null
            }
        )
    }

    pinTarget?.let { emp ->
        ResetPinDialog(
            employee = emp,
            onDismiss = { pinTarget = null },
            onConfirm = { newPin ->
                onResetPin(emp.uid, newPin)
                pinTarget = null
            }
        )
    }

    deleteTarget?.let { emp ->
        DeleteEmployeeConfirmDialog(
            employee = emp,
            onDismiss = { deleteTarget = null },
            onDeactivateInstead = {
                onToggleActive(emp.uid, false)
                deleteTarget = null
            },
            onConfirmDelete = {
                onDeleteEmployee(emp.uid)
                deleteTarget = null
            }
        )
    }

    resetTimeTarget?.let { emp ->
        ResetAttendanceFlowDialogs(
            employee = emp,
            nowMillis = nowMillis,
            onDismiss = { resetTimeTarget = null },
            onConfirmReset = onResetAttendance
        )
    }

    pendingRestoreJson?.let { json ->
        AlertDialog(
            onDismissRequest = { pendingRestoreJson = null },
            icon = { Icon(Icons.Default.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Restore Database Backup?") },
            text = {
                Text("Restoring this backup will replace your current employees, attendance logs, and break records with the data in the backup file.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreBackupJson(json)
                        pendingRestoreJson = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Restore Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreJson = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmployeeManagementCard(
    employee: EmployeeEntity,
    onEdit: () -> Unit,
    onResetPin: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit,
    onResetTime: () -> Unit
) {
    val palette = LocalStatusPalette.current
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    EmployeeAvatar(fullName = employee.fullName, isActive = employee.active, size = 42.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = employee.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${employee.employeeId} • ${employee.role}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    color = if (employee.admin) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else if (employee.active) {
                        palette.workingBg
                    } else {
                        palette.notClockedInBg
                    },
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = when {
                            employee.admin -> "Admin"
                            employee.active -> "Active"
                            else -> "Deactivated"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            employee.admin -> MaterialTheme.colorScheme.onPrimaryContainer
                            employee.active -> palette.workingFg
                            else -> palette.notClockedInFg
                        },
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Edit")
                }

                OutlinedButton(
                    onClick = onResetPin,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Set PIN")
                }

                if (!employee.admin) {
                    OutlinedButton(
                        onClick = onResetTime,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("reset_time_${employee.employeeId}")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Reset Time")
                    }

                    OutlinedButton(
                        onClick = onToggleActive,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (employee.active) Icons.Default.PersonOff else Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(if (employee.active) "Deactivate" else "Activate")
                    }

                    OutlinedButton(
                        onClick = onDelete,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.testTag("delete_employee_${employee.employeeId}")
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Delete Employee")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployeeFormDialog(
    title: String,
    initialName: String,
    initialEmployeeId: String,
    initialRole: String,
    includePinField: Boolean,
    includeActiveToggle: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (name: String, employeeId: String, role: String, pin: String, isActive: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var empId by remember { mutableStateOf(initialEmployeeId) }
    var role by remember { mutableStateOf(initialRole) }
    var pin by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(true) }

    val canSave = name.isNotBlank() && empId.isNotBlank() &&
        (!includePinField || (pin.length in 4..6 && pin.all { it.isDigit() }))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = empId,
                    onValueChange = { empId = it },
                    label = { Text("Employee ID") },
                    placeholder = { Text("e.g. 3KB-007") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Role / Position") },
                    placeholder = { Text("e.g. Software Engineer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (includePinField) {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { input ->
                            if (input.length <= 6 && input.all { it.isDigit() }) pin = input
                        },
                        label = { Text("Initial PIN (4–6 digits)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (includeActiveToggle) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Employee", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(name, empId, role, pin, active) },
                enabled = canSave
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ResetPinDialog(
    employee: EmployeeEntity,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newPin by remember { mutableStateOf("") }
    val valid = newPin.length in 4..6 && newPin.all { it.isDigit() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set / Reset PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Set a new 4–6 digit numeric PIN for ${employee.fullName} (${employee.employeeId}).",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { input ->
                        if (input.length <= 6 && input.all { it.isDigit() }) newPin = input
                    },
                    label = { Text("New PIN") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(newPin) }, enabled = valid) {
                Text("Update PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DeleteEmployeeConfirmDialog(
    employee: EmployeeEntity,
    onDismiss: () -> Unit,
    onDeactivateInstead: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Delete Employee?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "You are about to permanently delete ${employee.fullName} (${employee.employeeId}).",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "Warning: Deleting this employee will also permanently delete all associated attendance records and break logs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                if (employee.active) {
                    Text(
                        text = "Safer alternative: Deactivate Employee removes them from active lists while preserving all historical attendance data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalButton(
                        onClick = onDeactivateInstead,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("deactivate_instead_button")
                    ) {
                        Icon(Icons.Default.PersonOff, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Deactivate Instead (Keep History)")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.testTag("confirm_delete_employee_button")
            ) {
                Text("Delete Permanently")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_delete_employee_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
