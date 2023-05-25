/*
 * Copyright 2023 Patrick Goldinger
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

// Suppress needed until https://youtrack.jetbrains.com/issue/KTIJ-19369 is fixed
@file:Suppress("DSL_SCOPE_VIOLATION")

plugins {
    alias(libs.plugins.agp.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

val projectCompileSdk: String by project
val projectMinSdk: String by project
val projectVersionName: String by project
val projectVersionNameSuffix: String by project

android {
    namespace = "dev.patrickgold.florisboard.ime"
    compileSdk = projectCompileSdk.toInt()

    defaultConfig {
        minSdk = projectMinSdk.toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    sourceSets {
        maybeCreate("main").apply {
            java.srcDir("src/main/kotlin")
        }
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

/*val sourcesJar = tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(android.sourceSets.getByName("main").java.srcDirs)
}

mavenPublishing {
    coordinates(projectGroupId, artifactId, projectVersion)
}*/
