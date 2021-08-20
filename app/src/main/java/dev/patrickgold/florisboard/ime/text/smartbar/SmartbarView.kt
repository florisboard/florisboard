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
import android.widget.inline.InlineContentView
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
import dev.patrickgold.florisboard.ime.keyboard.KeyboardState
import dev.patrickgold.florisboard.ime.keyboard.updateKeyboardState
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
import java.util.*
import kotlin.math.roundToInt

/**
 * View class which manages the state and the UI of the Smartbar, a key element in the usefulness
 * of FlorisBoard. The view automatically tries to get the current FlorisBoard instance, which it
 * needs to decide when a specific feature component is shown.
 */
class SmartbarView : ConstraintLayout, KeyboardState.OnUpdateStateListener, ThemeManager.OnThemeUpdatedListener {
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

    private val cachedState: KeyboardState = KeyboardState.new(
        maskOfInterest = KeyboardState.INTEREST_TEXT
            or KeyboardState.F_IS_QUICK_ACTIONS_VISIBLE
            or KeyboardState.F_IS_SHOWING_INLINE_SUGGESTIONS
    )

    private lateinit var binding: SmartbarBinding
    private var indexedActionStartArea: MutableList<Int> = mutableListOf()
    private var indexedMainArea: MutableList<Int> = mutableListOf()
    private var indexedActionEndArea: MutableList<Int> = mutableListOf()

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = SmartbarBinding.bind(this)
    }

    /**
     * Called by Android when this view has been attached to a window. At this point we can be
     * certain that all children have been instantiated and that we can begin working with them.
     * After initializing all child views, this method registers the SmartbarView in the
     * TextInputManager, which then starts working together with this view.
     */
    override fun onAttachedToWindow() {
        Timber.i("onAttachedToWindow()")

        super.onAttachedToWindow()

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
            eventListener.get()?.onSmartbarQuickActionPressed(it.id)
        }

        configureFeatureVisibility()

        themeManager.registerOnThemeUpdatedListener(this)
    }

    override fun onDetachedFromWindow() {
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

        cachedActionStartAreaVisible = actionStartAreaVisible
        cachedActionStartAreaId = actionStartAreaId
        cachedMainAreaId = mainAreaId
        cachedActionEndAreaVisible = actionEndAreaVisible
        cachedActionEndAreaId = actionEndAreaId
    }

    override fun onInterceptUpdateKeyboardState(newState: KeyboardState): Boolean {
        return true // SmartbarView is manually managing the dispatching of new states
    }

    override fun onUpdateKeyboardState(newState: KeyboardState) {
        flogInfo(LogTopic.SMARTBAR)
        if (cachedState.isDifferentTo(newState)) {
            cachedState.reset(newState)
            updateUi()
            when (cachedMainAreaId) {
                R.id.clipboard_cursor_row -> binding.clipboardCursorRow.updateKeyboardState(newState)
                R.id.number_row -> binding.numberRow.updateKeyboardState(newState)
            }
        }
    }

    private fun updateUi() {
        binding.quickActionToggle.rotation = if (cachedState.isQuickActionsVisible) 180.0f else 0.0f
        when (florisboard) {
            null -> configureFeatureVisibility(
                actionStartAreaVisible = false,
                actionStartAreaId = null,
                mainAreaId = null,
                actionEndAreaVisible = false,
                actionEndAreaId = null
            )
            else -> configureFeatureVisibility(
                actionStartAreaVisible = when (cachedState.keyVariation) {
                    KeyVariation.PASSWORD -> false
                    else -> true
                },
                actionStartAreaId = when (cachedState.keyboardMode) {
                    KeyboardMode.EDITING -> R.id.back_button
                    else -> R.id.quick_action_toggle
                },
                mainAreaId = when {
                    cachedState.isQuickActionsVisible -> R.id.quick_actions
                    cachedState.isShowingInlineSuggestions -> R.id.inline_suggestions
                    cachedState.keyVariation == KeyVariation.PASSWORD -> {
                        if (!prefs.keyboard.numberRow) R.id.number_row else null
                    }
                    else -> when (cachedState.keyboardMode) {
                        KeyboardMode.EDITING -> null
                        KeyboardMode.NUMERIC,
                        KeyboardMode.PHONE,
                        KeyboardMode.PHONE2 -> R.id.clipboard_cursor_row
                        else -> when {
                            cachedState.isComposingEnabled && cachedState.isCursorMode
                                && cachedState.isRichInputEditor -> R.id.candidates
                            else -> R.id.clipboard_cursor_row
                        }
                    }
                },
                actionEndAreaVisible = when (cachedState.keyVariation) {
                    KeyVariation.PASSWORD -> false
                    else -> true
                },
                actionEndAreaId = when {
                    cachedState.isPrivateMode -> R.id.private_mode_button
                    else -> null
                }
            )
        }
    }

    fun sync() {
        binding.numberRow.sync()
        binding.clipboardCursorRow.sync()
        binding.candidates.updateDisplaySettings(prefs.suggestion.displayMode, prefs.suggestion.clipboardContentTimeout * 1_000)
    }

    fun onPrimaryClipChanged() {
        if (prefs.suggestion.enabled && prefs.suggestion.clipboardContentEnabled && !cachedState.isPrivateMode) {
            florisboard?.florisClipboardManager?.primaryClip?.let { binding.candidates.updateClipboardItem(it) }
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

    /**
     * Clears the inline suggestions and triggers thw Smartbar update cycle.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun clearInlineSuggestions() {
        updateInlineSuggestionStrip(listOf())
    }

    /**
     * Inflates the given inline suggestions. Once all provided views are ready, the suggestions
     * strip is updated and the Smartbar update cycle is triggered.
     *
     * @param inlineSuggestions A collection of inline suggestions to be inflated and shown.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun showInlineSuggestions(inlineSuggestions: Collection<InlineSuggestion>) {
        if (inlineSuggestions.isEmpty()) {
            updateInlineSuggestionStrip(listOf())
        } else {
            val suggestionMap: TreeMap<Int, InlineContentView?> = TreeMap()
            for ((i, inlineSuggestion) in inlineSuggestions.withIndex()) {
                val size = Size(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                try {
                    inlineSuggestion.inflate(context, size, context.mainExecutor) { suggestionView ->
                        flogDebug { "New inline suggestion view ready" }
                        suggestionMap[i] = suggestionView
                        if (suggestionMap.size >= inlineSuggestions.size) {
                            updateInlineSuggestionStrip(suggestionMap.values)
                        }
                    }
                } catch (e: Throwable) {
                    flogWarning { "Failed to inflate inline suggestion: $e" }
                }
            }
        }
    }

    /**
     * Updates the suggestion strip with given inline content views and triggers the Smartbar
     * update cycle.
     *
     * @param suggestionViews A collection of inline content views to show.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun updateInlineSuggestionStrip(suggestionViews: Collection<InlineContentView?>) {
        flogDebug { "Updating the inline suggestion strip with ${suggestionViews.size} items" }
        binding.inlineSuggestionsStrip.removeAllViews()
        val florisboard = florisboard ?: return
        if (suggestionViews.isEmpty()) {
            florisboard.activeState.isShowingInlineSuggestions = false
            return
        } else {
            for (suggestionView in suggestionViews) {
                if (suggestionView == null) {
                    continue
                }
                binding.inlineSuggestionsStrip.addView(suggestionView)
            }
            florisboard.activeState.isShowingInlineSuggestions = true
        }
        updateKeyboardState(florisboard.activeState)
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
                (florisboard?.uiBinding?.inputView?.desiredSmartbarHeight ?: resources.getDimension(R.dimen.smartbar_baseHeight)).coerceAtMost(heightSize)
            }
            else -> {
                // Be whatever you want
                florisboard?.uiBinding?.inputView?.desiredSmartbarHeight ?: resources.getDimension(R.dimen.smartbar_baseHeight)
            }
        }

        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height.roundToInt(), MeasureSpec.EXACTLY))
    }

    override fun onThemeUpdated(theme: Theme) {
        setBackgroundColor(theme.getAttr(Theme.Attr.SMARTBAR_BACKGROUND).toSolidColor().color)
        invalidate()
    }

    fun setEventListener(listener: EventListener?) {
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
