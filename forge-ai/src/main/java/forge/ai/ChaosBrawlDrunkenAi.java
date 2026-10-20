package forge.ai;

import forge.game.ChaosBrawlRules;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Avoid optional Chaos Brawl triggers that would stun a friendly creature next turn. */
public final class ChaosBrawlDrunkenAi {
    private ChaosBrawlDrunkenAi() { }

    public static boolean applies(final SpellAbility sa) {
        return sa.getHostCard().getGame().getRules().isChaosBrawl()
                && sa.isOptionalTrigger() && sa.isTrigger()
                && sa.getApi() == ApiType.PutCounter
                && "DRUNKEN".equals(sa.getParam("CounterType"))
                && (sa.usesTargeting() && sa.getMaxTargets() == 1
                    || "Self".equals(sa.getParam("Defined")));
    }

    public static AiAbilityDecision chooseTarget(final Player ai, final SpellAbility sa) {
        sa.resetTargets();
        final int amount = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParamOrDefault("CounterNum", "1"), sa);
        if (!sa.usesTargeting() && "Self".equals(sa.getParam("Defined"))) {
            final Card source = sa.getHostCard();
            final boolean safe = source.isCreature() && source.getController() == ai
                    && source.canReceiveCounters(CounterEnumType.DRUNKEN)
                    && source.getCounters(CounterEnumType.DRUNKEN) + amount <= ChaosBrawlRules.drunkenTolerance(source);
            return new AiAbilityDecision(safe ? 100 : 0,
                    safe ? AiPlayDecision.WillPlay : AiPlayDecision.CantPlayAi);
        }
        Card best = null;
        int bestValue = Integer.MIN_VALUE;
        for (Card card : CardLists.getTargetableCards(ai.getGame().getCardsIn(ZoneType.Battlefield), sa)) {
            if (!card.isCreature() || card.getController() != ai
                    || !card.canReceiveCounters(CounterEnumType.DRUNKEN)
                    || card.getCounters(CounterEnumType.DRUNKEN) + amount > ChaosBrawlRules.drunkenTolerance(card)) {
                continue;
            }
            final int value = ComputerUtilCard.evaluateCreature(card);
            if (value > bestValue) {
                best = card;
                bestValue = value;
            }
        }
        if (best == null) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        sa.getTargets().add(best);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
}
