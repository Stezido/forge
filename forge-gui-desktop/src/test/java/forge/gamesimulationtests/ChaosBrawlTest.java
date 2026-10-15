package forge.gamesimulationtests;

import forge.ai.simulation.SimulationTest;
import forge.ai.ComputerUtil;
import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.phase.Untap;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/** Exercise real cards, counter events, static effects and the actual untap step. */
public class ChaosBrawlTest extends SimulationTest {
    private Game customGame() {
        Game game = initAndCreateGame();
        game.getRules().setChaosBrawl(true);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, game.getPlayers().get(0));
        return game;
    }

    private void counters(Card card, CounterEnumType type, int amount) {
        GameEntityCounterTable table = new GameEntityCounterTable();
        card.addCounter(type, amount, card.getController(), table);
        table.replaceCounterEffect(card.getGame(), null);
    }

    private void untap(Game game, Player active) {
        game.getPhaseHandler().devModeSet(PhaseType.UNTAP, active);
        new Untap(game).executeAt();
    }

    private SpellAbility spell(String name, Player player) {
        Card card = createCard(name, player);
        player.getGame().getStackZone().add(card);
        SpellAbility sa = card.getFirstSpellAbility();
        sa.setActivatingPlayer(player);
        return sa;
    }

    @Test
    public void drunkenIsSeparateAndDisabledForOrdinaryMatches() {
        Game game = initAndCreateGame();
        game.getRules().setChaosBrawl(false);
        Player p = game.getPlayers().get(0);
        Card card = addCard("Couchpotato", p);
        counters(card, CounterEnumType.DRUNKEN, 4);
        counters(card, CounterEnumType.P1P1, 1);
        assertEquals(card.getNetPower(), 1);
        assertEquals(card.getNetToughness(), 5);
        untap(game, p);
        assertEquals(card.getCounters(CounterEnumType.DRUNKEN), 4);
        assertEquals(card.getCounters(CounterEnumType.STUN), 0);
        game.getRules().setChaosBrawl(true);
        assertEquals(card.getNetPower(), 5);
        assertEquals(card.getNetToughness(), 9);
    }

    @Test
    public void defaultToleranceRunsBeforeNormalUntapOnlyForActivePlayer() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        Player opponent = game.getPlayers().get(1);
        Card card = addCard("Couchpotato", p);
        Card other = addCard("Couchpotato", opponent);
        counters(card, CounterEnumType.DRUNKEN, 4);
        counters(other, CounterEnumType.DRUNKEN, 4);
        untap(game, p);
        assertEquals(card.getCounters(CounterEnumType.DRUNKEN), 2);
        assertEquals(card.getCounters(CounterEnumType.STUN), 1);
        assertTrue(card.isTapped());
        assertEquals(card.getNetPower(), 2);
        assertEquals(other.getCounters(CounterEnumType.DRUNKEN), 4);
        assertEquals(other.getCounters(CounterEnumType.STUN), 0);
        untap(game, p);
        assertEquals(card.getCounters(CounterEnumType.STUN), 0);
        assertTrue(card.isTapped());
        untap(game, p);
        assertFalse(card.isTapped());
    }

    @Test
    public void atToleranceUntapsAndAlreadyTappedStillGetsExcessStun() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        Card at = addCard("Couchpotato", p);
        Card over = addCard("Couchpotato", p);
        at.tap(true, null, p);
        over.tap(true, null, p);
        counters(at, CounterEnumType.DRUNKEN, 2);
        counters(over, CounterEnumType.DRUNKEN, 3);
        untap(game, p);
        assertFalse(at.isTapped());
        assertTrue(over.isTapped());
        assertEquals(over.getCounters(CounterEnumType.DRUNKEN), 2);
        assertEquals(over.getCounters(CounterEnumType.STUN), 0);
    }

    @Test
    public void drunkardEntersWithThreeAndUsesPrintedTolerance() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        Card card = addCardToZone("Drunkard", p, ZoneType.Hand);
        card = game.getAction().moveTo(ZoneType.Battlefield, card, null, null);
        assertEquals(card.getCounters(CounterEnumType.DRUNKEN), 3);
        assertTrue(card.hasKeyword("Haste"));
        untap(game, p);
        assertEquals(card.getCounters(CounterEnumType.DRUNKEN), 1);
        assertEquals(card.getCounters(CounterEnumType.STUN), 1);
        assertTrue(card.isTapped());
    }

    @Test
    public void toastAffectsBothSidesAndRamBuffTracksLastCounter() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        Player opponent = game.getPlayers().get(1);
        Card own = addCard("Couchpotato", p);
        Card other = addCard("Couchpotato", opponent);
        addCard("The Tipsy Ram", p);
        AbilityUtils.resolve(spell("Toast of Comfort", p));
        game.getAction().checkStaticAbilities();
        assertEquals(own.getNetPower(), 2);
        assertEquals(other.getNetPower(), 1);
        assertTrue(own.hasKeyword("Vigilance"));
        assertFalse(other.hasKeyword("Vigilance"));
        own.subtractCounter(CounterEnumType.DRUNKEN, 1, p);
        game.getAction().checkStaticAbilities();
        assertEquals(own.getNetPower(), 0);
        assertFalse(own.hasKeyword("Vigilance"));
    }

    @Test
    public void buyRoundCreatesTwoBeerTokensAndBeerAddsDrunken() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        Card creature = addCard("Couchpotato", p);
        AbilityUtils.resolve(spell("Buy a Round", p));
        assertEquals(countCardsWithName(game, "Beer"), 2);
        Card beer = findCardWithName(game, "Beer");
        assertTrue(beer.getType().hasSubtype("Beer"));
        SpellAbility ability = beer.getSpellAbilities().stream().filter(sa -> sa.getApi() == ApiType.PutCounter).findFirst().orElseThrow();
        ability.setActivatingPlayer(p);
        ability.getTargets().add(creature);
        AbilityUtils.resolve(ability);
        assertEquals(creature.getCounters(CounterEnumType.DRUNKEN), 1);
        assertEquals(creature.getNetPower(), 1);
    }

    @Test
    public void wastedDestroysSoberAndExilesDrunkenEvenWhenIndestructible() {
        for (boolean drunken : new boolean[] {false, true}) {
            Game game = customGame();
            Player p = game.getPlayers().get(0);
            Card target = addCard(drunken ? "Darksteel Myr" : "Couchpotato", game.getPlayers().get(1));
            if (drunken) {
                counters(target, CounterEnumType.DRUNKEN, 1);
            }
            SpellAbility ability = spell("Wasted", p);
            ability.getTargets().add(target);
            AbilityUtils.resolve(ability);
            assertTrue(target.isInZone(drunken ? ZoneType.Exile : ZoneType.Graveyard));
        }
    }

    @Test
    public void nutshotTapsAndAddsThreeStun() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        Card card = addCard("Couchpotato", p);
        SpellAbility ability = spell("Nutshot overload", p);
        ability.getTargets().add(card);
        AbilityUtils.resolve(ability);
        assertTrue(card.isTapped());
        assertEquals(card.getCounters(CounterEnumType.STUN), 3);
        untap(game, p);
        assertEquals(card.getCounters(CounterEnumType.STUN), 2);
        assertTrue(card.isTapped());
    }

    @Test
    public void foodCreaturesPayManaTapAndSacrificeAndPuddingTriggers() {
        for (String name : new String[] {"Couchpotato", "Wixbirne", "Puddinghead"}) {
            Game game = customGame();
            Player p = game.getPlayers().get(0);
            p.setLife(10, null);
            game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
            Card food = addCard(name, p);
            food.setSickness(false);
            addCards("Swamp", 2, p);
            game.getAction().checkStateEffects(true);
            SpellAbility ability = food.getSpellAbilities().stream()
                    .filter(sa -> sa.getApi() == ApiType.GainLife).findFirst().orElseThrow();
            ability.setActivatingPlayer(p);
            assertTrue(ComputerUtil.playNoStack(p, ability, game, false));
            assertTrue(food.isInZone(ZoneType.Graveyard));
            assertEquals(p.getLife(), 13);
            assertEquals(p.getCardsIn(ZoneType.Battlefield).stream()
                    .filter(c -> c.isLand() && c.isTapped()).count(), 2L);
            if (name.equals("Puddinghead")) {
                game.getTriggerHandler().runWaitingTriggers();
                game.getStack().addAllTriggeredAbilitiesToStack();
                assertFalse(game.getStack().isEmpty());
                game.getStack().resolveStack();
                assertEquals(p.getLife(), 10);
                assertTrue(food.hasKeyword("Defender"));
            }
        }
    }

    @Test
    public void beerCostsOneManaTapSacrificeAndIsSorcerySpeed() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        Card target = addCard("Couchpotato", p);
        Card land = addCard("Swamp", p);
        AbilityUtils.resolve(spell("Buy a Round", p));
        Card beer = findCardWithName(game, "Beer");
        SpellAbility ability = beer.getSpellAbilities().stream()
                .filter(sa -> sa.getApi() == ApiType.PutCounter).findFirst().orElseThrow();
        ability.setActivatingPlayer(p);
        ability.getTargets().add(target);
        assertTrue(ability.canPlay());
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_BEGIN, p);
        assertFalse(ability.canPlay());
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        assertTrue(ComputerUtil.playNoStack(p, ability, game, false));
        assertTrue(beer.isInZone(ZoneType.Graveyard));
        assertTrue(land.isTapped());
        assertEquals(target.getCounters(CounterEnumType.DRUNKEN), 1);
        assertEquals(countCardsWithName(game, "Beer"), 1);
    }

    @Test
    public void wivesArgumentRejectsCreaturesAndCountersUnlessTwoManaPaid() {
        for (boolean pay : new boolean[] {false, true}) {
            Game game = customGame();
            Player p = game.getPlayers().get(0);
            Player opponent = game.getPlayers().get(1);
            SpellAbility target = spell("Lightning Bolt", opponent);
            target.getTargets().add(p);
            game.getStack().add(target);
            if (pay) {
                addCards("Island", 2, opponent);
            }
            SpellAbility counter = spell("A Wife's Argument", p);
            assertTrue(counter.canTarget(target));
            assertFalse(counter.canTarget(spell("Couchpotato", opponent)));
            counter.getTargets().add(target);
            AbilityUtils.resolve(counter);
            assertEquals(game.getStack().isEmpty(), !pay);
            assertTrue(target.getHostCard().isInZone(pay ? ZoneType.Stack : ZoneType.Graveyard));
        }
    }

    @Test
    public void wixbirneEntryProducesDiscardOrLifePayment() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        Player opponent = game.getPlayers().get(1);
        opponent.setLife(3, null);
        Card inHand = addCardToZone("Island", opponent, ZoneType.Hand);
        Card wixbirne = addCardToZone("Wixbirne", p, ZoneType.Hand);
        game.getAction().checkStateEffects(true);
        wixbirne = game.getAction().moveTo(ZoneType.Battlefield, wixbirne, null, null);
        game.getTriggerHandler().runWaitingTriggers();
        game.getStack().addAllTriggeredAbilitiesToStack();
        assertFalse(game.getStack().isEmpty(), "ETB trigger missing; opponents=" + p.getOpponents().size() + "; triggers=" + wixbirne.getTriggers().size() + "; hand=" + opponent.getCardsIn(ZoneType.Hand).size());
        game.getStack().resolveStack();
        assertEquals(opponent.getCardsIn(ZoneType.Hand).size(), 0);
        assertTrue(inHand.isInZone(ZoneType.Graveyard));
        assertEquals(opponent.getLife(), 3);
    }
}
