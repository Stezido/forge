# Chaos Brawl — local Forge Beta migration

Private development repository: Stezido/Chaosbrawl-Forge. The full upstream source and GPL-3.0 notices are preserved. GitHub Actions is disabled; checks run locally.

The authoritative authoring source is the registered Beta MSE in website PR #50. Alpha remains published. The verified baseline through cb-311 contains **279 non-token entries and 22 token mappings**; ten cards await the collected owner decisions/implementation. See [migration status](MIGRATION_STATUS.md), the [complete card list](CARD_MIGRATION.csv) and [final batch report](batches/301-311/README.md).

## Run locally

```sh
cd /home/stefan/github/Chaosbrawl-Forge-Private-301-311
bash chaosbrawl/forge.sh test
bash chaosbrawl/forge.sh build
python3 chaosbrawl/prepare.py --website /home/stefan/github/Chaosbrawl-Beta-Forge-Wording --render
bash chaosbrawl/forge.sh smoke
bash chaosbrawl/forge.sh launch
```

The wrapper uses the isolated Java/Maven tools under ~/.local/share/chaosbrawl/forge-tools and disables Sentry. Runtime profiles, decks and images stay under ignored .chaosbrawl-local. Omit --render to reuse verified UID-named native renders. Preparation checks the source hash, all source identities/metadata and reviewed executable hashes before creating all **37 batch/group decks**.

In Constructed, choose any **Chaos Brawl Beta Batch 01–07** or a smaller **Chaos Brawl Beta NN - Group N** for yourself and the AI. Filter cards to **Chaos Brawl Beta Local Test (CBT)**. These are mechanics exercise decks, not balanced competitive decks. Authored tokens, generated-only AJDE! and unresolved cards are excluded. Restart Forge after changing a package. Source code is on the migration branch; private main preserves the original baseline.

## Validation

**416 passing checks** cover 279 source comparisons, 22 token type/stat comparisons and 115 behavior/payment/timing checks. Desktop build, 301 full native PNGs, all decks and a completed seeded AI game passed (final batch: turn 18; 1885 ms). A full manual playtest and exhaustive multiplayer/replacement combinations remain separate checks; AI strategy is not tuned. Native Reefgarden activator warnings in the earlier batch-06 smoke are recorded in its report.

Custom turn-order/counter rules are enabled per match by -Dforge.chaosbrawl=true; ordinary games have a regression with the flag disabled. Fixed tolerances and external modifiers are real abilities, including correct blanking. Celestial Waters moves counters before tolerance conversion/untapping. Creature Food does not imply Artifact. Independent Beta variants preserve official Recycle, The Fallen and Dungeon Master printings.

The owner is now answering [collected questions](QUESTIONS_FOR_OWNER.md). Exact approved wording is applied to MSE separately with receipts; no proposal is approval. Review branches/PRs, frozen variants, native UIDs and production access remain preserved.
