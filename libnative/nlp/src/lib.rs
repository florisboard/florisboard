pub mod binary_trie;

use binary_trie::{BinaryTrie, is_contraction, is_acronym, normalize_for_lookup, should_preserve_canonical};
use serde::{Deserialize, Serialize};
use std::cmp::{min, Ordering};
use std::collections::{BinaryHeap, HashMap};
use std::sync::RwLock;

const MIN_WORD_LENGTH: usize = 2;
const MAX_EDIT_DISTANCE: usize = 2;
const MAX_CONTEXT_WORDS: usize = 3;
const PERSONAL_BONUS: f64 = 0.25;
const CONTEXT_BONUS: f64 = 0.15;
const EXACT_MATCH_BONUS: f64 = 1.0;
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

pub struct LanguageDictionary {
    trie: TrieNode,
    binary_trie: Option<BinaryTrie>,
    dict: HashMap<String, u32>,
    ngrams: HashMap<String, HashMap<String, u32>>,
    canonical_forms: HashMap<String, String>,
}

impl Default for LanguageDictionary {
    fn default() -> Self {
        Self {
            trie: TrieNode::default(),
            binary_trie: None,
            dict: HashMap::new(),
            ngrams: HashMap::new(),
            canonical_forms: HashMap::new(),
        }
    }
}

pub struct NlpEngine {
    languages: RwLock<HashMap<String, LanguageDictionary>>,
    active_language: RwLock<String>,
    personal_trie: RwLock<TrieNode>,
    personal_dict: RwLock<HashMap<String, u32>>,
    context_map: RwLock<HashMap<String, HashMap<String, u32>>>,
}

impl NlpEngine {
    pub fn new() -> Self {
        Self {
            languages: RwLock::new(HashMap::new()),
            active_language: RwLock::new(String::from("en_US")),
            personal_trie: RwLock::new(TrieNode::default()),
            personal_dict: RwLock::new(HashMap::new()),
            context_map: RwLock::new(HashMap::new()),
        }
    }

    pub fn set_language(&self, lang_code: &str) {
        *self.active_language.write().unwrap() = lang_code.to_string();
    }

    pub fn get_language(&self) -> String {
        self.active_language.read().unwrap().clone()
    }

    pub fn get_canonical_form(&self, word: &str) -> Option<String> {
        let lang = self.active_language.read().unwrap();
        let languages = self.languages.read().unwrap();
        languages.get(&*lang)
            .and_then(|ld| ld.canonical_forms.get(&normalize_for_lookup(word)).cloned())
    }

    fn get_active_dict(&self) -> (HashMap<String, u32>, Option<BinaryTrie>, HashMap<String, String>) {
        let lang = self.active_language.read().unwrap();
        let languages = self.languages.read().unwrap();
        if let Some(ld) = languages.get(&*lang) {
            (ld.dict.clone(), ld.binary_trie.clone(), ld.canonical_forms.clone())
        } else {
            (HashMap::new(), None, HashMap::new())
        }
    }

    pub fn load_dictionary_binary(&self, data: &[u8]) -> Result<(), String> {
        self.load_dictionary_binary_for_language("en_US", data)
    }

    pub fn load_dictionary_binary_for_language(&self, lang_code: &str, data: &[u8]) -> Result<(), String> {
        let trie = BinaryTrie::deserialize(data).map_err(|e| e.to_string())?;
        
        let canonical_forms_from_trie = trie.canonical_forms().clone();
        
        let mut dict = HashMap::new();
        let mut words = Vec::new();
        trie.collect_words(0, "", &mut words, 100000);
        for (word, freq) in words {
            let lower = word.to_lowercase();
            dict.insert(lower, freq as u32);
        }
        
        let mut languages = self.languages.write().unwrap();
        let (existing_ngrams, existing_canonical) = languages.get(lang_code)
            .map(|ld| (ld.ngrams.clone(), ld.canonical_forms.clone()))
            .unwrap_or_default();
        
        let mut merged_canonical = existing_canonical;
        merged_canonical.extend(canonical_forms_from_trie);
        
        languages.insert(lang_code.to_string(), LanguageDictionary {
            trie: TrieNode::default(),
            binary_trie: Some(trie),
            dict,
            ngrams: existing_ngrams,
            canonical_forms: merged_canonical,
        });
        Ok(())
    }

