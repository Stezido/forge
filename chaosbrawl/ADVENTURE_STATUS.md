# Chaos Brawl Adventure / limited implementation — 2026-10-18

The published Windows download remains 0.1.0-beta.2 (classic Swing client, CBT).
Adventure uses a separate LibGDX desktop executable. This branch adds Adventure
and classic limited source/configuration; it does not replace the published ZIP.
A new downloadable release requires an explicit owner request. The requested
CBFRbeta / final CBFR code migration belongs to the future release batch.

## Implemented and verified

- Website-equivalent boosters: 7 common, 3 uncommon, 1 rare/mythic (mythic 1/8),
  1 wildcard, 12 distinct source printings. Eligible: 285 printings / 275 names.
  Tokens, generated-only AJDE! and all planeswalkers are excluded. Native nonbasic
  dual lands stay finite wildcard cards; M21 supplies unlimited ordinary basics.
- Three-pack Draft (36 picks each, eight seats); eight-pack Sealed (96 cards).
- Provisional per-card AI rankings and eight targeted Food/Beer/artifact-token/
  Milk synergy hints, separate from MSE rules. Seven real-engine limited tests
  include 1,000 packs, actual draft picks/decks and existing M11 behavior.
- Isolated ChaosBrawl Adventure plane/profile reuses Shandalar resources and
  whitelists CBT Draft/Sealed. Ordinary planes keep their original event rolls.
  This is a custom event setup, not a newly authored Chaos Brawl campaign.
- Six real Adventure model checks cover configuration/custom match rules, block
  eligibility, format compatibility, actual Sealed entry, saved-event restoration,
  and complete Draft/AI deck construction. With --games, four actual headless
  AI-vs-AI games run with AdventureEvent rules; caught AI exceptions fail checks.
- Fixed a null payer in AI pre-cast upkeep-cost evaluation by evaluating a copy
  for the AI. All 466 card/behavior checks pass, including the new regression.
- Seven-module Adventure desktop build passes. Actual Linux LibGDX GUI reaches
  a new world, tavern, custom Sealed event, paid entry and 96-card deck editor
  with native custom images. Isolated world autosave succeeds.

Native Windows model/match/image checks also pass: nine checks, including all
292 exact PNG/runtime paths and four complete decisive games. Actual native
Windows Adventure menu, paid Draft entry, 36-pick completion, saved-world/event
restoration, legal test deck and live match with native images are verified.
Linux Sealed completed all event stages, six actual GUI games (rounds 2-1 then
1-2), the AI-only final, standings, two reward boosters and saving afterward.
An external ignored test harness hands the human test seat to Forge AI; no
human-vs-AI or remote playtest is claimed, and no harness/backdoor ships.

Custom desktop launches skip upstream Forge updates to protect the custom engine
and scripts. Ordinary Forge and Android update behavior remains unchanged.

## Reproduce locally

Use the dedicated JDK/Maven under ~/.local/share/chaosbrawl/forge-tools, or an
appropriate local JDK 21/Maven environment. From this checkout:

    python3 chaosbrawl/limited/generate.py --check
    bash chaosbrawl/forge.sh test-limited
    bash chaosbrawl/forge.sh test
    mvn -B -pl forge-gui-mobile-dev -am -DskipTests -Dlaunch4j.skip=true package
    python3 chaosbrawl/prepare_adventure.py --shared-runtime /path/to/reviewed/.chaosbrawl-local

From .chaosbrawl-local/adventure/run, run Java with -Xmx4g,
-Dforge.chaosbrawl=true and -Dsentry.enabled=false. Launch the built
forge-gui-mobile-dev fat JAR. For the reproducible headless checks, additionally
set -Djava.awt.headless=true, use that JAR on the classpath and execute
chaosbrawl/VerifyAdventureEvents.java with --games --images (absolute paths).
A bundled JRE without the source compiler needs the verifier compiled with
javac first, then java -cp <jar-and-compiled-class> VerifyAdventureEvents.

Keep the test JAR stable while its GUI runs; rebuilding the live JAR invalidates
Java's cached class offsets. Local logs/captures/saves are ignored artifacts.

In Adventure, choose the ChaosBrawl plane, create/load its separate character,
then visit a town inn and select Info. Events offer CBT Draft or Sealed; standard
entry fees and tournament rules apply (Draft 3000 gold / 50 shards; Sealed 6000
/ 100). For repeatable testing only, Forge's existing F9 console supports
`set event cbt sealed` or `set event cbt draft` while inside a town with an inn.
No test backdoor or automated human control is shipped.

## Remaining work for a future release

Native Windows Draft also completed its real best-of-three match (0-2), match
return, remaining AI-only bracket rounds, rewards and saving. Linux restart
restores the two Sealed reward boosters and finalized event correctly; Windows
post-reward restart also restores exactly one Draft reward booster and the
finalized event correctly. Both complete client event flows are verified. Keep issue #56 open for
the coordinated future set-code/release batch. The published classic Windows
beta.2 is unchanged; no Adventure archive or download has been published. Android/mobile
build/signing/publishing remains separate issue #57. Custom booster rewards use
the ordinary card-back fallback until pack art exists. Manipulative Temptress can
log a rejected AI target choice; it does not stop the tested games. Provisional
AI ratings and these play choices still need refinement from human testing. Real remote/human-vs-AI
validation remains issue #55. No paid infrastructure or Actions is enabled.

The native Windows development launcher is `C:\MTG\ChaosBrawlForge-Adventure-Local-20261018\Play Adventure (local test).cmd`.
It uses a separate profile and depends on read-only resource/runtime junctions
to the preserved beta.2 test folder, so it is not a portable release. Exact
local JAR hash and verification counts are in limited/verification.json.
