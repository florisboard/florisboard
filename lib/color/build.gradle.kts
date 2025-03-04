import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}


dependencies {
    implementation(libs.androidx.compose.material3)

    implementation(project(":lib:kotlin"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    compileKotlin {
        compilerOptions.jvmTarget = JvmTarget.JVM_1_8
        compilerOptions.freeCompilerArgs = listOf(
            "-opt-in=kotlin.contracts.ExperimentalContracts",
            "-Xjvm-default=all-compatibility",
        )
    }
    compileTestKotlin {
        compilerOptions.jvmTarget = JvmTarget.JVM_1_8
    }
}

sourceSets {
    maybeCreate("main").apply {
        java.srcDir("src/main/kotlin")
    }
}

