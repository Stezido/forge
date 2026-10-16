# Batch 02 — cb-051–cb-100

50 new card records are implemented on top of batch 01. The cumulative package has 100 records and 99 distinct names: Spar’s Bazaar cb-067/cb-095 has identical mechanics and two native printings. Stable IDs and both native image indices are preserved.

## Observed validation

- All **157 checks passed**: 100 source metadata comparisons and 57 effect/rules checks. The original 86-check suite remains included.
- New behavior checks exercise Last Duel’s mandatory per-graveyard legal targeting, simultaneous resurrection/finality and persistent ability loss, opponent-card reanimation under your control, Owizahrer’s one-other-creature upkeep, all-creature counter totals, Hangry transformations, all-counter and one-counter transfer, global Food sacrifice, Beer production for both players, twin conditions, last-charge sacrifice, conditional destruction drain, increasing Fulfillment requirements and tap/draw accounting.
- Paid normal Plot is tested as a special action: immediate shared allowance consumption, no cast now, no same-turn free cast, later sorcery-speed free casting, multiple allowances and end-of-turn expiry. Tactical Purge is cast as an actual Aura and its ETB protection expires while blanking remains. Unpaid Overdose Revival upkeep exiles the creature via finality.
- Pumpkin Driver actually pays Crew 3 alone at printed power 1. Sacrifice eligibility is restricted to the triggering crew group; two sacrifices resolve independent fixed 2-damage triggers for 4 life loss. Native AI target selection bypasses the controller spy, so the test asserts the actual effect instead of a spy invocation count.
- Desktop package build passed. Preparation validated the canonical source and executable hashes, all 100 full-face PNG headers/dimensions and six batch decks. Duplicate Bazaar faces use suffixes 1 and 2. Native MSE rendered all 311 records for this archive without warnings.
- Seed **20261016**: completed AI game, turn 10, Ai(2) won; Forge reported **1790 ms**. Catapult dealt the winning combat damage. No exception/undefined-SVar/custom API error appeared in the smoke log.

Local evidence is ignored under `.chaosbrawl-local`: `verified-100-tests-2.log`, `build-100.log`, `prepare-100.log`, `smoke-100.log` and native image caches. The source/executable manifests are committed. First batch’s separate branch/worktree remains reproducible against its own pinned source.

## Canonical source and approved decisions

The pinned source is website PR #50, MSE SHA-256 `81e36272d3d2fc4308dc4d65c54be779944733010ed44ba167acd34c82da15ef`. Five owner-approved wording changes are applied to that review source: Last Duel, Numbing Mass Resurrection, Academic Quarter, Tactical Purge and Pumpkin Catapult. Alpha remains the published website variant.

Last Duel’s earlier optional “up to one” proposal was superseded: the caster must choose one legal target per graveyard if able. Numbing is one ETB reanimation followed by persistent blanking. Plot follows normal Plot timing/payment. Owizahrer keeps the owner’s one-other-creature version. Pumpkin uses one reflexive damage trigger per drunken crew sacrifice.

Youthful Inner Heat keeps its current short MSE wording and implements the owner-confirmed Ward draw payment. **Confusing Signals wording approval remains collected for later**: the implementation fixes a non-targeted chosen player during casting, targets only their creatures, and draws only for creatures actually tapped. Its MSE text is unchanged. See `QUESTIONS_FOR_OWNER.md`.

Seven supporting tokens are new in this batch: five colors of 2/2 Spirit, Hangry Siren and Pumpkin Gang Driver. The dynamic Spirit/Driver tokens have no corresponding authored MSE token illustration; their rules work with Forge’s token presentation. Full card faces for the 100 source records come from native MSE, not from editing rendered text.

## Manual testing

Use the commands in `chaosbrawl/README.md`. Default preparation selects this latest batch and its cumulative metadata/scripts. The main 100-card deck includes one of each record 51–100 plus 50 basic lands; five smaller group decks focus on ten cards each. Restart Forge after preparation. A full manual human playtest has not been claimed.