    pub fn load_dictionary(&self, json_data: &str) -> Result<(), String> {
        self.load_dictionary_for_language("en_US", json_data)
    }

    pub fn load_dictionary_for_language(&self, lang_code: &str, json_data: &str) -> Result<(), String> {
        let dict: HashMap<String, u32> =
            serde_json::from_str(json_data).map_err(|e| e.to_string())?;

        let mut trie = TrieNode::default();
        let mut dict_copy = HashMap::new();
        let mut canonical_forms = HashMap::new();

        for (word, freq) in dict {
            let trimmed = word.trim().to_string();
            if !trimmed.is_empty() {
                let lower = trimmed.to_lowercase();
                trie.insert(&lower, freq);
                dict_copy.insert(lower.clone(), freq);
                if should_preserve_canonical(&trimmed) {
                    let normalized_key = normalize_for_lookup(&trimmed);
                    canonical_forms.insert(normalized_key, trimmed);
                }
            }
        }

        let mut languages = self.languages.write().unwrap();
        let (existing_ngrams, existing_canonical) = languages.get(lang_code)
            .map(|ld| (ld.ngrams.clone(), ld.canonical_forms.clone()))
            .unwrap_or_default();
        
        let mut merged_canonical = existing_canonical;
        merged_canonical.extend(canonical_forms);
        
        languages.insert(lang_code.to_string(), LanguageDictionary {
            trie,
            binary_trie: None,
            dict: dict_copy,
            ngrams: existing_ngrams,
            canonical_forms: merged_canonical,
        });
        Ok(())
    }

    pub fn load_ngrams_for_language(&self, lang_code: &str, json_data: &str) -> Result<(), String> {
        let ngrams: HashMap<String, HashMap<String, u32>> =
            serde_json::from_str(json_data).map_err(|e| e.to_string())?;

        let mut languages = self.languages.write().unwrap();
        if let Some(ld) = languages.get_mut(lang_code) {
            ld.ngrams = ngrams;
        } else {
            languages.insert(lang_code.to_string(), LanguageDictionary {
                ngrams,
                ..Default::default()
            });
        }
        Ok(())
    }

