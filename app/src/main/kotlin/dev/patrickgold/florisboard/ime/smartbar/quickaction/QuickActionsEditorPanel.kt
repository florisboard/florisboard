/*
 * Copyright (C) 2022 Patrick Goldinger
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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.florisPreferenceModel
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.compose.FlorisIconButton
import dev.patrickgold.florisboard.lib.compose.safeTimes
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.florisboard.lib.toIntOffset
import org.florisboard.lib.snygg.SnyggPropertySet
import org.florisboard.lib.snygg.ui.snyggBackground
import org.florisboard.lib.snygg.ui.snyggClip
import org.florisboard.lib.snygg.ui.solidColor
import org.florisboard.lib.snygg.ui.spSize

private const val ItemNotFound = -1
private val NoopAction = QuickAction.InsertKey(TextKeyData(code = KeyCode.NOOP))
private val DragMarkerAction = QuickAction.InsertKey(TextKeyData(code = KeyCode.DRAG_MARKER))

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickActionsEditorPanel(modifier: Modifier = Modifier) {
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

    val panelStyle = FlorisImeTheme.style.get(FlorisImeUi.SmartbarActionsEditor)
    val headerStyle = FlorisImeTheme.style.get(FlorisImeUi.SmartbarActionsEditorHeader)
    val subheaderStyle = FlorisImeTheme.style.get(FlorisImeUi.SmartbarActionsEditorSubheader)

    fun findItemForOffset(offset: IntOffset): LazyGridItemInfo? {
        // Using manual for loop with indices instead of firstOrNull() because this method gets
        // called a lot and firstOrNull allocates an iterator for each call
        for (index in gridState.layoutInfo.visibleItemsInfo.indices) {
            val item = gridState.layoutInfo.visibleItemsInfo[index]
            if (offset.x in item.offset.x..(item.offset.x + item.size.width) &&
                offset.y in item.offset.y..(item.offset.y + item.size.height)) {
                return item
            }
        }
        return null
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
        val item = findItemForOffset(pos) ?: return
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
        val item = findItemForOffset(pos) ?: return
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

    Column(
        modifier = modifier
            .snyggBackground(context, panelStyle, fallbackColor = FlorisImeTheme.fallbackSurfaceColor())
            .snyggClip(panelStyle),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .snyggBackground(context, headerStyle),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlorisIconButton(
                onClick = {
                    keyboardManager.activeState.isActionsEditorVisible = false
                },
                icon = Icons.Default.KeyboardArrowLeft,
                iconColor = headerStyle.foreground.solidColor(context, default = FlorisImeTheme.fallbackContentColor()),
            )
            Text(
                modifier = Modifier.weight(1f),
                text = stringRes(R.string.quick_actions_editor__header),
                color = headerStyle.foreground.solidColor(context, default = FlorisImeTheme.fallbackContentColor()),
                fontSize = headerStyle.fontSize.spSize(),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.size(48.dp))
        }

        Box {
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
                        style = subheaderStyle,
                    )
                }
                item(key = keyOf(stickyAction)) {
                    QuickActionButton(
                        modifier = Modifier.animateItemPlacement(),
                        action = stickyAction,
                        evaluator = evaluator,
                        type = QuickActionBarType.STATIC_TILE,
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    val n = dynamicActions.count { it != NoopAction }
                    Subheader(
                        text = stringRes(R.string.quick_actions_editor__subheader_dynamic_actions, "n" to n),
                        style = subheaderStyle,
                    )
                }
                itemsIndexed(dynamicActions, key = { i, a -> keyOf(a) ?: i }) { _, action ->
                    QuickActionButton(
                        modifier = Modifier.animateItemPlacement(),
                        action = action,
                        evaluator = evaluator,
                        type = QuickActionBarType.STATIC_TILE,
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    val n = hiddenActions.count { it != NoopAction }
                    Subheader(
                        text = stringRes(R.string.quick_actions_editor__subheader_hidden_actions, "n" to n),
                        style = subheaderStyle,
                    )
                }
                itemsIndexed(hiddenActions, key = { i, a -> keyOf(a) ?: i }) { _, action ->
                    QuickActionButton(
                        modifier = Modifier.animateItemPlacement(),
                        action = action,
                        evaluator = evaluator,
                        type = QuickActionBarType.STATIC_TILE,
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
                    type = QuickActionBarType.STATIC_TILE,
                )
            }
        }
    }
}

@Composable
private fun Subheader(
    text: String,
    style: SnyggPropertySet,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Text(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
        text = text,
        color = style.foreground.solidColor(context, default = FlorisImeTheme.fallbackContentColor()),
        fontWeight = FontWeight.Bold,
        fontSize = style.fontSize.spSize() safeTimes 0.8f,
    )
}
