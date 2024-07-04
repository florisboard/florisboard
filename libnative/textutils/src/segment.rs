use icu_segmenter::{GraphemeClusterSegmenter, SentenceSegmenter, WordSegmenter};
use itertools::Itertools;

pub struct IcuSegmenterCache {
    sentence_segmenter: SentenceSegmenter,
    word_segmenter: WordSegmenter,
    grapheme_cluster_segmenter: GraphemeClusterSegmenter,
}

impl IcuSegmenterCache {
    pub fn new_auto() -> Self {
        let sentence_segmenter = SentenceSegmenter::new();
        let word_segmenter = WordSegmenter::new_auto();
        let grapheme_cluster_segmenter = GraphemeClusterSegmenter::new();
        return Self {
            sentence_segmenter,
            word_segmenter,
            grapheme_cluster_segmenter,
        };
    }

    pub fn split_sentences<'t>(&self, text: &'t str) -> Vec<&'t str> {
        return split_sentences(text, &self.sentence_segmenter);
    }

    pub fn split_words<'t>(&self, text: &'t str) -> Vec<&'t str> {
        return split_words(text, &self.word_segmenter);
    }

    pub fn split_grapheme_clusters<'t>(&self, text: &'t str) -> Vec<&'t str> {
        return split_grapheme_clusters(text, &self.grapheme_cluster_segmenter);
    }
}

pub fn split_sentences<'t>(text: &'t str, segmenter: &SentenceSegmenter) -> Vec<&'t str> {
    let sentences: Vec<&str> = segmenter
        .segment_str(text)
        .tuple_windows()
        .map(|(i, j)| text[i..j].trim())
        .filter(|sentence| !sentence.is_empty())
        .collect();
    return sentences;
}

pub fn split_words<'t>(text: &'t str, segmenter: &WordSegmenter) -> Vec<&'t str> {
    let words: Vec<&str> = segmenter
        .segment_str(text)
        .iter_with_word_type()
        .tuple_windows()
        .filter(|(_, (_, segment_type))| segment_type.is_word_like())
        .map(|((i, _), (j, _))| &text[i..j])
        .collect();
    return words;
}

pub fn split_grapheme_clusters<'t>(text: &'t str, segmenter: &GraphemeClusterSegmenter) -> Vec<&'t str> {
    let grapheme_clusters: Vec<&str> = segmenter
        .segment_str(text)
        .tuple_windows()
        .map(|(i, j)| &text[i..j])
        .collect();
    return grapheme_clusters;
}
