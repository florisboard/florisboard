/*
 * Copyright (C) 2013, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "utils/time_keeper.h"

#include <ctime>

namespace latinime {

int TimeKeeper::sCurrentTime;
bool TimeKeeper::sSetForTesting;

/* static  */ void TimeKeeper::setCurrentTime() {
    if (!sSetForTesting) {
        sCurrentTime = time(0);
    }
}

/* static */ void TimeKeeper::startTestModeWithForceCurrentTime(const int currentTime) {
    sCurrentTime = currentTime;
    sSetForTesting = true;
}

/* static */ void TimeKeeper::stopTestMode() {
    sSetForTesting = false;
}

} // namespace latinime
