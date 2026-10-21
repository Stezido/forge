package forge.ai;

import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCopyService;
import forge.game.card.CardLists;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.staticability.StaticAbilityMode;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

/** Targeting heuristics for the opt-in Chaos Brawl forced-attack counter. */
public final class ChaosBrawlObsessionAi {
    private ChaosBrawlObsessionAi() { }

    public static boolean applies(final SpellAbility sa) {
        return sa.getHostCard().getGame().getRules().isChaosBrawl()
                && "OBSESSION".equals(sa.getParam("CounterType")) && sa.usesTargeting()
                && sa.getMaxTargets() == 1;
    }

    public static AiAbilityDecision chooseTargets(final Player ai, final SpellAbility sa, final boolean mandatory) {
        sa.resetTargets();
        final int amount = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParamOrDefault("CounterNum", "1"), sa);
        final CardCollection candidates = CardLists.getTargetableCards(ai.getGame().getCardsIn(ZoneType.Battlefield), sa);
        Card best = null;
        int bestScore = Integer.MIN_VALUE;
        for (Card card : candidates) {
            if (!card.isCreature() || !card.canReceiveCounters(CounterEnumType.OBSESSION)) {
                continue;
            }
            final int benefit = counterBuff(card, amount);
            final int value = ComputerUtilCard.evaluateCreature(card) / 20;
            final boolean enemy = card.getController().isOpponentOf(ai);
            final int score;
            if (enemy) {
                // Avoid buffing an enemy payoff; a first forced attack is more useful than stacking more.
                score = benefit > 0 ? -100 - benefit :
                        ComputerUtilCombat.canAttackNextTurn(card)
                                ? (card.getCounters(CounterEnumType.OBSESSION) == 0 ? 60 : 20) + value : 0;
            } else {
                score = benefit > 0 ? 100 + benefit : -60 - value;
            }
            if (score > bestScore) {
                best = card;
                bestScore = score;
            }
        }
        if (best != null && (bestScore > 0 || mandatory && sa.getMinTargets() > 0)) {
            sa.getTargets().add(best);
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        // An "up to one" trigger can choose zero; a required trigger must still obey its minimum.
        return new AiAbilityDecision(sa.isTargetNumberValid() ? 100 : 0,
                sa.isTargetNumberValid() ? AiPlayDecision.WillPlay : AiPlayDecision.TargetingFailed);
    }

    /** Recognize actual card abilities, rather than giving every friendly creature a presumed upside. */
    private static int counterBuff(final Card card, final int amount) {
        if (amount <= 0) {
            return 0;
        }
        int benefit = 0;
        final var phase = card.getGame().getPhaseHandler();
        // An until-end-of-turn pump cannot help in combat once combat is over.
        if (phase.getPhase().isBefore(PhaseType.COMBAT_END)) {
            for (Trigger trigger : card.getTriggers()) {
                if ((trigger.getMode() == TriggerType.CounterAdded || trigger.getMode() == TriggerType.CounterAddedOnce)
                        && "OBSESSION".equals(trigger.getParam("CounterType"))
                        && trigger.zonesCheck(card.getZone()) && trigger.requirementsCheck(card.getGame())
                        && trigger.matchesValidParam("ValidCard", card)) {
                    SpellAbility effect = trigger.ensureAbility();
                    if (effect != null && effect.getApi() == ApiType.Pump && "Self".equals(effect.getParam("Defined"))) {
                        benefit += Math.max(0, AbilityUtils.calculateAmount(card, effect.getParamOrDefault("NumAtt", "0"), effect))
                                + Math.max(0, AbilityUtils.calculateAmount(card, effect.getParamOrDefault("NumDef", "0"), effect));
                    }
                }
            }
        }
        // Check threshold buffs on a detached LKI copy; never add real counters while considering a target.
        final Card after = CardCopyService.getLKICopy(card);
        after.setCounters(CounterEnumType.OBSESSION, card.getCounters(CounterEnumType.OBSESSION) + amount);
        for (StaticAbility effect : card.getStaticAbilities()) {
            if (!"Self".equals(effect.getParamOrDefault("AffectedDefined", ""))
                    || !effect.checkConditions(StaticAbilityMode.Continuous)) {
                continue;
            }
            final int before = Math.max(0, AbilityUtils.calculateAmount(card, effect.getParamOrDefault("AddPower", "0"), effect))
                    + Math.max(0, AbilityUtils.calculateAmount(card, effect.getParamOrDefault("AddToughness", "0"), effect));
            final StaticAbility afterEffect = after.getStaticAbilities().stream()
                    .filter(candidate -> "Self".equals(candidate.getParamOrDefault("AffectedDefined", ""))
                            && candidate.checkConditions(StaticAbilityMode.Continuous))
                    .findFirst().orElse(null);
            if (afterEffect != null) {
                final int afterValue = Math.max(0, AbilityUtils.calculateAmount(after, afterEffect.getParamOrDefault("AddPower", "0"), afterEffect))
                        + Math.max(0, AbilityUtils.calculateAmount(after, afterEffect.getParamOrDefault("AddToughness", "0"), afterEffect));
                benefit += Math.max(0, afterValue - before);
            }
        }
        for (Card source : card.getGame().getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (StaticAbility effect : source.getStaticAbilities()) {
                if (!effect.getParamOrDefault("Affected", "").contains("OBSESSION")
                        || !effect.checkConditions(StaticAbilityMode.Continuous)
                        || effect.matchesValidParam("Affected", card) || !effect.matchesValidParam("Affected", after)) {
                    continue;
                }
                benefit += Math.max(0, AbilityUtils.calculateAmount(source, effect.getParamOrDefault("AddPower", "0"), effect))
                        + Math.max(0, AbilityUtils.calculateAmount(source, effect.getParamOrDefault("AddToughness", "0"), effect));
            }
        }
        return benefit;
    }
}
