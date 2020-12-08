# Contributing

First off, thanks for considering contributing to FlorisBoard!

There are several ways to contribute to FlorisBoard. This document
provides some general guidelines for each type of contribution.

## Giving general feedback

Either use the review function within Google Play or email me at
[florisboard@patrickgold.dev](mailto:florisboard@patrickgold.dev). I
love to hear from you!

## Translations

To make FlorisBoard accessible in as many languages as possible, the
platform [Crowdin](https://crowdin.florisboard.patrickgold.dev) is used
to crowdsource and manage translations. This is the only source of
translations from now on - **PRs that add/update translations are no
longer accepted.** The list of languages in Crowdin covers the top 20
languages, but feel free to email me at
[florisboard@patrickgold.dev](mailto:florisboard@patrickgold.dev) to
request a language and I'll add it.

## Adding a new feature or making large changes

If you intend to add a new feature or to make large changes, please
discuss this first through a proposal on GitHub. Discussing your idea
enables both you and the dev team that we are on the same page before
you start on working on your change. If you have any questions, feel
free to ask for help at any time!

## Adding a new keyboard layout / dictionary for locale

As FlorisBoard is currently in alpha stage, things might change
drastically. This also includes the config scheme of keyboard layouts.
To prevent incompatible configs because some features and structures may
change, please do not add this kind of content yet. As FlorisBoard's
state progresses and its core stabilizes, you will be able to add
keyboard layouts.

## Bug reporting

This kind of contribution is the most important, as it tells where
FlorisBoard has flaws and thus should be improved to maximize stability
and user experience. To make this process as smooth as possible, please
use the premade [issue template](.github/ISSUE_TEMPLATE/bug_report.md)
for bug reporting. This makes it easy for us to understand what the bug
is and how to solve it.

### Capturing error logs

Logs are captured by FlorisBoard's crash handler, which gives you the
ability to copy it to the clipboard and paste it in GitHub. This is the
preferred way to capture logs.

Alternatively, you can also use ADB (Android Debug Bridge) to capture
the error log. This is recommended for experienced users only.
