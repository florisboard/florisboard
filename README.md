<img align="left" width="80" height="80"
src="fastlane/metadata/android/en-US/images/icon.png" alt="App icon">

# FlorisBoard [![Release](https://img.shields.io/github/v/release/florisboard/florisboard)](https://github.com/florisboard/florisboard/releases) [![Crowdin](https://badges.crowdin.net/florisboard/localized.svg)](https://crowdin.florisboard.patrickgold.dev) ![FlorisBoard CI](https://github.com/florisboard/florisboard/workflows/FlorisBoard%20CI/badge.svg?event=push)

**FlorisBoard** is a free and open-source keyboard for Android 6.0+
devices. It aims at being modern, user-friendly and customizable while
fully respecting your privacy. Currently in alpha/early-beta state.

## Public Alpha Test Programme
Wanna try it out on your device? Use one of the following options:

_A. Get it on F-Droid_:

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" height="64" alt="F-Droid badge">](https://f-droid.org/packages/dev.patrickgold.florisboard)

_B. Google Play Public Alpha Test_:

You can join the public alpha test programme on Google Play. To become a
tester, follow these steps:
1. Join the
   [FlorisBoard Public Alpha Test](https://groups.google.com/g/florisboard-public-alpha-test)
   Google Group to be able to access the testing programme.
2. Go to the
   [FlorisBoard Testing Page](https://play.google.com/apps/testing/dev.patrickgold.florisboard),
   then click "Become a tester". Now you are enrolled in the testing
   programme.
3. To try out FlorisBoard, download it via Google Play. To do so, click
   on "Download it on Google Play", which takes you to the [PlayStore
   listing](https://play.google.com/store/apps/details?id=dev.patrickgold.florisboard).
4. Finished! You will receive future versions of FlorisBoard via Google
   Play.

With the v0.4.0 release FlorisBoard will enter the public beta in GPlay, allowing to directly search
for and download FlorisBoard without prior joining the alpha group.

_C. Use the APK provided in the release section of this repo_

### Giving feedback
If you want to give feedback to FlorisBoard, there are several ways to
do so, as listed [here](CONTRIBUTING.md#giving-general-feedback).

---

<img align="right" height="256"
src="https://patrickgold.dev/media/previews/florisboard-preview-day.png"
alt="Preview image">

## Implemented features
This list contains all implemented and fully functional features
FlorisBoard currently has to offer. For planned features and its
milestones, please refer to the [Feature roadmap](#feature-roadmap).

### Basics
* [x] Implementation of the keyboard core (InputMethodService)
* [x] Custom implementation of deprecated KeyboardView (base only)
* [x] Caps + Caps Lock
* [x] Key popups
* [x] Extended key popups (e.g. a -> á, à, ä, ...)
* [x] Key press sound/vibration
* [x] Portrait orientation support
* [x] Landscape orientation support (needs tweaks)

### Layouts
* [x] Latin character layouts (QWERTY, QWERTZ, AZERTY, Swiss, Spanish,
      Norwegian, Swedish/Finnish, Icelandic, Danish, Hungarian,
      Croatian, Polish, Romanian, Colemak, Dvorak); more coming in future versions
* [x] Non-latin character layouts (Arabic, Persian, Greek, Russian
      (JCUKEN))
* [x] Adapt to situation in app (password, url, text, etc. )
* [x] Special character layout(s)
* [x] Numeric layout
* [x] Numeric layout (advanced)
* [x] Phone number layout
* [x] Emoji layout
* [x] Emoticon layout

### Preferences
* [x] Setup wizard
* [x] Preferences screen
* [x] Customize look and behaviour of keyboard
* [x] Theme presets (currently only day/night theme + borderless)
* [x] Theme customization
* [x] Subtype selection (language/layout)
* [x] Keyboard behaviour preferences
* [x] Gesture preferences

### Other useful features
* [x] One-handed mode
* [x] Clipboard/cursor tools
* [x] Integrated number row / symbols in character layouts
* [x] Gesture support
* [x] Full integration in IME service list of Android (xml/method)
      (integration is internal-only, because Android's default subtype
      implementation not really allows for dynamic language/layout
      pairs, only compile-time defined ones)
* [ ] Description and settings reference in System Language & Input
* [ ] (dev only) Generate well-structured documentation of code
* [ ] ...

## Feature roadmap
This section describes the features which are planned to be implemented
in FlorisBoard for the next major versions, modularized into sections.
Please note that the milestone due dates are only raw estimates and will
most likely be delayed back, even though I'm eager to stick to these as
close as possible.

### [v0.4.0](https://github.com/florisboard/florisboard/milestone/4)
- Module A: Smartbar rework (Implemented with [#91])
  - Ability to enable/disable Smartbar (features below thus only work if
    Smartbar is enabled)
  - Dynamic switching between clipboard tools and word suggestions
  - Ability to show both the number row and word suggestions at once
  - Better icons in quick actions
  - Complete rework of the Smartbar code base and the Smartbar layout
    definition in XML

- Module B: Composing suggestions (Phase 1: [#329])
  - Auto-suggestion of words based of precompiled dictionaries
  - Management of custom dictionary entries
  - Next-word suggestions by training language models. Data collected here is stored locally and never leaves
    the user's device.

- Module C: Extension packs (base implementation with [#162])
  - Ability to load dictionaries (and later potentially other cool
    features too) only if needed to keep the core APK size small
  - Currently unclear how exactly this will work, but this is definitely
    a must-have feature

- Module D: Glide typing
  - Swiping over the characters will automatically convert this to a word
  - Possibly also add improvements based on the Flow keyboard

- Module E: Theme rework (Implemented with [#162])
  - Themes are now based on the Asset schema
  - Dynamic theme creation
  - Different theme modes (`Always day`, `Always night`, `Follow system`
    and `Follow time`)
  - Define a separate theme both for day and night theme
  - Adapt to app theme if possible

### [v0.5.0](https://github.com/florisboard/florisboard/milestone/5)
There's no exact roadmap yet but it is planned that the media part of
FlorisBoard (emojis, emoticons, kaomoji) gets a rework. Also as an extension
(requires v0.4.0/Module C) GIF support is planned.

### > v0.5.0
This is completely open as of now and will gather planned features as time
passes...

Backlog (currently not assigned to any milestone):

- Theme import/export
- Floating keyboard

[#91]: https://github.com/florisboard/florisboard/pull/91
[#162]: https://github.com/florisboard/florisboard/pull/162
[#329]: https://github.com/florisboard/florisboard/pull/329

## Contributing
Wanna contribute to FlorisBoard? That's great to hear! There are lots of
different ways to help out. Bug reporting, making pull requests,
translating FlorisBoard to make it more accessible, etc. For more
information see the ![contributing guidelines](CONTRIBUTING.md). Thank
you for your help!

## List of permissions FlorisBoard requests
Please refer to this [page](https://github.com/florisboard/florisboard/wiki/List-of-permissions-FlorisBoard-requests)
to get more information on this topic.

## Used libraries, components and icons
* [Google Flexbox Layout for Android](https://github.com/google/flexbox-layout)
  by [google](https://github.com/google)
* [Google Material icons](https://github.com/google/material-design-icons) by
  [google](https://github.com/google)
* [Moshi JSON library](https://github.com/square/moshi) by
  [square](https://github.com/square)
* [ColorPicker preference](https://github.com/jaredrummler/ColorPicker) by
  [Jared Rummler](https://github.com/jaredrummler)
* [Timber](https://github.com/JakeWharton/timber) by
  [JakeWharton](https://github.com/JakeWharton)
* [kotlin-result](https://github.com/michaelbull/kotlin-result) by
  [Michael Bull](https://github.com/michaelbull)
* [expandable-fab](https://github.com/nambicompany/expandable-fab) by
  [Nambi](https://github.com/nambicompany)

## Usage notes for included binary dictionary files
All binary dictionaries included within this project in
(this)[app/src/main/assets/ime/dict) asset folder are built from various
sources, as stated below.

### Source 1: [wordfreq library by LuminosoInsight](https://github.com/LuminosoInsight/wordfreq):
`wordfreq` is a repository which provides both a Python library and raw
data (the wordlists). Only the data has been extracted in order to build
binary dictionary files from it. `wordfreq`'s data is licensed under the
Creative Commons Attribution-ShareAlike 4.0 license
(https://creativecommons.org/licenses/by-sa/4.0/).

For further information on what wordfreq's data depends on, see
(https://github.com/LuminosoInsight/wordfreq#license).

## License
```
Copyright 2020 Patrick Goldinger

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
