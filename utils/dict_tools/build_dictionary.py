#!/usr/bin/env python3
"""
Dictionary builder for FlorisBoard.
Cleans, filters, and properly weights word frequency data.
"""

import json
import re
import sys
from pathlib import Path

SCRIPT_DIR = Path(__file__).parent
PROJECT_ROOT = SCRIPT_DIR.parent.parent
INPUT_FILE = PROJECT_ROOT / "app/src/main/assets/ime/dict/data.json"
OUTPUT_FILE = PROJECT_ROOT / "app/src/main/assets/ime/dict/en_US.json"

TARGET_WORD_COUNT = 60000
MIN_WORD_LENGTH = 2
MAX_WORD_LENGTH = 24

OFFENSIVE_WORDS = {
    "word 1", "word 2", "word 3"
}

# Words that look offensive but have legitimate uses
ALLOWED_DUAL_PURPOSE = {
    "ass",      # donkey
}

# Top 10K words get highest priority (from linguistic research)
TOP_WORDS_BOOST = {
    "the": 255, "be": 255, "to": 255, "of": 255, "and": 255, "a": 255, "in": 255,
    "that": 254, "have": 254, "i": 254, "it": 254, "for": 254, "not": 254, "on": 254,
    "with": 254, "he": 254, "as": 254, "you": 254, "do": 254, "at": 254, "this": 254,
    "but": 253, "his": 253, "by": 253, "from": 253, "they": 253, "we": 253, "say": 253,
    "her": 253, "she": 253, "or": 253, "an": 253, "will": 253, "my": 253, "one": 253,
    "all": 252, "would": 252, "there": 252, "their": 252, "what": 252, "so": 252,
    "up": 252, "out": 252, "if": 252, "about": 252, "who": 252, "get": 252, "which": 252,
    "go": 251, "me": 251, "when": 251, "make": 251, "can": 251, "like": 251, "time": 251,
    "no": 251, "just": 251, "him": 251, "know": 251, "take": 251, "people": 251,
    "into": 250, "year": 250, "your": 250, "good": 250, "some": 250, "could": 250,
    "them": 250, "see": 250, "other": 250, "than": 250, "then": 250, "now": 250,
    "look": 249, "only": 249, "come": 249, "its": 249, "over": 249, "think": 249,
    "also": 249, "back": 249, "after": 249, "use": 249, "two": 249, "how": 249,
    "our": 248, "work": 248, "first": 248, "well": 248, "way": 248, "even": 248,
    "new": 248, "want": 248, "because": 248, "any": 248, "these": 248, "give": 248,
    "day": 247, "most": 247, "us": 247, "is": 247, "are": 247, "was": 247, "were": 247,
    "been": 247, "has": 247, "had": 247, "did": 247, "does": 247, "doing": 247,
    "said": 246, "each": 246, "she's": 246, "he's": 246, "it's": 246, "i'm": 246,
    "don't": 246, "didn't": 246, "won't": 246, "can't": 246, "couldn't": 246,
    "wouldn't": 246, "shouldn't": 246, "wasn't": 246, "weren't": 246, "isn't": 246,
    "aren't": 246, "haven't": 246, "hasn't": 246, "hadn't": 246, "i've": 246,
    "you've": 246, "we've": 246, "they've": 246, "i'll": 246, "you'll": 246,
    "he'll": 246, "she'll": 246, "it'll": 246, "we'll": 246, "they'll": 246,
    "i'd": 246, "you'd": 246, "he'd": 246, "she'd": 246, "we'd": 246, "they'd": 246,
    "let's": 246, "that's": 246, "what's": 246, "here's": 246, "there's": 246,
    "who's": 246, "how's": 246, "where's": 246, "when's": 246, "why's": 246,
}

VALID_WORD_PATTERN = re.compile(r"^[a-zA-Z][a-zA-Z'-]*[a-zA-Z]$|^[a-zA-Z]$|^[aAiI]$")


def is_valid_word(word: str) -> bool:
    if len(word) < MIN_WORD_LENGTH or len(word) > MAX_WORD_LENGTH:
        return False
    if not VALID_WORD_PATTERN.match(word):
        return False
    if word.lower() in OFFENSIVE_WORDS and word.lower() not in ALLOWED_DUAL_PURPOSE:
        return False
    if word.startswith("-") or word.endswith("-"):
        return False
    if "--" in word or "''" in word:
        return False
    return True


def calculate_frequency(word: str, original_freq: int, rank: int) -> int:
    lower = word.lower()
    if lower in TOP_WORDS_BOOST:
        return TOP_WORDS_BOOST[lower]
    
    # Logarithmic decay based on rank
    if rank < 1000:
        base = 240
    elif rank < 5000:
        base = 200
    elif rank < 10000:
        base = 150
    elif rank < 20000:
        base = 100
    elif rank < 40000:
        base = 50
    else:
        base = 20
    
    # Boost based on original frequency
    if original_freq >= 200:
        boost = 15
    elif original_freq >= 100:
        boost = 10
    elif original_freq >= 50:
        boost = 5
    else:
        boost = 0
    
    return min(255, max(1, base + boost))


def main():
    print(f"Loading dictionary from {INPUT_FILE}...")
    with open(INPUT_FILE, "r", encoding="utf-8") as f:
        raw_data = json.load(f)
    
    print(f"Loaded {len(raw_data):,} words")
    
    # Filter and sort
    valid_words = []
    for word, freq in raw_data.items():
        if is_valid_word(word):
            valid_words.append((word, freq))
    
    print(f"After validation: {len(valid_words):,} words")
    
    # Sort by frequency (descending), then alphabetically
    valid_words.sort(key=lambda x: (-x[1], x[0].lower()))
    
    # Take top N words
    valid_words = valid_words[:TARGET_WORD_COUNT]
    print(f"After trimming to top {TARGET_WORD_COUNT:,}: {len(valid_words):,} words")
    
    # Recalculate frequencies based on rank
    output_dict = {}
    for rank, (word, orig_freq) in enumerate(valid_words):
        new_freq = calculate_frequency(word, orig_freq, rank)
        output_dict[word] = new_freq
    
    # Write output
    print(f"Writing to {OUTPUT_FILE}...")
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(output_dict, f, indent=2, ensure_ascii=False)
    
    # Stats
    file_size = OUTPUT_FILE.stat().st_size
    print(f"\nDone!")
    print(f"  Words: {len(output_dict):,}")
    print(f"  File size: {file_size / 1024 / 1024:.2f} MB")
    
    # Frequency distribution
    freq_buckets = {
        "240-255 (top)": 0,
        "200-239": 0,
        "150-199": 0,
        "100-149": 0,
        "50-99": 0,
        "1-49": 0,
    }
    for freq in output_dict.values():
        if freq >= 240:
            freq_buckets["240-255 (top)"] += 1
        elif freq >= 200:
            freq_buckets["200-239"] += 1
        elif freq >= 150:
            freq_buckets["150-199"] += 1
        elif freq >= 100:
            freq_buckets["100-149"] += 1
        elif freq >= 50:
            freq_buckets["50-99"] += 1
        else:
            freq_buckets["1-49"] += 1
    
    print("\nFrequency distribution:")
    for bucket, count in freq_buckets.items():
        print(f"  {bucket}: {count:,} words")


if __name__ == "__main__":
    main()
