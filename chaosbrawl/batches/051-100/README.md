# Batch 02 — saved draft, not ready for play

Owner requested saving work and continuing later. This branch is stacked on batch 01 and contains **45 definition drafts**, not 50 verified additions. The original 50-card worktree/PR remains independently runnable.

The source snapshot contains all 50 records cb-051–cb-100 from the owner-approved three-card wording source in website PR #50. It does not yet contain the subsequently approved Tactical Purge/Pumpkin Catapult text changes.

Missing definitions: cb-056 Youthful Inner Heat, cb-058 Tactical Purge, cb-075 Confusing Signals, cb-081 Pumpkin Catapult, cb-085 Bromoment. Six draft supporting tokens were added: five colors of 2/2 Spirit and Hangry Siren. Spar’s Bazaar cb-067/cb-095 has duplicate printings; preserve both stable IDs but use shared mechanics and distinct native images.

## Checks actually observed

- `git diff --check` passed.
- The first expanded suite (40 new metadata rows plus the original suite) had one loader failure: Controlling Relationship used an invalid GainControl static mode. That script was corrected afterwards.
- There was no rerun after that correction and the later five scripts/Plot/targeting engine edits. These additions are unverified. The metadata data provider does not yet include those five cards.
- No batch-02 build, decks, image preparation or AI smoke has passed. `prepare.py` and `forge.sh` still select batch 01. The old executable hashes deliberately reject the changed engine; do not bypass the guard or report this draft as playable.

## Owner decisions to preserve

- Numbing Mass Resurrection: one ETB resurrection; persistent ability loss for all creatures with finality counters. Approved wording is already in source snapshot.
- Academic Quarter: one normal Plot special action this turn, paying the card’s mana cost, no cast event now. Cast free only as a sorcery on a later turn. Approved wording is already in source snapshot.
- Last Duel: the caster must select one legal creature target per graveyard if able. The earlier optional “up to one” wording was superseded. Empty graveyards do not stop casting. Approved wording is already in source snapshot.
- Tactical Purge: owner approved ETB indestructible until end of turn, persistent ability loss. Apply approved wording to Beta before pinning final source.
- Pumpkin Catapult: owner approved one reflexive damage trigger per sacrificed drunken crew member, each with its own target. Sacrifice eligibility is limited to the specific crew event. Apply approved wording to Beta before pinning final source.
- Youthful Inner Heat: behavior confirmed as ward requiring the targeting opponent to let the enchanted creature’s controller draw one card. Keep current short MSE wording; the long text replacement was not approved.
- Confusing Signals: explicit as-you-cast player-choice wording was proposed and remains pending. Do not change MSE or assume its approval.

Exact follow-up texts and approval state are stored in website PR #50 at `sets/the-foul-realms/changes/beta-20261016-forge-handoff.md`.

## Resume

1. Apply only the two newly approved MSE texts through the website owner-chat workflow, regenerate the snapshot/hash/report and push the review branch. Await Confusing Signals approval before changing that card.
2. Refresh this batch’s source metadata/hash/commit from the exact review archive. Reuse native MSE rendering only when its archive hash matches.
3. Finish the five missing definitions; review the other 45 drafts with behavior tests. New generic OneEachIfAble target counting and the opt-in shared Plot allowance need tests, including empty/protected graveyards, casting triggers, multiple allowance effects and second-plot rejection.
4. Check Owizahrer’s draft condition: `Count$ChosenCardAmount` is not the native expression; use/review `ChosenCard$Amount` and test paid/declined/missing-creature upkeep. Source requires ONE other untapped creature.
5. Extend the metadata provider to all 100 records, then cover cross-player resurrection/finality, global Food sacrifice, counters/auras/costs and actual Crew/Plot execution. Do not count metadata checks as proof of card behavior.
6. Generalize preparation/decks for batch 02, handle duplicate-name image suffixes, refresh reviewed executable hashes only after checks pass, build desktop, run seeded AI game, and push reviewable results.

Local logs and scratch generators are ignored under `.chaosbrawl-local`. The generators append data/engine blocks and MUST NOT be rerun blindly. Continue from these committed definitions.
