# Contributing

First off, thanks for considering contributing to FlorisBoard!

There are several ways to contribute to FlorisBoard. This document provides some
general guidelines for each type of contribution.

## Giving general feedback

Either use the review function within Google Play or email me at
[florisboard@patrickgold.dev](mailto:florisboard@patrickgold.dev). I
love to hear from you!

## Adding a new feature or making large changes

If you intend to add a new feature or to make large changes, please discuss this
first through a proposal on GitHub. Discussing your idea enables both you and the
dev team that we are on the same page before you start on working on your change.
If you have any questions, feel free to ask for help at any time!

## Adding a new keyboard layout / dictionary for locale

As FlorisBoard is currently in alpha stage, things might change drastically. This
also includes the config scheme of keyboard layouts. To prevent incompatible
configs because some features and structures may change, please do not add this
kind of content yet. As FlorisBoard's state progresses and its core stabilizes,
you will be able to add keyboard layouts.

## Translating FlorisBoard

Before starting to translate, when adding a new translation please file
an issue stating that you want to translate FlorisBoard into a language.
Once this gets approved you can start translating. When updating an
already existing translation file you can just send a PR directly.

If you are not familiar with PRs, check out this guide:
[https://www.gun.io/blog/how-to-github-fork-branch-and-pull-request](https://www.gun.io/blog/how-to-github-fork-branch-and-pull-request)

Notes for tips below:
- Replace `<language>` with the language you want to add
- Replace `<code>` with the ISO 639-1 code of the language you want to
  add
  ([List of codes](https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes))

### Tips when adding a new translation

- To add the new translation file, navigate to `app/src/main/res/values`
  and copy the file `strings.xml` into the folder
  `app/src/main/res/values-<code>` (you have to create this folder)
- Translate only the phrases inside the brackets, leave the name
  attribute as it is  
  E.g.: `<string name="hello_string">Hello World!</string>`  
  `<string name="hello_string">Ciao mondo!</string>`
- When finished translating, commit your changes locally, as the commit
  message use `Add <language> translation`
- Push your change(s) and create the PR. When everything checks out, it
  will get accepted.

### Tips when updating a translation

- To update a translation, check the `strings.xml` in
  `app/src/main/res/values` for newly added strings and add them to the
  translation file in `app/src/main/res/values-<code>`
- When finished translating, commit your changes locally, as the commit
  message use `Update <language> translation`
- Push your change(s) and create the PR. When everything checks out, it
  will get accepted.

## Bug reporting

This kind of contribution is the most important, as it tells where
FlorisBoard has flaws and thus should be improved to maximize stability
and user experience. To make this process as smooth as possible, please
use the premade [issue template](.github/ISSUE_TEMPLATE/bug_report.md)
for bug reporting. This makes it easy for us to understand what the bug
is and how to solve it.

### Capturing ADB debug logs

Logs are captured by FlorisBoard's crash handler, which gives you the
ability to copy it to the clipboard and paste it in GitHub.
