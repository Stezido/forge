package forge.game;

import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.spellability.SpellAbility;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;

/** Opt-in Chaos Brawl rules for local Beta migration, enabled per match. */
public final class ChaosBrawlRules {
    private ChaosBrawlRules() { }

    /** The global Obsession requirement does not pick a defender or waive attack costs. */
    public static boolean mustAttack(final Card card) {
        return card.getGame().getRules().isChaosBrawl() && card.isCreature()
                && card.getCounters(CounterEnumType.OBSESSION) > 0;
    }

    /** Register the counter-removal trigger for the attack event about to be dispatched. */
    public static void beforeAttackTrigger(final Card card) {
        if (!mustAttack(card)) {
            return;
        }
        final Trigger trigger = TriggerHandler.parseTrigger(
                "Mode$ Attacks | ValidCard$ Card.Self | TriggerDescription$ Obsession — remove an Obsession counter from CARDNAME.",
                card, false);
        trigger.setOverridingAbility(AbilityFactory.getAbility(
                "DB$ RemoveCounter | Defined$ Self | CounterType$ OBSESSION | CounterNum$ 1", card));
        card.getGame().getTriggerHandler().registerDelayedTrigger(trigger);
    }

    public static void beforeUntap(final Game game) {
        if (!game.getRules().isChaosBrawl()) {
            return;
        }
        final Player active = game.getPhaseHandler().getPlayerTurn();
        // Celestial Waters is an explicitly ordered Chaos Brawl turn action, before tolerance.
        for (final Card card : new CardCollection(active.getCardsIn(ZoneType.Battlefield))) {
            if (card.isPhasedOut() || !card.hasSVar("ChaosBrawlBeforeTolerance")) {
                continue;
            }
            final SpellAbility move = AbilityFactory.getAbility(card, card.getSVar("ChaosBrawlBeforeTolerance"));
            move.setActivatingPlayer(active);
            if (active.getController().confirmAction(move, null,
                    "Move counters with " + card.getName() + " before Drunken conversion and untapping?", null)
                    && active.getController().chooseTargetsFor(move)) {
                AbilityUtils.resolve(move);
            }
        }
        final GameEntityCounterTable counters = new GameEntityCounterTable();
        // Snapshot the battlefield so counter events cannot invalidate iteration.
        for (final Card card : new CardCollection(active.getCardsIn(ZoneType.Battlefield))) {
            if (!card.isCreature() || card.isPhasedOut()) {
                continue;
            }
            final int tolerance = card.hasSVar("ChaosBrawlTolerance")
                    ? Integer.parseInt(card.getSVar("ChaosBrawlTolerance")) : 2;
            final int excess = card.getCounters(CounterEnumType.DRUNKEN) - tolerance;
            if (excess > 0) {
                card.subtractCounter(CounterEnumType.DRUNKEN, excess, active);
                card.tap(true, null, active);
                card.addCounter(CounterEnumType.STUN, excess, active, counters);
            }
        }
        counters.replaceCounterEffect(game, null);
    }
}
