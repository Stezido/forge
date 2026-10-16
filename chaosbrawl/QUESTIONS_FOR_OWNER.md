# Collected card questions

The owner requested collecting questions for later and continuing migration without interruptions. Proposed wording is not approval. Do not edit the Beta MSE until the owner approves the exact text.

## cb-075 — Confusing Signals: pending wording approval

Current Forge implementation fixes the chosen player while casting, without targeting that player. Only that player's creatures can be targeted. Already-tapped creatures receive stun but do not produce a draw. The player choice is preserved if control changes before resolution.

Proposed MSE wording (not yet approved):

> As you cast this spell, choose a player.
> Tap up to two target creatures that player controls. Put a stun counter on each of them. The chosen player draws a card for each creature tapped this way.

The current MSE text is retained until approval. Tests cover mixed tapped/untapped creatures and the chosen-player target restriction.

## Upcoming upkeep interactions to review while migrating 101–150

Natalie grants other Sirens upkeep {1}; Süüü says Sirens cannot have upkeep costs while Natalie is controlled; Shady Herb Dealer reduces upkeep costs by one mana of any color; The Loan Syndicate produces mana restricted to upkeep. Preserve these printed interactions, explicitly document any unresolved timing/payment choices, and collect a concrete question if the engine implementation requires a new design choice.

## Confirmed decisions, not open questions

- Last Duel: its caster must choose one legal creature card per graveyard if able. The optional “up to one” proposal was superseded.
- Numbing Mass Resurrection: one ETB resurrection, then persistent ability loss for creatures with finality counters.
- Academic Quarter: one normal Plot special action this turn for the creature card's mana cost; no cast event now; free sorcery-speed cast only on a later turn.
- Tactical Purge: ETB indestructible this turn, permanent blanking while enchanted.
- Pumpkin Catapult: only this crew event's drunken creatures may be sacrificed; separate reflexive 2-damage trigger per sacrifice, each with its own target.
- Youthful Inner Heat: confirmed Ward behavior lets the enchanted creature's controller draw one card as payment by the targeting opponent. Keep the current short MSE wording.

## cb-126 — Cini Minis: optional Milk sacrifice timing

Provisional Forge interpretation: pay/sacrifice Cini Minis as the activation cost, gain 3 life on resolution, then optionally sacrifice Milk on resolution to draw three cards. The phrase “as part of this ability” does not specify an additional activation cost. Confirm this timing later before any MSE rewording; current text is retained.

## cb-141 — Shady Herb Dealer: reduction choices

Forge permits choosing one mana symbol to remove from each marked upkeep cost per Dealer, including a generic symbol. It preserves nonmana upkeep costs. Confirm whether “one mana of any color” is intended to include reducing generic upkeep {1}; this matches the apparent Natalie synergy but is an implementation assumption. No MSE text changed.

## cb-147 — The Rake: source subtype typo

The MSE subtype is “Euipment”. Forge normalizes it to “Equipment” so Equip and equipped-creature effects function. Proposed MSE field correction: Euipment → Equipment. Await approval before editing the source.

## cb-113 — Distant Cousin Shotcrusher: duplicate interaction

The printed mandatory trigger is implemented per Drunken counter added to another creature you control. Two copies under one controller can trigger each other repeatedly. The source text is retained; collect an owner decision later if a non-looping design is desired.

## Deferred batch 04 cards (not yet playable)

- cb-154 Wedding Ring: what exactly does “bond” mean? Define duration, whether multiple bonds coexist, whether copied spells select new targets, and what happens when an original target cannot legally receive the copy.
- cb-163 Just Do It: d20 determines how many lands to sacrifice after casting; when should affected opponents/creatures be chosen? Should the drawn land cards go straight onto the battlefield? An explicit reflexive trigger may be required; collect exact wording for approval.
- cb-192 Annoying Running Gag: mandatory cast on draw with four lands—does the discard count casts of this physical card or every spell with this name? What happens if it cannot legally be cast, and does the shuffle replace going to the graveyard?
- cb-199 Friend or Foe: who wins if both d20 rolls are 20? Does a critical 20 override the ordinary comparison even when the other player also rolls 20?

## Additional source field corrections awaiting approval

- cb-153 Magical Bucket Pump: Equipemnt → Equipment.
- cb-184 Recycle: remove “Common” from the subtype field (retain rarity Common).
- cb-186 Always With Us: remove “Rare” from the subtype field (retain rarity Rare).

Forge normalizes these fields only; the canonical MSE remains unchanged.

## Deferred batch 05 cards

- cb-215 Tavern Slot Machine: on a 20, after choosing to sacrifice the Machine, should the damage use a separate reflexive “When you do” trigger, with the target chosen then and a response window? Proposed text for that last part: “Then you may sacrifice Tavern Slot Machine. When you do, it deals damage equal to the number of artifact tokens you control to any target.” Await approval before editing MSE or loading this card.
- cb-225 Culinary Disaster: does “Upkeep—Lose 2 life” mean paying 2 life as an upkeep cost (impossible at 1 life), or choosing to lose 2 life as an effect even at 1 life? The current text distinguishes neither the payment restriction nor the choice to sacrifice instead. Await an owner decision; no approximation loaded.

## Batch 05 implementation interpretation

Sofa Pizza's graveyard trigger grants a paid {1}{B} activated return option until the end of your next turn, usable with ordinary priority, not only sorcery speed. The permission tracks that graveyard object and expires when it leaves. Source wording remains unchanged; confirm this timing when reviewing questions.

## Deferred batch 06 cards

- cb-269 Moshpit: does “dies this way” count only the immediate deaths from this damage, or any later death that turn of a creature dealt damage? Proposed standard wording for the latter: “Moshpit deals 2 damage to each creature. Whenever a creature dealt damage this way dies this turn, Moshpit deals 1 damage to that creature's controller.” The immediate-only interpretation needs a different implementation. No source edit or card loaded yet.
- cb-279 Gastronomic Downfall: chapter III is blank. Should this be a two-chapter Saga, or what should III do? Chapter II also needs explicit targeting timing if damage is intended as a separate trigger after counter removal.
- cb-282 Rollercoaster of Emotions: should chapter II simultaneously move each creature's original counters, or process creatures in a chosen order (which can move newly received counters again)? Who chooses that order and each destination? Preserve source until clarified.
- cb-283 Hofi, the Fragrant: is sacrificing a Food required to get the -2 ability's -3/-3? For the ultimate, when and how should the creatures “that died due to this effect” be counted for the life loss? A death trigger/delayed count needs precise timing and source wording approval.

## Additional batch 06 source corrections/interpretations

- cb-254 Wild Fermentation: source type “Entchantment” is normalized to Enchantment in Forge only; proposed source field correction awaits approval.
- cb-263 Fighting Pit Champion: “Plaincycling” and “Plain” are interpreted as Plainscycling and Plains. Source text unchanged; approve these exact spelling corrections later.
- cb-299 authored token says “Ari, the ripper”, while Peter creates “Ari, the Ripper”. Runtime uses the creator's name. Propose capitalizing the token name's R, pending approval.
- cb-297 authored Vengeful Gnome Pack face lacks haste, while Sasa explicitly creates it with haste. The runtime token used by Sasa has haste as instructed by Sasa; no MSE change was made.
