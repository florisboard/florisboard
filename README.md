# FlorisBoard

An open-source keyboard for Android. Currently in early-alpha/alpha
stage.

![Preview image](https://patrickgold.dev/media/previews/florisboard.png)

## Feature roadmap

### Basics
* [x] Implementation of the keyboard core (InputMethodService)
* [x] Own implementation of deprecated KeyboardView (base only)
* [x] Caps + Caps Lock
* [x] Key popups
* [x] Extended key popups (e.g. a -> á, à, ä, ...) (needs tweaks for
      emojis)
* [x] Key press sound/vibration
* [x] Portrait orientation support
* [x] Landscape orientation support (needs tweaks)
* [ ] Tablet screen support

### Layouts
* [x] Latin character layout (QWERTY)
* [ ] Other character layouts (both latin and non-latin)
* [x] Adapt to situation in app (password, url, text, etc. )
* [x] Special character layout(s)
* [x] Numeric layout
* [x] Numeric layout (advanced)
* [x] Phone number layout
* [x] Emoji layout (buggy atm)
* [ ] Emoticon layout

### Preferences
* [x] Preferences screen
* [x] Customize look and behaviour of keyboard (currently only
      light/dark theme)
* [ ] Theme customization
* [ ] Theme import/export (?)
* [ ] Layout selection
* [ ] Text suggestion / Auto correct preferences

### Composing suggestions
* [ ] Auto suggest words from precompiled dictionary
* [ ] Auto suggest words from user dictionary
* [ ] Auto suggest contacts
* [ ] Multilingual typing

### Other useful features
* [x] One-handed mode
* [ ] Clipboard manager (?)
* [ ] Floating keyboard
* [ ] Gesture support
* [ ] Glide typing (?)
* [ ] Search emojis/emoticons by name within media context
* [ ] Full integration in IME service list of Android (xml/method)
* [ ] (dev only) Generate well-structured documentation of code
* [ ] ...

Note: (?) = not sure if it will be implemented

## Used libraries and icons
* [Google Flexbox Layout for Android](https://github.com/google/flexbox-layout)
  by [google](https://github.com/google)
* [Google Material icons](https://github.com/google/material-design-icons) by
  [google](https://github.com/google)
* [Moshi JSON library](https://github.com/square/moshi) by
  [square](https://github.com/square)

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
