package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AdminDevotionalFeed
import com.example.data.AttendanceWithBreaks
import com.example.data.DevotionalPrivacyPolicy
import com.example.data.DevotionalStatusEntity
import com.example.data.EmployeeEntity
import com.example.ui.components.AppWidthClass
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandCharcoal
import com.example.ui.theme.BrandEspresso
import com.example.util.CsvExportScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPortalShell(
    adminEmployee: EmployeeEntity?,
    employees: List<EmployeeEntity>,
    records: List<AttendanceWithBreaks>,
    devotionalStatuses: List<DevotionalStatusEntity>,
    adminDevotionalFeed: AdminDevotionalFeed,
    privacyPolicy: DevotionalPrivacyPolicy,
    nowMillis: Long,
    snackbarHostState: SnackbarHostState,
    suggestedNextId: () -> String,
    onAddEmployee: (String, String, String, String, Boolean) -> Unit,
    onUpdateEmployee: (String, String, String, String) -> Unit,
    onResetPin: (String, String) -> Unit,
    onToggleActive: (String, Boolean) -> Unit,
    onDeleteEmployee: (String) -> Unit,
    onResetAttendance: (String, String) -> Unit,
    onUpdatePrivacyPolicy: (DevotionalPrivacyPolicy) -> Unit,
    onBuildCsv: (CsvExportScope, Int, String, String, String?) -> Pair<String, String>,
    onCreateBackupJson: suspend () -> String,
    onRestoreBackupJson: (String) -> Unit,
    onNotify: (String, Boolean) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    val navItems = listOf(
        Triple("Overview", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
        Triple("Weekly", Icons.Filled.CalendarViewWeek, Icons.Outlined.CalendarViewWeek),
        Triple("Devotion", Icons.Filled.MenuBook, Icons.Outlined.MenuBook),
        Triple("Export", Icons.Filled.FileDownload, Icons.Outlined.FileDownload),
        Triple("Team", Icons.Filled.Groups, Icons.Outlined.Groups)
    )

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
                                    modifier = Modifier.size(36.dp)
                                )
                                Column {
                                    Text(
                                        text = "3KB TimeTrack Admin",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "374 Kingdom Builders • ${adminEmployee?.fullName ?: "CEO Portal"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrandAmber,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = onSignOut,
                                modifier = Modifier.testTag("admin_sign_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "Sign Out",
                                    tint = Color.White
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
                    val navColors = NavigationBarItemDefaults.colors(
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
                            navItems.forEachIndexed { index, (label, filled, outlined) ->
                                NavigationBarItem(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    colors = navColors,
                                    icon = {
                                        Icon(
                                            imageVector = if (selectedTab == index) filled else outlined,
                                            contentDescription = label
                                        )
                                    },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("admin_nav_$index")
                                )
                            }
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
                        navItems.forEachIndexed { index, (label, filled, outlined) ->
                            NavigationRailItem(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                colors = railColors,
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == index) filled else outlined,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("admin_rail_$index")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val contentModifier = if (selectedTab == 1) {
                        Modifier.fillMaxSize() // Weekly matrix benefits from full width on tablets/desktop
                    } else {
                        Modifier
                            .widthIn(max = 880.dp)
                            .fillMaxSize()
                    }

                    when (selectedTab) {
                        0 -> AdminDashboardScreen(
                            employees = employees,
                            records = records,
                            nowMillis = nowMillis,
                            onResetAttendance = onResetAttendance,
                            modifier = contentModifier
                        )
                        1 -> WeeklyOverviewScreen(
                            employees = employees,
                            records = records,
                            nowMillis = nowMillis,
                            onResetAttendance = onResetAttendance,
                            modifier = contentModifier
                        )
                        2 -> AdminDevotionalsScreen(
                            employees = employees,
                            devotionalStatuses = devotionalStatuses,
                            adminDevotionalFeed = adminDevotionalFeed,
                            privacyPolicy = privacyPolicy,
                            nowMillis = nowMillis,
                            onUpdatePrivacyPolicy = onUpdatePrivacyPolicy,
                            modifier = contentModifier
                        )
                        3 -> HistoryAndExportScreen(
                            employees = employees,
                            records = records,
                            nowMillis = nowMillis,
                            onBuildCsv = onBuildCsv,
                            onNotify = onNotify,
                            onResetAttendance = onResetAttendance,
                            modifier = contentModifier
                        )
                        else -> EmployeeManagementScreen(
                            employees = employees,
                            nowMillis = nowMillis,
                            suggestedNextId = suggestedNextId,
                            onAddEmployee = onAddEmployee,
                            onUpdateEmployee = onUpdateEmployee,
                            onResetPin = onResetPin,
                            onToggleActive = onToggleActive,
                            onDeleteEmployee = onDeleteEmployee,
                            onResetAttendance = onResetAttendance,
                            onCreateBackupJson = onCreateBackupJson,
                            onRestoreBackupJson = onRestoreBackupJson,
                            onNotify = onNotify,
                            modifier = contentModifier
                        )
                    }
                }
            }
        }
    }
}
