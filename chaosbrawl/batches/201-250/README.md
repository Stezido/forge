# Batch 05 — cb-201–cb-250

48 additional records (41 new definitions, seven repeated land printings) reach **244 non-token records / 236 names**. Tavern Slot Machine and Culinary Disaster remain excluded pending the precise damage-trigger and life-payment decisions collected in QUESTIONS_FOR_OWNER.md. The four earlier deferred cards also remain excluded. No source wording was edited.

All **337 checks passed**: 244 source comparisons and 93 behavior checks. New tests cover real Grinder land-sacrifice/mana payment, Rotborn's upkeep token-versus-nontoken bonus and sacrifice without payment, once-per-event Obsession removal versus individual counter triggers, Austrian Slap's 2/4 damage distinction, per-counter Patron growth versus once-per-event Puff Mama treasure creation, actual Jolly Brewbelly counter payment/failure, continuous Location/drunken keywords, paid/unpaid Flora upkeep, simultaneous Copycat counter-map copying without recopying old counters, paid Sofa Pizza graveyard return, Honest Laborer's actual-attack restriction, and Sauna Elemental triggering only for the second instant/sorcery.

The behavior test found that chained numeric operators are not supported by Forge's amount grammar; Austrian Slap now uses separate intermediate SVars and native CardCounters.DRUNKEN. Honest Laborer uses the native CheckSVar/AttackersDeclared restriction. Sofa Pizza uses a timed grant on its specific graveyard object; the collected timing interpretation remains explicit.

Desktop build passed. **244 full native PNGs** and six decks were prepared: main 98 cards; groups 70/70/70/70/62. Seed 20261016: **turn 11**, **Ai(2) won**, **2108 ms**, no custom exceptions or undefined SVars. Local evidence: .chaosbrawl-local/{verified-244-tests,prepare-244,build-244,smoke-244}.log. No manual human playtest is claimed.

Canonical Beta remains SHA-256 81e36272d3d2fc4308dc4d65c54be779944733010ed44ba167acd34c82da15ef in website PR #50. Continue 251–300 and the final 301–311 records, distinguishing authored token printings from playable cards.
