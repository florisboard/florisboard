import json
from sys import argv

if len(argv) != 2:
    print(f"Usage: {argv[0]} FILE")
    exit(1)

with open(argv[1], "r") as file:
    layout_json = json.load(file)

for i, row in enumerate(layout_json["arrangement"]):
    for j, key in enumerate(row):
        ch = key["label"]

        layout_json["arrangement"][i][j]["code"] = ord(ch)

with open(argv[1], "w") as file:
    json.dump(layout_json, file, ensure_ascii=False)