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

### 2025-02-14 – gpt-5-codex
* **Plan:** Resolve the `OPENAI_API_KEY` unresolved reference by inlining the user-provided key for debugging, per the latest request.
* **Changes:** Removed the `BuildConfig` dependency and hardcoded the supplied key inside `FlorisImeService` so the project no longer requires a Gradle property for compilation.
* **Attempts:** Not run (Android tooling unavailable in the current environment).