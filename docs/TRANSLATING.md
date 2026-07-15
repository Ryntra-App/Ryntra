# Translating Ryntra

Ryntra ships native Android and iOS interfaces. Every translation pull request must update both platforms so users receive the same language coverage everywhere.

## Start a new language

1. Fork the repository, create a branch, and install Python 3.11 or newer.
2. From the repository root, run the localization scaffold with a [BCP 47 language tag](https://www.rfc-editor.org/rfc/bcp/bcp47.txt) and the language name written in that language:

   ```bash
   python tools/localization.py create de "Deutsch"
   ```

   Region and script variants are supported:

   ```bash
   python tools/localization.py create pt-BR "Português (Brasil)"
   python tools/localization.py create zh-Hans "简体中文"
   ```

3. The command registers the language in Android, iOS, and Xcode. It prints the two files that need translation.
4. Translate the values in both files. Do not rename or remove keys.
5. Validate the result:

   ```bash
   python tools/localization.py check
   ./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug
   ```

6. Open a pull request using the [translation PR template](../.github/PULL_REQUEST_TEMPLATE/translation.md).

## Files to translate

Android uses semantic XML keys:

```xml
<string name="nav_projects">Projects</string>
```

Translate only the text between the tags. Keep `name="nav_projects"` unchanged.

iOS uses the English source phrase as the key:

```text
"Projects" = "Projects";
```

Translate only the value on the right:

```text
"Projects" = "Projekte";
```

For an existing language, skip the `create` command and edit its current Android and iOS files directly.

## Translation rules

- Preserve placeholders exactly: `%1$s`, `%1$d`, `%1$+.1f`, `%@`, and `%%` must remain present and keep their indices and types.
- Preserve escapes such as `\n`, `\"`, and apostrophes required by the source format.
- Keep `Ryntra`, `Modrinth`, `Markdown`, usernames, URLs, IDs, and file extensions unchanged.
- Use natural product language, not a literal word-for-word translation.
- Check short labels on a phone-sized screen. Navigation, tabs, buttons, and segmented controls must remain concise.
- Translate accessibility labels and error messages as carefully as visible controls.
- Do not translate user-generated project descriptions, version changelogs, organization names, or API data.

## What the validator checks

`python tools/localization.py check` fails when:

- Android or iOS is missing a registered locale;
- a translation has missing, duplicated, or unknown keys;
- Android XML or iOS `.strings` syntax is invalid;
- formatting placeholders differ from the English source;
- locale registration is missing from Android, Swift, or the Xcode project.

The same command runs in GitHub Actions for every pull request.

## Pull request scope

Keep one language per pull request. Translation PRs should contain localization files and registration changes produced by the tool, without unrelated code or formatting changes. Native speakers should mention whether they reviewed the translation on Android, iOS, or both.
