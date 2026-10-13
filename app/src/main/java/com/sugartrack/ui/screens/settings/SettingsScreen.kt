package com.sugartrack.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sugartrack.DeveloperInfo
import com.sugartrack.DeveloperLinks
import com.sugartrack.R
import com.sugartrack.SettingsPage
import com.sugartrack.data.GlucoseRepository
import com.sugartrack.data.SugarTrackDatabase
import com.sugartrack.data.UserProfile
import com.sugartrack.data.export.ExcelExporter
import com.sugartrack.data.export.PdfExporter
import com.sugartrack.ui.components.GlucoCard
import com.sugartrack.ui.components.GlucoOutlinedButton
import com.sugartrack.ui.components.GlucoPrimaryButton
import com.sugartrack.ui.screens.analysis.AnalysisScreen
import com.sugartrack.ui.screens.users.UsersScreen
import com.sugartrack.ui.tutorial.TutorialTarget
import com.sugartrack.ui.tutorial.LocalTutorialController
import com.sugartrack.ui.tutorial.TutorialStep
import com.sugartrack.ui.tutorial.tutorialTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    page: SettingsPage,
    userName: String,
    profiles: List<UserProfile>,
    profileId: Long,
    language: String,
    appearance: String,
    onPage: (SettingsPage) -> Unit,
    onBack: () -> Unit,
    onProfileSelected: (UserProfile) -> Unit,
    onProfileAdded: (String) -> Unit,
    onProfileUpdated: (UserProfile) -> Unit,
    onProfileDeleted: (UserProfile) -> Unit,
    onLanguage: (String) -> Unit,
    onAppearance: (String) -> Unit,
    openHistory: () -> Unit,
    openReminders: () -> Unit,
    openAnalysis: () -> Unit,
    onReplayTutorial: () -> Unit
) {
    if (page == SettingsPage.ROOT) {
        var profilesOpen by remember { mutableStateOf(false) }
        SettingsRoot(userName, onPage, { profilesOpen = true }, openReminders, openHistory, openAnalysis, onReplayTutorial)
        if (profilesOpen) {
            UsersScreen(
                profiles,
                profileId,
                onSelected = { onProfileSelected(it); profilesOpen = false },
                onAdd = onProfileAdded,
                onUpdate = onProfileUpdated,
                onDelete = onProfileDeleted,
                close = { profilesOpen = false }
            )
        }
    } else if (page == SettingsPage.APPEARANCE || page == SettingsPage.LANGUAGE) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.back))
            }
            Text(
                stringResource(if (page == SettingsPage.APPEARANCE) R.string.appearance else R.string.language),
                style = MaterialTheme.typography.headlineMedium
            )
            GlucoCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    if (page == SettingsPage.APPEARANCE) AppearanceSelector(appearance, onAppearance)
                    else LanguageSelector(language, onLanguage)
                }
            }
        }
    } else if (page == SettingsPage.EXPORTS) {
        ExportScreen(profileId = profileId, userName = userName, openHistory = openHistory, onBack = onBack)
    } else if (page == SettingsPage.ANALYSIS) {
        AnalysisScreen(profileId = profileId, onBack = onBack)
    } else {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.back))
            }
            when (page) {
                SettingsPage.SUPPORT -> SupportScreen()
                SettingsPage.SERVICES -> ServicesScreen()
                SettingsPage.CONTACT -> ContactScreen()
                SettingsPage.ABOUT -> AboutScreen()
                else -> Unit
            }
        }
    }
}

