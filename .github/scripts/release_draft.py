"""Create/update only this version's draft, targeting the commit that passed Build."""
import json
import os
from pathlib import Path
import subprocess
import sys

from release_metadata import metadata


def gh(*arguments):
    return subprocess.check_output(["gh", *arguments], text=True, encoding="utf-8")


def existing_tag_commit(repository, tag):
    refs = json.loads(gh("api", "repos/" + repository + "/git/matching-refs/tags/" + tag))
    for ref in refs:
        if ref["ref"] != "refs/tags/" + tag:
            continue
        obj = ref["object"]
        # Support annotated tags as well as lightweight tags.
        for _ in range(10):
            if obj["type"] == "commit":
                return obj["sha"]
            if obj["type"] != "tag":
                break
            obj = json.loads(gh("api", "repos/" + repository + "/git/tags/" + obj["sha"]))["object"]
        raise ValueError("Tag does not resolve to a commit: " + tag)
    return None


def main():
    values, notes = metadata(Path.cwd())
    repository = os.environ["GITHUB_REPOSITORY"]
    commit = os.environ["GITHUB_SHA"]
    tag = values["tag"]
    pages = json.loads(gh("api", "--paginate", "--slurp", "repos/" + repository + "/releases"))
    matches = [release for page in pages for release in page if release["tag_name"] == tag]
    if any(not release["draft"] for release in matches):
        print(tag + " is already published; bump pluginVersion to prepare another release.")
        return
    tag_commit = existing_tag_commit(repository, tag)
    if tag_commit is not None and tag_commit != commit:
        raise ValueError(tag + " already points to another commit. Choose a new version; this workflow never moves tags.")
    notes_file = Path("build/release-notes.md")
    notes_file.parent.mkdir(parents=True, exist_ok=True)
    notes_file.write_text(notes, encoding="utf-8")
    command = "edit" if matches else "create"
    arguments = ["release", command, tag, "--repo", repository, "--draft",
                 "--title", tag, "--target", commit, "--notes-file", str(notes_file),
                 "--prerelease=" + values["prerelease"]]
    print(gh(*arguments).strip() or "Updated draft " + tag)


if __name__ == "__main__":
    try:
        main()
    except (ValueError, KeyError, OSError, subprocess.CalledProcessError) as error:
        print("Could not prepare release draft: " + str(error), file=sys.stderr)
        sys.exit(1)
