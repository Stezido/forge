# Beta migration

Canonical source: the registered development MSE in `Stezido/Chaosbrawl-Website`. The website catalogue remains published Alpha; this migration explicitly reads Beta.

| Batch | Range | Status |
| --- | --- | --- |
| 01 | cb-001–cb-050 | 50 definitions verified; native images/decks prepared; AI smoke passed |
| 02 | cb-051–cb-100 | 50 definitions verified; 157 cumulative checks; build/images/decks/AI smoke passed |
| 03 | cb-101–cb-150 | 50 definitions verified; 217 cumulative checks; build/images/decks/AI smoke passed |
| 04 | cb-151–cb-200 | 46 implemented, four deferred; 277 checks/build/native images/decks/AI smoke passed |
| 05 | cb-201–cb-250 | 48 implemented, two deferred; 337 checks/build/native images/decks/AI smoke passed |
| 06 | cb-251–cb-300 | 31 non-token entries and 15 token mappings verified; four deferred; 397 checks/build/AI passed |
| 07 | cb-301–cb-311 | WIP: four definitions and seven token bindings; final checks running |

Supporting tokens are implemented with their first dependent card, without consuming the batch's normal-card slots. Final batch accounting will distinguish token printings from playable cards; native UIDs/stable IDs are never reassigned.

The first ten-card baseline passed its 12 original integration tests before migration. The expanded suite verifies all 50 cards against the reviewed MSE metadata, plus real game effects, payments and timing. The final expanded run passed all 86 checks, including repeated casts of the same graveyard card and the zero/one/two-cast controls for Overdose. Full native MSE rendering completed successfully for all 311 source records without warnings. The desktop dependency JAR built successfully. The seeded AI-versus-AI game completed successfully with Ai(2) winning; Forge reported 2425 ms. Six isolated test decks and full card-face images are prepared.

Owner decisions for batch 01:

- Celestial Waters moves counters before untapping and before Drunken-to-stun conversion. It is a special turn action, not a normal upkeep stack trigger. The isolated engine test exercises that exact order.
- Buy a Round costs {1}{W}; Nutshot overload puts two stun counters.
- Brie-lliant Trapper is a legendary Horror Food creature without Artifact. Scavenger does not gain Food.
- Chaotic Kitchen makes one Food token and grants {2}, {T}, sacrifice for 3 life.

Food Coma and Inspiring Bard share names and mechanics with upstream cards. Forge uses their existing definitions; the metadata comparison accounts for the standard Food reminder and the source's extra words in Inspiring Bard's trigger. Their Beta full-card images still come from the canonical MSE. The copied Explosive Singularity ability on Feuerball, Junge! uses CARDNAME as its damage source, preserving the intended self-reference without rewriting the MSE Oracle field.

The private repository begins with a complete source snapshot at `aa1ec33c`, corresponding to public fork source `7f16ea60` and upstream `34a8662b`. This avoids importing the entire upstream history from the earlier shallow checkout. Original license/notices and the public fork/PR remain intact. New migration work is private; GitHub Actions is disabled in this private repository and validation runs locally.

Owner requested stopping after saving on 2026-10-16. Batch 02 source/definitions and rules changes are saved for review; see `batches/051-100/README.md` for the known failed check, unverified edits, approvals and exact next steps. Total verified remains 50. Batches 03–07 have not started. No background migration is running.

2026-10-16 resumed: all 50 batch-02 card definitions and seven token definitions are now present. All 136 checks passed with the complete 100-card metadata provider. The new cross-player/counter/upkeep behavior tests are running; this is not yet a verified playable batch. Updated source snapshot references website PR #50 commit 0f7b606, including approved Tactical Purge and Pumpkin Catapult wording. Confusing Signals uses a provisional explicit casting-time player choice; wording approval is collected for later. Next: resolve behavior failures, validate costs/Plot/Crew, prepare/build/smoke, then start batch 03.

Batch 02 validation update: first behavior run executed 150 checks, with 148 passing and two test-fixture failures caused by missing activating players on synthetic destruction/sacrifice effects. The fixtures are corrected. Expanded Plot/Crew/Ward/Aura tests are running after correcting two Java test compilation issues. Preparation now supports cumulative batches and duplicate-name art indices; executable hashes/decks/build/smoke remain pending until behavior checks pass.

Batch 02 checkpoint: 157 checks now exercise the 100 source records plus 57 rule/effect checks. The latest completed run had 156 passing checks and one remaining Catapult test; paid Plot/later free cast, two independent Plot allowances, ETB-only Aura protection, finality/ability loss and unpaid reanimation upkeep passed. The Catapult test's target-selection spy did not observe native AI selection; the current rerun checks actual life loss from its fixed 2-damage effects. Questions are collected in QUESTIONS_FOR_OWNER.md. No completed build/smoke claim yet.

