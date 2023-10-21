# Roadmap

This feature roadmap intents to provide transparency to what is planned to be added to FlorisBoard in the foreseeable future. Note that there are no ETAs for any version milestones down below, experience has shown these won't hold anyways.

Each major milestone has associated alpha/beta releases, so if you are interested in previewing features quicker, keep an eye out! Each major 0.x release has also patch releases after the initial major release, which will be published on both the stable and beta tracks.

## 0.4

**Main focus**: Getting the project back on track, see [TODO: insert discussion link!!](javascript:void(0)) for details. Note that this has also replaced the previous roadmap, however this step is necessary for getting the project back on track again.

This includes, but is not exclusive to:
- Fixing the most reported bugs/issues
- Merging in the Material You theme PR -> Adds Material You support (v0.4.0-alpha05)
- Merging in other external PRs as best as possible
- Reworking the Settings UI warning boxes and hiding any UI for features related to word suggestions until they are ready
- Remove existing glide/swipe typing (see 0.5 milestone)
- Fix compilation issues introduced with the 0.4 alphas as best as possible

Maybe in this release, but no guarantee and may be delayed to 0.5:
- Develop usable preview of an on-device statistical word suggestion algorithm (see the main [NLP project](https://github.com/florisboard/nlp) for details)
- Add experimental plugin system which supports communication with the aforementioned native suggestion algorithm
- Include precompiled dictionaries for major languages: English (US/UK), German, Spanish, French, Italian & Russian

Note that the previous versioning scheme has been dropped in favor of using a major.minor.patch versioning scheme, so versions like `0.3.16` are a thing of the past :)

## 0.5

- New text processing logic
  - RFC document with technical details will be released later
- New keyboard layout engine + file syntax based on the upcoming Unicode Keyboard v3 standard
  - RFC document with technical details will be released later
- Add Tablet mode / Optimizations for landscape input based on new keyboard layout engine
- Reimplementation of glide typing with the new layout engine and word suggestion core
- Add support for any remaining new features introduced with Android 13

## 0.6

- Complete rework of the Emoji panel
  - Recently used / Emoji history (already implemented with 0.3.14)
  - Emoji search
  - Emoji suggestions when using :emoji_name: syntax
  - Kaomoji panel implementation (the third tab which currently has "not yet implemented")
  - Maybe: consider upgrading to emoji2 for better unified system-wide emoji styles)
- Prepare FlorisBoard repository and app store presence for public beta release on Google Play (will go live with stable 0.6)
- Rework branding images and texts of FlorisBoard for the app stores
- Focus on stability and experience improvements of the app and keyboard
- Add support for new features introduced with Android 14
- Not finalized, but planned: raise minimum required Android version from Android 7 (SDK level 24) to Android 8 (SDK level 26)

## Backlog / Planned (unassigned)

**Features that MAY be added (even in versions mentioned above) or dismissed**

- Upgrade Settings UI to Material 3
- Full on-board layout editor which allows users to create their own layouts without writing a JSON file
- Import/Export of custom layout files packed in Flex extensions
- Theme rework part II
- Adaptive themes v2
- Voice-to-text with Mozilla's open-source voice service (or any other oss voice provider)
- Text translation
- Proximity-based key typo detection
- Floating keyboard
- Stickers/GIFs
- FlorisBoard landing web page for presentation
- Implementing additional layouts
- Support for Tasker/Automate/MacroDroid plugins
- Support for WearOS/Smartwatches
- Handwriting
