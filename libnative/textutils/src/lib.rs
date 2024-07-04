mod filter;
mod segment;

pub use filter::*;
pub use segment::*;

#[cfg(test)]
mod tests {
    use icu_segmenter::{SentenceSegmenter, WordSegmenter};

    use super::*;

    #[test]
    fn segment_sentences_simple() {
        let text = "Hello, world! How are you? I'm fine.";
        let segmenter = SentenceSegmenter::new();
        let sentences = split_sentences(text, &segmenter);
        assert_eq!(&sentences, &["Hello, world!", "How are you?", "I'm fine."]);
    }

    #[test]
    fn segment_words_simple() {
        let text = "Hello, world! How are you? I'm fine.";
        let segmenter = WordSegmenter::new_auto();
        let words = split_words(text, &segmenter);
        assert_eq!(&words, &["Hello", "world", "How", "are", "you", "I'm", "fine"]);
    }

    #[test]
    fn preprocess_auto_simple() {
        let text = "Hello, world! How are you? I'm fine. https://example.com and more";
        let cleaned_text = preprocess_auto(text);
        assert_eq!(&cleaned_text, "Hello, world! How are you? I'm fine.  and more");
    }

    #[test]
    fn preprocess_reddit_ids() {
        let text = "have a look at r/cats, user u/example posed a cute cat in there";
        let cleaned_text = preprocess_auto(text);
        assert_eq!(&cleaned_text, "have a look at , user  posed a cute cat in there");
    }

    #[test]
    fn preprocess_url_markdown() {
        let text = "You can find an example [in the documentation](https://example.com) or on GitHub";
        let cleaned_text = preprocess_auto(text);
        assert_eq!(&cleaned_text, "You can find an example [in the documentation]() or on GitHub");
        let segmenter = WordSegmenter::new_auto();
        let words = split_words(&cleaned_text, &segmenter);
        assert_eq!(&words, &["You", "can", "find", "an", "example", "in", "the", "documentation", "or", "on", "GitHub"]);
    }
}