Batch 02 completed: all 157 checks passed, desktop build passed, 100 native full card faces and six latest-batch test decks prepared. Seeded AI game completed on turn 10 in 1790 ms with Ai(2) winning, no custom exception or undefined-SVar error. The cumulative package contains 100 records (99 names; duplicated Spar’s Bazaar printing retained). Reviewed manifests pin 130 executable resources to the five-card-wording MSE hash 81e36272d3d2fc4308dc4d65c54be779944733010ed44ba167acd34c82da15ef. Confusing Signals wording is a collected question; current implementation/timing is explicitly documented. Next: cb-101–cb-150 on its own stacked branch, regular pushes, questions collected for later.

Batch 03 started from verified batch-02 commit 3c9f9af1. All 50 definition drafts and three supporting Gnome/Elemental tokens are saved. Generated-only AJDE! has a zero deck limit and is excluded from prepared starting decks. Native support is being extended for restricted counter removal, the original declared attacking pair, colored upkeep reduction, Siren upkeep waiver and upkeep-only mana. The 150-card metadata/baseline suite is running; no completed validation/build/smoke claim yet. Questions continue to be collected for later.

Batch 03 checkpoint: all 207 source/baseline checks passed. Expanded behavior run now has 216 checks; latest completed run had 215 passing and one Colossus test depending on the AI voluntarily paying a negative-counter cost. The test now explicitly selects payment through the native cost transaction. Generated real blue/black AJDE! cards, selective counter removal, Natalie/Dealer/Süüü interactions, upkeep-only mana payment, original attack-pair tracking and persistent Stethoscope permission/color conversion passed. Build/images/decks/smoke remain pending. Native counter-cost syntax and multiset filtering were corrected from observed failures.

Batch 03 completed: all 217 checks passed (150 source comparisons now include types and colors; 67 behavior checks), desktop build passed, 150 native full faces and six batch decks prepared. AJDE! is excluded from all starting decks and created as three real blue/black cards during play. Seeded AI game completed on turn 13 in 2384 ms with Ai(1) winning, no custom exception/undefined-SVar error. Added entry regression coverage fixed Straggler’s previously untested ETB-tapped shorthand as well as Untamable Grove Colossus. Source MSE is unchanged; pending wording/interpretation questions remain collected. Next: 151–200 on a separate stacked branch.

Batch 04 checkpoint: 46 clear definitions drafted (196 cumulative), four rule questions deferred. Initial run: 262/263 checks pass; discovered an upstream Dungeon Master name collision, now isolated with a Beta functional variant, as for Recycle and The Fallen. Build, new behavior validation and smoke remain pending. No source wording edits.

Batch 04 completed for all 46 unambiguous cards: 196 cumulative records (195 names), 277 passing checks, successful desktop build, 196 full native images, six decks and completed seed-20261016 AI game (turn 11, Ai(1), 2061 ms). Four cards remain explicitly deferred in QUESTIONS_FOR_OWNER.md. Continue 201–250 without asking now.

Batch 05 checkpoint: 40 new definitions and eight repeated shock-land printings drafted (244 cumulative implemented candidates). Tavern Slot Machine and Culinary Disaster are excluded pending rule decisions. Metadata and behavior validation in progress.

Batch 05 complete for 48 clear entries: 244 cumulative non-token records (236 names), 337 passing checks, desktop build, 244 native PNGs, six decks and completed AI game (turn 11, Ai(2), 2108 ms). Six deferred cards remain excluded. Continue 251–300.

Batch 06 checkpoint: 275 cumulative non-token entries and 15 authored token bindings. Four additional cards are excluded pending rule clarification; metadata and new mechanics checks are in progress. New tolerance modifiers and one-native-d20-per-Beer replacement are implemented but not yet verified.

Batch 06 behavior checkpoint: all 397 checks pass (275 non-token source comparisons, 15 token type/stat checks, 107 behavior checks). Fixed native additional-land field and separate copiable haste token for Sasa. Printed tolerances now use real static abilities, so blanking removes them while external tolerance modifiers remain; regression covered. Build and AI smoke still pending.

Batch 06 completed: 275 non-token records plus 15 token mappings = 290 records, 397 passing checks, desktop build, 290 native PNGs, five decks, completed AI game (turn 21, Ai(1), 10082 ms). Native Reefgarden activator warnings are recorded. Ten cards remain deferred. Proceed to 301–311.

Final batch checkpoint: all 311 source records inspected. 279 non-token records are implemented and all 22 authored token records are mapped. Ten cards are explicitly deferred. Final suite/build/smoke and all-batch deck preparation are running.


# Superseded owner-implementation checkpoints (2026-10-17)

Checkpoint: all previous 311 records passed 455 checks. Three newly owner-authored cards (Maternal Instinct, Higher Protection, Haze-Fueled Frenzy) and all approved source corrections now load; 458 checks passed including their source comparisons. New behavior tests for these cards, optional Shotcrusher, Cini Minis payment and named-cast snapshots are drafted; final rerun/build/images/decks/smoke pending. Native render/preparation for the preceding 311-card source succeeded (311 images, 39 decks); it is historical and must be refreshed for the final 314-card source.

