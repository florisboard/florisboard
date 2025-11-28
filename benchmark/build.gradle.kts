/*
 * Benchmark build configuration with safer defaults and properties for CI.
 */
plugins {
    alias(libs.plugins.agp.test)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "dev.patrickgold.florisboard.benchmark"
    compileSdk = 34

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    defaultConfig {
        minSdk = 24
        targetSdk = 34

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        sourceSets {
            maybeCreate("main").apply {
                java {
                    srcDirs("src/main/kotlin")
                }
            }
        }
    }

    buildTypes {
        create("benchmark") {
            isDebuggable = true
            signingConfig = getByName("debug").signingConfig
            matchingFallbacks += listOf("release")
        }
    }

    // For CI flexibility: allow overriding targetProjectPath via gradle properties
    targetProjectPath = findProperty("benchmark.targetProjectPath") as? String ?: ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
    implementation(libs.androidx.test.ext)
    implementation(libs.androidx.test.espresso.core)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro)
}

androidComponents {
    beforeVariants(selector().all()) {
        it.enable = it.buildType == "benchmark"
    }
}
