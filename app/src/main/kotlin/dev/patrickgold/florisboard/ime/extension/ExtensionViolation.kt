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

package dev.patrickgold.florisboard.ime.extension

import dev.patrickgold.florisboard.ime.io.FlorisRef
import org.k3lp.lib.meta.report.Report
import org.k3lp.lib.meta.report.ReportLevel
import org.k3lp.lib.meta.source.SourceFileRef
import org.k3lp.lib.meta.source.SourcePosition
import org.k3lp.lib.meta.source.SourceRange
import kotlin.reflect.KClass

sealed interface ExtensionViolation : Report {
    override val cause: Throwable?
        get() = null
}

sealed interface ExtensionIndexViolation : ExtensionViolation {
    override val level: ReportLevel
        get() = ReportLevel.ERROR

    data class UnexpectedError(
        override val cause: Throwable,
        override val sourceRange: SourceRange,
    ) : ExtensionIndexViolation {
        override val message: String
            get() = "Unexpected fatal error: ${cause.message}"
    }

    data class DuplicateExtension(
        val extRefA: SourceFileRef,
        val extRefB: SourceFileRef,
        override val sourceRange: SourceRange,
    ) : ExtensionIndexViolation {
        override val message: String
            get() = "duplicate"
    }

    data class IdMismatch(
        val idInDirName: String,
        val idInManifest: String,
        override val sourceRange: SourceRange,
    ) : ExtensionIndexViolation {
        override val message: String
            get() = "Id mismatch"
    }

    data class UnknownManifest(
        val klass: KClass<*>,
        override val sourceRange: SourceRange,
    ) : ExtensionIndexViolation {
        override val message: String
            get() = "Id mismatch"
    }
}

private val NoSourcePosition = SourcePosition(-1, -1, -1)

fun FlorisRef.asSourceRange(): SourceRange {
    return SourceRange(this, NoSourcePosition)
}
