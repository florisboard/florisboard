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

package dev.patrickgold.florisboard.ime.nlp

import android.content.Context
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.nlp.han.HanShapeBasedLanguageProvider
import dev.patrickgold.florisboard.ime.nlp.latin.LatinLanguageProvider
import dev.patrickgold.florisboard.lib.kotlin.guardedByLock
import java.util.concurrent.atomic.AtomicBoolean

class NlpProviderRegistry(context: Context) {
    private val registeredProviders = guardedByLock {
        buildProviderRegistry {
            addProvider(LatinLanguageProvider.ProviderId) {
                LatinLanguageProvider(context)
            }
            addProvider(HanShapeBasedLanguageProvider.ProviderId) {
                HanShapeBasedLanguageProvider(context)
            }
        }
    }

    suspend fun getWrapper(providerId: String): NlpProviderWrapper? {
        return registeredProviders.withLock { providers ->
            providers.find { it.provider.providerId == providerId }
        }
    }

    suspend fun getSpellingProvider(subtype: Subtype): SpellingProvider {
        return registeredProviders.withLock { wrappers ->
            wrappers.find { it.provider.providerId == subtype.nlpProviders.spelling } as? SpellingProvider
                ?: FallbackNlpProvider
        }
    }

    suspend fun getSuggestionProvider(subtype: Subtype): SuggestionProvider {
        return registeredProviders.withLock { wrappers ->
            wrappers.find { it.provider.providerId == subtype.nlpProviders.suggestion } as? SuggestionProvider
                ?: FallbackNlpProvider
        }
    }
}

private inline fun buildProviderRegistry(
    registerAction: MutableList<NlpProviderWrapper>.() -> Unit,
): List<NlpProviderWrapper> {
    return buildList(registerAction)
}

private inline fun MutableList<NlpProviderWrapper>.addProvider(
    providerId: String,
    initializer: () -> NlpProvider,
) {
    val provider = initializer()
    assert(provider.providerId == providerId)
    add(NlpProviderWrapper(provider))
}

class NlpProviderWrapper(val provider: NlpProvider) {
    private val isInitialized = AtomicBoolean(false)

    suspend fun createIfNecessary() {
        if (!isInitialized.getAndSet(true)) {
            provider.create()
        }
    }

    suspend fun preload(subtype: Subtype) {
        provider.preload(subtype)
    }

    suspend fun destroyIfNecessary() {
        if (!isInitialized.getAndSet(true)) {
            provider.create()
        }
    }
}
