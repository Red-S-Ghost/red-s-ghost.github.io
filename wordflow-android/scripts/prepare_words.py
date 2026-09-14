#!/usr/bin/env python3
"""Build two independent offline vocabularies from pinned open datasets."""

import csv
import gzip
import hashlib
import io
import pathlib
import re
import tarfile
import urllib.request

FREQUENCY_COMMIT = "e20471c15a758be3362b16d07870b34df4f7ccc3"
ENGLISH_FREQUENCY_URL = (
    "https://raw.githubusercontent.com/orgtre/google-books-ngram-frequency/"
    + FREQUENCY_COMMIT
    + "/ngrams/1grams_english.csv"
)
RUSSIAN_FREQUENCY_URL = (
    "https://raw.githubusercontent.com/orgtre/google-books-ngram-frequency/"
    + FREQUENCY_COMMIT
    + "/ngrams/1grams_russian.csv"
)
EXTENDED_FREQUENCY_COMMIT = "5e9902468ab09802474884c3df00d77463e5cb24"
EXTENDED_FREQUENCY_URL = (
    "https://raw.githubusercontent.com/hackerb9/gwordlist/"
    + EXTENDED_FREQUENCY_COMMIT
    + "/frequency-alpha-alldicts.txt"
)
MUELLER_URL = (
    "https://downloads.sourceforge.net/mueller-dict/"
    "mueller-dict-3.1.1.tar.gz"
)
MUELLER_SHA1 = "6aebd3fdbe8f921e0011630d35137caf523fd40a"

EN_RU_LIMIT = 10000
RU_EN_LIMIT = 8000
ROOT = pathlib.Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"
SOURCES = ROOT / "build" / "sources"
MUELLER_ARCHIVE = SOURCES / "mueller-dict-3.1.1.tar.gz"
EN_RU_OUTPUT = ASSETS / "words_en_ru.csv"
RU_EN_OUTPUT = ASSETS / "words_ru_en.csv"

SPACE = re.compile(r"\s+")
ENGLISH_WORD = re.compile(r"^[A-Za-z]+(?:['-][A-Za-z]+)*$")
ENGLISH_TRANSLATION = re.compile(r"^[A-Za-z][A-Za-z' -]{0,63}$")
RUSSIAN_WORD = re.compile(r"^[А-Яа-яЁё-]+$")
CYRILLIC = re.compile(r"[А-Яа-яЁё]")
LATIN = re.compile(r"[A-Za-z]")
GWORD_ROW = re.compile(
    r"^\s*#?[0-9][0-9,]*\s+([A-Za-z][A-Za-z'-]*)\s+"
)
DICT_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
DICT_DIGITS = {character: index for index, character in enumerate(DICT_ALPHABET)}
SPECIALIZED_LABELS = (
    "_ав.",
    "_анат.",
    "_арх.",
    "_астр.",
    "_биол.",
    "_бот.",
    "_воен.",
    "_геол.",
    "_горн.",
    "_ж-д.",
    "_зоол.",
    "_мат.",
    "_мед.",
    "_мор.",
    "_муз.",
    "_опт.",
    "_полигр.",
    "_радио.",
    "_спорт.",
    "_тех.",
    "_физ.",
    "_физиол.",
    "_фин.",
    "_хим.",
    "_юр.",
)

# A short, reviewable layer for very common words whose first dictionary
# article is not enough to represent everyday part-of-speech ambiguity.
CORE_OVERRIDES = {
    "go": "идти, ходить; ехать",
    "get": "получать, доставать; становиться",
    "make": "делать, создавать",
    "take": "брать, взять",
    "run": "бежать, бегать; бег",
    "set": "ставить, устанавливать; набор",
    "right": "правильный, верный; правый",
    "light": "свет; лёгкий",
    "mean": "значить, означать; иметь в виду",
    "still": "всё ещё; неподвижный",
    "change": "менять, изменять; изменение",
    "turn": "поворачивать; поворот, очередь",
    "work": "работать; работа",
    "play": "играть; игра",
    "like": "нравиться; как, подобно",
}


def normalized(value: str) -> str:
    return SPACE.sub(" ", value.strip())


