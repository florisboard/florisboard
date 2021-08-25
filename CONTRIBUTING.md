# Contributing

First off, thanks for considering contributing to FlorisBoard!

There are several ways to contribute to FlorisBoard. This document
provides some general guidelines for each type of contribution.

## Giving general feedback

NEW! You can now [give general feedback](https://github.com/florisboard/florisboard/discussions/new?category=feedback)
directly here on GitHub. This is the preferred way to give feedback, as
it allows not only for me to read and respond to feedback, but for everyone
in this community.

Optionally you can also use the review function within Google Play or email me
at [florisboard@patrickgold.dev](mailto:florisboard@patrickgold.dev). I
love to hear from you! Note, that the amount of feedback emails I get
is overwhelmingly high - so if I don't answer or answer really late, I
apologize - I guarantee though that I read through every email and that
I will use every feedback to improve FlorisBoard :)

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

## Adding a new keyboard layout

Adding a layout to FlorisBoard is very simple and does not require any
coding skills, although you should understand the basics of the JSON
syntax (it is very easy though by just looking at some other layout files).
There are two main steps in adding new layouts, though the config step can
be skipped if you only add a layout without a new default language support.

### The config file ([`app/src/main/assets/ime/config.json`](app/src/main/assets/ime/config.json))

This file is very important, as it defines all default currency sets as
well as all default subtypes available in the Settings Subtype UI. Note
that you don't have to modify this file if you add a layout for an already
pre-configured language.

- `currencySets`: This is a list of all currency sets, which can be chosen
  for each subtype. If you consider adding a new one, make sure that the
  first currency symbol matches the name of the currency set and also
  ensure that you have exactly 6 currency symbols. This is important as the
  symbol layouts have exactly 6 slots available to fill these defined
  currency symbols in.
- `defaultSubtyes`: This is a list of all pre-made subtypes. Each time the
  user selects a language in the `Subtype Add`-dialog, all options configured
  here will get pre-selected. The language tag must adhere to the IETF BCP
  47 code ([ISO 639-1 language code](https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes)
  and [ISO 3166-1 region code](https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2#Officially_assigned_code_elements)).
  For example, Dutch as spoken in Belgium is `nl-be`. Use a unique value
  for `id` to avoid possible crashes caused by duplicate ids.

### Adding the layout

Since v0.3.10-beta05 it is possible to add custom layouts for all types.

To add a new layout, head to [`app/src/main/assets/ime/text`](app/src/main/assets/ime/text) and then select
the correct sub-directory for the type of layout you want to add. In most cases
this will be `characters` to add a layout like QWERTY etc.

For the `code` field of each key, make sure to use the UTF-8 code. An
useful tool for finding the correct code is [unicode-table.com](https://unicode-table.com/en/).
From there, you search for your letter and then use the HTML code, but without the `&#;`
For internal codes of functional or UI keys, see
[`app/src/main/java/dev/patrickgold/florisboard/ime/text/key/KeyCode.kt`](app/src/main/java/dev/patrickgold/florisboard/ime/text/key/KeyCode.kt).

The label is equally important and should always match up with the defined
code. If `code` and `label` don't match up, FlorisBoard won't crash but
it will most likely lead to confusion in the key processing logic.

Any accents or diacritics that should be exposed via long press can be
added at [`app/src/main/assets/ime/text/characters/extended_popups/<languageTag_name_here>.json`](app/src/main/assets/ime/text/characters/extended_popups).
For each key, you can add 1 main and several relevant accents. The main
accent should be used for accents which are important for the language
you add. The main field is used for determining if a hint or an accent
should take priority, so please make sure to leave main empty and just
use relevant for accents which are not-so important.

For popups of non-`characters` layout, simply add the popup directly to
each key via the `popup` field.

## Adding a new dictionary for a language

Currently the suggestions implementation is highly experimental and will
get a major if not complete rework, so dictionaries are currently not
accepted.

## Bug reporting

This kind of contribution is the most important, as it tells where
FlorisBoard has flaws and thus should be improved to maximize stability
and user experience. To make this process as smooth as possible, please
use the pre-made [issue template](.github/ISSUE_TEMPLATE/bug_report.md)
for bug reporting. This makes it easy for us to understand what the bug
is and how to solve it.

### Capturing error logs

Logs are captured by FlorisBoard's crash handler, which gives you the
ability to copy it to the clipboard and paste it in GitHub. This is the
preferred way to capture logs.

Alternatively, you can also use ADB (Android Debug Bridge) to capture
the error log. This is recommended for experienced users only.

## Donating

If none of the above options are feasible for you but you still want to
show your support, you can also buy me a coffee, so I can stay up all night
and chase away bugs or add new cool stuff :)
See the `Sponsors` button for available options!
