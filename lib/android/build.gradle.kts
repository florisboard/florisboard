plugins {
    alias(libs.plugins.agp.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

val projectMinSdk: String by project
val projectCompileSdk: String by project

android {
    namespace = "org.florisboard.lib.android"
    compileSdk = projectCompileSdk.toInt()

    defaultConfig {
        minSdk = projectMinSdk.toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("beta") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf(
            "-opt-in=kotlin.contracts.ExperimentalContracts",
        )
    }
}

dependencies {
    implementation(project(":lib:kotlin"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.serialization.json)
}
