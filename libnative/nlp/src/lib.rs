pub mod binary_trie;

use binary_trie::{BinaryTrie, is_contraction, is_acronym, normalize_for_lookup};
use serde::{Deserialize, Serialize};
use std::cmp::{min, Ordering};
use std::collections::{BinaryHeap, HashMap};
use std::sync::{RwLock, RwLockReadGuard};

const MIN_WORD_LENGTH: usize = 2;
const MAX_EDIT_DISTANCE: usize = 2;
const MAX_CONTEXT_WORDS: usize = 3;
const PERSONAL_BONUS: f64 = 0.25;
const CONTEXT_BONUS: f64 = 0.15;
const EXACT_MATCH_BONUS: f64 = 1.0;
const DECAY_FACTOR: f64 = 0.95;
const MAX_PERSONAL_FREQ: u32 = 255;

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

#[derive(Default)]
pub struct LanguageDictionary {
    binary_trie: Option<BinaryTrie>,
    ngrams: HashMap<String, HashMap<String, u32>>,
    canonical_forms: HashMap<String, String>,
}

impl LanguageDictionary {
    fn frequency(&self, word: &str) -> Option<u32> {
        self.binary_trie.as_ref()?.frequency(word).map(u32::from)
    }

    fn for_each_word(&self, max_len: usize, mut f: impl FnMut(&str, u32)) {
        if let Some(trie) = &self.binary_trie {
            trie.for_each_word(max_len, |word, freq| f(word, freq.into()));
        }
    }
}

// Holds both read locks for as long as it lives. Taking another one in the same call
// can deadlock while a writer is waiting.
struct ActiveDicts<'a> {
    langs: RwLockReadGuard<'a, Vec<String>>,
    languages: RwLockReadGuard<'a, HashMap<String, LanguageDictionary>>,
}

impl ActiveDicts<'_> {
    fn dicts(&self) -> impl Iterator<Item = &LanguageDictionary> {
        self.langs.iter().filter_map(|lang| self.languages.get(lang))
    }

    fn frequency(&self, word: &str) -> Option<u32> {
        self.dicts().filter_map(|ld| ld.frequency(word)).max()
    }

    fn canonical(&self, key: &str) -> Option<&String> {
        self.dicts().filter_map(|ld| ld.canonical_forms.get(key)).last()
    }

    fn for_each_word(&self, max_len: usize, mut f: impl FnMut(&str, u32)) {
        for ld in self.dicts() {
            ld.for_each_word(max_len, &mut f);
        }
    }
}

pub struct NlpEngine {
    languages: RwLock<HashMap<String, LanguageDictionary>>,
    active_languages: RwLock<Vec<String>>,
    personal_dict: RwLock<HashMap<String, u32>>,
    context_map: RwLock<HashMap<String, HashMap<String, u32>>>,
}

impl NlpEngine {
    pub fn new() -> Self {
        Self {
            languages: RwLock::new(HashMap::new()),
            active_languages: RwLock::new(vec![String::from("en_US")]),
            personal_dict: RwLock::new(HashMap::new()),
            context_map: RwLock::new(HashMap::new()),
        }
    }

    pub fn set_languages(&self, lang_codes: &[&str]) {
        *self.active_languages.write().unwrap() = lang_codes.iter().map(|s| s.to_string()).collect();
    }

    pub fn get_canonical_form(&self, word: &str) -> Option<String> {
        self.active_dicts().canonical(&normalize_for_lookup(word)).cloned()
    }

