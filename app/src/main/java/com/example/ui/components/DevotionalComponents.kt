package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.DevotionalEntity
import com.example.data.DevotionalPrivacyPolicy
import com.example.data.DevotionalStatusEntity
import com.example.ui.DevotionalPhotoUploadState
import com.example.ui.theme.BrandAmber
import com.example.ui.theme.BrandCharcoal
import com.example.ui.theme.BrandEspresso
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.LocalStatusPalette
import com.example.util.ManilaTime
import com.example.util.PhotoUploadHelper
import kotlin.math.roundToInt

private data class SoapGuideItem(
    val letter: String,
    val title: String,
    val question: String,
    val explanation: String,
    val example: String
)

private val SOAP_GUIDE_ITEMS = listOf(
    SoapGuideItem(
        letter = "S",
        title = "Scripture",
        question = "What Bible passage or verse did you read?",
        explanation = "Write the verse reference and key words that stood out during your reading.",
        example = "\"Proverbs 16:3 — Commit to the Lord whatever you do, and he will establish your plans.\""
    ),
    SoapGuideItem(
        letter = "O",
        title = "Observation",
        question = "What did you learn or observe from the passage?",
        explanation = "Summarize the meaning, context, or truth God highlighted in the verse.",
        example = "God invites us to surrender our work before rushing into tasks so our plans stand on a faithful foundation."
    ),
    SoapGuideItem(
        letter = "A",
        title = "Application",
        question = "How will you apply this Scripture to your daily life?",
        explanation = "Choose one practical, specific step you will live out at work or home today.",
        example = "Before starting today's deliverables, I will review priorities with integrity and encourage one teammate."
    ),
    SoapGuideItem(
        letter = "P",
        title = "Prayer",
        question = "What would you like to pray about today?",
        explanation = "Talk honestly with God—thank Him, ask for wisdom, or lift up your family and team.",
        example = "\"Lord, guide my hands and mind today. Give me patience, focus, and grace to serve others well. Amen.\""
    )
)

