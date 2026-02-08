use std::collections::HashMap;
use lz4_flex::{compress_prepend_size, decompress_size_prepended};

const MAGIC: &[u8; 4] = b"FBTD"; // FlorisBoard Trie Dictionary
const VERSION: u32 = 3; // v3: Added canonical forms section

#[repr(C, packed)]
pub struct FileHeader {
    pub magic: [u8; 4],
    pub version: u32,
    pub word_count: u32,
    pub node_count: u32,
    pub checksum: u32,
    pub canonical_count: u32,
}

impl FileHeader {
    pub const SIZE: usize = 24;

    pub fn new(word_count: u32, node_count: u32, checksum: u32, canonical_count: u32) -> Self {
        Self {
            magic: *MAGIC,
            version: VERSION,
            word_count,
            node_count,
            checksum,
            canonical_count,
        }
    }

    pub fn to_bytes(&self) -> [u8; Self::SIZE] {
        let mut buf = [0u8; Self::SIZE];
        buf[0..4].copy_from_slice(&self.magic);
        buf[4..8].copy_from_slice(&self.version.to_le_bytes());
        buf[8..12].copy_from_slice(&self.word_count.to_le_bytes());
        buf[12..16].copy_from_slice(&self.node_count.to_le_bytes());
        buf[16..20].copy_from_slice(&self.checksum.to_le_bytes());
        buf[20..24].copy_from_slice(&self.canonical_count.to_le_bytes());
        buf
    }

    pub fn from_bytes(buf: &[u8]) -> Result<Self, &'static str> {
        if buf.len() < Self::SIZE {
            return Err("Buffer too small");
        }
        let mut magic = [0u8; 4];
        magic.copy_from_slice(&buf[0..4]);
        if &magic != MAGIC {
            return Err("Invalid magic bytes");
        }
        let version = u32::from_le_bytes([buf[4], buf[5], buf[6], buf[7]]);
        if version != VERSION {
            return Err("Unsupported version");
        }
        Ok(Self {
            magic,
            version,
            word_count: u32::from_le_bytes([buf[8], buf[9], buf[10], buf[11]]),
            node_count: u32::from_le_bytes([buf[12], buf[13], buf[14], buf[15]]),
            checksum: u32::from_le_bytes([buf[16], buf[17], buf[18], buf[19]]),
            canonical_count: u32::from_le_bytes([buf[20], buf[21], buf[22], buf[23]]),
        })
    }
}

/// 12-byte node optimized for cache efficiency
#[repr(C, packed)]
#[derive(Clone, Copy, Default)]
pub struct BinaryTrieNode {
    pub char_code: u16,       // Unicode char (BMP only for Latin)
    pub frequency: u8,        // 0 = not a word, 1-255 = word frequency
    pub flags: u8,            // Reserved for future use. Possible uses can be 0=offensive, 1=proper noun, 2=contraction, 3=slang, etc.
    pub first_child: u32,     // Index of first child node (0 = none)
    pub next_sibling: u32,    // Index of next sibling node (0 = none)
}

impl BinaryTrieNode {
    pub const SIZE: usize = 12;

    pub fn to_bytes(&self) -> [u8; Self::SIZE] {
        let mut buf = [0u8; Self::SIZE];
        buf[0..2].copy_from_slice(&self.char_code.to_le_bytes());
        buf[2] = self.frequency;
        buf[3] = self.flags;
        buf[4..8].copy_from_slice(&self.first_child.to_le_bytes());
        buf[8..12].copy_from_slice(&self.next_sibling.to_le_bytes());
        buf
    }

    pub fn from_bytes(buf: &[u8]) -> Self {
        Self {
            char_code: u16::from_le_bytes([buf[0], buf[1]]),
            frequency: buf[2],
            flags: buf[3],
            first_child: u32::from_le_bytes([buf[4], buf[5], buf[6], buf[7]]),
            next_sibling: u32::from_le_bytes([buf[8], buf[9], buf[10], buf[11]]),
        }
    }
}

