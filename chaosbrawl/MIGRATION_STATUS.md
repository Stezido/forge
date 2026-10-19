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


2026-10-18: Real client game proof and protected custom desktop updates.

- Actual Linux Adventure Sealed match opened from the paid event using a legal 40-card deck from its 96-card pool. An ignored external render-thread test harness temporarily hands the human seat to Forge AI; first real GUI game ended on turn 13, Drakoe won. Continuing the standard best-of-three match/event. This is automated client/AI coverage, not a human playtest; the harness is not included in source/package resources.
- Native Windows 1280x720 Adventure main menu is visibly verified in addition to its nine model/image/game checks. Native captures target only the owned Forge window.
- LibGDX's updater ignored the classic auto-update setting and queried upstream on startup. Custom desktop launches with forge.chaosbrawl=true now skip that upstream update path, preserving the reviewed fork/resources; ordinary Forge/Android behavior stays unchanged. Seven-module build passes (21.887 s). No source MSE or published downloads changed.
- Website issue #56 updated with exact completed/remaining checks; private PR #10 remains the review location. Full GUI reward/save/reload/Draft flow is still in progress.


2026-10-18: Full Sealed client event and saved Windows Draft pool verified.

- Actual Linux LibGDX Sealed event completed: 96-card pool, legal 40-card test deck, six played GUI games across two best-of-three rounds (2-1 then 1-2), correct match return/standings, remaining AI-only final, event Completed/Awarded and two booster rewards. Saved afterward to the isolated world. Human test seat was temporarily AI-controlled through an external ignored harness; no human or remote playtest is claimed.
- Actual native Windows Draft UI displays native images/pick ranks. All 36 picks completed with seven AI decks; saved the completed draft, exited/restarted the native client, loaded the world and restored that event/pool. Rebuilt a legal 40-card test deck from the restored picks and started its first normal match.
- Custom booster rewards currently use the ordinary card-back fallback because no CBT pack image is authored. Manipulative Temptress sometimes logs an AI target-selection rejection; games continue without exceptions. These are presentation/AI-polish limitations, not missing custom card scripts.
- No source MSE edits, artifact publication or production website changes. Future release/code migration remains deferred per owner policy.


2026-10-18: Native Draft event completed; Sealed rewards survive restart.

- Native Windows Draft played a full best-of-three match (0-2, turns 10 and 13), returned to standings, advanced the existing AI-only bracket rounds, reached Completed/Awarded, collected its booster reward and saved. Draft picks had already survived an earlier native restart.
- Restarted the final protected-updater Linux client and loaded the post-Sealed world. Actual runtime reports the owned profile/ChaosBrawl plane, Drakoe, 40 collection cards and exactly two owned booster rewards; the finalized Sealed event is removed as expected. A fresh eligible Draft is offered at an inn.
- Repeated all nine native Windows model/image/game checks against the final custom-update-protected JAR; they pass. No caught match exceptions. Local Windows test launcher is C:\MTG\ChaosBrawlForge-Adventure-Local-20261018\Play Adventure (local test).cmd. This development folder depends on read-only junctions to the preserved beta.2 test installation and is not a portable release.


2026-10-18: Adventure/limited implementation verification complete.

- Final native Windows restart reports the correct isolated ChaosBrawl profile, Drakoe, 40 collection cards and exactly one Draft reward booster; the finalized event is removed as expected. Both full event/reward/save/restart flows are verified. Eight actual client games were played with temporary AI control of the test seat; ordinary AI-only bracket pairings use upstream Adventure's random resolution. No human/remote proof is claimed.
- Final local Adventure JAR: 59,036,882 bytes, SHA-256 ca9a5d120f1d5e94e18294f54bef622c11135e4cbd542bfffa1807e22c385bdf. Receipt: limited/verification.json. All 466 card checks, seven classic limited checks, nine native Windows Adventure/model/image/game checks and local preparation for 314 faces/41 decks pass. Latest reviewed executable pins and generated data match.
- Ready to merge private PR #10 into the existing Windows integration branch while preserving all earlier PRs. Issue #56 stays open for the future coordinated CBFRbeta/CBFR packaging/release batch. No new ZIP/release/upload/deployment; current production beta.2 remains unchanged. Android/mobile remains #57.


2026-10-18: Saved and merged handoff.

