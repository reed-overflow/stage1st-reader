<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# stage1st-reader Changelog

## [Unreleased]
### Added
- GitHub Actions build/release automation with Marketplace signing, release guards, and publishing documentation.
- Application-wide forum URL validation and discreet window preferences.
- Persistent login sessions in local IDE settings, browser Cookie import, ordinary topics/replies and BBCode quotes.
- Page navigation, current-page search/filter, local bookmarks, and a hide shortcut.

### Changed
- Migrate to IntelliJ Platform Gradle Plugin 2.13.1 and Gradle 9.8.0; require IDEA 2022.3 (223) or newer and Java 17.
- Remove the IDE version upper bound and verify compatibility against IDEA 2022.3 and 2025.2.6.3 Community.
- Replace legacy service/content factory APIs and remove Java 11 and manual instrumentation workarounds.
- Pin the Gradle daemon to automatically provisioned Temurin 17 to avoid Ant's legacy Microsoft JVM detection.

### Fixed
- Update HTTP Client and remove the unused Kotlin JVM plugin.
- Use Discuz `tpp`/`ppp` metadata for pagination and report API permission/maintenance errors.
- Isolate site/account cookies, prevent stale requests from repopulating switched views, and release project readers.
- Preserve links and image references when rendering posts as plain text.
- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
