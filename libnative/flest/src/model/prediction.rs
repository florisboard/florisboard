use textutils::{fuzzy, normalization::StringNormalizationHelpers};

use crate::{candidates::{Candidate, CandidateQueue}, dyntrie::TOKEN_SEPARATOR};

use super::NgramModel;

impl NgramModel {
    pub fn predict(&self, partial_sentence: &Vec<&str>) -> Vec<Candidate> {
        if partial_sentence.is_empty() {
            return vec![];
        }

        let mut partial_sentence_nfd = Vec::with_capacity(partial_sentence.len() + 1);
        partial_sentence_nfd.push(self.meta.sentence_token.to_nfd_chars());
        for word in partial_sentence {
            partial_sentence_nfd.push(word.to_nfd_chars());
        }
        let curr_word_nfd = &partial_sentence_nfd.last().unwrap();
        let history_nfd = &partial_sentence_nfd[..partial_sentence_nfd.len() - 1];

        if curr_word_nfd.is_empty() {
            return self.predict_next_word(history_nfd);
        }

        return self.predict_curr_word(curr_word_nfd, history_nfd);
    }

    fn predict_next_word(&self, history_nfd: &[Vec<char>]) -> Vec<Candidate> {
        let mut candidate_queue = CandidateQueue::with_capacity(self.options.max_candidates);
        let max_history_depth = (self.options.max_ngram_size - 1).min(history_nfd.len());

        let tmax = self.meta.global_time;
        let tmin = if tmax >= 300 { tmax - 300 } else { 0 };
        let cmax = self.meta.global_count;
        let cmin = 0;

        self.trie_root.for_each_fnmut(&mut |word, word_node| {
            let node = word_node.traverse(TOKEN_SEPARATOR);
            if node.is_none() {
                return;
            }
            let value = word_node.value.as_ref().unwrap();
            let time_conf = norm_weight(value.time, tmin, tmax);
            let count_conf = norm_weight(value.count, cmin, cmax);

            let mut hist_node = node.unwrap();
            for hist_index in 0..max_history_depth {
                let hist_word = &history_nfd[history_nfd.len() - hist_index - 1];
                // TODO: instead of get use fuzzy get with:
                // case-insentive match and accent-insensitive match
                let hist_node_opt = hist_node.get(hist_word);
                if hist_node_opt.is_none() {
                    return;
                }
                hist_node = hist_node_opt.unwrap();
                let hist_value = hist_node.value.as_ref();
                if hist_value.is_none() {
                    return;
                }
                let hist_value = hist_value.unwrap();
                let hist_time_conf = norm_weight(hist_value.time, tmin, tmax);
                let hist_count_conf = norm_weight(hist_value.count, cmin, cmax);
                let hist_conf = calc_confidence(hist_time_conf, hist_count_conf, 1.0);
                let conf = calc_confidence(time_conf, count_conf, hist_conf);
                candidate_queue.push(word.iter().collect(), conf);

                let hist_node_opt = hist_node.traverse(TOKEN_SEPARATOR);
                if hist_node_opt.is_none() {
                    return;
                }
                hist_node = hist_node_opt.unwrap();
            }
        });

        return candidate_queue.into_sorted_vec();
    }

    fn predict_curr_word(&self, curr_word_nfd: &Vec<char>, history_nfd: &[Vec<char>]) -> Vec<Candidate> {
        let mut candidate_queue = CandidateQueue::with_capacity(self.options.max_candidates);
        let max_history_depth = (self.options.max_ngram_size - 1).min(history_nfd.len());

        let tmax = self.meta.global_time;
        let tmin = if tmax >= 300 { tmax - 300 } else { 0 };
        let cmax = self.meta.global_count;
        let cmin = 0;

        // TODO: implement fuzzy_for_each_fnmut
        self.trie_root.for_each_fnmut(&mut |word, word_node| {
            // TODO: the fuzzy matcher needs to be written completely froms cratch, return a FuzzyResult instead of f64
            if fuzzy::str_match_live(word, curr_word_nfd) < 0.5 {
                return;
            }
            let node = word_node.traverse(TOKEN_SEPARATOR);
            if node.is_none() {
                return;
            }
            let value = word_node.value.as_ref().unwrap();
            let time_conf = norm_weight(value.time, tmin, tmax);
            let count_conf = norm_weight(value.count, cmin, cmax);

            let mut hist_node = node.unwrap();
            for hist_index in 0..max_history_depth {
                let hist_word = &history_nfd[history_nfd.len() - hist_index - 1];
                // TODO: instead of get use fuzzy get with:
                // case-insentive match and accent-insensitive match
                let hist_node_opt = hist_node.get(hist_word);
                if hist_node_opt.is_none() {
                    return;
                }
                hist_node = hist_node_opt.unwrap();
                let hist_value = hist_node.value.as_ref();
                if hist_value.is_none() {
                    return;
                }
                let hist_value = hist_value.unwrap();
                let hist_time_conf = norm_weight(hist_value.time, tmin, tmax);
                let hist_count_conf = norm_weight(hist_value.count, cmin, cmax);
                let hist_conf = calc_confidence(hist_time_conf, hist_count_conf, 1.0);
                let conf = calc_confidence(time_conf, count_conf, hist_conf);
                candidate_queue.push(word.iter().collect(), conf);

                let hist_node_opt = hist_node.traverse(TOKEN_SEPARATOR);
                if hist_node_opt.is_none() {
                    return;
                }
                hist_node = hist_node_opt.unwrap();
            }
        });

        return candidate_queue.into_sorted_vec();
    }
}

fn calc_confidence(time_conf: f64, count_conf: f64, hist_conf: f64) -> f64 {
    println!("time_conf: {}, count_conf: {}, hist_conf: {}", time_conf, count_conf, hist_conf);
    // TODO: count_conf is messed up
    return 0.45 * time_conf + 0.10 * count_conf + 0.45 * hist_conf;
}

fn norm_weight(x: u64, xmin: u64, xmax: u64) -> f64 {
    if x <= xmin {
        return 0.0;
    }
    if x >= xmax {
        return 1.0;
    }
    let xnorm = (x - xmin) as f64 / (xmax - xmin) as f64;
    return 2.0 * xnorm - xnorm.powi(2);
}
