# Gemini Project Context: FlorisBoard

## Project Goal

This project is FlorisBoard, a free and open-source keyboard for Android. It is written in Kotlin and uses modern Android development technologies like Jetpack Compose. The project aims to be a privacy-respecting, customizable, and user-friendly keyboard alternative.

## Core Architecture

FlorisBoard is a multi-module Android application built with Kotlin and Jetpack Compose. Its architecture is centered around a core Input Method Engine (IME) and a settings UI. The application is highly modular, with features like theming, clipboard management, and extensions separated into distinct library modules. It also leverages native Rust code for performance-critical tasks.

## Key Modules & Directories

*   **`app`**: The main application module containing the core IME service, UI activities, and settings screens.
*   **`lib/snygg`**: A custom styling and theming engine for the keyboard and application UI.
*   **`lib/compose`**: Contains shared, custom Jetpack Compose UI components used throughout the app.
*   **`lib/kotlin`**: Provides core Kotlin utility and extension functions.
*   **`lib/native`**: Manages the JNI bridge to the native Rust code located in the `libnative` directory.
*   **`ime`**: The core Input Method Engine package, handling all keyboard-related logic like text input, gestures, and the smartbar.
*   **`clipboard`**: Manages clipboard history and related UI components.
*   **`nlp`**: Handles Natural Language Processing tasks like suggestions and spell-checking.

## Key Build Config

*   **`packageName`**: `dev.patrickgold.florisboard`
*   **Modules**: `:app`, `:lib:android`, `:lib:color`, `:lib:compose`, `:lib:kotlin`, `:lib:native`, `:lib:snygg`
*   **Key Dependencies**:
    *   `androidx.compose` (UI Toolkit)
    *   `androidx.room` (Database)
    *   `kotlinx.coroutines` (Concurrency)
    *   `kotlinx.serialization.json` (JSON processing)
    *   `patrickgold.jetpref` (Preferences)

## Main Entry Points

*   **Application Class**: `dev.patrickgold.florisboard.FlorisApplication`
*   **Main Activity**: `dev.patrickgold.florisboard.app.FlorisAppActivity`
*   **IME Service**: `dev.patrickgold.florisboard.FlorisImeService`
*   **Spell Checker Service**: `dev.patrickgold.florisboard.FlorisSpellCheckerService`