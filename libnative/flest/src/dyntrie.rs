use fxhash::FxHashMap;

#[derive(Default)]
struct DynTrieNode<V> where V: Default {
    children: FxHashMap<char, Box<DynTrieNode<V>>>,
    value: Option<V>,
}

impl<V> DynTrieNode<V> where V: Default {
    fn for_each_recursive<'a, F>(&'a self, current_word: &mut Vec<char>, f: &mut F)
    where F: FnMut(&[char], &'a V) {
        if let Some(value) = &self.value {
            f(&current_word, value);
        }
        for (letter, node) in &self.children {
            current_word.push(*letter);
            node.for_each_recursive(current_word, f);
            current_word.pop();
        }
    }
}

#[derive(Default)]
pub struct DynTrie<V> where V: Default {
    root: DynTrieNode<V>,
}

impl<V> DynTrie<V>
where V: Default {
    pub fn find(&self, word: &[char]) -> Option<&V> {
        let mut current_node = &self.root;
        for letter in word {
            match current_node.children.get(letter) {
                Some(node) => current_node = node,
                None => return None,
            }
        }
        return current_node.value.as_ref();
    }

    fn str_fuzzy_match_whole(str1: &[char], str2: &[char]) -> f64 {
        let len1 = str1.len();
        let len2 = str2.len();
        let max_len = std::cmp::max(len1, len2);
        let mut score: f64 = 0.0;
        let mut penalty: f64 = 0.0;
        for i in 0..max_len {
            let ch1 = str1.get(i).unwrap_or(&' ');
            let ch2 = str2.get(i).unwrap_or(&' ');
            if ch1 == ch2 {
                score += 1.0;
            } else if ch1.to_lowercase().eq(ch2.to_lowercase()) {
                score += 0.5;
            } else {
                penalty += if i == 0 { 2.0 } else { 1.0 };
            }
        }
        return f64::max(0.0, score - penalty)
    }

    // TODO: optimization: we do not need to iterate over all
    // the trie, we can predict if the score will never be >= 0
    // and skip the whole subtree
    pub fn find_many(&self, word: &[char]) -> Vec<(Vec<char>, &V)> {
        let mut results = Vec::new();
        self.for_each(&mut |current_word, value| {
            let score = Self::str_fuzzy_match_whole(word, current_word);
            if score > 0.0 {
                results.push((current_word.to_owned(), value));
            }
        });
        return results;
    }

    pub fn find_or_insert(&mut self, word: &[char], value: V) -> &mut V {
        let mut current_node = &mut self.root;
        for letter in word {
            current_node = current_node.children.entry(*letter)
                .or_insert_with(|| Box::new(DynTrieNode::default()));
        }
        if current_node.value.is_none() {
            current_node.value = Some(value);
        }
        return current_node.value.as_mut().unwrap();
    }

    #[allow(dead_code)]
    fn insert(&mut self, word: &[char], value: V) {
        let mut current_node = &mut self.root;
        for letter in word {
            current_node = current_node.children.entry(*letter)
                .or_insert_with(|| Box::new(DynTrieNode::default()));
        }
        current_node.value = Some(value);
    }

    pub fn for_each<'a, F>(&'a self, f: &mut F)
    where F: FnMut(&[char], &'a V) {
        let mut current_word: Vec<char> = Vec::new();
        self.root.for_each_recursive(&mut current_word, f);
    }
}
