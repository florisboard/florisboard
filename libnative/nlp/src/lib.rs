use serde::{Deserialize, Serialize};
use std::cmp::{min, Ordering};
use std::collections::{BinaryHeap, HashMap};
use std::sync::RwLock;

const MIN_WORD_LENGTH: usize = 2;
const MAX_EDIT_DISTANCE: usize = 2;
const MAX_CONTEXT_WORDS: usize = 3;
const PERSONAL_BONUS: f64 = 0.25;
const CONTEXT_BONUS: f64 = 0.15;
const EXACT_MATCH_BONUS: f64 = 0.5;
const DECAY_FACTOR: f64 = 0.95;
const MAX_PERSONAL_FREQ: u32 = 255;

#[derive(Default)]
struct TrieNode {
    children: HashMap<char, TrieNode>,
    frequency: Option<u32>,
    word: Option<String>,
}

impl TrieNode {
    fn insert(&mut self, word: &str, freq: u32) {
        let mut node = self;
        for ch in word.chars() {
            node = node.children.entry(ch).or_default();
        }
        node.frequency = Some(freq);
        node.word = Some(word.to_string());
    }

    fn search_prefix(&self, prefix: &str) -> Option<&TrieNode> {
        let mut node = self;
        for ch in prefix.chars() {
            node = node.children.get(&ch)?;
        }
        Some(node)
    }

    fn collect_words(&self, results: &mut Vec<(String, u32)>, limit: usize) {
        if results.len() >= limit {
            return;
        }
        if let (Some(word), Some(freq)) = (&self.word, self.frequency) {
            results.push((word.clone(), freq));
        }
        for (_, child) in &self.children {
            child.collect_words(results, limit);
        }
    }
}

#[derive(Clone, Serialize, Deserialize)]
pub struct Suggestion {
    pub text: String,
    pub confidence: f64,
    pub is_eligible_for_auto_commit: bool,
}

impl Eq for Suggestion {}

impl PartialEq for Suggestion {
    fn eq(&self, other: &Self) -> bool {
        self.text == other.text
    }
}

impl Ord for Suggestion {
    fn cmp(&self, other: &Self) -> Ordering {
        self.confidence.partial_cmp(&other.confidence).unwrap_or(Ordering::Equal)
    }
}

impl PartialOrd for Suggestion {
    fn partial_cmp(&self, other: &Self) -> Option<Ordering> {
        Some(self.cmp(other))
    }
}

#[derive(Clone, Serialize, Deserialize)]
pub struct SpellCheckResult {
    pub is_valid: bool,
    pub is_typo: bool,
    pub suggestions: Vec<String>,
}

pub struct NlpEngine {
    main_trie: RwLock<TrieNode>,
    personal_trie: RwLock<TrieNode>,
    main_dict: RwLock<HashMap<String, u32>>,
    personal_dict: RwLock<HashMap<String, u32>>,
    context_map: RwLock<HashMap<String, HashMap<String, u32>>>,
}

impl NlpEngine {
    pub fn new() -> Self {
        Self {
            main_trie: RwLock::new(TrieNode::default()),
            personal_trie: RwLock::new(TrieNode::default()),
            main_dict: RwLock::new(HashMap::new()),
            personal_dict: RwLock::new(HashMap::new()),
            context_map: RwLock::new(HashMap::new()),
        }
    }

    pub fn load_dictionary(&self, json_data: &str) -> Result<(), String> {
        let dict: HashMap<String, u32> =
            serde_json::from_str(json_data).map_err(|e| e.to_string())?;

        let mut main_trie = self.main_trie.write().unwrap();
        let mut main_dict = self.main_dict.write().unwrap();

        for (word, freq) in dict {
            let trimmed = word.trim().to_string();
            if !trimmed.is_empty() {
                main_trie.insert(&trimmed.to_lowercase(), freq);
                main_dict.insert(trimmed.to_lowercase(), freq);
            }
        }
        Ok(())
    }

