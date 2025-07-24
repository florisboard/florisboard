/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.app.settings.theme

/**
 * SnyggLevel indicates if a rule property is intended to be edited by all users (BASIC) or only by advanced users
 * (ADVANCED). This level is intended for theme editor UIs to hide certain properties in a "basic" mode, for the Snygg
 * theme engine internally this level will be ignored completely.
 */
enum class SnyggLevel : Comparable<SnyggLevel> {
    /** A property is intended to be edited by all users **/
    BASIC,
    /** A property is intended to be edited by advanced users **/
    ADVANCED,
    /** A property is intended to be edited by developers **/
    DEVELOPER;
}
