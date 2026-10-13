package com.sugartrack.ui.tutorial

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

enum class TutorialTarget {
    ADD_READING_BUTTON,
    HISTORY_TAB,
    HISTORY_ROW,
    REMINDERS_TAB,
    ADD_REMINDER_BUTTON,
    SETTINGS_TAB,
    SETTINGS_USERS,
    SETTINGS_REMINDERS,
    SETTINGS_APPEARANCE,
    SETTINGS_LANGUAGE,
    SETTINGS_EXPORT,
    SETTINGS_ANALYSIS,
    SETTINGS_APP_TUTORIAL,
    SETTINGS_SUPPORT,
    SETTINGS_DEVELOPER_SERVICES,
    SETTINGS_CONTACT_DEVELOPER,
    SETTINGS_ABOUT
}

enum class TutorialStepType {
    ACTION_REQUIRED,
    EXPLANATION_ONLY
}

enum class TutorialStep(val type: TutorialStepType) {
    ADD_CURRENT_READING(TutorialStepType.ACTION_REQUIRED),
    ADD_PAST_READING(TutorialStepType.ACTION_REQUIRED),
    HISTORY(TutorialStepType.EXPLANATION_ONLY),
    HISTORY_EDIT(TutorialStepType.ACTION_REQUIRED),
    HISTORY_DELETE(TutorialStepType.ACTION_REQUIRED),
    ADD_REMINDER(TutorialStepType.ACTION_REQUIRED),
    SETTINGS_USERS(TutorialStepType.EXPLANATION_ONLY),
    SETTINGS_REMINDERS(TutorialStepType.EXPLANATION_ONLY),
    SETTINGS_APPEARANCE(TutorialStepType.EXPLANATION_ONLY),
    SETTINGS_LANGUAGE(TutorialStepType.EXPLANATION_ONLY),
    SETTINGS_EXPORT(TutorialStepType.EXPLANATION_ONLY),
    SETTINGS_ANALYSIS(TutorialStepType.EXPLANATION_ONLY),
    SETTINGS_APP_TUTORIAL(TutorialStepType.EXPLANATION_ONLY),
    SETTINGS_SUPPORT(TutorialStepType.EXPLANATION_ONLY),
    SETTINGS_DEVELOPER_SERVICES(TutorialStepType.EXPLANATION_ONLY),
    SETTINGS_CONTACT_DEVELOPER(TutorialStepType.EXPLANATION_ONLY),
    SETTINGS_ABOUT(TutorialStepType.EXPLANATION_ONLY),
    COMPLETE(TutorialStepType.EXPLANATION_ONLY);
    
    fun isActionRequired(): Boolean = type == TutorialStepType.ACTION_REQUIRED
}

class TutorialController {
    var active by mutableStateOf(false)
        private set
        
    var currentStep by mutableStateOf(TutorialStep.ADD_CURRENT_READING)
        private set

    val targets = mutableStateMapOf<TutorialTarget, Rect>()

    private var tutorialReadingIds by mutableStateOf<Set<Long>>(emptySet())
    private var firstReadingMeasuredAt by mutableStateOf<Long?>(null)

    val createdReadingIds: Set<Long>
        get() = tutorialReadingIds

    fun start() {
        active = true
        currentStep = TutorialStep.ADD_CURRENT_READING
        tutorialReadingIds = emptySet()
        firstReadingMeasuredAt = null
        targets.clear()
    }

    fun finish() {
        active = false
        currentStep = TutorialStep.COMPLETE
        targets.clear()
        tutorialReadingIds = emptySet()
        firstReadingMeasuredAt = null
    }
    
    fun skip() {
        finish()
    }

    fun clearTarget(target: TutorialTarget) {
        targets.remove(target)
    }