    pub fn spell_check(&self, word: &str, context: &[String], max_suggestions: usize) -> SpellCheckResult {
        let normalized = word.to_lowercase();
        if normalized.is_empty() {
            return SpellCheckResult {
                is_valid: false,
                is_typo: false,
                suggestions: vec![],
            };
        }

        if self.is_known_word(&normalized) {
            return SpellCheckResult {
                is_valid: true,
                is_typo: false,
                suggestions: vec![],
            };
        }

        let suggestions = self.suggest_corrections(&normalized, context, max_suggestions);
        SpellCheckResult {
            is_valid: false,
            is_typo: true,
            suggestions,
        }
    }

    fn is_known_word(&self, word: &str) -> bool {
        let main = self.main_dict.read().unwrap();
        let personal = self.personal_dict.read().unwrap();

        let variants = [
            word.to_string(),
            capitalize(word),
            word.to_uppercase(),
        ];

        variants.iter().any(|v| main.contains_key(v) || personal.contains_key(v))
    }

    fn suggest_corrections(&self, word: &str, context: &[String], max: usize) -> Vec<String> {
        let mut heap: BinaryHeap<(i64, String)> = BinaryHeap::new();

        let main = self.main_dict.read().unwrap();
        let personal = self.personal_dict.read().unwrap();

        for (candidate, freq) in main.iter() {
            let dist = edit_distance(word, candidate);
            if dist <= MAX_EDIT_DISTANCE {
                let score = self.spelling_score(word, candidate, *freq, dist, 0.0, context);
                heap.push(((score * 10000.0) as i64, candidate.clone()));
            }
        }

        for (candidate, freq) in personal.iter() {
            let dist = edit_distance(word, candidate);
            if dist <= MAX_EDIT_DISTANCE {
                let score = self.spelling_score(word, candidate, *freq, dist, PERSONAL_BONUS, context);
                heap.push(((score * 10000.0) as i64, candidate.clone()));
            }
        }

        let mut results = Vec::with_capacity(max);
        let mut seen = std::collections::HashSet::new();
        while results.len() < max {
            match heap.pop() {
                Some((_, word)) if seen.insert(word.to_lowercase()) => results.push(word),
                Some(_) => continue,
                None => break,
            }
        }
        results
    }

    fn spelling_score(
        &self,
        input: &str,
        candidate: &str,
        freq: u32,
        dist: usize,
        bonus: f64,
        context: &[String],
    ) -> f64 {
        let freq_score = match freq {
            f if f >= 5000 => 1.0,
            f if f >= 1000 => 0.9,
            f if f >= 200 => 0.6,
            f if f >= 10 => 0.3,
            _ => 0.1,
        };
        let dist_score = (MAX_EDIT_DISTANCE - dist) as f64 / MAX_EDIT_DISTANCE as f64;
        let prefix_bonus = if candidate.starts_with(input) { 0.2 } else { 0.0 };
        let ctx_score = self.context_score(candidate, context);

        freq_score * 0.4 + dist_score * 0.4 + prefix_bonus * 0.2 + bonus + ctx_score * CONTEXT_BONUS
    }

    pub fn suggest(&self, prefix: &str, context: &[String], max_count: usize) -> Vec<Suggestion> {
        if prefix.len() < MIN_WORD_LENGTH {
            return vec![];
        }

        let normalized = prefix.to_lowercase();
        let mut suggestions: BinaryHeap<Suggestion> = BinaryHeap::new();

        self.collect_from_trie(&self.personal_trie.read().unwrap(), &normalized, prefix, context, PERSONAL_BONUS, &mut suggestions);
        self.collect_from_trie(&self.main_trie.read().unwrap(), &normalized, prefix, context, 0.0, &mut suggestions);

        let mut results = Vec::with_capacity(max_count);
        let mut seen = std::collections::HashSet::new();
        while results.len() < max_count {
            match suggestions.pop() {
                Some(s) if seen.insert(s.text.to_lowercase()) => results.push(s),
                Some(_) => continue,
                None => break,
            }
        }
        results
    }

