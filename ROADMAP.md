# Roadmap

This feature roadmap intents to provide transparency to what is planned to be added to FlorisBoard in the foreseeable future. Note that there are no ETAs for any version milestones down below, experience has shown these won't hold anyways.

Each major milestone has associated alpha/beta releases, so if you are interested in previewing features quicker, keep an eye out! Each major 0.x release has also patch releases after the initial major release, which will be published on both the stable and beta tracks.

## 0.4

**Main focus**: Getting the project back on track, see [this announcement](https://github.com/florisboard/florisboard/discussions/2314) for details. Note that this has also replaced the previous roadmap, however this step is necessary for getting the project back on track again.

This includes, but is not exclusive to:
- Fixing the most reported bugs/issues
- Merging in the Material You theme PR -> Adds Material You support (v0.4.0-alpha05)
- Merging in other external PRs as best as possible
- Reworking the Settings UI warning boxes and hiding any UI for features related to word suggestions until they are ready
- Remove existing glide/swipe typing (see 0.5 milestone)
- Improvements in clipboard / emoji functionality (v0.4.0-beta01/beta02)
- Prepare project to have native code implemented in [Rust](https://www.rust-lang.org/) (v0.4.0-beta02)
- - Upgrade Settings UI to Material 3 (v0.4.0-beta03)
- Add support for importing extensions via system file handler APIs (relevant for Addons store) (v0.4.0-beta03)

Note that the previous versioning scheme has been dropped in favor of using a major.minor.patch versioning scheme, so versions like `0.3.16` are a thing of the past :)

## 0.5

- Implement predictive text support / spell checking
  - Consider adding proximity-based key typo detection
- Add new extension type: Language Pack
  - Basically groups all locale-relevant data (predictive base model, emoji suggestion data, ...)
    in a dynamically importable extension file
- New keyboard layout engine + file syntax based on the upcoming Unicode Keyboard v3 standard
  - RFC document with technical details will be released later
- New text processing logic (maybe moved back to 0.6)
  - RFC document with technical details will be released later
- Add Tablet mode / Optimizations for landscape input based on new keyboard layout engine
- Reimplementation of glide typing with the new layout engine and predictive text core
- Add support for any remaining new features introduced with Android 13

## 0.6

- Complete rework of the Emoji panel
  - Recently used / Emoji history (already implemented with 0.3.14)
  - Emoji search
  - Emoji suggestions when using :emoji_name: syntax (already implemented with v0.4.0-beta02)
  - Maybe: consider upgrading to emoji2 for better unified system-wide emoji styles
- Prepare FlorisBoard repository and app store presence for public beta release on Google Play (will go live with stable 0.6)
- Rework branding images and texts of FlorisBoard for the app stores
- Focus on stability and experience improvements of the app and keyboard
- Add support for new features introduced with Android 14
- Not finalized, but planned: raise minimum required Android version from Android 7 (SDK level 24) to Android 8 (SDK level 26)

## Backlog / Planned (unassigned)

**Features that MAY be added (even in versions mentioned above) or dismissed**

- Full on-board layout editor which allows users to create their own layouts without writing a JSON file
- Theme rework part II
- Adaptive themes v2
- Voice-to-text with Mozilla's open-source voice service (or any other oss voice provider)
- Text translation
- Floating keyboard
- Stickers/GIFs
- Kaomoji panel implementation
- FlorisBoard landing web page for presentation
- Implementing additional layouts
- Support for Tasker/Automate/MacroDroid plugins
- Support for WearOS/Smartwatches
- Handwriting
