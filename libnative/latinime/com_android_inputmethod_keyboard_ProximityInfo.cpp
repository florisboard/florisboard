/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "LatinIME: jni: ProximityInfo"

#include "com_android_inputmethod_keyboard_ProximityInfo.h"

#include "defines.h"
#include "jni.h"
#include "jni_common.h"
#include "suggest/core/layout/proximity_info.h"

namespace latinime {

static jlong latinime_Keyboard_setProximityInfo(JNIEnv *env, jclass clazz,
        jint displayWidth, jint displayHeight, jint gridWidth, jint gridHeight,
        jint mostCommonkeyWidth, jint mostCommonkeyHeight, jintArray proximityChars, jint keyCount,
        jintArray keyXCoordinates, jintArray keyYCoordinates, jintArray keyWidths,
        jintArray keyHeights, jintArray keyCharCodes, jfloatArray sweetSpotCenterXs,
        jfloatArray sweetSpotCenterYs, jfloatArray sweetSpotRadii) {
    ProximityInfo *proximityInfo = new ProximityInfo(env, displayWidth, displayHeight,
            gridWidth, gridHeight, mostCommonkeyWidth, mostCommonkeyHeight, proximityChars,
            keyCount, keyXCoordinates, keyYCoordinates, keyWidths, keyHeights, keyCharCodes,
            sweetSpotCenterXs, sweetSpotCenterYs, sweetSpotRadii);
    return reinterpret_cast<jlong>(proximityInfo);
}

static void latinime_Keyboard_release(JNIEnv *env, jclass clazz, jlong proximityInfo) {
    ProximityInfo *pi = reinterpret_cast<ProximityInfo *>(proximityInfo);
    delete pi;
}

static const JNINativeMethod sMethods[] = {
    {
        const_cast<char *>("setProximityInfoNative"),
        const_cast<char *>("(IIIIII[II[I[I[I[I[I[F[F[F)J"),
        reinterpret_cast<void *>(latinime_Keyboard_setProximityInfo)
    },
    {
        const_cast<char *>("releaseProximityInfoNative"),
        const_cast<char *>("(J)V"),
        reinterpret_cast<void *>(latinime_Keyboard_release)
    }
};

int register_ProximityInfo(JNIEnv *env) {
    const char *const kClassPathName = "com/android/inputmethod/keyboard/ProximityInfo";
    return registerNativeMethods(env, kClassPathName, sMethods, NELEMS(sMethods));
}
} // namespace latinime
