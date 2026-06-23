import argparse
import csv
import gzip
import json
from pathlib import Path

BOOKS = {
    "cet4": 101,
    "cet6": 102,
    "ky": 103,
    "ielts": 104,
    "toefl": 105,
}


def clean(value: str) -> str:
    return (value or "").replace("\r\n", "\n").strip()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()

    positions = {book_id: 0 for book_id in BOOKS.values()}
    counts = {tag: 0 for tag in BOOKS}
    unique = 0
    args.output.parent.mkdir(parents=True, exist_ok=True)

    with args.source.open("r", encoding="utf-8-sig", newline="") as source:
        with gzip.open(args.output, "wt", encoding="utf-8", newline="\n") as output:
            for row in csv.DictReader(source):
                tags = set((row.get("tag") or "").split())
                selected = [tag for tag in BOOKS if tag in tags]
                if not selected:
                    continue
                word = clean(row.get("word"))
                if not word:
                    continue
                unique += 1
                memberships = []
                for tag in selected:
                    book_id = BOOKS[tag]
                    positions[book_id] += 1
                    counts[tag] += 1
                    memberships.append({"bookId": book_id, "position": positions[book_id]})
                translation = clean(row.get("translation"))
                definition = clean(row.get("definition"))
                payload = {
                    "id": 100000 + unique,
                    "word": word,
                    "phonetic": clean(row.get("phonetic")),
                    "meaning": translation or definition or "暂无释义",
                    "definition": definition,
                    "memberships": memberships,
                }
                output.write(json.dumps(payload, ensure_ascii=False, separators=(",", ":")) + "\n")

    print(json.dumps({"unique": unique, "counts": counts}, ensure_ascii=False))


if __name__ == "__main__":
    main()
