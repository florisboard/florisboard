/*
 * Copyright (C) 2024-2025 The FlorisBoard Contributors
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

import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.kover)
}

val projectGroupId: String by project
val artifactId = "florisboard-lib-kotlin"
val projectVersion: String by project

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

sourceSets {
    maybeCreate("main").apply {
        java.srcDir("src/main/kotlin")
    }
}

tasks {
    compileKotlin {
        compilerOptions.jvmTarget = JvmTarget.JVM_11
        compilerOptions.freeCompilerArgs = listOf(
            "-opt-in=kotlin.contracts.ExperimentalContracts",
            "-Xjvm-default=all-compatibility",
        )
    }
    compileTestKotlin {
        compilerOptions.jvmTarget = JvmTarget.JVM_11
    }
}

tasks.withType<Test> {
    testLogging {
        events = setOf(TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.SKIPPED)
    }
    useJUnitPlatform()
}

kover {
    useJacoco()
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines)

    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.kotlin.test.junit5)
}

