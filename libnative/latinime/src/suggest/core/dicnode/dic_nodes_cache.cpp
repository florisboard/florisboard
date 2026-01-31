/*
 * Copyright (C) 2012 The Android Open Source Project
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

#include <list>

#include "defines.h"
#include "suggest/core/dicnode/dic_node_priority_queue.h"
#include "suggest/core/dicnode/dic_node_utils.h"
#include "suggest/core/dicnode/dic_nodes_cache.h"

namespace latinime {

// The biggest value among MAX_CACHE_DIC_NODE_SIZE, MAX_CACHE_DIC_NODE_SIZE_FOR_SINGLE_POINT, ...
const int DicNodesCache::LARGE_PRIORITY_QUEUE_CAPACITY = 310;
// Capacity for reducing memory footprint.
const int DicNodesCache::SMALL_PRIORITY_QUEUE_CAPACITY = 100;

}  // namespace latinime
