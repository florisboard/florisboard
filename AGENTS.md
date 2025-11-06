# Project: FlorisBoard with Whisper API Integration

## Agent Protocol

**IMPORTANT:** All agents working on this project are required to document their plans, thought processes, and attempts after every change made to the code or pivots in plans. This is to ensure that all collaborators (human and AI) stay on the same page. This documentation should be added to this `AGENTS.md` file.

## 1. Project Overview

This project is a fork of FlorisBoard, an open-source Android keyboard. The primary goal is to replace the default voice input functionality with a direct integration of OpenAI's Whisper API for speech-to-text transcription.

This document serves as a guide for future agents to understand the project's context, the user's requests, the work that has been done, and the future steps.

## 2. User's Goal

The user wants to create a proof-of-concept (POC) that demonstrates the integration of the Whisper API into FlorisBoard. For this initial version, the user has specified that API key security is not a primary concern and that hardcoding the key is acceptable for now.

The user also requested a GitHub workflow to automate the build process and produce a downloadable APK artifact.

## 3. Initial Analysis (Repo Crawl)

Upon examining the repository, I made the following findings:

*   **Project Base:** The project is FlorisBoard, a keyboard for Android written in Kotlin.
*   **UI Framework:** The user interface is built with Jetpack Compose.
*   **Voice Input Implementation:** The existing voice input functionality is triggered by a `VOICE_INPUT` key code. This action is handled in `KeyboardManager.kt` and calls the `switchToVoiceInputMethod()` function in `FlorisImeService.kt`. This function's purpose is to switch to a separate, system-installed voice IME, not to handle voice input directly.
*   **Key Files:** The core files identified for this task were:
    *   `app/src/main/kotlin/dev/patrickgold/florisboard/FlorisImeService.kt`
    *   `app/src/main/kotlin/dev/patrickgold/florisboard/ime/keyboard/KeyboardManager.kt`
    *   `app/src/main/AndroidManifest.xml`
    *   `app/build.gradle.kts`
    *   `gradle/libs.versions.toml`

## 4. Implementation Plan

My plan to achieve the user's goal was as follows:

1.  **Add Permissions:** Add the `RECORD_AUDIO` and `INTERNET` permissions to the `AndroidManifest.xml` file to allow microphone access and network requests.
2.  **Integrate Networking Library:** Since the project did not have a networking library, I planned to add one to facilitate communication with the Whisper API.
3.  **Implement Voice Recording and Transcription:**
    *   Create new functions in `FlorisImeService.kt` to handle audio recording (`startWhisperVoiceInput`) and API calls (`stopAndTranscribe`).
    *   Use Android's `MediaRecorder` to capture audio and save it to a temporary file.
    *   Make a multipart POST request to the Whisper API with the audio file.
    *   Parse the JSON response to extract the transcribed text.
    *   Commit the transcribed text to the active input field.
4.  **Update Key Handling:** Modify `KeyboardManager.kt` to call the new voice input functions when the `VOICE_INPUT` key is pressed.
5.  **Create GitHub Workflow:** Create a new GitHub workflow file to build the app and upload the APK as an artifact.

## 5. Record of Changes

Here is a summary of the changes I have made:

*   **`.github/workflows/build.yml`:**
    *   **What:** Created a new GitHub workflow file.
    *   **Why:** To automate the build process and provide a downloadable APK, as requested by the user.

*   **`app/src/main/AndroidManifest.xml`:**
    *   **What:** Added `<uses-permission android:name="android.permission.RECORD_AUDIO" />` and `<uses-permission android:name="android.permission.INTERNET" />`.
    *   **Why:** To enable the app to record audio from the microphone and to make network requests to the Whisper API.

*   **`gradle/libs.versions.toml` & `app/build.gradle.kts`:**
    *   **What:** Added the `OkHttp` dependency.
    *   **Why:** To provide a library for making HTTP requests to the Whisper API, as no such library was previously included in the project.

*   **`app/src/main/kotlin/dev/patrickgold/florisboard/FlorisImeService.kt`:**
    *   **What:**
        *   Added a hardcoded `OPENAI_API_KEY` variable.
        *   Added a `MediaRecorder` instance and a `File` object for audio recording.
        *   Added an `isRecording` flag to track the recording state.
        *   Implemented the `startWhisperVoiceInput()` and `stopAndTranscribe()` functions.
        *   Changed the `OPENAI_API_KEY` variable to be loaded from `BuildConfig`.
        *   Corrected the class structure to fix a build error.
    *   **Why:** To encapsulate the logic for recording audio, sending it to the Whisper API, and handling the transcription response. The change to `BuildConfig` was made to improve security and make the API key configurable. The structural correction was to fix a build error caused by incorrect placement of the companion object.

