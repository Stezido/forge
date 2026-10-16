package forge.game;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/** Rules for the isolated Chaos Brawl ten-card spike, enabled per match. */
public final class ChaosBrawlRules {
    private ChaosBrawlRules() { }

    public static void beforeUntap(final Game game) {
        if (!game.getRules().isChaosBrawl()) {
            return;
        }
        final Player active = game.getPhaseHandler().getPlayerTurn();
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
