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
