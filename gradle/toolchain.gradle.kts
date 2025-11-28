// Arquivo opcional: gradle/toolchain.gradle.kts
// Instrução de uso: aplicar no root build.gradle.kts com:
// apply(from = "gradle/toolchain.gradle.kts")
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Força kotlin/java toolchain para todos os módulos Kotlin/Android
kotlin {
    jvmToolchain(17)
}

subprojects {
    // Para projetos Android: forçar compatibilidade Java
    plugins.withId("com.android.application") {
        extensions.configure<com.android.build.gradle.BaseExtension>("android") {
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }
    plugins.withId("com.android.library") {
        extensions.configure<com.android.build.gradle.BaseExtension>("android") {
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }

    // Para projetos Kotlin/JVM puros:
    plugins.withId("org.jetbrains.kotlin.jvm") {
        (extensions.findByName("kotlin") as? org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension)?.jvmToolchain {
            (this as org.gradle.jvm.toolchain.JavaToolchainSpec).languageVersion.set(org.gradle.jvm.toolchain.JavaLanguageVersion.of(17))
        }
    }
}
