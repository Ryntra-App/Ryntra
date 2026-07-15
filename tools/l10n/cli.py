from __future__ import annotations

import argparse
import sys

from .scaffold import create_locale
from .validate import validate_all


def _parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Create and validate Android/iOS translations for Ryntra."
    )
    commands = parser.add_subparsers(dest="command", required=True)
    commands.add_parser("check", help="Validate all registered translations.")
    create = commands.add_parser("create", help="Create and register a new locale.")
    create.add_argument("tag", help="BCP 47 language tag, for example de or pt-BR.")
    create.add_argument("native_name", help='Native display name, for example "Deutsch".')
    return parser


def main(argv: list[str] | None = None) -> int:
    args = _parser().parse_args(argv)
    try:
        if args.command == "create":
            locale = create_locale(args.tag, args.native_name)
            print(f"Created {locale.tag}. Translate these files:")
            print(f"  androidApp/src/main/res/{locale.android_qualifier}/strings.xml")
            print(f"  iosApp/Ryntra/{locale.tag}.lproj/Localizable.strings")

        errors = validate_all()
    except (OSError, ValueError, KeyError) as error:
        print(f"Localization error: {error}", file=sys.stderr)
        return 1

    if errors:
        print("Localization validation failed:", file=sys.stderr)
        for error in errors:
            print(f"  - {error}", file=sys.stderr)
        return 1
    print("Localization validation passed.")
    return 0
