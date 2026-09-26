
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/*
 * Copyright (C) 2025-2026 The FlorisBoard Contributors
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

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.agp.multiplatform.library)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlinx.kover)
}

kotlin {
    android {
        namespace = "org.florisboard.lib.snygg"
        compileSdk = providers.gradleProperty("projectCompileSdk").get().toInt()
        minSdk = providers.gradleProperty("projectMinSdk").get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }

        withHostTestBuilder {}.configure {}
    }
    compilerOptions {
        freeCompilerArgs.set(listOf(
            "-Xconsistent-data-class-copy-visibility",
        ))
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.lib.kotlin)

            api(libs.material.kolor)

            implementation(libs.compose.multiplatform.runtime)
            implementation(libs.compose.multiplatform.foundation)
            implementation(libs.compose.multiplatform.material3)
            // TODO: remove and replace with imageVector or XML asset.
            //  This is only used in SnygIcon preview.
            implementation(libs.compose.multiplatform.material.icons.extended)

            implementation(libs.coil.compose)
            implementation(libs.coil.gif)
            implementation(libs.kotlinx.serialization.json)
        }

        androidMain.dependencies {
            implementation(projects.lib.android)

            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.compose.ui.tooling.preview)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test.junit5)
        }
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

tasks.register<JavaExec>("generateJsonSchema") {
    description = "Generate the JSON schema for Snygg themes"
    group = "Generation"
    dependsOn("compileAndroidMain")
    mainClass.set("org.florisboard.lib.snygg.SnyggJsonSchemaGenerator")
    val debugRuntime = configurations.named("androidRuntimeClasspath")
    val compileTask = tasks.named<KotlinCompile>("compileAndroidMain")
    val debugRuntimeArtifactView = debugRuntime.get().incoming.artifactView {
        attributes { attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "android-classes") }
    }
    classpath = files(
        compileTask.map { it.destinationDirectory },
        debugRuntimeArtifactView.files
    )
    args = listOf("schemas/stylesheet.schema.json")
    workingDir = projectDir
    standardOutput = System.out
}

tasks.matching { it.name == "compileAndroidMain" }.configureEach {
    finalizedBy("generateJsonSchema")
}
