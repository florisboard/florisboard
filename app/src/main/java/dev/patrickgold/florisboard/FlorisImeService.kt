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

package dev.patrickgold.florisboard

import dev.patrickgold.florisboard.ime.core.FlorisBoard

/**
 * This class only exists to prevent accidental IME deactivation after an update
 * of FlorisBoard to a new version when the location of the FlorisBoard class has
 * changed. The Android Framework uses the service class path as the IME id,
 * using this extension here makes sure it won't change ever again for the system.
 *
 * Important: DO NOT PUT ANY LOGIC INTO THIS CLASS. Make the necessary changes
 *  within the FlorisBoard class instead.
 */
class FlorisImeService : FlorisBoard()
