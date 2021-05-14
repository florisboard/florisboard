/*
 * Copyright (C) 2020 Patrick Goldinger
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

package dev.patrickgold.florisboard.ime.text.smartbar

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InlineSuggestion
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.databinding.SmartbarBinding
import dev.patrickgold.florisboard.debug.*
import dev.patrickgold.florisboard.ime.clip.provider.ClipboardItem
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.core.Preferences
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.nlp.SuggestionList
import dev.patrickgold.florisboard.ime.text.key.KeyVariation
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardMode
import dev.patrickgold.florisboard.ime.theme.Theme
import dev.patrickgold.florisboard.ime.theme.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.lang.ref.WeakReference
import kotlin.math.roundToInt

/**
 * View class which manages the state and the UI of the Smartbar, a key element in the usefulness
 * of FlorisBoard. The view automatically tries to get the current FlorisBoard instance, which it
 * needs to decide when a specific feature component is shown.
 */
class SmartbarView : ConstraintLayout, ThemeManager.OnThemeUpdatedListener {
    private val florisboard = FlorisBoard.getInstanceOrNull()
    private val prefs get() = Preferences.default()
    private val themeManager = ThemeManager.default()
    private var eventListener: WeakReference<EventListener?> = WeakReference(null)
    private val mainScope = MainScope()
    private var lastSuggestionInitDate: Long = 0

    private var cachedActionStartAreaVisible: Boolean = false
    @IdRes private var cachedActionStartAreaId: Int? = null
    @IdRes private var cachedMainAreaId: Int? = null
    private var cachedActionEndAreaVisible: Boolean = false
    @IdRes private var cachedActionEndAreaId: Int? = null

    var isQuickActionsVisible: Boolean = false
        set(v) {
            binding.quickActionToggle.rotation = if (v) 180.0f else 0.0f
            field = v
        }
    private var isShowingInlineSuggestions: Boolean = false

    private lateinit var binding: SmartbarBinding
    private var indexedActionStartArea: MutableList<Int> = mutableListOf()
    private var indexedMainArea: MutableList<Int> = mutableListOf()
    private var indexedActionEndArea: MutableList<Int> = mutableListOf()

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    /**
     * Called by Android when this view has been attached to a window. At this point we can be
     * certain that all children have been instantiated and that we can begin working with them.
     * After initializing all child views, this method registers the SmartbarView in the
     * TextInputManager, which then starts working together with this view.
     */
    override fun onAttachedToWindow() {
        Timber.i("onAttachedToWindow()")

        super.onAttachedToWindow()

        binding = SmartbarBinding.bind(this)

        for (view in binding.actionStartArea.children) {
            indexedActionStartArea.add(view.id)
        }
        for (view in binding.mainArea.children) {
            indexedMainArea.add(view.id)
        }
        for (view in binding.actionEndArea.children) {
            indexedActionEndArea.add(view.id)
        }

        binding.backButton.setOnClickListener { eventListener.get()?.onSmartbarBackButtonPressed() }

        binding.candidates.updateDisplaySettings(prefs.suggestion.displayMode, prefs.suggestion.clipboardContentTimeout * 1_000)

        mainScope.launch(Dispatchers.Default) {
            florisboard?.let {
                val layout = florisboard.textInputManager.layoutManager.computeKeyboardAsync(
                    KeyboardMode.SMARTBAR_CLIPBOARD_CURSOR_ROW,
                    Subtype.DEFAULT
                ).await()
                withContext(Dispatchers.Main) {
                    binding.clipboardCursorRow.setComputingEvaluator(florisboard.textInputManager.evaluator)
                    binding.clipboardCursorRow.setIconSet(florisboard.textInputManager.textKeyboardIconSet)
                    binding.clipboardCursorRow.setComputedKeyboard(layout)
                }
            }
        }

        mainScope.launch(Dispatchers.Default) {
            florisboard?.let {
                val layout = florisboard.textInputManager.layoutManager.computeKeyboardAsync(
                    KeyboardMode.SMARTBAR_NUMBER_ROW,
                    Subtype.DEFAULT
                ).await()
                withContext(Dispatchers.Main) {
                    binding.numberRow.setComputingEvaluator(florisboard.textInputManager.evaluator)
                    binding.numberRow.setIconSet(florisboard.textInputManager.textKeyboardIconSet)
                    binding.numberRow.setComputedKeyboard(layout)
                }
            }
        }

        binding.privateModeButton.setOnClickListener {
            eventListener.get()?.onSmartbarPrivateModeButtonClicked()
        }

        for (quickAction in binding.quickActions.children) {
            if (quickAction is SmartbarQuickActionButton) {
                quickAction.id.let { quickActionId ->
                    quickAction.setOnClickListener { eventListener.get()?.onSmartbarQuickActionPressed(quickActionId) }
                }
            }
        }

        binding.quickActionToggle.setOnClickListener {
            isQuickActionsVisible = !isQuickActionsVisible
            updateSmartbarState()
        }

        configureFeatureVisibility(
            actionStartAreaVisible = false,
            actionStartAreaId = null,
            mainAreaId = null,
            actionEndAreaVisible = false,
            actionEndAreaId = null
        )

        themeManager.registerOnThemeUpdatedListener(this)

        florisboard?.textInputManager?.registerSmartbarView(this)
    }

