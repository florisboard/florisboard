use icu::properties::sets;

const NULLCHAR: char = '\0';

fn str_match_impl(word1: &[char], word2: &[char]) -> f64 {
    let len1: usize = word1.len();
    let len2: usize = word2.len();
    let mut score1: f64 = 0.0;
    let mut score2: f64 = 0.0;
    let mut penalty: f64 = 0.0;
    let mut last_penalty_awarded: f64 = 0.0;

    let mut i1: usize = 0;
    let mut i2: usize = 0;
    let mut last_ch1: char = NULLCHAR;
    let mut last_ch2: char = NULLCHAR;

    fn next(word: &[char], i: &mut usize) -> char {
        let mut ch: char;
        loop {
            ch = *word.get(*i).unwrap_or(&NULLCHAR);
            if ch == NULLCHAR || !sets::diacritic().contains(ch) {
                break;
            }
            *i += 1;
        }
        return ch;
    }

    while i1 < len1 || i2 < len2 {
        let ch1 = next(word1, &mut i1);
        let ch2 = next(word2, &mut i2);

        if ch1 == NULLCHAR && ch2 == NULLCHAR {
            break;
        }
        if ch1 != NULLCHAR {
            score1 += 1.0;
        }
        if ch2 != NULLCHAR {
            score2 += 1.0;
        }
        if ch1 == NULLCHAR || ch2 == NULLCHAR {
            i1 += 1;
            i2 += 1;
            continue;
        }

        if ch1 == ch2 {
            // no penalty
        } else if ch1.to_lowercase().eq(ch2.to_lowercase()) {
            penalty += 0.1;
        } else if ch1 == last_ch2 && ch2 == last_ch1 {
            // transposition
            // reduce penalty for transpositions
            penalty -= 0.5 * last_penalty_awarded;
        } else {
            last_penalty_awarded = 1.0;
            if last_ch1 == NULLCHAR && last_ch2 == NULLCHAR {
                last_penalty_awarded += 1.0;
            }
            penalty += last_penalty_awarded;
        }

        i1 += 1;
        i2 += 1;
        last_ch1 = ch1;
        last_ch2 = ch2;
    }

    let mut score = f64::max(score1, score2);
    if score == 0.0 {
        // both strings essentially empty, thus they match
        return 1.0;
    }
    score = 1.0 - penalty / score;
    return f64::max(0.0, score);
}

#[inline]
pub fn str_match(word1: &[char], word2: &[char]) -> f64 {
    // TODO: evaluate if impl wrapper is necessary
    return str_match_impl(word1, word2);
}

#[allow(non_snake_case)]
#[cfg(test)]
mod tests {
    use std::vec;

    use crate::normalization::StringNormalizationHelpers;

    use super::*;

    #[test]
    fn ascii_basic_match() {
        let abc = "abc".to_nfd_chars();

        let result = str_match(&abc, &abc);
        assert_eq!(result, 1.0);
    }

    #[test]
    fn ascii_basic_mismatch() {
        let abc = "abc".to_nfd_chars();
        let def = "def".to_nfd_chars();

        let result = str_match(&abc, &def);
        assert_eq!(result, 0.0);
    }

    #[test]
    fn ascii_casing_diff_one_char() {
        let a = "a".to_nfd_chars();
        let A = "A".to_nfd_chars();

        let result = str_match(&a, &A);
        assert_eq!(result, 0.9);
    }

    #[test]
    fn ascii_casing_diff_multiple_chars() {
        let abc = "abc".to_nfd_chars();
        let ABC = "ABC".to_nfd_chars();

        let result = str_match(&abc, &ABC);
        assert_eq!(result, 0.9);
    }

    #[test]
    fn diacritic_basic_match_lowercase() {
        let ae = "ä".to_nfd_chars();

        let result = str_match(&ae, &ae);
        assert_eq!(result, 1.0);
    }

    #[test]
    fn diacritic_basic_match_uppercase() {
        let AE = "Ä".to_nfd_chars();

        let result = str_match(&AE, &AE);
        assert_eq!(result, 1.0);
    }

    #[test]
    fn diacritic_basic_mismatch_lowercase() {
        let ae = "ä".to_nfd_chars();
        let oe = "ö".to_nfd_chars();

        let result = str_match(&ae, &oe);
        assert_eq!(result, 0.0);
    }

    #[test]
    fn diacritic_basic_mismatch_uppercase() {
        let AE = "Ä".to_nfd_chars();
        let OE = "Ö".to_nfd_chars();

        let result = str_match(&AE, &OE);
        assert_eq!(result, 0.0);
    }

    #[test]
    fn diacritic_casing_and_accent_diff() {
        let ae = "ä".to_nfd_chars();
        let AE = "Ä".to_nfd_chars();
        let a: Vec<char> = "a".to_nfd_chars();
        let A = "A".to_nfd_chars();

        let result = str_match(&ae, &AE);
        assert_eq!(result, 0.9);

        let result = str_match(&ae, &A);
        assert_eq!(result, 0.9);

        let result = str_match(&AE, &a);
        assert_eq!(result, 0.9);

        let result = str_match(&ae, &a);
        assert_eq!(result, 1.0);

        let result = str_match(&AE, &A);
        assert_eq!(result, 1.0);
    }

    #[test]
    fn empty_match() {
        let empty = "".to_nfd_chars();

        let result = str_match(&empty, &empty);
        assert_eq!(result, 1.0);
    }

    #[test]
    fn transposition_basic_1_start() {
        let str1 = "abxx".to_nfd_chars();
        let str2 = "baxx".to_nfd_chars();

        let result = str_match(&str1, &str2);
        assert_eq!(result, 0.75);
    }

    #[test]
    fn transposition_basic_2_middle() {
        let str1 = "xabx".to_nfd_chars();
        let str2 = "xbax".to_nfd_chars();

        let result = str_match(&str1, &str2);
        assert_eq!(result, 0.875);
    }

    #[test]
    fn transposition_basic_3_end() {
        let str1 = "xxab".to_nfd_chars();
        let str2 = "xxba".to_nfd_chars();

        let result = str_match(&str1, &str2);
        assert_eq!(result, 0.875);
    }

    #[test]
    fn transposition_diactric_1_start() {
        let str1 = "äbxx".to_nfd_chars();
        let str2 = "bäxx".to_nfd_chars();

        let result = str_match(&str1, &str2);
        assert_eq!(result, 0.75);
    }

    #[test]
    fn transposition_diactric_2_middle() {
        let str1 = "xäbx".to_nfd_chars();
        let str2 = "xbäx".to_nfd_chars();

        let result = str_match(&str1, &str2);
        assert_eq!(result, 0.875);
    }

    #[test]
    fn transposition_diactric_3_end() {
        let str1 = "xxäb".to_nfd_chars();
        let str2 = "xxbä".to_nfd_chars();

        let result = str_match(&str1, &str2);
        assert_eq!(result, 0.875);
    }

    #[test]
    fn unicode_normalization_basic_mismatch() {
        let ae_nfd = "ä".to_nfd_chars();
        let ae_nfc = "ä".to_nfc_chars();

        let result = str_match(&ae_nfd, &ae_nfc);
        assert_eq!(result, 0.0);
    }

    #[test]
    fn words_english_many() {
        let words = vec![
            ("hello", "hello", 1.0),
            ("hello", "hallo", 0.8),
            ("hello", "helo", 0.8),
        ];

        for (word1, word2, expected_score) in words {
            let result = str_match(&word1.to_nfd_chars(), &word2.to_nfd_chars());
            assert_eq!(result, expected_score, "Mismatch for words '{}' and '{}'", word1, word2);
        }
    }
}