def download(url: str) -> bytes:
    request = urllib.request.Request(
        url,
        headers={"User-Agent": "Slovopotok reproducible build"},
    )
    with urllib.request.urlopen(request, timeout=120) as response:
        return response.read()


def verified_mueller_archive() -> bytes:
    payload = download(MUELLER_URL)
    actual = hashlib.sha1(payload).hexdigest()
    if actual != MUELLER_SHA1:
        raise RuntimeError(
            "Mueller archive checksum mismatch: expected "
            + MUELLER_SHA1
            + ", got "
            + actual
        )
    SOURCES.mkdir(parents=True, exist_ok=True)
    MUELLER_ARCHIVE.write_bytes(payload)
    return payload


def archive_member(bundle: tarfile.TarFile, suffix: str) -> bytes:
    member = next(
        (item for item in bundle.getmembers() if item.name.endswith(suffix)),
        None,
    )
    if member is None:
        raise RuntimeError("Mueller archive has no " + suffix)
    stream = bundle.extractfile(member)
    if stream is None:
        raise RuntimeError("Cannot read Mueller archive member " + member.name)
    return stream.read()


def decode_dict_number(value: str) -> int:
    result = 0
    for character in value:
        result = result * 64 + DICT_DIGITS[character]
    return result


def load_mueller():
    payload = verified_mueller_archive()
    with tarfile.open(fileobj=io.BytesIO(payload), mode="r:gz") as bundle:
        compressed_data = archive_member(bundle, "/dict/mueller-base.dict.dz")
        index_data = archive_member(bundle, "/dict/mueller-base.index")

    dictionary_data = gzip.decompress(compressed_data)
    entries = {}
    for raw_line in index_data.decode("utf-8").splitlines():
        parts = raw_line.split("\t")
        if len(parts) < 3:
            continue
        try:
            offset = decode_dict_number(parts[1])
            size = decode_dict_number(parts[2])
        except (KeyError, ValueError):
            continue
        if offset < 0 or size <= 0 or offset + size > len(dictionary_data):
            continue
        key = parts[0].strip().casefold()
        entries.setdefault(key, []).append((offset, size))

    if len(entries) < 40000:
        raise RuntimeError(
            "Mueller index unexpectedly contains only " + str(len(entries)) + " keys"
        )
    print("Loaded " + str(len(entries)) + " Mueller headwords")
    return dictionary_data, entries


def compact_russian(value: str) -> str:
    value = normalized(value)
    prefix = value[:48].casefold()
    if any(label in prefix for label in SPECIALIZED_LABELS):
        return ""

    value = re.sub(r"\{[^}]*\}", " ", value)
    value = re.sub(r"^(?:_[^\s]+\s*)+", "", value)
    value = re.sub(r"^(?:\([^)]*\)\s*)+", "", value)
    value = re.sub(r"^(?:_[^\s]+\s*)+", "", value)
    value = value.split(";", 1)[0]
    value = re.sub(r"\([^)]*\)", " ", value)
    value = re.sub(r"_[^\s]+", " ", value)

    latin = LATIN.search(value)
    if latin:
        value = value[: latin.start()]

    value = re.sub(r"[^А-Яа-яЁё,\- ]+", " ", value)
    value = normalized(value).strip(" ,-")
    if not value:
        return ""

    pieces = []
    seen = set()
    for raw_piece in value.split(","):
        piece = normalized(raw_piece).strip(" -")
        key = piece.casefold()
        if not piece or key in seen:
            continue
        if key.startswith(("см ", "см.", "то же", "мн.", "ед.")):
            continue
        if len(CYRILLIC.findall(piece)) < 2 or len(piece.split()) > 4:
            continue
        candidate = ", ".join(pieces + [piece])
        if len(candidate) > 56:
            break
        seen.add(key)
        pieces.append(piece)
        if len(pieces) == 3:
            break
    return ", ".join(pieces)


