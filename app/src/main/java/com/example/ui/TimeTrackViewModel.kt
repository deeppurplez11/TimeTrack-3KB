package com.example.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ActionResult
import com.example.data.AdminDevotionalFeed
import com.example.data.AttendanceWithBreaks
import com.example.data.DevotionalEntity
import com.example.data.DevotionalPrivacyPolicy
import com.example.data.DevotionalStatusEntity
import com.example.data.EmployeeEntity
import com.example.data.TimeTrackRepository
import com.example.util.BackupManager
import com.example.util.CsvExportScope
import com.example.util.CsvExporter
import com.example.util.ManilaTime
import com.example.util.PhotoUploadHelper
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed class SessionState {
    data object LoggedOut : SessionState()
    data class EmployeeSession(val employeeUid: String) : SessionState()
    data class AdminSession(val adminUid: String) : SessionState()
}

data class FeedbackBanner(
    val message: String,
    val isError: Boolean = false
)

sealed interface DevotionalPhotoUploadState {
    data object Idle : DevotionalPhotoUploadState
    data class Uploading(val localUri: String, val progress: Float) : DevotionalPhotoUploadState
    data class Uploaded(
        val localUri: String,
        val storagePath: String,
        val downloadUrl: String,
        val dateKey: String
    ) : DevotionalPhotoUploadState
    data class Error(val localUri: String, val message: String) : DevotionalPhotoUploadState
}

data class WorkspaceData(
    val employees: List<EmployeeEntity> = emptyList(),
    val attendanceRecords: List<AttendanceWithBreaks> = emptyList(),
    val devotionalStatuses: List<DevotionalStatusEntity> = emptyList(),
    val privacyPolicy: DevotionalPrivacyPolicy = DevotionalPrivacyPolicy.OPT_IN_ONLY
)