- Private Forge PR #10 merged at ec90594d51271e20deee54630d18d580e78f2a82 into chaosbrawl/windows-playtest-001. Earlier PRs/branches and the public fork/main are preserved. Local Windows integration worktree was fast-forwarded to that merge after verifying it was clean. PR #9 now describes the combined Windows/Adventure source scope.
- Website #56 checklist records implementation and all verified event flows; it stays open only for the coordinated future CBFRbeta/CBFR packaging/release batch. #34 has the current milestone update. #55/#54 and Android #57 remain separate follow-ups.
- Both owned GUI test processes were closed after saving. The native local test launcher/README and ignored logs/captures remain available; the full source verification receipt is committed. Published beta.2, production website access, MSE and user-owned dirty worktrees were not modified. No new ZIP, release or deployment.


2026-10-18: Owner explicitly requested the next downloadable release.

- Preparing Windows 0.1.0-beta.3 with both classic and Adventure launchers. Migrated current edition, ranking, test/preparation code and upcoming generated cache/decks to CBFRbeta; CBFR reserved for the final set. Historical MSE/source batches and old release receipts stay unchanged. CBT is retained as an edition alias for old deck references; compatibility checks are in progress.
- Seven renamed limited tests passed. Portable packager now includes the Adventure JAR, physical ChaosBrawl plane resources, a fresh default profile and both runtime image and real Adventure checks before ZIP creation. No archive uploaded or website changed yet.
- Cloudflare read-only checks confirm existing Free plans/capacity: 3 Workers, 2 D1 databases, 290816 storage bytes, 71 requests/16 rows read/0 rows written today. GitHub billing endpoints are unavailable to current authentication; use locally validated, existing Free Worker deployment instead of starting unverified-cost Actions.
- User asks to close #56 after publication and brainstorm #54 before UI implementation. #54 has been read; its UI is not being edited in this release.


2026-10-18: Beta.3 packaged and published on explicitly requested private release.

- Source9374fb5559500758d4681b816e22c459ae43028f includes classic and Adventure, CBFRbeta and CBT compatibility alias. 466 card checks, seven limited checks, nine packaged Adventure checks and all 292 native Windows images pass. Both actual packaged GUI launchers and seeded classic game pass; test GUI processes closed.
- Windows/source ZIP hashes and exact native checks recorded in releases/windows-beta3-verification.json. Immutable assets uploaded to private website release forge-desktop-v0.1.0-beta.3; provider digests match. Both dedicated-credential provider handoffs and anonymous CDN ZIP signatures verified.
- Website staging beta.3 deployed at a34124c1-03a3-4eca-b439-90496fb8c8ed; access checks pass after fresh Free-capacity verification. Production promotion is next. #54 UI is awaiting brainstorming, with no visual implementation.

2026-10-18 Android #57: Owner authorized website/GitHub APK distribution, Android 15 RedMagic 10 Pro target, and Android 8/API26 eligibility for older testers. Isolated Android branch contains resource bootstrap, app identity/storage isolation, disabled upstream custom-build updates, APK profile and reproducible reviewed-resource bundler. SDK installed; bundled resource counts and JVM installer tests pass. Full Android compile/emulator/signing and download publication remain in progress. No paid Actions or infrastructure.

Android checkpoint: full compile/package succeeds; independent APK signature/identity/API26/alignment verified (359128642 bytes). Added safe signing and exact-source archive tools. Emulator boot is in progress; no connected physical phone. No publication yet.

Android emulator checkpoint: signed APK installs on API35. Startup exposed SuperNotCalledException while delayed setup ran inside the game Activity. Resource installation moved to Launcher before Main creation. Draft GitHub assets are not published to testers and will be replaced after rebuilt startup verification. 466 engine tests pass.

API35 startup caught the upstream PublicFileProvider assertion requiring exported=true. Custom builds now omit that provider entirely and reject app-driven installer launches; browser-delivered APK updates are the intended path. Rebuilding before replacement of private draft assets. No tester publication.

Mobile packaging refinement: use upstream Android cardsfolder.zip instead of installing 34369 separate card-script files, reducing first-launch filesystem work for older devices. Launcher now uses the full-screen loading theme. Rebuilding resource payload/APK before replacing unpublished draft assets.

Android final artifact/source pins and startup matrix recorded in releases/android-verification.json. API26/API35 emulators install/unpack41 decks and enter native card loading. Source/engine4280a730 and exact resource payload retained; 466 engine/7 limited/292 image paths pass. Physical phone/full Android games remain unverified, so #57 stays open. Private prerelease published with matching source; website staging passes at version6da7679a-58de-4b4c-b84c-2190fedbf9d0. Production website deployment is in progress, not yet claimed.


