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

package dev.patrickgold.florisboard.ime.nlp.latin

import dev.patrickgold.florisboard.ime.core.ComputedSubtype
import dev.patrickgold.florisboard.ime.keyboard.KeyProximityChecker
import dev.patrickgold.florisboard.lib.io.subFile
import dev.patrickgold.florisboard.lib.io.writeJson
import dev.patrickgold.florisboard.lib.kotlin.guardedByLock
import dev.patrickgold.florisboard.native.NativeStr
import dev.patrickgold.florisboard.native.toNativeStr
import dev.patrickgold.florisboard.plugin.FlorisPluginService
import dev.patrickgold.florisboard.subtypeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val DEFAULT_PREDICTION_WEIGHTS = LatinPredictionWeights(
    lookup = LatinPredictionLookupWeights(
        maxCostSum = 1.5,
        costIsEqual = 0.0,
        costIsEqualIgnoringCase = 0.25,
        costInsert = 0.5,
        costInsertStartOfStr = 1.0,
        costDelete = 0.5,
        costDeleteStartOfStr = 1.0,
        costSubstitute = 0.5,
        costSubstituteInProximity = 0.25,
        costSubstituteStartOfStr = 1.0,
        costTranspose = 0.0,
    ),
    training = LatinPredictionTrainingWeights(
        usageBonus = 128,
        usageReductionOthers = 1,
    ),
)

private val DEFAULT_KEY_PROXIMITY_CHECKER = KeyProximityChecker(
    enabled = false,
    mapping = mapOf(),
)

private data class LatinNlpSessionWrapper(
    var subtype: ComputedSubtype,
    var session: LatinNlpSession,
)

class LatinLanguageProviderService : FlorisPluginService() {
    companion object {
        const val NlpSessionConfigFileName = "nlp_session_config.json"
        const val UserDictionaryFileName = "user_dict.fldic"

        external fun nativeInitEmptyDictionary(dictPath: NativeStr)
    }

    private val subtypeManager by subtypeManager()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val cachedSessionWrappers = guardedByLock {
        mutableListOf<LatinNlpSessionWrapper>()
    }

    override fun create() {
        // Do nothing
    }

    override fun preload(subtype: ComputedSubtype) {
        if (subtype.isFallback()) return
        scope.launch {
            cachedSessionWrappers.withLock { sessionWrappers ->
                var sessionWrapper = sessionWrappers.find { it.subtype.id == subtype.id }
                if (sessionWrapper == null || sessionWrapper.subtype != subtype) {
                    if (sessionWrapper == null) {
                        sessionWrapper = LatinNlpSessionWrapper(
                            subtype = subtype,
                            session = LatinNlpSession(),
                        )
                        sessionWrappers.add(sessionWrapper)
                    } else {
                        sessionWrapper.subtype = subtype
                    }
                    val cacheDir = subtypeManager.cacheDirFor(subtype)
                    val filesDir = subtypeManager.filesDirFor(subtype)
                    val configFile = cacheDir.subFile(NlpSessionConfigFileName)
                    val userDictFile = filesDir.subFile(UserDictionaryFileName)
                    if (!userDictFile.exists()) {
                        nativeInitEmptyDictionary(userDictFile.absolutePath.toNativeStr())
                    }
                    val config = LatinNlpSessionConfig(
                        primaryLocale = subtype.primaryLocale,
                        secondaryLocales = subtype.secondaryLocales,
                        // TODO: dynamically find base dictionaries
                        baseDictionaryPaths = listOf(),
                        userDictionaryPath = userDictFile.absolutePath,
                        predictionWeights = DEFAULT_PREDICTION_WEIGHTS,
                        keyProximityChecker = DEFAULT_KEY_PROXIMITY_CHECKER,
                    )
                    configFile.writeJson(config)
                    sessionWrapper.session.loadFromConfigFile(configFile)
                }
            }
        }
    }

    override fun destroy() {
        //
    }
}
