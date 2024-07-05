/*
 * Copyright (C) 2022 Patrick Goldinger
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

import org.florisboard.lib.android.AndroidVersion

/**
 * Helper object containing methods to validate and extract network names and components from strings.
 */
@Suppress("RegExpRedundantEscape", "RegExpUnnecessaryNonCapturingGroup")
object NetworkUtils {
    private val Ipv4Regex = """(?<Ipv4>(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?))""".toRegex()
    private val Ipv6Regex = """(?<Ipv6>(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]+|::(ffff(:0{1,4})?:)?((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])))""".toRegex()
    private val HostRegex = """(?<Host>(?:[a-zA-Z\-]+\.)+[a-zA-Z]{2,}|$Ipv4Regex|$Ipv6Regex)""".toRegex()
    private val TcpIpPortRegex = """(?<TcpIpPort>6553[0-5]|655[0-2][0-9]|65[0-4][0-9]{2}|6[0-4][0-9]{3}|(?<![0-9])[0-5]?[0-9]{1,4}(?![0-9]))""".toRegex()
    private val UrlRegex = """(?<Url>(?:(?:(?:https?:\/\/)?$HostRegex)|(?:https?:\/\/[a-zA-Z]+))(?::$TcpIpPortRegex)?(?:\/[\p{L}0-9.,;?'\\\/+&%$#=~_\-]*)?)""".toRegex()
    private val EmailRegex = """(?<Email>(?:[a-z0-9!#${'$'}%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#${'$'}%&'*+/=?^_`{|}~-]+)*|"(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21\x23-\x5b\x5d-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])*")@$HostRegex)""".toRegex()
    private val PhoneNumberRegex = """(?<Phone>(?<![0-9]|[0-9][\x20.-]|[+]|[-])(?:(?:0?1-)?[0-9]{3}-[A-Z]{2}(?:-?[A-Z]){4}[A-Z]|(?:[(]?(?:[+]|00)[\x20.-]?)?(?:[(]?[0-9](?:[)]?[\x20.-]?[(]?[0-9]){4,14}[)]?))(?![\x20.-][0-9]|[0-9]|[-]))""".toRegex()

    fun isUrl(str: CharSequence): Boolean {
        return UrlRegex.matches(str.trim())
    }

    fun getUrls(str: CharSequence): List<MatchGroup> {
        if (AndroidVersion.ATMOST_API25_N_MR1) return emptyList() // See issue #1970
        return UrlRegex.findAll(str).mapNotNull { it.groups["Url"] }.toList()
    }

    fun isEmailAddress(str: CharSequence): Boolean {
        return EmailRegex.matches(str.trim())
    }

    fun getEmailAddresses(str: CharSequence): List<MatchGroup> {
        if (AndroidVersion.ATMOST_API25_N_MR1) return emptyList() // See issue #1970
        return EmailRegex.findAll(str).mapNotNull { it.groups["Email"] }.toList()
    }

    fun isPhoneNumber(str: CharSequence): Boolean {
        return PhoneNumberRegex.matches(str.trim())
    }

    fun getPhoneNumbers(str: CharSequence): List<MatchGroup> {
        if (AndroidVersion.ATMOST_API25_N_MR1) return emptyList() // See issue #1970
        return PhoneNumberRegex.findAll(str).mapNotNull { it.groups["Phone"] }.toList()
    }
}
