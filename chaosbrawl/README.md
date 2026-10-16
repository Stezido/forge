# Chaos Brawl — local Forge Beta migration

Private development: `Stezido/Chaosbrawl-Forge`. The full upstream source and its GPL-3.0 license/notices are retained. The initial private snapshot corresponds to Card-Forge upstream `34a8662b` plus the earlier ten-card integration at public fork commit `7f16ea60`. New migration work is private.

The authoritative set is the registered **The Foul Realms — Beta** MSE in the companion website repository. The published website catalogue still selects Alpha. A source hash/native UID snapshot and reviewed executable-file hashes pin each migration batch. AI interpretation produces the executable rules; displaying Oracle text alone does not implement a card.

See [migration status](MIGRATION_STATUS.md) and [batch 01](batches/001-050/README.md). The current package contains cb-001–cb-050, including the original ten cards updated for Beta. Supporting tokens accompany their first dependent card.

## Run locally

Requires Java JDK 17+ and Maven. The wrapper recognizes the isolated Java 21/Maven tools under `~/.local/share/chaosbrawl/forge-tools`, or `CHAOSBRAWL_FORGE_TOOLS`. Runtime profiles, generated decks and pictures remain under ignored `.chaosbrawl-local`; the wrapper disables Sentry reporting.

```sh
cd /home/stefan/github/Chaosbrawl-Forge-Private
bash chaosbrawl/forge.sh test
bash chaosbrawl/forge.sh build
python3 chaosbrawl/prepare.py --website /home/stefan/github/Chaosbrawl-Website --render
bash chaosbrawl/forge.sh smoke
bash chaosbrawl/forge.sh launch
```

Preparation verifies the canonical MSE hash, imported card metadata and reviewed executable files. `--render` uses the installed native `mse.com` to render the full source, with UID filenames, before copying this batch's complete card faces into Forge. Pass `--mse` for a different native installation. Omit `--render` to reuse the already verified renders. This machine already has all 311 native source renders; preparation and the 50-card deck are ready.

In Constructed, select **Chaos Brawl Beta Batch 01** for yourself and an AI opponent. This deck contains one copy of each of the 50 cards and ten of each basic land (100 cards). Five smaller **Chaos Brawl Beta 01 - Group N** decks contain four copies of ten consecutive cards and six of each basic (70 cards). These are mechanics exercises, not balanced competitive decks. Filter cards to **Chaos Brawl Beta Batch 01 (CBT)**. Restart Forge after preparing a changed card package.

## Validation and scope

The current suite has 86 passing checks: 50 source-to-runtime comparisons plus engine behavior tests. They exercise real payments, targeting, tokens, counter events, actual untapping, Obsession attack requirements/removal, Beer variants, Food creatures, Forest animation, graveyard access and Overdose's repeated-cast/end-step loss. A completed seeded AI game passed with the 50-card package. A full manual human playtest and exhaustive multiplayer/replacement combinations remain separate checks. The AI has not been tuned for Chaos Brawl strategy.

Custom Drunken/Obsession/turn-order rules are enabled per match through `GameRules.isChaosBrawl()`, initialized from `-Dforge.chaosbrawl=true`. Default tolerance is 2; fixed printed tolerances are supported. Celestial Waters is the owner-confirmed exception: move counters before tolerance conversion and normal untapping. Food remains when explicitly printed on nonartifact creatures; it is not added to changeling's global creature-type list.

To extend a batch, read the next canonical MSE records, implement the executable scripts and dependent mechanics, add behavior checks for new interactions, run the existing suite/build and prepare/smoke the package. Refresh reviewed executable hashes only after assessing source changes. Preserve stable IDs, frozen source variants, upstream notices, and existing review branches/PRs. GitHub Actions is disabled in the private repository; validation runs locally with no new hosted resources.
