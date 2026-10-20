package forge.ai;

import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Chaos Brawl targeting for Last Duel, which returns a creature for every player. */
public final class ChaosBrawlLastDuelAi {
    private ChaosBrawlLastDuelAi() { }

    public static boolean applies(final Player ai, final SpellAbility sa) {
        return ai.getGame().getRules().isChaosBrawl() && sa.isSpell()
                && "Last Duel".equals(sa.getHostCard().getName())
                && sa.getApi() == ApiType.ChangeZone && sa.usesTargeting()
                && sa.hasParam("TargetsForEachPlayer") && "True".equals(sa.getParam("TargetsForEachPlayer"))
                && "OneEachIfAble".equals(sa.getTargetRestrictions().getMaxTargets());
    }

    public static AiAbilityDecision chooseTargets(final Player ai, final SpellAbility sa) {
        sa.resetTargets();
        final CardCollection candidates = CardLists.getTargetableCards(ai.getGame().getCardsIn(ZoneType.Graveyard), sa);
        Card bestForAi = null;
        int bestAiValue = Integer.MIN_VALUE;
        final java.util.Map<Player, Card> selected = new java.util.LinkedHashMap<>();

        for (Player player : ai.getGame().getPlayers()) {
            Card bestForPlayer = null;
            int bestValue = player == ai ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            for (Card card : candidates) {
                if (card.getOwner() != player || !card.isCreature()
                        || ComputerUtil.isETBprevented(card) || ComputerUtilCard.wouldDieToStaticPT(card)) {
                    continue;
                }
                final int value = ComputerUtilCard.evaluateCreature(card);
                if (player == ai ? value > bestValue : value < bestValue) {
                    bestForPlayer = card;
                    bestValue = value;
                }
            }
            if (bestForPlayer != null) {
                selected.put(player, bestForPlayer);
                if (player == ai) {
                    bestForAi = bestForPlayer;
                    bestAiValue = bestValue;
                }
            }
        }

        // Without a favorable creature of its own, Last Duel mostly helps the other side.
        if (bestForAi == null) {
            return noPlay();
        }
        for (Player player : ai.getOpponents()) {
            final Card gift = selected.get(player);
            if (gift != null && bestAiValue <= ComputerUtilCard.evaluateCreature(gift)) {
                return noPlay();
            }
        }

        // OneEachIfAble requires the strongest own return and one legal return for each other player.
        for (Player player : ai.getGame().getPlayers()) {
            final Card choice = selected.get(player);
            if (choice != null && sa.canTarget(choice)) {
                sa.getTargets().add(choice);
            }
        }
        if (!sa.isTargetNumberValid()) {
            sa.resetTargets();
            return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    private static AiAbilityDecision noPlay() {
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }
}
