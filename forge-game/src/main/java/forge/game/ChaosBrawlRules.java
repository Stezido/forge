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
    public static Card grantedPlotAllowance(final Player player) {
        if (!player.getGame().getRules().isChaosBrawl()) {
            return null;
        }
        for (Card effect : player.getCardsIn(forge.game.zone.ZoneType.Command)) {
            if (effect.hasSVar("ChaosBrawlPlotUsed")) {
                continue;
            }
            for (forge.game.staticability.StaticAbility ability : effect.getStaticAbilities()) {
                if (ability.hasParam("ChaosBrawlPlotAllowance")) {
                    return effect;
                }
            }
        }
        return null;
    }

    public static void consumeGrantedPlotAllowance(final Player player) {
        Card allowance = grantedPlotAllowance(player);
        if (allowance == null) {
            throw new IllegalStateException("No unused Plot allowance");
        }
        allowance.setSVar("ChaosBrawlPlotUsed", "1");
    }

    public static boolean isUpkeepCost(final SpellAbility ability) {
        return ability.getHostCard().getGame().getRules().isChaosBrawl()
                && (ability.hasParam("ChaosBrawlUpkeep") || ability.getRootAbility().hasParam("ChaosBrawlUpkeep"));
    }

    public static boolean isUpkeepWaived(final Card creature) {
        if (!creature.getGame().getRules().isChaosBrawl() || !creature.getType().hasSubtype("Siren")) {
            return false;
        }
        for (Card permanent : creature.getController().getCardsIn(ZoneType.Battlefield)) {
            for (forge.game.staticability.StaticAbility ability : permanent.getStaticAbilities()) {
                if (ability.hasParam("ChaosBrawlWaiveSirenUpkeep")
                        && ability.checkConditions(forge.game.staticability.StaticAbilityMode.Continuous)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static forge.game.cost.Cost adjustUpkeepCost(final forge.game.cost.Cost original,
            final SpellAbility ability, final boolean beforePayment) {
        if (!isUpkeepCost(ability) || !original.hasManaCost()) {
            return original;
        }
        int reductions = 0;
        Player payer = ability.getActivatingPlayer();
        for (Card permanent : payer.getCardsIn(ZoneType.Battlefield)) {
            for (forge.game.staticability.StaticAbility effect : permanent.getStaticAbilities()) {
                if (effect.hasParam("ChaosBrawlUpkeepReduction")
                        && effect.checkConditions(forge.game.staticability.StaticAbilityMode.Continuous)) {
                    reductions += Integer.parseInt(effect.getParam("ChaosBrawlUpkeepReduction"));
                }
            }
        }
        if (reductions == 0) {
            return original;
        }
        forge.game.cost.Cost result = original.copy();
        forge.game.mana.ManaCostBeingPaid mana = new forge.game.mana.ManaCostBeingPaid(original.getTotalMana());
        if (!beforePayment && ability.hasSVar("ChaosBrawlReducedMana")) {
            result.getCostParts().removeIf(p -> p instanceof forge.game.cost.CostPartMana);
            result.getCostParts().addAll(new forge.game.cost.Cost(ability.getSVar("ChaosBrawlReducedMana"), true).getCostParts());
            return result;
        }
        for (int n = 0; n < reductions && !mana.getUnpaidShards().isEmpty(); n++) {
            java.util.Map<String, forge.card.mana.ManaCostShard> options = new java.util.LinkedHashMap<>();
            for (forge.card.mana.ManaCostShard shard : mana.getUnpaidShards()) {
                options.put(shard.toString(), shard);
            }
            String selected = beforePayment && options.size() > 1
                    ? payer.getController().chooseSomeType("Mana symbol to reduce", ability, options.keySet())
                    : options.keySet().iterator().next();
            forge.card.mana.ManaCostShard shard = options.get(selected);
            if (shard == null) {
                shard = options.values().iterator().next();
            }
            mana.decreaseShard(shard, 1);
        }
        result.getCostParts().removeIf(p -> p instanceof forge.game.cost.CostPartMana);
        result.getCostParts().add(new forge.game.cost.CostPartMana(mana.toManaCost(), null));
        if (beforePayment) {
            ability.setSVar("ChaosBrawlReducedMana", mana.toManaCost().toString()
                    .replace("{", "").replace("}", " ").replace("/", "").trim());
        }
        return result;
    }

}