@Composable
fun SoapGuideCard(
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("soap_guide_toggle"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = BrandAmber.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = BrandAmber,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "SOAP Devotional Guide",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (expanded) "Tap to collapse guide & examples" else "Tap for Scripture, Observation, Application & Prayer examples",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse SOAP guide" else "Expand SOAP guide",
                    tint = BrandAmber
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SOAP_GUIDE_ITEMS.forEach { item ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(BrandAmber),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = item.letter,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = BrandCharcoal,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "${item.letter} – ${item.title}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = BrandAmber
                                    )
                                }
                                Text(
                                    text = item.question,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = item.explanation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    color = BrandEspresso.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Example: ${item.example}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.88f),
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DailyDevotionalCheckInCard(
    employeeUid: String,
    todayKey: String,
    photoState: DevotionalPhotoUploadState,
    isSubmitting: Boolean,
    privacyPolicy: DevotionalPrivacyPolicy,
    onUploadPhoto: (Uri) -> Unit,
    onClearPhoto: () -> Unit,
    onSubmitDevotional: (
        scripture: String,
        observation: String,
        application: String,
        prayer: String,
        shareWithAdmin: Boolean
    ) -> Unit,
    onNotify: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalStatusPalette.current

    var scripture by rememberSaveable(employeeUid, todayKey) { mutableStateOf("") }
    var observation by rememberSaveable(employeeUid, todayKey) { mutableStateOf("") }
    var application by rememberSaveable(employeeUid, todayKey) { mutableStateOf("") }
    var prayer by rememberSaveable(employeeUid, todayKey) { mutableStateOf("") }
    var shareWithAdmin by rememberSaveable(employeeUid, todayKey) { mutableStateOf(false) }
    var attemptedSubmit by rememberSaveable(employeeUid, todayKey) { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            onUploadPhoto(uri)
        } else if (!success) {
            pendingCameraUri = null
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = PhotoUploadHelper.createCameraPhotoUri(context)
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        } else {
            onNotify("Camera permission denied. You can choose a photo from your library instead.", true)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUploadPhoto(uri)
        }
    }

    val launchCamera = {
        val hasCameraPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasCameraPerm) {
            val uri = PhotoUploadHelper.createCameraPhotoUri(context)
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val launchGallery = {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    val isPhotoUploaded = photoState is DevotionalPhotoUploadState.Uploaded
    val allSoapFilled = scripture.isNotBlank() &&
        observation.isNotBlank() &&
        application.isNotBlank() &&
        prayer.isNotBlank()
    val canSubmit = isPhotoUploaded && allSoapFilled && !isSubmitting

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(BrandEspresso, BrandCharcoal)
                        )
                    )
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "REQUIRED BEFORE CLOCK IN",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandAmber
                        )
                        Text(
                            text = "Daily Devotional Check-In",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${ManilaTime.formatShortDate(todayKey)} (Asia/Manila)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                    }
                    Surface(
                        color = BrandAmber.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = BrandAmber,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(24.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SoapGuideCard()

                // Step 1: Photo Upload
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "1. Devotional Photo Proof *",
                        style = MaterialTheme.typography.titleSmall,
                        color = BrandAmber
                    )
                    Text(
                        text = "Upload any devotional image (camera or gallery). Blurry, dark, or unreadable images are still accepted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = launchCamera,
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("devotional_take_photo_button"),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Take Photo")
                        }

                        OutlinedButton(
                            onClick = launchGallery,
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("devotional_choose_photo_button"),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Choose Photo")
                        }
                    }

                    PhotoUploadStatusCard(
                        photoState = photoState,
                        showRequiredWarning = attemptedSubmit && photoState is DevotionalPhotoUploadState.Idle,
                        onRetry = {
                            val localStr = when (photoState) {
                                is DevotionalPhotoUploadState.Error -> photoState.localUri
                                is DevotionalPhotoUploadState.Uploading -> photoState.localUri
                                is DevotionalPhotoUploadState.Uploaded -> photoState.localUri
                                DevotionalPhotoUploadState.Idle -> null
                            }
                            if (localStr != null) {
                                onUploadPhoto(Uri.parse(localStr))
                            }
                        },
                        onClear = onClearPhoto,
                        verifiedFg = palette.workingFg,
                        verifiedBg = palette.workingBg
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Step 2: SOAP Reflection Fields
                Text(
                    text = "2. SOAP Reflection *",
                    style = MaterialTheme.typography.titleSmall,
                    color = BrandAmber
                )

                SoapInputField(
                    label = "S – Scripture *",
                    placeholder = "What Bible passage or verse did you read?",
                    value = scripture,
                    onValueChange = { if (it.length <= 1000) scripture = it },
                    maxLength = 1000,
                    minLines = 2,
                    isError = attemptedSubmit && scripture.isBlank(),
                    enabled = !isSubmitting,
                    tag = "soap_scripture_input"
                )
                SoapInputField(
                    label = "O – Observation *",
                    placeholder = "What did you learn or observe from the passage?",
                    value = observation,
                    onValueChange = { if (it.length <= 2000) observation = it },
                    maxLength = 2000,
                    minLines = 3,
                    isError = attemptedSubmit && observation.isBlank(),
                    enabled = !isSubmitting,
                    tag = "soap_observation_input"
                )
                SoapInputField(
                    label = "A – Application *",
                    placeholder = "How will you apply this Scripture to your daily life?",
                    value = application,
                    onValueChange = { if (it.length <= 2000) application = it },
                    maxLength = 2000,
                    minLines = 3,
                    isError = attemptedSubmit && application.isBlank(),
                    enabled = !isSubmitting,
                    tag = "soap_application_input"
                )
                SoapInputField(
                    label = "P – Prayer *",
                    placeholder = "What would you like to pray about today?",
                    value = prayer,
                    onValueChange = { if (it.length <= 2000) prayer = it },
                    maxLength = 2000,
                    minLines = 3,
                    isError = attemptedSubmit && prayer.isBlank(),
                    enabled = !isSubmitting,
                    tag = "soap_prayer_input"
                )

                // Privacy & Opt-In control
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = BrandAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Share photo & SOAP with CEO/Admin",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = when (privacyPolicy) {
                                        DevotionalPrivacyPolicy.STATUS_ONLY ->
                                            "Company policy is Status Only — reflections remain hidden."
                                        DevotionalPrivacyPolicy.OPT_IN_ONLY ->
                                            "Optional opt-in: off by default so your prayer stays private."
                                        DevotionalPrivacyPolicy.ADMIN_PASTORAL_VIEW ->
                                            "Verified CEO/Admin oversight is active; never shown in attendance CSV."
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = shareWithAdmin,
                            onCheckedChange = { shareWithAdmin = it },
                            enabled = !isSubmitting,
                            modifier = Modifier.testTag("devotional_share_switch")
                        )
                    }
                }

                // Validation summary message
                if (!canSubmit && (attemptedSubmit || !isPhotoUploaded || !allSoapFilled)) {
                    val missingMsg = when {
                        photoState is DevotionalPhotoUploadState.Uploading ->
                            "Wait for your devotional photo upload to complete before submitting."
                        photoState is DevotionalPhotoUploadState.Error ->
                            "Photo upload failed. Tap Retry Upload or choose another photo."
                        !isPhotoUploaded ->
                            "Step 1: Take or choose a devotional photo to enable submission."
                        !allSoapFilled ->
                            "Complete all 4 SOAP sections (Scripture, Observation, Application, Prayer)."
                        else -> ""
                    }
                    if (missingMsg.isNotBlank()) {
                        Text(
                            text = missingMsg,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (attemptedSubmit || photoState is DevotionalPhotoUploadState.Error) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Button(
                    onClick = {
                        attemptedSubmit = true
                        if (canSubmit) {
                            onSubmitDevotional(
                                scripture,
                                observation,
                                application,
                                prayer,
                                shareWithAdmin
                            )
                        }
                    },
                    enabled = canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("submit_devotional_button"),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandAmber,
                        contentColor = BrandCharcoal
                    )
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = BrandCharcoal,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Submitting Devotional…",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Submit Daily Devotional",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoUploadStatusCard(
    photoState: DevotionalPhotoUploadState,
    showRequiredWarning: Boolean,
    onRetry: () -> Unit,
    onClear: () -> Unit,
    verifiedFg: Color,
    verifiedBg: Color
) {
    val previewUri = when (photoState) {
        is DevotionalPhotoUploadState.Uploading -> photoState.localUri
        is DevotionalPhotoUploadState.Uploaded -> photoState.localUri
        is DevotionalPhotoUploadState.Error -> photoState.localUri
        DevotionalPhotoUploadState.Idle -> null
    }

    if (previewUri == null) {
        if (showRequiredWarning) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "A devotional photo is required.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        return
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = when (photoState) {
                    is DevotionalPhotoUploadState.Uploaded -> verifiedFg.copy(alpha = 0.65f)
                    is DevotionalPhotoUploadState.Error -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    else -> MaterialTheme.colorScheme.outlineVariant
                },
                shape = MaterialTheme.shapes.medium
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            var previewFailed by remember(previewUri) { mutableStateOf(false) }

            Box(modifier = Modifier.fillMaxWidth()) {
                if (!previewFailed) {
                    AsyncImage(
                        model = previewUri,
                        contentDescription = "Devotional photo preview",
                        contentScale = ContentScale.Crop,
                        onError = { previewFailed = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(MaterialTheme.shapes.small)
                    )
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Image attached",
                                tint = BrandAmber,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Image attached (preview unreadable)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (photoState !is DevotionalPhotoUploadState.Uploading) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(BrandCharcoal.copy(alpha = 0.78f))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Remove photo",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            when (photoState) {
                is DevotionalPhotoUploadState.Uploading -> {
                    val pct = (photoState.progress * 100f).roundToInt().coerceIn(0, 100)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LinearProgressIndicator(
                            progress = { photoState.progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = BrandAmber
                        )
                        Text(
                            text = "Uploading photo… $pct%",
                            style = MaterialTheme.typography.labelMedium,
                            color = BrandAmber
                        )
                    }
                }
                is DevotionalPhotoUploadState.Uploaded -> {
                    Surface(
                        color = verifiedBg,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = verifiedFg,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Photo uploaded & ready for submission",
                                style = MaterialTheme.typography.labelMedium,
                                color = verifiedFg,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                is DevotionalPhotoUploadState.Error -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = photoState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        OutlinedButton(
                            onClick = onRetry,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Retry Upload")
                        }
                    }
                }
                DevotionalPhotoUploadState.Idle -> Unit
            }
        }
    }
}

@Composable
private fun SoapInputField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    maxLength: Int,
    minLines: Int,
    isError: Boolean,
    enabled: Boolean,
    tag: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        enabled = enabled,
        isError = isError,
        minLines = minLines,
        maxLines = 6,
        supportingText = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isError) "Required field" else "",
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "${value.length}/$maxLength",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono)
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    )
}

