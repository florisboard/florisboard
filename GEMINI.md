# Gemini Project Context: OmniBoard

*This file contains the core project context, rules, and S.O.P.s for all agents.*

## Project Goal

This project is OmniBoard, a free and open-source keyboard for Android. It is written in Kotlin and uses modern Android development technologies like Jetpack Compose. The project aims to be a privacy-respecting, customizable, and user-friendly keyboard alternative.

## Core Architecture

OmniBoard is a multi-module Android application built with Kotlin and Jetpack Compose. Its architecture is centered around a core Input Method Engine (IME) and a settings UI. The application is highly modular, with features like theming, clipboard management, and extensions separated into distinct library modules. It also leverages native Rust code for performance-critical tasks.

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

*   **`packageName`**: `dev.silo.omniboard`
*   **Modules**: `:app`, `:lib:android`, `:lib:color`, `:lib:compose`, `:lib:kotlin`, `:lib:native`, `:lib:snygg`
*   **Key Dependencies**:
    *   `androidx.compose` (UI Toolkit)
    *   `androidx.room` (Database)
    *   `kotlinx.coroutines` (Concurrency)
    *   `kotlinx.serialization.json` (JSON processing)
    *   `patrickgold.jetpref` (Preferences)

## Main Entry Points

*   **Application Class**: `dev.silo.omniboard.OmniApplication`
*   **Main Activity**: `dev.silo.omniboard.app.OmniAppActivity`
*   **IME Service**: `dev.silo.omniboard.OmniImeService`
*   **Spell Checker Service**: `dev.silo.omniboard.OmniSpellCheckerService`

---

## Core Context Files

On starting **any** new session, you MUST read the following files in this order:

1.  **`ROADMAP.md`**: (Read the *entire* file to understand all project goals and feature schematics).
2.  **`DEVLOG.md`**: (To stay current, read only the **last 15 entries** from this file).

## Ground Rules (FOR ALL AGENTS)

* **Environment:** You are operating in a **Termux environment** on Android.
* **Builds:** **NEVER** attempt to run a build or compile (e.g., `gradlew build`). All builds are handled by a remote CI/CD workflow runner.
* **Token Safety (CRITICAL):**
    * **NEVER** use the `SearchText` agent. It causes token-explosions.
    * **NEVER** `cat` (print) multiple files at once.
    * To find text, **ALWAYS** use `rg -l 'term'` or `grep -rl 'term'` to get a *list of filenames first*. I will then tell you which files to read.
* **Whisper API Logic:** The Whisper API integration (see `ROADMAP.md`) is built in a specific way *by necessity*. Do not question its file formats or core logic; your job is to build upon it.

## Standard Operating Procedure (S.O.P.)

After you successfully complete *any* task (e.g., code modification, plan update), you will **autonomously append a summary to `DEVLOG.md`**.

**You will NOT prompt me for this.**

The entry must use this exact Markdown format:

### YYYY-MM-DD
* **Task:** [Your 1-sentence summary of the task]
* **Files:** `[file/path/one.kt]`, `[file/path/two.kt]`