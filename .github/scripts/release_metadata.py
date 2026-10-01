"""Release guards shared by Build, Release and the draft helper. Standard library only."""
import argparse
import json
import os
from pathlib import Path
import re
import sys

# Deliberately exclude build metadata and numeric channel names (Marketplace channel semantics).
VERSION = re.compile(r"(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)(?:-([a-z][a-z0-9]*)(?:\.(?:0|[1-9][0-9]*))?)?")
SECTION = re.compile(r"^## \[([^\]]+)\](?:[^\r\n]*)$", re.MULTILINE)
REQUIRED_SECRETS = ("PUBLISH_TOKEN", "CERTIFICATE_CHAIN", "PRIVATE_KEY", "PRIVATE_KEY_PASSWORD")


def read_properties(path):
    values = {}
    for line in path.read_text(encoding="utf-8-sig").splitlines():
        if not line.strip() or line.lstrip().startswith(("#", "!")):
            continue
        key, separator, value = line.partition("=")
        if separator:
            key = key.strip()
            if key in values:
                raise ValueError("Duplicate Gradle property: " + key)
            values[key] = value.strip()
    return values


def release_notes(markdown, version):
    headings = list(SECTION.finditer(markdown))
    sections = {}
    for index, heading in enumerate(headings):
        key = heading.group(1)
        if key in sections:
            raise ValueError("Duplicate changelog section: " + key)
        end = headings[index + 1].start() if index + 1 < len(headings) else len(markdown)
        sections[key] = markdown[heading.end():end].strip()
    notes = sections.get(version, sections.get("Unreleased", ""))
    # Headings alone are not release notes. Require a real bullet, matching gradle-changelog 1.x.
    if not re.search(r"(?m)^-\s+\S", notes):
        raise ValueError("CHANGELOG.md needs bullet entries under [" + version + "] or [Unreleased].")
    return notes + "\n"


def metadata(root):
    properties = read_properties(root / "gradle.properties")
    version = properties.get("pluginVersion", "")
    match = VERSION.fullmatch(version)
    if not match:
        raise ValueError("pluginVersion must be X.Y.Z or X.Y.Z-channel[.N], e.g. 0.0.36-beta.1.")
    if match.group(1) == "default":
        raise ValueError("The default channel is reserved for versions without a pre-release suffix.")
    name = properties.get("pluginName", "")
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]*", name):
        raise ValueError("pluginName is missing or unsuitable for a distribution filename.")
    values = {
        "version": version,
        "tag": "v" + version,
        "channel": match.group(1) or "default",
        "prerelease": "true" if match.group(1) else "false",
        "artifact_name": name + "-" + version,
    }
    notes = release_notes((root / "CHANGELOG.md").read_text(encoding="utf-8-sig"), version)
    return values, notes


def validate_release(values, event):
    release = event.get("release", {})
    if event.get("action") != "published" or release.get("draft") is not False:
        raise ValueError("Only a published GitHub Release may upload to Marketplace.")
    if release.get("tag_name") != values["tag"]:
        raise ValueError("Release tag must equal " + values["tag"] + " from the tagged gradle.properties.")
    expected = values["prerelease"] == "true"
    if release.get("prerelease") is not expected:
        raise ValueError("GitHub pre-release flag must match the version suffix; channel is " + values["channel"] + ".")


def check_secrets(environment):
    missing = [name for name in REQUIRED_SECRETS if not environment.get(name, "").strip()]
    if missing:
        raise ValueError("Missing GitHub Actions secrets: " + ", ".join(missing) + ". See docs/publishing.md.")
    # Never print values or exception text containing credential contents.


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--release", action="store_true")
    parser.add_argument("--check-secrets", action="store_true")
    parser.add_argument("--notes-file", type=Path)
    args = parser.parse_args()
    if args.check_secrets:
        check_secrets(os.environ)
        print("All required publishing secrets are present.")
        return
    values, notes = metadata(Path.cwd())
    if args.release:
        event = json.loads(Path(os.environ["GITHUB_EVENT_PATH"]).read_text(encoding="utf-8"))
        validate_release(values, event)
    if args.notes_file:
        args.notes_file.parent.mkdir(parents=True, exist_ok=True)
        args.notes_file.write_text(notes, encoding="utf-8")
    if os.environ.get("GITHUB_OUTPUT"):
        with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as output:
            for key, value in values.items():
                output.write(key + "=" + value + "\n")
    print("Version: {version}; tag: {tag}; Marketplace channel: {channel}".format(**values))


if __name__ == "__main__":
    try:
        main()
    except (ValueError, KeyError, OSError) as error:
        print("Release validation failed: " + str(error), file=sys.stderr)
        sys.exit(1)
