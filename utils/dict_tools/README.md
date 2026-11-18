# Dictionary sources

`compile_dicts.sh` compiles `en_US.json` into `app/src/main/assets/ime/dict/en_US.dict`.

The words in `en_US.json` come from:

- FlorisBoard's glide typing dictionary, `app/src/main/assets/ime/dict/data.json` under the project's Apache 2.0 license
- `words.txt` from [dwyl/english-words](https://github.com/dwyl/english-words), released under the [Unlicense](https://unlicense.org)
- a few added words, such as "y'all", "TikTok" and some country names

The combined list was trimmed to about 60,000 common words and filtered for offensive words and its frequencies were rescaled.
