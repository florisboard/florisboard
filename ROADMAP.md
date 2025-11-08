# Project Roadmap

*This file details all current and future plans. It is the "source of truth" for all agents.*

## 1. Whisper API (In Progress)

* **Status:** Proof-of-concept (PoC) is implemented.
* **Next Steps:** Make the feature "shiny." Refine UI/UX, add error handling, and improve robustness.

### Whisper API Schematic

*This is the technical flow-chart for the Whisper integration.*

* **Trigger:** `KeyCode.VOICE_INPUT` in `KeyboardManager.kt`
* **Permissions:** Requests `android.permission.RECORD_AUDIO` via `KeyboardManager.kt`
* **Data Capture:** `Recorder` class in `Recorder.kt` captures audio
* **File Format:** Audio is saved as `.mp4` to the cache directory
* **Network Request:** `WhisperClient.kt` uses `OkHttp` to build and send the multipart request
* **Dependencies:** `OkHttp`
* **Response Handling:** `WhisperClient.kt` parses the JSON response
* **UI Update:** A `Toast` is shown with the transcribed text via `KeyboardManager.kt`

## 2. App Rebranding (To-Do)

* **Status:** Planned.
* **Goal:** A complete visual and textual rebrand.

## 3. Smart Bar Customization (Planned)

* **Status:** Planned.
* **Goal:** Implement new features and AI integrations into the Smart Bar.

## 4. New AI API Integrations (Planned)

* **Status:** Planned.
* **Goal:** Integrate other AI models for text generation, form filling, etc.