    fn get_active_ngrams(&self) -> HashMap<String, HashMap<String, u32>> {
        let lang = self.active_language.read().unwrap();
        let languages = self.languages.read().unwrap();
        languages.get(&*lang)
            .map(|ld| ld.ngrams.clone())
            .unwrap_or_default()
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

        if let Some(canonical) = self.get_canonical_form(&normalized) {
            return SpellCheckResult {
                is_valid: false,
                is_typo: true,
                suggestions: vec![canonical],
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
        let (main_dict, _, _) = self.get_active_dict();
        let personal = self.personal_dict.read().unwrap();

        let variants = [
            word.to_string(),
            capitalize(word),
            word.to_uppercase(),
        ];

        variants.iter().any(|v| main_dict.contains_key(v) || personal.contains_key(v))
    }

    fn suggest_corrections(&self, word: &str, context: &[String], max: usize) -> Vec<String> {
        let mut heap: BinaryHeap<(i64, String)> = BinaryHeap::new();

        let (main_dict, _, canonical_forms) = self.get_active_dict();
        let personal = self.personal_dict.read().unwrap();

        let normalized_key = normalize_for_lookup(word);
        if let Some(canonical) = canonical_forms.get(&normalized_key) {
            heap.push((10000000, canonical.clone()));
        }

        for (candidate, freq) in main_dict.iter() {
            let dist = edit_distance(word, candidate);
            if dist <= MAX_EDIT_DISTANCE {
                let score = self.spelling_score(word, candidate, *freq, dist, 0.0, context);
                let display = format_with_canonical(candidate, word, &canonical_forms);
                heap.push(((score * 10000.0) as i64, display));
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
                Some((_, w)) if seen.insert(normalize_for_lookup(&w)) => results.push(w),
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

        let (main_dict, binary_trie, canonical_forms) = self.get_active_dict();
        
        let typed_is_valid_word = {
            let personal = self.personal_dict.read().unwrap();
            main_dict.contains_key(&normalized) || personal.contains_key(&normalized)
        };

        // Check for direct canonical form match (e.g., "im" -> "I'm", "usa" -> "USA")
        let normalized_key = normalize_for_lookup(&normalized);
        if let Some(canonical) = canonical_forms.get(&normalized_key) {
            let dict_key = canonical.to_lowercase();
            let freq = main_dict.get(&dict_key).copied().unwrap_or(200) as u32;
            let conf = self.frequency_score(freq) + 0.3 + EXACT_MATCH_BONUS; // High priority for exact canonical match
            suggestions.push(Suggestion {
                text: canonical.clone(),
                confidence: conf,
                is_eligible_for_auto_commit: true, // Canonical forms should auto-commit
            });
        }

        self.collect_from_trie(&self.personal_trie.read().unwrap(), &normalized, prefix, context, PERSONAL_BONUS, typed_is_valid_word, &canonical_forms, &mut suggestions);
        
        if let Some(ref bt) = binary_trie {
            self.collect_from_binary_trie(bt, &normalized, prefix, context, typed_is_valid_word, &canonical_forms, &mut suggestions);
        } else {
            let languages = self.languages.read().unwrap();
            let lang = self.active_language.read().unwrap();
            if let Some(ld) = languages.get(&*lang) {
                self.collect_from_trie(&ld.trie, &normalized, prefix, context, 0.0, typed_is_valid_word, &canonical_forms, &mut suggestions);
            }
        }

        if !typed_is_valid_word {
            self.collect_typo_corrections(&normalized, prefix, context, &main_dict, &canonical_forms, &mut suggestions);
        }

        let mut results = Vec::with_capacity(max_count);
        let mut seen = std::collections::HashSet::new();
        while results.len() < max_count {
            match suggestions.pop() {
                Some(s) if seen.insert(normalize_for_lookup(&s.text)) => results.push(s),
                Some(_) => continue,
                None => break,
            }
        }
        results
    }

    fn collect_typo_corrections(
        &self,
        normalized_input: &str,
        original_input: &str,
        context: &[String],
        main_dict: &HashMap<String, u32>,
        canonical_forms: &HashMap<String, String>,
        heap: &mut BinaryHeap<Suggestion>,
    ) {
        if normalized_input.len() < 2 {
            return;
        }

        let personal = self.personal_dict.read().unwrap();

        for (candidate, freq) in main_dict.iter() {
            if candidate.starts_with(normalized_input) {
                continue;
            }

            let dist = edit_distance(normalized_input, candidate);
            if dist > 0 && dist <= MAX_EDIT_DISTANCE {
                let display = format_with_canonical(candidate, original_input, canonical_forms);
                let conf = self.typo_correction_score(*freq, dist, context, candidate);
                let auto_commit = conf >= 0.65 && *freq >= 100 && dist <= 1;
                heap.push(Suggestion {
                    text: display,
                    confidence: conf,
                    is_eligible_for_auto_commit: auto_commit,
                });
            }
        }

        for (candidate, freq) in personal.iter() {
            if candidate.starts_with(normalized_input) {
                continue;
            }

            let dist = edit_distance(normalized_input, candidate);
            if dist > 0 && dist <= MAX_EDIT_DISTANCE {
                let display = format_with_canonical(candidate, original_input, canonical_forms);
                let conf = self.typo_correction_score(*freq, dist, context, candidate) + PERSONAL_BONUS;
                let auto_commit = conf >= 0.65 && *freq >= 50 && dist <= 1;
                heap.push(Suggestion {
                    text: display,
                    confidence: conf,
                    is_eligible_for_auto_commit: auto_commit,
                });
            }
        }
    }

    fn typo_correction_score(&self, freq: u32, dist: usize, context: &[String], word: &str) -> f64 {
        let freq_score = self.frequency_score(freq);
        let dist_penalty = match dist {
            1 => 0.0,
            2 => 0.2,
            _ => 0.4,
        };
        let ctx_score = self.context_score(word, context);
        
        // Higher weight on frequency, lower threshold for common words
        freq_score * 0.7 + ctx_score * 0.2 - dist_penalty
    }

    fn collect_from_binary_trie(
        &self,
        trie: &BinaryTrie,
        normalized_prefix: &str,
        original_prefix: &str,
        context: &[String],
        typed_is_valid_word: bool,
        canonical_forms: &HashMap<String, String>,
        heap: &mut BinaryHeap<Suggestion>,
    ) {
        if let Some(idx) = trie.search_prefix(normalized_prefix) {
            let mut words = Vec::new();
            trie.collect_words(idx, normalized_prefix, &mut words, 100);

            for (word, freq) in words {
                let display = format_with_canonical(&word, original_prefix, canonical_forms);
                let is_exact_match = word.eq_ignore_ascii_case(original_prefix);
                
                let prefix_bonus = 0.3;
                let mut conf = self.frequency_score(freq as u32) * 0.6 + self.context_score(&word, context) * 0.2 + prefix_bonus;
                if is_exact_match {
                    conf += EXACT_MATCH_BONUS;
                }

                let auto_commit = !typed_is_valid_word && !is_exact_match && conf >= 0.7 && freq >= 100;

                heap.push(Suggestion {
                    text: display,
                    confidence: conf,
                    is_eligible_for_auto_commit: auto_commit,
                });
            }
        }
    }

    fn collect_from_trie(
        &self,
        trie: &TrieNode,
        normalized_prefix: &str,
        original_prefix: &str,
        context: &[String],
        bonus: f64,
        typed_is_valid_word: bool,
        canonical_forms: &HashMap<String, String>,
        heap: &mut BinaryHeap<Suggestion>,
    ) {
        if let Some(node) = trie.search_prefix(normalized_prefix) {
            let mut words = Vec::new();
            node.collect_words(&mut words, 100);

            for (word, freq) in words {
                let display = format_with_canonical(&word, original_prefix, canonical_forms);
                let is_exact_match = word.eq_ignore_ascii_case(original_prefix);
                
                let prefix_bonus = 0.3;
                let mut conf = self.frequency_score(freq) * 0.6 + self.context_score(&word, context) * 0.2 + bonus + prefix_bonus;
                if is_exact_match {
                    conf += EXACT_MATCH_BONUS;
                }

                let auto_commit = !typed_is_valid_word && !is_exact_match && conf >= 0.7 && freq >= 100;

                heap.push(Suggestion {
                    text: display,
                    confidence: conf,
                    is_eligible_for_auto_commit: auto_commit,
                });
            }
        }
    }

    fn frequency_score(&self, freq: u32) -> f64 {
        // Our dictionary uses 0-255 range after normalization
        match freq {
            f if f >= 250 => 1.0,    // Top-tier words (the, and, is, etc.)
            f if f >= 200 => 0.9,    // Very common words
            f if f >= 150 => 0.8,    // Common words
            f if f >= 100 => 0.7,    // Moderately common
            f if f >= 50 => 0.5,     // Less common
            f if f >= 10 => 0.3,     // Rare
            _ => 0.1,                // Very rare
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

    pub fn predict_next_word(&self, context: &[String], max_count: usize) -> Vec<Suggestion> {
        if context.is_empty() {
            return vec![];
        }

        let ctx_map = self.context_map.read().unwrap();
        let ngrams = self.get_active_ngrams();
        let (_, _, canonical_forms) = self.get_active_dict();
        let mut candidates: Vec<(String, u32)> = Vec::new();

        let context_set: std::collections::HashSet<String> = 
            context.iter().map(|w| w.to_lowercase()).collect();

        let add_candidate = |candidates: &mut Vec<(String, u32)>, word: &str, score: u32, context_set: &std::collections::HashSet<String>, canonical_forms: &HashMap<String, String>| {
            if context_set.contains(&word.to_lowercase()) {
                return;
            }
            let display = canonical_forms.get(&normalize_for_lookup(word))
                .cloned()
                .unwrap_or_else(|| word.to_string());
            if let Some(existing) = candidates.iter_mut().find(|(w, _)| normalize_for_lookup(w) == normalize_for_lookup(&display)) {
                existing.1 += score;
            } else {
                candidates.push((display, score));
            }
        };

        for (i, prev_word) in context.iter().rev().enumerate() {
            let weight = (context.len() - i) as u32;
            let prev_lower = prev_word.to_lowercase();
            
            if let Some(following_words) = ctx_map.get(&prev_lower) {
                for (word, freq) in following_words {
                    add_candidate(&mut candidates, word, freq * weight * 10, &context_set, &canonical_forms);
                }
            }
            
            if let Some(following_words) = ngrams.get(&prev_lower) {
                for (word, freq) in following_words {
                    add_candidate(&mut candidates, word, freq * weight, &context_set, &canonical_forms);
                }
            }
        }

        if candidates.is_empty() {
            return vec![];
        }

        candidates.sort_by(|a, b| b.1.cmp(&a.1));
        candidates.truncate(max_count);

        let max_freq = candidates.first().map(|(_, f)| *f).unwrap_or(1) as f64;

        candidates
            .into_iter()
            .map(|(word, freq)| {
                let confidence = (freq as f64 / max_freq).min(1.0);
                Suggestion {
                    text: word,
                    confidence,
                    is_eligible_for_auto_commit: false,
                }
            })
            .collect()
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
        let (main_dict, _, _) = self.get_active_dict();
        let personal = self.personal_dict.read().unwrap();

        let main_freq = main_dict.get(&normalized).copied().unwrap_or(0);
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
        *self.personal_trie.write().unwrap() = TrieNode::default();
        self.personal_dict.write().unwrap().clear();
        self.context_map.write().unwrap().clear();
        self.languages.write().unwrap().clear();
        *self.active_language.write().unwrap() = String::from("en_US");
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
            
            if i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1] {
                dp[i][j] = min(dp[i][j], dp[i - 2][j - 2] + 1);
            }
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

fn format_with_canonical(word: &str, reference: &str, canonical_forms: &HashMap<String, String>) -> String {
    let normalized_key = normalize_for_lookup(word);
    if let Some(canonical) = canonical_forms.get(&normalized_key) {
        if is_contraction(canonical) || is_acronym(canonical) {
            return canonical.clone();
        }
    }
    format_case(word, reference)
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::binary_trie::is_proper_noun;

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

    #[test]
    fn test_predict_next_word() {
        let engine = NlpEngine::new();
        engine.learn_word("are", &["how".to_string()]);
        engine.learn_word("are", &["how".to_string()]);
        engine.learn_word("you", &["are".to_string()]);
        engine.learn_word("doing", &["you".to_string()]);

        let predictions = engine.predict_next_word(&["how".to_string()], 5);
        assert!(!predictions.is_empty());
        assert_eq!(predictions[0].text.to_lowercase(), "are");

        let predictions = engine.predict_next_word(&["how".to_string(), "are".to_string()], 5);
        assert!(predictions.iter().any(|s| s.text.to_lowercase() == "you"));
    }

    #[test]
    fn test_ngrams_prediction() {
        let engine = NlpEngine::new();
        let ngrams = r#"{"how": {"are": 100, "is": 90}, "good": {"morning": 80, "night": 70}}"#;
        engine.load_ngrams_for_language("en_US", ngrams).unwrap();
        engine.set_language("en_US");

        let predictions = engine.predict_next_word(&["how".to_string()], 5);
        assert!(!predictions.is_empty());
        assert!(predictions.iter().any(|s| s.text == "are"));

        let predictions = engine.predict_next_word(&["good".to_string()], 5);
        assert!(predictions.iter().any(|s| s.text == "morning"));
    }

    #[test]
    fn test_learned_beats_ngrams() {
        let engine = NlpEngine::new();
        let ngrams = r#"{"how": {"is": 10}}"#;
        engine.load_ngrams_for_language("en_US", ngrams).unwrap();
        engine.set_language("en_US");

        for _ in 0..5 {
            engine.learn_word("are", &["how".to_string()]);
        }

        let predictions = engine.predict_next_word(&["how".to_string()], 5);
        assert!(!predictions.is_empty());
        assert_eq!(predictions[0].text.to_lowercase(), "are");
    }

    #[test]
    fn test_canonical_form_detection() {
        assert!(is_contraction("I'm"));
        assert!(is_contraction("don't"));
        assert!(!is_contraction("hello"));
        
        assert!(is_acronym("USA"));
        assert!(is_acronym("NASA"));
        assert!(!is_acronym("Hello"));
        
        assert!(is_proper_noun("Europe"));
        assert!(!is_proper_noun("hello"));
        assert!(!is_proper_noun("USA"));
    }

    #[test]
    fn test_canonical_form_storage() {
        let engine = NlpEngine::new();
        let json = r#"{"I'm": 200, "don't": 180, "USA": 150, "hello": 100}"#;
        engine.load_dictionary(json).unwrap();

        assert_eq!(engine.get_canonical_form("im"), Some("I'm".to_string()));
        assert_eq!(engine.get_canonical_form("dont"), Some("don't".to_string()));
        assert_eq!(engine.get_canonical_form("usa"), Some("USA".to_string()));
        assert_eq!(engine.get_canonical_form("hello"), None);
    }

    #[test]
    fn test_suggest_returns_canonical_forms() {
        let engine = NlpEngine::new();
        let json = r#"{"I'm": 200, "don't": 180, "USA": 150, "hello": 100, "image": 90}"#;
        engine.load_dictionary(json).unwrap();

        let suggestions = engine.suggest("im", &[], 5);
        let texts: Vec<&str> = suggestions.iter().map(|s| s.text.as_str()).collect();
        assert!(texts.contains(&"I'm"), "Expected I'm in suggestions: {:?}", texts);

        let suggestions = engine.suggest("us", &[], 5);
        let texts: Vec<&str> = suggestions.iter().map(|s| s.text.as_str()).collect();
        assert!(texts.contains(&"USA"), "Expected USA in suggestions: {:?}", texts);

        let suggestions = engine.suggest("don", &[], 5);
        let texts: Vec<&str> = suggestions.iter().map(|s| s.text.as_str()).collect();
        assert!(texts.contains(&"don't"), "Expected don't in suggestions: {:?}", texts);
    }

    #[test]
    fn test_spell_check_contractions() {
        let engine = NlpEngine::new();
        let json = r#"{"I'm": 200, "don't": 180, "USA": 150, "hello": 100}"#;
        engine.load_dictionary(json).unwrap();

        // "im" should suggest "I'm"
        let result = engine.spell_check("im", &[], 3);
        assert!(result.is_typo, "im should be typo");
        assert!(result.suggestions.contains(&"I'm".to_string()), 
            "Expected I'm in suggestions: {:?}", result.suggestions);

        // "dont" should suggest "don't"
        let result = engine.spell_check("dont", &[], 3);
        assert!(result.is_typo, "dont should be typo");
        assert!(result.suggestions.contains(&"don't".to_string()),
            "Expected don't in suggestions: {:?}", result.suggestions);

        // "usa" is valid (maps to USA via uppercase check) - suggest() handles formatting
        let result = engine.spell_check("usa", &[], 3);
        assert!(result.is_valid, "usa should be valid (maps to USA)");

        // "hello" should be valid
        let result = engine.spell_check("hello", &[], 3);
        assert!(result.is_valid);
    }
}
