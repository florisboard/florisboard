use core::fmt;
use std::collections::HashMap;

pub const TOKEN_SEPARATOR: char = '\u{00}';

#[derive(Default)]
pub struct DynTrieNode<T> {
    children: HashMap<char, DynTrieNode<T>>,
    pub value: Option<T>,
}

impl<T: Default> DynTrieNode<T> {
    pub fn new() -> Self {
        DynTrieNode {
            children: HashMap::new(),
            value: None,
        }
    }

    #[inline]
    pub fn traverse(&self, c: char) -> Option<&DynTrieNode<T>> {
        return self.children.get(&c);
    }

    #[inline]
    pub fn traverse_mut(&mut self, c: char) -> Option<&mut DynTrieNode<T>> {
        return self.children.get_mut(&c);
    }

    #[inline]
    pub fn traverse_or_insert(&mut self, c: char) -> &mut DynTrieNode<T> {
        return self.children.entry(c).or_insert_with(|| DynTrieNode::default());
    }

    pub fn get(&self, token: &Vec<char>) -> Option<&DynTrieNode<T>> {
        let mut node = self;
        for c in token {
            node = node.traverse(*c)?;
        }
        return Some(node);
    }

    pub fn get_mut(&mut self, token: &Vec<char>) -> Option<&mut DynTrieNode<T>> {
        let mut node = self;
        for c in token {
            node = node.traverse_mut(*c)?;
        }
        return Some(node);
    }

    pub fn get_or_insert(&mut self, token: &Vec<char>) -> &mut DynTrieNode<T> {
        let mut node = self;
        for c in token {
            node = node.traverse_or_insert(*c);
        }
        if node.value.is_none() {
            node.value = Some(T::default());
        }
        return node;
    }

    pub fn get_ngram(&self, ngram: &[Vec<char>]) -> Option<&DynTrieNode<T>> {
        let mut node = self;
        for (i, token) in ngram.iter().enumerate() {
            if i > 0 {
                node = node.traverse(TOKEN_SEPARATOR)?;
            }
            node = node.get(token)?;
        }
        return Some(node);
    }

    pub fn get_ngram_mut(&mut self, ngram: &[Vec<char>]) -> Option<&mut DynTrieNode<T>> {
        let mut node = self;
        for (i, token) in ngram.iter().enumerate() {
            if i > 0 {
                node = node.traverse_mut(TOKEN_SEPARATOR)?;
            }
            node = node.get_mut(token)?;
        }
        return Some(node);
    }

    pub fn get_ngram_or_insert(&mut self, ngram: &[Vec<char>]) -> &mut DynTrieNode<T> {
        let mut node = self;
        for (i, token) in ngram.iter().enumerate() {
            if i > 0 {
                node = node.traverse_or_insert(TOKEN_SEPARATOR);
            }
            node = node.get_or_insert(token);
        }
        return node;
    }

    pub fn for_each<F>(&self, f: &F) where F: Fn(&Vec<char>, &DynTrieNode<T>) {
        let mut token = Vec::with_capacity(32);
        self.for_each_recursive(&mut token, f);
    }

    fn for_each_recursive<F>(&self, token: &mut Vec<char>, f: &F) where F: Fn(&Vec<char>, &DynTrieNode<T>) {
        if self.value.is_some() {
            f(token, self);
        }
        for (c, child) in &self.children {
            if *c == TOKEN_SEPARATOR {
                continue;
            }
            token.push(*c);
            child.for_each_recursive(token, f);
            token.pop();
        }
    }

    pub fn for_each_fnmut<F>(&self, f: &mut F) where F: FnMut(&Vec<char>, &DynTrieNode<T>) {
        let mut token = Vec::with_capacity(32);
        self.for_each_recursive_fnmut(&mut token, f);
    }

    fn for_each_recursive_fnmut<F>(&self, token: &mut Vec<char>, f: &mut F) where F: FnMut(&Vec<char>, &DynTrieNode<T>) {
        if self.value.is_some() {
            f(token, self);
        }
        for (c, child) in &self.children {
            if *c == TOKEN_SEPARATOR {
                continue;
            }
            token.push(*c);
            child.for_each_recursive_fnmut(token, f);
            token.pop();
        }
    }
}

impl<T: Default + fmt::Debug> DynTrieNode<T> {
    pub fn debug_pretty_print(&self) {
        self.debug_pretty_print_recursive(0);
    }

    fn debug_pretty_print_recursive(&self, depth: usize) {
        for (c, child) in &self.children {
            for _ in 0..depth {
                print!("  ");
            }
            println!("{:?}: {:?}", c, child.value);
            child.debug_pretty_print_recursive(depth + 1);
        }
    }
}
