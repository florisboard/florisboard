#!/bin/bash
# Compiles JSON dictionaries to binary trie format
# Usage: compile_dicts.sh [--build-compiler]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
COMPILER="$PROJECT_ROOT/libnative/dict-compiler/target/release/dict-compiler"
DICT_DIR="$PROJECT_ROOT/app/src/main/assets/ime/dict"

if [[ "$1" == "--build-compiler" ]] || [[ ! -f "$COMPILER" ]]; then
    echo "Building dict-compiler..."
    cd "$PROJECT_ROOT/libnative/dict-compiler"
    cargo build --release
fi

echo "Compiling dictionaries..."

for json_file in "$DICT_DIR"/*.json; do
    if [[ -f "$json_file" ]]; then
        base_name=$(basename "$json_file" .json)
        dict_file="$DICT_DIR/${base_name}.dict"
        
        echo "  $base_name.json -> $base_name.dict"
        "$COMPILER" --input "$json_file" --output "$dict_file" --stats false
    fi
done

echo "Done!"
