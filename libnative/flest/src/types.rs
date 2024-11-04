use std::error::Error;

pub type FlestResult<T> = Result<T, Box<dyn Error>>;
