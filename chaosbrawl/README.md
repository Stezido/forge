# Chaos Brawl — local Forge Beta

All **314 Beta records** are available: **292 non-token entries and 22 authored token mappings**, including Maternal Instinct, Higher Protection and Haze-Fueled Frenzy. No collected rules question remains open. [Private PR #8](https://github.com/Stezido/Chaosbrawl-Forge/pull/8) contains the cumulative implementation; [website PR #50](https://github.com/Stezido/Chaosbrawl-Website/pull/50) holds the authoritative Beta MSE. Alpha remains published.

## Play locally

```sh
cd /home/stefan/github/Chaosbrawl-Forge-Private-Owner-Decisions
bash chaosbrawl/forge.sh launch
```

In Constructed, select **Chaos Brawl Beta Batch 01–08**, or a focused **Chaos Brawl Beta NN - Group N** deck. **41 decks** are prepared. Batch 08 contains the clarified cards and the three new instants. Filter the card pool to **Chaos Brawl Beta Local Test (CBT)**. Exercise decks are not balanced competitive decks; authored tokens and generated-only AJDE! are excluded from starting decks.

## Reproduce validation and preparation

```sh
bash chaosbrawl/forge.sh test
bash chaosbrawl/forge.sh build
python3 chaosbrawl/prepare.py --website /home/stefan/github/Chaosbrawl-Beta-Forge-Wording --batch owner-decisions-001 --render
bash chaosbrawl/forge.sh smoke
```

The wrapper uses isolated Java/Maven tools, enables Chaos Brawl and disables Sentry. Profiles, decks and images stay under ignored `.chaosbrawl-local`. Omit `--render` to reuse verified UID-named native images. Preparation verifies the exact source hash, all source records and reviewed executable hashes. Earlier batches keep their historical pins; use the latest owner-decisions batch for the current source. See [status](MIGRATION_STATUS.md), [card mapping](CARD_MIGRATION.csv) and [batch report](batches/owner-decisions-001/README.md).

**465 checks pass**, covering source/type/stat comparisons and actual rules, choices, payments and timing. Desktop build, native images/decks and a seeded AI game passed. Manual human and remote playtests remain separate validation. AI strategy is not tuned; earlier native Reefgarden warnings are documented in batch 06. Original official name collisions and ordinary non-Chaos-Brawl games have regressions.

## Play remotely

Forge supports manually arranged host/join lobbies, with no built-in matchmaking. Use the same Chaos Brawl build, card resources and launcher on every device. Desktop: **Online Multiplayer → Lobby → Host a Game / Join a Game**. Share the host address and port (default TCP 36743); remote access needs appropriate forwarding or a shared private network. No firewall/network configuration has been changed by this migration, and no remote Chaos Brawl session is claimed tested. [Official network-play documentation](https://github.com/Card-Forge/forge/wiki/Network-Play).

The private repository preserves upstream source/GPL notices, existing PRs and the original baseline on main. GitHub Actions remains disabled; all checks run locally.
