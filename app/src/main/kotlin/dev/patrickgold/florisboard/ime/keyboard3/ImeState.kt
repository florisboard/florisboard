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

import dev.patrickgold.florisboard.ime.keyboard.ImeStateFlags
import org.k3lp.model.K3Model
import org.k3lp.model.layer.K3LayerId
import org.k3lp.runtime.K3Content
import org.k3lp.runtime.K3InputMethodState

class ImeState(
    model: K3Model = FlorisEmptyK3Model,
    editor: ImeEditor = ImeEditor.Disconnected,
    content: K3Content = K3Content.Empty,
    touchLayerId: K3LayerId = ImeLayerIds.Base,
    val effRowCount: Int = 4,
    val flags: ImeStateFlags = ImeStateFlags(),
) : K3InputMethodState<ImeState, ImeEditor>(
    model, editor, content, touchLayerId,
) {
    override fun copy(
        model: K3Model,
        editor: ImeEditor,
        content: K3Content,
        touchLayerId: K3LayerId,
    ) = ImeState(model, editor, content, touchLayerId, this.effRowCount, this.flags)

    fun copy(
        model: K3Model = this.model,
        editor: ImeEditor = this.editor,
        content: K3Content = this.content,
        touchLayerId: K3LayerId = this.touchLayerId,
        effRowCount: Int = this.effRowCount,
        flags: ImeStateFlags = this.flags,
    ) = ImeState(model, editor, content, touchLayerId, effRowCount, flags)
}