## Playtester feedback investigation — 2026-10-18

Isolated branch chaosbrawl/playtest-feedback starts from Android release merge fb67a19c. Initial engine checks pass 468 tests, including new Bottle Bash damage and counter movement checks. Token packaging currently copies raw illustrations into full-card token image slots; changed preparation to native full renders. Mobile stack appends previously traversed each whole identical-token chain; cached per-rebuild tails remove that quadratic traversal. Booster collation changes and full integration validation are in progress. No new downloadable release has been created. Drunken conversion requires a reported creature/counter example; existing real untap tests pass.


Feedback validation: 469 engine tests and 7 limited tests pass. The limited suite samples 2000 real 12-card packs, checks uniqueness, 50% dual-land common replacement, 2% planeswalker rare replacement, and builds sealed/draft AI decks. Six authored planeswalkers now have rankings; generated AJDE! remains excluded. Temptress now chooses donor/recipient at resolution without targeting, matching the printed text, including an opposing donor or itself as recipient; declining is tested. Mobile and Android reactor compilation and the desktop build pass. Prepared all 314 native renders with 41 isolated test decks using the clean website checkout (the original website checkout has a different authoring source and was left untouched). Runtime image audit resolves 292 exact card paths and the authored token images without missing files. Drunken conversion remains unreproduced: default/printed tolerance, excess conversion and immediate stun consumption pass the real untap tests; a creature/counter example is still needed. No physical-device performance claim and no new download release.


Owner clarification: the reported Drunken-to-stun failure was a testing misunderstanding (owner confirmed); no engine change is needed for that item. PR13 merged the remaining validated fixes into chaosbrawl/windows-playtest-001. Current public download assets remain unchanged.


## Draft Tap normal-stack regression — 2026-10-18

Reproduced the owner report: activating Beer through ComputerUtil.playStack yields one Drunken counter rather than two. The previous test used playNoStack, which hid this failure. Normal stack insertion copies the activated ability and clears the original targets; the sacrifice trigger retained a reference to that original. Added an opt-in Chaos Brawl sacrifice-event target snapshot so the delayed trigger can retain the original chosen cards. Expanded normal-stack coverage to all seven Beer kinds, multiple Draft Tap triggers and opposing control. Validation is in progress. No new downloadable release is requested or published.


Draft Tap validation: all 471 engine tests pass. The normal-stack regression covers all seven Beer variants and one/two controlled Draft Taps (Mirror Gallery keeps duplicate legendaries legal), checks the original activation targets were actually cleared, and includes Bock Beer doubling after the extra counters. An opponent's Draft Tap does not boost your Beer. Existing tests also cover sacrificing Beer without a targeting activation. Current reviewed executable hashes refreshed; authoring source and printed card text are unchanged. The booster change still rolls only one designated common slot per pack; six other commons remain fixed and dual lands are excluded from the wildcard.


## Obsession AI targeting — 2026-10-18

The existing counter enum already calls Obsession negative, but CountersPutAi selected friendly targets for unflagged abilities based on isCurse rather than counter downside. Added opt-in Chaos Brawl targeting shared by spells/activated abilities and triggered abilities. It prefers forcing enemy creatures to attack, permits a friendly creature when actual counter-added pump or a newly reached static power/toughness threshold helps it, avoids feeding an enemy payoff, declines optional self-harm, and still satisfies mandatory target requirements. New real-card tests cover five source cards, Devotee, Blood Moon Rising thresholds and optional/mandatory decisions. Validation is in progress; printed rules and downloadable releases are unchanged.


Obsession AI validation: all 482 engine tests pass. Eleven added cases exercise the actual CountersPutAi spell/activation/trigger entry points: enemy-first targeting for Sweet Harmless Siren, Wagon Enthusiast, Baiting Siren, Peter the Manipulative and Ball Scratching; optional decline versus mandatory target choice; friendly/opposing Devotee; friendly/enemy aura threshold; Hexproof; postcombat temporary buffs; unchanged positive-counter targeting and opt-in gating. Detached-copy threshold evaluation leaves real counters/stats unchanged. Current reviewed executable hashes include the new AI helper. No downloadable release has been created.


## Owner-authorized beta.4 release — 2026-10-18