    override fun onDetachedFromWindow() {
        florisboard?.textInputManager?.unregisterSmartbarView(this)
        themeManager.unregisterOnThemeUpdatedListener(this)
        super.onDetachedFromWindow()
    }

    /**
     * Updates the visibility of features based on the provided attributes.
     *
     * @param actionStartAreaVisible True if the action start area should be shown, else false.
     * @param actionStartAreaId The ID of the element to show within the action start area. Set to
     *  null to leave this area blank.
     * @param mainAreaId The ID of the element to show within the main area. Set to null to leave
     *  this area blank.
     * @param actionEndAreaVisible True if the action end area should be shown, else false.
     * @param actionEndAreaId The ID of the element to show within the action end area. Set to null
     *  to leave this area blank.
     */
    private fun configureFeatureVisibility(
        actionStartAreaVisible: Boolean = cachedActionStartAreaVisible,
        @IdRes actionStartAreaId: Int? = cachedActionStartAreaId,
        @IdRes mainAreaId: Int? = cachedMainAreaId,
        actionEndAreaVisible: Boolean = cachedActionEndAreaVisible,
        @IdRes actionEndAreaId: Int? = cachedActionEndAreaId
    ) {
        binding.actionStartArea.visibility = when {
            actionStartAreaVisible && actionStartAreaId != null -> View.VISIBLE
            actionStartAreaVisible && actionStartAreaId == null -> View.INVISIBLE
            else -> View.GONE
        }
        if (actionStartAreaId != null) {
            binding.actionStartArea.displayedChild =
                indexedActionStartArea.indexOf(actionStartAreaId).coerceAtLeast(0)
        }
        binding.mainArea.visibility = when (mainAreaId) {
            null -> View.INVISIBLE
            else -> View.VISIBLE
        }
        if (mainAreaId != null) {
            binding.mainArea.displayedChild =
                indexedMainArea.indexOf(mainAreaId).coerceAtLeast(0)
        }
        binding.actionEndArea.visibility = when {
            actionEndAreaVisible && actionEndAreaId != null -> View.VISIBLE
            actionEndAreaVisible && actionEndAreaId == null -> View.INVISIBLE
            else -> View.GONE
        }
        if (actionEndAreaId != null) {
            binding.actionEndArea.displayedChild =
                indexedActionEndArea.indexOf(actionEndAreaId).coerceAtLeast(0)
        }
    }

    /**
     * Updates the Smartbar UI state by looking at the current keyboard mode, key variation, active
     * editor instance, etc. Passes the evaluated attributes to [configureFeatureVisibility].
     */
    fun updateSmartbarState() {
        //binding.clipboardCursorRow.updateVisibility()
        binding.candidates.updateDisplaySettings(prefs.suggestion.displayMode, prefs.suggestion.clipboardContentTimeout * 1_000)
        when (florisboard) {
            null -> configureFeatureVisibility(
                actionStartAreaVisible = false,
                actionStartAreaId = null,
                mainAreaId = null,
                actionEndAreaVisible = false,
                actionEndAreaId = null
            )
            else -> configureFeatureVisibility(
                actionStartAreaVisible = when (florisboard.textInputManager.keyVariation) {
                    KeyVariation.PASSWORD -> false
                    else -> true
                },
                actionStartAreaId = when (florisboard.textInputManager.getActiveKeyboardMode()) {
                    KeyboardMode.EDITING -> R.id.back_button
                    else -> R.id.quick_action_toggle
                },
                mainAreaId = if (isShowingInlineSuggestions) {
                    R.id.inline_suggestions
                } else {
                    when (florisboard.textInputManager.keyVariation) {
                        KeyVariation.PASSWORD -> R.id.number_row
                        else -> when (isQuickActionsVisible) {
                            true -> R.id.quick_actions
                            else -> when (florisboard.textInputManager.getActiveKeyboardMode()) {
                                KeyboardMode.EDITING -> null
                                KeyboardMode.NUMERIC,
                                KeyboardMode.PHONE,
                                KeyboardMode.PHONE2 -> R.id.clipboard_cursor_row
                                else -> when {
                                    florisboard.activeEditorInstance.isComposingEnabled &&
                                        florisboard.activeEditorInstance.selection.isCursorMode
                                    -> R.id.candidates
                                    else -> R.id.clipboard_cursor_row
                                }
                            }
                        }
                    }
                },
                actionEndAreaVisible = when (florisboard.textInputManager.keyVariation) {
                    KeyVariation.PASSWORD -> false
                    else -> true
                },
                actionEndAreaId = when {
                    florisboard.activeEditorInstance.isPrivateMode -> R.id.private_mode_button
                    else -> null
                }
            )
        }
        binding.clipboardCursorRow.notifyStateChanged()
        binding.numberRow.notifyStateChanged()
    }

