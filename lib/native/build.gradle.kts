plugins {
    alias(libs.plugins.agp.library)
    alias(libs.plugins.kotlin.android)
}

val projectMinSdk: String by project
val projectCompileSdk: String by project
val projectNdkVersion: String by project

android {
    namespace = "org.florisboard.libnative"
    compileSdk = projectCompileSdk.toInt()
    ndkVersion = projectNdkVersion

    defaultConfig {
        minSdk = projectMinSdk.toInt()

        externalNativeBuild {
            cmake {
                targets("fl_native")
                arguments(
                    "-DCMAKE_ANDROID_API=" + minSdk.toString(),
                )
            }
        }

        ndk {
            //abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
            )
        }
        create("beta") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
            )
        }

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
            java {
                srcDirs("src/main/kotlin")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path("src/main/rust/CMakeLists.txt")
        }
    }
}

tasks.named("clean") {
    doLast {
        delete("src/main/rust/target", "src/main/rust/Cargo.lock")
    }
}

dependencies {
    // none
}
