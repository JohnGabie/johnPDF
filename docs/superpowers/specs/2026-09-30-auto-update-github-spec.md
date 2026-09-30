# johnPDF Auto-Update via GitHub — Specification

**Document**: johnPDF Auto-Update System (Path A — Lightweight)  
**Date**: 2026-09-30 (Implementation started 2026-09-30)  
**Branch**: feat/v1  
**Status**: Path A data + UI layer IMPLEMENTED; CI workflow (Phase 2) pending keystore recovery

---

## 0. Implementation Status (as of 2026-09-30)

### ✅ Completed (Commit a03c025)

- **UpdateRepository** (`data/UpdateRepository.kt`): GitHub API client with 24h cache, silent-fail on errors
- **UpdateAvailableDialog** (`ui/common/Dialogs.kt`): Material3 AlertDialog, theme-aware, links to release page
- **HomeScreen integration**: Checks for update on launch, shows dialog if remote > local (1)
- **AppContainer**: Injects `updates` repository
- **INTERNET permission**: Added to manifest

### ⏳ Pending (Phase 2, requires keystore recovery)

- **CI/Release Workflow** (`.github/workflows/release.yml`): Builds, signs, publishes APK + latest.json
- **Version increment strategy**: versionCode/versionName from git tags
- **Publish first release** (v1.0.0 tag) to validate workflow

### ℹ️ Notes

- Currently hardcoded version comparison: remote > 1. Will use BuildConfig.VERSION_CODE after versionCode derivation from tags (Phase 2).
- Network/parse errors are silently handled; user can dismiss dialog.
- `latest.json` published by CI will include SHA-256 checksums for future Path B (in-app downloader).

---

## 1. Overview

**What**: A lightweight automatic update system for johnPDF that checks for new releases on GitHub and notifies the user when an update is available.

**Why**: Currently, versionCode and versionName are hardcoded in `app/build.gradle.kts`. This blocks a two-main-stream distribution model (private family APKs vs. public store):
- No way to announce new releases to family users
- Manual APK download from GitHub is friction-heavy
- Prerequisite for scaling beyond current private distribution
- Foundation for future in-app update flows (Path B)

**Scope**: **Path A only** — notification + download link. Users install via GitHub releases page or external manager (e.g., Obtainium as a tactical shortcut post-launch). In-app downloader (Path B) is out of scope; Obtainium is mentioned as an immediate workaround.

---

## 2. Goals

1. **Reduce friction**: Family users see a persistent notification when an update exists, with a direct link to download.
2. **Zero adoption friction**: No new UI patterns required; reuse existing Material3 AlertDialog.
3. **Enable CI**: Establish versioning automation (tags, metadata) so future CI workflows can publish releases without manual steps.
4. **Foundation for Path B**: Versioncheck and comparison logic can be reused if in-app downloader is later built.

---

## 3. User Stories & Acceptance Criteria

### US-1: Check for Updates at App Launch

**As a** family user  
**I want** the app to check GitHub for a new version when I open it  
**So that** I always know if an update exists without manual checking

**Acceptance Criteria**:
- [ ] On app launch, `UpdateRepository` queries GitHub API for latest release tag
- [ ] Check runs asynchronously and does not block app initialization
- [ ] Check result is cached; maximum once per day per device session
- [ ] If GitHub is unreachable, app continues without error (fail-silent)

### US-2: Display Update Available Dialog

**As a** family user  
**I want** a clear notification when a newer version is available  
**So that** I can decide whether to update

**Acceptance Criteria**:
- [ ] If local versionCode < remote versionCode, a Material3 AlertDialog is shown
- [ ] Dialog displays: app version (current), remote version, link text
- [ ] Link opens GitHub release page in browser
- [ ] User can dismiss the dialog; dismissed state persists until next check
- [ ] Dialog respects light/dark theme (via `JohnTheme`)

### US-3: Inform About Version Structure

**As a** CI system  
**I want** to publish releases with a predictable tag/version format  
**So that** version checks are deterministic

**Acceptance Criteria**:
- [ ] Git tag format: `v<major>.<minor>.<patch>` (e.g., `v1.1.0`)
- [ ] Tag is source of truth for versionName in release builds
- [ ] `latest.json` artifact published at each release with versionCode and SHA-256 hash
- [ ] Mismatches between git tag and latest.json are detected in CI

