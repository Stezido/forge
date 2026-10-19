package forge.ai;

import forge.game.ChaosBrawlRules;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Sorcery-speed Beer decisions, confined to the opt-in custom game. */
public final class ChaosBrawlBeerAi {
    private ChaosBrawlBeerAi() { }

    public static boolean applies(final SpellAbility sa) {
        final Card source = sa.getHostCard();
        return source.getGame().getRules().isChaosBrawl() && source.isToken()
                && source.getType().hasSubtype("Beer") && sa.isActivatedAbility()
                && sa.getApi() == ApiType.PutCounter && "DRUNKEN".equals(sa.getParam("CounterType"))
                && sa.hasParam("SorcerySpeed") && sa.usesTargeting() && sa.getMaxTargets() == 1;
    }

    public static AiAbilityDecision chooseTargets(final Player ai, final SpellAbility sa) {
        sa.resetTargets();
        final var phase = ai.getGame().getPhaseHandler();
        if (!phase.isPlayerTurn(ai) || !(phase.is(PhaseType.MAIN1) || phase.is(PhaseType.MAIN2))) {
            return no(AiPlayDecision.TimingRestrictions);
        }
        Card best = null;
        int bestScore = 0;
        final SpellAbility extra = sa.getSubAbility();
        final boolean expired = extra != null && extra.getApi() == ApiType.Pump
                && "-2".equals(extra.getParam("NumDef"));
        final boolean doubles = extra != null && extra.getApi() == ApiType.MultiplyCounter;
        int amount = 1;
        // Each controlled Draft Tap adds a counter before the Beer ability resolves.
        for (Card permanent : ai.getCardsIn(ZoneType.Battlefield)) {
            if ("Draft Tap".equals(permanent.getName())) { amount++; }
        }
        for (Card card : CardLists.getTargetableCards(ai.getGame().getCardsIn(ZoneType.Battlefield), sa)) {
            if (!card.isCreature() || !card.canReceiveCounters(CounterEnumType.DRUNKEN)) { continue; }
            final int before = card.getCounters(CounterEnumType.DRUNKEN);
            final int after = (before + amount) * (doubles ? 2 : 1);
            final int gain = after - before;
            final boolean own = card.getController() == ai;
            final int value = ComputerUtilCard.evaluateCreature(card);
            int score = 0;
            if (expired) {
                // The counter buffs first: an ordinary Expired Beer is only a net -1/-1.
                if (!own && card.getNetToughness() + gain - 2 <= card.getDamage()) {
                    score = 1000 + value;
                }
            } else if (own && after <= ChaosBrawlRules.drunkenTolerance(card)) {
                if (phase.is(PhaseType.MAIN2) || ComputerUtilCombat.canAttackNextTurn(card)
                        && !card.isTapped() && !card.isSick()) {
                    score = 100 + value + gain * 20;
                }
            }
            if (score > bestScore) { best = card; bestScore = score; }
        }
        if (best == null) { return no(AiPlayDecision.TargetingFailed); }
        if (phase.is(PhaseType.MAIN2) && !preserveInstantMana(ai, sa)) {
            return no(AiPlayDecision.CantAfford);
        }
        sa.getTargets().add(best);
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    /** Keep a payable, useful instant available; excess mana may still buy Beer. */
    private static boolean preserveInstantMana(final Player ai, final SpellAbility beer) {
        if (!(ai.getController() instanceof PlayerControllerAi controller)) { return true; }
        for (Card card : ai.getCardsIn(ZoneType.Hand)) {
            if (!card.isInstant()) { continue; }
            for (SpellAbility original : card.getSpellAbilities()) {
                if (!original.isSpell()) { continue; }
                final SpellAbility instant = original.copy(ai);
                // Optional effect evaluation asks whether it has a useful play without
                // rejecting an instant merely because the AI normally waits until their turn.
                final boolean counter = instant.getApi() == ApiType.Counter
                        && ai.getOpponents().stream().anyMatch(p -> !p.getCardsIn(ZoneType.Hand).isEmpty());
                if (!counter && (instant.getApi() == null || !SpellApiToAi.Converter.get(instant)
                        .doTriggerNoCostWithSubs(ai, instant, false).willingToPlay())) { continue; }
                if (!ComputerUtilCost.canPayCost(instant, ai, false)) { continue; }
                // Test the instant's real colored cost plus Beer's generic mana together.
                if (!ComputerUtilMana.canPayManaCost(instant, ai, 1, false)) { return false; }
                // Protect its colored sources when the generic Beer payment is selected.
                controller.getAi().reserveManaSourcesForNextSpell(instant, null);
                return ComputerUtilCost.canPayCost(beer, ai, false);
            }
        }
        return true;
    }

    private static AiAbilityDecision no(final AiPlayDecision reason) {
        return new AiAbilityDecision(0, reason);
    }
}
