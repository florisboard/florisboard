# FlorisBoard's feature roadmap & milestones

This feature roadmap intents to provide transparency to what I want to add to FlorisBoard in the foreseeable future.
Note that there are no ETAs for any version milestones down below, experience says these won't hold anyways.

I try my best to release regularly, though some features take a lot longer than others and thus releases can be spaced
out a bit on the stable track. If you are interested in following the development more closely, make sure to follow
along the beta track releases! These are generally more unstable but you get new stuff faster and can provide early
feedback, which helps a lot!

## 0.4

Major release which mainly focuses on adding proper word suggestions and inline autocorrect (for Latin-based languages
only at first). This is a big effort which will take some time to be fully completed. Additionally general small bug
fixes and improvements will be made alongside the development of the main objective.

With this release the versioning scheme changes to `0.x.y`, where `x` specifies the major changes, and `y` are just
small bug fixes and improvements for the former major stable release `x`. This is different to `0.3.x`, where the
version scheme just did not make any sense anymore, especially with the latest `0.3.x` releases. As for the beta track,
major developments (`0.x`) will have alpha, beta and release candidate releases on the beta track before it goes live on
the stable track. Small follow-up bug fixes (`0.x.y`) will be published on both the stable and beta track without
release candidates.

### Word suggestions / Autocorrect

The development effort of this feature is quite big, thus it is split into multiple phases:

**Phase 1: Preparations of suggestions UI & interfacing API (first alpha release(s))**

- Rework Smartbar suggestions UI
    - Allow for primary and optionally secondary label (in a smaller font) to be shown per suggestion
    - Better integrate clipboard suggestions into word suggestion flow
    - Add long-press suggestion action for user to prevent from showing again
    - Generally fix and polish suggestions UI design (3-column mode and scrollable mode)
- Add `SuggestionProvider` interface API
    - Is responsible for the interaction between UI, input logic and suggestion core
- Try to add toggle for not underlining the current word (composing region) while not loosing the caching benefits
- In parallel: Do local research and preps for phase 2

**Phase 2: Add native (C++) Latin word suggestion core (alpha releases)**

- Research and experiment with different approaches/data sources for Latin-based language prediction and autocorrect
    - Research will mainly be done first locally on Linux to decide what to use
    - Implementation will be in C++ using STL libraries and if needed other open-source libraries, with compatibility
      and CPU/memory restrictions on Android devices in mind
    - Once an experiment runs well locally it will be included in the main project and tested out within the keyboard UI
      in different alpha releases
    - Especially at the beginning an idea may be scrapped and replaced by something else if found that another approach
      is better
- (Based on research) Introduce new dictionary/language model format
    - Importing the dictionaries/models as well as management relies on the Flex extension core and UI in Kotlin
    - Actually parsing and generating suggestions happens in C++
    - The actual format of the dictionary/model source is not decided yet
    - Add system in preprocessing stage to properly mark slightly offensive words and prevent extremely offensive words
      from being included at all
    - Add system in preprocessing stage to filter out email addresses and phone numbers that may be included in the
      large datasets which are used for building the models

**Phase 3: Add support for more languages & Allow glide typing to utilize new word prediction system (beta releases)**

- Glide typing: Utilize new prediction system and get rid of current English (US) json dictionary
- Add support for more languages (Latin-based), may need to utilize datasets like Opensubtitles or Wikimedia, although
  those need extensive cleaning and are not as reliable
- Focus on improving performance and stabilizing the Latin suggestion core
- Possibly address some language-specific issues and ensure suggested word capitalization is correct
- Finalize Settings and keyboard UI regarding word suggestions.

### Other planned features for 0.4

- General small fixes and improvements
- Community repository on GitHub for extension sharing across users (may be 0.5.0 though)
- Localized emoji suggestions (may be 0.5.0 though)

## 0.5

- Complete rework of the Emoji panel
    - Recently used / Emoji history (already implemented with 0.3.14)
    - Emoji search
    - Emoji suggestions when using :emoji_name: syntax
    - Kaomoji panel implementation (the third tab which currently has "not yet implemented")
- Smartbar customization improvements
    - Quick actions customization (order and which buttons to show)
- Prepare FlorisBoard repository and app store presence for public beta release on Google Play (will go live with stable
  0.5.0!!)
- Rework branding images and texts of FlorisBoard for the app stores
- Focus on stability and experience improvements of the app and keyboard

## Backlog

**Features that MAY be added (even in versions mentioned above) or dismissed altogether**

- Full on-board layout editor which allows users to create their own layouts without writing a JSON file
- Import/Export of custom layout files packed in Flex extensions
- Theme rework part II
- Adaptive themes v2
- Voice-to-text with Mozilla's open-source voice service
- Text translation
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
