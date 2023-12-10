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

package dev.patrickgold.florisboard.app.ext

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.lib.compose.FlorisHyperlinkText
import dev.patrickgold.florisboard.lib.compose.FlorisOutlinedButton
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.florisboard.lib.ext.ExtensionMaintainer
import dev.patrickgold.florisboard.lib.io.FlorisRef
import dev.patrickgold.florisboard.nlpManager
import dev.patrickgold.florisboard.plugin.IndexedPlugin
import kotlinx.coroutines.runBlocking

@Composable
fun PluginViewScreen(id: String) {
    val context = LocalContext.current
    val nlpManager by context.nlpManager()

    val plugin = runBlocking {
        nlpManager.plugins.getOrNull(id)
    }
    if (plugin != null) {
        ViewScreen(plugin)
    } else {
        ExtensionNotFoundScreen(id)
    }
}

@Composable
private fun ViewScreen(plugin: IndexedPlugin) = FlorisScreen {
    val packageContext = plugin.packageContext()

    title = plugin.metadata.title.getOrNull(packageContext).toString()

    val navController = LocalNavController.current
    val context = LocalContext.current

    content {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            plugin.metadata.longDescription?.getOrNull(packageContext)?.let { Text(it) }
            Spacer(modifier = Modifier.height(16.dp))
            val maintainers = plugin.metadata.maintainers?.getOrNull(packageContext)
            if (!maintainers.isNullOrEmpty()) {
                ExtensionMetaRowScrollableChips(
                    label = stringRes(R.string.ext__meta__maintainers),
                    showDividerAbove = false,
                ) {
                    for ((n, maintainer) in maintainers.withIndex()) {
                        if (n > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        val extMaintainer = remember(maintainer) {
                            ExtensionMaintainer.from(maintainer) ?: ExtensionMaintainer(maintainer)
                        }
                        ExtensionMaintainerChip(extMaintainer)
                    }
                }
            }
            ExtensionMetaRowSimpleText(label = stringRes(R.string.ext__meta__id)) {
                Text(text = plugin.metadata.id)
            }
            ExtensionMetaRowSimpleText(label = stringRes(R.string.ext__meta__version)) {
                Text(text = plugin.metadata.version)
            }
            val homepage = plugin.metadata.homepage?.getOrNull(packageContext)
            if (!homepage.isNullOrBlank()) {
                ExtensionMetaRowSimpleText(label = stringRes(R.string.ext__meta__homepage)) {
                    FlorisHyperlinkText(
                        text = FlorisRef.fromUrl(homepage).authority,
                        url = homepage,
                    )
                }
            }
            val issueTracker = plugin.metadata.issueTracker?.getOrNull(packageContext)
            if (!issueTracker.isNullOrBlank()) {
                ExtensionMetaRowSimpleText(label = stringRes(R.string.ext__meta__issue_tracker)) {
                    FlorisHyperlinkText(
                        text = FlorisRef.fromUrl(issueTracker).authority,
                        url = issueTracker,
                    )
                }
            }
            val privacyPolicy = plugin.metadata.privacyPolicy?.getOrNull(packageContext)
            if (!privacyPolicy.isNullOrBlank()) {
                ExtensionMetaRowSimpleText(label = stringRes(R.string.ext__meta__privacy_policy)) {
                    FlorisHyperlinkText(
                        text = FlorisRef.fromUrl(privacyPolicy).authority,
                        url = privacyPolicy,
                    )
                }
            }
            val license = plugin.metadata.license?.getOrNull(packageContext)
            if (!license.isNullOrBlank()) {
                ExtensionMetaRowSimpleText(label = stringRes(R.string.ext__meta__license)) {
                    Text(text = license)
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                if (plugin.isExternalPlugin()) {
                    FlorisOutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).also {
                                it.data = Uri.parse("package:${plugin.serviceName.packageName}")
                            }
                            context.startActivity(intent)
                        },
                        icon = painterResource(R.drawable.ic_delete),
                        text = stringRes(R.string.action__uninstall),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colors.error,
                        ),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                FlorisOutlinedButton(
                    onClick = {
                        // TODO
                    },
                    icon = painterResource(R.drawable.ic_share),
                    text = stringRes(R.string.action__share),
                    enabled = false,
                )
            }
        }
    }
}
