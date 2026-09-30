# SDD ledger — plan: docs/superpowers/plans/2026-09-28-johnpdf-leitor-android.md
Spec: docs/superpowers/specs/2026-09-28-johnpdf-leitor-android-design.md
Branch: feat/v1 (from main @ a7eb740)

## Pre-flight scan
| Pair / Task | Produces → Consumes | Finding |
|---|---|---|
| T1 ↔ T13 | MainActivity, AndroidManifest, MainActivitySmokeTest (T13 replaces all three) | consistent; T13 smoke test relies on Robolectric isExternalStorageManager()=false → "Permitir" visible |
| T2 → T4,T5,T6,T7,T9,T12 | Origin(label), originFromPath/Authority, AppError, PAGE_RENDER_FAILED_MESSAGE, filterByName, friendlyDate | names/signatures match at all use sites |
| T2 → T11 | VisiblePage, dominantPage, pageLabel | match |
| T3 → T8, T11, T13 | PdfEngine(open/pageSizes/render/close), OpenResult, PageSize, MuPdfEngine() | FakePdfEngine implements same 4 members; match |
| T4 → T7, T9, T12 | RecentItem(name, origin, path, openedAt, imported), RecentsRepository(load/add/remove(path)/items) | positional construction in T7 matches field order |
| T5 → T7, T9 tests | fun interface Importer (suspend) | tests use SAM lambda for suspend fun interface — supported by Kotlin 2.1 |
| T6 → T9, T12, T13 | PdfFile, fun interface PdfLibrary, StorageAccess.settingsIntents/hasAllFilesAccess | match |
| T7 → T9, T13 | OpenPdfUseCase(importer, recents, clock), OpenOutcome | match |
| T8 → T9 | MainDispatcherRule (testutil) | match |
| T8 → T11, T13 | ReaderViewModel(file, title, engine, RotationLockSetting), consts MIN/MAX_ZOOM | T13 passes container.settings (SettingsRepository : RotationLockSetting) ok |
| T9 → T12, T13 | HomeViewModel API, HomeUiState, HomeTab, ReaderRoute/HomeRoute | match |
| T10 → T11, T12 | BigButton, ErrorDialog, ConfirmDialog, PasswordDialog, MinTouchTarget, PageGapColor, JohnPdfTheme | match |
| T1 self | wrapper 8.11.1 vs AGP 8.10.1 (needs ≥8.11.1) | ok |
| T3 self | tests vs engine; close() idempotent (fixed in plan before start) | ok |
| T4 self | tests vs repo | ok |
| T5 self | tests vs repo | ok |
| T6 self | tests vs repo | ok |
| T7 self | tests vs use case | ok |
| T8 self | render retry-only-on-OOM (plan fixed before start) | ok |
| T9 self | tests vs VM | ok |
| T10 self | tests vs dialogs | ok |
| T11 self | test state holder uses remember (plan fixed before start) | ok |
| T12 self | tests vs HomeContent | ok |
| T13 self | smoke test vs wiring | ok |
| T14 self | needs mobile-mcp approved in a new claude session | may block E2E; see ruling |

Ruling: work on branch feat/v1 in the main checkout instead of a separate worktree — single-purpose repo, nothing else in flight — cost if wrong: none, branch can be moved.
Ruling: user asked to speed up with another agent; instead of two implementers at once (skill forbids: shared tree + 7 GB RAM with emulator), overlap task N's review (read-only, no Gradle) with task N+1's implementation; fix rounds for N run after N+1's implementer returns; review packages use explicit SHAs — cost if wrong: a fix to N may need a small follow-up in N+1.
Ruling: implementers for transcription-only tasks (2,4,6,10) on haiku; tasks with build/API/integration risk (1,3,5,7,8,9,11,12,13,14) on sonnet; reviewers on sonnet — cost if wrong: extra fix rounds.

