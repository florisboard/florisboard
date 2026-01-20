# Phase 3 — AI Integration Roadmap 🤖

## 🏗️ Architecture & Setup
- [x] **Design AI service architecture**: Robust, asynchronous provider system.
- [x] **Choose AI provider**: Gemini AI (Vertex AI SDK) for high-quality cloud processing.
- [x] **API Key Configuration**: Secure storage in preferences.
- [x] **`AIService.kt`**: Base interface for all AI operations.
- [x] **Network Logic**: Resilient handling (Gemini SDK).
- **Architect Note**: *Must implement a Circuit Breaker pattern. If the AI service is slow or down, the keyboard must remain 100% functional without AI.*

## 💬 Reply Suggestions
- [x] **`ReplySuggestionProvider.kt`**: Fetches and parses context-aware replies.
- [x] **Context Awareness**: Basic context support implemented.
- [ ] **Language Templates**: Specific templates for Telugu, Teluglish, and English.
- [x] **Smartbar Integration**: Non-intrusive UI updates using StateFlow.
- [x] **Error/Loading States**: Loading spinner and error handling.
- **Architect Note**: *Extraction of app context (what the user is replying to) must be done carefully to avoid SecurityExceptions on Android 12+.*

## ✍️ Text Rewrite
- [x] **`TextRewriteService.kt`**: Logic for modifying existing text (Implemented in `GeminiAIService`).
- [x] **Modes**: Implement `Formal`, `Casual`, `Shorter`, `Longer`.
- [x] **UI Overlay**: Selection-based action menu in the Smartbar.
- [x] **Multi-language**: Basic support via system instructions.
- **Architect Note**: *The "Replace" operation must be transactional to prevent text duplication or loss.*

## 🎭 Tone Adjustment
- [x] **`ToneAdjustmentService.kt`**: Personality-based modifications (Implemented in `GeminiAIService`).
- [x] **Tone Options**: `Professional`, `Friendly`, `Polite`, `Direct`.
- [x] **Tone Selector UI**: Quick-access chips in the AI panel.
- **Architect Note**: *Tone adjustment should be a "preview-first" feature where users can see the change before it replaces their text.*

## ⚙️ Settings & UI
- [x] **AI Settings Screen**: Dedicated section in FlorisBoard (Integrated into Typing screen).
- [x] **API Key Input**: masked input field with validation.
- [x] **Toggles**: Fine-grained control for each AI feature.
- [x] **Privacy Disclaimer**: Clear explanation of what data leaves the device.
- **Architect Note**: *Privacy is #1 for keyboards. AI features must be Opt-In.*

---

## 🛠️ Performance & QA Strategy
1. **ANR Prevention**: All AI calls strictly on `Dispatchers.IO`.
2. **Memory Leaks**: ViewModels/Services must be lifecycle-aware.
3. **Ghost Suggestions**: Cancel pending AI jobs the moment the user types a new character.
4. **Battery**: No background AI sync; only trigger on user action.