    fun onPrimaryClipChanged() {
        if (prefs.suggestion.enabled && prefs.suggestion.clipboardContentEnabled && florisboard?.activeEditorInstance?.isPrivateMode == false ) {
            florisboard.florisClipboardManager?.primaryClip?.let { binding.candidates.updateClipboardItem(it) }
            updateSmartbarState()
        }
    }

    fun setCandidateSuggestionWords(suggestionInitDate: Long, suggestions: SuggestionList?) {
        if (suggestionInitDate > lastSuggestionInitDate) {
            lastSuggestionInitDate = suggestionInitDate
            binding.candidates.updateCandidates(suggestions)
        }
    }

    fun updateCandidateSuggestionCapsState() {
        //
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun setInlineSuggestions(list: List<InlineSuggestion>?) {
        if (list == null) {
            flogDebug { "Removing inline suggestions" }
            isShowingInlineSuggestions = false
            binding.inlineSuggestionsInner.removeAllViews()
        } else if (list.isNotEmpty()) {
            flogDebug { "Adding inline suggestions (${list.size})" }
            isShowingInlineSuggestions = true
            binding.inlineSuggestionsInner.let {
                for (inlineSuggestion in list) {
                    try {
                        inlineSuggestion.inflate(
                            context,
                            Size(ViewGroup.LayoutParams.WRAP_CONTENT, florisboard?.inputView?.desiredInlineSuggestionsMaxHeight ?: 0),
                            context.mainExecutor
                        ) { view ->
                            if (view != null) {
                                flogDebug { "Adding inline suggestion view" }
                                it.addView(view)
                            } else {
                                flogDebug { "Inline suggestion view is null" }
                            }
                        }
                    } catch (e: Throwable) {
                        flogWarning { "Failed to inflate inline suggestion: $e" }
                    }
                }
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec).toFloat()
        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> {
                // Must be this size
                heightSize
            }
            MeasureSpec.AT_MOST -> {
                // Can't be bigger than...
                (florisboard?.inputView?.desiredSmartbarHeight ?: resources.getDimension(R.dimen.smartbar_baseHeight)).coerceAtMost(heightSize)
            }
            else -> {
                // Be whatever you want
                florisboard?.inputView?.desiredSmartbarHeight ?: resources.getDimension(R.dimen.smartbar_baseHeight)
            }
        }

        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height.roundToInt(), MeasureSpec.EXACTLY))
    }

    override fun onThemeUpdated(theme: Theme) {
        setBackgroundColor(theme.getAttr(Theme.Attr.SMARTBAR_BACKGROUND).toSolidColor().color)
        invalidate()
    }

    fun setEventListener(listener: EventListener) {
        eventListener = WeakReference(listener)
        binding.candidates.setEventListener(listener)
    }

    /**
     * Event listener interface which can be used by other classes to receive updates when something
     * important happens in the Smartbar.
     */
    interface EventListener {
        fun onSmartbarBackButtonPressed() {}
        fun onSmartbarCandidatePressed(word: String) {}
        fun onSmartbarClipboardCandidatePressed(clipboardItem: ClipboardItem) {}
        //fun onSmartbarCandidateLongPressed() {}
        fun onSmartbarPrivateModeButtonClicked() {}
        fun onSmartbarQuickActionPressed(@IdRes quickActionId: Int) {}
    }
}
