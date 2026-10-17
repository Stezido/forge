# Adventure readiness — local audit, 2026-10-17

Owner priority is Windows classic desktop release first, then Adventure with
custom draft/sealed events and provisional AI rankings, then mobile. Windows
package 0.1.0-beta.1 and website delivery are implemented and locally verified;
hosted publication awaits current Actions allowance/hard-budget facts and shared
staging availability. Do not bypass that gate or replace the first package.

## Verified

The separate LibGDX desktop entry point is `forge-gui-mobile-dev`, not the Swing
`forge-gui-desktop` package. Local compilation of all seven required modules
passed with `mvn -B -pl forge-gui-mobile-dev -am -DskipTests compile` (40.231 s).
The shared engine already contains the reviewed custom mechanics. This is compile
evidence only: no Adventure window, event, match or mobile APK has been tested.

Audit against engine fe9a753e / packaging baseline 435cf48d found:

- `res/editions/Chaos Brawl Local Test.txt` registers CBT but has no booster
  template. `CardEdition.hasBoosterTemplate()` therefore cannot qualify it for
  Adventure event selection.
- Neither `res/blockdata/blocks.txt` nor `fantasyblocks.txt` contains a CBT block.
  Adventure's valid-block selection requires a registered block whose editions
  have booster templates with more than seven cards.
- There is no CBT entry in the draft-ranking resources. `CardRanker.getRawScore`
  assigns missing rankings its lowest base score; useful AI picks/deck building
  need provisional per-card ranks plus relevant synergy hints.
- Adventure applies edition/event whitelist and restriction configuration before
  selecting blocks. A reliable custom-set event setup needs explicit CBT
  eligibility and a tested path to open those events.
- Plane-scoped `AdventureOverrides` already supports additive blocks and booster
  overlays without mutating upstream static data; inspect that path before adding
  Java hooks. Do not alter normal Forge or existing Adventure saves by default.

## Next local implementation after Windows release

1. Define/test CBT booster collation and a draft/sealed block. Review the existing
   website pack generator and native rarities, including lands and excluded AJDE!,
   before choosing slots. Preserve MSE card rules and the released Windows hashes.
2. Add separate, explicitly provisional rankings for eligible non-token cards;
   preserve duplicate-printing identity and basic-land behavior. Verify that all
   eligible cards have a usable score and AI picks respect colors/synergies.
3. Prepare an isolated Adventure profile/overlay and build the LibGDX executable.
   Make the custom events reachable without replacing upstream mode behavior.
4. Exercise a real draft and sealed pool, AI deck construction and a complete
   event match. Validate token/art resources and native Windows startup before
   advertising Adventure availability.
5. Only afterward prepare the mobile/Android build and signing/distribution path.

No card rankings, booster/block data, Java behavior, MSE source or existing
Windows release artifact was changed during this audit. Compile output and its
log remain ignored under `.chaosbrawl-local`.
