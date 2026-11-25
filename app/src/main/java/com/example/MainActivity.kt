package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.signOutFromGoogle
import com.example.data.TimeTrackRepository
import com.example.ui.SessionState
import com.example.ui.TimeTrackViewModel
import com.example.ui.UiState
import com.example.util.ManilaTime
import com.example.ui.screens.AdminPortalShell
import com.example.ui.screens.EmployeePortalScreen
import com.example.ui.screens.GoogleAuthScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandCharcoal
import com.example.ui.theme.TimeTrackTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val charcoal = android.graphics.Color.parseColor("#1D1D1D")
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(charcoal),
            navigationBarStyle = SystemBarStyle.dark(charcoal)
        )

        setContent {
            TimeTrackTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authUser by remember { authStateFlow() }
                        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

                    val user = authUser
                    if (user == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .windowInsetsPadding(WindowInsets.safeDrawing)
                        ) {
                            GoogleAuthScreen(onAuthSuccess = {})
                        }
                    } else {
                        key(user.uid) {
                            AuthenticatedWorkspaceRoot(user = user)
                        }
                    }
                }
            }
        }
    }
}

private fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val auth = Firebase.auth
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
    }
    auth.addAuthStateListener(listener)
    trySend(auth.currentUser)
    awaitClose { auth.removeAuthStateListener(listener) }
}

