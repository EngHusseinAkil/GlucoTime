package com.sugartrack.ui.tutorial

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sugartrack.R

@Composable
fun TutorialOverlay(
    controller: TutorialController,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()

        if (controller.active) {
            val step = controller.currentStep
            val targetKey = when (step) {
                TutorialStep.ADD_CURRENT_READING -> TutorialTarget.ADD_READING_BUTTON
                TutorialStep.ADD_PAST_READING -> TutorialTarget.ADD_READING_BUTTON
                TutorialStep.HISTORY -> TutorialTarget.HISTORY_TAB
                TutorialStep.HISTORY_EDIT -> TutorialTarget.HISTORY_ROW
                TutorialStep.HISTORY_DELETE -> TutorialTarget.HISTORY_ROW
                TutorialStep.ADD_REMINDER -> TutorialTarget.ADD_REMINDER_BUTTON
                TutorialStep.SETTINGS_USERS -> TutorialTarget.SETTINGS_USERS
                TutorialStep.SETTINGS_REMINDERS -> TutorialTarget.SETTINGS_REMINDERS
                TutorialStep.SETTINGS_APPEARANCE -> TutorialTarget.SETTINGS_APPEARANCE
                    TutorialStep.SETTINGS_LANGUAGE -> TutorialTarget.SETTINGS_LANGUAGE
                    TutorialStep.SETTINGS_EXPORT -> TutorialTarget.SETTINGS_EXPORT
                    TutorialStep.SETTINGS_ANALYSIS -> TutorialTarget.SETTINGS_ANALYSIS
                    TutorialStep.SETTINGS_APP_TUTORIAL -> TutorialTarget.SETTINGS_APP_TUTORIAL
                    TutorialStep.SETTINGS_SUPPORT -> TutorialTarget.SETTINGS_SUPPORT
                    TutorialStep.SETTINGS_DEVELOPER_SERVICES -> TutorialTarget.SETTINGS_DEVELOPER_SERVICES
                    TutorialStep.SETTINGS_CONTACT_DEVELOPER -> TutorialTarget.SETTINGS_CONTACT_DEVELOPER
                    TutorialStep.SETTINGS_ABOUT -> TutorialTarget.SETTINGS_ABOUT
                TutorialStep.COMPLETE -> null
            }

            val bounds = targetKey?.let { controller.targets[it] }
            // If an action screen replaced the highlighted control, do not put a
            // scrim over the form. The real controls must remain usable.
            val showCoachMark = !step.isActionRequired() || bounds != null

            val infiniteTransition = rememberInfiniteTransition()
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 0.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )

            if (showCoachMark) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    with(drawContext.canvas.nativeCanvas) {
                        val checkPoint = saveLayer(null, null)

                        drawRect(
                            color = Color(0xFF031024).copy(alpha = 0.85f),
                            size = size
                        )

                        bounds?.let {
                            val padding = 12.dp.toPx()
                            val cutoutRect = Rect(
                                left = it.left - padding,
                                top = it.top - padding,
                                right = it.right + padding,
                                bottom = it.bottom + padding
                            )

                            drawRoundRect(
                                color = Color.Transparent,
                                topLeft = cutoutRect.topLeft,
                                size = cutoutRect.size,
                                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                                blendMode = BlendMode.Clear
                            )

                            drawRoundRect(
                                color = Color(0xFF38B6FF).copy(alpha = pulseAlpha),
                                topLeft = cutoutRect.topLeft,
                                size = cutoutRect.size,
                                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }

                        restoreToCount(checkPoint)
                    }
                }

                val instructionResource = when (step) {
                    TutorialStep.ADD_CURRENT_READING -> R.string.tutorial_add_current_reading
                    TutorialStep.ADD_PAST_READING -> R.string.tutorial_add_past_reading
                    TutorialStep.HISTORY -> R.string.tutorial_history
                    TutorialStep.HISTORY_EDIT -> R.string.tutorial_history_edit
                    TutorialStep.HISTORY_DELETE -> R.string.tutorial_history_delete
                    TutorialStep.ADD_REMINDER -> R.string.tutorial_add_reminder
                    TutorialStep.SETTINGS_USERS -> R.string.tutorial_settings_users
                    TutorialStep.SETTINGS_REMINDERS -> R.string.tutorial_settings_reminders
                    TutorialStep.SETTINGS_APPEARANCE -> R.string.tutorial_settings_appearance
                    TutorialStep.SETTINGS_LANGUAGE -> R.string.tutorial_settings_language
                    TutorialStep.SETTINGS_EXPORT -> R.string.tutorial_settings_export
                    TutorialStep.SETTINGS_ANALYSIS -> R.string.tutorial_settings_analysis
                    TutorialStep.SETTINGS_APP_TUTORIAL -> R.string.tutorial_settings_app_tutorial
                    TutorialStep.SETTINGS_SUPPORT -> R.string.tutorial_settings_support
                    TutorialStep.SETTINGS_DEVELOPER_SERVICES -> R.string.tutorial_settings_developer_services
                    TutorialStep.SETTINGS_CONTACT_DEVELOPER -> R.string.tutorial_settings_contact_developer
                    TutorialStep.SETTINGS_ABOUT -> R.string.tutorial_settings_about
                    TutorialStep.COMPLETE -> null
                }

                // No pointer handler is attached to this layer. Action-required
                // steps therefore pass taps through to the real app controls.
                if (instructionResource != null) {
                    Column(
                        modifier = if (step.isActionRequired()) {
                            Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 32.dp, vertical = 72.dp)
                                .navigationBarsPadding()
                        } else {
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 32.dp)
                                .navigationBarsPadding()
                        },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(instructionResource),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        if (!step.isActionRequired()) {
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { controller.next() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF38B6FF),
                                    contentColor = Color.White
                                )
                            ) {
                                Text(stringResource(R.string.tutorial_got_it))
                            }
                        }
                    }
                }
            }

            // Skip remains available even while an action-required screen is open.
            TextButton(
                onClick = { controller.skip() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .navigationBarsPadding(),
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF94A3B8))
            ) {
                Text(stringResource(R.string.skip))
            }
        }
    }
}