#[derive(Clone)]
pub struct BinaryTrie {
    nodes: Vec<BinaryTrieNode>,
    words: Vec<String>,
    canonical_forms: HashMap<String, String>, // normalized_key -> original_form
}

impl BinaryTrie {
    pub fn new() -> Self {
        let mut trie = Self {
            nodes: Vec::new(),
            words: Vec::new(),
            canonical_forms: HashMap::new(),
        };
        trie.nodes.push(BinaryTrieNode::default());
        trie
    }

    pub fn canonical_forms(&self) -> &HashMap<String, String> {
        &self.canonical_forms
    }

    pub fn insert(&mut self, word: &str, frequency: u8) {
        if word.is_empty() || frequency == 0 {
            return;
        }

        let mut current_idx = 0u32;
        
        for ch in word.chars() {
            let char_code = ch as u16;
            current_idx = self.find_or_create_child(current_idx, char_code);
        }

        self.nodes[current_idx as usize].frequency = frequency;
        self.words.push(word.to_string());
    }

    fn find_or_create_child(&mut self, parent_idx: u32, char_code: u16) -> u32 {
        let parent = &self.nodes[parent_idx as usize];
        let mut child_idx = parent.first_child;

        if child_idx == 0 {
            let new_idx = self.nodes.len() as u32;
            self.nodes.push(BinaryTrieNode {
                char_code,
                frequency: 0,
                flags: 0,
                first_child: 0,
                next_sibling: 0,
            });
            self.nodes[parent_idx as usize].first_child = new_idx;
            return new_idx;
        }

        loop {
            let child = &self.nodes[child_idx as usize];
            if child.char_code == char_code {
                return child_idx;
            }
            if child.next_sibling == 0 {
                break;
            }
            child_idx = child.next_sibling;
        }

        let new_idx = self.nodes.len() as u32;
        self.nodes.push(BinaryTrieNode {
            char_code,
            frequency: 0,
            flags: 0,
            first_child: 0,
            next_sibling: 0,
        });
        self.nodes[child_idx as usize].next_sibling = new_idx;
        new_idx
    }

    pub fn serialize(&self) -> Vec<u8> {
        let checksum = self.calculate_checksum();
        let header = FileHeader::new(
            self.words.len() as u32,
            self.nodes.len() as u32,
            checksum,
            self.canonical_forms.len() as u32,
        );

        let mut node_data = Vec::with_capacity(self.nodes.len() * BinaryTrieNode::SIZE);
        for node in &self.nodes {
            node_data.extend_from_slice(&node.to_bytes());
        }
        
        let compressed_nodes = compress_prepend_size(&node_data);
        
        // Serialize canonical forms as JSON
        let canonical_json = serde_json::to_string(&self.canonical_forms).unwrap_or_default();
        let canonical_bytes = canonical_json.as_bytes();
        let compressed_canonical = compress_prepend_size(canonical_bytes);
        
        let mut data = Vec::with_capacity(
            FileHeader::SIZE + 4 + compressed_nodes.len() + compressed_canonical.len()
        );
        data.extend_from_slice(&header.to_bytes());
        data.extend_from_slice(&(compressed_nodes.len() as u32).to_le_bytes());
        data.extend_from_slice(&compressed_nodes);
        data.extend_from_slice(&compressed_canonical);
        
        data
    }

    pub fn deserialize(data: &[u8]) -> Result<Self, &'static str> {
        let header = FileHeader::from_bytes(data)?;
        
        // Read compressed nodes length
        let nodes_len_offset = FileHeader::SIZE;
        if data.len() < nodes_len_offset + 4 {
            return Err("Data too small for nodes length");
        }
        let compressed_nodes_len = u32::from_le_bytes([
            data[nodes_len_offset],
            data[nodes_len_offset + 1],
            data[nodes_len_offset + 2],
            data[nodes_len_offset + 3],
        ]) as usize;
        
