use flest::model::NgramModel;
use pancurses::Input;
use textutils::filter::preprocess_auto;
use textutils::segment::IcuSegmenterCache;
use std::env;
use std::fs;
use std::io::BufRead;
use std::io::BufReader;

const TOKEN_SENTENCE_SEPARATOR: &str = "\\sep";

fn tokenize_text(text: &str) -> Vec<&str> {
    let segmenters = IcuSegmenterCache::new_auto();
    let sentences = segmenters.split_sentences(text);
    let mut tokens: Vec<&str> = Vec::new();

    tokens.push(TOKEN_SENTENCE_SEPARATOR);
    for sentence in sentences {
        let words = segmenters.split_words(sentence);
        for word in words {
            tokens.push(word);
        }
        tokens.push(TOKEN_SENTENCE_SEPARATOR);
    }

    //println!("Tokens: {:?}", tokens);
    return tokens;
}

fn train_model(text: &str, model: &mut NgramModel) {
    let text = preprocess_auto(text);
    let text = text.trim();
    if text.is_empty() {
        return;
    }
    let tokens = tokenize_text(&text);
    //println!("Tokens: {:?}", tokens);
    model.train_from_tokens(&tokens);
}

fn train_from_plain_text(path: &str, model: &mut NgramModel) {
    let text = fs::read_to_string(path).expect("Failed to read file");
    train_model(&text, model);
}

fn train_from_reddit_comments(path: &str, model: &mut NgramModel) {
    let file = fs::File::open(path).expect("Failed to open file");
    let reader = BufReader::new(file);
    let mut line_count = 0;
    for line in reader.lines() {
        if let Ok(line) = line {
            let json: serde_json::Value = serde_json::from_str(&line).expect("Failed to parse JSON");

            if let Some(author) = json.get("author").and_then(|it| it.as_str()) {
                if author == "AutoModerator" {
                    continue;
                }
            }
            if let Some(body) = json.get("body").and_then(|it| it.as_str()) {
                train_model(body, model);
            }
        }
        line_count += 1;
        if line_count > 10000 {
            break;
        }
    }
}

fn main() {
    let args: Vec<String> = env::args().collect();
    if args.len() != 2 {
        eprintln!("Usage: {} <file_path>", args[0]);
        return;
    }

    let path = &args[1];
    let mut model = NgramModel::new();
    model.meta.sentence_token = TOKEN_SENTENCE_SEPARATOR.to_owned();

    if path.ends_with(".reddit.jsonl") {
        train_from_reddit_comments(path, &mut model);
    } else {
        train_from_plain_text(path, &mut model);
    }
    //model.trie_root.debug_pretty_print();
    //return;

    let window = pancurses::initscr();
    let mut input_text = String::new();

    pancurses::noecho();
    window.keypad(true);
    loop {
        let mut words: Vec<&str> = input_text.split_whitespace().collect();
        words.insert(0, TOKEN_SENTENCE_SEPARATOR);

        if input_text.ends_with(' ') || words.last() == Some(&TOKEN_SENTENCE_SEPARATOR) {
            words.push("");
        }

        let predictions = model.predict(&words);

        window.clear();
        window.addstr("N-gram model debug frontend\n");
        window.addstr("  demo tokenizer only supports single-line sentence in input text!\n\n");
        window.addstr(format!("enter text: {}\n", input_text));
        window.addstr(format!("detected words: {:?}\n\n", words));
        window.addstr("predictions:\n");
        for (i, candidate) in predictions.iter().enumerate() {
            if i == 0 && candidate.confidence > (0.9 * 255.0) as u8 {
                window.attron(pancurses::A_BOLD);
            }
            window.addstr(format!("  {}. {} (c={:.2})\n", i + 1, candidate.text, candidate.confidence));
            if i == 0 && candidate.confidence > (0.9 * 255.0) as u8 {
                window.attroff(pancurses::A_BOLD);
            }
        }
        if predictions.is_empty() {
            window.addstr("  (none)\n");
        }
        window.mv(3, 12 + input_text.len() as i32);
        window.refresh();

        match window.getch().unwrap() {
            Input::KeyF10 => {
                break
            }
            Input::KeyBackspace => {
                input_text.pop();
            }
            Input::Character('\n') => {
                train_model(&input_text, &mut model)
            }
            Input::Character(ch) => {
                input_text.push(ch)
            }
            _ => { () }
        }
    }

    pancurses::endwin();
}
