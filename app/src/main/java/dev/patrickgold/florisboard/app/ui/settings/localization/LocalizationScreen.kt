/*
 * Copyright (C) 2021 Patrick Goldinger
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

package dev.patrickgold.florisboard.app.ui.settings.localization

import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.res.stringRes
import dev.patrickgold.florisboard.app.ui.Routes
import dev.patrickgold.florisboard.app.ui.components.FlorisScreen
import dev.patrickgold.florisboard.app.ui.components.FlorisWarningCard
import dev.patrickgold.florisboard.common.observeAsNonNullState
import dev.patrickgold.florisboard.ime.keyboard.LayoutType
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.subtypeManager
import dev.patrickgold.jetpref.ui.compose.Preference
import dev.patrickgold.jetpref.ui.compose.PreferenceGroup

@Composable
fun LocalizationScreen() = FlorisScreen(
    title = stringRes(R.string.settings__localization__title),
    iconSpaceReserved = false,
    floatingActionButton = {
        val navController = LocalNavController.current
        ExtendedFloatingActionButton(
            icon = { Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = stringRes(R.string.settings__localization__subtype_add_title),
            ) },
            text = { Text(
                text = stringRes(R.string.settings__localization__subtype_add_title),
            ) },
            onClick = { navController.navigate(Routes.Settings.SubtypeAdd) },
        )
    },
) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val subtypeManager by context.subtypeManager()

    PreferenceGroup(title = stringRes(R.string.settings__localization__group_subtypes__label)) {
        val subtypes by subtypeManager.subtypes.observeAsNonNullState()
        if (subtypes.isNullOrEmpty()) {
            FlorisWarningCard(
                modifier = Modifier.padding(all = 8.dp),
                text = stringRes(R.string.settings__localization__subtype_no_subtypes_configured_warning),
            )
        } else {
            val layouts by keyboardManager.resources.layouts.observeAsState()
            for (subtype in subtypes) {
                val layoutMeta = layouts?.get(LayoutType.CHARACTERS)?.get(subtype.layoutMap.characters)
                val summary = layoutMeta?.label ?: stringRes(
                    R.string.settings__localization__subtype_error_layout_not_installed,
                    "layout_id" to subtype.layoutMap.characters,
                )
                Preference(
                    title = subtype.primaryLocale.displayName(subtype.primaryLocale),
                    summary = summary,
                    onClick = { navController.navigate(
                        Routes.Settings.SubtypeEdit(subtype.id)
                    ) },
                )
            }
        }
    }

    PreferenceGroup(title = stringRes(R.string.settings__localization__group_layouts__label)) {

    }
}