        let nodes_start = nodes_len_offset + 4;
        let nodes_end = nodes_start + compressed_nodes_len;
        if data.len() < nodes_end {
            return Err("Data too small for nodes");
        }
        
        let compressed_nodes = &data[nodes_start..nodes_end];
        let node_data = decompress_size_prepended(compressed_nodes)
            .map_err(|_| "LZ4 decompression failed for nodes")?;
        
        let expected_size = (header.node_count as usize) * BinaryTrieNode::SIZE;
        if node_data.len() < expected_size {
            return Err("Decompressed node data too small");
        }

        let mut nodes = Vec::with_capacity(header.node_count as usize);
        let mut offset = 0;
        
        for _ in 0..header.node_count {
            nodes.push(BinaryTrieNode::from_bytes(&node_data[offset..offset + BinaryTrieNode::SIZE]));
            offset += BinaryTrieNode::SIZE;
        }
        
        // Deserialize canonical forms
        let canonical_forms = if header.canonical_count > 0 && data.len() > nodes_end {
            let compressed_canonical = &data[nodes_end..];
            match decompress_size_prepended(compressed_canonical) {
                Ok(canonical_data) => {
                    match std::str::from_utf8(&canonical_data) {
                        Ok(json_str) => serde_json::from_str(json_str).unwrap_or_default(),
                        Err(_) => HashMap::new(),
                    }
                }
                Err(_) => HashMap::new(),
            }
        } else {
            HashMap::new()
        };

        let trie = Self {
            nodes,
            words: Vec::new(),
            canonical_forms,
        };

        if trie.calculate_checksum() != header.checksum {
            return Err("Checksum mismatch");
        }

        Ok(trie)
    }

    fn calculate_checksum(&self) -> u32 {
        let mut sum: u32 = 0;
        for (i, node) in self.nodes.iter().enumerate() {
            sum = sum.wrapping_add((i as u32).wrapping_mul(node.char_code as u32));
            sum = sum.wrapping_add(node.frequency as u32);
        }
        sum
    }

    pub fn search_prefix(&self, prefix: &str) -> Option<u32> {
        let mut current_idx = 0u32;
        
        for ch in prefix.chars() {
            let char_code = ch as u16;
            current_idx = self.find_child(current_idx, char_code)?;
        }
        
        Some(current_idx)
    }

    fn find_child(&self, parent_idx: u32, char_code: u16) -> Option<u32> {
        let mut child_idx = self.nodes[parent_idx as usize].first_child;
        
        while child_idx != 0 {
            let child = &self.nodes[child_idx as usize];
            if child.char_code == char_code {
                return Some(child_idx);
            }
            child_idx = child.next_sibling;
        }
        
        None
    }

    pub fn collect_words(&self, start_idx: u32, prefix: &str, results: &mut Vec<(String, u8)>, limit: usize) {
        if results.len() >= limit {
            return;
        }

        let node = &self.nodes[start_idx as usize];
        
        if node.frequency > 0 {
            results.push((prefix.to_string(), node.frequency));
        }

        let mut child_idx = node.first_child;
        while child_idx != 0 && results.len() < limit {
            let child = &self.nodes[child_idx as usize];
            let ch = char::from_u32(child.char_code as u32).unwrap_or('?');
            let mut new_prefix = prefix.to_string();
            new_prefix.push(ch);
            self.collect_words(child_idx, &new_prefix, results, limit);
            child_idx = child.next_sibling;
        }
    }

    pub fn node_count(&self) -> usize {
        self.nodes.len()
    }

    pub fn word_count(&self) -> usize {
        self.words.len()
    }
    
    pub fn canonical_count(&self) -> usize {
        self.canonical_forms.len()
    }
}

