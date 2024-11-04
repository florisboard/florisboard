mod prediction;
mod serialization;
mod spellcheck;
mod training;
mod version;

pub use prediction::*;
pub use serialization::*;
pub use spellcheck::*;
pub use training::*;
pub use version::*;

use crate::dyntrie::DynTrieNode;

#[derive(Default, Debug)]
pub struct NgramData {
    time: u64,
    count: u64,
    is_offensive: bool,
    // flag should only be set for 1st level words!!
    is_dictionary_word: bool,
}

pub struct NgramModelMeta {
    version: NgramModelVersion,
    global_time: u64,
    global_count: u64,
    pub sentence_token: String,
}

pub struct NgramModelOptions {
    pub max_candidates: usize,
    pub max_ngram_size: usize,
    pub allow_offensive: bool,
}

pub struct NgramModel {
    pub trie_root: DynTrieNode<NgramData>,
    pub meta: NgramModelMeta,
    pub options: NgramModelOptions,
}

impl NgramModel {
    pub fn new() -> Self {
        NgramModel {
            trie_root: DynTrieNode::new(),
            meta: NgramModelMeta {
                version: NgramModelVersion::latest(),
                global_time: 0,
                global_count: 0,
                sentence_token: "\u{1e}".to_owned(),
            },
            options: NgramModelOptions {
                max_candidates: 5,
                max_ngram_size: 3,
                allow_offensive: false,
            },
        }
    }
}

impl NgramModelMeta {
    fn update_and_get_time(&mut self) -> u64 {
        self.global_time += 1;
        self.global_count += 1;
        return self.global_time;
    }
}
