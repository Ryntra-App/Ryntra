from __future__ import annotations

import re
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path


ANDROID_PLACEHOLDER = re.compile(
    r"%(?:\d+\$)?[#+\- 0,(<]*\d*(?:\.\d+)?(?:[a-zA-Z]|%)"
)
IOS_PLACEHOLDER = re.compile(
    r"%(?:\d+\$)?[-+0 #'\u2009]*(?:\d+|\*)?(?:\.(?:\d+|\*))?"
    r"(?:hh|h|ll|l|q|z|t|j)?(?:[@a-zA-Z]|%)"
)
IOS_ENTRY = re.compile(
    r'^\s*"((?:\\.|[^"\\])*)"\s*=\s*"((?:\\.|[^"\\])*)"\s*;\s*$'
)


def android_entries(path: Path) -> tuple[dict[str, str], dict[str, dict[str, str]]]:
    root = ET.parse(path).getroot()
    strings: dict[str, str] = {}
    plurals: dict[str, dict[str, str]] = {}
    for child in root:
        name = child.attrib.get("name")
        if not name:
            continue
        if child.tag == "string":
            if name in strings:
                raise ValueError(f"{path}: duplicate string key {name!r}")
            strings[name] = "".join(child.itertext())
        elif child.tag == "plurals":
            if name in plurals:
                raise ValueError(f"{path}: duplicate plurals key {name!r}")
            plurals[name] = {
                item.attrib["quantity"]: "".join(item.itertext())
                for item in child.findall("item")
            }
    return strings, plurals


def ios_entries(path: Path) -> dict[str, str]:
    entries: dict[str, str] = {}
    in_block_comment = False
    for line_number, line in enumerate(path.read_text(encoding="utf-8-sig").splitlines(), 1):
        stripped = line.strip()
        if in_block_comment:
            if "*/" in stripped:
                in_block_comment = False
            continue
        if stripped.startswith("/*"):
            in_block_comment = "*/" not in stripped
            continue
        if not stripped or stripped.startswith("//"):
            continue
        match = IOS_ENTRY.fullmatch(line)
        if match is None:
            raise ValueError(f"{path}:{line_number}: invalid .strings entry")
        key, value = match.groups()
        if key in entries:
            raise ValueError(f"{path}:{line_number}: duplicate key {key!r}")
        entries[key] = value
    return entries


def placeholder_signature(value: str, *, ios: bool) -> Counter[str]:
    pattern = IOS_PLACEHOLDER if ios else ANDROID_PLACEHOLDER
    return Counter(pattern.findall(value))
