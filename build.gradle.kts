import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    id("com.github.ben-manes.versions") version "0.36.0"
    base // adds clean task to root project
}

subprojects {
    repositories {
        mavenCentral()
        google()
        jcenter()
    }
}

// for dependency updates task, only display versions that aren't tagged with alpha or beta
tasks.named<DependencyUpdatesTask>("dependencyUpdates").configure {
    rejectVersionIf {
        "alpha" in candidate.version || "beta" in candidate.version
    }
}
