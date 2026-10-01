import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import release_draft
from release_metadata import check_secrets, metadata, read_properties, release_notes, validate_release


class ReleaseMetadataTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.configure()

    def configure(self, version="0.0.36", notes="## [Unreleased]\n### Added\n- A useful change.\n"):
        (self.root / "gradle.properties").write_text(
            "pluginName = stage1st-reader\npluginVersion = " + version + "\n", encoding="utf-8")
        (self.root / "CHANGELOG.md").write_text(notes, encoding="utf-8")

    def event(self, tag="v0.0.36", prerelease=False, draft=False):
        return {"action": "published", "release": {"tag_name": tag, "prerelease": prerelease, "draft": draft}}

    def test_stable_release(self):
        values, notes = metadata(self.root)
        self.assertEqual(values["channel"], "default")
        self.assertEqual(values["artifact_name"], "stage1st-reader-0.0.36")
        self.assertEqual(values["tag"], "v0.0.36")
        validate_release(values, self.event())
        self.assertIn("A useful change.", notes)

    def test_prerelease_channel(self):
        for suffix, channel in (("beta.1", "beta"), ("rc.2", "rc"), ("alpha", "alpha")):
            with self.subTest(suffix=suffix):
                self.configure("0.0.36-" + suffix)
                values, _ = metadata(self.root)
                self.assertEqual(values["channel"], channel)
                validate_release(values, self.event("v0.0.36-" + suffix, prerelease=True))

    def test_invalid_versions_rejected(self):
        for version in ("", "v0.0.36", "01.0.0", "1.0", "1.0.0+build", "1.0.0-beta.01", "1.0.0-1", "1.0.0\nBAD=value"):
            with self.subTest(version=version):
                self.configure(version)
                # A newline creates another property, so use the version validator directly.
                from release_metadata import VERSION
                self.assertIsNone(VERSION.fullmatch(version))

    def test_duplicate_property_rejected(self):
        path = self.root / "gradle.properties"
        path.write_text("pluginVersion=1.0.0\npluginVersion=2.0.0\n", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "Duplicate"):
            read_properties(path)

    def test_prerelease_cannot_target_default_channel(self):
        self.configure("0.0.36-default.1")
        with self.assertRaisesRegex(ValueError, "reserved"):
            metadata(self.root)

    def test_shell_text_in_plugin_name_rejected(self):
        path = self.root / "gradle.properties"
        path.write_text("pluginName=x$(bad)\npluginVersion=1.0.0\n", encoding="utf-8")
        with self.assertRaises(ValueError):
            metadata(self.root)

    def test_tag_must_match_version(self):
        values, _ = metadata(self.root)
        for tag in ("0.0.36", "v0.0.37", "v0.0.36;bad"):
            with self.subTest(tag=tag), self.assertRaises(ValueError):
                validate_release(values, self.event(tag))

    def test_pre_release_flag_must_match_channel(self):
        values, _ = metadata(self.root)
        with self.assertRaises(ValueError):
            validate_release(values, self.event(prerelease=True))
        self.configure("0.0.36-beta.1")
        values, _ = metadata(self.root)
        with self.assertRaises(ValueError):
            validate_release(values, self.event("v0.0.36-beta.1", prerelease=False))

    def test_draft_and_non_publish_event_cannot_publish(self):
        values, _ = metadata(self.root)
        with self.assertRaises(ValueError):
            validate_release(values, self.event(draft=True))
        event = self.event()
        event["action"] = "edited"
        with self.assertRaises(ValueError):
            validate_release(values, event)

    def test_versioned_notes_take_priority(self):
        notes = release_notes(
            "## [Unreleased]\n### Added\n- Future.\n\n## [0.0.36] - 2026-09-29\n### Fixed\n- Shipped.\n", "0.0.36")
        self.assertIn("Shipped.", notes)
        self.assertNotIn("Future.", notes)

    def test_empty_and_stale_notes_rejected(self):
        for notes in ("", "## [Unreleased]\n### Added\n", "## [0.0.35]\n- Old change.\n"):
            with self.subTest(notes=notes), self.assertRaises(ValueError):
                release_notes(notes, "0.0.36")

    def test_duplicate_changelog_section_rejected(self):
        with self.assertRaisesRegex(ValueError, "Duplicate"):
            release_notes("## [Unreleased]\n- A.\n## [Unreleased]\n- B.\n", "0.0.36")

    def test_credential_errors_only_name_missing_keys(self):
        environment = {"PUBLISH_TOKEN": "secret-token", "PRIVATE_KEY": "secret-key",
                       "CERTIFICATE_CHAIN": "secret-chain", "PRIVATE_KEY_PASSWORD": " "}
        with self.assertRaises(ValueError) as raised:
            check_secrets(environment)
        self.assertIn("PRIVATE_KEY_PASSWORD", str(raised.exception))
        self.assertNotIn("secret-token", str(raised.exception))
        self.assertNotIn("secret-key", str(raised.exception))
        environment["PRIVATE_KEY_PASSWORD"] = "secret-password"
        check_secrets(environment)