    fn collect_from_trie(
        &self,
        trie: &TrieNode,
        normalized_prefix: &str,
        original_prefix: &str,
        context: &[String],
        bonus: f64,
        heap: &mut BinaryHeap<Suggestion>,
    ) {
        if let Some(node) = trie.search_prefix(normalized_prefix) {
            let mut words = Vec::new();
            node.collect_words(&mut words, 100);

            for (word, freq) in words {
                let display = format_case(&word, original_prefix);
                let mut conf = self.frequency_score(freq) * 0.6 + self.context_score(&word, context) * 0.2 + bonus;
                if word.eq_ignore_ascii_case(original_prefix) {
                    conf += EXACT_MATCH_BONUS;
                }

                heap.push(Suggestion {
                    text: display,
                    confidence: conf,
                    is_eligible_for_auto_commit: true,
                });
            }
        }
    }

    fn frequency_score(&self, freq: u32) -> f64 {
        match freq {
            f if f >= 5000 => 1.0,
            f if f >= 1000 => 0.9,
            f if f >= 500 => 0.75,
            f if f >= 100 => 0.5,
            f if f >= 10 => 0.25,
            _ => 0.1,
        }
    }

    fn context_score(&self, word: &str, context: &[String]) -> f64 {
        let ctx_map = self.context_map.read().unwrap();
        let mut score = 0.0;
        for (i, prev) in context.iter().enumerate() {
            if let Some(assoc) = ctx_map.get(prev) {
                if let Some(count) = assoc.get(word) {
                    let weight = 1.0 - i as f64 / context.len() as f64;
                    score += *count as f64 * weight;
                }
            }
        }
        score.min(1.0)
    }

    pub fn learn_word(&self, word: &str, context: &[String]) {
        let normalized = word.to_lowercase().trim().to_string();
        if normalized.len() < MIN_WORD_LENGTH {
            return;
        }

        {
            let mut personal = self.personal_dict.write().unwrap();
            let current = personal.get(&normalized).copied().unwrap_or(0);
            personal.insert(normalized.clone(), min(current + 5, MAX_PERSONAL_FREQ));
        }

        {
            let mut trie = self.personal_trie.write().unwrap();
            let freq = self.personal_dict.read().unwrap().get(&normalized).copied().unwrap_or(5);
            trie.insert(&normalized, freq);
        }

        {
            let mut ctx_map = self.context_map.write().unwrap();
            for prev in context.iter().take(MAX_CONTEXT_WORDS) {
                let assoc = ctx_map.entry(prev.to_lowercase()).or_default();
                let count = assoc.get(&normalized).copied().unwrap_or(0);
                assoc.insert(normalized.clone(), count + 1);
            }
        }
    }

    pub fn penalize_word(&self, word: &str) {
        let normalized = word.to_lowercase();
        let mut personal = self.personal_dict.write().unwrap();
        if let Some(freq) = personal.get_mut(&normalized) {
            *freq = ((*freq as f64) * DECAY_FACTOR) as u32;
            if *freq == 0 {
                personal.remove(&normalized);
            }
        }
    }

    pub fn remove_word(&self, word: &str) -> bool {
        let normalized = word.to_lowercase();
        let mut personal = self.personal_dict.write().unwrap();
        personal.remove(&normalized).is_some()
    }

    pub fn get_frequency(&self, word: &str) -> f64 {
        let normalized = word.to_lowercase();
        let main = self.main_dict.read().unwrap();
        let personal = self.personal_dict.read().unwrap();

        let main_freq = main.get(&normalized).copied().unwrap_or(0);
        let personal_freq = personal.get(&normalized).copied().unwrap_or(0) * 2;
        self.frequency_score(main_freq.max(personal_freq))
    }

    pub fn export_personal_dict(&self) -> String {
        let personal = self.personal_dict.read().unwrap();
        serde_json::to_string(&*personal).unwrap_or_default()
    }

    pub fn import_personal_dict(&self, json_data: &str) -> Result<(), String> {
        let dict: HashMap<String, u32> =
            serde_json::from_str(json_data).map_err(|e| e.to_string())?;

        let mut personal = self.personal_dict.write().unwrap();
        let mut trie = self.personal_trie.write().unwrap();

        for (word, freq) in dict {
            let normalized = word.to_lowercase();
            personal.insert(normalized.clone(), freq);
            trie.insert(&normalized, freq);
        }
        Ok(())
    }

