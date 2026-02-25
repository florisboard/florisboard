import os
import requests
import csv
from concurrent.futures import ThreadPoolExecutor
from rich.progress import Progress, SpinnerColumn, TextColumn, BarColumn, DownloadColumn

CSV_FILE = "ngrams_index.csv"
OUTPUT_DIR = "ngrams_downloads"
BASE_URL = "http://storage.googleapis.com/books/ngrams/books/20200217/eng/"
MAX_WORKERS = 32

def parse_csv_for_links(csv_path):
    links = []
    with open(csv_path, 'r', encoding='utf-8') as f:
        reader = csv.reader(f)
        for row in reader:
            if len(row) >= 2:
                url, name = row[0], row[1]
                links.append((url, name))
    return links

def download_file(progress, url, name):
    filename = os.path.join(OUTPUT_DIR, url.split('/')[-1])
    if os.path.exists(filename):
        return filename, "exists", name
    try:
        response = requests.get(f"{BASE_URL}{url}-of-00589.gz", stream=True, timeout=60)
        response.raise_for_status()
        total_size = int(response.headers.get('content-length', 0))
        task_id = progress.add_task(name, total=total_size)
        with open(filename, 'wb') as f:
            for chunk in response.iter_content(chunk_size=8192):
                f.write(chunk)
                progress.update(task_id, advance=len(chunk))
        progress.remove_task(task_id)
        return filename, "success", name
    except Exception as e:
        return filename, f"error: {str(e)}", name

def main():
    links = parse_csv_for_links(CSV_FILE)
    print(f"Found {len(links)} files to download")
    
    with Progress(SpinnerColumn(), TextColumn("[progress.description]{task.description}"), BarColumn(), DownloadColumn()) as progress:
        with ThreadPoolExecutor(max_workers=MAX_WORKERS) as executor:
            futures = {executor.submit(download_file, progress, url, name): (url, name) for url, name in links}

if __name__ == "__main__":
    main()