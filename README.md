# FlorisBoard

An open-source keyboard for Android. Currently in alpha stage.

#### Public Alpha Test Programme
Wanna try it out on your device? You can join the public alpha test
programme on Google Play. To become a tester, follow these steps:
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

##### Giving feedback
If you want to give feedback to FlorisBoard, there are 2 ways to do so,
as listed below:
- *General feedback:* use the private feedback to developer section on
  the PlayStore listing.
- *Bug reports or feature requests:* see the
  [contribution guidelines](CONTRIBUTING.md)

Thank you for contributing to FlorisBoard!

##### Note on F-Droid release
FlorisBoard is currently only available through Google Play, but it is
planned to also release it via F-Droid later on. There is no exact
timeline for this, but I aim for the 0.2.0 or 0.3.0 release.

---

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
* [x] Latin character layouts (QWERTY, QWERTZ, AZERTY, Swiss, Spanish,
      Norwegian, Swedish/Finnish, Icelandic, Danish)
* [x] Non-latin character layouts (Persian)
* [x] Adapt to situation in app (password, url, text, etc. )
* [x] Special character layout(s)
* [x] Numeric layout
* [x] Numeric layout (advanced)
* [x] Phone number layout
* [x] Emoji layout (popups buggy atm)
* [x] Emoticon layout
* [ ] Kaomoji layout

### Preferences
* [x] Setup wizard
* [x] Preferences screen
* [x] Customize look and behaviour of keyboard (currently only
      light/dark theme)
* [ ] Theme customization
* [ ] Theme import/export (?)
* [x] Subtype selection (language/layout)
* [x] Keyboard behaviour preferences
* [ ] Text suggestion / Auto correct preferences
* [ ] Gesture preferences

### Composing suggestions
* [ ] Auto suggest words from precompiled dictionary
* [ ] Auto suggest words from user dictionary
* [ ] Auto suggest contacts
* [ ] Multilingual typing

### Other useful features
* [x] One-handed mode
* [x] Clipboard/cursor tools
* [ ] Floating keyboard
* [ ] Gesture support
* [ ] Glide typing (?)
* [x] Full integration in IME service list of Android (xml/method)
      (integration is internal-only, because Android's default subtype
      implementation not really allows for dynamic language/layout
      pairs, only compile-time defined ones)
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