*   **`app/src/main/kotlin/dev/patrickgold/florisboard/ime/keyboard/KeyboardManager.kt`:**
    *   **What:** Changed the action for `KeyCode.VOICE_INPUT` in the `onInputKeyUp` function to call `FlorisImeService.startWhisperVoiceInput()`.
    *   **Why:** To trigger the new voice input functionality when the microphone button is pressed.

*   **`.gitignore`:**
    *   **What:** Added `local.properties` to the `.gitignore` file.
    *   **Why:** To prevent the user's API key from being committed to the repository.

## 6. Future Work and Considerations

For a future agent, here are some things to consider to move this project from a POC to a more robust implementation:

*   **API Key Security:** The API key is now loaded from `BuildConfig`, which is a good practice. The user is responsible for creating the `local.properties` file with their key.
*   **UI/UX for Recording:** The current implementation uses toasts to indicate the recording status. A more user-friendly approach would be to provide visual feedback directly on the keyboard, such as changing the microphone icon's appearance.
*   **Error Handling:** The current error handling is minimal. More robust error handling should be added for network failures, API errors, and microphone access issues.
*   **State Management:** The `isRecording` flag is a simple way to manage the recording state. A more robust state management solution would be beneficial, especially if more complex features are added in the future.

## 7. Agent Activity Log

### 2025-11-01 – gpt-5-codex
* **Plan:** Restore the `FlorisImeService` companion helper methods so other modules can compile, reinstate the Whisper recording and transcription workflow, and clean up the temporary resources lifecycle.
* **Changes:** Re-added the missing imports, rebuilt the `companion object` with shared helpers and a `startWhisperVoiceInput()` bridge, rewrote the recording/transcription helpers with better error handling and cleanup, and ensured `onDestroy()` tears down any pending recorder or temp files.
* **Attempts:** `./gradlew compileDebugKotlin` *(fails: Android SDK is unavailable in the execution environment).* 

### 2025-11-01 (Gemini)
* **Plan:** Refactor the project to securely handle the OpenAI API key after a hardcoded key was blocked by GitHub's push protection. The new strategy uses a GitHub secret for CI/CD builds and the `local.properties` file for local development.
* **Changes:**
    *   Reverted the commit that hardcoded the API key.
    *   Modified `app/build.gradle.kts` to define a `buildConfigField` for `OPENAI_API_KEY`. This script now reads the key from a `OPENAI_API_KEY` environment variable or, if not found, falls back to the `local.properties` file.
    *   Updated `FlorisImeService.kt` to consume the API key from `BuildConfig.OPENAI_API_KEY`.
    *   Modified `.github/workflows/build.yml` to pass the `OPENAI_API_KEY` repository secret to the build step as an environment variable.
    *   Updated this `AGENTS.md` file to reflect the new, secure secret management strategy and remove the incorrect previous entry.
* **Result:** The changes were successfully pushed, and the build process is now secure and functional for both remote and local environments.

### 2025-11-01 – gpt-5-codex
* **Plan:** Instrument the Whisper integration to expose misconfiguration, switch recording to an OpenAI-supported container/codec, harden the multipart request, and extend network timeouts so transcription succeeds reliably on-device.
* **Changes:** Updated `FlorisImeService` to validate the API key at runtime, record AAC audio into an `.m4a` temp file, log and guard zero-byte captures, send multipart requests with explicit timeouts, and surface HTTP details plus parsed results back to the UI. Added helper utilities for recorder cleanup and client construction, along with verbose logging.
* **Attempts:** `./gradlew testDebugUnitTest` *(skipped: Android SDK/toolchain not available in this environment).* 

### 2025-11-02 – gpt-5-codex
* **Plan:** Ensure builds fail fast when the OpenAI secret is missing or malformed and accept Gradle properties so CI can inject the key reliably.
* **Changes:** Updated `app/build.gradle.kts` to read the key from a Gradle property before falling back to environment variables or `local.properties`, escaped the value before emitting it into `BuildConfig`, and added a `preBuild` guard task that rejects empty or malformed keys. Adjusted the CI workflow to pass the secret via `-POPENAI_API_KEY` so the new guard succeeds.
* **Attempts:** Not run (`./gradlew` tasks require the Android toolchain, which is unavailable in this environment).

