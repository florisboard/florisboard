import json
import csv
import sys
import os
import gzip
from rich.progress import Progress, SpinnerColumn, TextColumn, BarColumn, TaskProgressColumn
from collections import Counter
from concurrent.futures import ProcessPoolExecutor, as_completed
from multiprocessing import get_context

WORD_LIST = set()

def process_single_file(filepath):
    local_bigrams = Counter()
    try:
        with gzip.open(filepath, 'rt', encoding='utf-8') as f:
            reader = csv.reader(f, delimiter='\t')
            for row in reader:
                if len(row) < 2:
                    continue
                ngram_part = row[0]
                if ' ' not in ngram_part:
                    continue
                ngram = ngram_part.split()
                if len(ngram) != 2:
                    continue
                
                word1 = ngram[0].split('_')[0].lower()
                word2 = ngram[1].split('_')[0].lower()
                
                if word2 not in WORD_LIST: 
                    continue
                
                freq = 0
                for col in row[1:]:
                    parts = col.split(',')
                    if len(parts) >= 2:
                        try:
                            freq += int(parts[1])
                        except ValueError:
                            continue

                if freq > 2:
                    key = f"{word1}|{word2}"
                    local_bigrams[key] += freq
                    
    except EOFError:
        return filepath, None, "corrupted"
    except Exception as e:
        return filepath, None, str(e)
        
    return filepath, local_bigrams, None

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("usage: python3 bigramGen.py <input_folder>", file=sys.stderr)
        sys.exit(1)

    input_folder = sys.argv[1]
    output_file = "bigrams.json"
    data_file = "data.json"

    with open(data_file, 'r', encoding='utf-8') as f:
        WORD_LIST = set(json.load(f).keys())

    gz_files = sorted([f for f in os.listdir(input_folder)])
    file_paths = [os.path.join(input_folder, f) for f in gz_files]

    print(f"Found {len(file_paths)} files to process")

    global_bigrams = Counter()
    MAX_WORKERS = 12

    executor = ProcessPoolExecutor(max_workers=MAX_WORKERS, mp_context=get_context('fork'))
    futures = {executor.submit(process_single_file, fp): fp for fp in file_paths}

    with Progress(SpinnerColumn(), TextColumn("[progress.description]{task.description}"), BarColumn(), TaskProgressColumn()) as progress:
        task_id = progress.add_task("processing files", total=len(file_paths))
        for future in as_completed(futures):
            filepath, local_counts, error = future.result()
            if error:
                print(f"Error processing {filepath}: {error}")
            elif local_counts:
                global_bigrams.update(local_counts)
            progress.advance(task_id)
    
    executor.shutdown()

    sorted_bigrams = dict(sorted(global_bigrams.items(), key=lambda x: x[1], reverse=True))

    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(sorted_bigrams, f, ensure_ascii=False)
    
    print(f"found {len(sorted_bigrams)} unique bigrams.")