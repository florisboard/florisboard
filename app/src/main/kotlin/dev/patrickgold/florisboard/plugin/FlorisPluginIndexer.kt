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
import dev.patrickgold.florisboard.BuildConfig
import dev.patrickgold.florisboard.lib.kotlin.guardedByLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking

class FlorisPluginIndexer(private val context: Context) {
    private val _pluginIndexFlow = MutableStateFlow(listOf<IndexedPlugin>())
    val pluginIndexFlow = _pluginIndexFlow.asStateFlow()
    val pluginIndex = guardedByLock { mutableListOf<IndexedPlugin>() }

    suspend fun indexBoundServices() {
        val intent = Intent(FlorisPluginService.SERVICE_INTERFACE)
        val packageManager = context.packageManager
        val resolveInfoList = packageManager.queryIntentServices(intent, PackageManager.GET_META_DATA)

        pluginIndex.withLock { pluginIndex ->
            val newPluginIndex = mutableListOf<IndexedPlugin>()

            suspend fun registerPlugin(
                serviceName: ComponentName,
                state: IndexedPluginState,
                metadata: FlorisPluginMetadata = FlorisPluginMetadata(""),
            ) {
                val plugin = pluginIndex.find { it.serviceName == serviceName }?.also {
                    it.state = state
                    it.metadata = metadata
                    pluginIndex.remove(it)
                } ?: IndexedPlugin(context, serviceName, state, metadata)
                plugin.create()
                newPluginIndex.add(plugin)
            }

            for (resolveInfo in resolveInfoList) {
                val serviceName = ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name)
                // TODO: hard-coding blocking third-party plugins for now
                if (serviceName.packageName != BuildConfig.APPLICATION_ID) {
                    continue
                }
                val metadataBundle = resolveInfo.serviceInfo.metaData
                if (metadataBundle == null) {
                    registerPlugin(serviceName, stateError(IndexedPluginError.NoMetadata))
                    continue
                }
                val metadataXmlId = metadataBundle.getInt(FlorisPluginService.SERVICE_METADATA, -1)
                if (metadataXmlId == -1) {
                    registerPlugin(serviceName, stateError(IndexedPluginError.NoMetadata))
                    continue
                }
                val packageContext = context.createPackageContext(resolveInfo.serviceInfo.packageName, 0)
                try {
                    val metadata = FlorisPluginMetadata.parseFromXml(packageContext, metadataXmlId)
                    registerPlugin(serviceName, stateOk(), metadata)
                } catch (e: Exception) {
                    registerPlugin(serviceName, stateError(IndexedPluginError.InvalidMetadata, e))
                }
            }

            // Destroy remaining plugins which are not present anymore
            for (oldPlugin in pluginIndex) {
                oldPlugin.destroy()
            }
            pluginIndex.clear()
            pluginIndex.addAll(newPluginIndex)
            _pluginIndexFlow.value = pluginIndex.toList()
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

    suspend fun getOrNull(pluginId: String?): IndexedPlugin? {
        if (pluginId == null) return null
        return pluginIndex.withLock { pluginIndex ->
            pluginIndex.find { it.metadata.id == pluginId }
        }
    }
}