@Composable
fun DevotionalCompletedCard(
    status: DevotionalStatusEntity,
    myDevotional: DevotionalEntity?,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalStatusPalette.current
    val submittedTime = status.submittedAt?.toDate()?.time?.let { ManilaTime.formatTime12h(it) } ?: "Verified today"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("devotional_completed_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = palette.workingBg,
                shape = CircleShape,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Devotional completed",
                        tint = palette.workingFg,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Daily Devotional Completed",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.workingFg,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${ManilaTime.formatShortDate(status.dateKey)} • Submitted $submittedTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Clock In is unlocked for today.",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandAmber
                )
            }

            if (myDevotional != null) {
                TextButton(
                    onClick = onViewDetails,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("View")
                }
            }
        }
    }
}

@Composable
fun DevotionalDetailDialog(
    devotional: DevotionalEntity,
    onDismiss: () -> Unit
) {
    val submittedMillis = devotional.submittedAt?.toDate()?.time
    val timestampText = if (submittedMillis != null) {
        "${ManilaTime.formatShortDate(devotional.dateKey)} • ${ManilaTime.formatTime12h(submittedMillis)} PHT"
    } else {
        ManilaTime.formatShortDate(devotional.dateKey)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = devotional.employeeName,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "${devotional.employeeId} • $timestampText",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandAmber
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                var dialogImageFailed by remember(devotional.photoUrl) { mutableStateOf(false) }
                val imageModel = remember(devotional.photoUrl) {
                    PhotoUploadHelper.resolvePreviewModel(devotional.photoUrl)
                }
                if (imageModel != null && !dialogImageFailed) {
                    AsyncImage(
                        model = imageModel,
                        contentDescription = "Devotional photo for ${devotional.employeeName}",
                        contentScale = ContentScale.Crop,
                        onError = { dialogImageFailed = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(MaterialTheme.shapes.medium)
                    )
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = BrandAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Image attached (${devotional.photoStoragePath.substringAfterLast('/')})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                SoapSectionReadOnly("S – Scripture", devotional.scripture)
                SoapSectionReadOnly("O – Observation", devotional.observation)
                SoapSectionReadOnly("A – Application", devotional.application)
                SoapSectionReadOnly("P – Prayer", devotional.prayer)
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("devotional_detail_close")
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun SoapSectionReadOnly(
    heading: String,
    body: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = heading,
                style = MaterialTheme.typography.labelLarge,
                color = BrandAmber,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