### 2025-11-02 – gpt-5-codex
* **Plan:** Update the CI workflow to use supported GitHub Actions, gate the API key assertion to trusted contexts, and keep forked pull requests building without secrets.
* **Changes:** Switched the build workflow to `actions/upload-artifact@v4`, `actions/checkout@v4`, and `actions/setup-java@v4`, added fork-aware logic that only passes the OpenAI key on trusted runs while setting `ciEnforceOpenAiKey`, and taught `app/build.gradle.kts` to conditionally wire `assertOpenAIKey` via the new enforcement flag.
* **Attempts:** Not run (Android tooling unavailable in the execution environment).

### 2025-11-03 – gpt-5-codex
* **Plan:** Consolidate CI into a single Android workflow, gate key injection for trusted contexts, and surface runtime diagnostics for missing API keys.
* **Changes:** Removed the duplicate workflow, rewrote `android.yml` with fork-aware build/upload steps, taught `app/build.gradle.kts` to emit both the key string and a `HAS_OPENAI_KEY` flag while honoring `REQUIRE_OPENAI_KEY`, and updated `FlorisImeService` to log the key length, respect the new flag, and warn PR builds via toast.
* **Attempts:** Not run (`./gradlew` tasks require the Android toolchain, which is unavailable in this environment).

### 2025-11-03 – gpt-5-codex
* **Plan:** Stabilize the unified Android CI by provisioning the native toolchain and pinning the Gradle-side versions so forked PR builds stop failing when CMake 4.0.2 is missing on runners.
* **Changes:** Added Android SDK setup plus explicit CMake/NDK installation to `.github/workflows/android.yml`, taught `app/build.gradle.kts` to request the catalogued CMake version, and downgraded the catalog entry in `gradle/tools.versions.toml` to 3.31.1 so the workflow installs a matching toolchain.
* **Attempts:** Not run (Android tooling is unavailable in this execution environment).

### 2025-11-04 – gpt-5-codex
* **Plan:** Add an on-device Whisper diagnostics pipeline with rotating log storage, surface failures via toasts/notifications, and expose quick export/share paths so debugging works without ADB.
* **Changes:** Introduced `diagnostics/WhisperLogger` with masking, MediaStore export, and notification helpers; registered the broadcast receiver in the manifest; instrumented `FlorisImeService` with structured logging, failure notifications, toast snippets, and a manual export hook; wired a mic-key long-press to the exporter; and added the required UI strings.
* **Attempts:** Not run (`./gradlew` tasks require the Android SDK, which is unavailable in this environment).

### 2025-11-05 – gpt-5-codex
* **Plan:** Update Whisper log exporting so Android 8–9 devices can share logs without requesting `WRITE_EXTERNAL_STORAGE`, while preserving the existing MediaStore flow on Android 10+ and keeping notification actions functional.
* **Changes:** Added a dedicated FileProvider authority to the manifest and broadened `res/xml/file_paths.xml` to expose the app-scoped downloads directory; refactored `WhisperLogger` to choose between MediaStore and FileProvider exports, adjusted the broadcast receiver to consume the new API, and taught `FlorisImeService` to surface the right success message; added a helper string for pre-API 29 messaging.
* **Attempts:** No automated builds were run (Android tooling unavailable in this environment).

### 2025-11-02 – gpt-5-codex
* **Plan:** Freeze the current Whisper-working build, document the stability point, and inventory theming capabilities without modifying runtime code.
* **Changes:** Created the `v0.1-whisper-ok` annotated tag and `stable/whisper-ok` branch at commit `ee6c7a0`, appended a Stable Snapshots note plus protection checklist, and gathered theming reconnaissance for reporting.
* **Attempts:** `git push origin stable/whisper-ok` *(fails: repository has no `origin` remote in this workspace).*; `git push origin v0.1-whisper-ok` *(fails for the same reason).*

## Stable Snapshots

- **v0.1-whisper-ok** (`ee6c7a0`)
  - Branch: `stable/whisper-ok`
  - Latest with-key artifact run: https://github.com/yourowndog/keyboard/actions/runs/19015941667
  - Notes: Whisper mic capture/transcription, diagnostics logging & export flows verified.

### Protection Checklist

- [ ] Protect `stable/whisper-ok` on GitHub (no force-push, PRs required)
