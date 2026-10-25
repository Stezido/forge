package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Optional, owner-aware target selection for the two Chaos Brawl Saga counter chapters. */
final class ChaosBrawlSagaCounterAi {
    private ChaosBrawlSagaCounterAi() { }

    static boolean applies(final SpellAbility sa) {
        final Card source = sa.getHostCard();
        if (!source.getGame().getRules().isChaosBrawl() || sa.getApi() != ApiType.PutCounter
                || !sa.usesTargeting() || sa.getMaxTargets() != 1) {
            return false;
        }
        final String counter = sa.getParam("CounterType");
        return ("Gastronomic Downfall".equals(source.getName()) && CounterEnumType.M1M1.name().equals(counter))
                || ("Frühshoppen".equals(source.getName()) && CounterEnumType.P1P1.name().equals(counter));
    }

    static AiAbilityDecision chooseTargets(final Player ai, final SpellAbility sa) {
        sa.resetTargets();
        final boolean negative = CounterEnumType.M1M1.name().equals(sa.getParam("CounterType"));
        final CounterEnumType type = negative ? CounterEnumType.M1M1 : CounterEnumType.P1P1;
        final CardCollection candidates = CardLists.getTargetableCards(
                ai.getGame().getCardsIn(ZoneType.Battlefield), sa);
        Card best = null;
        int bestValue = Integer.MIN_VALUE;
        for (Card card : candidates) {
            if (!card.isCreature() || !card.canReceiveCounters(type)) {
                continue;
            }
            if (negative != card.getController().isOpponentOf(ai)) {
                continue;
            }
            final int value = ComputerUtilCard.evaluateCreature(card);
            if (best == null || value > bestValue) {
                best = card;
                bestValue = value;
            }
        }
        if (best != null) {
            sa.getTargets().add(best);
        }
        if (sa.isTargetNumberValid()) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
    }
}
