plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.android.gms.oss-licenses-plugin")
    id("de.mannodermaus.android-junit5")
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
        versionCode = 82
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
        kotlinCompilerExtensionVersion = "1.1.1"
    }

    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
        }
    }

    buildTypes {
        named("debug").configure {
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

        create("beta") // Needed because by default the "beta" BuildType does not exist
        named("beta").configure {
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta02"
            proguardFiles.add(getDefaultProguardFile("proguard-android-optimize.txt"))

            resValue("mipmap", "floris_app_icon", "@mipmap/ic_app_icon_beta")
            resValue("mipmap", "floris_app_icon_round", "@mipmap/ic_app_icon_beta_round")
            resValue("drawable", "floris_app_icon_foreground", "@drawable/ic_app_icon_beta_foreground")
            resValue("string", "floris_app_name", "FlorisBoard Beta")
        }

        named("release").configure {
            proguardFiles.add(getDefaultProguardFile("proguard-android-optimize.txt"))

            resValue("mipmap", "floris_app_icon", "@mipmap/ic_app_icon_stable")
            resValue("mipmap", "floris_app_icon_round", "@mipmap/ic_app_icon_stable_round")
            resValue("drawable", "floris_app_icon_foreground", "@drawable/ic_app_icon_stable_foreground")
            resValue("string", "floris_app_name", "@string/app_name")
        }
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
    implementation("androidx.activity:activity-compose:1.4.0")
    implementation("androidx.activity:activity-ktx:1.4.0")
    implementation("androidx.autofill:autofill:1.1.0")
    implementation("androidx.collection:collection-ktx:1.2.0")
    implementation("androidx.compose.material:material:1.1.1")
    implementation("androidx.compose.runtime:runtime-livedata:1.1.1")
    implementation("androidx.compose.ui:ui:1.1.1")
    implementation("androidx.compose.ui:ui-tooling-preview:1.1.1")
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.core:core-splashscreen:1.0.0-beta02")
    implementation("androidx.emoji2:emoji2:1.1.0")
    implementation("androidx.emoji2:emoji2-views:1.1.0")
    implementation("androidx.navigation:navigation-compose:2.4.2")
    implementation("com.google.accompanist:accompanist-flowlayout:0.23.1")
    implementation("com.google.accompanist:accompanist-insets:0.23.1")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.23.1")
    implementation("dev.patrickgold.jetpref:jetpref-datastore-model:0.1.0-beta08")
    implementation("dev.patrickgold.jetpref:jetpref-datastore-ui:0.1.0-beta08")
    implementation("dev.patrickgold.jetpref:jetpref-material-ui:0.1.0-beta08")
    implementation("io.github.reactivecircus.cache4k:cache4k:0.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    implementation("androidx.room:room-runtime:2.4.2")
    ksp("androidx.room:room-compiler:2.4.2")

    testImplementation("io.kotest:kotest-runner-junit5:5.2.3")
    testImplementation("io.kotest:kotest-assertions-core:5.2.3")
    testImplementation("io.kotest:kotest-property:5.2.3")
    testImplementation("io.kotest.extensions:kotest-extensions-robolectric:0.5.0")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:3.10")

    androidTestImplementation("androidx.test.ext", "junit", "1.1.2")
    androidTestImplementation("androidx.test.espresso", "espresso-core", "3.3.0")
}
