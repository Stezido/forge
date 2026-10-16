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
