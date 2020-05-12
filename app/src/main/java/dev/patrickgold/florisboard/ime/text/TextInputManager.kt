package dev.patrickgold.florisboard.ime.text

import android.content.Context
import android.os.Handler
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Toast
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyData
import dev.patrickgold.florisboard.ime.text.key.KeyType
import dev.patrickgold.florisboard.ime.text.key.KeyVariation
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardMode
import dev.patrickgold.florisboard.ime.text.keyboard.KeyboardView
import dev.patrickgold.florisboard.ime.text.layout.LayoutManager
import dev.patrickgold.florisboard.ime.text.smartbar.SmartbarManager
import java.util.*

/**
 * TextInputManager is responsible for managing everything which is related to text input. All of
 * the following count as text input: character, numeric (+advanced), phone and symbol layouts.
 *
 * All of the UI for the different keyboard layouts are kept under the same container element and
 * are separated from media-related UI.
 *
 * Also within the scope of this class are the suggestions in the Smartbar. These suggestions are
 * only available for text input.
 *
 * All events defined in [FlorisBoard.EventListener] will be passed through to this class by the
 * core.
 *
 * @property florisboard Reference to instance of core class [FlorisBoard].
 */
class TextInputManager(private val florisboard: FlorisBoard) : FlorisBoard.EventListener {

    private var activeKeyboardMode: KeyboardMode? = null
    private var hasCapsRecentlyChanged: Boolean = false
    private val keyboardViews = EnumMap<KeyboardMode, KeyboardView>(
        KeyboardMode::class.java)
    private val osHandler = Handler()
    private var textViewGroup: LinearLayout? = null

    var caps: Boolean = false
    var capsLock: Boolean = false
    var keyVariation: KeyVariation = KeyVariation.NORMAL
    val layoutManager = LayoutManager(florisboard)
    val smartbarManager: SmartbarManager = SmartbarManager(florisboard)

    override fun onCreateInputView() {
        layoutManager.autoFetchAssociationsFromPrefs()

        textViewGroup = florisboard.rootViewGroup?.findViewById(R.id.text_input)
        textViewGroup?.addView(smartbarManager.createSmartbarView())
        for (mode in KeyboardMode.values()) {
            val keyboardView = KeyboardView(florisboard)
            textViewGroup?.addView(keyboardView)
            keyboardView.visibility = View.GONE
            keyboardView.setKeyboardMode(mode)
            keyboardViews[mode] = keyboardView
        }
        florisboard.rootViewGroup?.findViewById<LinearLayout>(R.id.keyboard_preview)?.visibility = View.GONE
        setActiveKeyboardMode(KeyboardMode.CHARACTERS)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        val keyboardMode = when (info) {
            null -> KeyboardMode.CHARACTERS
            else -> when (info.inputType and InputType.TYPE_MASK_CLASS) {
                InputType.TYPE_CLASS_NUMBER -> {
                    keyVariation = KeyVariation.NORMAL
                    KeyboardMode.NUMERIC
                }
                InputType.TYPE_CLASS_PHONE -> {
                    keyVariation = KeyVariation.NORMAL
                    KeyboardMode.PHONE
                }
                InputType.TYPE_CLASS_TEXT -> {
                    keyVariation = when (info.inputType and InputType.TYPE_MASK_VARIATION) {
                        InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                        InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> KeyVariation.EMAIL_ADDRESS
                        InputType.TYPE_TEXT_VARIATION_PASSWORD,
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                        InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> KeyVariation.PASSWORD
                        InputType.TYPE_TEXT_VARIATION_URI -> KeyVariation.URI
                        else -> KeyVariation.NORMAL
                    }
                    KeyboardMode.CHARACTERS
                }
                else -> {
                    keyVariation = KeyVariation.NORMAL
                    KeyboardMode.CHARACTERS
                }
            }
        }
        smartbarManager.onStartInputView(keyboardMode)
        setActiveKeyboardMode(keyboardMode)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        smartbarManager.onFinishInputView()
    }

    override fun onWindowShown() {
        smartbarManager.activeContainerId = smartbarManager.getPreferredContainerId()
    }

    fun show() {
        textViewGroup?.visibility = View.VISIBLE
    }
    fun hide() {
        textViewGroup?.visibility = View.GONE
    }

    fun getActiveKeyboardMode(): KeyboardMode {
        return activeKeyboardMode ?: KeyboardMode.CHARACTERS
    }

    private fun setActiveKeyboardMode(mode: KeyboardMode) {
        keyboardViews[activeKeyboardMode]?.visibility = View.GONE
        keyboardViews[mode]?.visibility = View.VISIBLE
        keyboardViews[mode]?.updateVariation()
        activeKeyboardMode = mode
        smartbarManager.activeContainerId = smartbarManager.getPreferredContainerId()
    }

