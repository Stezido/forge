# Chaos Brawl local mechanics spike

Branch `chaosbrawl/local-testing` is based on Card-Forge upstream commit `34a8662b7966a6ee757411feae2ae8ff38cce436` (2026-10-15). It implements ten custom cards plus Beer. This is an isolated local experiment, not an upstream Forge feature or a full-set conversion.

```sh
bash chaosbrawl/forge.sh test
bash chaosbrawl/forge.sh build
# From the companion website checkout, prepare the profile/art/deck:
node scripts/playtesting/forge/prepare.mjs /path/to/Chaosbrawl-Forge
# Then in this checkout:
bash chaosbrawl/forge.sh launch
bash chaosbrawl/forge.sh smoke
```

Requires Java 17+ and Maven. The wrapper also recognizes isolated tools at `~/.local/share/chaosbrawl/forge-tools/{jdk,maven}`, or `CHAOSBRAWL_FORGE_TOOLS`. Runtime files are ignored under `.chaosbrawl-local`; source-development tests use an isolated profile too. The launcher disables Sentry reporting for this custom experiment.

The website companion branch contains the full instructions, reviewed card scripts and catalogue manifest: [forge-local.md](https://github.com/Stezido/Chaosbrawl-Website/blob/codex/forge-local-testing/docs/playtesting/forge-local.md).

Custom rules are enabled per match through `GameRules.isChaosBrawl()`, initialized from `-Dforge.chaosbrawl=true`. Drunken counters add +1/+1 independently of ordinary +1/+1 counters. Before normal untapping, active non-phased creatures remove counters over tolerance (default 2), tap and receive that many stun counters. Drunkard supplies fixed tolerance 1 through its SVar. The 12 integration tests exercise all ten scripts, ordinary-match isolation, payments, triggers, counterspell choices and actual untap behavior. Full-set mechanics, dynamic tolerance modifiers and AI strategy tuning remain outside this first batch.

AI-assisted implementation: OpenAI Codex. Upstream license and notices remain in place; this branch does not bundle the website's artwork.