---

## 4. Architecture

### 4.1 Versioning & Release Strategy

**Current state**: versionCode=1, versionName="1.0" (hardcoded in `build.gradle.kts`)

**Future state** (Phase 2, outside this spec):
- versionCode is auto-incremented by CI on each release build
- versionName is extracted from git tag (e.g., tag `v1.2.3` → versionName="1.2.3")
- This spec provides the foundation; automation lives in CI workflow

**Approach for Phase 1** (this spec):
- Manually increment versionCode/versionName when tagging a release
- Tag format: `v<major>.<minor>.<patch>`
- Publish release to GitHub with assets

### 4.2 CI/Release Workflow (Phase 2 Dependency)

This spec does **not** include CI configuration, but describes what CI must provide:

1. **On git tag push** (e.g., `git push origin v1.1.0`):
   - Checkout code at tag
   - Increment versionCode in `build.gradle.kts` (or inject via Gradle property)
   - Build release APK (`./gradlew assembleRelease`)
   - Compute SHA-256 of each ABL split (arm64-v8a, armeabi-v7a, x86_64) and universal APK
   - Generate `latest.json` with versionCode, versionName, checksums

2. **Upload to GitHub Releases**:
   - APK files (splits + universal)
   - Release notes
   - `latest.json` as artifact

**Note**: Keystore (`~/.android-keys/johnpdf.jks` + `keystore.properties`) must be retrieved before build; setup is a blocker (see §7).

### 4.3 UpdateRepository & Data Layer

New component in `data/UpdateRepository.kt`:

```kotlin
interface UpdateCheck {
    val remoteVersion: Flow<RemoteVersion?>
    suspend fun checkForUpdate()
}

data class RemoteVersion(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String, // GitHub release page
    val sha256: Map<String, String>, // e.g. { "arm64-v8a": "abc123...", "universal": "def456..." }
)

class UpdateRepository(
    private val httpClient: () -> HttpClient, // lazy, only instantiated if check runs
    private val settingsDataStore: DataStore<Preferences>,
) : UpdateCheck {
    // 1. Fetch from GitHub API: GET https://api.github.com/repos/{owner}/{repo}/releases/latest
    // 2. Parse: extract tag, assets, compute versionCode from semantic version
    // 3. Store in DataStore (latest check time + result)
    // 4. Compare with BuildConfig.VERSION_CODE
}
```

**Why lazy HttpClient**: Avoids unnecessary network stack instantiation on every app launch if check is cached and skipped.

### 4.4 UI Layer — Dialog

New component in `ui/common/Dialogs.kt`:

```kotlin
@Composable
fun UpdateAvailableDialog(
    current: RemoteVersion,
    onDismiss: () -> Unit,
    onOpenLink: (String) -> Unit,
) {
    // Material3 AlertDialog
    // Title: "Update Available"
    // Body: "Current: 1.0\nNew version: 1.1.0"
    // CTA: "Download" (opens URL in browser)
    // Dismiss: "Later" (marks as dismissed until next daily check)
}
```

**Integration**: 
- Show dialog in `MainActivity` or `AppNavHost` after app initialization
- Read `updateRepository.remoteVersion` as a state flow
- Dismiss state stored in `settingsDataStore` (key: `last_dismissed_version`)

### 4.5 Integration in AppContainer

Add to `AppContainer.kt`:

```kotlin
class AppContainer(context: Context) {
    // ... existing repositories ...
    val updates = UpdateRepository(
        httpClient = { /* lazy-init HttpClient */ },
        settingsDataStore = app.settingsDataStore,
    )
}
```

---

## 5. API & Contracts

### 5.1 GitHub API Endpoint

**Endpoint**: `GET https://api.github.com/repos/{owner}/{repo}/releases/latest`

**Response** (excerpt):
```json
{
  "tag_name": "v1.1.0",
  "name": "1.1.0",
  "assets": [
    {
      "name": "app-arm64-v8a-release.apk",
      "browser_download_url": "https://github.com/.../releases/download/v1.1.0/..."
    },
    {
      "name": "latest.json",
      "browser_download_url": "https://github.com/.../releases/download/v1.1.0/latest.json"
    }
  ]
}
```

**Requirements**:
- No authentication needed (public repo); rate limit is 60 req/hour per IP (sufficient for one check per day per device)
- If rate-limited (HTTP 403), silently skip check and continue