    pub fn export_context_map(&self) -> String {
        let ctx = self.context_map.read().unwrap();
        serde_json::to_string(&*ctx).unwrap_or_default()
    }

    pub fn import_context_map(&self, json_data: &str) -> Result<(), String> {
        let ctx: HashMap<String, HashMap<String, u32>> =
            serde_json::from_str(json_data).map_err(|e| e.to_string())?;

        let mut context_map = self.context_map.write().unwrap();
        *context_map = ctx;
        Ok(())
    }

    pub fn clear(&self) {
        *self.main_trie.write().unwrap() = TrieNode::default();
        *self.personal_trie.write().unwrap() = TrieNode::default();
        self.main_dict.write().unwrap().clear();
        self.personal_dict.write().unwrap().clear();
        self.context_map.write().unwrap().clear();
    }
}

impl Default for NlpEngine {
    fn default() -> Self {
        Self::new()
    }
}

fn edit_distance(a: &str, b: &str) -> usize {
    let a: Vec<char> = a.chars().collect();
    let b: Vec<char> = b.chars().collect();
    let m = a.len();
    let n = b.len();

    if m == 0 {
        return n;
    }
    if n == 0 {
        return m;
    }

    let mut dp = vec![vec![0usize; n + 1]; m + 1];
    for i in 0..=m {
        dp[i][0] = i;
    }
    for j in 0..=n {
        dp[0][j] = j;
    }

    for i in 1..=m {
        for j in 1..=n {
            let cost = if a[i - 1] == b[j - 1] { 0 } else { 1 };
            dp[i][j] = min(min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
        }
    }
    dp[m][n]
}

fn capitalize(s: &str) -> String {
    let mut chars = s.chars();
    match chars.next() {
        None => String::new(),
        Some(first) => first.to_uppercase().collect::<String>() + chars.as_str(),
    }
}

fn format_case(word: &str, reference: &str) -> String {
    if reference.chars().all(|c| c.is_uppercase()) {
        word.to_uppercase()
    } else if reference.chars().next().map(|c| c.is_uppercase()).unwrap_or(false) {
        capitalize(word)
    } else {
        word.to_lowercase()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_edit_distance() {
        assert_eq!(edit_distance("kitten", "sitting"), 3);
        assert_eq!(edit_distance("hello", "hello"), 0);
        assert_eq!(edit_distance("", "abc"), 3);
    }

    #[test]
    fn test_trie_insert_and_search() {
        let mut trie = TrieNode::default();
        trie.insert("hello", 100);
        trie.insert("help", 50);
        trie.insert("world", 75);

        assert!(trie.search_prefix("hel").is_some());
        assert!(trie.search_prefix("wor").is_some());
        assert!(trie.search_prefix("xyz").is_none());
    }

    #[test]
    fn test_engine_load_and_suggest() {
        let engine = NlpEngine::new();
        let json = r#"{"hello": 1000, "help": 500, "world": 750}"#;
        engine.load_dictionary(json).unwrap();

        let suggestions = engine.suggest("hel", &[], 5);
        assert!(!suggestions.is_empty());
        assert!(suggestions.iter().any(|s| s.text.to_lowercase() == "hello" || s.text.to_lowercase() == "help"));
    }

    #[test]
    fn test_spell_check() {
        let engine = NlpEngine::new();
        let json = r#"{"hello": 1000, "world": 750}"#;
        engine.load_dictionary(json).unwrap();

        let result = engine.spell_check("hello", &[], 3);
        assert!(result.is_valid);

        let result = engine.spell_check("helo", &[], 3);
        assert!(result.is_typo);
        assert!(!result.suggestions.is_empty());
    }

    #[test]
    fn test_learn_word() {
        let engine = NlpEngine::new();
        engine.learn_word("floris", &["hello".to_string()]);

        let personal = engine.personal_dict.read().unwrap();
        assert!(personal.contains_key("floris"));
    }
}
