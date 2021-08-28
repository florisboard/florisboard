￼

FlorisBoard￼

￼

￼

FlorisBoard is a free and open-source keyboard for Android 6.0+ devices. It aims at being modern, user-friendly and customizable while fully respecting your privacy. Currently in early-beta state.

Stable￼

Releases on this track are in general stable and ready for everyday use, except for features marked as experimental. Use one of the following options to receive FlorisBoard's stable releases:

A. Get it on F-Droid :

￼

B. Google Play Public Alpha Test :

You can join the public alpha test programme on Google Play. To become a tester, follow these steps:
1. Join the FlorisBoard Public Alpha Test Google Group to be able to access the testing programme.
2. Go to the FlorisBoard Testing Page , then click "Become a tester". Now you are enrolled in the testing programme.
3. To try out FlorisBoard, download it via Google Play. To do so, click on "Download it on Google Play", which takes you to the PlayStore listing .
4. Finished! You will receive future versions of FlorisBoard via Google Play.

With the v0.4.0 release FlorisBoard will enter the public beta in GPlay, allowing to directly search for and download FlorisBoard without prior joining the alpha group.

C. Use the APK provided in the release section of this repo

Beta￼

Releases on this track are also in general stable and should be ready for everyday use, though crashes and bugs are more likely to occur. Use releases from this track if you want to get new features faster and give feedback for brand-new stuff. Options to get beta releases:

A. IzzySoft's repo for F-Droid :

￼

B. Google Play :

Follow the same steps as for the stable track, the app can then be accessed here .

C. Use the APK provided in the release section of this repo

Giving feedback

If you want to give feedback to FlorisBoard, there are several ways to do so, as listed here .

$

￼

Implemented features

This list contains all implemented and fully functional features FlorisBoard currently has to offer. For planned features and its milestones, please refer to the Feature roadmap .

Basics
• [x] Implementation of the keyboard core (InputMethodService)
• [x] Custom implementation of deprecated KeyboardView (base only)
• [x] Caps + Caps Lock
• [x] Key popups
• [x] Extended key popups (e.g. a -> á, à, ä, ...)
• [x] Audio/haptic feedback for keyboard touch interaction
• [x] Portrait orientation support
• [x] Landscape orientation support (needs tweaks)

Layouts
• [x] Latin character layouts (QWERTY, QWERTZ, AZERTY, Swiss, Spanish, Norwegian, Swedish/Finnish, Icelandic, Danish, Hungarian, Croatian, Polish, Romanian, Colemak, Dvorak, Turkish-Q, Turkish-F, and more...)
• [x] Non-latin character layouts (Arabic, Persian, Kurdish, Greek, Russian (JCUKEN), Japanese JIS, and more...)
• [x] Adapt to situation in app (password, url, text, etc. )
• [x] Special character layout(s)
• [x] Numeric layout
• [x] Numeric layout (advanced)
• [x] Phone number layout
• [x] Emoji layout
• [x] Emoticon layout

Preferences
• [x] Setup wizard
• [x] Preferences screen
• [x] Customize look and behaviour of keyboard
• [x] Theme presets (currently only day/night theme + borderless)
• [x] Theme customization
• [x] Subtype selection (language/layout)
• [x] Keyboard behaviour preferences
• [x] Gesture preferences
• [x] User dictionary manager (system and internal)

Other useful features
• [x] Support for Android 11+ inline autofill API
• [x] One-handed mode
• [x] Clipboard/cursor tools
• [x] Clipboard manager/history
• [x] Integrated number row / symbols in character layouts
• [x] Gesture support
• [x] System-wide spell checker with spell results from FlorisBoard
• [x] Full support for the system user dictionary (shared dictionary between all keyboards) and a private, internal user dictionary
• [x] Full integration in IME service list of Android (xml/method) (integration is internal-only, because Android's default subtype implementation not really allows for dynamic language/layout pairs, only compile-time defined ones)
• [x] Description and settings reference in System Language & Input
• [ ] (dev only) Generate well-structured documentation of code
• [ ] ...

Contributing

Wanna contribute to FlorisBoard? That's great to hear! There are lots of different ways to help out. Bug reporting, making pull requests, translating FlorisBoard to make it more accessible, etc. For more information see the contributing guidelines . Thank you for your help!

List of permissions FlorisBoard requests

Please refer to this page to get more information on this topic.

Used libraries, components and icons
• Google Flexbox Layout for Android by google
• Google Material icons by google
• KotlinX serialization library by Kotlin
• ColorPicker preference by Jared Rummler
• Timber by JakeWharton
• expandable-fab by Nambi
• ICU4C by The Unicode Consortium
• Nuspell by Nuspell

License

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



