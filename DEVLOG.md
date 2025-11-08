### 2025-11-07 (Fix)
* **Task:** [Fixed a build failure caused by incorrect icon resource references in `AndroidManifest.xml`.]
* **Action:** [Moved the new icon to the `res/drawable` directory and updated all `android:icon` and `android:roundIcon` attributes in `AndroidManifest.xml` to point directly to the new `@drawable/omni_icon` resource. This bypasses the adaptive icon system that was causing resource linking errors.]
* **Files:** `[app/src/main/AndroidManifest.xml]`, `[app/src/main/res/drawable/omni_icon.png]`
### 2025-11-08
* **Task:** Rolled back tracked files to the stable checkpoint `0ff5bb87` to revert breaking changes from the recent rebrand. A new branch `restore/whisper-green` was created for this state. Key documentation files (`DEVLOG.md`, `GEMINI.md`, `ROADMAP.md`) and the app icon were preserved from the latest version.
* **Files:** `[git]`
### 2025-11-08
* **Task:** Reset the `main` branch to the stable `restore/whisper-green` state to recover from rebranding issues. The original `main` branch was backed up to `backup/main-before-restore`.
* **Files:** `[git]`
