# Contribution guidelines

Thanks for considering contributing to FlorisBoard!

There are several ways to contribute to FlorisBoard. This document provides some general guidelines for each type of contribution.

## General contributions

### Translation

To make FlorisBoard accessible in as many languages as possible, the platform [Crowdin](https://crowdin.florisboard.patrickgold.dev) is used to crowdsource and manage translations.  The list of languages in Crowdin covers a good range of languages, but feel free to send an email to [florisboard@patrickgold.dev](mailto:florisboard@patrickgold.dev) to request a new language.

> [!IMPORTANT]
> This is the only source of translations - **PRs that add/update translations are not accepted.**

### Feedback

You can [give general feedback](https://github.com/florisboard/florisboard/discussions/new?category=feedback) directly here on GitHub. This is the preferred way to give feedback, as it allows not only for me to read and respond to feedback, but for everyone in this community.

### Bug reporting

This kind of contribution is the most important, as it tells where FlorisBoard has flaws and thus should be improved to maximize stability and user experience. To make this process as smooth as possible, please use the pre-made [issue template](.github/ISSUE_TEMPLATE/bug_report.md) for bug reporting. This makes it easy for us to understand what the bug is and how to solve it.

#### Capturing error logs

Logs are captured by FlorisBoard's crash handler, which gives you the ability to copy it to the clipboard and paste it in GitHub. This is the preferred way to capture logs.

Alternatively, you can also use ADB (Android Debug Bridge) to capture the error log. This is recommended for experienced users only.

### Feature proposals

Use the feature proposal issue template to suggest a new idea or improvement for this project.

## Code contributions

You are always welcome to contribute new features or work on existing issues, there are a lot to choose from :) It is always best to quickly ask if someone is already working on this issue to avoid duplicate issues.

> [!NOTE]
> If you intend to implement a bigger feature please coordinate with us so we can prevent that there's a major difference in expected implementation.

If you are overwhelmed by the code don't hesistate to ask for help in the [dev chat](https://matrix.to/#/#florisboard-dev:matrix.org) or the discussions tab! Some issues are also marked as good first issue, which are easy to do tasks.

### System requirements for development

- Desktop PC with Linux or WSL2 (Windows)
  - MacOS and Windows without WSL2 probably works too however there's no official support
- At least 16GB of RAM (because of Android Studio)
- The following tools must be installed:
  - Android Studio (bundles SDK and NDK)
  - Java 17
  - CMake 3.22+
  - Clang 15+
  - Git
  - [Rust](https://www.rust-lang.org/tools/install)
- Utilities (optional)
  - Python 3.10+
  - Bash, realpath, grep, ...

### Manual build without Android Studio

If you want to manually build the project without Android Studio you must ensure that the Android SDK and NDK are properly installed on your system. Then issue

```./gradlew clean && ./gradlew assembleDebug```

and Gradle should take care of every build task.

## Joining the team

If you want to join the core maintainer/moderator team on a volunteer basis and be part of this project's journey that's great to hear!

### Basic Requirements

- A passion for seeing FlorisBoard flourish
- Good English skills for team and public communication
- A GitHub account and a Matrix handle

### Why Join

You'll have the chance to work directly with me and other team members. While the general idea is for us to work on all kinds of different aspects of the project as a team, if you're particularly interested in a specific area (e.g., UI, extensions, text processing), that's totally okay too!

### Available Roles

Currently the following roles are available and need help:

Role Description | Required Dev Experience
---|---
Software Developer (Kotlin) for Core App | Java/Kotlin development experience (on Android)
Software Developer (Rust) for Native Core | Some Rust development experience
GitHub Issues/Discussions Moderator | None
Crowdin Translation Verifier | Language proficiency for the language you want to verify

Interested? Feel free to dm me ([@patrickgold](https://matrix.to/#/@patrickgold:matrix.org)) on Matrix or send an email to [florisboard@patrickgold.dev](mailto:florisboard@patrickgold.dev).

## Donating

Alternatively you can also show your support by buying me a coffee, so I can stay up all night and chase away bugs or add new cool stuff :)
See the `Sponsors` button for available options!
