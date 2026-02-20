/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.ime.smartbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speekez.api.ApiRouterManager
import com.speekez.core.ApiMode
import com.speekez.security.EncryptedPreferencesManager
import com.speekez.voice.VoiceState
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.ime.input.LocalInputFeedbackController
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.nlp.NlpInlineAutofill
import dev.patrickgold.florisboard.ime.smartbar.quickaction.QuickActionButton
import dev.patrickgold.florisboard.ime.smartbar.quickaction.QuickActionsRow
import dev.patrickgold.florisboard.ime.smartbar.quickaction.ToggleOverflowPanelAction
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import com.speekez.data.presetDao
import com.speekez.voice.voiceManager
import dev.patrickgold.florisboard.nlpManager
import kotlinx.coroutines.withTimeoutOrNull
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.compose.horizontalTween
import org.florisboard.lib.compose.verticalTween
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggColumn
import org.florisboard.lib.snygg.ui.SnyggIcon
import org.florisboard.lib.snygg.ui.SnyggIconButton
import org.florisboard.lib.snygg.ui.SnyggRow
import org.florisboard.lib.snygg.ui.rememberSnyggThemeQuery

const val AnimationDuration = 200

val VerticalEnterTransition = EnterTransition.verticalTween(AnimationDuration)
val VerticalExitTransition = ExitTransition.verticalTween(AnimationDuration)

private val HorizontalEnterTransition = EnterTransition.horizontalTween(AnimationDuration)
private val HorizontalExitTransition = ExitTransition.horizontalTween(AnimationDuration)

private val NoEnterTransition = EnterTransition.horizontalTween(0)
private val NoExitTransition = ExitTransition.horizontalTween(0)

private val AnimationTween = tween<Float>(AnimationDuration)
private val NoAnimationTween = tween<Float>(0)

@Composable
fun Smartbar() {
    val prefs by FlorisPreferenceStore
    val smartbarEnabled by prefs.smartbar.enabled.collectAsState()
    val smartbarLayout by prefs.smartbar.layout.collectAsState()
    val extendedActionsPlacement by prefs.smartbar.extendedActionsPlacement.collectAsState()

    AnimatedVisibility(
        visible = smartbarEnabled,
        enter = VerticalEnterTransition,
        exit = VerticalExitTransition,
    ) {
        if (smartbarLayout == SmartbarLayout.SPEEKEZ) {
            SpeekEZSmartbarMainRow()
        } else {
            when (extendedActionsPlacement) {
                ExtendedActionsPlacement.ABOVE_CANDIDATES -> {
                    SnyggColumn(FlorisImeUi.Smartbar.elementName) {
                        SmartbarSecondaryRow()
                        SmartbarMainRow()
                    }
                }

                ExtendedActionsPlacement.BELOW_CANDIDATES -> {
                    SnyggColumn(FlorisImeUi.Smartbar.elementName) {
                        SmartbarMainRow()
                        SmartbarSecondaryRow()
                    }
                }

                ExtendedActionsPlacement.OVERLAY_APP_UI -> {
                    SnyggBox(
                        FlorisImeUi.Smartbar.elementName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(FlorisImeSizing.smartbarHeight),
                        allowClip = false,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(FlorisImeSizing.smartbarHeight * 2)
                                .absoluteOffset(y = -FlorisImeSizing.smartbarHeight),
                            contentAlignment = Alignment.BottomStart,
                        ) {
                            SmartbarSecondaryRow()
                        }
                        SmartbarMainRow()
                    }
                }
            }
        }
    }
}

private val SpeekEZTeal = Color(0xFF00D4AA)
private val SpeekEZRed = Color(0xFFFF4444)
private val SpeekEZPurple = Color(0xFF8A2BE2)
private val SpeekEZGreen = Color(0xFF4CAF50)
private val SpeekEZOrange = Color(0xFFFF8844)

