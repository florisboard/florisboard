use std::collections::HashMap;

use crate::dyntrie::DynTrie;

#[derive(Default)]
struct NgramModelNode {
    children: DynTrie<Box<NgramModelNode>>,
    time: u64,
    usage: u64,
}

impl NgramModelNode {
    fn find(&self, ngram: &[&str]) -> Option<&NgramModelNode> {
        if ngram.is_empty() {
            return None;
        }
        let token: Vec<char> = ngram[0].chars().collect();
        let child = self.children.find(&token);
        if child.is_none() {
            return None;
        }
        let child = child.unwrap();
        if ngram.len() == 1 {
            return Some(child);
        }
        return child.find(&ngram[1..]);
    }

    fn find_many(&self, ngram: &[&str]) -> Vec<(Vec<char>, &NgramModelNode)> {
        if ngram.is_empty() {
            return Vec::new();
        }
        let token: Vec<char> = ngram[0].chars().collect();
        let ret = self.children.find_many(&token);
        if ngram.len() == 1 {
            return ret
                .into_iter()
                .map(|node| (node.0, node.1.as_ref()))
                .collect();
        }
        let mut ret2 = Vec::new();
        for (_, child) in &ret {
            ret2.extend(child.find_many(&ngram[1..]));
        }
        return ret2;
    }

    fn train(&mut self, ngram: &[&str], current_time: u64) {
        if ngram.is_empty() {
            panic!("ngram must not be empty");
        }
        let token: Vec<char> = ngram[0].chars().collect();
        let child = self.children.find_or_insert(&token, Box::new(NgramModelNode::default()));
        if ngram.len() == 1 {
            if current_time != 0 {
                child.time = current_time;
            }
            child.usage += 1;
        } else {
            child.train(&ngram[1..], current_time);
        }
    }

    fn debug_print(&self, _indent: usize) {
        // println!("{}{}{}", "  ".repeat(indent), self.token, if self.time > 0 { "*" } else { "" });
        // for child in &self.children {
        //     child.debug_print(indent + 1);
        // }
    }
}

#[derive(Default)]
pub struct NgramModel {
    root: NgramModelNode,
    time: u64,
}

impl NgramModel {
    #[allow(dead_code)]
    fn find(&self, ngram: &[&str]) -> Option<&NgramModelNode> {
        self.root.find(ngram)
    }

    fn find_many(&self, ngram: &[&str]) -> Vec<(Vec<char>, &NgramModelNode)> {
        self.root.find_many(ngram)
    }

    pub fn train_dataset(&mut self, token_list: &[&str]) {
        self.root.train(token_list, 0);
    }

    pub fn train_input(&mut self, token_list: &[&str]) {
        self.time += 1;
        self.root.train(token_list, self.time);
    }

    pub fn debug_print(&self) {
        self.root.debug_print(0);
    }

    pub fn predict(&self, history: &Vec<&str>) -> Vec<(String, f64)> {
        let mut tmin = u64::MAX;
        let mut tmax = u64::MIN;
        let mut umin = u64::MAX;
        let mut umax = u64::MIN;
        let nmin = 1;
        let nmax = 3;
        let mut candidate_nodes: Vec<(Vec<char>, &NgramModelNode, f64)> = Vec::new();

        let user_input_word = history.last().unwrap_or(&"");

        for n in nmin..=std::cmp::min(history.len(), nmax) {
            let nweight = 1.0 - (nmax - n) as f64 * 0.1;
            let ngram = &history[history.len() - n..history.len() - 1];
            let nodes = self.find_many(ngram);
            for (_, node) in nodes {
                node.children.for_each(&mut |curr_word, child| {
                    candidate_nodes.push((curr_word.to_owned(), child, nweight));
                    tmin = tmin.min(child.time);
                    tmax = tmax.max(child.time);
                    umin = umin.min(child.usage);
                    umax = umax.max(child.usage);
                });
            }
        }

        candidate_nodes = candidate_nodes
            .into_iter()
            .map(|(word, node, nweight)| {
                (
                    word,
                    node,
                    nweight
                        * norm_weight(node.time, tmin, tmax)
                        * norm_weight(node.usage, umin, umax),
                )
            })
            .collect();

        if !user_input_word.is_empty() {
            let user_input_word: Vec<char> = user_input_word.chars().collect();
            let mut filtered_nodes = Vec::new();
            for (word, node, weight) in candidate_nodes {
                let score_len = std::cmp::min(
                    (word.len() + user_input_word.len()) / 2,
                    user_input_word.len(),
                ) as f64;
                let score = str_fuzzy_match_live(&word, &user_input_word);
                if score > 0.0 {
                    let new_weight = 0.95 * (score / score_len) + 0.05 * weight;
                    filtered_nodes.push((word, node, new_weight));
                }
            }
            self.root.children.for_each(&mut |word, node| {
                let score_len = std::cmp::min(
                    (word.len() + user_input_word.len()) / 2,
                    user_input_word.len(),
                ) as f64;
                let score = str_fuzzy_match_live(&word, &user_input_word);
                if score > 0.0 {
                    let new_weight = 0.75 * (score / score_len) + 0.25 * 0.0;
                    filtered_nodes.push((word.to_owned(), node, new_weight));
                }
            });
            candidate_nodes = filtered_nodes;
        }

        candidate_nodes.sort_by(|a, b| b.2.partial_cmp(&a.2).unwrap());

        let mut predictions: HashMap<String, f64> = HashMap::new();
        for (word, _, weight) in candidate_nodes {
            predictions
                .entry(word.iter().collect())
                .or_insert(weight);
        }

        let mut predictions_vec: Vec<(String, f64)> = predictions.into_iter().collect();
        predictions_vec.sort_by(|a, b| b.1.partial_cmp(&a.1).unwrap());

        predictions_vec.into_iter().take(8).collect()
    }
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

fn str_fuzzy_match_live(word: &[char], current_word: &[char]) -> f64 {
    //let len1 = word.len();
    let len2 = current_word.len();
    let mut score = 0.0;
    let mut penalty: f64 = 0.0;
    for i in 0..len2 {
        let ch1 = word.get(i).unwrap_or(&' ');
        let ch2 = current_word.get(i).unwrap_or(&' ');
        if ch1 == ch2 {
            score += 1.0;
        } else if ch1.to_lowercase().eq(ch2.to_lowercase()) {
            score += 0.9;
        } else {
            penalty += if i == 0 { 2.0 } else { 1.0 };
        }
    }
    return f64::max(0.0, score - 0.125 * penalty.powi(2));
}