Owner explicitly requested deployment of a new beta.4 release. Preparing Windows Classic/Adventure and Android 0.1.0-beta.4 from the merged feedback fixes, with matching GPL source archives. Android retains app identity, API26 minimum and release signing key; versionCode increases from10001 to10004 for in-place updates. Corrected the release-only Adventure audit to admit the six planeswalkers now intentionally eligible in boosters. Builds, package checks and publication are in progress. No release has been uploaded yet.


Beta.4 source validation: 482 engine tests and 7 limited tests pass. Android manifest version and signing/source archive metadata now match beta.4/versionCode10004. Existing key material remains outside the repository. Windows/Adventure reactor build is running locally; emulators API26/API35 started for upgrade checks. Nothing uploaded yet.


Beta.4 packaging completed from exact clean source78f9be13e8041236bb1f4a75e827ece428fd05cd: Windows ZIP497231720 bytes and source279209921 bytes. Native Windows checks pass292 card/17 token images,9 Adventure checks with4 completed games, a decisive Classic game, and Classic/Adventure window startup. Artifacts copied/checksummed to new C:\MTG beta.4 folders without overwriting earlier releases. Android APK363920962 bytes signed with the unchanged03d5c326 certificate, versionCode10004; exact Android source622133026 bytes, both from the same source commit. API26/API35 in-place installations over beta.1 succeed; resource update/seed preservation is being checked. GitHub draft uploads started; one API request timed out and is being retried, so no publication or website pin change is claimed yet.


Both beta.4 private GitHub prereleases are published, with matching source: Windows assets654321536/654321539; Android assets654329439/654329440. API26/API35 resource markers equal the new477e4b3592 bundle digest, the synthetic user deck survived, and Expired/Bock token bytes match native card renders. API35 native card loading was observed; software emulation also displayed system UI/launcher ANR prompts. No full Android game or physical-device performance is claimed. Publication observed on2026-10-18; resumed2026-10-19 to finish website pins/deployment.


## Owner card-name/Beer feedback and future-agent guide — 2026-10-19

Owner confirms Beer stays sorcery speed. Added opt-in Beer targeting/planning: safe permanent friendly buffs with Bock/Draft Tap counter accounting, Expired Beer removal, MAIN2 useful-instant mana preservation including real colored-source payment, and MAIN1 Beer priority before another spell when the activation is useful. Seven new engine regressions exercise costs, target/timing failures, useful versus unpayable/no-target instants, tolerance and actual controller selection. All489 engine tests and7 limited tests pass; the limited suite excludes AJDE from2000 real packs and generated decks.

Website PR69 holds registered MSE source95068f7b:172 self-references on114 cards use native dynamic names, with only four imported rules values changing (Feuerball, Lena, Hanna, Süüü). Other-card references and frozen Alpha stay intact. Native MSE renders all314 final faces without warnings and a renamed disposable fixture proves dynamic substitution. New reviewed owner-feedback-002 pins website source commit90ba1b3, unchanged native identities, updated Oracle text and executable hashes. Isolated preparation validates314 records, full faces and43 exercise decks. Packaging/Adventure now select the current reviewed batch.

Added chaosbrawl/CARD_FEEDBACK.md, linked from AGENTS.md, with the companion website workflow: MSE truth, owner-chat scope, Forge behavior/AI/limited checks, reviewed provenance, paired PRs and explicit release boundaries. Existing release assets, user installs/profiles and disabled private Actions remain unchanged. No APK/ZIP publication or website deployment. Desktop rebuild passes after final AI ordering review.


## Owner-confirmed ratings and authorized beta.5 (2026-10-19)

Paired website PR #81 contains exact CSV/native-ID mappings and 292 Beta MSE rating notes. All 291 Limited-eligible printings now use owner-supplied 0–100 scores (281 ranked names); AJDE! retains its authoring score but remains generated-only. The owner explicitly authorized the 12 newly added Pending spreadsheet rows; no sheet status was modified. Only 22 token entries remain unrated.

Desktop and Adventure reactor package builds pass. Native MSE rendered all 314 full faces without warnings and the paired source was prepared successfully. Updated engine tests and beta.5 packaging are in progress. Android identity/signing/minSdk stay unchanged; versionCode increases to10005. Packaging now counts the actual reviewed seed decks, including the prior paired owner-feedback batch, rather than requiring the historical41. No beta.5 asset is published yet.
