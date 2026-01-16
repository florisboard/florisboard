/*
 * Copyright (C) 2022-2025 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

rootProject.name = "FlorisBoard"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // Uncomment the following if testing snapshots from Maven Central
        // maven("https://central.sonatype.com/repository/maven-snapshots/")
        // Uncomment the following if testing snapshots from Maven Local
        // mavenLocal()
    }

    versionCatalogs {
        create("tools") {
            from(files("gradle/tools.versions.toml"))
        }
    }
}
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")
//include(":benchmark")
include(":lib:android")
include(":lib:color")
include(":lib:compose")
include(":lib:kotlin")
include(":lib:native")
include(":lib:snygg")
