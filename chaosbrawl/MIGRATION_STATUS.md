Windows release preparation saved at owner-requested pause. Full 314-record migration is complete and pushed on PR #8. New packaging branch has a detailed WINDOWS_RELEASE_HANDOFF.md and checksum-verified Windows JRE pin; no distributable, native Windows test or deployment exists yet. Website issue #43 closed; #34 updated.

# Chaos Brawl Beta — all 314 source records implemented

Private review branch: `chaosbrawl/beta-owner-decisions-001`, [PR #8](https://github.com/Stezido/Chaosbrawl-Forge/pull/8), based on the preserved seven-batch PR stack. Canonical source: website [PR #50](https://github.com/Stezido/Chaosbrawl-Website/pull/50). Exact source commit/hash and executable hashes are pinned in `batches/owner-decisions-001/{source,scripts}.json`.

**292 non-token source entries implemented + 22 authored token records mapped = 314 records. No unresolved card is excluded.** Source entries include repeated printings. The three newly authored cards are cb-312 Maternal Instinct, cb-313 Higher Protection and cb-314 Haze-Fueled Frenzy. Full native UID/definition mapping: [CARD_MIGRATION.csv](CARD_MIGRATION.csv).

**465 checks pass:** 292 source comparisons, 22 token type/stat comparisons and 151 behavior/payment/timing checks. Desktop build passed. Preparation verified all identities/metadata, native full renders and **41 decks across eight batches**. Final 314-record AI smoke completed on turn 10, Ai(1) won, 1832 ms; final native rendering/preparation passed (314 faces, no warnings). No manual human or remote multiplayer playtest is claimed.

The final tests cover the new instants, arbitrary optional own-creature counter movement on Maternal Instinct, temporary protection/trample, Cini Minis' optional Milk sacrifice on resolution, stopping the Shotcrusher feedback loop, and preserving named cast history in a game snapshot. Owner decisions additionally cover simultaneous individual Rollercoaster counters, actual Wedding Ring spell/activated-ability copies without copy loops, reattachment timestamps and the official printing; all six Friend/Foe dice outcomes; Just Do it's actual sacrifices and separate triggers; Slot Machine bands/costs/reflexive damage; Culinary life payment; Hofi and Moshpit's complete turn-long death windows; native repeated Saga chapters. Gastronomic chapter progression uses deterministic player target choices independently of AI evaluation.

All collected owner questions are resolved. Eight initially deferred cards plus Moshpit and Gastronomic are loaded. Approved MSE changes include Confusing Signals, Cini Minis, Shady Herb Dealer, optional Shotcrusher, spelling/type/name fixes and Sofa Pizza. MSE is authoritative; native UIDs, frozen Alpha, original public fork/main and all review branches/PRs are preserved. The owner-edited Feedback source was read to import the three new cards and was not overwritten. Chat-only instructions do not change Tavern statuses.

Manual next step: follow [README.md](README.md) and select a Batch 01–08 deck in Constructed. Network play uses Forge's existing host/join lobby; all players should use this same build/resources and Chaos Brawl launcher. A remote session remains untested. Earlier batch-06 native Reefgarden AI activator warnings are documented in its historical report; AI strategy and exhaustive multiplayer/replacement combinations remain outside the current automated coverage.

Historical checkpoints are in STATUS_HISTORY.md; their partial counts and former questions are superseded.


2026-10-17: Owner-reported deck-builder image issue reproduced.

- Runtime image-path audit against the original beta.1 package finds 292 CBT printings and exactly four missing paths: Wedding Ring, Dungeon Master, Recycle and The Fallen. Their independent ChaosBrawlBeta functional variant suffix was missing from packaged cache filenames. Rules and native renders themselves are present.
- Shared filename preparation now reads the edition's functional-variant metadata for both local preparation and Windows packaging. Added an executable Java audit using the built Forge JAR's PaperCard/ImageKeys lookup, exact file existence and PNG decoding. Corrected beta.2 package/audit are in progress; no new release upload yet.
- A four-image patch ZIP is at C:\MTG\ChaosBrawl-Forge-0.1.0-beta.1-Image-Fix.zip; corrected aliases were also added to the known C:\MTG test installation. Restart Forge to clear its missing-image cache. No user decks/settings or card rules changed.
- Website staging 503 fix was deployed and owner confirmed downloads work. Production is held until the corrected image package is pinned and verified.


2026-10-17: Corrected Windows beta.2 package verified locally.

- Complete runtime image audit passes all 292 CBT non-token printings on both Linux and native Windows 11 using the bundled JRE; exact cache paths exist, PNGs decode and ImageKeys resolves them. Original package failed exactly four.
- Versioned beta.2 Windows ZIP: 436,883,784 bytes, SHA-256 009852777086f979a0a7135fc533b509126aac50f5f31f4eb0aea79f3600f4f8. Matching source remains byte-identical to beta.1 because the engine/JAR/MSE are unchanged. 314 native source faces, 41 decks and case-insensitive path uniqueness verified. Native GUI/game proof remains tied to the unchanged executable/runtime.
- Copied/rehashed complete beta.2 ZIP to C:\MTG and extracted only into a fresh versioned test folder for native image verification. Earlier beta.1 folder received only the four missing image aliases; no user data/settings changed. Small patch ZIP is also ready.
- Private GitHub beta.2 upload is in progress with bounded retries after intermittent GitHub connection timeouts. Do not claim the new website version is live until its exact new pins and deployment have been verified.


2026-10-17: Beta.2 private release uploaded and verified.

- Website private prerelease forge-desktop-v0.1.0-beta.2 is published with uploaded Windows/source assets. Provider-reported names/sizes/digests match the local manifest. Beta.1 remains preserved. Website pins now reference Windows asset 651614919 and source 651614917.
- Website branch passes 84 checks, TypeScript and production build. Compiled workerd verifies both new handoffs with the dedicated read-only token and anonymous CDN bytes. Staging-only run 39070401046 is deploying the new pins; production continuation is explicitly requested. Source PR #50 is merged with skip-ci, download PR #52 targets main.
- Packager defaults to beta.2 and requires the engine image audit before ZIP creation. Forge PR #9 contains the corrected preparation/packager and native proof; engine rule changes remain preserved on PR #8. Adventure/mobile still follow Windows publication.


2026-10-18: Resume Adventure/limited issue #56.

- Resumed after the owner-requested overnight pause. Windows beta.2 is already published; no new downloadable release is authorized. Merged the packaging/release-policy branch into the existing Adventure branch, preserving its earlier compile audit and the published artifacts.
- Read current issue #56 and inherited AGENTS.md. Website boosters use 12 cards (7 common, 3 uncommon, 1 rare/mythic at 1-in-8 replacement, 1 wildcard), excluding tokens, generated-only cards and all planeswalkers. Website sealed opens eight packs. This is the starting collation for Forge limited mode.
- Inspected actual BoosterGenerator, draft ranking loader, Adventure block/edition filters and plane-scoped overrides. Missing pieces include explicit eligible print sheets, booster/block metadata, ranking data, isolated Adventure configuration and real pool/draft/event checks. Native named slots currently allow a wildcard to repeat an earlier card, so exact website collation needs an opt-in slot exclusion or equivalent tested handling.
- No MSE/rules text, set code, production settings or downloadable assets changed. Next: implement/test booster/rank data and AI limited decks, then the isolated Adventure client/events.


2026-10-18: Classic limited generation passes six real-engine checks.

- Added a generated eligible print sheet (285 source printings / 275 names), website-equivalent 12-card booster slots and a 3-pack draft / 8-pack sealed block. Mythic replacement is explicitly 1 in 8. An opt-in ExcludeAlreadySelected slot flag prevents the wildcard repeating an earlier source printing without mutating shared sheets or changing ordinary editions.
- Added reproducible provisional ranking data with documented reviewed exceptions, separate from MSE/card rules. Six engine tests pass: exact eligible/variant printings, 1,000 packs, all eligible ratings, eight seeded 96-card sealed pools/legal AI decks, full 8-seat draft (36 picks each) with seven legal AI decks, and ordinary M11 booster/default-slot compatibility. Generator --check passes.
- Initial tests exposed unpadded collector lookups dropping one printing, an AI-draft test reading Main instead of Sideboard, and custom-card conformance requiring the explicit isolated-profile preference. Corrected collector IDs/test flow and enabled custom conformance in the future Windows profile builder; no published ZIP changed.
- Adventure GUI/events are still pending; current changes are local source work only. Next: isolated Adventure configuration/event verification, full existing rules regression and client build.


2026-10-18: Actual Adventure model/event checks pass; local GUI started.

- Added an isolated ChaosBrawl plane/profile preparation command that reuses Shandalar resources without modifying upstream planes/saves. Its allowed events are CBT Draft/Sealed. An optional plane-specific eventFormats list preserves all legacy event rolls when absent and prevents unsupported Jumpstart events on the custom plane.
- Seven-module Adventure desktop build passes (18.480 s). Checked-in VerifyAdventureEvents runs the real built Adventure model from the isolated workspace: six checks pass for configuration/custom match rules, block eligibility, legacy/opt-in format rolls, actual 96-card Sealed entry/seven legal AI decks, saved-event round trip, and actual three-pack/eight-seat Adventure Draft/seven legal AI decks. No OpenGL mock is used for event/card generation; only GUI callbacks are replaced in that headless check.
- Existing ChaosBrawlTest regression: all 465 checks pass. The separate actual LibGDX application has started locally under WSLg; window/event visual verification and full games are still pending. No new downloadable archive/release or production deployment.


2026-10-18: Adventure world GUI proof and remaining verification.

- The actual LibGDX client opened under WSLg, created a new character/world in the isolated ChaosBrawl profile, entered the tutorial and a town. Captures are local ignored test artifacts. No normal player save was read or changed.
- That GUI process later exited with signal 15 (exit 143), without a Java exception in its log; the cause is not established. This is not recorded as a successful full match or event-screen test. Restarting with targeted event/UI verification next.
- All six limited checks, six real Adventure model checks and 465 card regression checks remain green. No downloadable release or website deployment.


2026-10-18: Provisional synergy checks and review branch saved.

- Private Forge draft PR #10 stacks on Windows playtest PR #9. Seven limited engine checks now pass, including actual Food-token-generator and Milk/Cini Minis ranking bonuses. Eight targeted DeckHints are generated from an auditable AI-only mapping; MSE/rules text are unchanged.
- Local classic preparation now preserves unrelated preferences while enabling custom variant conformance inside its owned profile. Existing published packages remain unchanged. Complete real-engine Adventure-format AI games from generated Sealed/Draft decks are being checked separately from GUI navigation.


2026-10-18: Upkeep AI regression fixed and strict complete games pass.

- First complete limited games exposed a caught null-payer exception in PermanentAi's pre-cast upkeep-cost evaluation. It inspected an untriggered stored ability without an activating player. Evaluate a copy for the current AI instead, preserving the original trigger and avoiding cached payment choices. A focused real-AI regression passes; full card suite now 466 checks.
- Rebuilt the seven-module Adventure desktop client (18.168 s). Strict match verification now fails even on caught AI exceptions: two Sealed and two Draft games completed decisively without exceptions, on turns 45/24/9/18. Together with the six configuration/pool/draft/save checks, eight Adventure checks pass. These are explicitly headless AI-vs-AI games, not human playtests.
- Real LibGDX tavern GUI shows the custom Sealed event. New world progress saves successfully into the isolated ChaosBrawl auto_save.sav. A local GUI run was invalidated by rebuilding its live JAR; restarted from a stable copied JAR. No runtime patch/backdoor was added to production source.
- Refreshed only the latest reviewed script-hash manifest after validation, preserving source MSE hash/IDs and historical batches. Full local classic preparation succeeds for all 314 faces and 41 decks with custom conformance enabled. No published artifacts changed.


2026-10-18: Native Windows Adventure checks pass.

- Separate local Windows folder C:\MTG\ChaosBrawlForge-Adventure-Local-20261018 uses the verified LibGDX JAR, dedicated profile/cache/save data and read-only shared base resource/runtime directory junctions. It is a development test folder, not a new portable release or archive. Published beta.2 files remain unchanged.
- Bundled Windows JRE passes all nine checks: six actual model/event checks, exact PNG decode/runtime lookup for all 292 CBT printings and four decisive headless Adventure-format games. The JRE does not include the source compiler; verifier is compiled with the development JDK first. Upstream FRA resource warnings during model loading are separate from CBT; custom matches emit no caught exceptions.
- Actual native Windows GUI opens at 1280x720 with the expected Forge title. Linux real Sealed flow reaches the 96-card pool editor with native images after paying its standard entry fee. Both event completion/rewards and Windows Adventure-menu navigation continue next.