    private fun handleEnter() {
        val action = florisboard.currentInputEditorInfo.imeOptions
        Log.d("imeOptions", action.toString())
        Log.d("imeOptions action only", (action and 0xFF).toString())
        if (action and EditorInfo.IME_FLAG_NO_ENTER_ACTION > 0) {
            florisboard.currentInputConnection.sendKeyEvent(
                KeyEvent(
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_ENTER
            )
            )
        } else {
            when (action and EditorInfo.IME_MASK_ACTION) {
                EditorInfo.IME_ACTION_DONE,
                EditorInfo.IME_ACTION_GO,
                EditorInfo.IME_ACTION_NEXT,
                EditorInfo.IME_ACTION_PREVIOUS,
                EditorInfo.IME_ACTION_SEARCH,
                EditorInfo.IME_ACTION_SEND -> {
                    florisboard.currentInputConnection.performEditorAction(action)
                }
                else -> {
                    florisboard.currentInputConnection.sendKeyEvent(KeyEvent(
                        KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_ENTER
                    ))
                }
            }
        }
    }

    fun sendKeyPress(keyData: KeyData) {
        val ic = florisboard.currentInputConnection
        if (activeKeyboardMode == KeyboardMode.NUMERIC) {
            when (keyData.type) {
                KeyType.CHARACTER,
                KeyType.NUMERIC-> {
                    val text = keyData.code.toChar().toString()
                    ic.commitText(text, 1)
                }
                else -> when (keyData.code) {
                    KeyCode.DELETE -> {
                        ic.deleteSurroundingText(1, 0)
                    }
                    KeyCode.ENTER -> handleEnter()
                }
            }
        } else if (keyData.type == KeyType.CHARACTER || keyData.type == KeyType.NUMERIC) {
            var text = keyData.code.toChar()
            if (caps) {
                text = text.toUpperCase()
            }
            if (keyVariation == KeyVariation.PASSWORD) {
                ic.commitText(text.toString(), 1)
            } else {
                when (keyData.code) {
                    KeyCode.SPACE -> {
                        ic.finishComposingText()
                        smartbarManager.composingText = ""
                        ic.commitText(text.toString(), 1)
                    }
                    KeyCode.URI_COMPONENT_TLD -> {
                        val tld = when (caps) {
                            true -> keyData.label.toUpperCase(Locale.getDefault())
                            false -> keyData.label.toLowerCase(Locale.getDefault())
                        }
                        smartbarManager.composingText += tld
                        smartbarManager.generateCandidatesFromSuggestions()
                        ic.setComposingText(smartbarManager.composingText, 1)
                    }
                    else -> {
                        smartbarManager.composingText += text.toString()
                        smartbarManager.generateCandidatesFromSuggestions()
                        ic.setComposingText(smartbarManager.composingText, 1)
                    }
                }
            }
            if (!capsLock) {
                caps = false
            }
        } else {
            when (keyData.code) {
                KeyCode.DELETE -> {
                    if (smartbarManager.composingText.isNotBlank() && keyVariation != KeyVariation.PASSWORD) {
                        smartbarManager.composingText = smartbarManager.composingText.dropLast(1)
                        ic.setComposingText(smartbarManager.composingText, 1)
                    } else {
                        ic.deleteSurroundingText(1, 0)
                    }
                }
                KeyCode.ENTER -> handleEnter()
                KeyCode.LANGUAGE_SWITCH -> {
                    /*Toast.makeText(florisboard, "[NYI]: Language switch",
                        Toast.LENGTH_SHORT).show()*/
                    florisboard.setActiveInput(R.id.media_input)
                }
                KeyCode.PHONE_PAUSE,
                KeyCode.PHONE_WAIT -> {
                    val text = keyData.code.toChar().toString()
                    ic.commitText(text, 1)
                }
                KeyCode.SHIFT -> {
                    if (hasCapsRecentlyChanged) {
                        osHandler.removeCallbacksAndMessages(null)
                        caps = true
                        capsLock = true
                        hasCapsRecentlyChanged = false
                    } else {
                        caps = !caps
                        capsLock = false
                        hasCapsRecentlyChanged = true
                        osHandler.postDelayed({
                            hasCapsRecentlyChanged = false
                        }, 300)
                    }
                }
                KeyCode.SHOW_INPUT_METHOD_PICKER -> {
                    val im = florisboard.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    im.showInputMethodPicker()
                }
                KeyCode.VIEW_CHARACTERS -> setActiveKeyboardMode(KeyboardMode.CHARACTERS)
                // TODO: Implement numeric layout
                KeyCode.VIEW_NUMERIC -> setActiveKeyboardMode(KeyboardMode.NUMERIC)
                KeyCode.VIEW_NUMERIC_ADVANCED -> setActiveKeyboardMode(KeyboardMode.NUMERIC_ADVANCED)
                KeyCode.VIEW_PHONE -> setActiveKeyboardMode(KeyboardMode.PHONE)
                KeyCode.VIEW_PHONE2 -> setActiveKeyboardMode(KeyboardMode.PHONE2)
                KeyCode.VIEW_SYMBOLS -> setActiveKeyboardMode(KeyboardMode.SYMBOLS)
                KeyCode.VIEW_SYMBOLS2 -> setActiveKeyboardMode(KeyboardMode.SYMBOLS2)
            }
        }
    }
}
