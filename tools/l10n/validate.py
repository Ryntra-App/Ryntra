from __future__ import annotations

import xml.etree.ElementTree as ET
from pathlib import Path

from .config import (
    ANDROID_LANGUAGE_SOURCE,
    ANDROID_LOCALE_CONFIG,
    ANDROID_RES,
    IOS_LANGUAGE_SOURCE,
    IOS_ROOT,
    XCODE_PROJECT,
    LocaleSpec,
    load_locales,
)
from .formats import android_entries, ios_entries, placeholder_signature


def _compare_keys(
    base: dict[str, object],
    translated: dict[str, object],
    path: Path,
    kind: str,
) -> list[str]:
    errors: list[str] = []
    missing = sorted(base.keys() - translated.keys())
    extra = sorted(translated.keys() - base.keys())
    if missing:
        errors.append(f"{path}: missing {kind}: {', '.join(missing)}")
    if extra:
        errors.append(f"{path}: unknown {kind}: {', '.join(extra)}")
    return errors


def _validate_android_locale(spec: LocaleSpec, base_path: Path) -> list[str]:
    path = ANDROID_RES / spec.android_qualifier / "strings.xml"
    if not path.is_file():
        return [f"{path}: file is missing"]
    try:
        base_strings, base_plurals = android_entries(base_path)
        strings, plurals = android_entries(path)
    except (ET.ParseError, ValueError) as error:
        return [str(error)]

    errors = _compare_keys(base_strings, strings, path, "string keys")
    errors.extend(_compare_keys(base_plurals, plurals, path, "plurals keys"))
    if path == base_path:
        return errors

    for key in base_strings.keys() & strings.keys():
        expected = placeholder_signature(base_strings[key], ios=False)
        actual = placeholder_signature(strings[key], ios=False)
        if expected != actual:
            errors.append(f"{path}: placeholders differ for {key!r}: {actual} != {expected}")

    for key in base_plurals.keys() & plurals.keys():
        fallback = base_plurals[key].get("other", "")
        for quantity, value in plurals[key].items():
            source = base_plurals[key].get(quantity, fallback)
            expected = placeholder_signature(source, ios=False)
            actual = placeholder_signature(value, ios=False)
            if expected != actual:
                errors.append(
                    f"{path}: placeholders differ for {key!r}/{quantity}: {actual} != {expected}"
                )
    return errors


def _validate_ios_locale(spec: LocaleSpec, base_entries: dict[str, str]) -> list[str]:
    path = IOS_ROOT / f"{spec.tag}.lproj" / "Localizable.strings"
    if not path.is_file():
        return [f"{path}: file is missing"]
    try:
        entries = ios_entries(path)
    except ValueError as error:
        return [str(error)]

    errors = _compare_keys(base_entries, entries, path, "keys")
    for key in base_entries.keys() & entries.keys():
        expected = placeholder_signature(base_entries[key], ios=True)
        actual = placeholder_signature(entries[key], ios=True)
        if expected != actual:
            errors.append(f"{path}: placeholders differ for {key!r}: {actual} != {expected}")
    return errors


def validate_all() -> list[str]:
    errors: list[str] = []
    locales = load_locales()
    tags = [locale.tag for locale in locales]
    if len(tags) != len(set(tags)):
        errors.append("localization/locales.json: locale tags must be unique")
    if not locales or locales[0].tag != "en":
        errors.append("localization/locales.json: English must remain the source locale")
        return errors

    configured_tags = [
        node.attrib["{http://schemas.android.com/apk/res/android}name"]
        for node in ET.parse(ANDROID_LOCALE_CONFIG).getroot().findall("locale")
    ]
    if configured_tags != tags:
        errors.append(
            "android locales_config.xml does not match localization/locales.json: "
            f"{configured_tags} != {tags}"
        )

    kotlin_source = ANDROID_LANGUAGE_SOURCE.read_text(encoding="utf-8")
    swift_source = IOS_LANGUAGE_SOURCE.read_text(encoding="utf-8")
    xcode_source = XCODE_PROJECT.read_text(encoding="utf-8")
    for locale in locales:
        if f'{locale.android_name}("{locale.native_name}", "{locale.tag}")' not in kotlin_source:
            errors.append(f"Android AppLanguage is not registered for {locale.tag}")
        if f'case {locale.ios_case} = "{locale.tag}"' not in swift_source:
            errors.append(f"iOS RyntraAppLanguage is not registered for {locale.tag}")
        if f"{locale.tag}.lproj/Localizable.strings" not in xcode_source:
            errors.append(f"Xcode Localizable.strings is not registered for {locale.tag}")

    base_android = ANDROID_RES / locales[0].android_qualifier / "strings.xml"
    for locale in locales:
        errors.extend(_validate_android_locale(locale, base_android))

    base_ios_path = IOS_ROOT / f"{locales[0].tag}.lproj" / "Localizable.strings"
    try:
        base_ios = ios_entries(base_ios_path)
    except ValueError as error:
        return errors + [str(error)]
    for locale in locales:
        errors.extend(_validate_ios_locale(locale, base_ios))
    return errors
