# FlorisBoard's feature roadmap & milestones

This feature roadmap intents to provide transparency to what I want to add
to FlorisBoard in the foreseeable future. Note that there are no ETAs for any
version milestones down below, experience says these won't hold anyways.

I try my best to release regularly, though some features take a lot longer
than others and thus releases can be spaced out a bit on the stable track.
If you are interested in following the development more closely, make sure to
follow along the beta track releases! These are generally more unstable but
you get new stuff faster and can provide early feedback, which helps a lot!

## 0.3.x and 0.4.0
Releases in this section still follow the old versioning scheme, meaning the
patch number is a feature upgrade. As this naming convention is more confusing
than useful, after the v0.4.0 release a new release/development cycle will be
introduced.

### 0.3.13 (currently in development and soon done)
- Spell checking (mainly completed and relatively well working, Smartbar integration still missing)
- Performance improvements in keyboard rendering
- Audio/haptic feedback rework
- Lots and lots of bug fixing in all areas, really fix some annoying bugs
- New layouts added by contributors

### 0.3.14
- Re-write of the Preference core
  - Reduce redundancy in key/default value definitions
  - Avoid having to manually add redundant code for adding a new pref
  - Goes hand-in-hand with the Settings UI re-write
- Re-write of the Settings UI with Jetpack Compose
  - Also re-structure UI into a more list-like panel
  - Adjust theme colors of Settings a bit to make it more modern
  - Preview the keyboard at any time from within the Settings
  - Settings language different than device language
- Re-write the Setup UI in Jetpack Compose
  - Simplify screen based on previously discussed ideas and mock-ups
  - Improve backend setup logic
- Implement base-UI for extensions and further continue development
  of existing Flex (FlorisBoard extension) format
  - Allows for a continuous experience of customizing FlorisBoard in different areas
  - Planned in the future (not in this version though) what will use Flex:
    - Themes
    - Layouts (Characters, symbols, numeric, ...)
    - Composers for non-Latin script languages
    - Word suggestion dictionaries
    - Spell check dictionaries
    - User dictionaries
    - Other features that require only data and no logic
- Maybe full backup of preferences? Not 100% confirmed though and may be pushed back

### 0.3.15
- Re-adding word suggestions (at least for Latin-based languages at first)
  - Importing the dictionaries as well as management relies on the Flex extension core and UI in Kotlin
  - Actually parsing and generating suggestions happens in C++ to avoid another OOM catastrophe like in 0.3.9/10
  - The actual format of the dictionary and word list source is not decided yet
- Improvement of the candidate view in Smartbar (for word suggestions)
- Theme rework part I:
  - Custom key corner radius
  - Custom key border color (not shadow!!)
  - Re-work theme internals so they use Flex format
  - Community repository on GitHub for theme sharing across users (when Theme Flex format is ready)

### 0.4.0
- Prepare FlorisBoard repository and app store presence for public beta release
  on Google Play
- Rework branding images and texts of FlorisBoard for the app stores
- Focus on polishing the app and fixing bugs/crashes

With this release the versioning scheme changes: the second number now indicates new features,
changes in the third "patch" number now indicates bug fixes for the stable track. The development
cycle for each 0.x release will have -betaXX and -rcXX (release candidate) releases on the beta
track for interested people to follow along the development.

## 0.5.0
- Complete rework of the Emoji panel
  - Recently used / Emoji history
  - Emoji search
  - Emoji suggestions when using :emoji_name: syntax
  - Kaomoji panel implementation (the third tab which currently has "not yet implemented")
- Full Smartbar customization
  - Includes internal rework how Smartbar is build and assembled
  - Allow for more than one Smartbar / Stackable and Collapsible Smartbars
  - Customizable quick actions, clipboard row

## 0.6.0
- Full on-board layout editor which allows users to create their own layouts
  without writing a JSON file
- Import/Export of custom layout files packed in Flex extensions

## Backlog / Features that MAY be added
- Theme rework part II
- Adaptive themes v2
- Voice-to-text with Mozilla's open-source voice service
- Text translation
- Glide typing better word detection
- Proximity-based key typo detection
- Floating keyboard
- Tablet mode / Optimizations for landscape input
- Stickers/GIFs
- FlorisBoard landing web page for presentation
- Implementing additional layouts
- Support for Tasker/Automate/MacroDroid plugins
- Support for WearOS/Smartwatches
- Handwriting
- ...
