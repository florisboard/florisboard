/*
 * Copyright (C) 2026 The FlorisBoard Contributors
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

@file:OptIn(ExperimentalMaterial3Api::class)

package dev.patrickgold.florisboard.app.setup

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import dev.patrickgold.florisboard.R
import kotlinx.coroutines.launch
import org.florisboard.lib.compose.florisVerticalScroll
import org.florisboard.lib.compose.stringRes

interface SetupNavigator {
    fun next()
    fun back()
}

enum class SetupSecondaryStyle { TEXT, TONAL }

@Stable
data class SetupStep(
    val id: Int,
    val required: Boolean,
    val satisfied: Boolean,
    val icon: ImageVector,
    val shape: RoundedPolygon,
    val title: String,
    val summary: String,
    val description: String? = null,
    val primaryLabel: String,
    val onPrimary: SetupNavigator.() -> Unit,
    val primaryEnabled: Boolean = true,
    val secondaryLabel: String? = null,
    val onSecondary: SetupNavigator.() -> Unit = {},
    val secondaryStyle: SetupSecondaryStyle = SetupSecondaryStyle.TEXT,
    val showProgress: Boolean = true,
    val body: (@Composable ColumnScope.() -> Unit)? = null,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal object SetupShapes {
    val Welcome = MaterialShapes.Cookie9Sided.normalized()
    val Enable = MaterialShapes.Cookie6Sided.normalized()
    val Select = MaterialShapes.Clover4Leaf.normalized()
    val Notifications = MaterialShapes.Pentagon.normalized()
    val Finish = MaterialShapes.SoftBurst.normalized()
}

@Composable
fun SetupFlowLayout(
    steps: List<SetupStep>,
    modifier: Modifier = Modifier,
) {
    if (steps.isEmpty()) return
    val currentSteps by rememberUpdatedState(steps)
    val scope = rememberCoroutineScope()

    val firstOpenIndex = steps.indexOfFirst {
        !it.satisfied
    }.let {if (it < 0) steps.lastIndex else it }

    val pagerState = rememberPagerState(initialPage = firstOpenIndex) { currentSteps.size }

    LaunchedEffect(firstOpenIndex) {
        if (pagerState.currentPage != firstOpenIndex) {
            pagerState.animateScrollToPage(firstOpenIndex)
        }
    }

    val navigator = remember(pagerState) {
        object : SetupNavigator {
            override fun next() {
                val page = pagerState.currentPage
                val step = currentSteps.getOrNull(page) ?: return
                if (step.required && !step.satisfied) return
                if (page < currentSteps.lastIndex) {
                    scope.launch { pagerState.animateScrollToPage(page + 1) }
                }
            }

            override fun back() {
                val page = pagerState.currentPage
                val previous = currentSteps.getOrNull(page - 1) ?: return
                if (previous.required) return
                scope.launch { pagerState.animateScrollToPage(page - 1) }
            }
        }
    }

    val targetPage = pagerState.targetPage.coerceIn(0, steps.lastIndex)
    val targetStep = steps[targetPage]

    val canGoBack = steps.getOrNull(pagerState.currentPage - 1)?.required == false
    BackHandler(enabled = canGoBack) { navigator.back() }

    var infoStepId by rememberSaveable { mutableStateOf<Int?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        SetupTopBar(
            canGoBack = canGoBack,
            onBack = navigator::back,
            progress = if (steps.size > 1) targetPage / steps.lastIndex.toFloat() else 1f,
            progressVisible = targetStep.showProgress,
            step = targetStep,
            onShowInfo = { infoStepId = targetStep.id },
        )

        MorphingStepIcon(
            shape = targetStep.shape,
            icon = targetStep.icon,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 12.dp, bottom = 20.dp)
                .size(156.dp)
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            userScrollEnabled = false,
            beyondViewportPageCount = 0,
            key = { currentSteps.getOrNull(it)?.id ?: it }
        ) { page ->
            val step = steps.getOrNull(page) ?: return@HorizontalPager
            StepPage(step = step, navigator = navigator)
        }
    }

    val infoStep = steps.firstOrNull { it.id == infoStepId }
    if (infoStep?.description != null) {
        val sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(
                SheetValue.Hidden,
                SheetValue.Expanded,
            ),
        )
        LaunchedEffect(Unit) {
            sheetState.expand()
        }
        ModalBottomSheet(
            onDismissRequest = { infoStepId = null },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    modifier = Modifier
                        .semantics { heading() },
                    text = infoStep.title,
                    style = MaterialTheme.typography.headlineMediumEmphasized,
                    textAlign = TextAlign.Center,
                )

                Text(infoStep.description)

                FilledTonalButton(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { infoStepId = null }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = ButtonDefaults.MediumContainerHeight),
                    shapes = ButtonDefaults.shapes(),
                    contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
                ) {
                    Text(stringRes(R.string.setup__info__got_it))
                }
            }
        }
    }

}


@Composable
private fun SetupTopBar(
    canGoBack: Boolean,
    onBack: () -> Unit,
    progress: Float,
    progressVisible: Boolean,
    step: SetupStep,
    onShowInfo: () -> Unit,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.fastCoerceAtLeast(0.06f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "setupProgress",
    )
    val progressAlpha by animateFloatAsState(if (progressVisible) 1f else 0f, label = "setupProgessAlpha")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .alpha(progressAlpha),
        ) {
            if (canGoBack) {
                FilledTonalIconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringRes(R.string.action__back),
                    )
                }
            }
        }
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
                .alpha(progressAlpha),
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .alpha(progressAlpha),
        ) {
            if (step.description != null) {
                FilledTonalIconButton(
                    onClick = onShowInfo,
                    shapes = IconButtonDefaults.shapes(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = stringRes(R.string.setup__step_required_hint__label),
                    )
                }
            }
        }
    }
}

@Composable
private fun MorphingStepIcon(
    shape: RoundedPolygon,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    val containerColor = MaterialTheme.colorScheme.primaryContainer
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer

    var from by remember { mutableStateOf(shape) }
    var to by remember { mutableStateOf(shape) }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(shape) {
        if (shape !== to) {
            from = to
            to = shape
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
        }
    }

    val morph = remember(from, to) { Morph(from, to) }

    Box(
        modifier = modifier
            .clearAndSetSemantics {}
            .drawWithCache {
                val matrix = Matrix().apply { scale(size.width, size.height) }
                onDrawBehind {
                    val morphPath = morph.toPath(progress.value).asComposePath()
                    morphPath.transform(matrix)
                    drawPath(path = morphPath, color = containerColor)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(targetState = icon, label = "setupMorphingStepIcon") { current ->
            Icon(
                imageVector = current,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = contentColor,
            )
        }
    }
}

@Composable
private fun StepPage(
    step: SetupStep,
    navigator: SetupNavigator,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
                text = step.title,
                style = MaterialTheme.typography.headlineMediumEmphasized,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = step.summary,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Left,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .florisVerticalScroll()
                .padding(vertical = 16.dp),
        ) {
            step.body?.invoke(this)
            if (step.required) {
                Card {
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        text = stringRes(R.string.setup__step_required_hint__label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val buttonModifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ButtonDefaults.MediumContainerHeight)
            val buttonPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight)

            Button(
                onClick = { step.onPrimary(navigator) },
                enabled = step.primaryEnabled,
                modifier = buttonModifier,
                shapes = ButtonDefaults.shapes(),
                contentPadding = buttonPadding,
            ) {
                Text(step.primaryLabel)
            }
            if (step.secondaryLabel != null) {
                when (step.secondaryStyle) {
                    SetupSecondaryStyle.TEXT -> TextButton(
                        onClick = { step.onSecondary(navigator) },
                        modifier = buttonModifier,
                        shapes = ButtonDefaults.shapes(),
                        contentPadding = buttonPadding,
                    ) {
                        Text(step.secondaryLabel)
                    }

                    SetupSecondaryStyle.TONAL -> FilledTonalButton(
                        onClick = { step.onSecondary(navigator) },
                        modifier = buttonModifier,
                        shapes = ButtonDefaults.shapes(),
                        contentPadding = buttonPadding,
                    ) {
                        Text(step.secondaryLabel)
                    }
                }
            }
        }
    }
}
