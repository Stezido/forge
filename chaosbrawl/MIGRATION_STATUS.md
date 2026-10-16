# Beta migration

Canonical source: the registered development MSE in `Stezido/Chaosbrawl-Website`. The website catalogue remains published Alpha; this migration explicitly reads Beta.

| Batch | Range | Status |
| --- | --- | --- |
| 01 | cb-001–cb-050 | 50 definitions verified; native images/decks prepared; AI smoke passed |
| 02 | cb-051–cb-100 | 50 definitions verified; 157 cumulative checks; build/images/decks/AI smoke passed |
| 03 | cb-101–cb-150 | 50 definitions drafted; engine and tests in progress |
| 04 | cb-151–cb-200 | Pending |
| 05 | cb-201–cb-250 | Pending |
| 06 | cb-251–cb-300 | Pending |
| 07 | cb-301–cb-311 | Pending |

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
