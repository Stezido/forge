# Windows desktop playtest release — paused at owner request

## Completed and pushed

The full Beta migration is complete on `chaosbrawl/beta-owner-decisions-001`, private Forge PR #8, ready for review. Engine/source baseline commit: `fe9a753e` (full hash via git). All 314 source records are accounted for: 292 non-token entries and 22 authored token mappings. Includes Maternal Instinct, Higher Protection and Haze-Fueled Frenzy. No collected rules question remains unanswered.

465 checks pass: 292 metadata, 22 token type/stat, 151 behavior/payment/timing. Desktop Java build passes. Native MSE rendered all 314 faces without warnings; 41 test decks prepared. Exact-final seeded AI game: turn 10, Ai(1) won, 1832 ms, no exception/undefined SVar. This is local validation, not a Windows-native or remote multiplayer playtest.

Canonical Beta SHA-256: `73cdf99abf3bdb9e619cbb6af5ab3cb7d9305cfe86defe61692b85ff93392703`. Source commit `43087af` on website PR #50; later website `e9cd333` adds only final status/change summary. Beta MSE and receipt summary are under `sets/the-foul-realms/` in `/home/stefan/github/Chaosbrawl-Beta-Forge-Wording`. The summary lists 30 changed/new cards. Alpha remains frozen/published. Owner-edited `/home/stefan/github/Chaosbrawl-Beta-Feedback` MSE (three new cards) was read/imported and left untouched; unrelated dirty lore checkout is preserved.

## New authorized objective

The owner requested a first desktop deployment downloadable through the website Playtest area, explicitly confirming **Windows first**. Android follows later. Website issue #43 is closed and #34 updated with verified counts and this priority. No deployment or Windows release has been created yet.

Worktrees/branches:
- Forge packaging: `/home/stefan/github/Chaosbrawl-Forge-Windows-Playtest`, `chaosbrawl/windows-playtest-001`, based on fe9a753e.
- Website download: `/home/stefan/github/Chaosbrawl-Forge-Desktop-Download`, `codex/forge-desktop-download`, based on e9cd333/PR #50.
- Verified executable/runtime evidence: `/home/stefan/github/Chaosbrawl-Forge-Private-Owner-Decisions/.chaosbrawl-local/`. This contains the built desktop jar, latest prepared decks/profile/cache and `verified-314-{tests,build,prepare,smoke}.log`.

## Downloaded Windows runtime

Eclipse Temurin JRE 21.0.12.1+1, x64 Windows, 48,999,141 bytes, SHA-256 verified against the official published checksum. Pin: `chaosbrawl/releases/windows-runtime.json`. Local ZIP/checksum: `.chaosbrawl-local/windows-release/temurin21-windows-x64-jre.{zip,sha256.txt}` in this worktree. Not extracted or executed yet. These binaries are ignored and not committed/pushed.

The Adoptium API returned 403; `gh release download` eventually timed out. Direct `curl -L` to the official GitHub release URL succeeded. Do not repeat failed access paths. Windows also has `C:\Program Files\Java\jdk-25`, observed read-only; prefer the pinned bundled JRE for the release.

## Packaging plan (not implemented)

1. Add a reproducible allowlist-based Windows ZIP builder, e.g. `chaosbrawl/package_windows.py`, version `0.1.0-beta.1`. Use the verified parent build (`--build-root` points to Owner-Decisions), validate its clean tracked source and executable manifest; record engine commit, jar SHA, MSE SHA, runtime pin, package/source hashes and test evidence. Matching source can be `git archive` of the exact engine commit, with preserved GPL/notices and build instructions.
2. Include the 31 MB desktop jar, full upstream `forge-gui/res` (476 MB uncompressed), fresh native card/token pictures (156 MB), all 41 exercise decks, bundled Windows JRE and notices. Avoid all existing user profiles, secrets, sessions and arbitrary `.chaosbrawl-local` contents. Do not copy the whole old picture cache blindly: a previous Ari spelling can leave a case-only filename collision on Windows. Derive exactly the current 314 image names from reviewed IDs/native renders.
3. One-click `Play Chaos Brawl.cmd`: work from its own directory, disable Sentry, invoke bundled `runtime\bin\javaw.exe` with `-Dforge.chaosbrawl=true`, UTF-8, Xmx4g and the custom jar. A portable `forge.profile.properties` can use relative `userDir=UserData`, `cacheDir=Cache`, `decksDir=Decks`, `decksConstructedDir=Decks/constructed`; Forge canonicalizes them. This isolates normal Forge data.
4. Fresh `UserData/preferences/forge.preferences` should include `AUTO_UPDATE=none` and `CHECK_SNAPSHOT_AT_STARTUP=false`, so an upstream update does not replace the custom fork. These exact existing keys were inspected. Do not alter ordinary Forge defaults globally.
5. Verify ZIP paths/checksums/content, then extract to a new owned Windows test folder (e.g. C:\MTG\ChaosBrawlForge-Playtest-0.1.0-beta.1) without overwriting an existing folder. Run bundled `java.exe` natively through WSL interop with the package-relative profile and seeded AI simulation; then verify GUI startup. Linux/WSL success is not Windows execution evidence. Preserve diagnostics and test only the newly created process/window.
6. Upload versioned binary and exact matching source ZIP as a **private** prerelease asset in the existing private website repo, for its existing `GITHUB_TOKEN` to fetch during deployment. Both website and Forge repos are private. Do not make either repo/artifact public or enable Forge Actions.

## Website delivery plan (not implemented)

See `docs/forge-desktop-handoff.md` in the website download branch. Proposed zero-added-cost path: private GitHub release assets as build inputs, split into <=20 MiB static asset parts for existing Cloudflare Workers Free, authenticated API per part, browser reassembles/saves one ZIP. No R2/new paid service. Critical: existing hosting wrapper serves ordinary static assets before app auth; private parts must be explicitly blocked at that boundary and available only through the authenticated route/ASSETS binding. Sites exports must never include them.

## Pause and resumption

The owner explicitly requested pausing and saving/pushing. No background work should continue after this handoff. Resume with package builder/native Windows test, then website download and staging verification. Complete a reviewable result before any dependent deployment/merge; verify current zero-cost capacity and preserve production access first. No subagents unless explicitly authorized. Never print or commit credentials.
