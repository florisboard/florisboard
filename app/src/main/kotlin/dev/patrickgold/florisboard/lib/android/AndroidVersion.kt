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

package dev.patrickgold.florisboard.lib.android

import android.os.Build

@Suppress("unused")
object AndroidVersion {
    /** Android 7.1 **/
    inline val ATLEAST_API25_N_MR1 get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1
    inline val ATMOST_API25_N_MR1 get() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1

    /** Android 8 **/
    inline val ATLEAST_API26_O get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    inline val ATMOST_API26_O get() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.O

    /** Android 8.1 **/
    inline val ATLEAST_API27_O_MR1 get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
    inline val ATMOST_API27_O_MR1 get() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1

    /** Android 9 **/
    inline val ATLEAST_API28_P get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    inline val ATMOST_API28_P get() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P

    /** Android 10 **/
    inline val ATLEAST_API29_Q get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    inline val ATMOST_API29_Q get() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q

    /** Android 11 **/
    inline val ATLEAST_API30_R get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    inline val ATMOST_API30_R get() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.R

    /** Android 12 **/
    inline val ATLEAST_API31_S get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    inline val ATMOST_API31_S get() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.S
}
