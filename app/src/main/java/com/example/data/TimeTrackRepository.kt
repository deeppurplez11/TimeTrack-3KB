package com.example.data

import android.content.Context
import com.example.R
import com.example.util.ManilaTime
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class ActionResult<out T> {
    data class Success<T>(val data: T, val message: String = "") : ActionResult<T>()
    data class Error(val message: String) : ActionResult<Nothing>()
}

data class AdminDevotionalFeed(
    val records: List<DevotionalEntity> = emptyList(),
    val isAuthorized: Boolean = false,
    val errorMessage: String? = null
)

private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
    }
    addOnFailureListener { exc ->
        if (cont.isActive) cont.resumeWithException(exc)
    }
    addOnCanceledListener {
        cont.cancel()
    }
}

class TimeTrackRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        const val SHARED_WORKSPACE_ID = "3kb_main"
        const val WORKSPACE_DISPLAY_NAME = "374 Kingdom Builders"
    }

    @Volatile
    private var verifiedMemberUid: String? = null

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun workspaceDoc() = db.collection("workspaces").document(SHARED_WORKSPACE_ID)
    private fun membersCol() = workspaceDoc().collection("members")
    private fun settingsCol() = workspaceDoc().collection("settings")
    private fun employeesCol() = workspaceDoc().collection("employees")
    private fun devotionalStatusCol() = workspaceDoc().collection("devotional_status")
    private fun devotionalsCol() = workspaceDoc().collection("devotionals")
    private fun attendanceCol() = workspaceDoc().collection("attendance")
    private fun breaksCol() = workspaceDoc().collection("breaks")

    private suspend fun ensureWorkspaceMembership(uid: String) {
        if (verifiedMemberUid == uid) return

        val wsRef = workspaceDoc()
        val memberRef = membersCol().document(uid)

        val wsExists = wsRef.get().awaitTask().exists()
        val memberExists = memberRef.get().awaitTask().exists()

        if (!wsExists || !memberExists) {
            val batch = db.batch()
            if (!wsExists) {
                batch.set(
                    wsRef,
                    mapOf(
                        "ownerUid" to uid,
                        "workspaceId" to SHARED_WORKSPACE_ID,
                        "name" to WORKSPACE_DISPLAY_NAME,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            if (!memberExists) {
                batch.set(
                    memberRef,
                    mapOf(
                        "ownerUid" to uid,
                        "workspaceId" to SHARED_WORKSPACE_ID,
                        "role" to "member",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().awaitTask()
        }
        verifiedMemberUid = uid
    }

    fun observeEmployees(userId: String): Flow<List<EmployeeEntity>> = flow {
        require(userId.isNotBlank()) { "Authenticated userId required." }
        val path = "workspaces/$SHARED_WORKSPACE_ID/employees"
        emitAll(
            employeesCol()
                .whereEqualTo("workspaceId", SHARED_WORKSPACE_ID)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        EmployeeEntity::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(compareByDescending<EmployeeEntity> { it.admin }.thenBy { it.employeeId })
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeAllAttendanceWithBreaks(userId: String): Flow<List<AttendanceWithBreaks>> {
        require(userId.isNotBlank()) { "Authenticated userId required." }
        val attPath = "workspaces/$SHARED_WORKSPACE_ID/attendance"
        val brkPath = "workspaces/$SHARED_WORKSPACE_ID/breaks"

        val attFlow = attendanceCol()
            .whereEqualTo("workspaceId", SHARED_WORKSPACE_ID)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    AttendanceRecordEntity::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.dateKey }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val toLog = firestoreError ?: (error as? Exception)
                if (toLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(toLog, OperationType.LIST, attPath)
                }
                throw error
            }

        val brkFlow = breaksCol()
            .whereEqualTo("workspaceId", SHARED_WORKSPACE_ID)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    BreakRecordEntity::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val toLog = firestoreError ?: (error as? Exception)
                if (toLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(toLog, OperationType.LIST, brkPath)
                }
                throw error
            }

        return combine(attFlow, brkFlow) { attendanceList, breakList ->
            val breaksByAtt = breakList.groupBy { it.attendanceId }
            attendanceList.map { rec ->
                AttendanceWithBreaks(
                    record = rec,
                    breaks = breaksByAtt[rec.id].orEmpty().sortedBy { it.breakStartMillis }
                )
            }
        }
    }

    fun observePrivacyPolicy(userId: String): Flow<DevotionalPrivacyPolicy> = flow {
        require(userId.isNotBlank()) { "Authenticated userId required." }
        val path = "workspaces/$SHARED_WORKSPACE_ID/settings"
        emitAll(
            settingsCol()
                .whereEqualTo("workspaceId", SHARED_WORKSPACE_ID)
                .snapshots()
                .map { snapshot ->
                    val docs = snapshot.toObjects(
                        DevotionalPrivacySettingsEntity::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                    docs.firstOrNull { it.id == "devotional_privacy" }?.privacyPolicy
                        ?: DevotionalPrivacyPolicy.OPT_IN_ONLY
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeDevotionalStatuses(userId: String): Flow<List<DevotionalStatusEntity>> = flow {
        require(userId.isNotBlank()) { "Authenticated userId required." }
        val path = "workspaces/$SHARED_WORKSPACE_ID/devotional_status"
        emitAll(
            devotionalStatusCol()
                .whereEqualTo("workspaceId", SHARED_WORKSPACE_ID)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DevotionalStatusEntity::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<DevotionalStatusEntity> { it.dateKey }
                            .thenByDescending { it.submittedAt?.toDate()?.time ?: 0L }
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeMyDevotionals(userId: String): Flow<List<DevotionalEntity>> = flow {
        require(userId.isNotBlank()) { "Authenticated userId required." }
        val path = "workspaces/$SHARED_WORKSPACE_ID/devotionals"
        emitAll(
            devotionalsCol()
                .whereEqualTo("ownerUid", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DevotionalEntity::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).filter { it.workspaceId == SHARED_WORKSPACE_ID }
                        .sortedByDescending { it.dateKey }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (firestoreError?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        emit(emptyList())
                    } else {
                        val toLog = firestoreError ?: (error as? Exception)
                        if (toLog != null && auth.currentUser?.uid == userId) {
                            handleFirestoreError(toLog, OperationType.LIST, path)
                        }
                        throw error
                    }
                }
        )
    }

    fun observeAdminDevotionals(userId: String): Flow<AdminDevotionalFeed> = flow {
        require(userId.isNotBlank()) { "Authenticated userId required." }
        val path = "workspaces/$SHARED_WORKSPACE_ID/devotionals"
        emitAll(
            devotionalsCol()
                .whereEqualTo("workspaceId", SHARED_WORKSPACE_ID)
                .snapshots()
                .map { snapshot ->
                    val items = snapshot.toObjects(
                        DevotionalEntity::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.dateKey }
                    AdminDevotionalFeed(records = items, isAuthorized = true)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (firestoreError?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        emit(
                            AdminDevotionalFeed(
                                records = emptyList(),
                                isAuthorized = false,
                                errorMessage = "Private photo previews & SOAP reflections are restricted to verified CEO/Admin accounts."
                            )
                        )
                    } else {
                        val toLog = firestoreError ?: (error as? Exception)
                        if (toLog != null && auth.currentUser?.uid == userId) {
                            handleFirestoreError(toLog, OperationType.LIST, path)
                        }
                        emit(
                            AdminDevotionalFeed(
                                records = emptyList(),
                                isAuthorized = false,
                                errorMessage = error.localizedMessage ?: "Unable to load devotional details."
                            )
                        )
                    }
                }
        )
    }

    suspend fun ensureSeeded(userId: String): ActionResult<Unit> {
        val path = "workspaces/$SHARED_WORKSPACE_ID/employees"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            ensureWorkspaceMembership(uid)
            val existing = employeesCol()
                .whereEqualTo("workspaceId", SHARED_WORKSPACE_ID)
                .limit(1)
                .get()
                .awaitTask()
            if (existing.isEmpty) {
                SeedData.populateIfEmpty(db, SHARED_WORKSPACE_ID, uid) { batch ->
                    batch.commit().awaitTask()
                }
            } else {
                val privacyDoc = settingsCol().document("devotional_privacy").get().awaitTask()
                if (!privacyDoc.exists()) {
                    settingsCol().document("devotional_privacy").set(
                        mapOf(
                            "ownerUid" to uid,
                            "workspaceId" to SHARED_WORKSPACE_ID,
                            "policy" to DevotionalPrivacyPolicy.OPT_IN_ONLY.name,
                            "updatedByUid" to uid,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).awaitTask()
                }
            }
            cleanupOrphanedDevotionals()
            ActionResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            ActionResult.Error(e.localizedMessage ?: "Failed to initialize Firestore workspace.")
        }
    }

    private suspend fun cleanupOrphanedDevotionals() {
        val empUids = employeesCol()
            .whereEqualTo("workspaceId", SHARED_WORKSPACE_ID)
            .get()
            .awaitTask()
            .documents
            .mapTo(mutableSetOf()) { it.id }

        val orphanStatuses = devotionalStatusCol()
            .whereEqualTo("workspaceId", SHARED_WORKSPACE_ID)
            .get()
            .awaitTask()
            .documents
            .filter { doc ->
                val empUid = doc.getString("employeeUid").orEmpty()
                empUid.isNotBlank() && empUid !in empUids
            }

        if (orphanStatuses.isEmpty()) return

        orphanStatuses.map { it.reference }.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.delete(it) }
            batch.commit().awaitTask()
        }
        orphanStatuses.forEach { statusDoc ->
            runCatching {
                devotionalsCol().document(statusDoc.id).delete().awaitTask()
            }
        }
    }

    suspend fun submitDevotional(
        employee: EmployeeEntity,
        dateKey: String,
        scripture: String,
        observation: String,
        application: String,
        prayer: String,
        photoStoragePath: String,
        photoUrl: String,
        shareWithAdmin: Boolean
    ): ActionResult<Unit> {
        val cleanScripture = scripture.trim()
        val cleanObservation = observation.trim()
        val cleanApplication = application.trim()
        val cleanPrayer = prayer.trim()

        if (photoStoragePath.isBlank() || photoUrl.isBlank()) {
            return ActionResult.Error("Please upload your devotional photo before submitting.")
        }
        if (cleanScripture.isEmpty()) return ActionResult.Error("Scripture (S) is required.")
        if (cleanScripture.length > 1000) return ActionResult.Error("Scripture must be 1,000 characters or fewer.")
        if (cleanObservation.isEmpty()) return ActionResult.Error("Observation (O) is required.")
        if (cleanObservation.length > 2000) return ActionResult.Error("Observation must be 2,000 characters or fewer.")
        if (cleanApplication.isEmpty()) return ActionResult.Error("Application (A) is required.")
        if (cleanApplication.length > 2000) return ActionResult.Error("Application must be 2,000 characters or fewer.")
        if (cleanPrayer.isEmpty()) return ActionResult.Error("Prayer (P) is required.")
        if (cleanPrayer.length > 2000) return ActionResult.Error("Prayer must be 2,000 characters or fewer.")

        val devId = "${employee.uid}_$dateKey"
        val path = "workspaces/$SHARED_WORKSPACE_ID/devotionals/$devId"

        return try {
            val uid = requireUserId()
            ensureWorkspaceMembership(uid)

            val existingStatus = devotionalStatusCol().document(devId).get().awaitTask()
            if (existingStatus.exists()) {
                return ActionResult.Error("Today's devotional has already been submitted.")
            }

            val devotionalPayload = mapOf(
                "ownerUid" to uid,
                "workspaceId" to SHARED_WORKSPACE_ID,
                "employeeUid" to employee.uid,
                "employeeId" to employee.employeeId,
                "employeeName" to employee.fullName,
                "dateKey" to dateKey,
                "scripture" to cleanScripture,
                "observation" to cleanObservation,
                "application" to cleanApplication,
                "prayer" to cleanPrayer,
                "photoStoragePath" to photoStoragePath,
                "photoUrl" to photoUrl,
                "status" to "SUBMITTED",
                "shareWithAdmin" to shareWithAdmin,
                "submittedAt" to FieldValue.serverTimestamp(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val statusPayload = mapOf(
                "ownerUid" to uid,
                "workspaceId" to SHARED_WORKSPACE_ID,
                "employeeUid" to employee.uid,
                "employeeId" to employee.employeeId,
                "employeeName" to employee.fullName,
                "dateKey" to dateKey,
                "status" to "SUBMITTED",
                "hasPhoto" to true,
                "shareWithAdmin" to shareWithAdmin,
                "submittedAt" to FieldValue.serverTimestamp(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.set(devotionalsCol().document(devId), devotionalPayload)
            batch.set(devotionalStatusCol().document(devId), statusPayload)
            batch.commit().awaitTask()

            ActionResult.Success(Unit, "Daily Devotional submitted! Clock In is now unlocked.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            ActionResult.Error(friendlyNetworkError(e, "Failed to submit Daily Devotional. Check your internet connection and try again."))
        }
    }

    suspend fun updateDevotionalPrivacyPolicy(policy: DevotionalPrivacyPolicy): ActionResult<Unit> {
        val path = "workspaces/$SHARED_WORKSPACE_ID/settings/devotional_privacy"
        return try {
            val uid = requireUserId()
            ensureWorkspaceMembership(uid)
            val ref = settingsCol().document("devotional_privacy")
            val snap = ref.get().awaitTask()
            if (snap.exists()) {
                ref.update(
                    mapOf(
                        "policy" to policy.name,
                        "updatedByUid" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).awaitTask()
            } else {
                ref.set(
                    mapOf(
                        "ownerUid" to uid,
                        "workspaceId" to SHARED_WORKSPACE_ID,
                        "policy" to policy.name,
                        "updatedByUid" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).awaitTask()
            }
            ActionResult.Success(Unit, "Privacy policy updated to ${policy.title}.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            ActionResult.Error(friendlyNetworkError(e, "Failed to update devotional privacy policy."))
        }
    }

    private fun friendlyNetworkError(e: Exception, fallback: String): String {
        val firestoreError = generateSequence<Throwable>(e) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        return when (firestoreError?.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "No internet connection. Changes were not confirmed—please reconnect and retry."
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Action rejected by backend security rules. Ensure today's devotional is submitted and you are signed in."
            else -> e.localizedMessage?.takeIf { it.isNotBlank() } ?: fallback
        }
    }

    suspend fun authenticateEmployee(
        identifier: String,
        pin: String
    ): ActionResult<EmployeeEntity> {
        val trimmedId = identifier.trim()
        val trimmedPin = pin.trim()
        if (trimmedId.isEmpty()) return ActionResult.Error("Select or enter your employee name or ID.")
        if (trimmedPin.isEmpty()) return ActionResult.Error("Enter your PIN.")

        return try {
            requireUserId()
            val snapshot = employeesCol()
                .whereEqualTo("workspaceId", SHARED_WORKSPACE_ID)
                .get()
                .awaitTask()
            val employees = snapshot.toObjects(
                EmployeeEntity::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            val employee = employees.firstOrNull {
                it.employeeId.equals(trimmedId, ignoreCase = true) ||
                    it.fullName.equals(trimmedId, ignoreCase = true)
            } ?: return ActionResult.Error("Employee \"$trimmedId\" not found.")

            if (!employee.active) {
                return ActionResult.Error("Your account is deactivated. Contact the CEO/Admin.")
            }
            if (employee.pin != trimmedPin) {
                return ActionResult.Error("Incorrect PIN. Please try again.")
            }
            ActionResult.Success(employee, "Welcome, ${employee.fullName}!")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "workspaces/$SHARED_WORKSPACE_ID/employees")
            ActionResult.Error(e.localizedMessage ?: "Unable to verify employee credentials.")
        }
    }

    suspend fun authenticateAdmin(pin: String): ActionResult<EmployeeEntity> {
        val trimmedPin = pin.trim()
        if (trimmedPin.isEmpty()) return ActionResult.Error("Enter the Admin PIN.")

        return try {
            requireUserId()
            val snapshot = employeesCol()
                .whereEqualTo("workspaceId", SHARED_WORKSPACE_ID)
                .get()
                .awaitTask()
            val employees = snapshot.toObjects(
                EmployeeEntity::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            val admin = employees.firstOrNull { it.admin && it.active && it.pin == trimmedPin }
                ?: return ActionResult.Error("Invalid Admin PIN.")
            ActionResult.Success(admin, "Admin portal unlocked.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "workspaces/$SHARED_WORKSPACE_ID/employees")
            ActionResult.Error(e.localizedMessage ?: "Unable to verify Admin PIN.")
        }
    }

    suspend fun clockIn(employeeUid: String): ActionResult<Unit> {
        var path = "workspaces/$SHARED_WORKSPACE_ID/attendance"
        return try {
            val uid = requireUserId()
            ensureWorkspaceMembership(uid)
            val empSnap = employeesCol().document(employeeUid).get().awaitTask()
            val emp = empSnap.toObject(EmployeeEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return ActionResult.Error("Employee record not found.")
            if (!emp.active) {
                return ActionResult.Error("Inactive employee cannot clock in.")
            }

            val now = System.currentTimeMillis()
            val todayKey = ManilaTime.dateKey(now)
            val attId = "${employeeUid}_${todayKey}"

            // Verify devotional completion before attempting Clock In (backend rule also enforces /devotionals/{devId})
            val devStatusSnap = devotionalStatusCol().document(attId).get().awaitTask()
            if (!devStatusSnap.exists()) {
                return ActionResult.Error("Complete and submit today's Daily Devotional before clocking in.")
            }

            val attRef = attendanceCol().document(attId)
            path = attRef.path

            val existingSnap = attRef.get().awaitTask()
            if (existingSnap.exists()) {
                val rec = existingSnap.toObject(
                    AttendanceRecordEntity::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                return when (AttendanceStatus.fromString(rec?.status)) {
                    AttendanceStatus.WORKING, AttendanceStatus.ON_BREAK ->
                        ActionResult.Error("You already have an active shift today.")
                    AttendanceStatus.CLOCKED_OUT ->
                        ActionResult.Error("Shift already completed today. Ask Admin to reset if needed.")
                    AttendanceStatus.NOT_CLOCKED_IN -> {
                        attRef.update(
                            mapOf(
                                "clockInMillis" to now,
                                "status" to AttendanceStatus.WORKING.name,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).awaitTask()
                        ActionResult.Success(Unit, "Clocked in at ${ManilaTime.formatTime12h(now)}")
                    }
                }
            }

            val payload = mapOf(
                "ownerUid" to uid,
                "workspaceId" to SHARED_WORKSPACE_ID,
                "employeeUid" to employeeUid,
                "devotionalId" to attId,
                "dateKey" to todayKey,
                "clockInMillis" to now,
                "status" to AttendanceStatus.WORKING.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            attRef.set(payload).awaitTask()
            ActionResult.Success(Unit, "Clocked in at ${ManilaTime.formatTime12h(now)}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            ActionResult.Error(friendlyNetworkError(e, "Failed to clock in."))
        }
    }

    suspend fun startBreak(employeeUid: String): ActionResult<Unit> {
        var path = "workspaces/$SHARED_WORKSPACE_ID/breaks"
        return try {
            val uid = requireUserId()
            ensureWorkspaceMembership(uid)
            val now = System.currentTimeMillis()
            val todayKey = ManilaTime.dateKey(now)
            val attId = "${employeeUid}_${todayKey}"
            val attRef = attendanceCol().document(attId)
            path = attRef.path

            val existingSnap = attRef.get().awaitTask()
            if (!existingSnap.exists()) {
                return ActionResult.Error("Please clock in before starting a break.")
            }
            val rec = existingSnap.toObject(
                AttendanceRecordEntity::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return ActionResult.Error("Please clock in before starting a break.")

            when (AttendanceStatus.fromString(rec.status)) {
                AttendanceStatus.NOT_CLOCKED_IN ->
                    return ActionResult.Error("Please clock in before starting a break.")
                AttendanceStatus.ON_BREAK ->
                    return ActionResult.Error("You are already on break. Multiple simultaneous breaks are not allowed.")
                AttendanceStatus.CLOCKED_OUT ->
                    return ActionResult.Error("You have already clocked out for today.")
                AttendanceStatus.WORKING -> {
                    val existingBreaks = breaksCol()
                        .whereEqualTo("workspaceId", SHARED_WORKSPACE_ID)
                        .get()
                        .awaitTask()
                        .toObjects(BreakRecordEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.attendanceId == attId }
                    if (existingBreaks.any { it.breakEndMillis == null }) {
                        return ActionResult.Error("An open break is already active.")
                    }

                    val breakId = "brk-${UUID.randomUUID()}"
                    val breakRef = breaksCol().document(breakId)
                    path = breakRef.path

                    val batch = db.batch()
                    batch.set(
                        breakRef,
                        mapOf(
                            "ownerUid" to uid,
                            "workspaceId" to SHARED_WORKSPACE_ID,
                            "attendanceId" to attId,
                            "employeeUid" to employeeUid,
                            "dateKey" to todayKey,
                            "breakStartMillis" to now,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    batch.update(
                        attRef,
                        mapOf(
                            "status" to AttendanceStatus.ON_BREAK.name,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    batch.commit().awaitTask()
                    ActionResult.Success(Unit, "Break started at ${ManilaTime.formatTime12h(now)}")
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            ActionResult.Error(e.localizedMessage ?: "Failed to start break.")
        }
    }

    suspend fun endBreak(employeeUid: String): ActionResult<Unit> {
        var path = "workspaces/$SHARED_WORKSPACE_ID/breaks"
        return try {
            val uid = requireUserId()
            ensureWorkspaceMembership(uid)
            val now = System.currentTimeMillis()
            val todayKey = ManilaTime.dateKey(now)
            val attId = "${employeeUid}_${todayKey}"
            val attRef = attendanceCol().document(attId)
            path = attRef.path

            val existingSnap = attRef.get().awaitTask()
            if (!existingSnap.exists()) {
                return ActionResult.Error("No active shift found for today.")
            }
            val rec = existingSnap.toObject(
                AttendanceRecordEntity::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return ActionResult.Error("No active shift found for today.")

            if (AttendanceStatus.fromString(rec.status) != AttendanceStatus.ON_BREAK) {
                return ActionResult.Error("You are not currently on break.")
            }

            val breakDocs = breaksCol()
                .whereEqualTo("workspaceId", SHARED_WORKSPACE_ID)
                .get()
                .awaitTask()
                .toObjects(BreakRecordEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .filter { it.attendanceId == attId }
            val openBreak = breakDocs.firstOrNull { it.breakEndMillis == null }
                ?: return ActionResult.Error("No active break record found.")

            val breakRef = breaksCol().document(openBreak.id)
            path = breakRef.path

            val batch = db.batch()
            batch.update(
                breakRef,
                mapOf(
                    "breakEndMillis" to now,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                attRef,
                mapOf(
                    "status" to AttendanceStatus.WORKING.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().awaitTask()
            ActionResult.Success(Unit, "Break ended at ${ManilaTime.formatTime12h(now)}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            ActionResult.Error(e.localizedMessage ?: "Failed to end break.")
        }
    }

    suspend fun clockOut(employeeUid: String): ActionResult<Unit> {
        var path = "workspaces/$SHARED_WORKSPACE_ID/attendance"
        return try {
            val uid = requireUserId()
            ensureWorkspaceMembership(uid)
            val now = System.currentTimeMillis()
            val todayKey = ManilaTime.dateKey(now)
            val attId = "${employeeUid}_${todayKey}"
            val attRef = attendanceCol().document(attId)
            path = attRef.path

            val existingSnap = attRef.get().awaitTask()
            if (!existingSnap.exists()) {
                return ActionResult.Error("Please clock in before clocking out.")
            }
            val rec = existingSnap.toObject(
                AttendanceRecordEntity::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return ActionResult.Error("Please clock in before clocking out.")

            when (AttendanceStatus.fromString(rec.status)) {
                AttendanceStatus.NOT_CLOCKED_IN ->
                    return ActionResult.Error("You are not clocked in.")
                AttendanceStatus.ON_BREAK ->
                    return ActionResult.Error("Cannot clock out while on break. End your break first.")
                AttendanceStatus.CLOCKED_OUT ->
                    return ActionResult.Error("You have already clocked out today.")
                AttendanceStatus.WORKING -> {
                    val breakDocs = breaksCol()
                        .whereEqualTo("workspaceId", SHARED_WORKSPACE_ID)
                        .get()
                        .awaitTask()
                        .toObjects(BreakRecordEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.attendanceId == attId }
                    if (breakDocs.any { it.breakEndMillis == null }) {
                        return ActionResult.Error("Cannot clock out while on break. End your break first.")
                    }

                    attRef.update(
                        mapOf(
                            "clockOutMillis" to now,
                            "status" to AttendanceStatus.CLOCKED_OUT.name,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).awaitTask()
                    ActionResult.Success(Unit, "Clocked out at ${ManilaTime.formatTime12h(now)}")
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            ActionResult.Error(e.localizedMessage ?: "Failed to clock out.")
        }
    }

    // --- Employee Management ---
    suspend fun addEmployee(
        fullName: String,
        employeeId: String,
        role: String,
        pin: String,
        active: Boolean
    ): ActionResult<Unit> {
        val cleanName = fullName.trim()
        val cleanCode = employeeId.trim().uppercase()
        val cleanRole = role.trim().ifEmpty { "Team Member" }
        val cleanPin = pin.trim()

        if (cleanName.isEmpty() || cleanName.length > 80) return ActionResult.Error("Employee name must be 1–80 characters.")
        if (cleanCode.isEmpty() || cleanCode.length > 32) return ActionResult.Error("Employee ID must be 1–32 characters.")
        if (cleanRole.length > 80) return ActionResult.Error("Role must be 80 characters or fewer.")
        if (cleanPin.length !in 4..6 || !cleanPin.all { it.isDigit() }) {
            return ActionResult.Error("PIN must be 4–6 numeric digits.")
        }

        var path = "workspaces/$SHARED_WORKSPACE_ID/employees"
        return try {
            val uid = requireUserId()
            ensureWorkspaceMembership(uid)
            val all = employeesCol()
                .whereEqualTo("workspaceId", SHARED_WORKSPACE_ID)
                .get()
                .awaitTask()
                .toObjects(
                    EmployeeEntity::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            if (all.any { it.employeeId.equals(cleanCode, ignoreCase = true) }) {
                return ActionResult.Error("Employee ID \"$cleanCode\" is already assigned.")
            }

            val docId = "emp-${UUID.randomUUID()}"
            val docRef = employeesCol().document(docId)
            path = docRef.path
            docRef.set(
                mapOf(
                    "ownerUid" to uid,
                    "workspaceId" to SHARED_WORKSPACE_ID,
                    "employeeId" to cleanCode,
                    "fullName" to cleanName,
                    "role" to cleanRole,
                    "pin" to cleanPin,
                    "active" to active,
                    "admin" to false,
                    "createdAtMillis" to System.currentTimeMillis(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitTask()
            ActionResult.Success(Unit, "Added $cleanName ($cleanCode).")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            ActionResult.Error(e.localizedMessage ?: "Failed to add employee.")
        }
    }

    suspend fun updateEmployeeInfo(
        employeeDocId: String,
        fullName: String,
        employeeId: String,
        role: String
    ): ActionResult<Unit> {
        val cleanName = fullName.trim()
        val cleanCode = employeeId.trim().uppercase()
        val cleanRole = role.trim().ifEmpty { "Team Member" }

        if (cleanName.isEmpty() || cleanName.length > 80) return ActionResult.Error("Employee name must be 1–80 characters.")
        if (cleanCode.isEmpty() || cleanCode.length > 32) return ActionResult.Error("Employee ID must be 1–32 characters.")
        if (cleanRole.length > 80) return ActionResult.Error("Role must be 80 characters or fewer.")

        var path = "workspaces/$SHARED_WORKSPACE_ID/employees/$employeeDocId"
        return try {
            val uid = requireUserId()
            ensureWorkspaceMembership(uid)
            val all = employeesCol()
                .whereEqualTo("workspaceId", SHARED_WORKSPACE_ID)
                .get()
                .awaitTask()
                .toObjects(
                    EmployeeEntity::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            if (all.any { it.uid != employeeDocId && it.employeeId.equals(cleanCode, ignoreCase = true) }) {
                return ActionResult.Error("Employee ID \"$cleanCode\" is already in use.")
            }
            val ref = employeesCol().document(employeeDocId)
            path = ref.path
            ref.update(
                mapOf(
                    "employeeId" to cleanCode,
                    "fullName" to cleanName,
                    "role" to cleanRole,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitTask()
            ActionResult.Success(Unit, "Updated $cleanName.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            ActionResult.Error(e.localizedMessage ?: "Failed to update employee.")
        }
    }

    suspend fun resetPin(employeeDocId: String, newPin: String): ActionResult<Unit> {
        val cleanPin = newPin.trim()
        if (cleanPin.length !in 4..6 || !cleanPin.all { it.isDigit() }) {
            return ActionResult.Error("PIN must be 4 to 6 numeric digits.")
        }
        var path = "workspaces/$SHARED_WORKSPACE_ID/employees/$employeeDocId"
        return try {
            val uid = requireUserId()
            ensureWorkspaceMembership(uid)
            val ref = employeesCol().document(employeeDocId)
            path = ref.path
            val emp = ref.get().awaitTask().toObject(
                EmployeeEntity::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return ActionResult.Error("Employee not found.")

            ref.update(
                mapOf(
                    "pin" to cleanPin,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitTask()
            ActionResult.Success(Unit, "PIN updated for ${emp.fullName}.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            ActionResult.Error(e.localizedMessage ?: "Failed to update PIN.")
        }
    }

    suspend fun toggleEmployeeActive(employeeDocId: String, active: Boolean): ActionResult<Unit> {
        var path = "workspaces/$SHARED_WORKSPACE_ID/employees/$employeeDocId"
        return try {
            val uid = requireUserId()
            ensureWorkspaceMembership(uid)
            val ref = employeesCol().document(employeeDocId)
            path = ref.path
            val emp = ref.get().awaitTask().toObject(
                EmployeeEntity::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return ActionResult.Error("Employee not found.")

            if (emp.admin && !active) {
                return ActionResult.Error("Cannot deactivate the primary Admin account.")
            }

            ref.update(
                mapOf(
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitTask()
            val label = if (active) "activated" else "deactivated"
            ActionResult.Success(Unit, "${emp.fullName} $label.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            ActionResult.Error(e.localizedMessage ?: "Failed to update employee status.")
        }
    }

    suspend fun deleteEmployee(employeeDocId: String): ActionResult<Unit> {
        var path = "workspaces/$SHARED_WORKSPACE_ID/employees/$employeeDocId"
        return try {
            val uid = requireUserId()
            ensureWorkspaceMembership(uid)
            val empRef = employeesCol().document(employeeDocId)
            path = empRef.path
            val emp = empRef.get().awaitTask().toObject(
                EmployeeEntity::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return ActionResult.Error("Employee not found.")
            if (emp.admin) {
                return ActionResult.Error("Cannot delete the primary Admin account.")
            }

            val attDocs = attendanceCol()
                .whereEqualTo("workspaceId", SHARED_WORKSPACE_ID)
                .get()
                .awaitTask()
                .documents
                .filter { it.getString("employeeUid") == employeeDocId }
            val brkDocs = breaksCol()
                .whereEqualTo("workspaceId", SHARED_WORKSPACE_ID)
                .get()
                .awaitTask()
                .documents
                .filter { it.getString("employeeUid") == employeeDocId }
            val devStatusDocs = devotionalStatusCol()
                .whereEqualTo("workspaceId", SHARED_WORKSPACE_ID)
                .get()
                .awaitTask()
                .documents
                .filter { it.getString("employeeUid") == employeeDocId }

            // Delete employee first so !existsAfter(.../employees/{employeeUid}) allows deleting devotionals.
            db.batch().apply { delete(empRef) }.commit().awaitTask()

            val toDelete = buildList {
                brkDocs.forEach { add(it.reference) }
                attDocs.forEach { add(it.reference) }
                devStatusDocs.forEach { add(it.reference) }
            }
            toDelete.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it) }
                batch.commit().awaitTask()
            }
            devStatusDocs.forEach { statusDoc ->
                runCatching {
                    devotionalsCol().document(statusDoc.id).delete().awaitTask()
                }
            }
            ActionResult.Success(Unit, "Deleted ${emp.fullName} and associated records.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            ActionResult.Error(e.localizedMessage ?: "Failed to delete employee.")
        }
    }

    // --- Reset Attendance ---
    suspend fun resetAttendanceForDate(employeeDocId: String, dateKey: String): ActionResult<Unit> {
        var path = "workspaces/$SHARED_WORKSPACE_ID/attendance"
        return try {
            val uid = requireUserId()
            ensureWorkspaceMembership(uid)
            val emp = employeesCol().document(employeeDocId).get().awaitTask().toObject(
                EmployeeEntity::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return ActionResult.Error("Employee not found.")

            val attDocs = attendanceCol()
                .whereEqualTo("workspaceId", SHARED_WORKSPACE_ID)
                .get()
                .awaitTask()
                .documents
                .filter {
                    it.getString("employeeUid") == employeeDocId &&
                        it.getString("dateKey") == dateKey
                }
            val brkDocs = breaksCol()
                .whereEqualTo("workspaceId", SHARED_WORKSPACE_ID)
                .get()
                .awaitTask()
                .documents
                .filter {
                    it.getString("employeeUid") == employeeDocId &&
                        it.getString("dateKey") == dateKey
                }

            val refs = buildList {
                brkDocs.forEach { add(it.reference) }
                attDocs.forEach { add(it.reference) }
            }
            path = "workspaces/$SHARED_WORKSPACE_ID/attendance/${employeeDocId}_$dateKey"
            refs.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it) }
                batch.commit().awaitTask()
            }
            ActionResult.Success(
                Unit,
                "Reset attendance for ${emp.fullName} on ${ManilaTime.formatShortDate(dateKey)}."
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            ActionResult.Error(e.localizedMessage ?: "Failed to reset attendance.")
        }
    }

    // --- Backup & Restore ---
    suspend fun getFullSnapshot(): Triple<List<EmployeeEntity>, List<AttendanceRecordEntity>, List<BreakRecordEntity>> {
        val uid = requireUserId()
        ensureWorkspaceMembership(uid)
        val emps = employeesCol()
            .whereEqualTo("workspaceId", SHARED_WORKSPACE_ID)
            .get()
            .awaitTask()
            .toObjects(
                EmployeeEntity::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        val att = attendanceCol()
            .whereEqualTo("workspaceId", SHARED_WORKSPACE_ID)
            .get()
            .awaitTask()
            .toObjects(
                AttendanceRecordEntity::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        val brks = breaksCol()
            .whereEqualTo("workspaceId", SHARED_WORKSPACE_ID)
            .get()
            .awaitTask()
            .toObjects(
                BreakRecordEntity::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        return Triple(emps, att, brks)
    }

    suspend fun restoreSnapshot(
        employees: List<EmployeeEntity>,
        attendance: List<AttendanceRecordEntity>,
        breaks: List<BreakRecordEntity>
    ): ActionResult<Unit> {
        if (employees.isEmpty()) {
            return ActionResult.Error("Backup contains no employees.")
        }
        val path = "workspaces/$SHARED_WORKSPACE_ID"
        return try {
            val uid = requireUserId()
            ensureWorkspaceMembership(uid)

            // Clear existing workspace collections
            val existingBrks = breaksCol()
                .whereEqualTo("workspaceId", SHARED_WORKSPACE_ID)
                .get()
                .awaitTask()
                .documents
                .map { it.reference }
            val existingAtt = attendanceCol()
                .whereEqualTo("workspaceId", SHARED_WORKSPACE_ID)
                .get()
                .awaitTask()
                .documents
                .map { it.reference }
            val existingEmps = employeesCol()
                .whereEqualTo("workspaceId", SHARED_WORKSPACE_ID)
                .get()
                .awaitTask()
                .documents
                .map { it.reference }

            (existingBrks + existingAtt + existingEmps).chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it) }
                batch.commit().awaitTask()
            }

            // Preserve immutable devotionals and ensure any missing devotional link for restored attendance exists
            val empMap = employees.associateBy { it.uid }
            val existingDevStatusIds = devotionalStatusCol()
                .whereEqualTo("workspaceId", SHARED_WORKSPACE_ID)
                .get()
                .awaitTask()
                .documents
                .mapTo(mutableSetOf()) { it.id }

            val phase1Writes = mutableListOf<Pair<com.google.firebase.firestore.DocumentReference, Map<String, Any>>>()
            employees.forEach { e ->
                val docId = e.uid.ifBlank { "emp-${UUID.randomUUID()}" }
                phase1Writes.add(
                    employeesCol().document(docId) to mapOf(
                        "ownerUid" to uid,
                        "workspaceId" to SHARED_WORKSPACE_ID,
                        "employeeId" to e.employeeId,
                        "fullName" to e.fullName,
                        "role" to e.role,
                        "pin" to e.pin,
                        "active" to e.active,
                        "admin" to e.admin,
                        "createdAtMillis" to e.createdAtMillis.coerceAtLeast(0L),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            val addedDevIds = mutableSetOf<String>()
            attendance.forEach { a ->
                val devId = a.devotionalId.ifBlank { "${a.employeeUid}_${a.dateKey}" }
                if (devId !in existingDevStatusIds && addedDevIds.add(devId)) {
                    val emp = empMap[a.employeeUid]
                    val empCode = emp?.employeeId?.ifBlank { a.employeeUid } ?: a.employeeUid
                    val empName = emp?.fullName?.ifBlank { a.employeeUid } ?: a.employeeUid
                    phase1Writes.add(
                        devotionalsCol().document(devId) to mapOf(
                            "ownerUid" to uid,
                            "workspaceId" to SHARED_WORKSPACE_ID,
                            "employeeUid" to a.employeeUid,
                            "employeeId" to empCode,
                            "employeeName" to empName,
                            "dateKey" to a.dateKey,
                            "scripture" to "Restored attendance backup entry",
                            "observation" to "Devotional completed prior to attendance backup.",
                            "application" to "Verified attendance link.",
                            "prayer" to "Private reflection not stored in attendance backup.",
                            "photoStoragePath" to "backup/workspaces/$SHARED_WORKSPACE_ID/devotionals/${a.employeeUid}/${a.dateKey}.jpg",
                            "photoUrl" to "",
                            "status" to "SUBMITTED",
                            "shareWithAdmin" to false,
                            "submittedAt" to FieldValue.serverTimestamp(),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    phase1Writes.add(
                        devotionalStatusCol().document(devId) to mapOf(
                            "ownerUid" to uid,
                            "workspaceId" to SHARED_WORKSPACE_ID,
                            "employeeUid" to a.employeeUid,
                            "employeeId" to empCode,
                            "employeeName" to empName,
                            "dateKey" to a.dateKey,
                            "status" to "SUBMITTED",
                            "hasPhoto" to true,
                            "shareWithAdmin" to false,
                            "submittedAt" to FieldValue.serverTimestamp(),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            val phase2Writes = mutableListOf<Pair<com.google.firebase.firestore.DocumentReference, Map<String, Any>>>()
            attendance.forEach { a ->
                val docId = a.id.ifBlank { "${a.employeeUid}_${a.dateKey}" }
                val devId = a.devotionalId.ifBlank { "${a.employeeUid}_${a.dateKey}" }
                val map = mutableMapOf<String, Any?>(
                    "ownerUid" to uid,
                    "workspaceId" to SHARED_WORKSPACE_ID,
                    "employeeUid" to a.employeeUid,
                    "devotionalId" to devId,
                    "dateKey" to a.dateKey,
                    "clockInMillis" to (a.clockInMillis ?: 0L),
                    "clockOutMillis" to a.clockOutMillis,
                    "status" to a.status,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                phase2Writes.add(attendanceCol().document(docId) to map)
            }
            breaks.forEach { b ->
                val docId = b.id.ifBlank { "brk-${UUID.randomUUID()}" }
                val map = mutableMapOf<String, Any?>(
                    "ownerUid" to uid,
                    "workspaceId" to SHARED_WORKSPACE_ID,
                    "attendanceId" to b.attendanceId,
                    "employeeUid" to b.employeeUid,
                    "dateKey" to b.dateKey,
                    "breakStartMillis" to b.breakStartMillis,
                    "breakEndMillis" to b.breakEndMillis,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                phase2Writes.add(breaksCol().document(docId) to map)
            }

            phase1Writes.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { (ref, payload) -> batch.set(ref, payload) }
                batch.commit().awaitTask()
            }
            phase2Writes.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { (ref, payload) -> batch.set(ref, payload) }
                batch.commit().awaitTask()
            }

            ActionResult.Success(
                Unit,
                "Restored ${employees.size} employees and ${attendance.size} attendance records to Firestore."
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            ActionResult.Error(e.localizedMessage ?: "Failed to restore backup to Firestore.")
        }
    }
}
