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

package dev.patrickgold.florisboard.ime.spelling

import android.content.Context
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextServicesManager
import dev.patrickgold.florisboard.debug.flogWarning
import dev.patrickgold.florisboard.ime.extension.AssetManager
import dev.patrickgold.florisboard.ime.extension.AssetRef
import java.lang.ref.WeakReference
import java.util.*


class SpellingManager private constructor(
    val applicationContext: WeakReference<Context>,
    configRef: AssetRef
) {
    companion object {
        private var defaultInstance: SpellingManager? = null

        private val STUB_LISTENER = object : SpellCheckerSession.SpellCheckerSessionListener {
            override fun onGetSuggestions(results: Array<out SuggestionsInfo>?) {
                // Intentionally empty
            }

            override fun onGetSentenceSuggestions(results: Array<out SentenceSuggestionsInfo>?) {
                // Intentionally empty
            }
        }

        fun init(context: Context, configRef: AssetRef): SpellingManager {
            val applicationContext = WeakReference(context.applicationContext ?: context)
            val instance = SpellingManager(applicationContext, configRef)
            defaultInstance = instance
            return instance
        }

        fun default() = defaultInstance!!

        fun defaultOrNull() = defaultInstance
    }

    private val tsm =
        applicationContext.get()?.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE) as? TextServicesManager

    private val assetManager get() = AssetManager.default()
    private val spellingDictCache: MutableMap<AssetRef, SpellingDict> = mutableMapOf()
    private val indexedSpellingDictMetas: MutableMap<AssetRef, SpellingDict.Meta> = mutableMapOf()
    val indexedSpellingDicts: Map<AssetRef, SpellingDict.Meta>
        get() = indexedSpellingDictMetas

    val config = assetManager.loadJsonAsset<SpellingConfig>(configRef).getOrDefault(SpellingConfig.default())
    val importSourceLabels: List<String>
    val importSourceUrls: List<String?>

    init {
        config.importSources.map { it.label }.toMutableList().let {
            it.add(0, "-")
            importSourceLabels = it.toList()
        }
        config.importSources.map { it.url }.toMutableList().let {
            it.add(0, "-")
            importSourceUrls = it.toList()
        }
        indexSpellingDicts()
    }

    fun getCurrentSpellingServiceName(): String? {
        try {
            val session = tsm?.newSpellCheckerSession(
                null, Locale.ENGLISH, STUB_LISTENER, false
            ) ?: return null
            val context = applicationContext.get() ?: return null
            val pm = context.packageManager
            return session.spellChecker.loadLabel(pm).toString()
        } catch (e: Exception) {
            flogWarning { e.toString() }
            return null
        }
    }

    @Synchronized
    fun getSpellingDict(locale: Locale): SpellingDict? {
        val entry = indexedSpellingDictMetas.firstNotNullOfOrNull {
            if (it.value.locale.toString() == locale.toString()) it else null
        } ?: indexedSpellingDictMetas.firstNotNullOfOrNull {
            if (it.value.locale.language == locale.language) it else null
        } ?: return null
        val ref = entry.key
        val meta = entry.value
        val cachedDict = spellingDictCache[ref]
        if (cachedDict != null) {
            return cachedDict
        }
        val newDict = SpellingDict.new(ref, meta)
        spellingDictCache[ref] = newDict
        return newDict
    }

    fun indexSpellingDicts(): Boolean {
        indexedSpellingDictMetas.clear()
        assetManager.listAssets<SpellingDict.Meta>(AssetRef.internal(config.basePath)).onSuccess { map ->
            for ((ref, meta) in map) {
                indexedSpellingDictMetas[ref] = meta
            }
            return true
        }
        return false
    }
}
