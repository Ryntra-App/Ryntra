from __future__ import annotations

import hashlib
import re
import shutil

from .config import (
    ANDROID_LANGUAGE_SOURCE,
    ANDROID_LOCALE_CONFIG,
    ANDROID_RES,
    IOS_LANGUAGE_SOURCE,
    IOS_ROOT,
    XCODE_PROJECT,
    LocaleSpec,
    create_spec,
    load_locales,
    save_locales,
)


def _escaped(value: str) -> str:
    return value.replace("\\", "\\\\").replace('"', '\\"')


def _replace_generated_block(source: str, marker: str, content: str) -> str:
    start = f"// <localization-tool:{marker}>"
    end = f"// </localization-tool:{marker}>"
    pattern = re.compile(rf"({re.escape(start)}\n).*?(\n\s*{re.escape(end)})", re.DOTALL)
    updated, count = pattern.subn(rf"\1{content}\2", source, count=1)
    if count != 1:
        raise ValueError(f"Generated localization block {marker!r} was not found")
    return updated


def _register_android(locales: list[LocaleSpec]) -> None:
    locale_lines = "\n".join(
        f'    <locale android:name="{locale.tag}" />' for locale in locales
    )
    ANDROID_LOCALE_CONFIG.write_text(
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<locale-config xmlns:android="http://schemas.android.com/apk/res/android">\n'
        f"{locale_lines}\n"
        "</locale-config>\n",
        encoding="utf-8",
    )

    enum_lines = ["    System(\"System\", null),"]
    enum_lines.extend(
        f'    {locale.android_name}("{_escaped(locale.native_name)}", "{locale.tag}"),'
        for locale in locales
    )
    source = ANDROID_LANGUAGE_SOURCE.read_text(encoding="utf-8")
    source = _replace_generated_block(source, "android-languages", "\n".join(enum_lines))
    ANDROID_LANGUAGE_SOURCE.write_text(source, encoding="utf-8")


def _register_ios(locales: list[LocaleSpec]) -> None:
    case_lines = ["    case system"]
    case_lines.extend(f'    case {locale.ios_case} = "{locale.tag}"' for locale in locales)
    label_lines = [
        '        case .system: return NSLocalizedString("System", comment: "Language")'
    ]
    label_lines.extend(
        f'        case .{locale.ios_case}: return "{_escaped(locale.native_name)}"'
        for locale in locales
    )
    source = IOS_LANGUAGE_SOURCE.read_text(encoding="utf-8")
    source = _replace_generated_block(source, "ios-language-cases", "\n".join(case_lines))
    source = _replace_generated_block(source, "ios-language-labels", "\n".join(label_lines))
    IOS_LANGUAGE_SOURCE.write_text(source, encoding="utf-8")


def _register_xcode(locale: LocaleSpec) -> None:
    source = XCODE_PROJECT.read_text(encoding="utf-8")
    path = f"{locale.tag}.lproj/Localizable.strings"
    if path in source:
        return

    file_id = hashlib.sha1(f"ryntra:{path}".encode()).hexdigest()[:24].upper()
    file_reference = (
        f'\t\t{file_id} /* Localizable.strings */ = '
        f'{{isa = PBXFileReference; lastKnownFileType = text.plist.strings; '
        f'name = Localizable.strings; path = {path}; sourceTree = "<group>"; }};\n'
    )
    source = source.replace(
        "/* End PBXFileReference section */",
        file_reference + "/* End PBXFileReference section */",
        1,
    )
    source, region_count = re.subn(
        r"knownRegions = \((.*?)\);",
        lambda match: f"knownRegions = ({match.group(1)}{locale.tag}, );",
        source,
        count=1,
        flags=re.DOTALL,
    )
    source, group_count = re.subn(
        r"(D006 /\* Localizable\.strings \*/ = \{isa = PBXVariantGroup; children = \()([^)]*)",
        lambda match: f"{match.group(1)}{match.group(2)}{file_id}, ",
        source,
        count=1,
    )
    if region_count != 1 or group_count != 1:
        raise ValueError("Could not update Xcode localization groups")
    XCODE_PROJECT.write_text(source, encoding="utf-8")


def create_locale(tag: str, native_name: str) -> LocaleSpec:
    locale = create_spec(tag, native_name)
    locales = load_locales()
    if any(item.tag == locale.tag for item in locales):
        raise ValueError(f"Locale {locale.tag!r} already exists")
    if not locale.native_name:
        raise ValueError("Native language name must not be empty")

    android_target = ANDROID_RES / locale.android_qualifier / "strings.xml"
    ios_target = IOS_ROOT / f"{locale.tag}.lproj" / "Localizable.strings"
    if android_target.exists() or ios_target.exists():
        raise ValueError(f"Localization files for {locale.tag!r} already exist")

    android_target.parent.mkdir(parents=True)
    ios_target.parent.mkdir(parents=True)
    shutil.copy2(ANDROID_RES / "values" / "strings.xml", android_target)
    shutil.copy2(IOS_ROOT / "en.lproj" / "Localizable.strings", ios_target)

    updated_locales = [*locales, locale]
    _register_android(updated_locales)
    _register_ios(updated_locales)
    _register_xcode(locale)
    save_locales(updated_locales)
    return locale