    fun next() {
        if (!active) return
        val nextStep = when (currentStep) {
            TutorialStep.ADD_CURRENT_READING -> TutorialStep.ADD_PAST_READING
            TutorialStep.ADD_PAST_READING -> TutorialStep.HISTORY
            TutorialStep.HISTORY -> TutorialStep.HISTORY_EDIT
            TutorialStep.HISTORY_EDIT -> TutorialStep.HISTORY_DELETE
            TutorialStep.HISTORY_DELETE -> TutorialStep.ADD_REMINDER
            TutorialStep.ADD_REMINDER -> TutorialStep.SETTINGS_USERS
            TutorialStep.SETTINGS_USERS -> TutorialStep.SETTINGS_REMINDERS
            TutorialStep.SETTINGS_REMINDERS -> TutorialStep.SETTINGS_APPEARANCE
            TutorialStep.SETTINGS_APPEARANCE -> TutorialStep.SETTINGS_LANGUAGE
            TutorialStep.SETTINGS_LANGUAGE -> TutorialStep.SETTINGS_EXPORT
            TutorialStep.SETTINGS_EXPORT -> TutorialStep.SETTINGS_ANALYSIS
            TutorialStep.SETTINGS_ANALYSIS -> TutorialStep.SETTINGS_APP_TUTORIAL
            TutorialStep.SETTINGS_APP_TUTORIAL -> TutorialStep.SETTINGS_SUPPORT
            TutorialStep.SETTINGS_SUPPORT -> TutorialStep.SETTINGS_DEVELOPER_SERVICES
            TutorialStep.SETTINGS_DEVELOPER_SERVICES -> TutorialStep.SETTINGS_CONTACT_DEVELOPER
            TutorialStep.SETTINGS_CONTACT_DEVELOPER -> TutorialStep.SETTINGS_ABOUT
            TutorialStep.SETTINGS_ABOUT -> TutorialStep.COMPLETE
            TutorialStep.COMPLETE -> TutorialStep.COMPLETE
        }
        currentStep = nextStep
        targets.clear()
        if (currentStep == TutorialStep.COMPLETE) {
            finish()
        }
    }
    
    /** Called only after a newly entered reading has been inserted successfully. */
    fun onReadingSaved(readingId: Long, measuredAt: Long) {
        if (!active || currentStep != TutorialStep.ADD_CURRENT_READING) return

        val now = System.currentTimeMillis()
        val isCurrent = measuredAt <= now && measuredAt >= now - 24 * 60 * 60 * 1000L
        if (!isCurrent) return

        tutorialReadingIds = tutorialReadingIds + readingId
        firstReadingMeasuredAt = measuredAt
        next()
    }

    /** Called only after a newly entered past reading has been inserted successfully. */
    fun onPastReadingSaved(readingId: Long, measuredAt: Long) {
        if (!active || currentStep != TutorialStep.ADD_PAST_READING) return

        val firstMeasuredAt = firstReadingMeasuredAt ?: return
        if (measuredAt >= firstMeasuredAt || measuredAt >= System.currentTimeMillis()) return

        tutorialReadingIds = tutorialReadingIds + readingId
        next()
    }

    /** Called only after an existing reading has been updated successfully. */
    fun onReadingUpdated(readingId: Long, changed: Boolean) {
        if (!active || currentStep != TutorialStep.HISTORY_EDIT || !changed) return
        if (!isTutorialReading(readingId)) return
        next()
    }

    /** Called only after the confirmed deletion has completed successfully. */
    fun onReadingDeleted(readingId: Long) {
        if (!active || currentStep != TutorialStep.HISTORY_DELETE) return
        if (!isTutorialReading(readingId)) return
        next()
    }

    /** Called only after a new reminder has been persisted successfully. */
    fun onReminderSaved() {
        if (!active || currentStep != TutorialStep.ADD_REMINDER) return
        next()
    }

    fun isTutorialReading(readingId: Long): Boolean = readingId in tutorialReadingIds
}

val LocalTutorialController = staticCompositionLocalOf<TutorialController?> { null }

fun Modifier.tutorialTarget(target: TutorialTarget): Modifier = composed {
    val controller = LocalTutorialController.current
    this.then(
        Modifier.onGloballyPositioned { coordinates ->
            controller?.targets?.set(target, coordinates.boundsInRoot())
        }
    )
}