### 5.2 latest.json Schema

Artifact published with each release:

```json
{
  "versionCode": 2,
  "versionName": "1.1.0",
  "releaseNotes": "Bug fixes and performance improvements",
  "checksums": {
    "app-universal-release.apk": "sha256:...",
    "app-arm64-v8a-release.apk": "sha256:...",
    "app-armeabi-v7a-release.apk": "sha256:...",
    "app-x86_64-release.apk": "sha256:..."
  }
}
```

**Use**: Checksum validation is out of scope for Path A (notification only). Included for Path B (in-app downloader) and manual verification.

---

## 6. Requirements

### 6.1 Functional Requirements

1. **Check Triggering**:
   - [F1] Check runs on app launch (MainActivity onCreate)
   - [F2] Check is skipped if already checked in the last 24 hours
   - [F3] Check is non-blocking; app initializes regardless of result

2. **Version Comparison**:
   - [F4] Compare local BuildConfig.VERSION_CODE with remote versionCode
   - [F5] Update is considered available if remote > local
   - [F6] If equal or remote < local, no dialog is shown

3. **Dialog Behavior**:
   - [F7] Dialog is shown once per unique remote version
   - [F8] Dialog persists until user dismisses or new version is available
   - [F9] "Download" link opens GitHub release page in default browser
   - [F10] "Later" dismisses dialog and stores timestamp; respects 24-hour cache

4. **Error Handling**:
   - [F11] If GitHub API is unreachable (no network, timeout), silently continue
   - [F12] If JSON parsing fails, log error and skip check
   - [F13] If latest.json is missing, use tag_name for version fallback

### 6.2 Non-Functional Requirements

1. **Performance**:
   - [NF1] Check request times out after 10 seconds
   - [NF2] Check does not consume >1MB of data per query
   - [NF3] Cached check result persists across app restarts

2. **Compatibility**:
   - [NF4] Minimum SDK 24 (target: 36)
   - [NF5] Works on split APKs and universal APK

3. **User Experience**:
   - [NF6] Dialog follows johnPDF's Material3 theme (light/dark)
   - [NF7] Notification is non-intrusive; can be dismissed
   - [NF8] No dependency on Google Play Services or Firebase

4. **Maintainability**:
   - [NF9] UpdateRepository is testable (no static dependencies)
   - [NF10] Dialog is reusable in tests via Compose preview

### 6.3 Security Requirements

