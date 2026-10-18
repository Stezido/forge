# Provisional Chaos Brawl limited configuration

Run `python3 chaosbrawl/limited/generate.py` to derive print sheets, pack slots,
block data and rankings from the reviewed source records. `--check` checks that
tracked output is current. The source MSE and printed card rules are untouched.

Each pack has 12 cards: six commons, one common/dual-land slot (50% each),
three uncommons, one rare slot, and one ordinary wildcard. The rare slot has a
2% chance of a planeswalker; otherwise it has the existing 1-in-8 mythic chance
(12.25% of all packs). The wildcard excludes dual lands and planeswalkers so
these special odds apply once per pack, not once per card. The Loan Syndicate
remains an ordinary rare land, not a dual-land replacement. All six authored
planeswalkers and all twenty finite dual-land printings are included. Tokens
and generated-only AJDE! remain excluded. Each pack has no repeated printing.
Draft uses 3 packs, Sealed 8 (96 cards). M21 supplies unlimited ordinary basics;
the custom dual lands remain finite despite their MSE rarity being Basic Land.

Ratings use first-pass efficiency, removal, card advantage, evasion and narrow
setup requirements, with explicit exceptions in rating-overrides.json. The
ratings.json audit includes stable source IDs and scores; Forge loads cbfrbeta.rnk.
Eight targeted DeckHints in ai-hints.json add Food, Beer, artifact-token and
Milk/Cini Minis synergy using Forge's existing token-script inspection. These
are provisional AI pick preferences, not a balance study or printed card
properties. Refine them from real picks/matches and feedback.

`bash chaosbrawl/forge.sh test-limited` exercises the actual engine's boosters,
rankings, sealed deck builder, complete eight-seat draft and ordinary M11 packs.
The future isolated client profile must enable custom-card conformance because
the independent variants for overlapping official names are custom cards.

Commit/push/merge work normally. No downloadable release is authorized by this
implementation; the published beta.3 ZIP and Android beta.1 APK is unchanged.
