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

object AndroidVersion {
    /** Android 7 **/
    val ATLEAST_N = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    val ATMOST_N = Build.VERSION.SDK_INT <= Build.VERSION_CODES.N

    /** Android 7.1 **/
    val ATLEAST_N_MR1 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1
    val ATMOST_N_MR1 = Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1

    /** Android 8 **/
    val ATLEAST_O = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    val ATMOST_O = Build.VERSION.SDK_INT <= Build.VERSION_CODES.O

    /** Android 8.1 **/
    val ATLEAST_O_MR1 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
    val ATMOST_O_MR1 = Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1

    /** Android 9 **/
    val ATLEAST_P = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    val ATMOST_P = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P

    /** Android 10 **/
    val ATLEAST_Q = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    val ATMOST_Q = Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q

    /** Android 11 **/
    val ATLEAST_R = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    val ATMOST_R = Build.VERSION.SDK_INT <= Build.VERSION_CODES.R

    /** Android 12 **/
    val ATLEAST_S = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val ATMOST_S = Build.VERSION.SDK_INT <= Build.VERSION_CODES.S
}