1. **HTTPS Only**:
   - [SEC1] GitHub API calls use HTTPS (no fallback to HTTP)
   - [SEC2] Certificate pinning is not required (GitHub's CA chain is trusted)

2. **SHA-256 Validation** (Path B prerequisite):
   - [SEC3] latest.json includes SHA-256 checksums for all APK variants
   - [SEC4] Manual checksum validation can be documented for users who download directly

3. **Privacy**:
   - [SEC5] No telemetry or analytics from version checks
   - [SEC6] No user data sent to GitHub; only standard HTTP headers

4. **Input Validation**:
   - [SEC7] Parse latest.json strictly; reject if versionCode is non-numeric
   - [SEC8] Tag format must match `v<semver>` or `<semver>`; reject unexpected formats

---

## 7. Blockers & Dependencies

### Critical Blockers (Must Resolve Before Implementation)

1. **Keystore Recovery** (highest priority):
   - **Blocker**: `~/.android-keys/johnpdf.jks` and `keystore.properties` do not exist in the new environment
   - **Impact**: Without keystore, release APKs will not install over existing builds on family devices
   - **Resolution**: User must copy keystore files from secure backup or re-create signing key
   - **Owner**: User (outside this spec)
   - **Evidence**: Handoff doc (`docs/superpowers/handoff/2026-09-29-handoff.md`, section "Fora do git")

2. **Repository Visibility Decision**:
   - **Blocker**: Repo is currently private; CI workflow must be able to upload artifacts to GitHub Releases
   - **Impact**: If CI cannot authenticate to GitHub, workflow will fail
   - **Resolution**: Ensure GitHub token is configured in CI environment (e.g., `GITHUB_TOKEN` secret)
   - **Owner**: User + CI setup (outside this spec)

### Implementation Dependencies (in Order)

1. **Phase 1a — Setup** (before code):
   - Recover keystore files
   - Verify repo access for CI workflows

2. **Phase 1b — Data Layer**:
   - Implement `UpdateRepository` with GitHub API client
   - Write integration tests (mock GitHub API)
   - Add `com.google.android.material:material` if not present (likely already via BOM)

3. **Phase 1c — UI Layer**:
   - Implement `UpdateAvailableDialog` Composable
   - Add dialog to `MainActivity` or app-level state
   - Write Compose preview tests

4. **Phase 1d — CI Setup** (Phase 2, but informs this spec):
   - Add `.github/workflows/release.yml` workflow
   - Workflow must run on tag push, build APK, generate `latest.json`, upload to Releases

### Dependency Chain

```
Keystore Available
  ↓
UpdateRepository (data layer)
  ↓
UpdateAvailableDialog (UI)
  ↓
MainActivity Integration
  ↓
CI Release Workflow (Phase 2)
```

---

## 8. Out of Scope

The following are **not** included in this spec and are candidates for future work:

1. **Path B — In-App Downloader**:
   - Downloading APK directly within app
   - Automatic installation via `ACTION_INSTALL_PACKAGE`
   - Requires WRITE_EXTERNAL_STORAGE (Android 11+) or dedicated download directory
   - Requires INTERNET permission (will be added for Path A, but download logic is separate)
   - Will reuse `UpdateRepository` and checksum logic from this spec

2. **Obtainium Integration**:
   - Mentioned as an immediate tactical shortcut for family distribution
   - Independent of this system; users can configure johnPDF in Obtainium
   - Not a blocker for Path A

3. **Auto-Retry Logic**:
   - Exponential backoff for failed checks
   - Scheduled background checks via WorkManager
   - Out of scope for MVP; can be added if user adoption is high

4. **Manual Versioning Automation**:
   - Git hooks or local scripts to auto-increment versionCode
   - Belongs in CI workflow setup, not app code

5. **Staging/Beta Channel**:
   - Pre-release versioning (alpha, beta)
   - Out of scope for family distribution

---

## 9. Success Metrics

### Implementation Success

1. **Functional**:
   - All acceptance criteria in §3 are verified (manual + automated tests)
   - Version check returns in <2 seconds on typical 4G network
   - Dialog is displayed correctly in light and dark themes

2. **Code Quality**:
   - `UpdateRepository` has >80% code coverage (unit + integration tests)
   - Compose preview tests pass for `UpdateAvailableDialog`
   - No new lint warnings

3. **Integration**:
   - App successfully checks GitHub API and parses response
   - Dialog appears 1x per app launch if update is available
   - Dismiss state persists across app restart

### Deployment Success

1. **Release Process**:
   - Release workflow runs automatically on tag push
   - `latest.json` is correctly generated with versionCode and checksums
   - Family users receive notification on app next launch after release

2. **Adoption**:
   - No crash reports related to update checks
   - Network errors are silently handled (no user-visible failures)

---

## Appendix: Terms & Definitions

| Term | Definition |
|------|-----------|
| **Path A** | Lightweight: check version, show notification + link |
| **Path B** | Heavy: in-app download, validation, auto-install |
| **Keystore** | Android release signing key (JKS file + passwords) |
| **versionCode** | Integer; used by Android for upgrade detection (must increment) |
| **versionName** | String; displayed to users (e.g., "1.0", "1.1.0") |
| **latest.json** | Metadata artifact with remote version info and checksums |
| **Tag** | Git release marker; format `v<semver>` (e.g., `v1.1.0`) |
| **Obtainium** | Third-party app auto-updater; can fetch from GitHub without in-app integration |

---

## Sign-Off

**Spec Owner**: Auto-Update System (Path A)  
**Reviewed by**: (pending implementation planning)  
**Next Step**: Writing implementation plan (superpowers:writing-plans) before code

---

### References

- Investigation (Opus): Path A vs Path B comparison (context provided by user)
- Handoff doc: `docs/superpowers/handoff/2026-09-29-handoff.md`
- GitHub API: https://docs.github.com/en/rest/releases/releases?apiVersion=2022-11-28
- Material3 AlertDialog: https://developer.android.com/reference/androidx/compose/material3/AlertDialog
- johnPDF codebase: `app/build.gradle.kts`, `AppContainer.kt`, `ui/common/Dialogs.kt`
