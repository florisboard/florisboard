use clap::Parser;
use nlp::binary_trie::build_from_json_with_canonical;
use std::collections::HashMap;
use std::fs;
use std::path::PathBuf;
use std::time::Instant;

#[derive(Parser)]
#[command(name = "dict-compiler")]
#[command(about = "Compiles JSON dictionaries to FlorisBoard binary trie format")]
struct Args {
    /// Input JSON dictionary file
    #[arg(short, long)]
    input: PathBuf,

    /// Output binary dictionary file
    #[arg(short, long)]
    output: PathBuf,

    /// Verify output after writing
    #[arg(short, long, default_value = "true")]
    verify: bool,

    /// Print statistics
    #[arg(short, long, default_value = "true")]
    stats: bool,
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let args = Args::parse();

    println!("dict-compiler v{}", env!("CARGO_PKG_VERSION"));
    println!("Input:  {}", args.input.display());
    println!("Output: {}", args.output.display());
    println!();

    let start = Instant::now();

    // Read JSON
    println!("Reading JSON...");
    let json_data = fs::read_to_string(&args.input)?;
    let dict: HashMap<String, u8> = serde_json::from_str(&json_data)?;
    let word_count = dict.len();
    println!("  Loaded {} words", word_count);

    // Build trie with canonical forms
    println!("Building trie with canonical forms...");
    let trie = build_from_json_with_canonical(&json_data)?;
    let node_count = trie.node_count();
    let canonical_count = trie.canonical_count();
    println!("  Created {} nodes", node_count);
    println!("  Extracted {} canonical forms (contractions/acronyms)", canonical_count);

    // Serialize
    println!("Serializing...");
    let binary_data = trie.serialize();
    let binary_size = binary_data.len();

    // Write output
    println!("Writing binary file...");
    fs::write(&args.output, &binary_data)?;

    // Verify
    if args.verify {
        println!("Verifying...");
        let read_back = fs::read(&args.output)?;
        let loaded = nlp::binary_trie::BinaryTrie::deserialize(&read_back)
            .map_err(|e| format!("Verification failed: {}", e))?;
        
        if loaded.node_count() != node_count {
            return Err(format!(
                "Node count mismatch: expected {}, got {}",
                node_count,
                loaded.node_count()
            ).into());
        }
        
        if loaded.canonical_count() != canonical_count {
            return Err(format!(
                "Canonical count mismatch: expected {}, got {}",
                canonical_count,
                loaded.canonical_count()
            ).into());
        }
        
        // Spot check some words
        let test_words = ["the", "hello", "world", "test"];
        for word in test_words {
            if dict.contains_key(word) {
                if loaded.search_prefix(word).is_none() {
                    return Err(format!("Verification failed: '{}' not found in trie", word).into());
                }
            }
        }
        println!("  Verification passed");
    }

    let elapsed = start.elapsed();

    if args.stats {
        println!();
        println!("=== Statistics ===");
        println!("Words:        {:>10}", word_count);
        println!("Nodes:        {:>10}", node_count);
        println!("Canonical:    {:>10}", canonical_count);
        println!("JSON size:    {:>10} bytes ({:.2} MB)", json_data.len(), json_data.len() as f64 / 1024.0 / 1024.0);
        println!("Binary size:  {:>10} bytes ({:.2} MB)", binary_size, binary_size as f64 / 1024.0 / 1024.0);
        println!("Compression:  {:>10.1}%", (1.0 - binary_size as f64 / json_data.len() as f64) * 100.0);
        println!("Bytes/word:   {:>10.1}", binary_size as f64 / word_count as f64);
        println!("Bytes/node:   {:>10.1}", binary_size as f64 / node_count as f64);
        println!("Time:         {:>10.2}s", elapsed.as_secs_f64());
    }

    println!();
    println!("Done!");
    Ok(())
}
