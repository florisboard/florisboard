plugins {
    id("com.android.application") version "4.1.2"
    kotlin("android") version "1.4.30"
}

android {
    compileSdkVersion(30)
    buildToolsVersion("30.0.3")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
        freeCompilerArgs = listOf("-Xallow-result-return-type") // enables use of kotlin.Result
    }

    defaultConfig {
        applicationId = "dev.patrickgold.florisboard"
        minSdkVersion(23)
        targetSdkVersion(30)
        versionCode(29)
        versionName("0.3.10")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        named("debug").configure {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"

            resValue("mipmap", "floris_app_icon", "@mipmap/ic_app_icon_debug")
            resValue("mipmap", "floris_app_icon_round", "@mipmap/ic_app_icon_debug_round")
            resValue("string", "floris_app_name", "FlorisBoard Debug")
        }

        create("beta") // Needed because by default the "beta" BuildType does not exist
        named("beta").configure {
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta01"
            proguardFiles.add(getDefaultProguardFile("proguard-android-optimize.txt"))

            resValue("mipmap", "floris_app_icon", "@mipmap/ic_app_icon_beta")
            resValue("mipmap", "floris_app_icon_round", "@mipmap/ic_app_icon_beta_round")
            resValue("string", "floris_app_name", "FlorisBoard Beta")
        }

        named("release").configure {
            proguardFiles.add(getDefaultProguardFile("proguard-android-optimize.txt"))

            resValue("mipmap", "floris_app_icon", "@mipmap/ic_app_icon_release")
            resValue("mipmap", "floris_app_icon_round", "@mipmap/ic_app_icon_release_round")
            resValue("string", "floris_app_name", "@string/app_name")
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    lintOptions {
        isAbortOnError = false
    }
}

dependencies {
    implementation("androidx.activity", "activity-ktx", "1.2.1")
    implementation("androidx.appcompat", "appcompat", "1.2.0")
    implementation("androidx.core", "core-ktx", "1.3.2")
    implementation("androidx.fragment", "fragment-ktx", "1.3.0")
    implementation("androidx.preference", "preference-ktx", "1.1.1")
    implementation("androidx.constraintlayout", "constraintlayout", "2.0.4")
    implementation("androidx.lifecycle", "lifecycle-service", "2.2.0")
    implementation("com.google.android", "flexbox", "2.0.1") // requires jcenter as of version 2.0.1
    implementation("com.squareup.moshi", "moshi-kotlin", "1.11.0")
    implementation("com.squareup.moshi", "moshi-adapters", "1.11.0")
    implementation("com.google.android.material", "material", "1.3.0")
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-android", "1.4.2")
    implementation("com.jaredrummler", "colorpicker", "1.1.0")
    implementation("com.jakewharton.timber", "timber", "4.7.1")
    implementation("com.nambimobile.widgets", "expandable-fab", "1.0.2")

    testImplementation("junit", "junit", "4.13.1")
    testImplementation("org.mockito", "mockito-inline", "3.7.7")
    testImplementation("org.robolectric", "robolectric", "4.5.1")
    androidTestImplementation("androidx.test.ext", "junit", "1.1.2")
    androidTestImplementation("androidx.test.espresso", "espresso-core", "3.3.0")
}
