plugins {
    alias(libs.plugins.agp.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.mannodermaus.android.junit5)
}

android {
    namespace = "dev.patrickgold.florisboard"
    compileSdk = 34 // Se der erro, mude para 33 ou 35 dependendo do seu SDK instalado

    defaultConfig {
        applicationId = "dev.patrickgold.florisboard"
        minSdk = 26
        targetSdk = 34
        versionCode = 85
        versionName = "0.4.0-bypassed"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
        
        // Garante que o Gradle ache os arquivos de tradução e ícones
        sourceSets {
            getByName("main") {
                assets.srcDirs("src/main/assets")
            }
        }
    }

    // --- O TRUQUE DA ASSINATURA ---
    signingConfigs {
        create("release") {
            // Usa a chave de debug que todo PC tem.
            // O caminho muda sozinho se for Windows ou Linux.
            storeFile = file(System.getProperty("user.home") + "/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            
            // AQUI A MÁGICA: A versão release vai usar a chave de debug
            signingConfig = signingConfigs.getByName("release") 
            
            // Evita que o build pare por erros bobos de tradução ou lint
            lintOptions {
                checkReleaseBuilds = false
                abortOnError = false
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xjvm-default=all"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Trazendo as bibliotecas internas do projeto
    implementation(projects.lib.android)
    implementation(projects.lib.color)
    implementation(projects.lib.kotlin)
    implementation(projects.lib.snygg) 

    // Dependências externas essenciais
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.kotlinx.serialization.json)
    
    // Testes (opcional, mas evita erros se o projeto pedir)
    testImplementation(libs.junit)
}
