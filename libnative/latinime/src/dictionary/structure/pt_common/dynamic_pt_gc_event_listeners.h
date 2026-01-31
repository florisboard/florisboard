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

#ifndef LATINIME_DYNAMIC_PT_GC_EVENT_LISTENERS_H
#define LATINIME_DYNAMIC_PT_GC_EVENT_LISTENERS_H

#include <vector>

#include "defines.h"
#include "dictionary/structure/pt_common/dynamic_pt_reading_helper.h"
#include "dictionary/structure/pt_common/pt_node_writer.h"
#include "dictionary/utils/buffer_with_extendable_buffer.h"

namespace latinime {

class PtNodeParams;

class DynamicPtGcEventListeners {
 public:
    // Updates all PtNodes that can be reached from the root. Checks if each PtNode is useless or
    // not and marks useless PtNodes as deleted. Such deleted PtNodes will be discarded in the GC.
    // TODO: Concatenate non-terminal PtNodes.
    class TraversePolicyToUpdateUnigramProbabilityAndMarkUselessPtNodesAsDeleted
        : public DynamicPtReadingHelper::TraversingEventListener {
     public:
        TraversePolicyToUpdateUnigramProbabilityAndMarkUselessPtNodesAsDeleted(
                PtNodeWriter *const ptNodeWriter)
                : mPtNodeWriter(ptNodeWriter), mValueStack(), mChildrenValue(0),
                  mValidUnigramCount(0) {}

        ~TraversePolicyToUpdateUnigramProbabilityAndMarkUselessPtNodesAsDeleted() {};

        bool onAscend() {
            if (mValueStack.empty()) {
                return false;
            }
            mChildrenValue = mValueStack.back();
            mValueStack.pop_back();
            return true;
        }

        bool onDescend(const int ptNodeArrayPos) {
            mValueStack.push_back(0);
            mChildrenValue = 0;
            return true;
        }

        bool onReadingPtNodeArrayTail() { return true; }

        bool onVisitingPtNode(const PtNodeParams *const ptNodeParams);

        int getValidUnigramCount() const {
            return mValidUnigramCount;
        }

     private:
        DISALLOW_IMPLICIT_CONSTRUCTORS(
                TraversePolicyToUpdateUnigramProbabilityAndMarkUselessPtNodesAsDeleted);

        PtNodeWriter *const mPtNodeWriter;
        std::vector<int> mValueStack;
        int mChildrenValue;
        int mValidUnigramCount;
    };

    // TODO: Remove when we stop supporting v402 format.
    // Updates all bigram entries that are held by valid PtNodes. This removes useless bigram
    // entries.
    class TraversePolicyToUpdateBigramProbability
            : public DynamicPtReadingHelper::TraversingEventListener {
     public:
        TraversePolicyToUpdateBigramProbability(PtNodeWriter *const ptNodeWriter)
                : mPtNodeWriter(ptNodeWriter), mValidBigramEntryCount(0) {}

        bool onAscend() { return true; }

        bool onDescend(const int ptNodeArrayPos) { return true; }

        bool onReadingPtNodeArrayTail() { return true; }

        bool onVisitingPtNode(const PtNodeParams *const ptNodeParams);

        int getValidBigramEntryCount() const {
            return mValidBigramEntryCount;
        }

     private:
        DISALLOW_IMPLICIT_CONSTRUCTORS(TraversePolicyToUpdateBigramProbability);

        PtNodeWriter *const mPtNodeWriter;
        int mValidBigramEntryCount;
    };

    class TraversePolicyToPlaceAndWriteValidPtNodesToBuffer
            : public DynamicPtReadingHelper::TraversingEventListener {
     public:
        TraversePolicyToPlaceAndWriteValidPtNodesToBuffer(
                PtNodeWriter *const ptNodeWriter, BufferWithExtendableBuffer *const bufferToWrite,
                PtNodeWriter::DictPositionRelocationMap *const dictPositionRelocationMap)
                : mPtNodeWriter(ptNodeWriter), mBufferToWrite(bufferToWrite),
                  mDictPositionRelocationMap(dictPositionRelocationMap), mValidPtNodeCount(0),
                  mPtNodeArraySizeFieldPos(NOT_A_DICT_POS) {};

        bool onAscend() { return true; }

        bool onDescend(const int ptNodeArrayPos);

        bool onReadingPtNodeArrayTail();

        bool onVisitingPtNode(const PtNodeParams *const ptNodeParams);

     private:
        DISALLOW_IMPLICIT_CONSTRUCTORS(TraversePolicyToPlaceAndWriteValidPtNodesToBuffer);

        PtNodeWriter *const mPtNodeWriter;
        BufferWithExtendableBuffer *const mBufferToWrite;
        PtNodeWriter::DictPositionRelocationMap *const mDictPositionRelocationMap;
        int mValidPtNodeCount;
        int mPtNodeArraySizeFieldPos;
    };

    class TraversePolicyToUpdateAllPositionFields
            : public DynamicPtReadingHelper::TraversingEventListener {
     public:
        TraversePolicyToUpdateAllPositionFields(PtNodeWriter *const ptNodeWriter,
                const PtNodeWriter::DictPositionRelocationMap *const dictPositionRelocationMap)
                : mPtNodeWriter(ptNodeWriter),
                  mDictPositionRelocationMap(dictPositionRelocationMap), mUnigramCount(0),
                  mBigramCount(0) {};

        bool onAscend() { return true; }

        bool onDescend(const int ptNodeArrayPos) { return true; }

        bool onReadingPtNodeArrayTail() { return true; }

        bool onVisitingPtNode(const PtNodeParams *const ptNodeParams);

        int getUnigramCount() const {
            return mUnigramCount;
        }

        int getBigramCount() const {
            return mBigramCount;
        }

     private:
        DISALLOW_IMPLICIT_CONSTRUCTORS(TraversePolicyToUpdateAllPositionFields);

        PtNodeWriter *const mPtNodeWriter;
        const PtNodeWriter::DictPositionRelocationMap *const mDictPositionRelocationMap;
        int mUnigramCount;
        int mBigramCount;
    };

 private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(DynamicPtGcEventListeners);
};
} // namespace latinime
#endif /* LATINIME_DYNAMIC_PT_GC_EVENT_LISTENERS_H */
