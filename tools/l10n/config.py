from __future__ import annotations

import json
import re
from dataclasses import asdict, dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MANIFEST_PATH = ROOT / "localization" / "locales.json"
ANDROID_RES = ROOT / "androidApp" / "src" / "main" / "res"
ANDROID_LOCALE_CONFIG = ANDROID_RES / "xml" / "locales_config.xml"
ANDROID_LANGUAGE_SOURCE = (
    ROOT
    / "androidApp"
    / "src"
    / "main"
    / "kotlin"
    / "com"
    / "ryntra"
    / "mobile"
    / "preferences"
    / "RyntraPreferences.kt"
)
IOS_ROOT = ROOT / "iosApp" / "Ryntra"
IOS_LANGUAGE_SOURCE = IOS_ROOT / "RyntraTheme.swift"
XCODE_PROJECT = ROOT / "iosApp" / "Ryntra.xcodeproj" / "project.pbxproj"


@dataclass(frozen=True)
class LocaleSpec:
    tag: str
    native_name: str
    android_qualifier: str
    android_name: str
    ios_case: str

    @classmethod
    def from_json(cls, raw: dict[str, str]) -> "LocaleSpec":
        return cls(
            tag=raw["tag"],
            native_name=raw["nativeName"],
            android_qualifier=raw["androidQualifier"],
            android_name=raw["androidName"],
            ios_case=raw["iosCase"],
        )

    def to_json(self) -> dict[str, str]:
        raw = asdict(self)
        return {
            "tag": raw["tag"],
            "nativeName": raw["native_name"],
            "androidQualifier": raw["android_qualifier"],
            "androidName": raw["android_name"],
            "iosCase": raw["ios_case"],
        }


def load_locales() -> list[LocaleSpec]:
    raw = json.loads(MANIFEST_PATH.read_text(encoding="utf-8"))
    return [LocaleSpec.from_json(item) for item in raw["locales"]]


def save_locales(locales: list[LocaleSpec]) -> None:
    payload = {"locales": [locale.to_json() for locale in locales]}
    MANIFEST_PATH.write_text(
        json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )


def validate_tag(tag: str) -> str:
    normalized = tag.replace("_", "-")
    parts = normalized.split("-")
    if not parts or not re.fullmatch(r"[A-Za-z]{2,3}", parts[0]):
        raise ValueError("Use a BCP 47 language tag such as de, pt-BR, or zh-Hans.")

    normalized_parts = [parts[0].lower()]
    for part in parts[1:]:
        if re.fullmatch(r"[A-Za-z]{4}", part):
            normalized_parts.append(part.title())
        elif re.fullmatch(r"[A-Za-z]{2}", part):
            normalized_parts.append(part.upper())
        elif re.fullmatch(r"\d{3}", part):
            normalized_parts.append(part)
        else:
            raise ValueError("Use a BCP 47 language tag such as de, pt-BR, or zh-Hans.")
    return "-".join(normalized_parts)


def create_spec(tag: str, native_name: str) -> LocaleSpec:
    normalized_tag = validate_tag(tag)
    identifier_parts = re.findall(r"[A-Za-z0-9]+", normalized_tag)
    android_name = "".join(part[:1].upper() + part[1:].lower() for part in identifier_parts)
    ios_case = android_name[:1].lower() + android_name[1:]
    qualifier = (
        f"values-{normalized_tag}"
        if len(identifier_parts) == 1
        else "values-b+" + "+".join(identifier_parts)
    )
    return LocaleSpec(
        tag=normalized_tag,
        native_name=native_name.strip(),
        android_qualifier=qualifier,
        android_name=android_name,
        ios_case=ios_case,
    )