/// Check if a word should have its canonical form preserved
pub fn should_preserve_canonical(word: &str) -> bool {
    is_contraction(word) || is_acronym(word) || is_proper_noun(word)
}

pub fn is_contraction(word: &str) -> bool {
    word.contains('\'')
}

pub fn is_acronym(word: &str) -> bool {
    word.len() >= 2 && word.len() <= 5 && word.chars().all(|c| c.is_ascii_uppercase())
}

pub fn is_proper_noun(word: &str) -> bool {
    let chars: Vec<char> = word.chars().collect();
    if chars.len() < 2 {
        return false;
    }
    chars[0].is_uppercase() 
        && chars[1..].iter().all(|c| c.is_lowercase())
        && !word.contains('\'')
}

pub fn normalize_for_lookup(word: &str) -> String {
    word.to_lowercase().chars().filter(|&c| c != '\'').collect()
}

/// Build trie from JSON with canonical forms extracted
pub fn build_from_json_with_canonical(json_data: &str) -> Result<BinaryTrie, String> {
    let dict: HashMap<String, u8> = serde_json::from_str(json_data)
        .map_err(|e| format!("JSON parse error: {}", e))?;
    
    let mut trie = BinaryTrie::new();
    let mut canonical_forms = HashMap::new();
    
    let mut words: Vec<_> = dict.into_iter().collect();
    words.sort_by(|a, b| b.1.cmp(&a.1));
    
    for (word, freq) in words {
        let trimmed = word.trim();
        if should_preserve_canonical(trimmed) {
            let normalized_key = normalize_for_lookup(trimmed);
            canonical_forms.insert(normalized_key, trimmed.to_string());
        }
        trie.insert(&word.to_lowercase(), freq);
    }
    
    trie.canonical_forms = canonical_forms;
    Ok(trie)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_insert_and_search() {
        let mut trie = BinaryTrie::new();
        trie.insert("hello", 200);
        trie.insert("help", 150);
        trie.insert("world", 100);

        assert!(trie.search_prefix("hel").is_some());
        assert!(trie.search_prefix("xyz").is_none());
    }

    #[test]
    fn test_serialize_deserialize() {
        let mut trie = BinaryTrie::new();
        trie.insert("test", 100);
        trie.insert("testing", 50);
        trie.canonical_forms.insert("im".to_string(), "I'm".to_string());
        trie.canonical_forms.insert("dont".to_string(), "don't".to_string());

        let data = trie.serialize();
        let loaded = BinaryTrie::deserialize(&data).unwrap();

        assert_eq!(loaded.node_count(), trie.node_count());
        assert_eq!(loaded.canonical_count(), 2);
        assert_eq!(loaded.canonical_forms().get("im"), Some(&"I'm".to_string()));
        assert_eq!(loaded.canonical_forms().get("dont"), Some(&"don't".to_string()));
    }

    #[test]
    fn test_collect_words() {
        let mut trie = BinaryTrie::new();
        trie.insert("hello", 200);
        trie.insert("help", 150);
        trie.insert("helper", 100);

        let idx = trie.search_prefix("hel").unwrap();
        let mut results = Vec::new();
        trie.collect_words(idx, "hel", &mut results, 10);

        assert_eq!(results.len(), 3);
    }
    
    #[test]
    fn test_build_with_canonical() {
        let json = r#"{"I'm": 200, "don't": 180, "USA": 150, "hello": 100}"#;
        let trie = build_from_json_with_canonical(json).unwrap();
        
        assert_eq!(trie.canonical_count(), 3); // I'm, don't, USA
        assert_eq!(trie.canonical_forms().get("im"), Some(&"I'm".to_string()));
        assert_eq!(trie.canonical_forms().get("dont"), Some(&"don't".to_string()));
        assert_eq!(trie.canonical_forms().get("usa"), Some(&"USA".to_string()));
        assert!(trie.canonical_forms().get("hello").is_none());
    }
}
