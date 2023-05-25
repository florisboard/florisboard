/*
 * Copyright (C) 2023 Patrick Goldinger
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

package dev.patrickgold.florisboard.plugin

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import dev.patrickgold.florisboard.lib.kotlin.guardedByLock
import kotlinx.coroutines.runBlocking

enum class IndexedPluginError {
    NoMetadata,
    InvalidMetadata,
}

sealed class IndexedPluginState {
    object Ok : IndexedPluginState()
    data class Error(val type: IndexedPluginError, val exception: Exception?) : IndexedPluginState()

    override fun toString(): String {
        return when (this) {
            is Ok -> "Ok"
            is Error -> "Error($type, $exception)"
        }
    }
}

data class IndexedPlugin(
    var state: IndexedPluginState,
    var metadata: FlorisPluginMetadata = FlorisPluginMetadata(""),
) {
    fun isValid(): Boolean {
        return state == IndexedPluginState.Ok
    }

    fun toString(packageContext: Context): String {
        return """
            IndexedPlugin {
                state=$state
                metadata=${metadata.toString(packageContext).prependIndent("                ").substring(16)}
            }
        """.trimIndent()
    }
}

private fun stateOk() = IndexedPluginState.Ok

private fun stateError(type: IndexedPluginError, exception: Exception? = null) =
    IndexedPluginState.Error(type, exception)

class FlorisPluginIndexer(private val context: Context) {
    val pluginIndex = guardedByLock { mutableMapOf<ComponentName, IndexedPlugin>() }

    suspend fun indexBoundServices() {
        val intent = Intent(FlorisPluginService.SERVICE_INTERFACE)
        val packageManager = context.packageManager
        val resolveInfoList = packageManager.queryIntentServices(intent, PackageManager.GET_META_DATA)

        pluginIndex.withLock { pluginIndex ->
            pluginIndex.clear()
            for (resolveInfo in resolveInfoList) {
                val componentName = ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name)
                val metadataBundle = resolveInfo.serviceInfo.metaData
                if (metadataBundle == null) {
                    pluginIndex[componentName] = IndexedPlugin(stateError(IndexedPluginError.NoMetadata))
                    continue
                }
                val metadataXmlId = metadataBundle.getInt(FlorisPluginService.SERVICE_METADATA, -1)
                if (metadataXmlId == -1) {
                    pluginIndex[componentName] = IndexedPlugin(stateError(IndexedPluginError.NoMetadata))
                    continue
                }
                val packageContext = context.createPackageContext(resolveInfo.serviceInfo.packageName, 0)
                try {
                    val metadata = FlorisPluginMetadata.parseFromXml(packageContext, metadataXmlId)
                    pluginIndex[componentName] = IndexedPlugin(stateOk(), metadata)
                } catch (e: Exception) {
                    pluginIndex[componentName] = IndexedPlugin(stateError(IndexedPluginError.InvalidMetadata, e))
                }
            }
        }
    }

    fun observeServiceChanges() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent != null && intent.action == "android.intent.action.PACKAGE_CHANGED") {
                    runBlocking { indexBoundServices() }
                }
            }
        }

        context.registerReceiver(receiver, IntentFilter("android.intent.action.PACKAGE_CHANGED"))
    }

    suspend fun getOrNull(pluginId: String): IndexedPlugin? {
        return pluginIndex.withLock { pluginIndex ->
            pluginIndex.values.find { plugin -> plugin.isValid() && plugin.metadata.id == pluginId }
        }
    }
}
