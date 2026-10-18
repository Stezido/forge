# Provisional Chaos Brawl limited configuration

Run `python3 chaosbrawl/limited/generate.py` to derive print sheets, pack slots,
block data and rankings from the reviewed source records. `--check` checks that
tracked output is current. The source MSE and printed card rules are untouched.

The website pack format is preserved: 7 commons, 3 uncommons, 1 rare with a 1/8
chance of mythic replacement, and 1 wildcard from all eligible source printings.
Each 12-card pack has no repeated printing. Tokens, generated-only AJDE! and
planeswalkers are excluded. Distinct native printings remain distinct, including
repeated nonbasic lands. Draft uses 3 packs, Sealed 8 (96 cards). M21 supplies
unlimited ordinary basics; native dual lands stay finite draftable cards even
though their MSE rarity is Basic Land.

Ratings use first-pass efficiency, removal, card advantage, evasion and narrow
setup requirements, with explicit exceptions in rating-overrides.json. The
ratings.json audit includes stable source IDs and scores; Forge loads cbt.rnk.
Eight targeted DeckHints in ai-hints.json add Food, Beer, artifact-token and
Milk/Cini Minis synergy using Forge's existing token-script inspection. These
are provisional AI pick preferences, not a balance study or printed card
properties. Refine them from real picks/matches and feedback.

`bash chaosbrawl/forge.sh test-limited` exercises the actual engine's boosters,
rankings, sealed deck builder, complete eight-seat draft and ordinary M11 packs.
The future isolated client profile must enable custom-card conformance because
the independent variants for overlapping official names are custom cards.

Commit/push/merge work normally. No downloadable release is authorized by this
implementation; the published beta.2 ZIP is unchanged.
