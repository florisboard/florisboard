/*
 * Copyright (C) 2014, The Android Open Source Project
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

#ifndef LATINIME_PROFILER_H
#define LATINIME_PROFILER_H

#ifdef FLAG_DO_PROFILE

#include "defines.h"

#include <ctime>
#include <unordered_map>

namespace latinime {

class Profiler final {
 public:
    Profiler(const clockid_t clockId)
            : mClockId(clockId), mStartTime(getTimeInMicroSec()), mStartTimes(), mTimes(),
              mCounters() {}

    ~Profiler() {
        const float totalTime =
                static_cast<float>(getTimeInMicroSec() - mStartTime) / 1000.f;
        AKLOGI("Total time is %6.3f ms.", totalTime);
        for (const auto &time : mTimes) {
            AKLOGI("(%d): Used %4.2f%%, %8.4f ms. Called %d times.", time.first,
                    time.second / totalTime * 100.0f, time.second, mCounters[time.first]);
        }
    }

    void startTimer(const int id) {
        mStartTimes[id] = getTimeInMicroSec();
    }

    void endTimer(const int id) {
        mTimes[id] += static_cast<float>(getTimeInMicroSec() - mStartTimes[id]) / 1000.0f;
        mCounters[id]++;
    }

    operator bool() const { return false; }

 private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(Profiler);

    const clockid_t mClockId;
    int64_t mStartTime;
    std::unordered_map<int, int64_t> mStartTimes;
    std::unordered_map<int, float> mTimes;
    std::unordered_map<int, int> mCounters;

    int64_t getTimeInMicroSec() {
        timespec time;
        clock_gettime(mClockId, &time);
        return static_cast<int64_t>(time.tv_sec) * 1000000
                + static_cast<int64_t>(time.tv_nsec) / 1000;
    }
};
} // namespace latinime

#define PROF_INIT Profiler __LATINIME__PROFILER__(CLOCK_THREAD_CPUTIME_ID)
#define PROF_TIMER_START(timer_id) __LATINIME__PROFILER__.startTimer(timer_id)
#define PROF_TIMER_END(timer_id) __LATINIME__PROFILER__.endTimer(timer_id)

#else // FLAG_DO_PROFILE

#define PROF_INIT
#define PROF_TIMER_START(timer_id)
#define PROF_TIMER_END(timer_id)

#endif // FLAG_DO_PROFILE

#endif /* LATINIME_PROFILER_H */
