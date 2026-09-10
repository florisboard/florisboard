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

package dev.patrickgold.florisboard.ime.keyboard3

import org.k3lp.cldr.CldrVersion
import org.k3lp.lib.text.mutableK3MarkerPoolOf
import org.k3lp.model.K3Model
import org.k3lp.model.display.K3DisplayOptions
import org.k3lp.model.display.K3Displays
import org.k3lp.model.flick.K3Flicks
import org.k3lp.model.form.K3Forms
import org.k3lp.model.key.K3Keys
import org.k3lp.model.layer.K3LayersByForm
import org.k3lp.model.meta.K3Info
import org.k3lp.model.transform.K3Transforms
import org.k3lp.model.variable.K3Variables

val FlorisEmptyK3Model = K3Model(
    conformsTo = CldrVersion.CLDR_45,
    null,
    emptyList(),
    null,
    K3Info("empty", null, null, null, null),
    null,
    K3Displays(emptyMap(), emptyMap(), K3DisplayOptions(null)),
    K3Keys(emptyMap()),
    K3Flicks(emptyMap()),
    K3Forms(emptyMap()),
    K3LayersByForm(null, emptyList()),
    K3Variables.Empty,
    K3Transforms(emptyMap()),
    mutableK3MarkerPoolOf(),
)