@Composable
private fun SpeekEZSmartbarMainRow(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs by FlorisPreferenceStore
    val keyboardManager by context.keyboardManager()
    val activeState by keyboardManager.activeState.collectAsState()
    val voiceManager by context.voiceManager()
    val inputFeedbackController = LocalInputFeedbackController.current
    val scope = rememberCoroutineScope()

    val voiceState by voiceManager.state.collectAsState()
    val errorMessage by voiceManager.errorMessage.collectAsState()

    val apiRouterManager = remember { ApiRouterManager(context, EncryptedPreferencesManager(context)) }
    val isNoApiKey = apiRouterManager.getApiMode() == ApiMode.NO_KEYS

    val presetDao = remember { context.presetDao() }
    val presets by presetDao.getAllPresets().collectAsState(initial = emptyList())
    val activePresetId by prefs.speekez.activePresetId.collectAsState()

    val displayPresets = remember(presets) { presets.reversed() }

    var timerSeconds by remember { mutableStateOf(0) }
    LaunchedEffect(voiceState) {
        if (voiceState == VoiceState.RECORDING) {
            timerSeconds = 0
            while (true) {
                kotlinx.coroutines.delay(1000)
                timerSeconds++
            }
        }
    }

    var showNoApiKeyBanner by remember { mutableStateOf(false) }
    LaunchedEffect(showNoApiKeyBanner) {
        if (showNoApiKeyBanner) {
            kotlinx.coroutines.delay(5000)
            showNoApiKeyBanner = false
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        SnyggRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(FlorisImeSizing.smartbarHeight)
                .padding(horizontal = 8.dp)
                .alpha(if (isNoApiKey) 0.4f else 1.0f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                if (voiceState == VoiceState.RECORDING) {
                    val minutes = timerSeconds / 60
                    val seconds = timerSeconds % 60
                    Text(
                        text = "%02d:%02d".format(minutes, seconds),
                        color = SpeekEZRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    AddPresetButton(onClick = {
                        if (isNoApiKey) {
                            showNoApiKeyBanner = true
                        }
                    })
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (voiceState == VoiceState.PROCESSING) {
                    Text(
                        text = "Transcribing...",
                        color = SpeekEZPurple,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                } else if (voiceState == VoiceState.ERROR) {
                    Text(
                        text = errorMessage ?: "Unknown Error",
                        color = Color.Red,
                        fontSize = 12.sp,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                } else {
                    displayPresets.forEach { preset ->
                        PresetChip(
                            preset = preset,
                            isActive = preset.id == activePresetId,
                            isRecording = voiceState == VoiceState.RECORDING,
                            onClick = {
                                if (isNoApiKey) {
                                    showNoApiKeyBanner = true
                                } else {
                                    scope.launch {
                                        prefs.speekez.activePresetId.set(preset.id)
                                    }
                                }
                            },
                            onHoldStart = {
                                if (isNoApiKey) {
                                    showNoApiKeyBanner = true
                                } else {
                                    voiceManager.startRecording(preset.id.toInt())
                                    keyboardManager.activeState.isRecording = true
                                    inputFeedbackController.keyLongPress()
                                }
                            },
                            onHoldEnd = {
                                voiceManager.stopRecording()
                                keyboardManager.activeState.isRecording = false
                                inputFeedbackController.keyPress()
                            },
                            onHoldCancel = {
                                voiceManager.cancelRecording()
                                keyboardManager.activeState.isRecording = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            MicButton(
                state = voiceState,
                onHoldStart = {
                    if (isNoApiKey) {
                        showNoApiKeyBanner = true
                    } else {
                        voiceManager.startRecording(activePresetId.toInt())
                        keyboardManager.activeState.isRecording = true
                        inputFeedbackController.keyLongPress()
                    }
                },
                onHoldEnd = {
                    voiceManager.stopRecording()
                    keyboardManager.activeState.isRecording = false
                    inputFeedbackController.keyPress()
                },
                onHoldCancel = {
                    voiceManager.cancelRecording()
                    keyboardManager.activeState.isRecording = false
                }
            )
        }

        AnimatedVisibility(
            visible = showNoApiKeyBanner,
            enter = fadeIn() + VerticalEnterTransition,
            exit = fadeOut() + VerticalExitTransition,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(FlorisImeSizing.smartbarHeight)
                    .background(SpeekEZOrange.copy(alpha = 0.12f))
                    .clickable {
                        showNoApiKeyBanner = false
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Need API Key",
                    color = SpeekEZOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun AddPresetButton(onClick: () -> Unit) {
    val stroke = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
    Box(
        modifier = Modifier
            .size(34.dp)
            .drawBehind {
                drawRoundRect(
                    color = Color.Gray.copy(alpha = 0.5f),
                    style = stroke,
                    cornerRadius = CornerRadius(17.dp.toPx())
                )
            }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add Preset",
            modifier = Modifier.size(20.dp),
            tint = Color.Gray
        )
    }
}

@Composable
private fun MicButton(
    state: VoiceState,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit,
    onHoldCancel: () -> Unit,
) {
    val currentOnHoldStart by rememberUpdatedState(onHoldStart)
    val currentOnHoldEnd by rememberUpdatedState(onHoldEnd)
    val currentOnHoldCancel by rememberUpdatedState(onHoldCancel)

    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse),
        label = "pulseScale"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "rotation"
    )
    val backgroundColor by animateColorAsState(
        targetValue = when (state) {
            VoiceState.IDLE -> SpeekEZTeal
            VoiceState.RECORDING -> SpeekEZRed
            VoiceState.PROCESSING -> SpeekEZPurple
            VoiceState.DONE -> SpeekEZGreen
            VoiceState.ERROR -> SpeekEZRed
        },
        animationSpec = tween(AnimationDuration), label = "backgroundColor"
    )

    Box(
        modifier = Modifier
            .size(34.dp)
            .let {
                when (state) {
                    VoiceState.RECORDING -> it.drawBehind {
                        drawCircle(SpeekEZRed.copy(alpha = 0.3f), radius = (size.minDimension / 2) * pulseScale, center = center)
                    }
                    VoiceState.PROCESSING -> it.drawBehind {
                        rotate(rotation) {
                            drawCircle(
                                brush = Brush.sweepGradient(listOf(SpeekEZPurple.copy(alpha = 0.1f), SpeekEZPurple, SpeekEZPurple.copy(alpha = 0.1f)), center = center),
                                radius = (size.minDimension / 2) + 4.dp.toPx(), style = Stroke(width = 3.dp.toPx())
                            )
                        }
                    }
                    else -> it
                }
            }
            .clip(CircleShape)
            .background(backgroundColor)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    var up: PointerInputChange? = null
                    val isTimeout = withTimeoutOrNull(200) {
                        up = waitForUpOrCancellation()
                        false
                    } ?: true

                    if (isTimeout) {
                        if (state == VoiceState.IDLE || state == VoiceState.ERROR || state == VoiceState.DONE) {
                            currentOnHoldStart()
                            val finalUp = waitForUpOrCancellation()
                            if (finalUp != null) {
                                currentOnHoldEnd()
                                finalUp.consume()
                            } else {
                                currentOnHoldCancel()
                            }
                        }
                    } else if (up != null) {
                        if (up!!.pressed != up!!.previousPressed) {
                            up!!.consume()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val icon = when (state) {
            VoiceState.IDLE, VoiceState.RECORDING, VoiceState.PROCESSING -> Icons.Default.Mic
            VoiceState.DONE -> Icons.Default.Check
            VoiceState.ERROR -> Icons.Default.Error
        }
        Icon(
            imageVector = icon,
            contentDescription = "Mic",
            modifier = Modifier.size(20.dp),
            tint = Color.White
        )
    }
}

@Composable
private fun SmartbarMainRow(modifier: Modifier = Modifier) {
    val prefs by FlorisPreferenceStore
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val nlpManager by context.nlpManager()
    val scope = rememberCoroutineScope()

    val inlineSuggestions by NlpInlineAutofill.suggestions.collectAsState()
    LaunchedEffect(inlineSuggestions) {
        nlpManager.autoExpandCollapseSmartbarActions(null, inlineSuggestions)
    }
    val shouldShowInlineSuggestionsUi = AndroidVersion.ATLEAST_API30_R && inlineSuggestions.isNotEmpty()

    val smartbarLayout by prefs.smartbar.layout.collectAsState()
    val flipToggles by prefs.smartbar.flipToggles.collectAsState()
    val sharedActionsExpanded by prefs.smartbar.sharedActionsExpanded.collectAsState()
    val extendedActionsExpanded by prefs.smartbar.extendedActionsExpanded.collectAsState()

    val shouldAnimate by prefs.smartbar.sharedActionsExpandWithAnimation.collectAsState()

    @Composable
    fun SharedActionsToggle() {
        SnyggIconButton(
            elementName = FlorisImeUi.SmartbarSharedActionsToggle.elementName,
            onClick = {
                if (/* was */ sharedActionsExpanded) {
                    keyboardManager.activeState.isActionsOverflowVisible = false
                }
                scope.launch {
                    prefs.smartbar.sharedActionsExpanded.set(!sharedActionsExpanded)
                }
            },
            modifier = Modifier.sizeIn(maxHeight = FlorisImeSizing.smartbarHeight).aspectRatio(1f)
        ) {
            val transition = updateTransition(sharedActionsExpanded, label = "sharedActionsExpandedToggleBtn")
            val rotation by transition.animateFloat(
                transitionSpec = {
                    if (shouldAnimate) AnimationTween else NoAnimationTween
                },
                label = "rotation",
            ) {
                if (it) 180f else 0f
            }
            val arrowIcon = if (flipToggles) {
                Icons.AutoMirrored.Default.KeyboardArrowLeft
            } else {
                Icons.AutoMirrored.Default.KeyboardArrowRight
            }
            val incognitoIcon = ImageVector.vectorResource(id = R.drawable.ic_incognito)
            val incognitoDisplayMode = prefs.keyboard.incognitoDisplayMode.collectAsState()
            val isIncognitoMode = keyboardManager.activeState.isIncognitoMode
            val icon = if (isIncognitoMode) {
                when (incognitoDisplayMode.value) {
                    IncognitoDisplayMode.REPLACE_SHARED_ACTIONS_TOGGLE -> incognitoIcon
                    IncognitoDisplayMode.DISPLAY_BEHIND_KEYBOARD -> arrowIcon
                }
            } else {
                arrowIcon
            }
            SnyggIcon(
                modifier = Modifier.rotate(if (incognitoDisplayMode.value == IncognitoDisplayMode.DISPLAY_BEHIND_KEYBOARD) rotation else 0f),
                imageVector = icon,
            )
        }
    }

    @Composable
    fun RowScope.CenterContent() {
        val expanded = sharedActionsExpanded && smartbarLayout == SmartbarLayout.SUGGESTIONS_ACTIONS_SHARED
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            val enterTransition = if (shouldAnimate) HorizontalEnterTransition else NoEnterTransition
            val exitTransition = if (shouldAnimate) HorizontalExitTransition else NoExitTransition
            this@CenterContent.AnimatedVisibility(
                visible = !expanded,
                enter = enterTransition,
                exit = exitTransition,
            ) {
                if (shouldShowInlineSuggestionsUi) {
                    InlineSuggestionsUi(inlineSuggestions)
                } else {
                    CandidatesRow()
                }
            }
            this@CenterContent.AnimatedVisibility(
                visible = expanded,
                enter = enterTransition,
                exit = exitTransition,
            ) {
                QuickActionsRow(
                    FlorisImeUi.SmartbarSharedActionsRow.elementName,
                    modifier = modifier.fillMaxSize(),
                )
            }
        }
    }

    @Composable
    fun ExtendedActionsToggle() {
        SnyggIconButton(
            FlorisImeUi.SmartbarExtendedActionsToggle.elementName,
            onClick = {
                if (/* was */ extendedActionsExpanded) {
                    keyboardManager.activeState.isActionsOverflowVisible = false
                }
                scope.launch {
                    prefs.smartbar.extendedActionsExpanded.set(!extendedActionsExpanded)
                }
            },
            modifier = Modifier.sizeIn(maxHeight = FlorisImeSizing.smartbarHeight).aspectRatio(1f)
        ) {
            val transition = updateTransition(extendedActionsExpanded, label = "smartbarSecondaryRowToggleBtn")
            val alpha by transition.animateFloat(label = "alpha") { if (it) 1f else 0f }
            val rotation by transition.animateFloat(label = "rotation") { if (it) 180f else 0f }
            // Expanded icon
            SnyggIcon(
                FlorisImeUi.SmartbarExtendedActionsToggle.elementName,
                modifier = Modifier
                    .alpha(alpha)
                    .rotate(rotation),
                imageVector = Icons.Default.UnfoldLess,
            )
            // Not expanded icon
            SnyggIcon(
                FlorisImeUi.SmartbarExtendedActionsToggle.elementName,
                modifier = Modifier
                    .alpha(1f - alpha)
                    .rotate(rotation - 180f),
                imageVector = Icons.Default.UnfoldMore,
            )
        }
    }

    @Composable
    fun StickyAction() {
        val actionArrangement by prefs.smartbar.actionArrangement.collectAsState()
        val evaluator by keyboardManager.activeSmartbarEvaluator.collectAsState()

        val action = when {
            actionArrangement.stickyAction != null -> {
                actionArrangement.stickyAction
            }

            smartbarLayout == SmartbarLayout.SUGGESTIONS_ACTIONS_SHARED && sharedActionsExpanded -> {
                ToggleOverflowPanelAction
            }

            else -> null
        }

        if (action != null) {
            QuickActionButton(
                modifier = Modifier.padding(horizontal = 4.dp),
                action = action,
                evaluator = evaluator,
            )
        } else {
            Spacer(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .aspectRatio(1f),
            )
        }
    }

    SideEffect {
        if (!shouldAnimate) {
            scope.launch {
                prefs.smartbar.sharedActionsExpandWithAnimation.set(true)
            }
        }
    }

    SnyggRow(
        modifier = modifier
            .fillMaxWidth()
            .height(FlorisImeSizing.smartbarHeight),
    ) {
        when (smartbarLayout) {
            SmartbarLayout.SUGGESTIONS_ONLY -> {
                if (shouldShowInlineSuggestionsUi) {
                    InlineSuggestionsUi(inlineSuggestions)
                } else {
                    CandidatesRow()
                }
            }

            SmartbarLayout.ACTIONS_ONLY -> {
                if (shouldShowInlineSuggestionsUi) {
                    InlineSuggestionsUi(inlineSuggestions)
                } else {
                    QuickActionsRow(FlorisImeUi.SmartbarSharedActionsRow.elementName)
                }
            }

            SmartbarLayout.SUGGESTIONS_ACTIONS_SHARED -> {
                if (!flipToggles) {
                    SharedActionsToggle()
                    CenterContent()
                    StickyAction()
                } else {
                    StickyAction()
                    CenterContent()
                    SharedActionsToggle()
                }
            }

            SmartbarLayout.SUGGESTIONS_ACTIONS_EXTENDED -> {
                if (!flipToggles) {
                    ExtendedActionsToggle()
                    CenterContent()
                    StickyAction()
                } else {
                    StickyAction()
                    CenterContent()
                    ExtendedActionsToggle()
                }
            }

            SmartbarLayout.SPEEKEZ -> {
                SpeekEZSmartbarMainRow(modifier)
            }
        }
    }
}

@Composable
private fun SmartbarSecondaryRow(modifier: Modifier = Modifier) {
    val prefs by FlorisPreferenceStore
    val smartbarLayout by prefs.smartbar.layout.collectAsState()
    val secondaryRowStyle = rememberSnyggThemeQuery(FlorisImeUi.SmartbarExtendedActionsRow.elementName)
    val windowStyle = rememberSnyggThemeQuery(FlorisImeUi.Window.elementName)
    val extendedActionsExpanded by prefs.smartbar.extendedActionsExpanded.collectAsState()
    val extendedActionsPlacement by prefs.smartbar.extendedActionsPlacement.collectAsState()
    val background = secondaryRowStyle.background().let { color ->
        if (extendedActionsPlacement == ExtendedActionsPlacement.OVERLAY_APP_UI) {
            if (color.isUnspecified || color.alpha == 0f) {
                windowStyle.background(default = Color.Black)
            } else {
                color
            }
        } else {
            color
        }
    }

    AnimatedVisibility(
        visible = smartbarLayout == SmartbarLayout.SUGGESTIONS_ACTIONS_EXTENDED && extendedActionsExpanded,
        enter = VerticalEnterTransition,
        exit = VerticalExitTransition,
    ) {
        QuickActionsRow(
            FlorisImeUi.SmartbarExtendedActionsRow.elementName,
            modifier = modifier
                .fillMaxWidth()
                .height(FlorisImeSizing.smartbarHeight)
                .background(background),
        )
    }
}
