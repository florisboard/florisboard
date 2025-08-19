# Roadmap

This feature roadmap intents to provide transparency to what is planned to be added to FlorisBoard in the foreseeable future. Note that there are no ETAs for any version milestones down below, experience has shown these won't hold anyway.

Each major milestone has associated alpha/beta releases, so if you are interested in previewing features quicker, keep an eye out! Each major 0.x release has also patch releases after the initial major release, which will be published on both the stable and preview tracks.

## 0.5 (currently in development)

> [!NOTE]
> The milestone 0.5 was split, thus the word suggestions now come with version 0.6. The old version 0.6 has been moved down and is now 0.7. The time it takes to implement word suggestions will not change, but we can now release the new theme editor earlier, which would otherwise lie dormant.

- [x] Theme rework part II / Snygg v2
  - [x] See https://github.com/florisboard/florisboard/pull/2855
  - [x] Spaces in URI bug (See https://github.com/florisboard/florisboard/issues/2898)
  - [x] Re-add time based theme switching (See https://github.com/florisboard/florisboard/pull/2977)
- [x] Add support for any remaining new features introduced with Android 13 / 14
- [x] Raise minimum required Android version from Android 7 (SDK level 24) to Android 8 (SDK level 26)

## 0.6

- [ ] Implement predictive text support / spell checking
- [ ] Add new extension type: Language Pack
    - Basically groups all locale-relevant data (predictive base model, emoji suggestion data, ...)
      in a dynamically importable extension file
- [ ] Proper physical keyboard support (See https://github.com/florisboard/florisboard/issues/1972)
- [ ] Rework cache manager (See https://github.com/florisboard/florisboard/issues/2870)

## k3lp

> [!NOTE]
> The development of k3lp is not tied to a florisboard version and takes place on [codeberg.org](https://codeberg.org/k3lp/k3lp) simultaneously.

- [ ] New keyboard layout engine + file syntax based on the upcoming Unicode Keyboard v3 standard
- [ ] Add Tablet mode / Optimizations for landscape input based on new keyboard layout engine

## 0.7+

> [!NOTE]
> From 0.6 onwards we plan to have more stable 0.X releases but with at most one large feature per release, thus having a much quicker iteration of new features on the stable track, which is a benefit for everyone involved.

- [ ] Add floating keyboard mode
- [ ] New text processing logic
- [ ] Complete rework of the Emoji panel
  - [ ] Emoji search
  - [ ] Fully scrollable emoji list (soft category borders)
  - [ ] Side scrollable emoji list (swipe for next category)
  - [ ] More granular theming options
  - [ ] Layout customization (e.g. placement of category buttons)
  - [ ] Maybe: consider upgrading to emoji2 for better unified system-wide emoji styles
- [ ] Reimplementation of glide typing with the new layout engine and predictive text core
- [ ] Prepare FlorisBoard repository and app store presence for public beta release on Google Play (will go live with stable 0.7)
- [ ] Rework branding images and texts of FlorisBoard for the app stores
- [ ] Focus on stability and experience improvements of the app and keyboard
- [ ] Add support for new features introduced with Android 15 / 16

## Backlog / Planned (unassigned)

**Features that MAY be added (even in versions mentioned above) or dismissed**

- Full on-board layout editor which allows users to create their own layouts without writing a JSON file
- Voice-to-text with Mozilla's open-source voice service (or any other oss voice provider)
- Text translation
- Stickers/GIFs
- Kaomoji panel implementation
- Implementing additional layouts
- Support for Tasker/Automate/MacroDroid plugins
- Support for WearOS/Smartwatches
- Handwriting
