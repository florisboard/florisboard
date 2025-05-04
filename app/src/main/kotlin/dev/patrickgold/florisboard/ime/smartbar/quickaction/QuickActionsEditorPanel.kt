/*
 * Copyright (C) 2022-2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.smartbar.quickaction

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.florisboard.lib.toIntOffset
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggButton
import org.florisboard.lib.snygg.ui.SnyggColumn
import org.florisboard.lib.snygg.ui.SnyggIcon
import org.florisboard.lib.snygg.ui.SnyggIconButton
import org.florisboard.lib.snygg.ui.SnyggRow
import org.florisboard.lib.snygg.ui.SnyggText

private const val ItemNotFound = -1
private val NoopAction = QuickAction.InsertKey(TextKeyData(code = KeyCode.NOOP))
private val DragMarkerAction = QuickAction.InsertKey(TextKeyData(code = KeyCode.DRAG_MARKER))

@Composable
fun QuickActionsEditorPanel() {
    val prefs by florisPreferenceModel()
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()

    // We get the current arrangement once and do not observe on purpose
    val actionArrangement = remember { prefs.smartbar.actionArrangement.get() }
    var stickyAction by remember(actionArrangement) {
        mutableStateOf(actionArrangement.stickyAction ?: NoopAction)
    }
    val dynamicActions = remember(actionArrangement) {
        actionArrangement.dynamicActions.ifEmpty { listOf(NoopAction) }.toMutableStateList()
    }
    val hiddenActions = remember(actionArrangement) {
        actionArrangement.hiddenActions.ifEmpty { listOf(NoopAction) }.toMutableStateList()
    }

    val evaluator by keyboardManager.activeSmartbarEvaluator.collectAsState()
    val gridState = rememberLazyGridState()
    var activeDragAction by remember { mutableStateOf<QuickAction?>(null) }
    var activeDragPosition by remember { mutableStateOf(IntOffset.Zero) }
    var activeDragSize by remember { mutableStateOf(IntSize.Zero) }

    fun findItemForOffsetOrClosestInRow(offset: IntOffset): LazyGridItemInfo? {
        var closestItemInRow: LazyGridItemInfo? = null
        // Using manual for loop with indices instead of firstOrNull() because this method gets
        // called a lot and firstOrNull allocates an iterator for each call
        for (index in gridState.layoutInfo.visibleItemsInfo.indices) {
            val item = gridState.layoutInfo.visibleItemsInfo[index]
            if (offset.y in item.offset.y..(item.offset.y + item.size.height)) {
                if (offset.x in item.offset.x..(item.offset.x + item.size.width)) {
                    return item
                }
                closestItemInRow = item
            }
        }
        return closestItemInRow
    }

    fun indexOfStickyAction(item: LazyGridItemInfo): Int {
        val i = item.index
        return if (i == 1) 0 else ItemNotFound
    }

    fun indexOfDynamicAction(item: LazyGridItemInfo): Int {
        val i = item.index
        val base = 3
        return if (i >= base && i < (dynamicActions.size + base)) i - base else ItemNotFound
    }

    fun indexOfHiddenAction(item: LazyGridItemInfo): Int {
        val i = item.index
        val base = dynamicActions.size + 4
        return if (i >= base && i < (hiddenActions.size + base)) i - base else ItemNotFound
    }

    fun keyOf(action: QuickAction): Any? {
        return if (action.keyData().code == KeyCode.NOOP) {
            null
        } else {
            action.hashCode()
        }
    }

    fun removeAllMarkers() {
        if (stickyAction == DragMarkerAction) {
            stickyAction = NoopAction
        }
        dynamicActions.remove(DragMarkerAction)
        if (dynamicActions.isEmpty()) {
            dynamicActions.add(NoopAction)
        }
        hiddenActions.remove(DragMarkerAction)
        if (hiddenActions.isEmpty()) {
            hiddenActions.add(NoopAction)
        }
    }

    fun beginDragGesture(pos: IntOffset) {
        val item = findItemForOffsetOrClosestInRow(pos) ?: return
        val stickyActionIndex = indexOfStickyAction(item)
        val dynamicActionIndex = indexOfDynamicAction(item)
        val hiddenActionIndex = indexOfHiddenAction(item)
        if (stickyActionIndex != ItemNotFound && stickyAction != NoopAction) {
            activeDragAction = stickyAction
            stickyAction = DragMarkerAction
        } else if (dynamicActionIndex != ItemNotFound && dynamicActions[dynamicActionIndex] != NoopAction) {
            activeDragAction = dynamicActions[dynamicActionIndex]
            dynamicActions[dynamicActionIndex] = DragMarkerAction
        } else if (hiddenActionIndex != ItemNotFound && hiddenActions[hiddenActionIndex] != NoopAction) {
            activeDragAction = hiddenActions[hiddenActionIndex]
            hiddenActions[hiddenActionIndex] = DragMarkerAction
        } else {
            return
        }
        activeDragPosition = pos
        activeDragSize = item.size
    }

    fun handleDragGestureChange(posChange: IntOffset) {
        if (activeDragAction == null) return
        val pos = activeDragPosition + posChange
        activeDragPosition = pos
        val item = findItemForOffsetOrClosestInRow(pos) ?: return
        val stickyActionIndex = indexOfStickyAction(item)
        val dynamicActionIndex = indexOfDynamicAction(item)
        val hiddenActionIndex = indexOfHiddenAction(item)
        if (stickyActionIndex != ItemNotFound && stickyAction != DragMarkerAction) {
            if (stickyAction != NoopAction) {
                dynamicActions.add(0, stickyAction)
            }
            removeAllMarkers()
            stickyAction = DragMarkerAction
        } else if (dynamicActionIndex != ItemNotFound && dynamicActions[dynamicActionIndex] != DragMarkerAction) {
            if (dynamicActions[dynamicActionIndex] == NoopAction) {
                removeAllMarkers()
                dynamicActions[dynamicActionIndex] = DragMarkerAction
            } else {
                removeAllMarkers()
                dynamicActions.add(dynamicActionIndex, DragMarkerAction)
            }
        } else if (hiddenActionIndex != ItemNotFound && hiddenActions[hiddenActionIndex] != DragMarkerAction) {
            if (hiddenActions[hiddenActionIndex] == NoopAction) {
                removeAllMarkers()
                hiddenActions[hiddenActionIndex] = DragMarkerAction
            } else {
                removeAllMarkers()
                hiddenActions.add(hiddenActionIndex, DragMarkerAction)
            }
        }
    }

    fun completeDragGestureAndCleanUp() {
        val action = activeDragAction
        if (action != null) {
            if (stickyAction == DragMarkerAction) {
                stickyAction = action
            } else {
                val i = dynamicActions.indexOf(DragMarkerAction)
                if (i >= 0) {
                    dynamicActions[i] = action
                } else {
                    val j = hiddenActions.indexOf(DragMarkerAction)
                    if (j >= 0) {
                        hiddenActions[j] = action
                    }
                }
            }
        }
        activeDragAction = null
        activeDragPosition = IntOffset.Zero
        activeDragSize = IntSize.Zero
    }

    DisposableEffect(Unit) {
        onDispose {
            completeDragGestureAndCleanUp()
            val newActionArrangement = QuickActionArrangement(
                if (stickyAction != NoopAction && stickyAction != DragMarkerAction) stickyAction else null,
                dynamicActions.filter { it != NoopAction && it != DragMarkerAction },
                hiddenActions.filter { it != NoopAction && it != DragMarkerAction },
            )
            prefs.smartbar.actionArrangement.set(newActionArrangement)
            if (keyboardManager.activeState.isActionsEditorVisible) {
                keyboardManager.activeState.isActionsEditorVisible = false
            }
        }
    }

    SnyggColumn(FlorisImeUi.SmartbarActionsEditor.elementName, modifier = Modifier.safeDrawingPadding()) {
        SnyggRow(
            elementName = FlorisImeUi.SmartbarActionsEditorHeader.elementName,
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Extra box wrapper is needed to enforce size constraint but still allow for Snygg margin to be used
            Box(modifier = Modifier.size(48.dp)) {
                SnyggIconButton(
                    elementName = FlorisImeUi.SmartbarActionsEditorHeaderButton.elementName,
                    modifier = Modifier.fillMaxHeight().aspectRatio(1f),
                    onClick = {
                        keyboardManager.activeState.isActionsEditorVisible = false
                    },
                ) {
                    SnyggIcon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    )
                }
            }
            SnyggText(
                modifier = Modifier.weight(1f),
                text = stringRes(R.string.quick_actions_editor__header),
            )
            Spacer(Modifier.size(48.dp))
        }

        SnyggBox(FlorisImeUi.SmartbarActionsEditorTileGrid.elementName) {
            LazyVerticalGrid(
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { beginDragGesture(it.toIntOffset()) },
                            onDrag = { _, it -> handleDragGestureChange(it.toIntOffset()) },
                            onDragEnd = { completeDragGestureAndCleanUp() },
                            onDragCancel = { completeDragGestureAndCleanUp() },
                        )
                    },
                columns = GridCells.Adaptive(FlorisImeSizing.smartbarHeight * 1.8f),
                state = gridState,
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    val n = if (stickyAction != NoopAction) 1 else 0
                    Subheader(
                        text = stringRes(R.string.quick_actions_editor__subheader_sticky_action, "n" to n),
                    )
                }
                item(key = keyOf(stickyAction)) {
                    QuickActionButton(
                        modifier = Modifier.animateItem(),
                        action = stickyAction,
                        evaluator = evaluator,
                        type = QuickActionBarType.EDITOR_TILE,
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    val n = dynamicActions.count { it != NoopAction }
                    Subheader(
                        text = stringRes(R.string.quick_actions_editor__subheader_dynamic_actions, "n" to n),
                    )
                }
                itemsIndexed(dynamicActions, key = { i, a -> keyOf(a) ?: i }) { _, action ->
                    QuickActionButton(
                        modifier = Modifier.animateItem(),
                        action = action,
                        evaluator = evaluator,
                        type = QuickActionBarType.EDITOR_TILE,
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    val n = hiddenActions.count { it != NoopAction }
                    Subheader(
                        text = stringRes(R.string.quick_actions_editor__subheader_hidden_actions, "n" to n),
                    )
                }
                itemsIndexed(hiddenActions, key = { i, a -> keyOf(a) ?: i }) { _, action ->
                    QuickActionButton(
                        modifier = Modifier.animateItem(),
                        action = action,
                        evaluator = evaluator,
                        type = QuickActionBarType.EDITOR_TILE,
                    )
                }
            }
            if (activeDragAction != null) {
                val size = with(LocalDensity.current) {
                    remember(activeDragSize) { activeDragSize.toSize().toDpSize() }
                }
                QuickActionButton(
                    modifier = Modifier
                        .size(size)
                        .offset { activeDragPosition }
                        .offset(-size.width / 2, -size.height / 2),
                    action = activeDragAction!!,
                    evaluator = evaluator,
                    type = QuickActionBarType.EDITOR_TILE,
                )
            }
        }
    }
}

@Composable
private fun Subheader(
    text: String,
    modifier: Modifier = Modifier,
) {
    SnyggText(
        elementName = FlorisImeUi.SmartbarActionsEditorSubheader.elementName,
        modifier = modifier.fillMaxWidth(),
        text = text,
    )
}
