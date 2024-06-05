package dev.patrickgold.florisboard.app.ext

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.extensionManager
import dev.patrickgold.florisboard.lib.android.launchUrl
import dev.patrickgold.florisboard.lib.compose.FlorisOutlinedBox
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.compose.FlorisTextButton
import dev.patrickgold.florisboard.lib.compose.defaultFlorisOutlinedBox
import dev.patrickgold.florisboard.lib.compose.stringRes
import dev.patrickgold.florisboard.lib.ext.generateUpdateUrl
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup
import org.florisboard.lib.kotlin.curlyFormat

@Composable
fun ExtensionHomeScreen() = FlorisScreen {
    title = stringRes(R.string.ext__home__title)
    previewFieldVisible = false

    val context = LocalContext.current
    val navController = LocalNavController.current

    content {
        FlorisOutlinedBox(
            modifier = Modifier.defaultFlorisOutlinedBox(),
        ) {
            Text(
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 4.dp),
                text = "You can download and install extensions from the FlorisBoard Addons Store or import any extension file you have downloaded from the internet.",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
            ) {
                FlorisTextButton(
                    onClick = {
                        //context.launchUrl("https://beta.addons.florisboard.org")
                        context.launchUrl("https://fladdonstest.patrickgold.dev")
                    },
                    icon = Icons.Default.Shop,
                    text = "Visit Addons Store",//stringRes(R.string.action__edit),
                )
                Spacer(modifier = Modifier.weight(1f))
                FlorisTextButton(
                    onClick = {
                        navController.navigate(Routes.Ext.Import(ExtensionImportScreenType.EXT_ANY, null))
                    },
                    icon = Icons.AutoMirrored.Filled.Input,
                    text = stringRes(R.string.action__import),
                )
            }
        }

        FlorisOutlinedBox(
            modifier = Modifier.defaultFlorisOutlinedBox(),
        ) {
            Text(
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 4.dp),
                text = "Since this app does not have Internet permission, updates for installed extensions must be checked manually.",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
            ) {
                val extensionManager by context.extensionManager()
                val extensionIndex = extensionManager.combinedExtensionList()
                FlorisTextButton(
                    onClick = {
                        context.launchUrl(extensionIndex.generateUpdateUrl(version = "v~draft2", host = "https://fladdonstest.patrickgold.dev"))
                    },
                    icon = Icons.Outlined.FileDownload,
                    text = "Search for Updates"
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        PreferenceGroup(title = "Manage installed extensions") {
            Preference(
                icon = Icons.Default.Palette,
                title = stringRes(R.string.ext__list__ext_theme),
                onClick = {
                    navController.navigate(Routes.Ext.List(ExtensionListScreenType.EXT_THEME))
                },
            )
            Preference(
                icon = Icons.Default.Keyboard,
                title = stringRes(R.string.ext__list__ext_keyboard),
                onClick = {
                    navController.navigate(Routes.Ext.List(ExtensionListScreenType.EXT_KEYBOARD))
                },
            )
            Preference(
                icon = Icons.Default.Language,
                title = stringRes(R.string.ext__list__ext_languagepack),
                onClick = {
                    navController.navigate(Routes.Ext.List(ExtensionListScreenType.EXT_LANGUAGEPACK))
                },
            )
        }
    }
}

@Composable
fun AddonManagementReferenceBox() {
    val navController = LocalNavController.current

    FlorisOutlinedBox(
        modifier = Modifier.defaultFlorisOutlinedBox(),
        title = "Managing extensions"
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            text = "All tasks related to importing, exporting, creating, customizing, and removing extensions can be handled through the centralized addon manager.",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
        ) {
            Spacer(modifier = Modifier.weight(1f))
            FlorisTextButton(
                onClick = {
                    navController.navigate(Routes.Ext.Home)
                },
                icon = Icons.Default.Shop,
                text = "Go to {ext_home_title}".curlyFormat(
                    "ext_home_title" to stringRes(R.string.ext__home__title),
                ),
            )
        }
    }
}
