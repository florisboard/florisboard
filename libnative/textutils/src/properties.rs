use icu::properties::sets::CodePointSetDataBorrowed;

pub use icu::properties::sets;

pub trait CodePointSetDataExt {
    fn debug_print(&self);

    fn debug_print_based(&self, base: char);
}

impl <'a> CodePointSetDataExt for CodePointSetDataBorrowed<'a> {
    fn debug_print(&self) {
        debug_print_impl(&self, None);
    }

    fn debug_print_based(&self, base: char) {
        debug_print_impl(&self, Some(base));
    }
}

fn debug_print_impl(set: &CodePointSetDataBorrowed, base: Option<char>) {
    for range in set.iter_ranges() {
        print!("{:#x}..={:#x}", range.start(), range.end());
        for codepoint in range {
            if let Some(base) = base {
                print!(" {}{}", base, char::from_u32(codepoint).unwrap());
            } else {
                print!(" {}", char::from_u32(codepoint).unwrap());
            }
        }
        println!();
    }
}
