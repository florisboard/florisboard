/*
 * Copyright (C) 2014 The Android Open Source Project
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

#ifndef LATINIME_NORMAL_DISTRIBUTION_2D_H
#define LATINIME_NORMAL_DISTRIBUTION_2D_H

#include <cmath>

#include "defines.h"
#include "suggest/core/layout/geometry_utils.h"
#include "suggest/core/layout/normal_distribution.h"

namespace latinime {

// Normal distribution on a 2D plane. The covariance is always zero, but the distribution can be
// rotated.
class NormalDistribution2D {
 public:
    NormalDistribution2D(const float uX, const float sigmaX, const float uY, const float sigmaY,
            const float theta)
            : mXDistribution(0.0f, sigmaX), mYDistribution(0.0f, sigmaY), mUX(uX), mUY(uY),
              mSinTheta(sinf(theta)), mCosTheta(cosf(theta)) {}

    float getProbabilityDensity(const float x, const float y) const {
        // Shift
        const float shiftedX = x - mUX;
        const float shiftedY = y - mUY;
        // Rotate
        const float rotatedShiftedX = mCosTheta * shiftedX + mSinTheta * shiftedY;
        const float rotatedShiftedY = -mSinTheta * shiftedX + mCosTheta * shiftedY;
        return mXDistribution.getProbabilityDensity(rotatedShiftedX)
                * mYDistribution.getProbabilityDensity(rotatedShiftedY);
    }

 private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(NormalDistribution2D);

    const NormalDistribution mXDistribution;
    const NormalDistribution mYDistribution;
    const float mUX;
    const float mUY;
    const float mSinTheta;
    const float mCosTheta;
};
} // namespace latinime
#endif // LATINIME_NORMAL_DISTRIBUTION_2D_H
