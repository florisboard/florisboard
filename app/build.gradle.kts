/*
 * Copyright (C) 2022-2025 The FlorisBoard Contributors
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

import java.io.ByteArrayOutputStream

plugins {
    alias(libs.plugins.agp.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.mikepenz.aboutlibraries)
}

val projectMinSdk: String by project
val projectTargetSdk: String by project
val projectCompileSdk: String by project
val projectBuildToolsVersion: String by project
val projectNdkVersion: String by project
val projectVersionCode: String by project
val projectVersionName: String by project
val projectVersionNameSuffix = projectVersionName.substringAfter("-", "").let { suffix ->
    if (suffix.isNotEmpty()) {
        "-$suffix"
    } else {
        suffix
    }
}

android {
    namespace = "dev.patrickgold.florisboard"
    compileSdk = projectCompileSdk.toInt()
    buildToolsVersion = projectBuildToolsVersion
    ndkVersion = projectNdkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf(
            "-opt-in=kotlin.contracts.ExperimentalContracts",
            "-Xjvm-default=all-compatibility",
            "-Xwhen-guards",
        )
    }

    defaultConfig {
        applicationId = "dev.patrickgold.florisboard"
        minSdk = projectMinSdk.toInt()
        targetSdk = projectTargetSdk.toInt()
        versionCode = projectVersionCode.toInt()
        versionName = projectVersionName.substringBefore("-")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BUILD_COMMIT_HASH", "\"${getGitCommitHash()}\"")
        buildConfigField("String", "FLADDONS_API_VERSION", "\"v~draft2\"")
        buildConfigField("String", "FLADDONS_STORE_URL", "\"beta.addons.florisboard.org\"")

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
            arg("room.expandProjection", "true")
        }

        sourceSets {
            maybeCreate("main").apply {
                assets {
                    srcDirs("src/main/assets")
                }
                java {
                    srcDirs("src/main/kotlin")
                }
            }
        }
    }

    bundle {
        language {
            // We disable language split because FlorisBoard does not use
            // runtime Google Play Service APIs and thus cannot dynamically
            // request to download the language resources for a specific locale.
            enableSplit = false
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    buildTypes {
        named("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug+${getGitCommitHash(short = true)}"

            isDebuggable = true
            isJniDebuggable = false

            resValue("mipmap", "floris_app_icon", "@mipmap/ic_app_icon_debug")
            resValue("mipmap", "floris_app_icon_round", "@mipmap/ic_app_icon_debug_round")
            resValue("drawable", "floris_app_icon_foreground", "@drawable/ic_app_icon_debug_foreground")
            resValue("string", "floris_app_name", "FlorisBoard Debug")
        }

        create("beta") {
            applicationIdSuffix = ".beta"
            versionNameSuffix = projectVersionNameSuffix

            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            isMinifyEnabled = true
            isShrinkResources = true

            resValue("mipmap", "floris_app_icon", "@mipmap/ic_app_icon_beta")
            resValue("mipmap", "floris_app_icon_round", "@mipmap/ic_app_icon_beta_round")
            resValue("drawable", "floris_app_icon_foreground", "@drawable/ic_app_icon_beta_foreground")
            resValue("string", "floris_app_name", "FlorisBoard Beta")
        }

        named("release") {
            versionNameSuffix = projectVersionNameSuffix

            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            isMinifyEnabled = true
            isShrinkResources = true

            resValue("mipmap", "floris_app_icon", "@mipmap/ic_app_icon_stable")
            resValue("mipmap", "floris_app_icon_round", "@mipmap/ic_app_icon_stable_round")
            resValue("drawable", "floris_app_icon_foreground", "@drawable/ic_app_icon_stable_foreground")
            resValue("string", "floris_app_name", "@string/app_name")
        }

        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    aboutLibraries {
        configPath = "app/src/main/config"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.autofill)
    implementation(libs.androidx.collection.ktx)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.emoji2)
    implementation(libs.androidx.emoji2.views)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.material.icons)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.profileinstaller)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.runtime)
    implementation(libs.cache4k)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.mikepenz.aboutlibraries.core)
    implementation(libs.mikepenz.aboutlibraries.compose)
    implementation(libs.patrickgold.compose.tooltip)
    implementation(libs.patrickgold.jetpref.datastore.model)
    implementation(libs.patrickgold.jetpref.datastore.ui)
    implementation(libs.patrickgold.jetpref.material.ui)

    implementation(project(":lib:android"))
    implementation(project(":lib:color"))
    implementation(project(":lib:kotlin"))
    implementation(project(":lib:native"))
    implementation(project(":lib:snygg"))

    testImplementation(libs.kotlin.test.junit5)
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.espresso.core)
}

fun getGitCommitHash(short: Boolean = false): String {
    if (!File(".git").exists()) {
        return "null"
    }

    val stdout = ByteArrayOutputStream()
    exec {
        if (short) {
            commandLine("git", "rev-parse", "--short", "HEAD")
        } else {
            commandLine("git", "rev-parse", "HEAD")
        }
        standardOutput = stdout
    }
    return stdout.toString().trim()
}