Checkpoint: all 311 records now have draft definitions/bindings, including confirmed native Gastronomic I/II and III, and approved all-creature Moshpit deaths until end of turn. Previous 449-check run had two Wedding Ring copy failures; tests exposed the new card-only BondPartner selector also being treated as a player. Added it to the existing cards-only selector exclusion; rerun and two final-card behavior checks pending. Saga native I/III and II/IV mapping is now preserved.

Checkpoint: 442 checks exercised; 441 pass. Final failure is the new Hofi end-of-turn test fixture (active-trigger refresh after manually expiring effects); fix drafted, rerun pending. Native render review uncovered Saga chapter-to-textbox mapping: Rollercoaster has I/III and II/IV, and Gastronomic already has a valid III. Earlier missing-chapter question was an importer error; owner informed and native-fidelity confirmation requested.

# Owner decisions — active implementation

The complete clear-card baseline is preserved in private PR #7 at 843002aa (416 passing checks, build/native images/37 decks/AI game). This new branch uses owner-approved Beta source eb0e173645acc362e54f903368b5d5c2fca2686192b79778c3da6af76ab1e097 from website commit 35fff53.

Eight clarified definitions are drafted: Wedding Ring, Just Do it, Annoying Running Gag, Friend or Foe, Tavern Slot Machine, Culinary Disaster, Rollercoaster of Emotions (each counter independently), Hofi. All 424 current checks pass, including the eight new source comparisons; new behavior/payment/timing checks remain pending. Source metadata overlay preserves historical pins and native UIDs. The Wedding Ring Beta definition is isolated from its official namesake.

Moshpit immediate-only behavior is confirmed; exact priority-window wording awaits approval. Gastronomic Downfall still awaits chapter III. No placeholder card is loaded for either. Build/preparation/smoke for this branch are pending.

# Beta migration — verified baseline through cb-311

Canonical source: website PR #50, registered development Beta MSE, SHA-256 81e36272d3d2fc4308dc4d65c54be779944733010ed44ba167acd34c82da15ef. Alpha remains published. No unapproved wording was edited.

All **311 source records** have been inspected: **279 non-token entries implemented**, **22 authored token records mapped**, **10 entries not yet loaded**. Printing duplicates are counted as source records, not new card names. Full identity/status/definition mapping: [CARD_MIGRATION.csv](CARD_MIGRATION.csv).

| Batch | Implemented non-token records | Token records mapped | Validation |
| --- | ---: | ---: | --- |
| 001–050 | 50 | 0 | 86 checks, build/images/decks/AI game |
| 051–100 | 50 | 0 | 157 cumulative checks, build/images/decks/AI game |
| 101–150 | 50 | 0 | 217 cumulative checks, build/images/decks/AI game |
| 151–200 | 46 | 0 | 277 cumulative checks, build/images/decks/AI game |
| 201–250 | 48 | 0 | 337 cumulative checks, build/images/decks/AI game |
| 251–300 | 31 | 15 | 397 cumulative checks, build/images/decks/AI game |
| 301–311 | 4 | 7 | 416 cumulative checks, build/images/decks/AI game |

Current baseline: **416 checks pass** (279 source comparisons, 22 token type/stat checks, 115 behavior checks). Desktop build passed. Preparation verified source/executable hashes, 301 full native PNGs and **37 test decks across all seven batches**. Final-batch seed 20261016 finished on turn 18, Ai(2) won, 1885 ms. Earlier batch 06 had native Reefgarden AI activator warnings; no custom exception/undefined SVar. No manual human playtest is claimed.

The final regression run additionally found/fixed Unpleasant Surprise's no-payment fallback selection and verifies the former controller of a stolen creature, actual minimum-power Crew payment, four comma-named Squad searches and private Experimental Invention peeking without card movement. Earlier source-batch reports remain historical; current engine fixes are cumulative.

## Owner decisions now being implemented

On 2026-10-17 the owner approved exact new wording for Wedding Ring, Just Do it, Annoying Running Gag, Friend or Foe, Tavern Slot Machine, Culinary Disaster, Rollercoaster of Emotions and Hofi. Moshpit is confirmed to count only immediate deaths. Gastronomic Downfall keeps three chapters and awaits the owner's chapter III effect. These ten cards are still excluded from this baseline; the next branch applies the approved source changes and implements the clarified mechanics. See OWNER_DECISIONS_2026-10-17.md and QUESTIONS_FOR_OWNER.md.

Review branches/PRs are preserved and pushed to the private repository, with Actions disabled. No deployment, production access change or hosted capacity was created. The unrelated website lore checkout is untouched. Prior checkpoints are archived in STATUS_HISTORY.md; they do not describe current verified counts.
