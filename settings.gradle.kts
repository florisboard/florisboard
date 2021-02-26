rootProject.name = "FlorisBoard"

include(":app")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }

    // allows the plugins syntax to be used with the android gradle plugin
    resolutionStrategy.eachPlugin {
        if (requested.id.id == "com.android.application") {
            useModule("com.android.tools.build:gradle:${requested.version}")
        }
    }
}
