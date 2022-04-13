/*
 * Copyright (C) 2020 Patrick Goldinger
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

package dev.patrickgold.florisboard.lib.util

import android.content.Context
import dev.patrickgold.florisboard.app.AppPrefs

object AppVersionUtils {
    private fun getRawVersionName(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            "undefined"
        }
    }

    fun shouldShowChangelog(context: Context, prefs: AppPrefs): Boolean {
        val installVersion =
            VersionName.fromString(prefs.internal.versionOnInstall.get()) ?: VersionName.DEFAULT
        val lastChangelogVersion =
            VersionName.fromString(prefs.internal.versionLastChangelog.get()) ?: VersionName.DEFAULT
        val currentVersion =
            VersionName.fromString(getRawVersionName(context)) ?: VersionName.DEFAULT

        return lastChangelogVersion < currentVersion && installVersion != currentVersion
    }

    fun updateVersionOnInstallAndLastUse(context: Context, prefs: AppPrefs) {
        if (prefs.internal.versionOnInstall.get() == VersionName.DEFAULT_RAW) {
            prefs.internal.versionOnInstall.set(getRawVersionName(context))
        }
        prefs.internal.versionLastUse.set(getRawVersionName(context))
    }

    fun updateVersionLastChangelog(context: Context, prefs: AppPrefs) {
        prefs.internal.versionLastChangelog.set(getRawVersionName(context))
    }
}

data class VersionName(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val extraName: String? = null,
    val extraValue: Int? = null
) {
    companion object {
        val DEFAULT: VersionName = VersionName(0, 0, 0)
        const val DEFAULT_RAW: String = "0.0.0"

        fun fromString(raw: String): VersionName? {
            if (raw.matches("""[0-9]+[.][0-9]+[.][0-9]+""".toRegex())) {
                val list = raw.split(".").map { it.toInt() }
                if (list.size == 3) {
                    return VersionName(list[0], list[1], list[2])
                }
            } else if (raw.matches("""[0-9]+[.][0-9]+[.][0-9]+[-][0-9]+""".toRegex())) {
                val list = raw.split(".").map { it.toInt() }
                if (list.size == 4) {
                    return VersionName(list[0], list[1], list[2], null, list[3])
                }
            } else if (raw.matches("""[0-9]+[.][0-9]+[.][0-9]+[-][a-zA-Z]+""".toRegex())) {
                val list = raw.split(".")
                if (list.size == 4) {
                    return VersionName(
                        list[0].toInt(), list[1].toInt(), list[2].toInt(),
                        list[3], null
                    )
                }
            } else if (raw.matches("""[0-9]+[.][0-9]+[.][0-9]+[-][a-zA-Z]+[0-9]+""".toRegex())) {
                val list = raw.split(".")
                if (list.size == 4) {
                    val extraName = list[3].split("""[0-9]+""".toRegex())[0]
                    val extraValue = list[3].split("""[a-zA-Z]+""".toRegex())[1].toInt()
                    return VersionName(
                        list[0].toInt(), list[1].toInt(), list[2].toInt(),
                        extraName, extraValue
                    )
                }
            }
            return null
        }
    }

    override fun toString(): String {
        val mmp = "$major.$minor.$patch"
        return if (extraName != null || extraValue != null) {
            val extraName = extraName ?: ""
            val extraValue = extraValue?.toString() ?: ""
            "$mmp.$extraName$extraValue"
        } else {
            mmp
        }
    }

    operator fun compareTo(other: VersionName): Int {
        if (major != other.major) {
            return major.compareTo(other.major)
        } else if (minor != other.minor) {
            return minor.compareTo(other.minor)
        } else if (patch != other.patch) {
            return patch.compareTo(other.patch)
        } else if (extraValue != null && other.extraValue != null) {
            if (extraValue != other.extraValue) {
                return extraValue.compareTo(other.extraValue)
            }
        }
        return 0
    }
}
