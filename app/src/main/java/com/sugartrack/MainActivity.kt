package com.sugartrack

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sugartrack.data.*
import com.sugartrack.reminders.AlarmOccurrenceState
import com.sugartrack.reminders.ReminderScheduler
import com.sugartrack.ui.screens.analysis.AnalysisScreen
import com.sugartrack.ui.screens.history.HistoryScreen
import com.sugartrack.ui.screens.home.HomeScreen
import com.sugartrack.ui.screens.onboarding.OnboardingScreen
import com.sugartrack.ui.screens.reading.AddEditReadingScreen
import com.sugartrack.ui.screens.reminders.RemindersScreen
import com.sugartrack.ui.screens.settings.SettingsScreen
import com.sugartrack.ui.tutorial.*
import com.sugartrack.ui.theme.GlucoTimeTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private data class AlarmNavigationRequest(
        val occurrence: Long?,
        val reminderId: Long?,
        val profileId: Long?
    )

    private var alarmNavigationRequest by mutableStateOf<AlarmNavigationRequest?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIntentExtras(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        intent?.let { consumeIntentExtras(it) }

        setContent {
            SugarTrackApp()
        }
    }
    
    private fun consumeIntentExtras(intent: Intent) {
        // Copy the complete one-time navigation request before consuming the
        // extras, so recreation cannot reopen a stale Add Reading screen.
        if (intent.getBooleanExtra(OPEN_ADD_READING, false)) {
            alarmNavigationRequest = AlarmNavigationRequest(
                occurrence = intent.getLongExtra(ALARM_OCCURRENCE, -1L).takeIf { it >= 0L },
                reminderId = intent.getLongExtra(ALARM_REMINDER_ID, -1L).takeIf { it >= 0L },
                profileId = intent.getLongExtra(ALARM_PROFILE_ID, -1L).takeIf { it >= 0L }
            )
        }
        intent.removeExtra(OPEN_ADD_READING)
        intent.removeExtra(ALARM_OCCURRENCE)
        intent.removeExtra(ALARM_REMINDER_ID)
        intent.removeExtra(ALARM_PROFILE_ID)
    }

    @Composable
    private fun SugarTrackApp() {
        val scope = rememberCoroutineScope()
        val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
        var showNotificationPermissionDialog by remember { mutableStateOf(false) }
        val database = remember { SugarTrackDatabase.get(applicationContext) }
        val profileRepository = remember { ProfileRepository(database) }
        val profiles by profileRepository.all.collectAsStateWithLifecycle(initialValue = emptyList())
        var currentProfileId by remember { mutableStateOf<Long?>(null) }

        val language = AppCompatDelegate
            .getApplicationLocales()
            .toLanguageTags()
            .substringBefore("-")
            .ifBlank { "en" }

        var completed by remember { mutableStateOf<Boolean?>(null) }
        var tutorialCompleted by remember { mutableStateOf<Boolean?>(null) }
        var preferencesLoaded by remember { mutableStateOf(false) }
        var autoTutorialStarted by remember { mutableStateOf(false) }
        var name by remember { mutableStateOf("") }
        var appearance by remember { mutableStateOf("system") }

        fun changeLanguage(value: String) {
            val currentLanguage = AppCompatDelegate
                .getApplicationLocales()
                .toLanguageTags()
                .substringBefore("-")
                .ifBlank { "en" }
            if (value == currentLanguage) return

            scope.launch { UserPreferences(applicationContext).saveLanguage(value) }
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(value))
        }

        LaunchedEffect(Unit) {
            val preferences = UserPreferences(applicationContext)
            val savedLanguage = preferences.readLanguage()

            if (AppCompatDelegate.getApplicationLocales().isEmpty) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(savedLanguage))
            }

            completed = preferences.completed()
            tutorialCompleted = preferences.tutorialCompleted()
            name = preferences.readName()
            appearance = preferences.readAppearance()
            profileRepository.ensureDefault(name)
            currentProfileId = preferences.readCurrentProfileId() ?: 1L
            preferencesLoaded = true
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                showNotificationPermissionDialog = true
            }
        }

        LaunchedEffect(profiles, currentProfileId) {
            val selected = profiles.firstOrNull { it.id == currentProfileId } ?: profiles.firstOrNull()
            if (selected != null && selected.id != currentProfileId) {
                currentProfileId = selected.id
                UserPreferences(applicationContext).saveCurrentProfileId(selected.id)
            }
        }
        
        LaunchedEffect(alarmNavigationRequest?.profileId) {
            alarmNavigationRequest?.profileId?.let { currentProfileId = it }
        }

        var startupFinished by remember { mutableStateOf(false) }
        var startupLogoVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            startupLogoVisible = true
            delay(650)
            startupFinished = true
        }

        val isDark = when (appearance) {
            "dark" -> true
            "light" -> false
            else -> androidx.compose.foundation.isSystemInDarkTheme()
        }
        
        val tutorialController = remember { TutorialController() }

        // Start tutorial when onboarding completes for the first time
        LaunchedEffect(completed, tutorialCompleted, preferencesLoaded) {
            if (preferencesLoaded && completed == true && tutorialCompleted == false && !autoTutorialStarted) {
                autoTutorialStarted = true
                tutorialController.start()
            }
        }

        // Complete tutorial in preferences if controller finishes
        LaunchedEffect(tutorialController.active) {
            if (preferencesLoaded && completed == true && tutorialCompleted == false && !tutorialController.active && tutorialController.currentStep == TutorialStep.COMPLETE) {
                tutorialCompleted = true
                UserPreferences(applicationContext).setTutorialCompleted(true)
            }
        }

        CompositionLocalProvider(
            LocalTutorialController provides tutorialController,
            LocalLayoutDirection provides if (language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
        ) {
            GlucoTimeTheme(darkTheme = isDark) {
            if (!startupFinished) {
                StartupBrand(visible = startupLogoVisible)
            } else when (completed) {
                null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                false -> {
                    OnboardingScreen(
                        complete = { enteredName ->
                            scope.launch {
                                UserPreferences(applicationContext).complete(enteredName)
                                completed = true
                                name = enteredName
                                val profile = profileRepository.ensureDefault(enteredName)
                                currentProfileId = profile.id
                                UserPreferences(applicationContext).saveCurrentProfileId(profile.id)
                            }
                        },
                        skip = { completed = true },
                        language = { changeLanguage(it) },
                        current = language
                    )
                }
                true -> {
                    val profile = profiles.firstOrNull { it.id == currentProfileId } ?: profiles.firstOrNull()
                    if (profile == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val mainContent: @Composable () -> Unit = {
                            MainContent(
                            userName = profile.name,
                            profileId = profile.id,
                            profiles = profiles,
                            onProfileSelected = { selected ->
                                currentProfileId = selected.id
                                scope.launch { UserPreferences(applicationContext).saveCurrentProfileId(selected.id) }
                            },
                            onProfileAdded = { profileName ->
                                scope.launch {
                                    val id = profileRepository.add(profileName)
                                    currentProfileId = id
                                    UserPreferences(applicationContext).saveCurrentProfileId(id)
                                }
                            },
                            onProfileUpdated = { updated -> scope.launch { profileRepository.update(updated) } },
                            onProfileDeleted = { deleted ->
                                scope.launch {
                                    ReminderPreferences(applicationContext).read(deleted.id).forEach { ReminderScheduler(applicationContext).cancel(it.id, deleted.id) }
                                    ReminderPreferences(applicationContext).deleteProfile(deleted.id)
                                    AlarmOccurrenceState.clearProfile(applicationContext, deleted.id)
                                    profileRepository.deleteData(deleted.id)
                                    profiles.firstOrNull { it.id != deleted.id }?.let {
                                        currentProfileId = it.id
                                        UserPreferences(applicationContext).saveCurrentProfileId(it.id)
                                    }
                                }
                            },
                            language = language,
                            onLanguage = { changeLanguage(it) },
                            appearance = appearance,
                            onAppearance = {
                                appearance = it
                                scope.launch { UserPreferences(applicationContext).saveAppearance(it) }
                            },
                            onReplayTutorial = { tutorialController.start() },
                            onAlarmNavigationFinished = { alarmNavigationRequest = null }
                            )
                        }
                        if (tutorialController.active && tutorialController.currentStep != TutorialStep.COMPLETE) {
                            TutorialOverlay(controller = tutorialController, content = mainContent)
                        } else {
                            mainContent()
                        }
                    }
                }
            }
            if (startupFinished && showNotificationPermissionDialog) {
                AlertDialog(
                    onDismissRequest = { showNotificationPermissionDialog = false },
                    title = { Text(stringResource(R.string.notification_permission_title)) },
                    text = { Text(stringResource(R.string.notification_permission_message)) },
                    dismissButton = { TextButton(onClick = { showNotificationPermissionDialog = false }) { Text(stringResource(R.string.cancel)) } },
                    confirmButton = {
                        TextButton(onClick = {
                            showNotificationPermissionDialog = false
                            if (Build.VERSION.SDK_INT >= 33) notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }) { Text(stringResource(R.string.notification_permission_action)) }
                    }
                )
            }
        }
        }
    }

    @Composable
    private fun StartupBrand(visible: Boolean) {
        val alpha by animateFloatAsState(if (visible) 1f else 0f, label = "startupAlpha")
        val scale by animateFloatAsState(if (visible) 1f else 0.9f, label = "startupScale")
        Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.alpha(alpha).scale(scale)) {
                Icon(painterResource(R.drawable.ic_glucotime_logo), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(116.dp))
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    @Composable
    private fun MainContent(
        userName: String,
        profileId: Long,
        profiles: List<UserProfile>,
        onProfileSelected: (UserProfile) -> Unit,
        onProfileAdded: (String) -> Unit,
        onProfileUpdated: (UserProfile) -> Unit,
        onProfileDeleted: (UserProfile) -> Unit,
        language: String,
        onLanguage: (String) -> Unit,
        appearance: String,
        onAppearance: (String) -> Unit,
        onReplayTutorial: () -> Unit,
        onAlarmNavigationFinished: () -> Unit
    ) {
        var tab by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(0) }
        var settingsPage by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(SettingsPage.ROOT) }
        val tutorial = LocalTutorialController.current
        // Coordinate tabs with the tutorial state automatically
        LaunchedEffect(tutorial?.currentStep) {
            if (tutorial?.active == true) {
                when (tutorial.currentStep) {
                    TutorialStep.ADD_CURRENT_READING, TutorialStep.ADD_PAST_READING -> tab = 0
                    TutorialStep.HISTORY -> tab = 1
                    TutorialStep.HISTORY_EDIT, TutorialStep.HISTORY_DELETE -> tab = 1
                    TutorialStep.ADD_REMINDER -> tab = 2
                    TutorialStep.SETTINGS_USERS,
                    TutorialStep.SETTINGS_REMINDERS,
                    TutorialStep.SETTINGS_APPEARANCE,
                    TutorialStep.SETTINGS_LANGUAGE,
                    TutorialStep.SETTINGS_EXPORT,
                    TutorialStep.SETTINGS_ANALYSIS,
                    TutorialStep.SETTINGS_APP_TUTORIAL,
                    TutorialStep.SETTINGS_SUPPORT,
                    TutorialStep.SETTINGS_DEVELOPER_SERVICES,
                    TutorialStep.SETTINGS_CONTACT_DEVELOPER,
                    TutorialStep.SETTINGS_ABOUT -> {
                        tab = 3
                        settingsPage = SettingsPage.ROOT
                    }
                    TutorialStep.COMPLETE -> {
                        tab = 0
                        settingsPage = SettingsPage.ROOT
                    }
                }
            } else if (tutorial?.currentStep == TutorialStep.COMPLETE) {
                tab = 0
                settingsPage = SettingsPage.ROOT
            }
        }

        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    listOf(
                        R.string.home to Icons.Outlined.Home,
                        R.string.history to Icons.Outlined.History,
                        R.string.reminders to Icons.Outlined.Notifications,
                        R.string.settings to Icons.Outlined.Settings
                    ).forEachIndexed { index, (resource, icon) ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { tab = index },
                            icon = { Icon(icon, contentDescription = null) },
                            label = { Text(stringResource(resource)) },
                            modifier = when (index) {
                                1 -> Modifier.tutorialTarget(TutorialTarget.HISTORY_TAB)
                                2 -> Modifier.tutorialTarget(TutorialTarget.REMINDERS_TAB)
                                3 -> Modifier.tutorialTarget(TutorialTarget.SETTINGS_TAB)
                                else -> Modifier
                            }
                        )
                    }
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                val repo = remember(profileId) { GlucoseRepository(SugarTrackDatabase.get(applicationContext), profileId) }
                var editingReading by remember { mutableStateOf<GlucoseReading?>(null) }

                when (tab) {
                    0 -> {
                        HomeScreen(
                            userName = userName,
                            profileId = profileId,
                            language = language,
                            onSetReminder = { tab = 2 },
                            startAdding = alarmNavigationRequest != null,
                            alarmOccurrence = alarmNavigationRequest?.occurrence,
                            alarmReminderId = alarmNavigationRequest?.reminderId,
                            alarmProfileId = alarmNavigationRequest?.profileId,
                            onAlarmNavigationFinished = onAlarmNavigationFinished,
                            onAddReadingAction = { tutorial?.clearTarget(TutorialTarget.ADD_READING_BUTTON) }
                        )
                    }
                    1 -> {
                        if (editingReading == null) {
                            HistoryScreen(profileId = profileId, userName = userName, onEdit = { editingReading = it })
                        } else {
                            AddEditReadingScreen(
                                repo = repo,
                                initial = editingReading,
                                done = { editingReading = null }
                            )
                        }
                    }
                    2 -> {
                        RemindersScreen(profileId)
                    }
                    else -> {
                        SettingsScreen(
                            page = settingsPage,
                            userName = userName,
                            profiles = profiles,
                            profileId = profileId,
                            language = language,
                            appearance = appearance,
                            onPage = { settingsPage = it },
                            onBack = { settingsPage = SettingsPage.ROOT },
                            onProfileSelected = onProfileSelected,
                            onProfileAdded = onProfileAdded,
                            onProfileUpdated = onProfileUpdated,
                            onProfileDeleted = onProfileDeleted,
                            onLanguage = onLanguage,
                            onAppearance = onAppearance,
                            openHistory = { tab = 1 },
                            openReminders = { tab = 2 },
                            openAnalysis = { settingsPage = SettingsPage.ANALYSIS },
                            onReplayTutorial = onReplayTutorial
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val OPEN_ADD_READING = "open_add_reading"
        const val ALARM_OCCURRENCE = "alarm_occurrence"
        const val ALARM_REMINDER_ID = "alarm_reminder_id"
        const val ALARM_PROFILE_ID = "alarm_profile_id"
    }
}
