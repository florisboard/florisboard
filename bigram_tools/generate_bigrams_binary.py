#!/usr/bin/env python3
"""
Generate memory-mapped binary format for bigram data.

Binary format:
┌─────────────────────────────────────┐
│ HEADER (16 bytes)                   │
│   - magic number (4 bytes): 0x42494752 ("BIGR")
│   - version (4 bytes): 1
│   - index_offset (4 bytes)          │
│   - index_count (4 bytes)           │
├─────────────────────────────────────┤
│ DATA SECTION                        │
│   Bigram entries sorted by word1:   │
│   [word2_len(2)][word2][prob(8)]    │
│   grouped by word1                  │
├─────────────────────────────────────┤
│ INDEX SECTION                       │
│   Sorted entries:                   │
│   [word1_len(2)][word1][offset(4)][count(4)]
└─────────────────────────────────────┘
"""

import json
import struct
import sys
from collections import defaultdict

def main():
    input_file = sys.argv[1] if len(sys.argv) > 1 else "bigrams_normalized_900000_fixed.json"
    output_file = sys.argv[2] if len(sys.argv) > 2 else "bigrams.bin"
    
    print(f"Reading {input_file}...")
    with open(input_file, 'r') as f:
        data = json.load(f)
    
    print(f"Loaded {len(data)} bigrams")
    
    grouped = defaultdict(list)
    for key, prob in data.items():
        parts = key.split('|')
        if len(parts) == 2:
            word1, word2 = parts
            grouped[word1].append((word2, prob))
    
    print(f"Grouped into {len(grouped)} unique word1 entries")
    
    for word1 in grouped:
        grouped[word1].sort(key=lambda x: x[0])
    
    sorted_word1s = sorted(grouped.keys())
    
    MAGIC = 0x42494752
    VERSION = 1
    
    header_size = 16
    
    data_section = bytearray()
    index_section = bytearray()
    
    index_offset = 0
    
    for word1 in sorted_word1s:
        entries = grouped[word1]
        offset = len(data_section)
        count = len(entries)
        
        word1_bytes = word1.encode('utf-8')
        index_section.extend(struct.pack('<H', len(word1_bytes)))
        index_section.extend(word1_bytes)
        index_section.extend(struct.pack('<I', offset))
        index_section.extend(struct.pack('<I', count))
        
        for word2, prob in entries:
            word2_bytes = word2.encode('utf-8')
            data_section.extend(struct.pack('<H', len(word2_bytes)))
            data_section.extend(word2_bytes)
            data_section.extend(struct.pack('<f', prob))
    
    index_offset = header_size + len(data_section)
    index_count = len(sorted_word1s)
    
    with open(output_file, 'wb') as f:
        f.write(struct.pack('<I', MAGIC))
        f.write(struct.pack('<I', VERSION))
        f.write(struct.pack('<I', index_offset))
        f.write(struct.pack('<I', index_count))
        f.write(data_section)
        f.write(index_section)
    
    total_size = header_size + len(data_section) + len(index_section)
    print(f"Written {output_file}:")
    print(f"  Total size: {total_size:,} bytes ({total_size / 1024 / 1024:.2f} MB)")
    print(f"  Data section: {len(data_section):,} bytes")
    print(f"  Index section: {len(index_section):,} bytes")
    print(f"  Index entries: {index_count:,}")

if __name__ == "__main__":
    main()
