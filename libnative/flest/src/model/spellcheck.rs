use crate::candidates::Candidate;

use super::NgramModel;

impl NgramModel {
    fn spell(&self, curr_word: &str, history: &Vec<&str>) -> Vec<Candidate> {
        todo!()
    }

    fn spell_sentence(&self, sentence: &Vec<&str>) -> Vec<Option<Vec<Candidate>>> {
        todo!()
    }
}
