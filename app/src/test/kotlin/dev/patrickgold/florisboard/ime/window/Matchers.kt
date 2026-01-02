/*
 * Copyright (C) 2026 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.window

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

fun ImeWindowSpec.shouldBeFixedNormal() = this
    .shouldBeInstanceOf<ImeWindowSpec.Fixed>()
    .also { fixedMode shouldBe ImeWindowMode.Fixed.NORMAL }

fun ImeWindowSpec.shouldBeFixedCompact() = this
    .shouldBeInstanceOf<ImeWindowSpec.Fixed>()
    .also { fixedMode shouldBe ImeWindowMode.Fixed.COMPACT }