class TimeTrackViewModel(
    private val repository: TimeTrackRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _nowMillis = MutableStateFlow(System.currentTimeMillis())
    val nowMillis: StateFlow<Long> = _nowMillis.asStateFlow()

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.LoggedOut)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _isSeedReady = MutableStateFlow(false)
    val isSeedReady: StateFlow<Boolean> = _isSeedReady.asStateFlow()

    private val _photoUploadState = MutableStateFlow<DevotionalPhotoUploadState>(DevotionalPhotoUploadState.Idle)
    val photoUploadState: StateFlow<DevotionalPhotoUploadState> = _photoUploadState.asStateFlow()

    private val _isSubmittingDevotional = MutableStateFlow(false)
    val isSubmittingDevotional: StateFlow<Boolean> = _isSubmittingDevotional.asStateFlow()

    private var photoUploadJob: Job? = null

    private val _feedback = MutableSharedFlow<FeedbackBanner>(extraBufferCapacity = 8)
    val feedback: SharedFlow<FeedbackBanner> = _feedback.asSharedFlow()

    val workspaceState: StateFlow<UiState<WorkspaceData>> = combine(
        repository.observeEmployees(currentUserId),
        repository.observeAllAttendanceWithBreaks(currentUserId),
        repository.observeDevotionalStatuses(currentUserId),
        repository.observePrivacyPolicy(currentUserId)
    ) { emps, att, devStatuses, policy ->
        UiState.Success(
            WorkspaceData(
                employees = emps,
                attendanceRecords = att,
                devotionalStatuses = devStatuses,
                privacyPolicy = policy
            )
        ) as UiState<WorkspaceData>
    }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing Firestore workspace", rootError)
            emit(UiState.Error(rootError.message ?: "Unable to sync data from Firebase Firestore."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val myDevotionals: StateFlow<List<DevotionalEntity>> = repository.observeMyDevotionals(currentUserId)
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Unable to observe personal devotionals", error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminDevotionalFeed: StateFlow<AdminDevotionalFeed> = repository.observeAdminDevotionals(currentUserId)
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Admin devotional feed restricted or unavailable", error)
            emit(AdminDevotionalFeed(records = emptyList(), isAuthorized = false, errorMessage = error.localizedMessage))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AdminDevotionalFeed())

    val employees: StateFlow<List<EmployeeEntity>> = workspaceState
        .map { state -> (state as? UiState.Success)?.data?.employees.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val attendanceRecords: StateFlow<List<AttendanceWithBreaks>> = workspaceState
        .map { state -> (state as? UiState.Success)?.data?.attendanceRecords.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val currentEmployee: StateFlow<EmployeeEntity?> = combine(
        _sessionState,
        employees
    ) { session, list ->
        when (session) {
            is SessionState.EmployeeSession -> list.firstOrNull { it.uid == session.employeeUid }
            is SessionState.AdminSession -> list.firstOrNull { it.uid == session.adminUid }
            SessionState.LoggedOut -> null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    init {
        viewModelScope.launch {
            val res = repository.ensureSeeded(currentUserId)
            if (res is ActionResult.Error) {
                _feedback.tryEmit(FeedbackBanner(res.message, isError = true))
            }
            _isSeedReady.value = true
        }
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                _nowMillis.value = System.currentTimeMillis()
            }
        }
    }

    fun clearLoginError() {
        _loginError.value = null
    }

    fun loginEmployee(identifier: String, pin: String) {
        viewModelScope.launch {
            when (val res = repository.authenticateEmployee(identifier, pin)) {
                is ActionResult.Success -> {
                    _loginError.value = null
                    _sessionState.value = SessionState.EmployeeSession(res.data.uid)
                    _feedback.tryEmit(FeedbackBanner(res.message, isError = false))
                }
                is ActionResult.Error -> {
                    _loginError.value = res.message
                }
            }
        }
    }

    fun loginAdmin(pin: String) {
        viewModelScope.launch {
            when (val res = repository.authenticateAdmin(pin)) {
                is ActionResult.Success -> {
                    _loginError.value = null
                    _sessionState.value = SessionState.AdminSession(res.data.uid)
                    _feedback.tryEmit(FeedbackBanner(res.message, isError = false))
                }
                is ActionResult.Error -> {
                    _loginError.value = res.message
                }
            }
        }
    }

    fun logoutPortal() {
        clearDevotionalPhoto()
        _loginError.value = null
        _sessionState.value = SessionState.LoggedOut
    }

    fun uploadDevotionalPhoto(context: Context, uri: Uri, employeeUid: String) {
        val appContext = context.applicationContext
        val uriString = uri.toString()
        photoUploadJob?.cancel()
        photoUploadJob = viewModelScope.launch {
            val todayKey = ManilaTime.dateKey()
            _photoUploadState.value = DevotionalPhotoUploadState.Uploading(uriString, 0.05f)
            try {
                val uploaded = PhotoUploadHelper.uploadDevotionalPhoto(
                    context = appContext,
                    sourceUri = uri,
                    employeeUid = employeeUid,
                    dateKey = todayKey,
                    onProgress = { progress ->
                        _photoUploadState.value = DevotionalPhotoUploadState.Uploading(uriString, progress)
                    }
                )
                _photoUploadState.value = DevotionalPhotoUploadState.Uploaded(
                    localUri = uriString,
                    storagePath = uploaded.storagePath,
                    downloadUrl = uploaded.downloadUrl,
                    dateKey = todayKey
                )
                _feedback.tryEmit(FeedbackBanner("Devotional photo uploaded.", isError = false))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Devotional photo upload failed", e)
                val msg = e.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "Photo upload failed. Check internet connection & Firebase Storage configuration, then retry."
                _photoUploadState.value = DevotionalPhotoUploadState.Error(uriString, msg)
                _feedback.tryEmit(FeedbackBanner(msg, isError = true))
            }
        }
    }

    fun clearDevotionalPhoto() {
        photoUploadJob?.cancel()
        photoUploadJob = null
        _photoUploadState.value = DevotionalPhotoUploadState.Idle
    }

    fun submitDevotional(
        employee: EmployeeEntity,
        scripture: String,
        observation: String,
        application: String,
        prayer: String,
        shareWithAdmin: Boolean
    ) {
        val uploaded = _photoUploadState.value as? DevotionalPhotoUploadState.Uploaded
        if (uploaded == null) {
            _feedback.tryEmit(
                FeedbackBanner(
                    "Please upload and finish verifying your devotional photo first.",
                    isError = true
                )
            )
            return
        }
        if (_isSubmittingDevotional.value) return

        viewModelScope.launch {
            _isSubmittingDevotional.value = true
            try {
                val dateKey = ManilaTime.dateKey()
                val res = repository.submitDevotional(
                    employee = employee,
                    dateKey = dateKey,
                    scripture = scripture,
                    observation = observation,
                    application = application,
                    prayer = prayer,
                    photoStoragePath = uploaded.storagePath,
                    photoUrl = uploaded.downloadUrl,
                    shareWithAdmin = shareWithAdmin
                )
                emitResult(res)
                if (res is ActionResult.Success) {
                    _photoUploadState.value = DevotionalPhotoUploadState.Idle
                }
            } finally {
                _isSubmittingDevotional.value = false
            }
        }
    }

    fun updateDevotionalPrivacyPolicy(policy: DevotionalPrivacyPolicy) {
        viewModelScope.launch {
            emitResult(repository.updateDevotionalPrivacyPolicy(policy))
        }
    }

    fun clockIn(employeeUid: String) {
        viewModelScope.launch {
            emitResult(repository.clockIn(employeeUid))
        }
    }

    fun startBreak(employeeUid: String) {
        viewModelScope.launch {
            emitResult(repository.startBreak(employeeUid))
        }
    }

    fun endBreak(employeeUid: String) {
        viewModelScope.launch {
            emitResult(repository.endBreak(employeeUid))
        }
    }

    fun clockOut(employeeUid: String) {
        viewModelScope.launch {
            emitResult(repository.clockOut(employeeUid))
        }
    }

    fun addEmployee(
        fullName: String,
        employeeId: String,
        role: String,
        pin: String,
        active: Boolean
    ) {
        viewModelScope.launch {
            emitResult(repository.addEmployee(fullName, employeeId, role, pin, active))
        }
    }

    fun updateEmployeeInfo(
        uid: String,
        fullName: String,
        employeeId: String,
        role: String
    ) {
        viewModelScope.launch {
            emitResult(repository.updateEmployeeInfo(uid, fullName, employeeId, role))
        }
    }

    fun resetPin(uid: String, newPin: String) {
        viewModelScope.launch {
            emitResult(repository.resetPin(uid, newPin))
        }
    }

    fun toggleEmployeeActive(uid: String, active: Boolean) {
        viewModelScope.launch {
            emitResult(repository.toggleEmployeeActive(uid, active))
        }
    }

    fun deleteEmployee(uid: String) {
        viewModelScope.launch {
            emitResult(repository.deleteEmployee(uid))
        }
    }

    fun resetAttendanceForDate(employeeUid: String, dateKey: String) {
        viewModelScope.launch {
            emitResult(repository.resetAttendanceForDate(employeeUid, dateKey))
        }
    }

    fun nextSuggestedEmployeeId(): String {
        val maxNum = employees.value
            .mapNotNull { emp ->
                val digits = emp.employeeId.substringAfterLast("-", "")
                digits.toIntOrNull()
            }
            .maxOrNull() ?: 0
        val next = (maxNum + 1).coerceAtLeast(1)
        return "3KB-${next.toString().padStart(3, '0')}"
    }

    fun buildCsvContent(
        scope: CsvExportScope,
        weekOffset: Int,
        customStartKey: String,
        customEndKey: String,
        selectedEmployeeUid: String?
    ): Pair<String, String> {
        val now = _nowMillis.value
        val todayKey = ManilaTime.dateKey(now)
        val dateKeys = when (scope) {
            CsvExportScope.TODAY -> listOf(todayKey)
            CsvExportScope.WEEKLY -> ManilaTime.getWeekDateKeys(weekOffset, now)
            CsvExportScope.MONTHLY -> {
                val (start, end) = ManilaTime.getCurrentMonthRange(now)
                expandDateRange(start, end)
            }
            CsvExportScope.CUSTOM_RANGE -> expandDateRange(customStartKey, customEndKey)
        }

        val csv = CsvExporter.generateCsv(
            employees = employees.value,
            records = attendanceRecords.value,
            dateKeys = dateKeys,
            selectedEmployeeUid = selectedEmployeeUid,
            nowMillis = now,
            includeNotClockedInForDates = (scope == CsvExportScope.TODAY)
        )
        val selectedEmpCode = employees.value.firstOrNull { it.uid == selectedEmployeeUid }?.employeeId
        val filename = CsvExporter.buildSuggestedFilename(scope, selectedEmpCode)
        return filename to csv
    }

    private fun expandDateRange(startKey: String, endKey: String): List<String> {
        val start = if (startKey <= endKey) startKey else endKey
        val end = if (startKey <= endKey) endKey else startKey
        val list = mutableListOf<String>()
        var cursor = start
        var guard = 0
        while (cursor <= end && guard < 366) {
            list.add(cursor)
            cursor = ManilaTime.addDays(cursor, 1)
            guard++
        }
        return list
    }

    suspend fun createBackupJson(): String {
        val (emps, att, brks) = repository.getFullSnapshot()
        return BackupManager.serialize(emps, att, brks)
    }

    fun restoreBackupJson(jsonText: String) {
        viewModelScope.launch {
            try {
                val snapshot = BackupManager.parse(jsonText)
                emitResult(
                    repository.restoreSnapshot(
                        employees = snapshot.employees,
                        attendance = snapshot.attendance,
                        breaks = snapshot.breaks
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _feedback.tryEmit(
                    FeedbackBanner(
                        "Invalid backup file: ${e.message ?: "Unable to parse JSON"}",
                        isError = true
                    )
                )
            }
        }
    }

    fun notifyInfo(message: String, isError: Boolean = false) {
        _feedback.tryEmit(FeedbackBanner(message, isError))
    }

    private fun emitResult(result: ActionResult<*>) {
        when (result) {
            is ActionResult.Success -> {
                if (result.message.isNotEmpty()) {
                    _feedback.tryEmit(FeedbackBanner(result.message, isError = false))
                }
            }
            is ActionResult.Error -> {
                _feedback.tryEmit(FeedbackBanner(result.message, isError = true))
            }
        }
    }

    private companion object {
        const val TAG = "TimeTrackVM"
    }
}
