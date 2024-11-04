#[derive(Clone, Debug, PartialEq, Eq)]
pub struct Candidate {
    pub text: String,
    pub secondary_text: Option<String>,
    pub confidence: u8,
}

impl PartialOrd for Candidate {
    fn partial_cmp(&self, other: &Self) -> Option<std::cmp::Ordering> {
        // Reverse ordering
        other.confidence.partial_cmp(&self.confidence)
    }
}

impl Ord for Candidate {
    fn cmp(&self, other: &Self) -> std::cmp::Ordering {
        // Reverse ordering
        other.confidence.cmp(&self.confidence)
    }
}

pub struct CandidateQueue {
    entries: Vec<Candidate>,
    capacity: usize,
}

impl CandidateQueue {
    pub fn with_capacity(capacity: usize) -> Self {
        let capacity = capacity.max(1);
        CandidateQueue {
            entries: Vec::with_capacity(capacity),
            capacity,
        }
    }

    pub fn push(&mut self, text: String, confidence: f64) {
        if confidence.is_nan() {
            return;
        }

        let confidence = confidence.clamp(0.0, 1.0);
        let confidence = ((u8::MAX as f64) * confidence) as u8;
        let entry = Candidate { text, secondary_text: None, confidence };

        if self.entries.is_empty() {
            self.entries.push(entry);
            return;
        }

        let existing_entry = self.entries.iter_mut().find(|it| it.text == entry.text);
        if let Some(entry) = existing_entry {
            entry.confidence = entry.confidence.max(confidence);
        } else {
            if self.entries.len() < self.capacity {
                self.entries.push(entry);
            } else {
                let last = &mut self.entries[self.capacity - 1];
                if last.confidence < entry.confidence {
                    *last = entry;
                }
            }
        }
        self.entries.sort();
    }

    pub fn into_sorted_vec(self) -> Vec<Candidate> {
        self.entries
    }
}


#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn basic_insertions() {
        let mut queue = CandidateQueue::with_capacity(3);
        queue.push("foo".to_string(), 0.5);
        queue.push("bar".to_string(), 0.7);
        queue.push("baz".to_string(), 0.6);
        queue.push("qux".to_string(), 0.8);
        queue.push("quux".to_string(), 0.9);
        let vec = queue.into_sorted_vec();
        assert_eq!(vec.len(), 3);
        assert_eq!(vec[0].text, "quux");
        assert_eq!(vec[1].text, "qux");
        assert_eq!(vec[2].text, "bar");
    }

    #[test]
    fn basic_insertions_with_duplicates() {
        let mut queue = CandidateQueue::with_capacity(3);
        queue.push("foo".to_string(), 0.5);
        queue.push("bar".to_string(), 0.7);
        queue.push("baz".to_string(), 0.6);
        queue.push("qux".to_string(), 0.8);
        queue.push("quux".to_string(), 0.9);
        queue.push("quux".to_string(), 0.9);
        let vec = queue.into_sorted_vec();
        assert_eq!(vec.len(), 3);
        assert_eq!(vec[0].text, "quux");
        assert_eq!(vec[1].text, "qux");
        assert_eq!(vec[2].text, "bar");
    }

    #[test]
    fn empty_candidate_set() {
        let queue = CandidateQueue::with_capacity(3);
        let vec = queue.into_sorted_vec();
        assert_eq!(vec.len(), 0);
    }

    #[test]
    fn nan_confidence_insertions() {
        let mut queue = CandidateQueue::with_capacity(3);
        queue.push("foo".to_string(), 0.5);
        queue.push("bar".to_string(), f64::NAN);
        queue.push("baz".to_string(), 0.6);
        let vec = queue.into_sorted_vec();
        assert_eq!(vec.len(), 2);
        assert_eq!(vec[0].text, "baz");
        assert_eq!(vec[1].text, "foo");
    }
}
