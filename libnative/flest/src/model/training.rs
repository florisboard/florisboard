use textutils::normalization::StringNormalizationHelpers;

use super::NgramModel;

impl NgramModel {
    pub fn train_from_sentence(&mut self, sentence: &Vec<&str>) {
        let mut sentence_nfd = Vec::with_capacity(sentence.len() + 1);
        sentence_nfd.push(self.meta.sentence_token.to_nfd_chars());
        for word in sentence {
            sentence_nfd.push(word.to_nfd_chars());
        }

        for sent_i in 0..sentence_nfd.len() {
            for ngram_i in 0..=(self.options.max_ngram_size - 1).clamp(0, sent_i) {
                let i = sent_i - ngram_i;
                assert!(i < sentence_nfd.len()); // catch overflow issues
                let ngram = &sentence_nfd[i..=sent_i].iter().rev().cloned().collect::<Vec<_>>();
                let node = self.trie_root.get_ngram_or_insert(ngram);
                let data = node.value.as_mut().unwrap();
                data.time = self.meta.update_and_get_time();
                data.count += 1;
            }
        }
    }

    pub fn train_from_tokens(&mut self, tokens: &Vec<&str>) {
        for n in 1..=self.options.max_ngram_size {
            if n > tokens.len() {
                continue;
            }
            for i in 0..tokens.len() - n + 1 {
                let ngram = &tokens[i..(i + n)];
                let ngram = ngram.iter().rev().map(|&x| x.to_nfd_chars()).collect::<Vec<_>>();
                let node = self.trie_root.get_ngram_or_insert(ngram.as_slice());
                let data = node.value.as_mut().unwrap();
                data.time = self.meta.update_and_get_time();
                data.count += 1;
            }
        }
    }
}