def extract_translation(entry: str) -> str:
    candidates = []
    for line in entry.splitlines()[1:]:
        stripped = line.strip()
        sense = re.match(
            r"^(?:\d+|[a-zа-я])\)\s*(.+)$",
            stripped,
            re.IGNORECASE,
        )
        if sense:
            candidates.append(sense.group(1))
            continue
        part_of_speech = re.match(
            r"^(?:\d+\.\s*)?(?:_[^\s]+\s*)+(.+)$",
            stripped,
        )
        if part_of_speech:
            candidates.append(part_of_speech.group(1))

    for candidate in candidates:
        translation = compact_russian(candidate)
        if translation:
            return translation
    return ""


def acceptable_english_source(value: str) -> bool:
    if not ENGLISH_WORD.fullmatch(value) or len(value) > 32:
        return False
    if value != "I" and value[:1].isupper():
        return False
    return True


def english_candidates():
    primary = download(ENGLISH_FREQUENCY_URL).decode("utf-8")
    for item in csv.DictReader(io.StringIO(primary)):
        yield normalized(item.get("ngram", ""))

    extended = download(EXTENDED_FREQUENCY_URL).decode("utf-8")
    matched = 0
    for line in extended.splitlines():
        match = GWORD_ROW.match(line)
        if match:
            matched += 1
            yield match.group(1)
    if matched < 100000:
        raise RuntimeError(
            "Extended frequency list format changed; parsed only "
            + str(matched)
            + " rows"
        )


def build_en_ru(dictionary_data, entries):
    rows = []
    seen = set()
    for source in english_candidates():
        if not acceptable_english_source(source):
            continue
        key = source.casefold()
        if key in seen or key not in entries:
            continue

        target = CORE_OVERRIDES.get(key, "")
        if not target:
            for offset, size in entries[key]:
                entry = dictionary_data[offset : offset + size].decode(
                    "utf-8",
                    errors="replace",
                )
                target = extract_translation(entry)
                if target:
                    break
        if not target:
            continue

        seen.add(key)
        rows.append((source, target))
        if len(rows) == EN_RU_LIMIT:
            break

    if len(rows) != EN_RU_LIMIT:
        raise RuntimeError(
            "Could prepare only "
            + str(len(rows))
            + " of "
            + str(EN_RU_LIMIT)
            + " English-to-Russian cards"
        )

    mapping = {source.casefold(): target for source, target in rows}
    if not mapping.get("go", "").startswith("идти"):
        raise RuntimeError("Quality gate failed for go: " + repr(mapping.get("go")))
    if mapping.get("make") != CORE_OVERRIDES["make"]:
        raise RuntimeError("Quality gate failed for make")
    return rows


def build_ru_en():
    payload = download(RUSSIAN_FREQUENCY_URL).decode("utf-8")
    rows = []
    seen = set()
    for item in csv.DictReader(io.StringIO(payload)):
        source = normalized(item.get("ngram", ""))
        target = normalized(item.get("en", "")).replace("’", "'")
        key = source.casefold()
        if key in seen:
            continue
        if not RUSSIAN_WORD.fullmatch(source):
            continue
        if not ENGLISH_TRANSLATION.fullmatch(target):
            continue
        if len(target.split()) > 4:
            continue
        if len(source) == 1 and source.casefold() != "я":
            continue
        seen.add(key)
        rows.append((source, target))
        if len(rows) == RU_EN_LIMIT:
            break

    if len(rows) != RU_EN_LIMIT:
        raise RuntimeError(
            "Could prepare only "
            + str(len(rows))
            + " of "
            + str(RU_EN_LIMIT)
            + " Russian-to-English cards"
        )
    return rows


def write_csv(path: pathlib.Path, rows) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.writer(stream)
        writer.writerow(("source", "target"))
        writer.writerows(rows)


def main() -> None:
    dictionary_data, entries = load_mueller()
    en_ru = build_en_ru(dictionary_data, entries)
    ru_en = build_ru_en()
    write_csv(EN_RU_OUTPUT, en_ru)
    write_csv(RU_EN_OUTPUT, ru_en)
    print(
        "Prepared "
        + str(len(en_ru))
        + " EN->RU and "
        + str(len(ru_en))
        + " RU->EN offline cards"
    )
    print("Quality sample: go -> " + dict(en_ru)["go"])


if __name__ == "__main__":
    main()