class ReleaseDraftTest(unittest.TestCase):
    def test_annotated_tag_is_resolved(self):
        responses = [
            json.dumps([{"ref": "refs/tags/v1.0.0", "object": {"type": "tag", "sha": "tag-object"}}]),
            json.dumps({"object": {"type": "commit", "sha": "commit"}})
        ]
        with patch.object(release_draft, "gh", side_effect=responses):
            self.assertEqual(release_draft.existing_tag_commit("owner/repo", "v1.0.0"), "commit")

    def test_similar_prefix_is_not_exact_tag(self):
        refs = [{"ref": "refs/tags/v1.0.01", "object": {"type": "commit", "sha": "other"}}]
        with patch.object(release_draft, "gh", return_value=json.dumps(refs)):
            self.assertIsNone(release_draft.existing_tag_commit("owner/repo", "v1.0.0"))

    def test_published_release_is_never_modified(self):
        values = {"tag": "v1.0.0", "prerelease": "false"}
        with patch.object(release_draft, "metadata", return_value=(values, "- Notes.\n")), \
                patch.dict(os.environ, {"GITHUB_REPOSITORY": "owner/repo", "GITHUB_SHA": "commit"}), \
                patch.object(release_draft, "gh", return_value=json.dumps([[{"tag_name": "v1.0.0", "draft": False}]])) as gh, \
                patch.object(release_draft, "existing_tag_commit") as resolve:
            release_draft.main()
            gh.assert_called_once()
            resolve.assert_not_called()

    def test_conflicting_tag_cannot_be_retargeted(self):
        values = {"tag": "v1.0.0", "prerelease": "false"}
        with patch.object(release_draft, "metadata", return_value=(values, "- Notes.\n")), \
                patch.dict(os.environ, {"GITHUB_REPOSITORY": "owner/repo", "GITHUB_SHA": "new-commit"}), \
                patch.object(release_draft, "gh", return_value=json.dumps([[]])) as gh, \
                patch.object(release_draft, "existing_tag_commit", return_value="old-commit"):
            with self.assertRaisesRegex(ValueError, "another commit"):
                release_draft.main()
            gh.assert_called_once()

    def test_draft_update_targets_verified_commit_with_notes_file(self):
        values = {"tag": "v1.0.0-beta.1", "prerelease": "true"}
        with tempfile.TemporaryDirectory() as directory, \
                patch.object(release_draft, "metadata", return_value=(values, "- Notes.\n")), \
                patch.dict(os.environ, {"GITHUB_REPOSITORY": "owner/repo", "GITHUB_SHA": "verified-commit"}), \
                patch.object(release_draft, "existing_tag_commit", return_value=None), \
                patch.object(release_draft, "Path", side_effect=lambda value=".": Path(directory) / value), \
                patch.object(release_draft, "gh", side_effect=[
                    json.dumps([[{"tag_name": values["tag"], "draft": True}]]), ""
                ]) as gh:
            release_draft.main()
            arguments = gh.call_args.args
            self.assertEqual(arguments[:3], ("release", "edit", values["tag"]))
            self.assertIn("--prerelease=true", arguments)
            self.assertEqual(arguments[arguments.index("--target") + 1], "verified-commit")
            self.assertIn("--notes-file", arguments)
            self.assertEqual((Path(directory) / "build/release-notes.md").read_text(encoding="utf-8"), "- Notes.\n")


if __name__ == "__main__":
    unittest.main()
