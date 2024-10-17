# Roadmap

This feature roadmap intents to provide transparency to what is planned to be added to FlorisBoard in the foreseeable future. Note that there are no ETAs for any version milestones down below, experience has shown these won't hold anyways.

Each major milestone has associated alpha/beta releases, so if you are interested in previewing features quicker, keep an eye out! Each major 0.x release has also patch releases after the initial major release, which will be published on both the stable and preview tracks.

## 0.5

- Implement predictive text support / spell checking
  - Consider adding proximity-based key typo detection
- Add new extension type: Language Pack
  - Basically groups all locale-relevant data (predictive base model, emoji suggestion data, ...)
    in a dynamically importable extension file
- New text processing logic (maybe moved back / split to 0.6)
- Add floating keyboard mode
- New keyboard layout engine + file syntax based on the upcoming Unicode Keyboard v3 standard
- Add Tablet mode / Optimizations for landscape input based on new keyboard layout engine
- Add support for any remaining new features introduced with Android 13

## 0.6

- Complete rework of the Emoji panel
  - Emoji search
  - Fully scrollable emoji list (soft category borders)
  - More granular themeing options
  - Layout customization (e.g. placement of category buttons)
  - Maybe: consider upgrading to emoji2 for better unified system-wide emoji styles
- Reimplementation of glide typing with the new layout engine and predictive text core
- Prepare FlorisBoard repository and app store presence for public beta release on Google Play (will go live with stable 0.6)
- Rework branding images and texts of FlorisBoard for the app stores
- Focus on stability and experience improvements of the app and keyboard
- Add support for new features introduced with Android 14 / 15
- Not finalized, but planned: raise minimum required Android version from Android 7 (SDK level 24) to Android 8 (SDK level 26)

## Backlog / Planned (unassigned)

**Features that MAY be added (even in versions mentioned above) or dismissed**

- Full on-board layout editor which allows users to create their own layouts without writing a JSON file
- Theme rework part II
- Adaptive themes v2
- Voice-to-text with Mozilla's open-source voice service (or any other oss voice provider)
- Text translation
- Stickers/GIFs
- Kaomoji panel implementation
- FlorisBoard landing web page for presentation
- Implementing additional layouts
- Support for Tasker/Automate/MacroDroid plugins
- Support for WearOS/Smartwatches
- Handwriting
