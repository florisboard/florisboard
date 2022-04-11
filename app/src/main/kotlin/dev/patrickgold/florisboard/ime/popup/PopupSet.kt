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

package dev.patrickgold.florisboard.ime.popup

import dev.patrickgold.florisboard.ime.keyboard.AbstractKeyData
import dev.patrickgold.florisboard.ime.keyboard.ComputingEvaluator
import dev.patrickgold.florisboard.ime.text.key.KeyHintConfiguration
import dev.patrickgold.florisboard.ime.text.key.KeyHintMode
import kotlinx.serialization.Serializable

/**
 * A popup set for a single key. This set describes, if the key has a [main] character and other [relevant] popups.
 *
 * Note that a hint character cannot and should not be set in a json extended popup file, rather it
 * should only be dynamically set by the LayoutManager.
 *
 * The order in which these defined popups will be shown depends on the current [KeyHintConfiguration].
 */
@Serializable
open class PopupSet<T : AbstractKeyData>(
    open val main: T? = null,
    open val relevant: List<T> = listOf()
) {
    private val popupKeys: PopupKeys<T> by lazy {
        PopupKeys(null, listOfNotNull(main), relevant)
    }

    open fun getPopupKeys(keyHintConfiguration: KeyHintConfiguration): PopupKeys<T> {
        return popupKeys
    }
}

@Suppress("UNCHECKED_CAST")
class MutablePopupSet<T : AbstractKeyData>(
    override var main: T? = null,
    override val relevant: ArrayList<T> = arrayListOf(),
    var symbolHint: T? = null,
    var numberHint: T? = null,
    private val symbolPopups: ArrayList<T> = arrayListOf(),
    private val numberPopups: ArrayList<T> = arrayListOf(),
    private val configCache: MutableMap<KeyHintConfiguration, PopupKeys<T>> = mutableMapOf()
) : PopupSet<T>(main, relevant) {

    fun clear() {
        symbolHint = null
        numberHint = null
        main = null
        relevant.clear()
        symbolPopups.clear()
        numberPopups.clear()
        configCache.clear()
    }

    override fun getPopupKeys(keyHintConfiguration: KeyHintConfiguration): PopupKeys<T> {
        return configCache.getOrPut(keyHintConfiguration) {
            initPopupList(keyHintConfiguration)
        }
    }

    private fun initPopupList(keyHintConfiguration: KeyHintConfiguration): PopupKeys<T> {
        val localMain = main
        val localRelevant = relevant
        val localSymbolHint = symbolHint
        val localNumberHint = numberHint
        if (localSymbolHint != null && keyHintConfiguration.symbolHintMode != KeyHintMode.DISABLED) {
            if (localNumberHint != null && keyHintConfiguration.numberHintMode != KeyHintMode.DISABLED) {
                val hintPopups = if (keyHintConfiguration.mergeHintPopups) { symbolPopups + numberPopups } else { listOf() }
                return when (keyHintConfiguration.symbolHintMode) {
                    KeyHintMode.ACCENT_PRIORITY -> when (keyHintConfiguration.numberHintMode) {
                        // when both hints are present in accent priority, always have a non-hint key first if possible
                        KeyHintMode.ACCENT_PRIORITY -> when {
                            localMain != null -> PopupKeys(localSymbolHint, listOf(localMain, localSymbolHint, localNumberHint), localRelevant + hintPopups)
                            localRelevant.isNotEmpty() -> PopupKeys(localSymbolHint, listOf(localRelevant[0], localSymbolHint, localNumberHint), localRelevant.subList(1, localRelevant.size) + hintPopups)
                            else -> PopupKeys(localSymbolHint, listOf(localSymbolHint, localNumberHint), hintPopups)
                        }
                        // hint priority of number hint wins and overrules accent priority of symbol hint
                        KeyHintMode.HINT_PRIORITY -> PopupKeys(localSymbolHint, listOfNotNull(localNumberHint, localMain, localSymbolHint), localRelevant + hintPopups)
                        // due to smart priority of number hint, main wins if it exists, otherwise number hint overrules accent priority of symbol hint
                        else -> PopupKeys(localSymbolHint, listOfNotNull(localMain, localNumberHint, localSymbolHint), localRelevant + hintPopups)
                    }
                    KeyHintMode.HINT_PRIORITY -> when (keyHintConfiguration.symbolHintMode) {
                        // when both hints are present in hint priority, symbol hint wins
                        KeyHintMode.HINT_PRIORITY -> PopupKeys(localSymbolHint, listOfNotNull(localSymbolHint, localNumberHint, localMain), localRelevant + hintPopups)
                        // hint priority of symbol hint wins, and overrules potential accent priority of number hint
                        else -> PopupKeys(localSymbolHint, listOfNotNull(localSymbolHint, localMain, localNumberHint), localRelevant + hintPopups)
                    }
                    else -> when (keyHintConfiguration.numberHintMode) {
                        // smart priority of symbol hint wins, and overrules accent priority of number hint
                        KeyHintMode.ACCENT_PRIORITY -> PopupKeys(localSymbolHint, listOfNotNull(localMain, localSymbolHint, localNumberHint), localRelevant + hintPopups)
                        // hint priority of number hint wins
                        KeyHintMode.HINT_PRIORITY -> PopupKeys(localSymbolHint, listOfNotNull(localNumberHint, localMain, localSymbolHint), localRelevant + hintPopups)
                        // when both hints are in smart priority, always have main first if possible
                        else -> PopupKeys(localSymbolHint, listOfNotNull(localMain, localSymbolHint, localNumberHint), localRelevant + hintPopups)
                    }
                }
            } else {
                val hintPopups = if (keyHintConfiguration.mergeHintPopups) { symbolPopups } else { listOf() }
                return when (keyHintConfiguration.symbolHintMode) {
                    // in accent priority, always show a non-hint key first if possible
                    KeyHintMode.ACCENT_PRIORITY -> when {
                        localMain != null -> PopupKeys(localSymbolHint, listOf(localMain, localSymbolHint), localRelevant + hintPopups)
                        localRelevant.isNotEmpty() -> PopupKeys(localSymbolHint, listOf(localRelevant[0], localSymbolHint), localRelevant.subList(1, localRelevant.size) + hintPopups)
                        else -> PopupKeys(localSymbolHint, listOf(localSymbolHint), hintPopups)
                    }
                    // in hint priority, always show hint first
                    KeyHintMode.HINT_PRIORITY -> PopupKeys(localSymbolHint, listOfNotNull(localSymbolHint, localMain), localRelevant + hintPopups)
                    // in smart priority, show main first if possible
                    else -> PopupKeys(localSymbolHint, listOfNotNull(localMain, localSymbolHint), localRelevant + hintPopups)
                }
            }
        } else if (localNumberHint != null && keyHintConfiguration.numberHintMode != KeyHintMode.DISABLED) {
            val hintPopups = if (keyHintConfiguration.mergeHintPopups) { numberPopups } else { listOf() }
            return when (keyHintConfiguration.numberHintMode) {
                // in accent priority, always show a non-hint key first if possible
                KeyHintMode.ACCENT_PRIORITY -> when {
                    localMain != null -> PopupKeys(localNumberHint, listOf(localMain, localNumberHint), localRelevant + hintPopups)
                    localRelevant.isNotEmpty() -> PopupKeys(localNumberHint, listOf(localRelevant[0], localNumberHint), localRelevant.subList(1, localRelevant.size) + hintPopups)
                    else -> PopupKeys(localNumberHint, listOf(localNumberHint), hintPopups)
                }
                // in hint priority, always show hint first
                KeyHintMode.HINT_PRIORITY -> PopupKeys(localNumberHint, listOfNotNull(localNumberHint, localMain), localRelevant + hintPopups)
                // in smart priority, show main first if possible
                else -> PopupKeys(localNumberHint, listOfNotNull(localMain, localNumberHint), localRelevant + hintPopups)
            }
        } else {
            // if no hints shall be shown, use main first if possible
            return PopupKeys(null, listOfNotNull(localMain), localRelevant)
        }
    }

    fun merge(other: PopupSet<AbstractKeyData>, evaluator: ComputingEvaluator) {
        mergeInternal(other, evaluator, relevant, true)
    }

    fun mergeSymbolHint(hintPopups: PopupSet<AbstractKeyData>, evaluator: ComputingEvaluator) {
        mergeInternal(hintPopups, evaluator, symbolPopups)
    }

    fun mergeNumberHint(hintPopups: PopupSet<AbstractKeyData>, evaluator: ComputingEvaluator) {
        mergeInternal(hintPopups, evaluator, numberPopups)
    }

    private fun mergeInternal(other: PopupSet<AbstractKeyData>, evaluator: ComputingEvaluator, targetList: MutableList<T>, useMain: Boolean = false) {
        other.relevant.forEach {
            val data = it.compute(evaluator) as? T
            if (data != null) {
                targetList.add(data)
            }
        }
        other.main?.let {
            val data = it.compute(evaluator) as? T
            if (data != null) {
                if (useMain && main == null) {
                    main = data
                } else {
                    targetList.add(data)
                }
            }
        }
    }
}

