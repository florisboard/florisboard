/*
 * Copyright (C) 2022 Patrick Goldinger
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
    alias(libs.plugins.agp.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.mannodermaus.android.junit5)
    alias(libs.plugins.mikepenz.aboutlibraries)
}

android {
    namespace = "dev.patrickgold.florisboard"
    compileSdk = 31
    buildToolsVersion = "31.0.0"
    ndkVersion = "22.1.7171670"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
        freeCompilerArgs = listOf(
            "-Xallow-result-return-type",
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xopt-in=kotlin.contracts.ExperimentalContracts",
            "-Xjvm-default=compatibility",
        )
    }

    defaultConfig {
        applicationId = "dev.patrickgold.florisboard"
        minSdk = 24
        targetSdk = 31
        versionCode = 83
        versionName = "0.3.16"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
            arg("room.expandProjection", "true")
        }

        externalNativeBuild {
            cmake {
                cFlags("-fvisibility=hidden", "-DU_STATIC_IMPLEMENTATION=1")
                cppFlags("-fvisibility=hidden", "-std=c++17", "-fexceptions", "-ffunction-sections", "-fdata-sections", "-DU_DISABLE_RENAMING=1", "-DU_STATIC_IMPLEMENTATION=1")
                arguments("-DANDROID_STL=c++_static")
            }
        }

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }

        sourceSets {
            maybeCreate("main").apply {
                assets {
                    srcDirs("src/main/assets", "src/main/icu4c/prebuilt/assets")
                }
                jniLibs {
                    srcDirs("src/main/icu4c/prebuilt/jniLibs")
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
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.androidx.compose.get()
    }

    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
        }
    }

    buildTypes {
        named("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"

            isDebuggable = true
            isJniDebuggable = false

            ndk {
                // For running FlorisBoard on the emulator
                abiFilters += listOf("x86", "x86_64")
            }

            resValue("mipmap", "floris_app_icon", "@mipmap/ic_app_icon_debug")
            resValue("mipmap", "floris_app_icon_round", "@mipmap/ic_app_icon_debug_round")
            resValue("drawable", "floris_app_icon_foreground", "@drawable/ic_app_icon_debug_foreground")
            resValue("string", "floris_app_name", "FlorisBoard Debug")
        }

        create("beta") {
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta03"

            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            isMinifyEnabled = true
            isShrinkResources = true

            resValue("mipmap", "floris_app_icon", "@mipmap/ic_app_icon_beta")
            resValue("mipmap", "floris_app_icon_round", "@mipmap/ic_app_icon_beta_round")
            resValue("drawable", "floris_app_icon_foreground", "@drawable/ic_app_icon_beta_foreground")
            resValue("string", "floris_app_name", "FlorisBoard Beta")
        }

        named("release") {
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

            ndk {
                // For running FlorisBoard on the emulator
                abiFilters += listOf("x86", "x86_64")
            }
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
    implementation(libs.accompanist.flowlayout)
    implementation(libs.accompanist.insets)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.autofill)
    implementation(libs.androidx.collection.ktx)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.emoji2)
    implementation(libs.androidx.emoji2.views)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.profileinstaller)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.runtime)
    implementation(libs.cache4k)
    implementation(libs.jetpref.datastore.model)
    implementation(libs.jetpref.datastore.ui)
    implementation(libs.jetpref.material.ui)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.mikepenz.aboutlibraries.core)
    implementation(libs.mikepenz.aboutlibraries.compose)

    testImplementation(libs.equalsverifier)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.extensions.roboelectric)
    testImplementation(libs.kotest.property)
    testImplementation(libs.kotest.runner.junit5)

    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
