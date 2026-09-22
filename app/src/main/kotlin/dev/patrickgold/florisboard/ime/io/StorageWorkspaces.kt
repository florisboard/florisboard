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

package dev.patrickgold.florisboard.ime.io

import kotlinx.io.files.FileNotFoundException
import kotlin.uuid.Uuid

private val WORKSPACES_BASE_REF = FlorisRef.cache("workspaces")

fun Storage.createWorkspace(): FlorisRef {
    val id = Uuid.random().toHexDashString()
    val ref = WORKSPACES_BASE_REF.subRef(id)
    mkdirs(ref)
    return ref
}

fun Storage.getWorkspaceRef(id: String): FlorisRef {
    val ref = WORKSPACES_BASE_REF.subRef(id)
    if (!exists(ref)) {
        throw FileNotFoundException("workspace with id $id not found")
    }
    return ref
}

fun Storage.deleteWorkspace(id: String) {
    val ref = getWorkspaceRef(id)
    deleteRecursively(ref)
}

fun Storage.deteleAllWorkspaces() {
    deleteContents(WORKSPACES_BASE_REF)
}
