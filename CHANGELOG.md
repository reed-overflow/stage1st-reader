<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# stage1st-reader Changelog

## [Unreleased]
### Added
- GitHub Actions build/release automation with Marketplace signing, release guards, and publishing documentation.
- Application-wide forum URL validation and discreet window preferences.
- Persistent login sessions in local IDE settings, browser Cookie import, ordinary topics/replies and BBCode quotes.
- Page navigation, current-page search/filter, local bookmarks, and a hide shortcut.

### Fixed
- Use Discuz `tpp`/`ppp` metadata for pagination and report API permission/maintenance errors.
- Isolate site/account cookies, prevent stale requests from repopulating switched views, and release project readers.
- Preserve links and image references when rendering posts as plain text.
- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