    fn active_dicts(&self) -> ActiveDicts<'_> {
        ActiveDicts {
            langs: self.active_languages.read().unwrap(),
            languages: self.languages.read().unwrap(),
        }
    }

    fn get_active_ngrams(&self) -> HashMap<String, HashMap<String, u32>> {
        let langs = self.active_languages.read().unwrap();
        let languages = self.languages.read().unwrap();
        let mut merged = HashMap::new();
        for lang in langs.iter() {
            if let Some(ld) = languages.get(lang) {
                for (key, followers) in &ld.ngrams {
                    let entry = merged.entry(key.clone()).or_insert_with(HashMap::new);
                    for (word, freq) in followers {
                        entry.entry(word.clone())
                            .and_modify(|f: &mut u32| *f = (*f).max(*freq))
                            .or_insert(*freq);
                    }
                }
            }
        }
        merged
    }

    pub fn load_dictionary_binary_for_language(&self, lang_code: &str, data: &[u8]) -> Result<(), String> {
        let mut trie = BinaryTrie::deserialize(data).map_err(|e| e.to_string())?;
        let canonical_forms_from_trie = trie.take_canonical_forms();

        let mut languages = self.languages.write().unwrap();
        let (existing_ngrams, existing_canonical) = languages.get(lang_code)
            .map(|ld| (ld.ngrams.clone(), ld.canonical_forms.clone()))
            .unwrap_or_default();
        
        let mut merged_canonical = existing_canonical;
        merged_canonical.extend(canonical_forms_from_trie);
        
        languages.insert(lang_code.to_string(), LanguageDictionary {
            binary_trie: Some(trie),
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
        self.active_dicts().frequency(word).is_some() || self.personal_dict.read().unwrap().contains_key(word)
    }

    fn suggest_corrections(&self, word: &str, context: &[String], max: usize) -> Vec<String> {
        let mut heap: BinaryHeap<(i64, String)> = BinaryHeap::new();

        let active = self.active_dicts();
        let personal = self.personal_dict.read().unwrap();

        let normalized_key = normalize_for_lookup(word);
        if let Some(canonical) = active.canonical(&normalized_key) {
            heap.push((10000000, canonical.clone()));
        }

        active.for_each_word(word.chars().count() + MAX_EDIT_DISTANCE, |candidate, freq| {
            if let Some(dist) = edit_distance(word, candidate, MAX_EDIT_DISTANCE) {
                let score = self.spelling_score(word, candidate, freq, dist, 0.0, context);
                let display = format_with_canonical(candidate, word, &active);
                heap.push(((score * 10000.0) as i64, display));
            }
        });

        for (candidate, freq) in personal.iter() {
            if let Some(dist) = edit_distance(word, candidate, MAX_EDIT_DISTANCE) {
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
        let freq_score = self.frequency_score(freq);
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

        let active = self.active_dicts();

        let typed_is_valid_word = {
            let personal = self.personal_dict.read().unwrap();
            active.frequency(&normalized).is_some() || personal.contains_key(&normalized)
        };

        let normalized_key = normalize_for_lookup(&normalized);
        if let Some(canonical) = active.canonical(&normalized_key) {
            let dict_key = canonical.to_lowercase();
            let freq = active.frequency(&dict_key).unwrap_or(200);
            let conf = self.frequency_score(freq) + 0.3 + EXACT_MATCH_BONUS;
            let display_text = format_with_canonical(canonical, prefix, &active);
            suggestions.push(Suggestion {
                text: display_text,
                confidence: conf,
                is_eligible_for_auto_commit: true,
            });
        }

        let personal_matches: Vec<(String, u32)> = self.personal_dict.read().unwrap()
            .iter()
            .filter(|(word, _)| word.starts_with(&normalized))
            .map(|(word, freq)| (word.clone(), *freq))
            .collect();
        self.collect_prefix_matches(personal_matches, prefix, context, PERSONAL_BONUS, typed_is_valid_word, &active, &mut suggestions);

        for bt in active.dicts().filter_map(|ld| ld.binary_trie.as_ref()) {
            self.collect_from_binary_trie(bt, &normalized, prefix, context, typed_is_valid_word, &active, &mut suggestions);
        }

        if !typed_is_valid_word {
            self.collect_typo_corrections(&normalized, prefix, context, &active, &mut suggestions);
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
        active: &ActiveDicts,
        heap: &mut BinaryHeap<Suggestion>,
    ) {
        if normalized_input.len() < 2 {
            return;
        }

        let personal = self.personal_dict.read().unwrap();

        let mut consider = |candidate: &str, freq: u32, bonus: f64, min_auto_commit_freq: u32| {
            if candidate.starts_with(normalized_input) {
                return;
            }

            if let Some(dist) = edit_distance(normalized_input, candidate, MAX_EDIT_DISTANCE).filter(|&dist| dist > 0) {
                let display = format_with_canonical(candidate, original_input, active);
                let conf = self.typo_correction_score(freq, dist, context, candidate) + bonus;
                let auto_commit = conf >= 0.65 && freq >= min_auto_commit_freq && dist <= 1;
                heap.push(Suggestion {
                    text: display,
                    confidence: conf,
                    is_eligible_for_auto_commit: auto_commit,
                });
            }
        };

        let max_len = normalized_input.chars().count() + MAX_EDIT_DISTANCE;
        active.for_each_word(max_len, |candidate, freq| consider(candidate, freq, 0.0, 100));
        for (candidate, freq) in personal.iter() {
            consider(candidate, *freq, PERSONAL_BONUS, 50);
        }
    }

    fn typo_correction_score(&self, freq: u32, dist: usize, context: &[String], word: &str) -> f64 {
        let freq_score = self.frequency_score(freq);
        let dist_penalty = if dist == 1 { 0.0 } else { 0.2 };
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
        active: &ActiveDicts,
        heap: &mut BinaryHeap<Suggestion>,
    ) {
        if let Some(idx) = trie.search_prefix(normalized_prefix) {
            let mut words = Vec::new();
            trie.collect_words(idx, normalized_prefix, &mut words, usize::MAX);
            words.sort_by(|a, b| b.1.cmp(&a.1));
            words.truncate(100);
            let words = words.into_iter().map(|(word, freq)| (word, u32::from(freq))).collect();
            self.collect_prefix_matches(words, original_prefix, context, 0.0, typed_is_valid_word, active, heap);
        }
    }

    fn collect_prefix_matches(
        &self,
        words: Vec<(String, u32)>,
        original_prefix: &str,
        context: &[String],
        bonus: f64,
        typed_is_valid_word: bool,
        active: &ActiveDicts,
        heap: &mut BinaryHeap<Suggestion>,
    ) {
        for (word, freq) in words {
            let display = format_with_canonical(&word, original_prefix, active);
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
        let active = self.active_dicts();
        let mut candidates: Vec<(String, u32)> = Vec::new();

        let context_set: std::collections::HashSet<String> =
            context.iter().map(|w| w.to_lowercase()).collect();

        let add_candidate = |candidates: &mut Vec<(String, u32)>, word: &str, score: u32, context_set: &std::collections::HashSet<String>, active: &ActiveDicts| {
            if context_set.contains(&word.to_lowercase()) {
                return;
            }
            let display = active.canonical(&normalize_for_lookup(word))
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
                    add_candidate(&mut candidates, word, freq * weight * 10, &context_set, &active);
                }
            }
            
            if let Some(following_words) = ngrams.get(&prev_lower) {
                for (word, freq) in following_words {
                    add_candidate(&mut candidates, word, freq * weight, &context_set, &active);
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
        let mut removed = self.personal_dict.write().unwrap().remove(&normalized).is_some();

        let mut ctx_map = self.context_map.write().unwrap();
        for followers in ctx_map.values_mut() {
            removed |= followers.remove(&normalized).is_some();
        }
        ctx_map.retain(|_, followers| !followers.is_empty());
        removed
    }

    pub fn get_frequency(&self, word: &str) -> f64 {
        let normalized = word.to_lowercase();
        let active = self.active_dicts();
        let personal = self.personal_dict.read().unwrap();

        let main_freq = active.frequency(&normalized).unwrap_or(0);
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
        for (word, freq) in dict {
            personal.insert(word.to_lowercase(), freq);
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
        self.personal_dict.write().unwrap().clear();
        self.context_map.write().unwrap().clear();
        self.languages.write().unwrap().clear();
        *self.active_languages.write().unwrap() = vec![String::from("en_US")];
    }
}

impl Default for NlpEngine {
    fn default() -> Self {
        Self::new()
    }
}

// Returns the edit distance only when it's at most `max`. Once two consecutive rows are
// both over `max`, no later row can come back under it, so the scan stops early.
fn edit_distance(a: &str, b: &str, max: usize) -> Option<usize> {
    if a.chars().count().abs_diff(b.chars().count()) > max {
        return None;
    }
    let a: Vec<char> = a.chars().collect();
    let b: Vec<char> = b.chars().collect();
    let n = b.len();

    let mut before_prev = vec![0usize; n + 1];
    let mut prev: Vec<usize> = (0..=n).collect();
    let mut curr = vec![0usize; n + 1];
    for i in 1..=a.len() {
        curr[0] = i;
        for j in 1..=n {
            let cost = if a[i - 1] == b[j - 1] { 0 } else { 1 };
            curr[j] = min(min(prev[j] + 1, curr[j - 1] + 1), prev[j - 1] + cost);
            if i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1] {
                curr[j] = min(curr[j], before_prev[j - 2] + 1);
            }
        }
        if curr.iter().all(|&dist| dist > max) && prev.iter().all(|&dist| dist > max) {
            return None;
        }
        std::mem::swap(&mut before_prev, &mut prev);
        std::mem::swap(&mut prev, &mut curr);
    }
    Some(prev[n]).filter(|&dist| dist <= max)
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

fn format_with_canonical(word: &str, reference: &str, active: &ActiveDicts) -> String {
    let normalized_key = normalize_for_lookup(word);
    if let Some(canonical) = active.canonical(&normalized_key) {
        // Always preserve contractions (e.g., "don't", "I'm")
        if is_contraction(canonical) {
            return canonical.clone();
        }
        // Only preserve acronym capitalization if user typed in uppercase
        // This prevents "us" from becoming "US" while allowing "US" -> "US"
        if is_acronym(canonical) && reference.chars().all(|c| c.is_uppercase()) {
            return canonical.clone();
        }
    }
    format_case(word, reference)
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::binary_trie::{build_from_json_with_canonical, is_proper_noun};

    fn load_json(engine: &NlpEngine, lang: &str, json: &str) {
        let trie = build_from_json_with_canonical(json).unwrap();
        engine.load_dictionary_binary_for_language(lang, &trie.serialize()).unwrap();
    }

    #[test]
    fn test_edit_distance() {
        assert_eq!(edit_distance("kitten", "sitting", 3), Some(3));
        assert_eq!(edit_distance("kitten", "sitting", 2), None);
        assert_eq!(edit_distance("hello", "hello", 2), Some(0));
        assert_eq!(edit_distance("", "abc", 3), Some(3));
        assert_eq!(edit_distance("teh", "the", 2), Some(1));
    }

    #[test]
    fn test_edit_distance_matches_full_matrix() {
        fn full(a: &str, b: &str) -> usize {
            let (a, b): (Vec<char>, Vec<char>) = (a.chars().collect(), b.chars().collect());
            let mut dp = vec![vec![0usize; b.len() + 1]; a.len() + 1];
            for i in 0..=a.len() { dp[i][0] = i; }
            for j in 0..=b.len() { dp[0][j] = j; }
            for i in 1..=a.len() {
                for j in 1..=b.len() {
                    let cost = if a[i - 1] == b[j - 1] { 0 } else { 1 };
                    dp[i][j] = min(min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
                    if i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1] {
                        dp[i][j] = min(dp[i][j], dp[i - 2][j - 2] + 1);
                    }
                }
            }
            dp[a.len()][b.len()]
        }

        let words = ["", "a", "ab", "ba", "the", "teh", "hte", "them", "then", "agian", "again", "asian",
            "becuase", "because", "knowle", "knowledge", "abcdef", "badcfe", "café", "cafe", "xyz"];
        for a in words {
            for b in words {
                for max in 0..=3 {
                    let expected = Some(full(a, b)).filter(|&dist| dist <= max);
                    assert_eq!(edit_distance(a, b, max), expected, "{a} vs {b} at max {max}");
                }
            }
        }
    }


    #[test]
    fn test_engine_load_and_suggest() {
        let engine = NlpEngine::new();
        let json = r#"{"hello": 250, "help": 200, "world": 220}"#;
        load_json(&engine, "en_US", json);

        let suggestions = engine.suggest("hel", &[], 5);
        assert!(!suggestions.is_empty());
        assert!(suggestions.iter().any(|s| s.text.to_lowercase() == "hello" || s.text.to_lowercase() == "help"));
    }

    #[test]
    fn test_spell_check() {
        let engine = NlpEngine::new();
        let json = r#"{"hello": 250, "world": 220}"#;
        load_json(&engine, "en_US", json);

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
    fn test_short_prefix_keeps_most_frequent_words() {
        let mut dict: HashMap<String, u8> = (0..120).map(|i| (format!("the{i}"), 10)).collect();
        dict.insert("the".to_string(), 255);
        dict.insert("that".to_string(), 254);
        let engine = NlpEngine::new();
        load_json(&engine, "en_US", &serde_json::to_string(&dict).unwrap());

        let texts: Vec<String> = engine.suggest("th", &[], 5).into_iter().map(|s| s.text).collect();
        assert!(texts.contains(&"that".to_string()), "Expected that in suggestions: {:?}", texts);
    }

    #[test]
    fn test_removed_word_is_not_suggested() {
        let engine = NlpEngine::new();
        engine.learn_word("zorbly", &["alpha".to_string()]);
        assert!(engine.suggest("zor", &[], 5).iter().any(|s| s.text == "zorbly"));
        assert!(engine.predict_next_word(&["alpha".to_string()], 5).iter().any(|s| s.text == "zorbly"));

        assert!(engine.remove_word("zorbly"));
        assert!(!engine.suggest("zor", &[], 5).iter().any(|s| s.text == "zorbly"));
        assert!(engine.predict_next_word(&["alpha".to_string()], 5).is_empty());
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
        load_json(&engine, "en_US", json);

        assert_eq!(engine.get_canonical_form("im"), Some("I'm".to_string()));
        assert_eq!(engine.get_canonical_form("dont"), Some("don't".to_string()));
        assert_eq!(engine.get_canonical_form("usa"), Some("USA".to_string()));
        assert_eq!(engine.get_canonical_form("hello"), None);
    }

    #[test]
    fn test_suggest_returns_canonical_forms() {
        let engine = NlpEngine::new();
        let json = r#"{"I'm": 200, "don't": 180, "USA": 150, "us": 160, "hello": 100, "image": 90}"#;
        load_json(&engine, "en_US", json);

        let suggestions = engine.suggest("im", &[], 5);
        let texts: Vec<&str> = suggestions.iter().map(|s| s.text.as_str()).collect();
        assert!(texts.contains(&"I'm"), "Expected I'm in suggestions: {:?}", texts);

        // Typing "us" (lowercase) should return "us", not "US" or "USA"
        let suggestions = engine.suggest("us", &[], 5);
        let texts: Vec<&str> = suggestions.iter().map(|s| s.text.as_str()).collect();
        assert!(texts.contains(&"us"), "Expected us in suggestions: {:?}", texts);

        // Typing "US" (uppercase) SHOULD give "USA"
        let suggestions = engine.suggest("US", &[], 5);
        let texts: Vec<&str> = suggestions.iter().map(|s| s.text.as_str()).collect();
        assert!(texts.contains(&"USA"), "Expected USA in suggestions: {:?}", texts);

        let suggestions = engine.suggest("don", &[], 5);
        let texts: Vec<&str> = suggestions.iter().map(|s| s.text.as_str()).collect();
        assert!(texts.contains(&"don't"), "Expected don't in suggestions: {:?}", texts);
    }

    #[test]
    fn test_frequency_lookup_and_typo_scan() {
        let engine = NlpEngine::new();
        load_json(&engine, "en_US", r#"{"cab": 190, "cub": 15, "cabinet": 200}"#);

        assert!(engine.spell_check("cab", &[], 2).is_valid);
        assert_eq!(engine.spell_check("cxb", &[], 2).suggestions, vec!["cab", "cub"]);
        assert_eq!(engine.get_frequency("cab"), engine.frequency_score(190));
    }

    #[test]
    fn test_spell_check_prefers_more_frequent_correction() {
        let engine = NlpEngine::new();
        load_json(&engine, "en_US", r#"{"cab": 190, "cub": 15}"#);

        let result = engine.spell_check("cxb", &[], 2);
        assert_eq!(result.suggestions, vec!["cab", "cub"]);
    }

    #[test]
    fn test_spell_check_contractions() {
        let engine = NlpEngine::new();
        let json = r#"{"I'm": 200, "don't": 180, "USA": 150, "hello": 100}"#;
        load_json(&engine, "en_US", json);

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

    #[test]
    fn test_multi_language_support() {
        let engine = NlpEngine::new();
        
        let en_json = r#"{"hello": 200, "world": 180, "computer": 150}"#;
        load_json(&engine, "en_US", en_json);
        
        let de_json = r#"{"hallo": 200, "welt": 180, "computer": 140}"#;
        load_json(&engine, "de_DE", de_json);
        
        engine.set_languages(&["en_US", "de_DE"]);
        
        let suggestions = engine.suggest("hal", &[], 5);
        let texts: Vec<&str> = suggestions.iter().map(|s| s.text.as_str()).collect();
        assert!(texts.contains(&"hallo"), "Expected hallo from de_DE: {:?}", texts);
        
        let suggestions = engine.suggest("hel", &[], 5);
        let texts: Vec<&str> = suggestions.iter().map(|s| s.text.as_str()).collect();
        assert!(texts.contains(&"hello"), "Expected hello from en_US: {:?}", texts);
        
        let suggestions = engine.suggest("comp", &[], 5);
        let texts: Vec<&str> = suggestions.iter().map(|s| s.text.as_str()).collect();
        assert!(texts.contains(&"computer"), "Expected computer (shared): {:?}", texts);
    }
}