@Composable
private fun SettingsRoot(
    userName: String,
    onPage: (SettingsPage) -> Unit,
    openUsers: () -> Unit,
    openReminders: () -> Unit,
    openHistory: () -> Unit,
    openAnalysis: () -> Unit,
    onReplayTutorial: () -> Unit
) {
    val tutorial = LocalTutorialController.current
    val settingsListState = rememberLazyListState()

    LaunchedEffect(tutorial?.currentStep) {
        val itemIndex = when (tutorial?.currentStep) {
            TutorialStep.SETTINGS_USERS -> 1
            TutorialStep.SETTINGS_REMINDERS -> 2
            TutorialStep.SETTINGS_APPEARANCE -> 4
            TutorialStep.SETTINGS_LANGUAGE -> 5
            TutorialStep.SETTINGS_EXPORT -> 6
            TutorialStep.SETTINGS_ANALYSIS -> 3
            TutorialStep.SETTINGS_APP_TUTORIAL -> 8
            TutorialStep.SETTINGS_SUPPORT -> 9
            TutorialStep.SETTINGS_DEVELOPER_SERVICES -> 10
            TutorialStep.SETTINGS_CONTACT_DEVELOPER -> 11
            TutorialStep.SETTINGS_ABOUT -> 12
            else -> null
        }
        itemIndex?.let { settingsListState.animateScrollToItem(it) }
    }

    LazyColumn(
        state = settingsListState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item {
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(34.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(userName, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(stringResource(R.string.current_user), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    }
                    TextButton(onClick = openUsers) { Text(stringResource(R.string.switch_user), color = MaterialTheme.colorScheme.primary) }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.user_settings), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        item { SettingsRow(Icons.Outlined.People, R.string.users, R.string.switch_user, Modifier.tutorialTarget(TutorialTarget.SETTINGS_USERS), openUsers) }
        item { SettingsRow(Icons.Outlined.Notifications, R.string.reminders, null, Modifier.tutorialTarget(TutorialTarget.SETTINGS_REMINDERS), openReminders) }
        item { SettingsRow(Icons.Outlined.Insights, R.string.data_analysis, null, Modifier.tutorialTarget(TutorialTarget.SETTINGS_ANALYSIS), openAnalysis) }
        item { SettingsRow(Icons.Outlined.Palette, R.string.appearance, null, Modifier.tutorialTarget(TutorialTarget.SETTINGS_APPEARANCE)) { onPage(SettingsPage.APPEARANCE) } }
        item { SettingsRow(Icons.Outlined.Language, R.string.language, null, Modifier.tutorialTarget(TutorialTarget.SETTINGS_LANGUAGE)) { onPage(SettingsPage.LANGUAGE) } }
        item { SettingsRow(Icons.Outlined.FileDownload, R.string.export_data, null, Modifier.tutorialTarget(TutorialTarget.SETTINGS_EXPORT)) { onPage(SettingsPage.EXPORTS) } }
        item {
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.app_developer), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        item { SettingsRow(Icons.Outlined.School, R.string.app_tutorial, null, Modifier.tutorialTarget(TutorialTarget.SETTINGS_APP_TUTORIAL), onReplayTutorial) }
        item { SettingsRow(Icons.Outlined.FavoriteBorder, R.string.support_app, null, Modifier.tutorialTarget(TutorialTarget.SETTINGS_SUPPORT)) { onPage(SettingsPage.SUPPORT) } }
        item { SettingsRow(Icons.Outlined.Language, R.string.need_app_or_website, null, Modifier.tutorialTarget(TutorialTarget.SETTINGS_DEVELOPER_SERVICES)) { onPage(SettingsPage.SERVICES) } }
        item { SettingsRow(Icons.Outlined.Email, R.string.contact_developer, null, Modifier.tutorialTarget(TutorialTarget.SETTINGS_CONTACT_DEVELOPER)) { onPage(SettingsPage.CONTACT) } }
        item { SettingsRow(Icons.Outlined.Info, R.string.about, null, Modifier.tutorialTarget(TutorialTarget.SETTINGS_ABOUT)) { onPage(SettingsPage.ABOUT) } }
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: Int, subtitle: Int?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Surface(onClick = onClick, shape = MaterialTheme.shapes.large, tonalElevation = 1.dp, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                subtitle?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.scale(scaleX = if (isRtl) -1f else 1f, scaleY = 1f)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppearanceSelector(appearance: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.appearance), style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "system" to R.string.appearance_system,
                "light" to R.string.appearance_light,
                "dark" to R.string.appearance_dark
            ).forEach { (value, label) ->
                FilterChip(
                    selected = appearance == value,
                    onClick = { onChange(value) },
                    label = { Text(stringResource(label)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

@Composable
private fun LanguageSelector(language: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp)
        ) {
            listOf("en" to R.string.english, "ar" to R.string.arabic).forEach { (value, label) ->
                val selectedColor by animateColorAsState(if (language == value) MaterialTheme.colorScheme.primary else Color.Transparent, label = "languageColor")
                Surface(
                    onClick = { onChange(value) },
                    shape = MaterialTheme.shapes.small,
                    color = selectedColor,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        stringResource(label),
                        Modifier.padding(vertical = 11.dp),
                        textAlign = TextAlign.Center,
                        color = if (language == value) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportScreen(profileId: Long, userName: String, openHistory: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember(profileId) { GlucoseRepository(SugarTrackDatabase.get(context), profileId) }
    val readings by repo.all.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    
    val excelLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            runCatching { context.contentResolver.openOutputStream(uri)?.use { ExcelExporter.write(context, readings, it) } }
        }
    }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            runCatching { context.contentResolver.openOutputStream(uri)?.use { PdfExporter.write(context, readings, userName, it) } }
        }
    }
    
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.back))
        }
        Text(stringResource(R.string.export_data), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.export_data_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        GlucoPrimaryButton(
            text = stringResource(R.string.export_excel),
            icon = Icons.Outlined.Description,
            onClick = { excelLauncher.launch("${context.getString(R.string.app_name)}_Readings_${java.time.LocalDate.now()}.xlsx") },
            modifier = Modifier.fillMaxWidth(),
            enabled = readings.isNotEmpty()
        )
        
        GlucoOutlinedButton(
            text = stringResource(R.string.export_pdf),
            icon = Icons.Outlined.PictureAsPdf,
            onClick = { pdfLauncher.launch("${context.getString(R.string.app_name)}_Readings_${java.time.LocalDate.now()}.pdf") },
            modifier = Modifier.fillMaxWidth(),
            enabled = readings.isNotEmpty()
        )
        
        TextButton(onClick = openHistory) { Text(stringResource(R.string.history)) }
    }
}

// ─── LTR-safe text style (for phone/email in any locale) ───────────────────
private val ltrTextStyle: TextStyle @Composable get() =
    MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr)

// ─── Copy helper ────────────────────────────────────────────────────────────
private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

// ─── Developer Info Card ────────────────────────────────────────────────────
@Composable
private fun DeveloperInfoCard() {
    val context = LocalContext.current
    GlucoCard(modifier = Modifier.fillMaxWidth(), paddingValues = PaddingValues(0.dp)) {
        Column {
            // Header band with avatar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.large
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Avatar circle
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    Text(
                        stringResource(R.string.developer_name),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            // Contact info rows
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Phone row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.developer_phone), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(DeveloperInfo.DISPLAY_PHONE, style = ltrTextStyle)
                    }
                    IconButton(onClick = { copyToClipboard(context, "Phone", DeveloperInfo.DISPLAY_PHONE) }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = stringResource(R.string.copy_phone), modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
                // Email row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.developer_email), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(DeveloperInfo.EMAIL, style = ltrTextStyle)
                    }
                    IconButton(onClick = { copyToClipboard(context, "Email", DeveloperInfo.EMAIL) }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = stringResource(R.string.copy_email), modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ─── Contact Screen ──────────────────────────────────────────────────────────
@Composable
private fun ContactScreen() {
    Text(stringResource(R.string.contact_developer), style = MaterialTheme.typography.headlineMedium)
    DeveloperInfoCard()
    Spacer(Modifier.height(4.dp))
    DeveloperContactActions()
}

// ─── Support Screen ──────────────────────────────────────────────────────────
@Composable
private fun SupportScreen() {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    Text(stringResource(R.string.support_app), style = MaterialTheme.typography.headlineMedium)

    // Description
    GlucoCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.support_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    // QR Card — the real QR must remain unaltered and unclipped
    GlucoCard(modifier = Modifier.fillMaxWidth(), paddingValues = PaddingValues(0.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Card header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.large
                    )
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        stringResource(R.string.support_app),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Body text above QR
            Text(
                stringResource(R.string.support_qr_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)
            )

            // Real QR code — white quiet-zone around it, no clipping, no gradients
            Surface(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = Color.White,
                tonalElevation = 0.dp
            ) {
                Image(
                    painter = painterResource(R.drawable.whish_support_qr),
                    contentDescription = stringResource(R.string.support_whish),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .aspectRatio(1f)
                )
            }

            // Scan label
            Text(
                stringResource(R.string.support_scan_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // Whish number (LTR-safe)
            Text(
                stringResource(R.string.support_number),
                style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Ltr),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }

    // Share QR action (if URL available)
    if (DeveloperLinks.WHISH_SUPPORT_URL.isNotBlank()) {
        GlucoOutlinedButton(
            text = stringResource(R.string.support_whish),
            icon = Icons.Outlined.QrCode,
            onClick = { uriHandler.openUri(DeveloperLinks.WHISH_SUPPORT_URL) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ─── Services Screen ─────────────────────────────────────────────────────────
@Composable
private fun ServicesScreen() {
    Text(stringResource(R.string.need_app_or_website), style = MaterialTheme.typography.headlineMedium)
    Text(stringResource(R.string.services_body), style = MaterialTheme.typography.bodyLarge)
    listOf(R.string.android_services, R.string.web_services, R.string.custom_services).forEach { label ->
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(label), Modifier.padding(18.dp), style = MaterialTheme.typography.titleMedium)
        }
    }
    DeveloperContactActions()
}

// ─── About Screen ────────────────────────────────────────────────────────────
@Composable
private fun AboutScreen() {
    val context = LocalContext.current
    val packageInfo = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    Text(stringResource(R.string.about), style = MaterialTheme.typography.headlineMedium)

    // App info card
    GlucoCard(modifier = Modifier.fillMaxWidth(), paddingValues = PaddingValues(0.dp)) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.large)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Image(
                        painter = painterResource(R.drawable.ic_glucotime_logo),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        stringResource(R.string.version, packageInfo?.versionName ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.about_description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    Spacer(Modifier.height(4.dp))
    Text(stringResource(R.string.developer), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    DeveloperInfoCard()
    Spacer(Modifier.height(4.dp))
    DeveloperContactActions()
}

// ─── Developer Contact Action Buttons ────────────────────────────────────────
@Composable
private fun DeveloperContactActions() {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    // Three equal action buttons
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Call
        OutlinedCard(
            onClick = { uriHandler.openUri(DeveloperLinks.PHONE_URL) },
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.large,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Column(
                Modifier.padding(vertical = 14.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Outlined.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.call), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        // WhatsApp
        OutlinedCard(
            onClick = { uriHandler.openUri(DeveloperLinks.WHATSAPP_URL) },
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.large,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Column(
                Modifier.padding(vertical = 14.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Outlined.Forum, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.whatsapp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        // Email
        OutlinedCard(
            onClick = {
                val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                    data = android.net.Uri.parse("mailto:")
                    putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(DeveloperInfo.EMAIL))
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "GlucoTime App")
                }
                context.startActivity(intent)
            },
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.large,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Column(
                Modifier.padding(vertical = 14.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Outlined.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.send_email), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