/**
 * A fully configured collection of popup keys. It contains a list of keys to be prioritized
 * during rendering (ordered by relevance descending) by showing those keys close to the
 * popup spawning point.
 *
 * The keys contain a separate [hint] key to ease rendering the hint label, but the hint, if
 * present, also occurs in the [prioritized] list.
 *
 * The popup keys can be accessed like an array with the addition that negative indexes defined
 * within this companion object are allowed (as long as the corresponding [prioritized] list
 * contains the corresponding amount of keys.
 */
class PopupKeys<T>(
    val hint: T?,
    val prioritized: List<T>,
    val other: List<T>
) : Collection<T> {
    companion object {
        const val FIRST_PRIORITIZED = -1
        const val SECOND_PRIORITIZED = -2
        const val THIRD_PRIORITIZED = -3
    }

    override val size: Int
        get() = prioritized.size + other.size

    override fun contains(element: T): Boolean {
        return prioritized.contains(element) || other.contains(element)
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return (prioritized + other).containsAll(elements)
    }

    override fun isEmpty(): Boolean {
        return prioritized.isEmpty() && other.isEmpty()
    }

    override fun iterator(): Iterator<T> {
        return (prioritized + other).listIterator()
    }

    fun getOrNull(index: Int): T? {
        if (index >= other.size || index < -prioritized.size) {
            return null
        }
        return when (index) {
            FIRST_PRIORITIZED -> prioritized[0]
            SECOND_PRIORITIZED -> prioritized[1]
            THIRD_PRIORITIZED -> prioritized[2]
            else -> other.getOrNull(index)
        }
    }

    operator fun get(index: Int): T {
        val item = getOrNull(index)
        if (item == null) {
            throw IndexOutOfBoundsException(
                "Specified index $index is not an valid entry in this PopupKeys!"
            )
        } else {
            return item
        }
    }
}