## Progress
Ruling: pre-generate Task 3 PDF fixtures now in the gitignored workspace (fixtures/) with a separate agent, no git ops, so Task 1's 'git add -A' can't sweep them in; Task 3 implementer copies them — cost if wrong: Task 3 regenerates them (~2 min).
Task 1: implemented cdd41a7 (MuPDF resolved to 1.28.5 per brief Step 1); review dispatched
Task 2: dispatched (BASE cdd41a7, haiku)
Task 1: minor (deferred): skeleton Text('johnPDF') unstyled <20sp — replaced in Task 13
Task 1: complete (commits 3402f03..cdd41a7, review clean)
Note: emulator was found dead after Task 2 (likely memory pressure); restarted headless by controller
Task 2: implemented 249cced; review dispatched
Task 3: dispatched (BASE 249cced, sonnet)
Task 2: complete (commits cdd41a7..249cced, review clean)
Ruling: ~/.bashrc returns early in non-interactive shells; implementers now source workspace env.sh (copy of the android-dev block) — cost if wrong: none
Task 3: implemented 0918049 (12/12 instrumented; MuPDF 1.28.5 API matched, no adaptation); review dispatched
Task 4: dispatched (BASE 0918049, haiku)
Task 4: implemented 2d326bd (report claims 8 tests; plan specifies 7 — reviewer to check); review dispatched
Task 5: dispatched (BASE 2d326bd, sonnet)
Ruling: emulator shut down until Task 13 (only Tasks 13 and 14 need it; memory contention made a JVM test take 486s) — cost if wrong: ~1 min cold boot later
Task 5: implemented 42d50dc; review dispatched
Task 6: dispatched (BASE 42d50dc, haiku)
Task 4: minor (deferred): report test counts wrong (7 tests, not 8) — report accuracy only
Task 4: minor (deferred): RecentsRepository write()/deleteCopy() IO errors propagate uncaught (plan-inherited) — final review to triage
Task 4: complete (commits 0918049..2d326bd, review clean)
Task 5: minor (deferred): 32-bit hash filename collision would overwrite another import (accepted tradeoff, undocumented)
Task 5: minor (deferred): null openInputStream branch untested
Task 5: complete (commits 2d326bd..42d50dc, review clean)
Task 3: review — Important (plan-mandated): close() race — calls after shutdown() are rerouted by kotlinx to Dispatchers.IO and may touch the native document off the MuPDF thread while it is destroyed.
Ruling: accept the finding over the plan text — spec + Review Focus #5 require that nothing touches a destroyed document; fix = @Volatile closed flag set first in close(), checked before withContext AND inside each dispatched block (throw CancellationException) — cost if wrong: small extra check per call
Task 3: minor (deferred): open() ignores a different File on a reused instance (outside contract)
Task 3: minor (deferred): render() divides by zero-width page bounds (Task 11 guards aspect only)
Task 3: minor (deferred): tools/make_test_pdfs.py not re-run by implementer (fixtures copied from identical pre-generated script)
Task 3: fix round 1 queued until Task 6 implementer returns (shared working tree)
Task 6: implemented 5982aa0; review dispatched
Task 3: fix round 1 dispatched (FIX_BASE 5982aa0 — fix commits will sit on top of Task 6)
Task 6: minor (deferred): PdfLibraryRepositoryTest never closes MatrixCursor → CloseGuard warnings in test output (plan-mandated); report falsely claimed pristine output
Task 6: complete (commits 42d50dc..5982aa0, review clean)
Task 3: fix round 1 implemented baf1a54 (13/13 instrumented); re-review dispatched; emulator stopped again
Task 7: dispatched (BASE baf1a54, sonnet)
Task 7: implemented bf87d12; review dispatched
Task 8: dispatched (BASE bf87d12, sonnet)
Task 3: fix round 1/5 (1 addressed, 0 open; commits 5982aa0..baf1a54)
Task 3: complete (commits 249cced..0918049 + baf1a54, review clean after 1 fix round)
Task 7: minor (deferred): openRecent relies implicitly on RecentsRepository.add dedup/re-sort; add comment
Task 7: complete (commits baf1a54..bf87d12, review clean)
Task 8: implemented 1cfee46; review dispatched
Task 9: dispatched (BASE 1cfee46, sonnet)
Task 8: minor (deferred): renderPage/tryRender near-duplicate try blocks
Task 8: minor (deferred): overlapping open() calls (init vs fast submitPassword) last-write-wins
Task 8: complete (commits bf87d12..1cfee46, review clean)
Task 9: implemented af9a5ee (reported suite 60 — reviewer to sanity-check counts); review dispatched
Ruling: Task 10 implementer on sonnet instead of haiku — first Compose/Robolectric UI tests, setup issues likely — cost if wrong: slightly higher token cost
Task 10: dispatched (BASE af9a5ee, sonnet)
Task 10: implemented e5410ea; review dispatched
Task 11: dispatched (BASE e5410ea, sonnet)
Task 9: minor (deferred): launchOpen/removeRecent don't guard RecentsRepository IO exceptions (ties to Task 4 deferred minor) — final review to triage together
Task 9: minor (deferred): no re-entrancy guard on refresh/open (last-write-wins)
Task 9: complete (commits 1cfee46..af9a5ee, review clean)
Task 10: minor (deferred): password OutlinedTextField ~56dp, below 64dp touch floor
Task 10: minor (deferred): theme leaves bodySmall/labelSmall/titleSmall below 20sp (unused today; M3 components like NavigationBar may use them implicitly — final review to check)
Task 10: minor (deferred): repeated TextButton+heightIn pattern x2
Task 10: complete (commits af9a5ee..e5410ea, review clean)
Task 11: implemented 917d01b; review dispatched
Task 12: dispatched (BASE 917d01b, sonnet)
Task 12: implemented 5395680; review dispatched
Task 13: dispatched (BASE 5395680, sonnet)
Task 11: ⚠️ resolved by controller: 360dp bottom-bar fit is checked on emulator in Task 14 (scenario 4 screenshot)
Task 11: minor (deferred): old+new bitmap alive during zoom re-render (bounded by 2048px cap + OOM fallback)
Task 11: minor (deferred): non-Ready states lack navigationBarsPadding (cosmetic)
Task 11: minor (deferred): LazyColumn itemsIndexed without key
Task 11: complete (commits e5410ea..917d01b, review clean)
Task 12: minor (deferred, plan-mandated): hardcoded 64.dp/80.dp instead of MinTouchTarget in HomeScreen
Task 12: complete (commits 917d01b..5395680, review clean)
Ruling: mobile-mcp tools are not loaded in this session (server is 'pending approval' until claude restarts in the project), so Task 14 E2E runs through adb directly (uiautomator dump to list elements, input tap/swipe/text, screencap, settings put system user_rotation for rotation) — same scenarios and evidence; cost if wrong: user may want a re-run via mobile-mcp after approving it
Task 13: implemented a1dc623 (DONE_WITH_CONCERNS: Robolectric shim in MainActivitySmokeTest for storage access); review dispatched
Task 14: dispatched (BASE a1dc623, sonnet, adb-driven E2E)
Task 13: minor (deferred): OP_MANAGE_EXTERNAL_STORAGE=92 magic number in test shim needs comment
Task 13: minor (deferred, plan-mandated): requestLegacyExternalStorage inert at targetSdk 36
Task 13: complete (commits 5395680..a1dc623, review clean)
Task 14: implementer hit API session limit mid-run (at scenario 4); resuming same agent after re-login
Task 14: implemented fc69e5d (14/14 PASS reported; one emulator QEMU crash during rapid swipes, reported as non-app); review dispatched
Task 14: review — Important: scenario 14 'QEMU crash, not app' claim has no preserved log evidence
Task 14: minor (deferred): 19 unreferenced screenshots committed; self-review claim inaccurate
Task 14: minor (deferred): scenario 6 page 15→16 discontinuity unexplained
Task 14: minor (deferred): 14b screenshot blank page area unexplained
Task 14: push not done — by controller instruction (push after final review)
Task 14: fix round 1 dispatched (FIX_BASE fc69e5d)
Task 14: fix round 1/5 (1 addressed, 0 open; commits fc69e5d..539c08d)
Task 14: complete (commits a1dc623..539c08d, review clean after 1 fix round)
Final review: dispatched (a7eb740..539c08d, opus)
Final review: With fixes — Important: F1 OOM retry uses uncapped width; F2 double-tap back pops Home (blank screen); F3 uncaught exceptions in launchOpen/removeRecent (incl. disk-full); F4 legacy permission dead end after 'don't ask again'. Minor fixed now: F5 small typography <20sp, F6 status-bar icons in dark mode, F7 e2e doc explanation.
Ruling: fix wave covers F1–F7; defer zoom anchoring to tap point, orphaned imports cleanup, revoked-access-reported-as-gone, Overview relaunch re-import (v1.1) — they are UX polish/rare paths with no crash — cost if wrong: an elderly user may see a wrong 'arquivo não disponível' after revoking access
Ruling: Task 13 minor 'requestLegacyExternalStorage inert' was wrong — flag still matters on Android 10 devices; keep it, finding closed
Final fix wave: dispatched (FIX_BASE 539c08d, sonnet)
Final fix wave: implemented 539c08d..a88fc56 (7 commits; 93/93 JVM, 13/13 instrumented, no INTERNET); scoped re-review dispatched
Physical-device test: dispatched on user's moto g41 (Android 12) after user offered it; strict rules (own test folder only, restore font_scale/rotation, app left installed)
Final fix wave: re-review — F1–F7 all ADDRESSED, no new Critical/Important
Final fix wave: minor (deferred): import order in HomeScreen.kt
Final fix wave: parked — double system-back (keyevent 4 4) bypasses dropUnlessResumed — Ruling: Navigation-Compose's NavHost back callback is only enabled while the back stack has >1 entry, so the second press goes to the Activity (app closes), not to a blank screen; verifying on the physical phone as scenario 8b — cost if wrong: blank screen after a fast double system-back
Final fix wave: parked — launchOpen catches Exception not OutOfMemoryError during import — Ruling: import is a stream copy with an 8 KB buffer, OOM there is implausible — cost if wrong: crash on a pathological import
Task D1 (design refresh) created from user request; analysis subagent dispatched (read-only)
Task D1 analysis done: .superpowers/sdd/2026-09-28-johnpdf-leitor-android/design-d1-analysis.md (recommend vendored Material Symbols Rounded in ui/icons/JohnIcons.kt). User forked a separate /subtask for immersive reader (tap PDF hides header/bottom bar) — overlaps ReaderScreen; coordinate before implementing either
