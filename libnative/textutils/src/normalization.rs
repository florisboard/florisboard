use unicode_normalization::UnicodeNormalization;

pub trait StringNormalizationHelpers {
    fn to_nfd_chars(&self) -> Vec<char>;

    fn to_nfd_string(&self) -> String;

    fn to_nfc_chars(&self) -> Vec<char>;

    fn to_nfc_string(&self) -> String;

    fn to_nfkd_chars(&self) -> Vec<char>;

    fn to_nfkd_string(&self) -> String;

    fn to_nfkc_chars(&self) -> Vec<char>;

    fn to_nfkc_string(&self) -> String;
}

impl StringNormalizationHelpers for str {
    #[inline]
    fn to_nfd_chars(&self) -> Vec<char> {
        self.nfd().collect()
    }

    #[inline]
    fn to_nfd_string(&self) -> String {
        self.nfd().collect()
    }

    #[inline]
    fn to_nfc_chars(&self) -> Vec<char> {
        self.nfc().collect()
    }

    #[inline]
    fn to_nfc_string(&self) -> String {
        self.nfc().collect()
    }

    #[inline]
    fn to_nfkd_chars(&self) -> Vec<char> {
        self.nfkd().collect()
    }

    #[inline]
    fn to_nfkd_string(&self) -> String {
        self.nfkd().collect()
    }

    #[inline]
    fn to_nfkc_chars(&self) -> Vec<char> {
        self.nfkc().collect()
    }

    #[inline]
    fn to_nfkc_string(&self) -> String {
        self.nfkc().collect()
    }
}
