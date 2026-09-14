#!/usr/bin/env python3
"""Build the offline vocabulary asset from a pinned, openly licensed dataset."""

import csv
import io
import pathlib
import re
import urllib.request

SOURCE_COMMIT = "e20471c15a758be3362b16d07870b34df4f7ccc3"
SOURCE_URL = (
    "https://raw.githubusercontent.com/orgtre/google-books-ngram-frequency/"
    + SOURCE_COMMIT
    + "/ngrams/1grams_russian.csv"
)
LIMIT = 8000
ROOT = pathlib.Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "app" / "src" / "main" / "assets" / "words.csv"

RUSSIAN = re.compile(r"^[А-Яа-яЁё-]+$")
ENGLISH = re.compile(r"^[A-Za-z][A-Za-z'’ -]{0,47}$")
SPACE = re.compile(r"\s+")


def normalized(value: str) -> str:
    return SPACE.sub(" ", value.strip())


def accepted(russian: str, english: str) -> bool:
    if not RUSSIAN.fullmatch(russian):
        return False
    if not ENGLISH.fullmatch(english):
        return False
    if len(english.split()) > 4:
        return False
    if len(russian) == 1 and russian.lower() != "я":
        return False
    return True


def main() -> None:
    request = urllib.request.Request(
        SOURCE_URL,
        headers={"User-Agent": "Slovopotok reproducible build"},
    )
    with urllib.request.urlopen(request, timeout=60) as response:
        payload = response.read().decode("utf-8")

    rows = []
    seen = set()
    for item in csv.DictReader(io.StringIO(payload)):
        russian = normalized(item.get("ngram", ""))
        english = normalized(item.get("en", ""))
        key = (russian.casefold(), english.casefold())
        if not accepted(russian, english) or key in seen:
            continue
        seen.add(key)
        rows.append((russian, english))
        if len(rows) == LIMIT:
            break

    if len(rows) < 7000:
        raise RuntimeError("Dataset filtering returned only " + str(len(rows)) + " rows")

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with OUTPUT.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream)
        writer.writerow(("ru", "en"))
        writer.writerows(rows)

    print("Prepared " + str(len(rows)) + " offline word pairs at " + str(OUTPUT))


if __name__ == "__main__":
    main()
