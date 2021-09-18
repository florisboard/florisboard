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

package dev.patrickgold.florisboard.util

import android.os.Build

@Suppress("unused")
object AndroidVersion {
    /** Android 7 **/
    inline val ATLEAST_N get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    inline val ATMOST_N get() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.N

    /** Android 7.1 **/
    inline val ATLEAST_N_MR1 get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1
    inline val ATMOST_N_MR1 get() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1

    /** Android 8 **/
    inline val ATLEAST_O get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    inline val ATMOST_O get() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.O

    /** Android 8.1 **/
    inline val ATLEAST_O_MR1 get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
    inline val ATMOST_O_MR1 get() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1

    /** Android 9 **/
    inline val ATLEAST_P get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    inline val ATMOST_P get() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P

    /** Android 10 **/
    inline val ATLEAST_Q get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    inline val ATMOST_Q get() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q

    /** Android 11 **/
    inline val ATLEAST_R get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    inline val ATMOST_R get() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.R

    /** Android 12 **/
    inline val ATLEAST_S get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    inline val ATMOST_S get() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.S
}
