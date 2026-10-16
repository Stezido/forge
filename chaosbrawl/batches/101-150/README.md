# Batch 03 — cb-101–cb-150

50 new definitions and three Gnome/Elemental token scripts extend the cumulative package to 150 records (149 distinct names). All **217 checks passed**, including 150 source comparisons now checking native types/colors as well as cost, Oracle and stats, plus 67 effect/rules checks.

New behavior checks verify three real blue/black AJDE! cards shuffled into the library, the zero starting-deck limit, selective stun/obsession removal with a combined two-counter maximum, increasing/decreasing native counters, Natalie’s tax with Dealer discounts/Süüü’s waiver, choosing a colored upkeep reduction while preserving nonmana costs, actual upkeep-only mana payment, the original declared attacking pair and Stethoscope’s persistent exile permission with actual {1}{R}{W} payment from three Islands after the Equipment leaves.

The Colossus test explicitly chooses payment through the native cost transaction; AI strategy may instead sacrifice it. The native AddCounter cost syntax was corrected. The new entry test also found the unsupported etbTapped shorthand: both Untamable Grove Colossus and the earlier Straggler now use real ETB replacement effects, with regression coverage. Earlier batch-02 checks did not cover Straggler’s entry state; this batch fixes it.

Desktop build passed. Preparation verified reviewed executable hashes, 150 native full-card PNGs and six batch decks. AJDE! is displayed as a card but excluded from every starting deck; the main batch deck has 99 cards, and group decks contain 66 or 70 cards. Windtongue creates the three real cards during play. The dynamic/source token illustrations remain byte-for-byte MSE assets where available.

Seed **20261016**: completed AI game, **turn 13**, **Ai(1) won**, Forge reported **2384 ms**. No custom exception or undefined-SVar error appeared. Local evidence: `.chaosbrawl-local/verified-150-types-tests.log`, `prepare-150.log`, `build-150.log`, `smoke-150.log`. No manual human playtest is claimed.

The canonical MSE is unchanged from batch 02, SHA-256 `81e36272d3d2fc4308dc4d65c54be779944733010ed44ba167acd34c82da15ef`, in website PR #50. AJDE!’s runtime definition is blue/black because Windtongue explicitly creates it with those colors despite its zero mana cost. The source subtype typo Euipment on The Rake is normalized to Equipment in Forge only.

Collected questions remain in `QUESTIONS_FOR_OWNER.md`: Cini Minis optional Milk sacrifice timing; whether Shady Herb Dealer’s reduction includes generic {1}; The Rake’s source typo; the duplicate Shotcrusher loop; and the earlier Confusing Signals wording. Current timing/reduction interpretations are explicit, tested assumptions; none of these pending texts was written into MSE. Continue other cards without interrupting the owner.
