use lazy_static::lazy_static;
use linkify::{self, LinkFinder};
use regex::Regex;

lazy_static! {
    static ref LINK_FINDER: LinkFinder = LinkFinder::new();
    static ref REDDIT_REGEX: Regex = Regex::new(r"\/?(r\/[a-zA-Z0-9_]{3}[a-zA-Z0-9_]{0,18}|u\/[a-zA-Z0-9_-]{3}[a-zA-Z0-9_-]{0,17})").unwrap();
}

pub fn preprocess_auto(text: &str) -> String {
    let mut cleaned_text = String::new();
    let mut begin_cleaned_index = 0;
    for span in LINK_FINDER.links(text) {
        cleaned_text.push_str(&text[begin_cleaned_index..span.start()]);
        begin_cleaned_index = span.end();
    }
    cleaned_text.push_str(&text[begin_cleaned_index..]);
    cleaned_text = REDDIT_REGEX.replace_all(&cleaned_text, "").to_string();
    return cleaned_text;
}
