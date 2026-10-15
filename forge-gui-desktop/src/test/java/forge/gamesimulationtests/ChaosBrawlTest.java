package forge.gamesimulationtests;

import forge.ai.simulation.SimulationTest;
import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityUtils;
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
        Card card = addCardToZone(name, player, ZoneType.Stack);
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
        SpellAbility ability = beer.getSpellAbilities().get(0);
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
}
