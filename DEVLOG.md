### 2025-11-07 (Fix)
* **Task:** [Fixed a build failure caused by incorrect icon resource references in `AndroidManifest.xml`.]
* **Action:** [Moved the new icon to the `res/drawable` directory and updated all `android:icon` and `android:roundIcon` attributes in `AndroidManifest.xml` to point directly to the new `@drawable/omni_icon` resource. This bypasses the adaptive icon system that was causing resource linking errors.]
* **Files:** `[app/src/main/AndroidManifest.xml]`, `[app/src/main/res/drawable/omni_icon.png]`