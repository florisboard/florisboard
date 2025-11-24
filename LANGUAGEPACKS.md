# Language Packs

## Languages

- [Summary](#summary)
- [Chinese / 中文](LANGUAGEPACKS-CHINESE.md)

## Summary

FlorisBoard supports internationalization through Crowdin, a community-driven translation platform. The project now supports **over 120 languages** (125 distinct languages with 29 additional regional variants, totaling 154 language configurations), allowing users from around the world to use FlorisBoard in their native language.

### Supported Languages

FlorisBoard provides UI translations for the following languages through [Crowdin](https://crowdin.florisboard.org):

#### Major Languages
- **Arabic** (Standard, Saudi Arabia, Egypt)
- **Chinese** (Simplified, Traditional, Hong Kong)
- **English** (US, UK, Australia, Canada, India, New Zealand, South Africa)
- **French** (France, Canada, Switzerland)
- **German** (Germany, Austria, Switzerland)
- **Portuguese** (Portugal, Brazil)
- **Spanish** (Spain, Latin America, Mexico, Argentina, United States)
- **Russian** (Russia, Ukraine)

#### European Languages
- Asturian, Basque, Belarusian, Bosnian, Bulgarian, Catalan, Corsican, Croatian, Czech, Danish, Dutch (Netherlands, Belgium), Esperanto, Estonian, Finnish, Galician, Greek, Hungarian, Icelandic, Irish, Italian (Italy, Switzerland), Latvian, Lithuanian, Luxembourgish, Macedonian, Maltese, Norwegian (Bokmal, Nynorsk), Polish, Romanian, Scottish Gaelic, Serbian (Cyrillic, Latin), Slovak, Slovenian, Swedish, Turkish, Ukrainian, Welsh, Western Frisian

#### Asian Languages
- Armenian, Azerbaijani, Bengali (Bangladesh, India), Burmese, Filipino/Tagalog, Georgian, Gujarati, Hebrew, Hindi, Hmong, Indonesian, Japanese, Javanese, Kannada, Kazakh, Khmer (Cambodian), Korean, Kurdish (Kurmanji, Sorani), Kyrgyz, Lao, Malay (Malaysia), Malayalam, Marathi, Mongolian, Nepali, Odia (Oriya), Pashto, Persian (Farsi), Punjabi, Sindhi, Sinhala, Tamil (India), Telugu, Thai, Tibetan Standard, Tigrinya, Turkmen, Uyghur, Urdu (Pakistan), Uzbek, Vietnamese

#### African Languages
- Afrikaans, Amharic, Hausa, Igbo, Kinyarwanda, Krio, Luo, Malagasy, Sesotho, Shona, Somali, Swahili, Tsonga, Tswana, Xhosa, Yoruba, Zulu, Tamazight

#### Pacific and Other Languages
- Hawaiian, Maori, Samoan, Tongan

#### Constructed and Indigenous Languages
- Esperanto, Latin, Aymara, Cebuano, Chichewa, Guarani, Haitian Creole, Quechua

#### Additional Regional Variants
- Kashmiri, Dhivehi, Tatar, and many regional dialects

### How Language Packs Work

1. **Crowdin Integration**: All translations are managed through [Crowdin](https://crowdin.florisboard.org), a collaborative translation platform where community members can contribute translations.

2. **Automatic Sync**: When translators complete work on Crowdin, the translations are automatically synchronized with the FlorisBoard repository and included in new releases.

3. **Resource Files**: Each language is stored in Android resource directories (e.g., `values-fr/` for French, `values-zh-rCN/` for Chinese Simplified).

4. **System Language Detection**: FlorisBoard automatically detects your device's system language and displays the UI in that language if translations are available.

### How to Contribute Translations

If you'd like to help translate FlorisBoard into your language or improve existing translations:

1. Visit the [FlorisBoard Crowdin project](https://crowdin.florisboard.org)
2. Sign up or log in to Crowdin
3. Select your language from the list
4. Start translating strings
5. Submit your translations for review

If your language is not yet listed in Crowdin, please email [florisboard@patrickgold.dev](mailto:florisboard@patrickgold.dev) to request it.

### Importing Language Packs

FlorisBoard includes all community-translated languages by default. When you install or update FlorisBoard, you automatically receive all available translations. Simply set your device's language to your preferred language, and FlorisBoard will display in that language if translations are available.

For advanced users developing language extensions or keyboard layouts, please refer to the [FlorisBoard Extensions documentation](https://github.com/florisboard/florisboard/wiki) and the [Addons Store](https://beta.addons.florisboard.org).

### Translation Coverage

Translation completeness varies by language. You can check the current translation status for each language on the [Crowdin project page](https://crowdin.florisboard.org). Languages with higher completion percentages will have more of the UI translated.

### Additional Resources

- [FlorisBoard Crowdin Project](https://crowdin.florisboard.org) - Contribute translations
- [Crowdin Language Codes](https://support.crowdin.com/developer/language-codes/) - Technical reference
- [Android Localization Guide](https://developer.android.com/guide/topics/resources/localization) - For developers
- [FlorisBoard Wiki](https://github.com/florisboard/florisboard/wiki) - General documentation
- [Addons Store](https://beta.addons.florisboard.org) - Download extensions

---

**Note**: The availability of a language in the configuration does not guarantee 100% translation coverage. Translation is an ongoing community effort, and some languages may be partially translated. Your contributions on Crowdin help improve translation coverage for all users!

