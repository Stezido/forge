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
import org.testng.annotations.DataProvider;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.trigger.TriggerType;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.player.PlayerController;
import forge.game.ChaosBrawlRules;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

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
    public void nutshotTapsAndAddsTwoStun() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        Card card = addCard("Couchpotato", p);
        SpellAbility ability = spell("Nutshot overload", p);
        ability.getTargets().add(card);
        AbilityUtils.resolve(ability);
        assertTrue(card.isTapped());
        assertEquals(card.getCounters(CounterEnumType.STUN), 2);
        untap(game, p);
        assertEquals(card.getCounters(CounterEnumType.STUN), 1);
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

    @DataProvider(name = "betaCards")
    public Object[][] betaCards() {
        return new Object[][] {
            {"Wasted", 3, "", "", "Destroy target creature. If that creature had one or more drunk counters on it, exile it instead."},
            {"Nutten und Koks", 3, "", "", "At the beginning of your upkeep, you lose 2 life. Then choose one \u2014\nCreate a Treasure token.\nDraw a card."},
            {"Wixbirne", 4, "2", "2", "When Wixbirne enters the battlefield, target opponent discards a card unless they pay 3 life.\n{2}, {T}, Sacrifice Wixbirne: You gain 3 life."},
            {"Couchpotato", 2, "0", "4", "{2}, {T}, Sacrifice Couchpotato: You gain 3 life."},
            {"Puddinghead", 2, "2", "4", "Defender\n{2}, {T}, Sacrifice Puddinghead: You gain 3 life.\nWhen Puddinghead dies, you lose 3 life."},
            {"Buy a Round", 2, "", "", "Create two Beer Token."},
            {"Food Coma", 4, "", "", "When Food Coma enters, exile target creature an opponent controls until Food Coma leaves the battlefield. Create a Food token."},
            {"Last Man Standing", 4, "", "", "Choose a creature at random. Destroy all other creatures."},
            {"The Tipsy Ram", 2, "", "", "Drunken creatures you control get +1/+1 and have vigilance."},
            {"Toast of Comfort", 1, "", "", "Each creature gains a Drunken counter."},
            {"A Wife's Argument", 1, "", "", "Counter target noncreature spell unless its controller pays {2}."},
            {"Charming Siren", 3, "2", "3", "Flash\nWhen Charming Siren enters the battlefield, obsess target creature an opponent controls."},
            {"Circular Conversation", 4, "", "", "Flash\nEnchant creature\nEnchanted creature doesn\u2019t untap during its controller\u2019s untap step."},
            {"Enraged Reef Leviathan", 5, "4", "4", "Enraged Reef Leviathan gets +3/+3 as long as there are three or more Sirens in your graveyard."},
            {"Mom's advice", 4, "", "", "Choose one \u2014\n\u2022 Counter target spell.\n\u2022 Surveil 2, then draw two cards."},
            {"Nagging Siren Twin", 3, "2", "1", "When Nagging Siren Twin enters the battlefield, create a 1/1 blue Siren creature with flying that is also named Nagging Siren Twin."},
            {"Nutshot overload", 2, "", "", "Tap target creature and put two stun counters on it."},
            {"Ambush on the Throne", 2, "", "", "Target creature gets -1/-1 until end of turn. It gets an additional -1/-1 until end of turn for each creature you control."},
            {"Overdose", 4, "", "", "Until end of turn, you may cast spells from your graveyard, but you lose life equal to their mana value.\nAt the end of the turn, if you cast three or more spells this way, you lose the game."},
            {"Drunkard", 2, "0", "0", "Haste, Drunken Tolerance 1\nDrunkard enters the battlefield with three Drunken counters on it."},
            {"Drunken Warrior", 2, "1", "1", "Haste\nDrunken Warrior enters the battlefield with three Drunken counter on it.\nDrunken counters cannot be removed from Drunken Warrior."},
            {"Hopsfield Vale", 0, "", "", "({T}: Add {G} or {W} to your mana pool.)\nAs Hopsfield Vale enters the battlefield, you may pay 2 life. If you don\u2019t, Hopsfield Vale enters the battlefield tapped."},
            {"Kegbearer", 6, "6", "6", "When Kegbearer, the Barrelback enters the battlefield, create two Beer token.\nDrunken Tolerance 4"},
            {"Blunt Force Trauma", 2, "", "", "Target creature you control fights target creature you don\u2019t control. If a creature would survive this fight, tap it. It doesn\u2019t untap during its controller\u2019s next untap step."},
            {"Gimme the herbs", 1, "", "", "Reveal the top four cards of your library. Put all forest cards revealed this way into your hand and the rest into your graveyard."},
            {"Hallucinating Trip", 3, "", "", "Until end of turn, all Forests you control become 1/1 green Plant creatures with haste in addition to their other types. They\u2019re still lands."},
            {"Inspiring Bard", 4, "3", "3", "When Inspiring Bard enters, the battlefield, choose one \u2014\n\u2022 Bardic Inspiration \u2014 Target creature gets +2/+2 until end of turn.\n\u2022 Song of Rest \u2014 You gain 3 life."},
            {"Mystic Herb Buddy", 4, "4", "4", "If you control four or more Forests, Mystic Herb Buddy has Reach and Trample."},
            {"Mystic Herb Seedling", 2, "1", "1", "When Mystic Herb Seedling enters the battlefield, mill three cards. You may put a land card from among the cards milled this way into your hand. If you don\u2019t, put a +1/+1 counter on Mystic Herb Seedling."},
            {"Mystic Hotbox Hut", 3, "", "", "Whenever you produce mana from a non-land source, put a Haze counter on Mystic Hotbox Hut.\n{T}: Add {G}.\n{T}, Remove three Haze counters: Create a 2/2 green Elemental creature token."},
            {"Brewmaster Grunthar", 3, "3", "4", "Drunken Tolerance 4\nWhen Brewmaster Grunthar enters the battlefield, create a Beer Token"},
            {"Br\u00fcder Hopfenbl\u00fcte", 4, "2", "5", "At the beginning of each upkeep, roll a six-sided die. Create a Beer token with the result indicated below.\n1 \u2014 Expired Beer\n2 \u2014 Zwickl Beer\n3 \u2014 Wheat Beer\n4 \u2014 Dark Beer\n5 \u2014 Pils Beer\n6 \u2014 Bock Beer\nDrunken tolerance 6"},
            {"Ice Cold Siren", 3, "2", "3", "Whenever an opponent discards a card, draw a card.\n{1UB},{T}: Target opponent discards a card. Activate this ability only once."},
            {"Wake and Bake", 3, "", "", "Draw a card, then you may put a land card from your hand onto the battlefield tapped. Create a Food token."},
            {"Rotting Flesh", 2, "2", "2", "When Rotting Flesh dies, you lose 2 life.\n{2},{T}, Sacrifice Rotting Flesh: You gain 3 life."},
            {"Decaying Orchard", 0, "", "", "({T}: Add {B} or {G} to your mana pool.)\nAs Decaying Orchard enters the battlefield, you may pay 2 life. If you don\u2019t, Decaying Orchard enters the battlefield tapped."},
            {"Distillery of Silent Death", 0, "", "", "({T}: Add {W} or {B} to your mana pool.)\nAs Distillery of Silent Death enters the battlefield, you may pay 2 life. If you don\u2019t, Distillery of Silent Death enters the battlefield tapped."},
            {"Feuerball, Junge!", 10, "", "", "As an additional cost to cast this spell, you may tap any number of untapped creatures you control. This spell costs {1} less to cast for each creature tapped this way.\nExplosive Singularity deals 10 damage to any target."},
            {"Sacred Brewery", 0, "", "", "({T}: Add {R} or {W} to your mana pool.)\nAs Sacred Brewery enters the battlefield, you may pay 2 life. If you don\u2019t, Sacred Brewery enters the battlefield tapped."},
            {"Parsley Root", 1, "", "", "When Parsley Root enters the battlefield, you gain 1 life.\nEquipped creature gets +1/+0.\nEquip {1}"},
            {"Celestial Waters", 3, "", "", "When Celestial Waters enters, tap each creature and put a stun counter on each of them.\nAt the beginning of your turn, you may move any number of counters of your choice from one target creature onto another target creature."},
            {"Gluttonous Scavenger", 5, "4", "5", "Whenever you sacrifice a Food, put a +1/+1 counter on Gluttonous Scavenger.\n{B/G}, Exile two cards from your graveyard: Create a Food token."},
            {"Brie-lliant Trapper", 3, "3", "3", "Whenever Brie-lliant Trapper enters the battlefield, create a Food token.\n{G}, Sacrifice a Food: Put a +1/+1 counter on target creature. Activate only as a sorcery.\n{B}, Sacrifice a Food: Target creature gets -1/-1 until end of turn. Activate only as a sorcery."},
            {"Chaotic Kitchen", 3, "", "", "When Chaotic Kitchen enters the battlefield, create a Food token.\nFood tokens you control are 2/2 creatures with \u201c{2}, {T}, Sacrifice this artifact: You gain 3 life\u201d in addition to their other types."},
            {"Double Burn", 2, "", "", "Double Burn deals 2 damage to any target.\nFlashback\u2014{3R} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"},
            {"Pumpkin Barrage Crew", 4, "3", "3", "When Pumpkin Barrage Crew enters the battlefield, create a Food token.\n{2}, Sacrifice a Food token: Pumpkin Barrage Crew deals 2 damage to target creature."},
            {"Mattn, the Tasty Prince", 5, "4", "5", "At the beginning of each upkeep, create a Food token.\nWhenever you sacrifice a Food token, put a +1/+1 counter on Mattn, the Tasty Prince."},
            {"Anschnly, the Vengeful Siren", 5, "4", "3", "Flying\nWhenever you cast a spell with mana value 2 or less, draw a card.\nWhenever you cast a spell with mana value 3 or greater, Anschnly, the Vengeful Siren deals 2 damage to target creature."},
            {"Noxious Cloud", 4, "", "", "All creatures get -4/-4 until end of turn."},
            {"Mistcliff Springs", 0, "", "", "({T}: Add {U} or {R} to your mana pool.)\nAs Mistcliff Springs enters the battlefield, you may pay 2 life. If you don\u2019t, Mistcliff Springs enters the battlefield tapped."}
        };
    }

    @Test(dataProvider = "betaCards")
    public void loadedCardMatchesReviewedMseSource(String name, int mv, String power, String toughness, String oracle) {
        Game game = customGame();
        Card card = createCard(name, game.getPlayers().get(0));
        assertEquals(card.getManaCost().getCMC(), mv, name);
        String displayed = card.getRules().getOracleText().replace("\\n", "\n").replace("\r\n", "\n");
        // Forge reuses two mechanically identical upstream cards with the same names.
        if (name.equals("Food Coma")) {
            displayed = displayed.replace(" (It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")", "");
        }
        if (name.equals("Inspiring Bard")) {
            oracle = oracle.replace("enters, the battlefield,", "enters,");
        }
        assertEquals(displayed, oracle, name);
        if (!power.isEmpty()) {
            assertEquals(card.getBasePower(), Integer.parseInt(power), name);
            assertEquals(card.getBaseToughness(), Integer.parseInt(toughness), name);
        }
    }

    private SpellAbility db(Card host, String name) {
        SpellAbility sa = AbilityFactory.getAbility(host, name);
        sa.setActivatingPlayer(host.getController());
        return sa;
    }

    private void resolveTriggers(Game game) {
        for (int n = 0; n < 20; n++) {
            game.getTriggerHandler().runWaitingTriggers();
            game.getStack().addAllTriggeredAbilitiesToStack();
            if (game.getStack().isEmpty()) {
                return;
            }
            game.getStack().resolveStack();
        }
        fail("Unexpected trigger loop");
    }

    private Card enter(String name, Player player) {
        Card card = addCardToZone(name, player, ZoneType.Hand);
        return player.getGame().getAction().moveTo(ZoneType.Battlefield, card, null, null);
    }

    @Test
    public void randomBoardWipeKeepsExactlyOneCreature() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        addCard("Couchpotato", p);
        addCard("Wixbirne", p);
        addCard("Puddinghead", game.getPlayers().get(1));
        AbilityUtils.resolve(spell("Last Man Standing", p));
        assertEquals(game.getCardsIn(ZoneType.Battlefield).stream().filter(Card::isCreature).count(), 1L);
    }

    @Test
    public void foodComaExilesOpponentUntilItLeavesAndCreatesFood() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        Card victim = addCard("Couchpotato", game.getPlayers().get(1));
        Card coma = enter("Food Coma", p);
        resolveTriggers(game);
        assertTrue(victim.isInZone(ZoneType.Exile));
        assertEquals(p.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getType().hasSubtype("Food")).count(), 1L);
        game.getAction().moveTo(ZoneType.Graveyard, coma, null, null);
        resolveTriggers(game);
        assertEquals(game.getPlayers().get(1).getCardsIn(ZoneType.Battlefield).size(), 1);
    }

    @Test
    public void obsessionRequiresAnAttackAndRemovesOneCounterOnTheStack() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        Card attacker = addCard("Couchpotato", p);
        attacker.setSickness(false);
        counters(attacker, CounterEnumType.OBSESSION, 2);
        assertTrue(ChaosBrawlRules.mustAttack(attacker));
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, p);
        Combat combat = new Combat(p);
        game.getPhaseHandler().setCombat(combat);
        combat.addAttacker(attacker, game.getPlayers().get(1));
        CombatUtil.checkDeclaredAttacker(game, attacker, combat, true);
        assertEquals(attacker.getCounters(CounterEnumType.OBSESSION), 2);
        resolveTriggers(game);
        assertEquals(attacker.getCounters(CounterEnumType.OBSESSION), 1);
        game.getRules().setChaosBrawl(false);
        assertFalse(ChaosBrawlRules.mustAttack(attacker));
    }

    @Test
    public void drunkenWarriorCannotLoseDrunkenButCanLoseOtherCounters() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        Card warrior = enter("Drunken Warrior", p);
        assertEquals(warrior.getCounters(CounterEnumType.DRUNKEN), 3);
        assertEquals(warrior.subtractCounter(CounterEnumType.DRUNKEN, 1, p), 0);
        counters(warrior, CounterEnumType.P1P1, 1);
        assertEquals(warrior.subtractCounter(CounterEnumType.P1P1, 1, p), 1);
        untap(game, p);
        assertEquals(warrior.getCounters(CounterEnumType.DRUNKEN), 3);
        assertTrue(warrior.isTapped());
    }

    @Test
    public void forestRevealSelectsAllForestCardsAndMillsTheRest() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        for (String name : new String[] {"Forest", "Hopsfield Vale", "Swamp", "Wixbirne"}) {
            addCardToZone(name, p, ZoneType.Library);
        }
        AbilityUtils.resolve(spell("Gimme the herbs", p));
        assertEquals(p.getCardsIn(ZoneType.Hand).size(), 2);
        assertTrue(p.getCardsIn(ZoneType.Hand).stream().allMatch(c -> c.getType().hasSubtype("Forest")));
        assertEquals(p.getCardsIn(ZoneType.Graveyard).size(), 2);
    }

    @Test
    public void forestsBecomePlantsButOpponentsForestsAndOwnSwampsDoNot() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        Card own = addCard("Forest", p);
        Card swamp = addCard("Swamp", p);
        Card other = addCard("Forest", game.getPlayers().get(1));
        AbilityUtils.resolve(spell("Hallucinating Trip", p));
        game.getAction().checkStaticAbilities();
        assertTrue(own.isCreature());
        assertTrue(own.isLand());
        assertEquals(own.getNetPower(), 1);
        assertTrue(own.hasKeyword("Haste"));
        assertFalse(swamp.isCreature());
        assertFalse(other.isCreature());
    }

    @Test
    public void forestThresholdAndGraveyardSirenThresholdAreContinuous() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        Card buddy = addCard("Mystic Herb Buddy", p);
        Card leviathan = addCard("Enraged Reef Leviathan", p);
        addCards("Forest", 3, p);
        addCardToZone("Charming Siren", p, ZoneType.Graveyard);
        addCardToZone("Ice Cold Siren", p, ZoneType.Graveyard);
        game.getAction().checkStaticAbilities();
        assertFalse(buddy.hasKeyword("Reach"));
        assertEquals(leviathan.getNetPower(), 4);
        addCard("Forest", p);
        addCardToZone("Nagging Siren Twin", p, ZoneType.Graveyard);
        game.getAction().checkStaticAbilities();
        assertTrue(buddy.hasKeyword("Reach"));
        assertTrue(buddy.hasKeyword("Trample"));
        assertEquals(leviathan.getNetPower(), 7);
    }

    @Test
    public void seedlingMillsAndGrowsWhenThereIsNoLandToReturn() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        for (int n = 0; n < 3; n++) {
            addCardToZone("Couchpotato", p, ZoneType.Library);
        }
        Card seedling = enter("Mystic Herb Seedling", p);
        resolveTriggers(game);
        assertEquals(p.getCardsIn(ZoneType.Graveyard).size(), 3);
        assertEquals(seedling.getCounters(CounterEnumType.P1P1), 1);
    }

    @Test
    public void chaoticKitchenAnimatesOnlyOwnFoodTokensAndRequiresTapToSacrifice() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        p.setLife(10, null);
        Card couch = addCard("Couchpotato", p);
        enter("Chaotic Kitchen", p);
        resolveTriggers(game);
        Card food = p.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.isToken() && c.getType().hasSubtype("Food")).findFirst().orElseThrow();
        assertEquals(p.getCardsIn(ZoneType.Battlefield).stream().filter(Card::isToken).count(), 1L);
        game.getAction().checkStaticAbilities();
        assertTrue(food.isCreature());
        assertTrue(food.isArtifact());
        assertEquals(food.getNetPower(), 2);
        assertEquals(food.getNetToughness(), 2);
        assertEquals(couch.getNetPower(), 0);
        assertFalse(couch.isArtifact());
        SpellAbility gain = food.getSpellAbilities().stream().filter(sa -> sa.getApi() == ApiType.GainLife).findFirst().orElseThrow();
        gain.setActivatingPlayer(p);
        assertFalse(gain.canPlay());
        food.setSickness(false);
        addCards("Swamp", 2, p);
        assertTrue(ComputerUtil.playNoStack(p, gain, game, false));
        assertEquals(p.getLife(), 13);
        assertTrue(food.isInZone(ZoneType.Graveyard));
    }

    @Test
    public void pumpkinAbilityTargetsCreaturesButNotPlayers() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        Card pumpkin = addCard("Pumpkin Barrage Crew", p);
        Card target = addCard("Couchpotato", game.getPlayers().get(1));
        SpellAbility damage = pumpkin.getSpellAbilities().stream().filter(sa -> sa.getApi() == ApiType.DealDamage).findFirst().orElseThrow();
        damage.setActivatingPlayer(p);
        assertTrue(damage.canTarget(target));
        assertFalse(damage.canTarget(game.getPlayers().get(1)));
        damage.getTargets().add(target);
        AbilityUtils.resolve(damage);
        assertEquals(target.getDamage(), 2);
    }

    @Test
    public void noxiousCloudAffectsBothSidesAndBrieFoodIsNotAnArtifact() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        Card trapper = addCard("Brie-lliant Trapper", p);
        Card opposing = addCard("Gluttonous Scavenger", game.getPlayers().get(1));
        assertTrue(trapper.getType().hasSubtype("Food"));
        assertFalse(trapper.isArtifact());
        assertFalse(opposing.getType().hasSubtype("Food"));
        AbilityUtils.resolve(spell("Noxious Cloud", p));
        assertEquals(trapper.getNetPower(), -1);
        assertEquals(opposing.getNetToughness(), 1);
    }

    @Test
    public void celestialWatersMovesCountersBeforeToleranceAndUntap() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        addCard("Celestial Waters", p);
        Card source = addCard("Couchpotato", p);
        Card target = addCard("Couchpotato", p);
        source.tap(true, null, p);
        counters(source, CounterEnumType.DRUNKEN, 4);
        counters(source, CounterEnumType.STUN, 1);
        PlayerController choices = spy(p.getController());
        doReturn(true).when(choices).confirmAction(any(), any(), anyString(), isNull());
        doAnswer(invocation -> {
            SpellAbility move = invocation.getArgument(0);
            move.getTargets().add(source);
            move.getTargets().add(target);
            return true;
        }).when(choices).chooseTargetsFor(any());
        doAnswer(invocation -> ((java.util.List<forge.game.card.CounterType>) invocation.getArgument(0)).get(0))
                .when(choices).chooseCounterType(anyList(), any(), anyString(), anyMap());
        doAnswer(invocation -> invocation.getArgument(3)).when(choices)
                .chooseNumber(any(), anyString(), anyInt(), anyInt(), anyMap());
        p.dangerouslySetController(choices);
        untap(game, p);
        assertEquals(source.getCounters(CounterEnumType.DRUNKEN), 0);
        assertEquals(source.getCounters(CounterEnumType.STUN), 0);
        assertFalse(source.isTapped());
        assertEquals(target.getCounters(CounterEnumType.DRUNKEN), 2);
        assertEquals(target.getCounters(CounterEnumType.STUN), 2);
        assertTrue(target.isTapped());
    }

    @Test
    public void allSpecialBeerTokensPayCostsAndApplyTheirPrintedBonus() {
        for (String variant : new String[] {"Expired", "Zwickl", "Wheat", "Dark", "Pils", "Bock"}) {
            Game game = customGame();
            Player p = game.getPlayers().get(0);
            Card brewer = addCard("Brüder Hopfenblüte", p);
            Card creature = addCard("Couchpotato", p);
            addCard("Swamp", p);
            counters(creature, CounterEnumType.DRUNKEN, 2);
            AbilityUtils.resolve(db(brewer, variant));
            Card beer = findCardWithName(game, variant + " Beer");
            assertNotNull(beer, variant);
            SpellAbility ability = beer.getSpellAbilities().stream().filter(sa -> sa.getApi() == ApiType.PutCounter).findFirst().orElseThrow();
            ability.setActivatingPlayer(p);
            ability.getTargets().add(creature);
            assertTrue(ComputerUtil.playNoStack(p, ability, game, false), variant);
            assertTrue(beer.isInZone(ZoneType.Graveyard), variant);
            assertEquals(creature.getCounters(CounterEnumType.DRUNKEN), variant.equals("Bock") ? 6 : 3, variant);
            assertEquals(creature.getNetPower(), variant.equals("Bock") ? 6 : variant.equals("Expired") ? 1 : 3, variant);
            String keyword = switch (variant) {
                case "Zwickl" -> "Haste";
                case "Wheat" -> "Vigilance";
                case "Dark" -> "Menace";
                case "Pils" -> "First strike";
                default -> "";
            };
            if (!keyword.isEmpty()) {
                assertTrue(creature.hasKeyword(keyword), variant);
            }
        }
    }

    @Test
    public void brewersCreateBeerAndUseTheirOwnTolerance() {
        for (String name : new String[] {"Kegbearer", "Brewmaster Grunthar"}) {
            Game game = customGame();
            Player p = game.getPlayers().get(0);
            Card brewer = enter(name, p);
            resolveTriggers(game);
            assertEquals(countCardsWithName(game, "Beer"), name.equals("Kegbearer") ? 2 : 1);
            counters(brewer, CounterEnumType.DRUNKEN, 5);
            untap(game, p);
            assertEquals(brewer.getCounters(CounterEnumType.DRUNKEN), 4);
            assertTrue(brewer.isTapped());
        }
    }

    @Test
    public void hopfenblueteRollCreatesExactlyOneValidBeerPerRoll() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        Card brewer = addCard("Brüder Hopfenblüte", p);
        for (int n = 0; n < 12; n++) {
            AbilityUtils.resolve(db(brewer, "Roll"));
        }
        assertEquals(p.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getType().hasSubtype("Beer")).count(), 12L);
        counters(brewer, CounterEnumType.DRUNKEN, 7);
        untap(game, p);
        assertEquals(brewer.getCounters(CounterEnumType.DRUNKEN), 6);
    }

    @Test
    public void hotboxTracksNonlandManaAndPaysThreeHazeForAnElemental() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        Card hut = enter("Mystic Hotbox Hut", p);
        Card forest = addCard("Forest", p);
        SpellAbility forestMana = forest.getManaAbilities().get(0);
        forestMana.setActivatingPlayer(p);
        assertTrue(ComputerUtil.playNoStack(p, forestMana, game, false));
        resolveTriggers(game);
        assertEquals(hut.getCounters(CounterEnumType.HAZE), 0);
        SpellAbility hutMana = hut.getManaAbilities().get(0);
        hutMana.setActivatingPlayer(p);
        assertTrue(ComputerUtil.playNoStack(p, hutMana, game, false));
        resolveTriggers(game);
        assertEquals(hut.getCounters(CounterEnumType.HAZE), 1);
        hut.setTapped(false);
        counters(hut, CounterEnumType.HAZE, 2);
        SpellAbility token = hut.getSpellAbilities().stream().filter(sa -> sa.getApi() == ApiType.Token).findFirst().orElseThrow();
        token.setActivatingPlayer(p);
        assertTrue(ComputerUtil.playNoStack(p, token, game, false));
        assertEquals(hut.getCounters(CounterEnumType.HAZE), 0);
        assertTrue(hut.isTapped());
        assertTrue(p.getCardsIn(ZoneType.Battlefield).stream().anyMatch(c -> c.isToken() && c.isCreature() && c.getNetPower() == 2));
    }

    @Test
    public void charmingSirenObsessesOnlyAnOpponentsCreature() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        Card own = addCard("Couchpotato", p);
        Card other = addCard("Couchpotato", game.getPlayers().get(1));
        enter("Charming Siren", p);
        resolveTriggers(game);
        assertEquals(own.getCounters(CounterEnumType.OBSESSION), 0);
        assertEquals(other.getCounters(CounterEnumType.OBSESSION), 1);
    }

    @Test
    public void bluntFightTapsBothSurvivorsAndFreezesOneUntapWithoutStun() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        Player other = game.getPlayers().get(1);
        Card own = addCard("Couchpotato", p);
        Card enemy = addCard("Wixbirne", other);
        SpellAbility fight = spell("Blunt Force Trauma", p);
        fight.getTargets().add(own);
        fight.getSubAbility().getTargets().add(enemy);
        AbilityUtils.resolve(fight);
        assertEquals(own.getDamage(), 2);
        assertEquals(enemy.getDamage(), 0);
        assertTrue(own.isTapped());
        assertTrue(enemy.isTapped());
        assertEquals(own.getCounters(CounterEnumType.STUN), 0);
        untap(game, p);
        assertTrue(own.isTapped());
        untap(game, other);
        assertTrue(enemy.isTapped());
        untap(game, p);
        assertFalse(own.isTapped());
    }

    @Test
    public void naggingTwinCreatesOneFlyingBlueSirenAndNoEtbLoop() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        enter("Nagging Siren Twin", p);
        resolveTriggers(game);
        assertEquals(p.getCardsIn(ZoneType.Battlefield).stream().filter(Card::isCreature).count(), 2L);
        Card token = p.getCardsIn(ZoneType.Battlefield).stream().filter(Card::isToken).findFirst().orElseThrow();
        assertEquals(token.getName(), "Nagging Siren Twin");
        assertEquals(token.getNetPower(), 1);
        assertTrue(token.hasKeyword("Flying"));
    }

    @Test
    public void celestialEntryTapsAndStunsCreaturesOnBothSides() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        Card own = addCard("Couchpotato", p);
        Card other = addCard("Couchpotato", game.getPlayers().get(1));
        enter("Celestial Waters", p);
        resolveTriggers(game);
        assertTrue(own.isTapped());
        assertTrue(other.isTapped());
        assertEquals(own.getCounters(CounterEnumType.STUN), 1);
        assertEquals(other.getCounters(CounterEnumType.STUN), 1);
    }

    @Test
    public void foodSacrificeTriggersDistinguishTokensFromFoodCreatures() {
        for (boolean tokenFood : new boolean[] {false, true}) {
            Game game = customGame();
            Player p = game.getPlayers().get(0);
            Card scavenger = addCard("Gluttonous Scavenger", p);
            Card prince = addCard("Mattn, the Tasty Prince", p);
            Card food;
            if (tokenFood) {
                SpellAbility createFood = AbilityFactory.getAbility("DB$ Token | TokenScript$ c_a_food_sac", prince);
                createFood.setActivatingPlayer(p);
                AbilityUtils.resolve(createFood);
                food = p.getCardsIn(ZoneType.Battlefield).stream().filter(Card::isToken).findFirst().orElseThrow();
            } else {
                food = addCard("Couchpotato", p);
            }
            game.getAction().checkStateEffects(true);
            SpellAbility sacrifice = AbilityFactory.getAbility("DB$ Sacrifice | SacValid$ Food", prince);
            sacrifice.setActivatingPlayer(p);
            AbilityUtils.resolve(sacrifice);
            resolveTriggers(game);
            assertTrue(food.isInZone(ZoneType.Graveyard));
            assertEquals(scavenger.getCounters(CounterEnumType.P1P1), 1);
            assertEquals(prince.getCounters(CounterEnumType.P1P1), tokenFood ? 1 : 0);
        }
    }

    @Test
    public void overdoseOnlyGrantsCastingFromYourOwnGraveyard() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        Card own = addCardToZone("Double Burn", p, ZoneType.Graveyard);
        Card land = addCardToZone("Swamp", p, ZoneType.Graveyard);
        Card opponent = addCardToZone("Double Burn", game.getPlayers().get(1), ZoneType.Graveyard);
        AbilityUtils.resolve(spell("Overdose", p));
        game.getAction().checkStaticAbilities();
        assertFalse(own.mayPlay(p).isEmpty());
        assertTrue(land.mayPlay(p).isEmpty());
        assertTrue(opponent.mayPlay(p).isEmpty());
    }

    @Test
    public void overdoseCountsRepeatedGraveyardCastsAndLosesAtTheEndStep() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        Player other = game.getPlayers().get(1);
        AbilityUtils.resolve(spell("Overdose", p));
        for (int n = 1; n <= 3; n++) {
            Card card = addCardToZone("Double Burn", p, ZoneType.Graveyard);
            game.getAction().checkStaticAbilities();
            SpellAbility cast = card.getFirstSpellAbility();
            cast.setActivatingPlayer(p);
            cast.setMayPlay(card.mayPlay(p).get(0));
            cast.getTargets().add(other);
            cast.setHostCard(game.getAction().moveToStack(card, cast));
            game.getStack().add(cast);
            resolveTriggers(game);
            assertEquals(p.getLife(), 20 - 2 * n);
            Card effect = p.getCardsIn(ZoneType.Command).stream().filter(c -> c.getName().contains("Overdose")).findFirst().orElseThrow();
            assertEquals(effect.getSVar("OverdoseCasts"), "Number$" + n);
        }
        game.getAction().checkStaticAbilities();
        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, p);
        game.getTriggerHandler().runTrigger(TriggerType.Phase, AbilityKey.mapFromPlayer(p), false);
        resolveTriggers(game);
        assertTrue(p.hasLost());
    }
}
