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

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import dev.patrickgold.florisboard.ime.core.ComputedSubtype
import dev.patrickgold.florisboard.ime.nlp.NlpProvider
import dev.patrickgold.florisboard.ime.nlp.SpellingProvider
import dev.patrickgold.florisboard.ime.nlp.SuggestionProvider
import dev.patrickgold.florisboard.lib.devtools.flogDebug
import dev.patrickgold.florisboard.lib.devtools.flogError
import dev.patrickgold.florisboard.lib.devtools.flogInfo
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.lang.ref.WeakReference

abstract class FlorisPluginService : Service(), NlpProvider {
    companion object {
        const val SERVICE_INTERFACE = "org.florisboard.plugin.FlorisPluginService"
        const val SERVICE_METADATA = "org.florisboard.plugin.flp"

        const val CONSUMER_PACKAGE_NAME = "org.florisboard.plugin.CONSUMER_PACKAGE_NAME"
        const val CONSUMER_VERSION_CODE = "org.florisboard.plugin.CONSUMER_VERSION_CODE"
        const val CONSUMER_VERSION_NAME = "org.florisboard.plugin.CONSUMER_VERSION_NAME"
    }

    private lateinit var serviceMessenger: Messenger
    private lateinit var consumerInfo: PluginConsumerInfo

    final override fun onCreate() {
        flogDebug { "" }

        super.onCreate()
        create()
    }

    final override fun onBind(intent: Intent?): IBinder? {
        flogDebug { "${intent?.toUri(0)}" }
        if (intent == null) return null

        consumerInfo = PluginConsumerInfo(
            packageName = intent.getStringExtra(CONSUMER_PACKAGE_NAME) ?: return null,
            versionCode = intent.getIntExtra(CONSUMER_VERSION_CODE, -1),
            versionName = intent.getStringExtra(CONSUMER_VERSION_NAME) ?: return null,
        )
        serviceMessenger = Messenger(IncomingHandler(this))

        flogDebug { "consumerInfo = $consumerInfo" }
        return serviceMessenger.binder
    }

    final override fun onDestroy() {
        flogDebug { "" }

        destroy()
        super.onDestroy()
    }

    protected data class PluginConsumerInfo(
        val packageName: String,
        val versionCode: Int,
        val versionName: String,
    )

    private class IncomingHandler(service: FlorisPluginService) : Handler(service.mainLooper) {
        private val serviceReference = WeakReference(service)

        override fun handleMessage(msg: Message) {
            val service = serviceReference.get() ?: return
            val message = FlorisPluginMessage(msg)
            val (source, type, action) = message.metadata()
            if (source != FlorisPluginMessage.SOURCE_CONSUMER) {
                return
            }
            when (type) {
                FlorisPluginMessage.TYPE_REQUEST -> when (action) {
                    FlorisPluginMessage.ACTION_PRELOAD -> {
                        try {
                            val data = message.data() ?: return
                            val subtype = Json.decodeFromString(ComputedSubtype.serializer(), data)
                            post {
                                flogInfo { "Processing action preload: $subtype" }
                                service.preload(subtype)
                            }
                        } catch (e: SerializationException) {
                            flogError { "Ill-formatted JSON data for action preload: $e" }
                            return
                        } catch (e: IllegalArgumentException) {
                            flogError { "Invalid JSON data for action preload: $e" }
                            return
                        }
                    }
                    FlorisPluginMessage.ACTION_SPELL -> {
                        if (service !is SpellingProvider) {
                            return
                        }
                    }
                    FlorisPluginMessage.ACTION_SUGGEST -> {
                        if (service !is SuggestionProvider) {
                            return
                        }
                    }
                    else -> {
                        return
                    }
                }
                else -> {
                    return
                }
            }
        }
    }
}