@Composable
private fun AuthenticatedWorkspaceRoot(user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val vm: TimeTrackViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val firestore = FirebaseFirestore.getInstance(dbId)
                TimeTrackViewModel(
                    repository = TimeTrackRepository(firestore),
                    currentUserId = user.uid
                )
            }
        }
    )

    val workspaceState by vm.workspaceState.collectAsStateWithLifecycle()
    val isSeedReady by vm.isSeedReady.collectAsStateWithLifecycle()
    val sessionState by vm.sessionState.collectAsStateWithLifecycle()
    val currentEmployee by vm.currentEmployee.collectAsStateWithLifecycle()
    val nowMillis by vm.nowMillis.collectAsStateWithLifecycle()
    val loginError by vm.loginError.collectAsStateWithLifecycle()
    val photoUploadState by vm.photoUploadState.collectAsStateWithLifecycle()
    val isSubmittingDevotional by vm.isSubmittingDevotional.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(vm) {
        vm.feedback.collect { banner ->
            snackbarHostState.showSnackbar(
                message = banner.message,
                duration = SnackbarDuration.Short
            )
        }
    }

    val onSignOutGoogle: () -> Unit = {
        vm.logoutPortal()
        signOutFromGoogle(credentialManager = credentialManager, scope = scope)
    }

    when (val state = workspaceState) {
        UiState.Loading -> {
            WorkspaceLoadingView()
        }

        is UiState.Error -> {
            WorkspaceErrorView(
                message = state.message,
                onSwitchAccount = onSignOutGoogle
            )
        }

        is UiState.Success -> {
            if (!isSeedReady && state.data.employees.isEmpty()) {
                WorkspaceLoadingView()
                return
            }

            val employees = state.data.employees
            val allRecords = state.data.attendanceRecords
            val devotionalStatuses = state.data.devotionalStatuses
            val privacyPolicy = state.data.privacyPolicy

            when (sessionState) {
                SessionState.LoggedOut -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                    ) {
                        LoginScreen(
                            employees = employees,
                            records = allRecords,
                            nowMillis = nowMillis,
                            googleEmail = user.email ?: user.displayName,
                            loginError = loginError,
                            onClearError = vm::clearLoginError,
                            onEmployeeLogin = vm::loginEmployee,
                            onAdminLogin = vm::loginAdmin,
                            onSignOutGoogle = onSignOutGoogle
                        )
                    }
                }

                is SessionState.EmployeeSession -> {
                    BackHandler {
                        vm.logoutPortal()
                    }
                    val emp = currentEmployee
                    if (emp == null) {
                        vm.logoutPortal()
                    } else {
                        val myDevotionals by vm.myDevotionals.collectAsStateWithLifecycle()
                        val todayKey = ManilaTime.dateKey(nowMillis)
                        val empRecords = remember(allRecords, emp.uid) {
                            allRecords.filter { it.record.employeeUid == emp.uid }
                        }
                        val empDevotionalStatuses = remember(devotionalStatuses, emp.uid) {
                            devotionalStatuses.filter { it.employeeUid == emp.uid && it.status == "SUBMITTED" }
                        }
                        val todayDevotionalStatus = remember(empDevotionalStatuses, todayKey) {
                            empDevotionalStatuses.firstOrNull { it.dateKey == todayKey }
                        }
                        EmployeePortalScreen(
                            employee = emp,
                            employeeRecords = empRecords,
                            todayDevotionalStatus = todayDevotionalStatus,
                            employeeDevotionalStatuses = empDevotionalStatuses,
                            myDevotionals = myDevotionals,
                            photoUploadState = photoUploadState,
                            isSubmittingDevotional = isSubmittingDevotional,
                            privacyPolicy = privacyPolicy,
                            nowMillis = nowMillis,
                            snackbarHostState = snackbarHostState,
                            onUploadDevotionalPhoto = { uri ->
                                vm.uploadDevotionalPhoto(context, uri, emp.uid)
                            },
                            onClearDevotionalPhoto = vm::clearDevotionalPhoto,
                            onSubmitDevotional = { s, o, a, p, share ->
                                vm.submitDevotional(
                                    employee = emp,
                                    scripture = s,
                                    observation = o,
                                    application = a,
                                    prayer = p,
                                    shareWithAdmin = share
                                )
                            },
                            onNotify = vm::notifyInfo,
                            onClockIn = { vm.clockIn(emp.uid) },
                            onStartBreak = { vm.startBreak(emp.uid) },
                            onEndBreak = { vm.endBreak(emp.uid) },
                            onClockOut = { vm.clockOut(emp.uid) },
                            onSignOut = vm::logoutPortal
                        )
                    }
                }

                is SessionState.AdminSession -> {
                    BackHandler {
                        vm.logoutPortal()
                    }
                    val adminDevotionalFeed by vm.adminDevotionalFeed.collectAsStateWithLifecycle()
                    AdminPortalShell(
                        adminEmployee = currentEmployee,
                        employees = employees,
                        records = allRecords,
                        devotionalStatuses = devotionalStatuses,
                        adminDevotionalFeed = adminDevotionalFeed,
                        privacyPolicy = privacyPolicy,
                        nowMillis = nowMillis,
                        snackbarHostState = snackbarHostState,
                        suggestedNextId = vm::nextSuggestedEmployeeId,
                        onAddEmployee = vm::addEmployee,
                        onUpdateEmployee = vm::updateEmployeeInfo,
                        onResetPin = vm::resetPin,
                        onToggleActive = vm::toggleEmployeeActive,
                        onDeleteEmployee = vm::deleteEmployee,
                        onResetAttendance = vm::resetAttendanceForDate,
                        onUpdatePrivacyPolicy = vm::updateDevotionalPrivacyPolicy,
                        onBuildCsv = vm::buildCsvContent,
                        onCreateBackupJson = vm::createBackupJson,
                        onRestoreBackupJson = vm::restoreBackupJson,
                        onNotify = vm::notifyInfo,
                        onSignOut = vm::logoutPortal
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkspaceLoadingView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_3kb_brand),
                contentDescription = "3KB TimeTrack logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(84.dp)
            )
            CircularProgressIndicator(
                color = BrandAmber,
                strokeWidth = 3.dp
            )
            Text(
                text = "Loading 3KB TimeTrack…",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun WorkspaceErrorView(
    message: String,
    onSwitchAccount: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = "Unable to Connect to Workspace",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onSwitchAccount,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandAmber,
                        contentColor = BrandCharcoal
                    ),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Switch Google Account", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
