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

Confirmed owner scores from the Google Sheets snapshot in `confirmed-ratings.json`
now take precedence by stable source ID, including lands and zero-valued ratings.
The paired Beta MSE notes contain `Limited-Rating: xx` on all 292 non-token printings.
The snapshot records its source URL, CSV hash and missing entries; duplicate names
share the confirmed score. `ratings.json` labels each eligible name as confirmed
or provisional. Forge consumes the resulting ordinal `cbfrbeta.rnk`, so the
source 0–100 scores control pick order rather than being absolute in-engine scores.

All playable cards now have owner-supplied scores, including the 12 Pending
spreadsheet rows explicitly authorized by the owner in chat. Original sheet
statuses remain in the snapshot. Only 22 tokens remain unrated; they never enter
rankings or boosters. AJDE!'s score is stored for authoring but it remains
generated-only and excluded from rankings, boosters and starting decks.
Confirmed ratings do not change pack collation, AI synergy hints or card rules.
The existing heuristic/overrides fallback remains for future unrated cards.

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
