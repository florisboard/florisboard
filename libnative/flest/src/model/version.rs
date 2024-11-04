use core::fmt;

#[derive(Clone, Copy, PartialEq, Eq, PartialOrd, Ord)]
pub struct NgramModelVersion {
    major: u8,
    minor: u8,
}

#[macro_export]
macro_rules! ngram_model_version {
    ($major:expr, $minor:expr) => {
        NgramModelVersion { major: $major, minor: $minor }
    };
    ($major:expr) => {
        NgramModelVersion { major: $major, minor: 0 }
    };
}

#[allow(non_upper_case_globals)]
impl NgramModelVersion {
    pub const vDEV: NgramModelVersion = ngram_model_version!(0, 0);
    pub const v0_1: NgramModelVersion = ngram_model_version!(0, 1);

    pub fn latest() -> NgramModelVersion {
        NgramModelVersion::v0_1
    }
}

impl fmt::Display for NgramModelVersion {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        if self.major == 0 && self.minor == 0 {
            write!(f, "vDEV")
        } else {
            write!(f, "v{}.{}", self.major, self.minor)
        }
    }
}

impl fmt::Debug for NgramModelVersion {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{} (0x{:02x}{:02x})", self, self.major, self.minor)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn display() {
        assert_eq!(format!("{}", ngram_model_version!(0, 1)), "v0.1");
        assert_eq!(format!("{:?}", ngram_model_version!(0, 1)), "v0.1 (0x0001)");
        assert_eq!(format!("{}", ngram_model_version!(1, 0)), "v1.0");
        assert_eq!(format!("{:?}", ngram_model_version!(1, 0)), "v1.0 (0x0100)");
    }

    #[test]
    fn equality() {
        assert_eq!(ngram_model_version!(0, 1), ngram_model_version!(0, 1));
        assert_eq!(ngram_model_version!(1, 0), ngram_model_version!(1, 0));
        assert_ne!(ngram_model_version!(0, 1), ngram_model_version!(1, 0));
    }

    #[test]
    fn comparison() {
        assert!(ngram_model_version!(0, 1) > ngram_model_version!(0, 0));
        assert!(ngram_model_version!(1, 0) > ngram_model_version!(0, 1));
        assert!(ngram_model_version!(1, 0) > ngram_model_version!(0, 42));
    }
}
