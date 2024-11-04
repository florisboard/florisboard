use crate::types::FlestResult;

use super::NgramModel;

impl NgramModel {
    pub fn from_file(path: &str) -> FlestResult<Self> {
        let mut model = NgramModel::new();
        model.load_from_file(path)?;
        return Ok(model);
    }

    pub fn load_from_file(&mut self, path: &str) -> FlestResult<()> {
        todo!()
    }

    pub fn persist_to_file(&self, path: &str) -> FlestResult<()> {
        todo!()
    }
}
