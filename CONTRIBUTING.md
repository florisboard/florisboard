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

You can now officially add layouts to FlorisBoard as described below.
FlorisBoard's core has stabilized enough that adding new content is
safe, although there will be some changes in the future.

Currently you need to modify `app/src/main/assets/ime/config.json` to
add the filename of the language/layout to the `characterLayouts`
section and the `defaultSubtypes` section, making sure to include
the language's IETF BCP 47 code ([ISO 639-1 language code](https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes)
and [ISO 3166-1 region code](https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2#Officially_assigned_code_elements)).
For example, Dutch as spoken in Belgium is `nl-be`. Use a unique value
for `id` to avoid possible crahses caused by duplicate ids.

Add the keyboard layout at `app/src/main/assets/ime/text/characters/<preferredLayout_name_here>.json`,
with `code` referring to the characters codepoint and `label` being the
respective unicode character.

Any accents or diacritics that should be exposed via long press can be
added at `assets/ime/text/characters/extended_popups/<languageTag_name_here>.json`.
For each key, you can add 1 main and several relevant accents. The main
accent should be used for accents which are important for the language
you add. The main field is used for determining if a hint or an accent
should take priority, so please make sure to leave main empty and just
use relevant for accents which are not-so important.

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
