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
    @Override
    protected Card createCard(String name, Player player) {
        // Strict printing selection avoids image-based fallbacks to an official
        // printing when a custom set intentionally uses an existing card name.
        for (forge.item.PaperCard paper : forge.StaticData.instance().getCommonCards().getAllCards(name)) {
            if ("CBT".equals(paper.getEdition())) {
                return Card.fromPaperCard(paper, player);
            }
        }
        return super.createCard(name, player);
    }

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
            {"Wasted", 3, "", "", "Destroy target creature. If that creature had one or more drunk counters on it, exile it instead.", "Instant", "B"},
            {"Nutten und Koks", 3, "", "", "At the beginning of your upkeep, you lose 2 life. Then choose one \u2014\nCreate a Treasure token.\nDraw a card.", "Enchantment", "BR"},
            {"Wixbirne", 4, "2", "2", "When Wixbirne enters the battlefield, target opponent discards a card unless they pay 3 life.\n{2}, {T}, Sacrifice Wixbirne: You gain 3 life.", "Creature \u2014 Food Mutant", "B"},
            {"Couchpotato", 2, "0", "4", "{2}, {T}, Sacrifice Couchpotato: You gain 3 life.", "Creature \u2014 Food Mutant", "B"},
            {"Puddinghead", 2, "2", "4", "Defender\n{2}, {T}, Sacrifice Puddinghead: You gain 3 life.\nWhen Puddinghead dies, you lose 3 life.", "Creature \u2014 Food Mutant", "B"},
            {"Buy a Round", 2, "", "", "Create two Beer Token.", "Sorcery", "W"},
            {"Food Coma", 4, "", "", "When Food Coma enters, exile target creature an opponent controls until Food Coma leaves the battlefield. Create a Food token.", "Enchantment", "W"},
            {"Last Man Standing", 4, "", "", "Choose a creature at random. Destroy all other creatures.", "Sorcery", "W"},
            {"The Tipsy Ram", 2, "", "", "Drunken creatures you control get +1/+1 and have vigilance.", "Enchantment \u2014 Location", "W"},
            {"Toast of Comfort", 1, "", "", "Each creature gains a Drunken counter.", "Instant", "W"},
            {"A Wife's Argument", 1, "", "", "Counter target noncreature spell unless its controller pays {2}.", "Instant", "U"},
            {"Charming Siren", 3, "2", "3", "Flash\nWhen Charming Siren enters the battlefield, obsess target creature an opponent controls.", "Creature \u2014 Siren", "U"},
            {"Circular Conversation", 4, "", "", "Flash\nEnchant creature\nEnchanted creature doesn\u2019t untap during its controller\u2019s untap step.", "Enchantment \u2014 Aura", "U"},
            {"Enraged Reef Leviathan", 5, "4", "4", "Enraged Reef Leviathan gets +3/+3 as long as there are three or more Sirens in your graveyard.", "Creature \u2014 Fish Leviathan", "U"},
            {"Mom's advice", 4, "", "", "Choose one \u2014\n\u2022 Counter target spell.\n\u2022 Surveil 2, then draw two cards.", "Instant", "U"},
            {"Nagging Siren Twin", 3, "2", "1", "When Nagging Siren Twin enters the battlefield, create a 1/1 blue Siren creature with flying that is also named Nagging Siren Twin.", "Creature \u2014 Siren", "U"},
            {"Nutshot overload", 2, "", "", "Tap target creature and put two stun counters on it.", "Sorcery", "U"},
            {"Ambush on the Throne", 2, "", "", "Target creature gets -1/-1 until end of turn. It gets an additional -1/-1 until end of turn for each creature you control.", "Instant", "B"},
            {"Overdose", 4, "", "", "Until end of turn, you may cast spells from your graveyard, but you lose life equal to their mana value.\nAt the end of the turn, if you cast three or more spells this way, you lose the game.", "Sorcery", "B"},
            {"Drunkard", 2, "0", "0", "Haste, Drunken Tolerance 1\nDrunkard enters the battlefield with three Drunken counters on it.", "Creature \u2014 Dwarf", "R"},
            {"Drunken Warrior", 2, "1", "1", "Haste\nDrunken Warrior enters the battlefield with three Drunken counter on it.\nDrunken counters cannot be removed from Drunken Warrior.", "Creature \u2014 Human Warrior", "R"},
            {"Hopsfield Vale", 0, "", "", "({T}: Add {G} or {W} to your mana pool.)\nAs Hopsfield Vale enters the battlefield, you may pay 2 life. If you don\u2019t, Hopsfield Vale enters the battlefield tapped.", "Land \u2014 Forest Plains", ""},
            {"Kegbearer", 6, "6", "6", "When Kegbearer, the Barrelback enters the battlefield, create two Beer token.\nDrunken Tolerance 4", "Creature \u2014 Half-Giant", "R"},
            {"Blunt Force Trauma", 2, "", "", "Target creature you control fights target creature you don\u2019t control. If a creature would survive this fight, tap it. It doesn\u2019t untap during its controller\u2019s next untap step.", "Instant", "G"},
            {"Gimme the herbs", 1, "", "", "Reveal the top four cards of your library. Put all forest cards revealed this way into your hand and the rest into your graveyard.", "Sorcery", "G"},
            {"Hallucinating Trip", 3, "", "", "Until end of turn, all Forests you control become 1/1 green Plant creatures with haste in addition to their other types. They\u2019re still lands.", "Sorcery", "G"},
            {"Inspiring Bard", 4, "3", "3", "When Inspiring Bard enters, the battlefield, choose one \u2014\n\u2022 Bardic Inspiration \u2014 Target creature gets +2/+2 until end of turn.\n\u2022 Song of Rest \u2014 You gain 3 life.", "Creature \u2014 Elf Bard", "G"},
            {"Mystic Herb Buddy", 4, "4", "4", "If you control four or more Forests, Mystic Herb Buddy has Reach and Trample.", "Creature \u2014 Plant", "G"},
            {"Mystic Herb Seedling", 2, "1", "1", "When Mystic Herb Seedling enters the battlefield, mill three cards. You may put a land card from among the cards milled this way into your hand. If you don\u2019t, put a +1/+1 counter on Mystic Herb Seedling.", "Creature \u2014 Plant", "G"},
            {"Mystic Hotbox Hut", 3, "", "", "Whenever you produce mana from a non-land source, put a Haze counter on Mystic Hotbox Hut.\n{T}: Add {G}.\n{T}, Remove three Haze counters: Create a 2/2 green Elemental creature token.", "Enchantment \u2014 Location", "G"},
            {"Brewmaster Grunthar", 3, "3", "4", "Drunken Tolerance 4\nWhen Brewmaster Grunthar enters the battlefield, create a Beer Token", "Creature \u2014 Dwarf", "WR"},
            {"Br\u00fcder Hopfenbl\u00fcte", 4, "2", "5", "At the beginning of each upkeep, roll a six-sided die. Create a Beer token with the result indicated below.\n1 \u2014 Expired Beer\n2 \u2014 Zwickl Beer\n3 \u2014 Wheat Beer\n4 \u2014 Dark Beer\n5 \u2014 Pils Beer\n6 \u2014 Bock Beer\nDrunken tolerance 6", "Legendary Creature \u2014 Human", "WR"},
            {"Ice Cold Siren", 3, "2", "3", "Whenever an opponent discards a card, draw a card.\n{1UB},{T}: Target opponent discards a card. Activate this ability only once.", "Creature \u2014 Siren", "UB"},
            {"Wake and Bake", 3, "", "", "Draw a card, then you may put a land card from your hand onto the battlefield tapped. Create a Food token.", "Sorcery", "WG"},
            {"Rotting Flesh", 2, "2", "2", "When Rotting Flesh dies, you lose 2 life.\n{2},{T}, Sacrifice Rotting Flesh: You gain 3 life.", "Creature \u2014 Food Mutant", "B"},
            {"Decaying Orchard", 0, "", "", "({T}: Add {B} or {G} to your mana pool.)\nAs Decaying Orchard enters the battlefield, you may pay 2 life. If you don\u2019t, Decaying Orchard enters the battlefield tapped.", "Land \u2014 Swamp Forest", ""},
            {"Distillery of Silent Death", 0, "", "", "({T}: Add {W} or {B} to your mana pool.)\nAs Distillery of Silent Death enters the battlefield, you may pay 2 life. If you don\u2019t, Distillery of Silent Death enters the battlefield tapped.", "Land \u2014 Plains Swamp", ""},
            {"Feuerball, Junge!", 10, "", "", "As an additional cost to cast this spell, you may tap any number of untapped creatures you control. This spell costs {1} less to cast for each creature tapped this way.\nExplosive Singularity deals 10 damage to any target.", "Sorcery", "R"},
            {"Sacred Brewery", 0, "", "", "({T}: Add {R} or {W} to your mana pool.)\nAs Sacred Brewery enters the battlefield, you may pay 2 life. If you don\u2019t, Sacred Brewery enters the battlefield tapped.", "Land \u2014 Mountain Plains", ""},
            {"Parsley Root", 1, "", "", "When Parsley Root enters the battlefield, you gain 1 life.\nEquipped creature gets +1/+0.\nEquip {1}", "Artifact \u2014 Equipment", ""},
            {"Celestial Waters", 3, "", "", "When Celestial Waters enters, tap each creature and put a stun counter on each of them.\nAt the beginning of your turn, you may move any number of counters of your choice from one target creature onto another target creature.", "Enchantment \u2014 Location", "U"},
            {"Gluttonous Scavenger", 5, "4", "5", "Whenever you sacrifice a Food, put a +1/+1 counter on Gluttonous Scavenger.\n{B/G}, Exile two cards from your graveyard: Create a Food token.", "Creature \u2014 Horror", "BG"},
            {"Brie-lliant Trapper", 3, "3", "3", "Whenever Brie-lliant Trapper enters the battlefield, create a Food token.\n{G}, Sacrifice a Food: Put a +1/+1 counter on target creature. Activate only as a sorcery.\n{B}, Sacrifice a Food: Target creature gets -1/-1 until end of turn. Activate only as a sorcery.", "Legendary Creature \u2014 Horror Food", "BG"},
            {"Chaotic Kitchen", 3, "", "", "When Chaotic Kitchen enters the battlefield, create a Food token.\nFood tokens you control are 2/2 creatures with \u201c{2}, {T}, Sacrifice this artifact: You gain 3 life\u201d in addition to their other types.", "Enchantment \u2014 Location", "BG"},
            {"Double Burn", 2, "", "", "Double Burn deals 2 damage to any target.\nFlashback\u2014{3R} (You may cast this card from your graveyard for its flashback cost. Then exile it.)", "Instant", "R"},
            {"Pumpkin Barrage Crew", 4, "3", "3", "When Pumpkin Barrage Crew enters the battlefield, create a Food token.\n{2}, Sacrifice a Food token: Pumpkin Barrage Crew deals 2 damage to target creature.", "Creature \u2014 Human Rogue", "B"},
            {"Mattn, the Tasty Prince", 5, "4", "5", "At the beginning of each upkeep, create a Food token.\nWhenever you sacrifice a Food token, put a +1/+1 counter on Mattn, the Tasty Prince.", "Legendary Creature \u2014 Human", "W"},
            {"Anschnly, the Vengeful Siren", 5, "4", "3", "Flying\nWhenever you cast a spell with mana value 2 or less, draw a card.\nWhenever you cast a spell with mana value 3 or greater, Anschnly, the Vengeful Siren deals 2 damage to target creature.", "Legendary Creature \u2014 Siren", "UR"},
            {"Noxious Cloud", 4, "", "", "All creatures get -4/-4 until end of turn.", "Sorcery", "B"},
            {"Mistcliff Springs", 0, "", "", "({T}: Add {U} or {R} to your mana pool.)\nAs Mistcliff Springs enters the battlefield, you may pay 2 life. If you don\u2019t, Mistcliff Springs enters the battlefield tapped.", "Land \u2014 Island Mountain", ""},
            {"Mistcliff Waterfall", 0, "", "", "({T}: Add {U} or {R} to your mana pool.)\nAs Mistcliff Waterfall enters the battlefield, you may pay 2 life. If you don\u2019t, Mistcliff Waterfall enters the battlefield tapped.", "Land \u2014 Island Mountain", ""},
            {"Caring Siren", 2, "2", "1", "Flash\nFlying\nWhen Caring Siren enters, target creature you control gains hexproof until end of turn.", "Creature \u2014 Siren", "U"},
            {"Jealous Siren", 3, "2", "3", "Whenever an opponent targets another creature you control with a spell or ability, Jealous Siren deals 2 damage to that player.", "Creature \u2014 Siren", "U"},
            {"Endless Expectations", 2, "", "", "At the beginning of your end step, if you control X or more untapped lands, where X is the number of Fulfillment counters on Endless Expectations, put a Fulfillment counter on it and draw a card. If you do not control X or more untapped lands, sacrifice Endless Expectations.", "Enchantment", "U"},
            {"Chaotic Council", 5, "", "", "For each color of mana spent to cast this spell, create a 2/2 Spirit creature token of that color.", "Sorcery", "WG"},
            {"Youthful Inner Heat", 2, "", "", "Flash\nEnchant creature\nWhen Youthful Inner Heat enters the battlefield, enchanted creature gains hexproof until end of turn.\nEnchanted creature has ward\u2014\u201cGift a card.\u201d", "Enchantment \u2014 Aura", "UR"},
            {"Flatfooted", 3, "", "", "Enchant creature\nWhenever enchanted creature becomes tapped, put a stun counter on it.\nWhen Flatfooted enters the battlefield, tap enchanted creature.", "Enchantment \u2014 Aura", "U"},
            {"Tactical Purge", 2, "", "", "Flash\nEnchant creature\nWhen Tactical Purge enters the battlefield, enchanted creature gains indestructible until end of turn.\nEnchanted creature loses all abilities.", "Enchantment \u2014 Aura", "WB"},
            {"Mysterious Remedy", 4, "", "", "Return target creature card from any graveyard to the battlefield under your control. Put a finality counter on it.(If it would die, exile it instead.)", "Sorcery", "W"},
            {"Last Duel", 2, "", "", "For each player, choose a target creature card in that player's graveyard if able. Return those cards to the battlefield under their owners' control with a finality counter on each of them. (If a creature with a finality counter on it would die, exile it instead.)", "Sorcery", "B"},
            {"Overdose Revival", 2, "", "", "Return target creature card from your graveyard to the battlefield. Put a finality counter on it. It gains\nUpkeep\u2014 Pay 2{B}", "Sorcery", "B"},
            {"Numbing Mass Resurrection", 6, "", "", "When Numbing Mass Resurrection enters the battlefield, each player returns all creature cards from their graveyard to the battlefield with a finality counter on each of them. (If a creature with a finality counter on it would die, exile it instead.)\nCreatures with finality counters on them lose all abilities.", "Enchantment", "WB"},
            {"Hangry", 2, "", "", "Target creature gets +3/+0 and gains haste until end of turn.\nIf you control a Food, it also gains first strike until end of turn.", "Instant", "R"},
            {"Sweet Harmless Siren", 2, "0", "4", "When Sweet Harmless Siren enters the battlefield, obsess up to one target creature.", "Creature \u2014 Siren", "U"},
            {"Baiting Siren", 4, "3", "4", "{2U}: Obsess target creature. Whenever a obsessed creature attacks, draw a card.", "Legendary Creature \u2014 Siren", "U"},
            {"The Hangry Games", 3, "", "", "Until end of turn, each creature becomes a 1/1 red and blue Siren creature with haste and loses all abilities, other creature types and colors.\nCreate three 1/1 red and blue \u201cHangry Siren\u201d creature tokens with haste.", "Instant", "UR"},
            {"Spar\u2019s Bazaar", 3, "", "", "{T}: Add one mana of any color.\nActivated abilities of Beer and Food tokens cost {1} less to activate.", "Enchantment \u2014 Location", ""},
            {"Owizahrer", 4, "1", "4", "upkeep \u2014 Tap another untapped creature you control and put a stun counter on it. If you do, draw a card. Otherwise, sacrifice Owizahrer.\nOwizahrer can\u2019t attack.", "Creature \u2014 Human Wizard", "U"},
            {"Hungry Disgust", 2, "2", "3", "When Hungry Disgust enters the battlefield, create a Food token.\nUpkeep\u2014 Sacrifice a Food. (At the beginning of your upkeep, you must Sacrifice a Food. If you don\u2019t, sacrifice this permanent.)\nWhenever you sacrifice a Food, put a +1/+1 counter on Hungry Disgust.", "Creature \u2014 Human", "BG"},
            {"Academic Quarter", 2, "", "", "Until end of turn, you may plot a creature card from your hand by paying its mana cost. You may do this only once. (Plot only as a sorcery. You may cast that card as a sorcery on a later turn without paying its mana cost.)\nDraw a card.", "Sorcery", "U"},
            {"Straggler", 1, "2", "2", "Straggler enters the battlefield tapped.", "Creature \u2014 Human", "R"},
            {"Happy Hour", 3, "", "", "Each player creates two Beer tokens.\nThen each creature gains an obsession counter.", "Sorcery", "R"},
            {"Echo of Empathy", 2, "", "", "Move all counters from target creature onto another target creature.", "Instant", "U"},
            {"Controlling Relationship", 4, "", "", "Enchant creature.\nWhen this Aura enters, tap enchanted permanent.\nYou control enchanted permanent.\nEnchanted permanent doesn\u2019t untap during its controller\u2019s untap step.", "Enchantment \u2014 Aura", "U"},
            {"Confusing Signals", 3, "", "", "Choose a player.\nTap up to two target creatures that player controls. Put a stun counter on each. That player draws a card for each creature tapped this way.", "Sorcery", "U"},
            {"Weedfield Herbalist", 2, "1", "3", "{T}: Add {G}.", "Creature \u2014 Plant Druid", "G"},
            {"Weedfield Grazer", 4, "3", "4", "{T}, Sacrifice a Forest: Create a Food token.", "Creature \u2014 Plant Ooze", "G"},
            {"Tavern Time Out", 2, "", "", "When Tavern Time-Out enters the battlefield, exile target drunken creature an opponent controls until Tavern Time-Out leaves the battlefield.", "Enchantment", "W"},
            {"Tavern Hand", 2, "2", "2", "When Tavern Hand enters the battlefield, create a Beer token.", "Creature \u2014 Human", "W"},
            {"Coffee Shop", 2, "", "", "Tokens you control have \u201c{T}: Add {G}.\u201d", "Enchantment \u2014 Location", "WG"},
            {"Pumpkin Catapult", 5, "7", "5", "When Pumpkin Catapult enters the battlefield, create a 1/1 red Pilot creature token named Pumpkin Gang Driver with \u201cThis creature crews Vehicles as though its power were 2 greater.\u201d\nCrew 3\nWhenever Pumpkin Catapult becomes crewed, you may sacrifice any number of drunken creatures that crewed it. When you sacrifice a creature this way, Pumpkin Catapult deals 2 damage to any target.", "Artifact \u2014 Vehicle", "BR"},
            {"Vandalism", 3, "", "", "Destroy target artifact or enchantment.\nIf a permanent is destroyed this way, its controller loses 2 life.", "Sorcery", "R"},
            {"Ogrontwin Steve", 4, "3", "4", "Trample\nAs long as you control a creature named \u201cOgrontwin Pete\u201d, Ogrontwin Steve gets +3/+3.", "Legendary Creature \u2014 Giant Warrior", "G"},
            {"Ogrontwin Pete", 4, "4", "3", "Menace\nAs long as you control a creature named \u201cOgrontwin Steve\u201d, Ogrontwin Pete gets +3/+3.", "Legendary Creature \u2014 Giant Warrior", "R"},
            {"Bromoment", 3, "", "", "Search your library for a card named Ogrontwin Pete or Ogrontwin Steve, reveal it, and put it into your hand. If you control one with that name, you may put it onto the battlefield instead. Then shuffle.", "Sorcery", "RG"},
            {"Blood Moon Rising", 3, "", "", "Enchant creature\nAt the beginning of enchanted creature\u2019s controller\u2019s upkeep, they lose 2 life.\nWhenever another permanent enters the battlefield, put a obsession counter on enchanted creature.\nAs long as enchanted creature has five or more obsession counters on it, it gets +10/+0, has menace and first strike.", "Enchantment \u2014 Aura", "BR"},
            {"Zimty", 3, "2", "3", "When Zimty dies, target opponent discards a card.\n{2}, {T}, Sacrifice Zimty: You gain 3 life.", "Creature \u2014 Food Mutant", "B"},
            {"Mystic Herb Vegetative", 3, "2", "2", "When Mystic Herb Vegetative enters the battlefield, mill three cards. You may put a land card from among the cards milled this way into your hand. If you don\u2019t, put a +1/+1 counter on Mystic Herb Vegetative.", "Creature \u2014 Plant", "G"},
            {"Raging Alcoholic", 3, "3", "2", "Haste\nDrunken Tolerance 5\nUpkeep\u2014 Sacrifice a Beer token.\nWhenever Raging Alcoholic attacks, create a Beer token.", "Creature \u2014 Dwarf Warrior", "WR"},
            {"Rotten Cellar", 1, "", "", "Whenever a Food is sacrificed, each opponent loses 1 life.", "Enchantment \u2014 Location", "B"},
            {"Rowdy Tavern", 2, "", "", "Drunken creatures you control get +1/+0 and have haste.", "Enchantment \u2014 Location", "R"},
            {"Laughing fit", 2, "", "", "As an additional cost to cast this spell, discard a card.\nDraw two cards.", "Instant", "R"},
            {"Puzzlemaster's Grimoire", 4, "", "", "When Puzzlemaster\u2019s Grimoire enters the battlefield, create a Treasure Token.\n{T}: Add one mana of any color.", "Artifact", ""},
            {"Brotherhoods Impact", 2, "", "", "Choose one \u2014\n\u2022 Up to two target creatures each get +1/+2 until end of turn.\n\u2022 Target creature an opponent controls loses all abilities until end of turn.", "Instant", "W"},
            {"Spar\u2019s Bazaar", 3, "", "", "{T}: Add one mana of any color.\nActivated abilities of Beer and Food tokens cost {1} less to activate.", "Enchantment \u2014 Location", ""},
            {"Blue Cow", 2, "", "", "Blue Cow enters the battlefield with three charge counters on it.\n{T}, Remove a charge counter from Blue Cow: Target creature gains flying until end of turn.\nWhen the last charge counter is removed from Blue Cow, sacrifice it.", "Artifact", ""},
            {"Library of Shared Memories", 1, "", "", "{T}: Move a counter from one creature you control to another creature you control. Activate only as a sorcery.", "Enchantment \u2014 Location", "U"},
            {"Sarma", 4, "", "", "Sarma enters the battlefield with four charge counters on it.\n{T}, Remove a charge counter from Sarma: Target creature gets +2/+2 until end of turn. Activate only as a sorcery.\n{2}, {T}, Sacrifice Sarma: You gain 4 life.", "Artifact \u2014 Food", "WG"},
            {"Soul Eater Siren", 2, "*", "*", "Soul Eater Siren\u2019s power and toughness are each equal to the number of counters on creatures on the battlefield.", "Creature \u2014 Siren", "WU"},
            {"Baked Ham Noodle Casserole", 3, "", "", "Baked Ham Noodle Casserole enters the battlefield with four charge counters on it.\n{T}, Remove a charge counter from Baked Ham Noodle Casserole: Creatures you control get +1/+1 until end of turn. Activate only as a sorcery.\n{2}, {T}, Sacrifice Baked Ham Noodle Casserole: You gain 4 life.", "Artifact \u2014 Food", "WG"},
            {"White Sausage Leberk\u00e4se", 3, "", "", "White Sausage Leberk\u00e4se enters the battlefield with four charge counters on it.\n{T}, Remove a charge counter from White Sausage Leberk\u00e4se: Target creature gets +1/+1 and gains vigilance until end of turn. Activate only as a sorcery.\n{2}, {T}, Sacrifice White Sausage Leberk\u00e4se: You gain 4 life.", "Artifact \u2014 Food", "WG"},
            {"Stink Normale Birnen", 4, "", "", "Sacrifice Stink Normale Birnen:\nCreatures you control get +0/+1 until end of turn. For each creature buffed this way, create a 1/1 green Elemental creature token named Stinky Elemental.", "Artifact \u2014 Food", "WG"},
            {"Mixed Grill Platter", 4, "", "", "Mixed Grill Platter enters the battlefield with four charge counters on it.\n{T}, Remove a charge counter from Mixed Grill Platter: Target creature gets +2/+1 and trample until end of turn. Activate only as a sorcery.\n{2}, {T}, Sacrifice Mixed Grill Platter: You gain 4 life.", "Artifact \u2014 Food", "WG"},
            {"Bochana Jugobeidl", 3, "", "", "When you sacrifice Bochana Jugobeidl, return target creature card with mana value 3 or less from your graveyard to the battlefield tapped.\n{2}, {T}, Sacrifice Bochana Jugobeidl: You gain 4 life.", "Artifact \u2014 Food", "WG"},
            {"Hairy Surprise", 1, "", "", "Return target creature to its owner\u2019s hand.", "Instant", "U"},
            {"Paladin\u2019s Nose Protection", 2, "", "", "Equipped creature gets +0/+2 and has Ward {1}.\nEquip {2}", "Artifact \u2014 Equipment", ""},
            {"Party Foul", 2, "", "", "Target creature gets -2/-2 until end of turn.", "Instant", "B"},
            {"Golden Stethoscope", 5, "", "", "Equip {3}\nWhen Golden Stethoscope enters the battlefield, attach it to target creature.\nWhenever equipped creature attacks, look at the top two cards of attacked opponent\u2019s library. Exile one of them and put the other on the bottom of that library.\nYou may play cards exiled this way for as long as they remain exiled. You may spend mana as though it were of any color to cast those spells.", "Legendary Artifact \u2014 Equipment", ""},
            {"Tschuschnpfeiferl", 2, "", "", "Equip {3}\nWhenever equipped creature attacks, create a 4/4 black Gnome creature token named \u201cVengeful Gnome Pack\u201d tapped and attacking. Exile that token at the beginning of the next end step.", "Artifact \u2014 Equipment", ""},
            {"Distant Cousin Windtongue", 2, "2", "2", "When Distant Cousin Windtongue enters the battlefield, create three blue and black Sorcery cards named AJDE! with mana cost {0} and \u201cDraw a card.\u201d Shuffle them into your library.", "Creature \u2014 Gnome", "UB"},
            {"AJDE!", 0, "", "", "This card can\u2019t be in your starting deck.\nDraw a card.", "Sorcery", ""},
            {"Distant Cousin Stinkpot", 3, "2", "3", "When Distant Cousin Stinkpot enters the battlefield, return up to one target Food card from your graveyard to your hand.", "Creature \u2014 Gnome", "BG"},
            {"Distant Cousin Shotcrusher", 1, "2", "1", "Drunken Tolerance 3\nWhenever another creature you control gets a Drunken counter, Distant Cousin Shotcrusher also gets a Drunken counter.", "Creature \u2014 Gnome", "BR"},
            {"Distant Uncle Goldbeard", 4, "3", "4", "When Distant Uncle Goldbeard enters the battlefield, create a Beer token for each other creature you control.", "Creature \u2014 Gnome", "WB"},
            {"Grandma Dragana Blackbrew", 3, "1", "3", "Other Gnomes you control get +1/+1.\nWhenever another Gnome enters the battlefield under your control, you gain 2 life.\nWhenever another Gnome you control dies, target opponent loses 2 life.", "Legendary Creature \u2014 Gnome", "B"},
            {"Distant Aunt Alestorm", 2, "3", "1", "Whenever a Gnome you control attacks, each opponent loses 1 life.", "Creature \u2014 Gnome", "BR"},
            {"Bottle Bash", 2, "", "", "Bottle Bash deals 3 damage to target creature or planeswalker.\nIf you control a Drunken creature, Bottle Bash deals 5 damage instead.", "Instant", "R"},
            {"Mocking Parrot", 2, "1", "2", "Flying\nWhenever you cast an instant or sorcery spell with mana value 2 or less for the first time each turn, you may copy that spell. You may choose new targets for the copy.", "Creature \u2014 Bird", "UR"},
            {"Loyal Watchdog", 2, "2", "2", "Whenever Loyal Watchdog attacks with exactly one other creature, untap Loyal Watchdog and that creature. They each get +1/+0 until end of turn.", "Creature \u2014 Dog", "W"},
            {"Goblin Party Mob", 6, "4", "4", "Affinity for Beer (This spell costs {1} less to cast for each Beer you control.)\nDrunken Tolerance 5", "Creature \u2014 Goblin", "R"},
            {"The Unflushed", 2, "1", "1", "The Unflushed gets +0/+1 for each Food card in your graveyard.", "Creature \u2014 Ooze Mutant Food", "B"},
            {"Indica Hedge", 2, "0", "4", "Defender, Ward {1}", "Creature \u2014 Plant", "G"},
            {"Cannabis Field Defender", 5, "4", "5", "Ward {2}", "Creature \u2014 Plant", "G"},
            {"Du gibst dem Baby a M\u00fcch", 2, "", "", "Enchant creature\nEnchanted creature can\u2019t attack or block, and its activated abilities can\u2019t be activated.", "Enchantment \u2014 Aura", "W"},
            {"Milk", 1, "", "", "{2}, {T}, Sacrifice Milk: You gain 3 life.", "Artifact \u2014 Food", "WG"},
            {"Cini Minis", 1, "", "", "{2}, {T}, Sacrifice Cini Minis:\nYou gain 3 life. You may sacrifice a Food named Milk as part of this ability. If you do, draw three cards.", "Artifact \u2014 Food", "WG"},
            {"Rowdy Riot", 3, "3", "2", "Whenever a Beer token is sacrificed, Rowdy Riot gets +1/+0 until end of turn.", "Creature \u2014 Human, Berserker", "R"},
            {"Hangover", 3, "", "", "Tap target creature. Remove all counters from it. Then put three stun counters on it.", "Instant", "U"},
            {"Cold Splash", 1, "", "", "Remove up to two Stun counters and/or Obsession counters from target creature.\nDraw a card.", "Instant", "U"},
            {"Too Drunk to Dodge", 1, "", "", "Tap target creature.\nIf it has one or more Drunken counters, put a stun counter on it.", "Instant", "U"},
            {"Bottomless Keg", 2, "", "", "{2}, {T}: Create a Beer token.", "Legendary Artifact", ""},
            {"Wobbly Gnome", 2, "2", "2", "As long as Wobbly Gnome is Drunken, it has menace.", "Creature \u2014 Gnome", "B"},
            {"Way Expired Wiener", 1, "1", "1", "Deathtouch", "Creature \u2014 Food Mutant", "B"},
            {"Undercity Back Room", 3, "", "", "{2}, {T}, Sacrifice a creature: Create a Another Brother token. (2/2 black Gnome creature)\n{5}, {T}, Sacrifice a creature: Create a Vengeful Gnome Pack token. (4/4 black Gnome creature)", "Enchantment \u2014 Location", "B"},
            {"Overgrown Fermentation Beds", 3, "", "", "{T}: Add {G}.\nAt the beginning of your end step, you may put a Drunken counter on a Plant creature.", "Enchantment \u2014 Location", "G"},
            {"Blightborne Colossus", 6, "9", "9", "Upkeep\u2014 Put a -1/-1 counter on Blightborne Colossus.\nReach", "Creature \u2014 Plant, Giant", "G"},
            {"Untamable Grove Colossus", 4, "8", "8", "This creature enters tapped with three stun counters on it.\nReach", "Creature \u2014 Plant, Giant", "G"},
            {"Lena, the Silent One", 3, "3", "3", "As long as there are three or more Obsession counters on creatures on the battlefield, The Silent One gets +3/+3.", "Legendary Creature \u2014 Siren", "U"},
            {"Hanna, Nature\u2019s Siren", 2, "2", "2", "Whenever another Plant or Siren creature dies, put a +1/+1 counter and an Obsession counter on Hanna.", "Legendary Creature \u2014 Siren", "UG"},
            {"Natalie, Mother Siren", 4, "3", "3", "When Natalie, Mother Siren enters the battlefield, create three 1/1 Hangry Siren tokens with haste.\nOther Sirens you control have Upkeep {1}.", "Legendary Creature \u2014 Siren", "UR"},
            {"Shady Herb Dealer", 2, "1", "2", "Upkeep costs you pay are reduced by one mana of any color.", "Creature \u2014 Human", "G"},
            {"Mutating Sapling", 2, "2", "2", "Exhaust \u2014 {W}: Mutating Sapling gains vigilance. (Activate each exhaust ability only once.)\nExhaust \u2014 {B}: Mutating Sapling gains deathtouch. (Activate each exhaust ability only once.)\nExhaust \u2014 {GU}: Put two +1/+1 counters on Mutating Sapling. (Activate each exhaust ability only once.)", "Creature \u2014 Plant", "G"},
            {"S\u00fc\u00fc\u00fc, the Fourth Child", 3, "3", "2", "S\u00fc\u00fc\u00fcnergy \u2014 As long as you control Natalie, Mother Siren, S\u00fc\u00fc\u00fc is a Siren and Sirens you control can\u2019t have upkeep costs.", "Legendary Creature \u2014 Human", "U"},
            {"The Loan Syndicate", 0, "", "", "{T}: Add {1}.\n{T}: Add two mana of any one color. Spend this mana only to pay upkeep costs.", "Land", ""},
            {"Gnome Loan", 3, "", "", "As an additional cost to cast this spell, sacrifice a permanent and pay 2 life.\nDraw three cards.", "Sorcery", "B"},
            {"Debt Collector", 5, "4", "4", "When Debt Collector enters the battlefield, create a Treasure token for each creature target opponent controls.", "Creature \u2014 Gnome", "B"},
            {"The Rake", 1, "", "", "Equipped creature gets +1/+0.\nWhenever equipped creature attacks, you may remove a counter from target creature.\nEquip {2}", "Artifact \u2014 Euipment", ""},
            {"The Good Stuff", 3, "", "", "Choose one \u2014\n\u2022 Destroy target artifact.\n\u2022 Destroy target enchantment.\n\u2022 Destroy target creature with flying.", "Instant", "G"},
            {"Mimimi", 4, "", "", "Target creature\u2019s owner puts it on the top or bottom of their library.", "Instant", "U"},
            {"Sleepless Night", 2, "", "", "Tap up to two target creatures. Then target creature you control with mana value 2 or less gets +2/+0 and gains haste until end of turn.", "Instant", "UR"},
            {"Shut up", 3, "", "", "Enchant creature\nEnchanted creature loses all abilities and can\u2019t attack.", "Enchantment \u2014 Aura", "W"},
            {"MAGAZIN!", 3, "", "", "Exile the top two cards of your library. Until the end of your next turn, you may play those cards.", "Sorcery", "R"},
            {"Magical Bucket Pump", 1, "", "", "Equipped creature has \u201c{T}: Tap target creature.\u201d\nEquip {2}", "Artifact \u2014 Equipemnt", ""},
            {"Unpleasant Surprise", 3, "", "", "Return target creature to its owner\u2019s hand. Its controller loses 3 life unless they sacrifice a nonland permanent or discard a card.", "Instant", "UB"},
            {"Dungeon Master", 4, "3", "5", "At the beginning of your upkeep, roll a d20.\n1\u20135 | Create a Treasure token.\n6\u201315 | Create a Beer token.\n16\u201319 | Draw a card.\n20 | You may cast a spell from your hand without paying its mana cost.", "Creature \u2014 Human, Wizard", "WU"},
            {"Marlene, the Leader", 2, "2", "2", "Vigilance\nOther Squadmembers you control get +0/+1.", "Legendary Creature \u2014 Squadmember", "W"},
            {"Robin, the Pusher", 2, "2", "2", "Haste\nOther Squadmembers you control get +1/+0.", "Legendary Creature \u2014 Squadmember", "R"},
            {"Tobias, the Grenadier", 2, "2", "2", "Deathtouch\nOther Squadmembers you control get +1/+0.", "Legendary Creature \u2014 Squadmember", "B"},
            {"Max, the Sniper", 2, "1", "1", "Flying\nOther Squadmembers you control get +1/+0.", "Legendary Creature \u2014 Squadmember", "U"},
            {"Let\u2019s Fucking Go", 1, "", "", "Target creature gets +3/+0 and gains haste until end of turn. You may discard a card. If you do, draw a card.", "Instant", "R"},
            {"Summon the Squad", 4, "", "", "Search your library for Marlene, the Leader, Robin, the Pusher, Max, the Sniper, and Tobias, the Grenadier. Put them onto the battlefield, then shuffle.", "Sorcery", "WUBR"},
            {"Homemade Paranoia", 3, "", "", "Reveal the top three cards of your library. If a creature card is revealed this way, put a creature card from among them into your hand. Otherwise, put all land cards revealed this way onto the battlefield tapped. Put the rest into your graveyard.", "Sorcery", "G"},
            {"Flooded Palace", 0, "", "", "({T}: Add {U} or {W} to your mana pool.)\nAs Flooded Palace enters the battlefield, you may pay 2 life. If you don\u2019t, Flooded Palace enters the battlefield tapped.", "Land \u2014 Island Plains", ""},
            {"Duskmire", 0, "", "", "({T}: Add {U} or {B} to your mana pool.)\nAs Duskmire enters the battlefield, you may pay 2 life. If you don\u2019t, Duskmire enters the battlefield tapped.", "Land \u2014 Island Swamp", ""},
            {"Reefgarden", 0, "", "", "({T}: Add {G} or {U} to your mana pool.)\nAs Reefgarden enters the battlefield, you may pay 2 life. If you don\u2019t, Reefgarden enters the battlefield tapped.", "Land \u2014 Forest Island", ""},
            {"The Stoned Peaks", 0, "", "", "({T}: Add {R} or {G} to your mana pool.)\nAs The Stoned Peaks enters the battlefield, you may pay 2 life. If you don\u2019t, The Stoned Peaks enters the battlefield tapped.", "Land \u2014 Mountain Forest", ""},
            {"The Blessed Hideaway", 2, "", "", "Whenever a multicolored spell is cast, choose one \u2014\n\u2022 Create a Food token.\n\u2022 Create a Beer token.\n\u2022 Create a Treasure token.", "Enchantment \u2014 Location", ""},
            {"Roadside Distraction", 2, "", "", "Target creature can\u2019t block this turn.\nDraw a card.", "Instant", "R"},
            {"Have a Beer, or Three", 2, "", "", "Roll a d20. If the result is odd, create three Expired Beer tokens and lose 3 life. If the result is even, create three Beer tokens. If the result is 20, also draw a card.", "Sorcery", "BG"},
            {"Draft Tap", 4, "", "", "When Draft Tap enters the battlefield, create three Beer tokens.\nWhenever you sacrifice a Beer token, put an additional Drunken counter on the creature it targets.", "Legendary Artifact", "W"},
            {"Fill Em Up", 3, "", "", "Choose one \u2014\n\u2022 Put Drunken counters on target creature equal to its Drunken Tolerance plus one.\n\u2022 Deal 6 damage to target planeswalker.", "Sorcery", "R"},
            {"Critical (S)hit", 2, "", "", "Destroy target tapped creature.", "Sorcery", "B"},
            {"Stoned Speech", 2, "", "", "Put a +1/+1 counter on each creature you control.", "Sorcery", "G"},
            {"Designated Driver", 3, "6", "3", "Designated Driver gets \u22121/\u22120 for each Beer and each drunken creature on the battlefield.\nDesignated Driver crews Vehicles as though its power were at least 3.", "Creature \u2014 Human Pilot", "W"},
            {"A Place to Stay", 2, "", "", "Search your library for a Location card, reveal it, put it into your hand, then shuffle.\nYou gain 2 life.", "Sorcery", "W"},
            {"Hexenschuss", 2, "", "", "Destroy target attacking creature.", "Instant", "W"},
            {"Tavern Regular", 1, "1", "2", "When Tavern Regular enters the battlefield, you may reveal a card from your hand. If you do, create a Beer token.", "Creature \u2014 Human", "W"},
            {"Tavern Bouncer", 2, "1", "3", "{T}: Tap target drunken creature.", "Creature \u2014 Dwarf", "W"},
            {"Tavern Medic", 2, "2", "2", "{T}: Remove any number of Drunken counters from target creature. If it\u2019s a creature you control, you gain 1 life for each counter removed this way. Otherwise, you lose 1 life for each counter removed this way.", "Creature \u2014 Dwarf", "W"},
            {"Edi, the Beerpig", 4, "3", "3", "Kicker\u2014Sacrifice one or more Beer (You may sacrifice one or more Beer in addition to any other costs as you cast this spell.)\nIf Edi was kicked, it enters with two +1/+1 counters on it for each Beer sacrificed to cast it.\nIf one or more Drunken counters would be put on Edi, instead put that many +1/+1 counters on it.", "Legendary Creature \u2014 Pig", "W"},
            {"Goner", 1, "0", "2", "Goner enters tapped.\nIf Goner would be exiled from anywhere, put it into its owner\u2019s graveyard instead.", "Creature \u2014 Human", "B"},
            {"Recycle", 1, "", "", "Exile X cards from your graveyard. Create a Treasure token for each card exiled this way.\nIf X is 4 or greater, draw a card.", "Sorcery \u2014 Common", "G"},
            {"Closing Hour", 3, "", "", "Return all tokens to their owners\u2019 hands.", "Instant", "U"},
            {"Always With Us", 3, "", "", "Whenever a creature you control becomes the target of a spell or ability, it gets +1/+1 until end of turn.", "Legendary Enchantment \u2014 Rare", "W"},
            {"Innkeepers Daugther", 2, "1", "3", "{T}: Create a Treasure token for each drunken creature on the battlefield.", "Creature \u2014 Human", "W"},
            {"Hoard Beast", 5, "*", "4", "When Hoard Beast enters the battlefield, create a Food token, a Beer token, and a Treasure token.\nThe Hoard Beast\u2019s power is equal to the number of artifact tokens on the battlefield.", "Creature \u2014 Horror", "G"},
            {"Nadine, Sirens\u2019 Origin", 3, "3", "3", "Flying\nOther Sirens you control get +1/+1 and have ward {1}.", "Legendary Creature \u2014 Siren", "U"},
            {"The Mind", 2, "", "", "You and target opponent each search your library for a card and reveal it. If those cards share a mana value or card type, put them into their owners\u2019 hands. Otherwise, each player shuffles. Draw a card.", "Sorcery", ""},
            {"Boosted Excitement", 1, "", "", "Target creature gets +2/+2 until end of turn. If you control three or more creatures, it also gains hexproof until end of turn.", "Instant", "W"},
            {"Provoker", 3, "3", "1", "Whenever Provoker enters the battlefield or attacks, put an Obsession counter on target creature an opponent controls.", "Creature \u2014 Goblin", "R"},
            {"Frozen Taps", 2, "", "", "Artifacts can\u2019t be tapped.\nWhen there are ten or more artifacts on the battlefield, sacrifice Frozen Taps.", "Enchantment", "U"},
            {"Experimental Invention", 2, "2", "2", "Flying\nWhenever Experimental Invention deals combat damage to a player, look at the top card of your library and the top card of that player\u2019s library.\nCrew 1", "Artifact \u2014 Vehicle", ""},
            {"Hosn Owe", 1, "", "", "Target opponent reveals their hand. You choose a noncreature, nonland card from it. That player discards that card.", "Sorcery", "B"},
            {"Improvised Escape", 1, "", "", "Exile target creature you control. Return that card to the battlefield under its owner\u2019s control at the beginning of the next end step.", "Instant", "W"},
            {"Strange Allergic Reaction", 2, "", "", "Enchant creature\nEnchanted creature is a 1/1 green Plant creature with \u201c{T}: Add {G}.\u201d", "Enchantment \u2014 Aura", "G"},
            {"The Fallen", 3, "3", "2", "The Fallen enters the battlefield tapped with a Drunken counter on it.\nIf you would discard The Fallen, put it onto the battlefield tapped with a Drunken counter on it instead.", "Creature \u2014 Orc", "R"},
            {"Grinder", 1, "", "", "{T}, Sacrifice a land: Add {GG}.", "Artifact", ""},
            {"Zigeuner-Duo", 4, "4", "3", "Whenever Zigeuner-Duo deals combat damage to a player, you may gain control of target artifact token that player controls.", "Creature \u2014 Gnome Goblin", "R"},
            {"Sauna Elemental", 4, "3", "3", "Prowess\nWhenever you cast your second instant or sorcery spell each turn, put a +1/+1 counter on Sauna Elemental. It gains menace until end of turn.", "Creature \u2014 Elemental", "R"},
            {"Rotborn Colossus", 3, "4", "4", "Upkeep\u2014 Sacrifice a Food. When you sacrifice a Food token this way, put two +1/+1 counters on Rotborn Colossus.\nWhenever another creature dies, create a Food token.", "Creature \u2014 Plant Giant", "G"},
            {"Igy's Astronomical Return", 4, "", "", "Choose one or both \u2014\n\u2022 Return target creature card from your graveyard to your hand.\n\u2022 Return target Food card from your graveyard to your hand.", "Sorcery", "B"},
            {"Fat Felicia", 6, "4", "6", "When Fat Felicia enters, you may return target creature with power 2 or less to its owner\u2019s hand.\nFat Felicia can\u2019t be blocked by creatures with power 2 or less.", "Creature \u2014 Siren", "U"},
            {"Devious Duo", 2, "3", "1", "Whenever one or more obsession counters are removed from a creature an opponent controls, that creature\u2019s controller loses 1 life.", "Creature \u2014 Human", "R"},
            {"Big Ball Bluebird", 3, "2", "2", "Flying\nBig-Ball Bluebird has vigilance as long as you control a Location.", "Creature \u2014 Bird", "W"},
            {"Austrian Slap", 1, "", "", "Austrian Slap deals 2 damage to target creature. If that creature has a Drunken counter on it, Austrian Slap deals 4 damage to it instead.", "Instant", "R"},
            {"Lecherous Patron", 3, "2", "2", "Whenever a Drunken counter is put on Lecherous Patron, it gets +2/+0 until end of turn.", "Creature \u2014 Gnome", "R"},
            {"Jolly Brewbelly", 5, "4", "4", "Jolly Brewbelly enters with three Drunken counters on it.\nUpkeep\u2014 Remove a Drunken counter from Jolly Brewbelly.\nDrunken Tolerance 3", "Creature \u2014 Dwarf", "R"},
            {"Desperate Hydrator", 4, "3", "3", "Whenever you cast an instant or sorcery spell, draw a card, then discard a card unless you remove a Drunken counter from a creature you control.", "Creature \u2014 Human", "R"},
            {"Puff Mama", 4, "2", "4", "Menace\nWhenever one or more Obsession counters are put on a creature, create a Treasure token.", "Creature \u2014 Tiefling", "R"},
            {"Domina\u2019s Devotee", 4, "2", "2", "Haste\nWhen Domina\u2019s Devotee enters, put an Obsession counter on it.\nWhenever an Obsession counter is put on Domina\u2019s Devotee, it gets +3/+3 until end of turn.", "Creature \u2014 Human", "R"},
            {"Honest Laborer", 4, "3", "4", "Vigilance\n{T}: Create a Beer token. Activate only if you attacked this turn.", "Creature \u2014 Dwarf", "W"},
            {"Gonorrhea", 3, "", "", "Enchant creature\nEnchanted creature gets +2/-2.\nAt the beginning of the upkeep of enchanted creature\u2019s controller, that player loses 2 life.", "Enchantment \u2014 Aura", "B"},
            {"Ale Addict", 3, "*", "3", "When Ale Addict enters, create a Beer token.\nAle Addict\u2019s power is equal to the number of Beer tokens you control.", "Creature \u2014 Dwarf", "W"},
            {"Devoted Homebody", 2, "3", "3", "Defender\nAs long as you control a Location, Devoted Homebody can attack as though it didn\u2019t have defender.", "Creature \u2014 Human", "W"},
            {"Bad Drunk", 3, "3", "2", "As long as Bad Drunk is Drunken, it has flying.", "Creature \u2014 Dwarf", "W"},
            {"Attention Seeker", 5, "3", "4", "Attention Seeker gets +1/+1 for each other creature you control.", "Creature \u2014 Human", "W"},
            {"Take a Sniff", 1, "", "", "Target creature gets +1/+1 and gains hexproof until end of turn.", "Instant", "W"},
            {"Midnight Fridge Raider", 2, "2", "2", "Whenever a Food you control is put into a graveyard from the battlefield, Midnight Fridge Raider gets +1/+1 until end of turn.", "Creature \u2014 Gnome", "B"},
            {"Hitman", 3, "5", "3", "Upkeep {B} (At the beginning of your upkeep, you must pay {B}. If you don\u2019t, sacrifice this permanent.)", "Creature \u2014 Gnome", "B"},
            {"Still Good", 3, "", "", "Return target Food card from your graveyard to your hand. Draw a card and lose 2 life.", "Sorcery", "B"},
            {"Stingats Gs\u00f6chts", 6, "4", "4", "Creatures your opponents control get -1/-1.\nWhenever another creature dies, put a +1/+1 counter on Stingats Gs\u00f6chts.", "Creature \u2014 Food Horror", "B"},
            {"Sofa Pizza", 3, "2", "2", "When Sofa Pizza is put into your graveyard from anywhere, until the end of your next turn, you may pay {1B}. If you do, return Sofa Pizza from your graveyard to your hand.", "Creature \u2014 Food Mutant", "B"},
            {"Nagging Little Sister", 2, "2", "1", "As long as there is an obsessed creature on the battlefield, Nagging Little Sister has flying.", "Creature \u2014 Siren", "U"},
            {"Manipulative Temptress", 3, "2", "3", "When Manipulative Temptress enters, you may move a counter from one creature to another creature.", "Creature \u2014 Siren", "U"},
            {"Cold Water Coward", 3, "4", "3", "At the beginning of your upkeep, if Cold Water Coward isn\u2019t Drunken, return it to its owner\u2019s hand.", "Creature \u2014 Human", "U"},
            {"Hunting Siren", 5, "3", "3", "Flying\nWhenever Hunting Siren attacks, another target Siren you control gets +1/+1 and gains flying until end of turn.", "Creature \u2014 Siren", "U"},
            {"Canonball Carl", 4, "2", "2", "Flash\nWhen Canonball Carl enters, return another target creature to its owner\u2019s hand.", "Creature \u2014 Human", "U"},
            {"Barmaid", 2, "2", "1", "When Barmaid enters, if you control an artifact token, put an Obsession counter on target creature an opponent controls. (A creature with one or more Obsession counters on it attacks each combat if able. Whenever a creature with one or more Obsession counters on it attacks, remove an Obsession counter from it.)", "Creature \u2014 Siren", "U"},
            {"Coastal Watchman", 3, "3", "2", "Whenever one or more obsessed creatures attack, untap target creature you control.", "Creature \u2014 Human", "U"},
            {"Not Invited", 2, "", "", "Counter target creature or planeswalker spell.", "Instant", "U"},
            {"Information Broker", 3, "1", "3", "Whenever you cast an instant or sorcery spell, you may pay {1U}. If you do, draw a card.", "Creature \u2014 Gnome", "U"},
            {"Copycat", 3, "2", "2", "Whenever one or more counters are put on another creature you control, you may put the same number and kinds of counters on Copycat.", "Legendary Creature \u2014 Shapeshifter", "U"},
            {"Blightpear Ancient", 4, "1", "6", "Defender, reach\nExhaust \u2014 {2GG}: Create two Food tokens. (Activate each exhaust ability only once.)", "Creature \u2014 Treefolk", "G"},
            {"Overstimulated Flora", 4, "4", "4", "Upkeep {1G} (At the beginning of your upkeep, you must pay {1G}. If you don\u2019t, sacrifice this permanent.)\nWhenever you pay Overstimulated Flora\u2019s upkeep cost, put a +1/+1 counter on it.", "Creature \u2014 Plant", "G"},
            {"Ravenous Trollkin", 3, "4", "4", "Upkeep {G} (At the beginning of your upkeep, you must pay {G}. If you don\u2019t, sacrifice this permanent.)", "Creature \u2014 Troll", "G"},
            {"Exasperated Patriarch", 6, "6", "6", "Reach, trample", "Creature \u2014 Troll", "G"},
            {"The Stoned Peaks", 0, "", "", "({T}: Add {R} or {G} to your mana pool.)\nAs The Stoned Peaks enters the battlefield, you may pay 2 life. If you don\u2019t, The Stoned Peaks enters the battlefield tapped.", "Land \u2014 Mountain Forest", ""},
            {"Hopsfield Vale", 0, "", "", "({T}: Add {G} or {W} to your mana pool.)\nAs Hopsfield Vale enters the battlefield, you may pay 2 life. If you don\u2019t, Hopsfield Vale enters the battlefield tapped.", "Land \u2014 Forest Plains", ""},
            {"Sacred Brewery", 0, "", "", "({T}: Add {R} or {W} to your mana pool.)\nAs Sacred Brewery enters the battlefield, you may pay 2 life. If you don\u2019t, Sacred Brewery enters the battlefield tapped.", "Land \u2014 Mountain Plains", ""},
            {"Decaying Orchard", 0, "", "", "({T}: Add {B} or {G} to your mana pool.)\nAs Decaying Orchard enters the battlefield, you may pay 2 life. If you don\u2019t, Decaying Orchard enters the battlefield tapped.", "Land \u2014 Swamp Forest", ""},
            {"Flooded Palace", 0, "", "", "({T}: Add {U} or {W} to your mana pool.)\nAs Flooded Palace enters the battlefield, you may pay 2 life. If you don\u2019t, Flooded Palace enters the battlefield tapped.", "Land \u2014 Island Plains", ""},
            {"Underground District", 0, "", "", "({T}: Add {B} or {R} to your mana pool.)\nAs Underground District enters the battlefield, you may pay 2 life. If you don\u2019t, Underground District enters the battlefield tapped.", "Land \u2014 Swamp Mountain", ""},
            {"Underground District", 0, "", "", "({T}: Add {B} or {R} to your mana pool.)\nAs Underground District enters the battlefield, you may pay 2 life. If you don\u2019t, Underground District enters the battlefield tapped.", "Land \u2014 Swamp Mountain", ""},
            {"Duskmire", 0, "", "", "({T}: Add {U} or {B} to your mana pool.)\nAs Duskmire enters the battlefield, you may pay 2 life. If you don\u2019t, Duskmire enters the battlefield tapped.", "Land \u2014 Island Swamp", ""}
        };
    }

    @Test(dataProvider = "betaCards")
    public void loadedCardMatchesReviewedMseSource(String name, int mv, String power, String toughness, String oracle, String types, String colors) {
        Game game = customGame();
        Card card = createCard(name, game.getPlayers().get(0));
        assertEquals(card.getManaCost().getCMC(), mv, name);
        // Normalize the source's known subtype spelling/punctuation without editing MSE.
        for (String type : types.replace(" — ", " ").replace(",", "")
                .replace("Euipment", "Equipment").replace("Equipemnt", "Equipment")
                .replace(name.equals("Recycle") ? " Common" : "__none__", "")
                .replace(name.equals("Always With Us") ? " Rare" : "__none__", "").split(" ")) {
            assertTrue(card.getType().hasStringType(type), name + " missing type " + type);
        }
        // Windtongue explicitly creates blue/black AJDE! cards; this generated-only
        // definition overrides color despite its zero mana cost.
        String expectedColors = name.equals("AJDE!") ? "UB" : colors;
        assertEquals(card.getColor(), forge.card.ColorSet.fromNames(expectedColors.split("")), name + " colors");

        String displayed = card.getRules().getOracleText().replace("\\n", "\n").replace("\r\n", "\n");
        // Forge reuses two mechanically identical upstream cards with the same names.
        if (name.equals("Food Coma")) {
            displayed = displayed.replace(" (It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")", "");
        }
        if (name.equals("Inspiring Bard")) {
            oracle = oracle.replace("enters, the battlefield,", "enters,");
        }
        assertEquals(displayed, oracle, name);
        if (!power.isEmpty() && !power.equals("*")) {
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
            Card card = n == 1 ? addCardToZone("Double Burn", p, ZoneType.Graveyard)
                    : p.getCardsIn(ZoneType.Graveyard).stream().filter(c -> c.getName().equals("Double Burn")).findFirst().orElseThrow();
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
        game.getTriggerHandler().resetActiveTriggers();
        game.getTriggerHandler().runTrigger(TriggerType.Phase, AbilityKey.mapFromPlayer(p), false);
        resolveTriggers(game);
        assertTrue(p.hasLost());
    }

    @Test
    public void overdoseDoesNotLoseAtEndStepWithFewerThanThreeCasts() {
        for (int count : new int[] {0, 1, 2}) {
            Game game = customGame();
            Player p = game.getPlayers().get(0);
            AbilityUtils.resolve(spell("Overdose", p));
            for (int n = 0; n < count; n++) {
                Card card = addCardToZone("Double Burn", p, ZoneType.Graveyard);
                game.getAction().checkStaticAbilities();
                SpellAbility cast = card.getFirstSpellAbility();
                cast.setActivatingPlayer(p);
                cast.setMayPlay(card.mayPlay(p).get(0));
                cast.getTargets().add(game.getPlayers().get(1));
                cast.setHostCard(game.getAction().moveToStack(card, cast));
                game.getStack().add(cast);
                resolveTriggers(game);
            }
            game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, p);
            game.getTriggerHandler().resetActiveTriggers();
            game.getTriggerHandler().runTrigger(TriggerType.Phase, AbilityKey.mapFromPlayer(p), false);
            resolveTriggers(game);
            assertFalse(p.hasLost());
        }
    }
    private void phaseEvent(Game game, Player player, PhaseType phase) {
        game.getPhaseHandler().devModeSet(phase, player);
        game.getTriggerHandler().resetActiveTriggers();
        game.getTriggerHandler().runTrigger(TriggerType.Phase, AbilityKey.mapFromPlayer(player), false);
        resolveTriggers(game);
    }

    @Test
    public void lastDuelRequiresOneLegalTargetPerGraveyardIfAble() {
        Game game = customGame();
        Player p = game.getPlayers().get(0), other = game.getPlayers().get(1);
        SpellAbility duel = spell("Last Duel", p);
        assertEquals(duel.getMinTargets(), 0);
        Card own = addCardToZone("Couchpotato", p, ZoneType.Graveyard);
        Card spare = addCardToZone("Puddinghead", p, ZoneType.Graveyard);
        Card theirs = addCardToZone("Drunken Warrior", other, ZoneType.Graveyard);
        assertEquals(duel.getMinTargets(), 2);
        assertEquals(duel.getMaxTargets(), 2);
        duel.getTargets().add(own);
        assertFalse(duel.isTargetNumberValid());
        assertFalse(duel.canTarget(spare));
        assertTrue(duel.canTarget(theirs));
        duel.getTargets().add(theirs);
        assertTrue(duel.isTargetNumberValid());
        AbilityUtils.resolve(duel);
        for (Player owner : game.getPlayers()) {
            Card returned = owner.getCardsIn(ZoneType.Battlefield).getFirst();
            assertEquals(returned.getOwner(), owner);
            assertEquals(returned.getController(), owner);
            assertEquals(returned.getCounters(CounterEnumType.FINALITY), 1);
        }
        assertTrue(spare.isInZone(ZoneType.Graveyard));
        addCard("Ground Seal", p);
        game.getAction().checkStaticAbilities();
        SpellAbility protectedDuel = spell("Last Duel", p);
        assertEquals(protectedDuel.getMinTargets(), 0);
    }

    @Test
    public void numbingResurrectionReturnsBothSidesAndFinalitySurvivesAbilityLoss() {
        Game game = customGame();
        Player p = game.getPlayers().get(0), other = game.getPlayers().get(1);
        addCardToZone("Drunken Warrior", p, ZoneType.Graveyard);
        addCardToZone("Caring Siren", other, ZoneType.Graveyard);
        Card enchantment = enter("Numbing Mass Resurrection", p);
        resolveTriggers(game);
        game.getAction().checkStaticAbilities();
        Card warrior = p.getCreaturesInPlay().getFirst();
        Card siren = other.getCreaturesInPlay().getFirst();
        assertEquals(warrior.getCounters(CounterEnumType.FINALITY), 1);
        assertEquals(siren.getCounters(CounterEnumType.FINALITY), 1);
        assertFalse(warrior.hasKeyword("Haste"));
        assertFalse(siren.hasKeyword("Flying"));
        Card later = addCardToZone("Couchpotato", other, ZoneType.Graveyard);
        game.getAction().checkStaticAbilities();
        assertTrue(later.isInZone(ZoneType.Graveyard));
        resolveEffect("DB$ Destroy | Defined$ Self", warrior);
        assertTrue(warrior.isInZone(ZoneType.Exile));
        game.getAction().moveTo(ZoneType.Graveyard, enchantment, null, null);
        game.getAction().checkStaticAbilities();
        assertTrue(siren.hasKeyword("Flying"));
    }

    @Test
    public void mysteriousRemedyStealsOpponentCreatureWithFinality() {
        Game game = customGame();
        Player p = game.getPlayers().get(0), other = game.getPlayers().get(1);
        Card target = addCardToZone("Couchpotato", other, ZoneType.Graveyard);
        SpellAbility remedy = spell("Mysterious Remedy", p);
        remedy.getTargets().add(target);
        AbilityUtils.resolve(remedy);
        Card returned = p.getCreaturesInPlay().getFirst();
        assertEquals(returned.getOwner(), other);
        assertEquals(returned.getController(), p);
        assertEquals(returned.getCounters(CounterEnumType.FINALITY), 1);
    }

    @Test
    public void owizahrerDrawsOnlyAfterTappingAndStunningOneOtherCreature() {
        for (boolean keep : new boolean[] {false, true}) {
            Game game = customGame();
            Player p = game.getPlayers().get(0);
            Card owl = enter("Owizahrer", p);
            Card helper = addCard("Couchpotato", p);
            addCardToZone("Island", p, ZoneType.Library);
            PlayerController choices = spy(p.getController());
            doReturn(keep ? new forge.game.card.CardCollection(helper) : forge.game.card.CardCollection.EMPTY)
                    .when(choices).chooseCardsForEffect(any(), any(), anyString(), anyInt(), anyInt(), anyBoolean(), any());
            p.dangerouslySetController(choices);
            phaseEvent(game, p, PhaseType.UPKEEP);
            assertEquals(helper.isTapped(), keep);
            assertEquals(helper.getCounters(CounterEnumType.STUN), keep ? 1 : 0);
            assertEquals(p.getCardsIn(ZoneType.Hand).size(), keep ? 1 : 0);
            assertEquals(owl.isInZone(ZoneType.Battlefield), keep);
        }
    }

    @Test
    public void soulEaterCountsAllCounterKindsOnBothSidesButNotNoncreatures() {
        Game game = customGame();
        Player p = game.getPlayers().get(0), other = game.getPlayers().get(1);
        Card own = addCard("Couchpotato", p), theirs = addCard("Couchpotato", other);
        counters(own, CounterEnumType.STUN, 2);
        counters(theirs, CounterEnumType.OBSESSION, 3);
        Card artifact = addCard("Blue Cow", p);
        counters(artifact, CounterEnumType.CHARGE, 7);
        Card siren = addCard("Soul Eater Siren", p);
        game.getAction().checkStaticAbilities();
        assertEquals(siren.getNetPower(), 5);
        assertEquals(siren.getNetToughness(), 5);
        counters(own, CounterEnumType.FINALITY, 1);
        game.getAction().checkStaticAbilities();
        assertEquals(siren.getNetPower(), 6);
    }

    @Test
    public void hangryGamesTransformsExistingCreaturesAndCreatesThreeHastySirens() {
        Game game = customGame();
        Player p = game.getPlayers().get(0), other = game.getPlayers().get(1);
        Card own = addCard("Darksteel Myr", p), theirs = addCard("Caring Siren", other);
        AbilityUtils.resolve(spell("The Hangry Games", p));
        game.getAction().checkStaticAbilities();
        for (Card creature : new Card[] {own, theirs}) {
            assertEquals(creature.getNetPower(), 1);
            assertEquals(creature.getNetToughness(), 1);
            assertTrue(creature.getType().hasSubtype("Siren"));
            assertTrue(creature.hasKeyword("Haste"));
            assertFalse(creature.hasKeyword("Flying"));
            assertFalse(creature.hasKeyword("Indestructible"));
        }
        assertEquals(p.getCreaturesInPlay().stream().filter(c -> c.isToken() && c.getName().equals("Hangry Siren")).count(), 3L);
        game.getEndOfTurn().executeUntil();
        game.getAction().checkStaticAbilities();
        assertTrue(own.hasKeyword("Indestructible"));
        assertEquals(own.getNetPower(), 0);
    }

    @Test
    public void echoMovesEveryCounterKindAndLibraryMovesOnlyOneChosenCounter() {
        Game game = customGame();
        Player p = game.getPlayers().get(0), other = game.getPlayers().get(1);
        Card source = addCard("Couchpotato", other), target = addCard("Couchpotato", p);
        counters(source, CounterEnumType.STUN, 2);
        counters(source, CounterEnumType.OBSESSION, 3);
        SpellAbility echo = spell("Echo of Empathy", p);
        echo.getTargets().add(source); echo.getTargets().add(target);
        AbilityUtils.resolve(echo);
        assertEquals(source.getCounters(CounterEnumType.STUN), 0);
        assertEquals(source.getCounters(CounterEnumType.OBSESSION), 0);
        assertEquals(target.getCounters(CounterEnumType.STUN), 2);
        assertEquals(target.getCounters(CounterEnumType.OBSESSION), 3);
        Card library = addCard("Library of Shared Memories", p);
        Card second = addCard("Couchpotato", p);
        SpellAbility move = library.getSpellAbilities().stream().filter(sa -> sa.getApi() == ApiType.MoveCounter).findFirst().orElseThrow();
        move.setActivatingPlayer(p); move.getTargets().add(target); move.getTargets().add(second);
        AbilityUtils.resolve(move);
        assertEquals(target.getCounters().size(), 4);
        assertEquals(second.getCounters().size(), 1);
    }

    @Test
    public void rottenCellarTriggersForOpponentsNontokenFoodAndDoesNotGainLife() {
        Game game = customGame();
        Player p = game.getPlayers().get(0), other = game.getPlayers().get(1);
        p.setLife(10, null); other.setLife(10, null);
        enter("Rotten Cellar", p);
        Card food = addCard("Couchpotato", other);
        resolveEffect("DB$ Sacrifice | Defined$ Self", food);
        resolveTriggers(game);
        assertEquals(other.getLife(), 9);
        assertEquals(p.getLife(), 10);
    }

    @Test
    public void happyHourMakesTwoBeersPerPlayerAndObsessesAllCreatures() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        for (Player player : game.getPlayers()) addCard("Couchpotato", player);
        AbilityUtils.resolve(spell("Happy Hour", p));
        for (Player player : game.getPlayers()) {
            assertEquals(player.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.isToken() && c.getName().equals("Beer")).count(), 2L);
            assertEquals(player.getCreaturesInPlay().getFirst().getCounters(CounterEnumType.OBSESSION), 1);
        }
    }

    @Test
    public void twinsOnlyGrowWhileTheNamedOtherTwinIsControlled() {
        Game game = customGame();
        Player p = game.getPlayers().get(0), other = game.getPlayers().get(1);
        Card steve = addCard("Ogrontwin Steve", p), enemy = addCard("Ogrontwin Pete", other);
        game.getAction().checkStaticAbilities();
        assertEquals(steve.getNetPower(), 3);
        Card pete = addCard("Ogrontwin Pete", p);
        game.getAction().checkStaticAbilities();
        assertEquals(steve.getNetPower(), 6);
        assertEquals(pete.getNetToughness(), 6);
        game.getAction().moveTo(ZoneType.Graveyard, pete, null, null);
        game.getAction().checkStaticAbilities();
        assertEquals(steve.getNetPower(), 3);
        assertEquals(enemy.getNetPower(), 4);
    }

    @Test
    public void blueCowOnlySacrificesAfterLastChargeIsRemoved() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        Card cow = enter("Blue Cow", p);
        assertEquals(cow.getCounters(CounterEnumType.CHARGE), 3);
        cow.subtractCounter(CounterEnumType.CHARGE, 2, p);
        resolveTriggers(game);
        assertTrue(cow.isInZone(ZoneType.Battlefield));
        cow.subtractCounter(CounterEnumType.CHARGE, 1, p);
        resolveTriggers(game);
        assertTrue(cow.isInZone(ZoneType.Graveyard));
    }

    @Test
    public void vandalismDoesNotDrainIfThePermanentCannotBeDestroyed() {
        for (boolean indestructible : new boolean[] {false, true}) {
            Game game = customGame();
            Player p = game.getPlayers().get(0), other = game.getPlayers().get(1);
            other.setLife(10, null);
            Card target = addCard(indestructible ? "Darksteel Ingot" : "Sol Ring", other);
            SpellAbility vandalism = spell("Vandalism", p); vandalism.getTargets().add(target);
            AbilityUtils.resolve(vandalism);
            assertEquals(other.getLife(), indestructible ? 10 : 8);
            assertEquals(target.isInZone(ZoneType.Graveyard), !indestructible);
        }
    }

    @Test
    public void endlessExpectationsUsesTheOldCounterCountAndSacrificesWhenShort() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        for (int n = 0; n < 3; n++) addCardToZone("Island", p, ZoneType.Library);
        Card expectation = enter("Endless Expectations", p);
        phaseEvent(game, p, PhaseType.END_OF_TURN);
        assertEquals(expectation.getCounters(CounterEnumType.FULFILLMENT), 1);
        assertEquals(p.getCardsIn(ZoneType.Hand).size(), 1);
        Card land = addCard("Island", p);
        phaseEvent(game, p, PhaseType.END_OF_TURN);
        assertEquals(expectation.getCounters(CounterEnumType.FULFILLMENT), 2);
        assertEquals(p.getCardsIn(ZoneType.Hand).size(), 2);
        phaseEvent(game, p, PhaseType.END_OF_TURN);
        assertTrue(expectation.isInZone(ZoneType.Graveyard));
        assertTrue(land.isUntapped());
    }

    @Test
    public void confusingSignalsDrawsOnlyForNewlyTappedCreaturesAndFixesChosenPlayer() {
        Game game = customGame();
        Player p = game.getPlayers().get(0), other = game.getPlayers().get(1);
        Card first = addCard("Couchpotato", other), second = addCard("Couchpotato", other);
        second.tap(true, null, other);
        addCardToZone("Island", other, ZoneType.Library);
        SpellAbility signals = spell("Confusing Signals", p);
        signals.getHostCard().setChosenPlayer(other);
        assertFalse(signals.canTarget(addCard("Couchpotato", p)));
        signals.getTargets().add(first); signals.getTargets().add(second);
        AbilityUtils.resolve(signals);
        assertEquals(other.getCardsIn(ZoneType.Hand).size(), 1);
        assertEquals(first.getCounters(CounterEnumType.STUN), 1);
        assertEquals(second.getCounters(CounterEnumType.STUN), 1);
    }

    private void resolveEffect(String text, Card host) {
        SpellAbility effect = AbilityFactory.getAbility(text, host);
        effect.setActivatingPlayer(host.getController());
        AbilityUtils.resolve(effect);
    }

    @Test
    public void academicQuarterPaysPlotNowAndOnlyCastsForFreeOnALaterTurn() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        Card first = addCardToZone("Couchpotato", p, ZoneType.Hand);
        Card second = addCardToZone("Couchpotato", p, ZoneType.Hand);
        addCardToZone("Island", p, ZoneType.Library);
        addCards("Swamp", 2, p);
        AbilityUtils.resolve(spell("Academic Quarter", p));
        game.getAction().checkStaticAbilities();
        SpellAbility plot = first.getSpellAbilities().stream().filter(SpellAbility::isPlotting).findFirst().orElseThrow();
        plot.setActivatingPlayer(p);
        assertTrue(plot.canPlay());
        assertTrue(ComputerUtil.playNoStack(p, plot, game, false));
        Card exiled = p.getCardsIn(ZoneType.Exile).getFirst();
        assertTrue(exiled.isPlotted());
        assertEquals(p.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getName().equals("Swamp") && c.isTapped()).count(), 2L);
        assertTrue(game.getCardsIn(ZoneType.Stack).stream().noneMatch(c -> c.getName().equals("Couchpotato")));
        SpellAbility again = second.getSpellAbilities().stream().filter(SpellAbility::isPlotting).findFirst().orElseThrow();
        again.setActivatingPlayer(p);
        assertFalse(again.canPlay(), "A normal trigger would leave a window for a second Plot; allowance must be consumed immediately.");
        SpellAbility cast = exiled.getFirstSpellAbility(); cast.setActivatingPlayer(p);
        assertTrue(forge.game.GameActionUtil.getAlternativeCosts(cast, p, false).stream()
                .noneMatch(sa -> sa.isAlternativeCost(forge.game.spellability.AlternativeCost.Plotted)));
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p, 3);
        SpellAbility free = forge.game.GameActionUtil.getAlternativeCosts(cast, p, false).stream()
                .filter(sa -> sa.isAlternativeCost(forge.game.spellability.AlternativeCost.Plotted)).findFirst().orElseThrow();
        assertEquals(free.getPayCosts().getTotalMana().getCMC(), 0);
        assertTrue(ComputerUtil.playNoStack(p, free, game, false));
        assertEquals(p.getCreaturesInPlay().getFirst().getName(), "Couchpotato");
    }

    @Test
    public void twoAcademicQuartersGrantTwoSeparateAllowancesAndExpire() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        for (int n = 0; n < 2; n++) {
            addCardToZone("Island", p, ZoneType.Library);
            AbilityUtils.resolve(spell("Academic Quarter", p));
        }
        assertNotNull(ChaosBrawlRules.grantedPlotAllowance(p));
        ChaosBrawlRules.consumeGrantedPlotAllowance(p);
        assertNotNull(ChaosBrawlRules.grantedPlotAllowance(p));
        ChaosBrawlRules.consumeGrantedPlotAllowance(p);
        assertNull(ChaosBrawlRules.grantedPlotAllowance(p));
        addCardToZone("Island", p, ZoneType.Library);
        AbilityUtils.resolve(spell("Academic Quarter", p));
        game.getEndOfTurn().executeUntil();
        assertNull(ChaosBrawlRules.grantedPlotAllowance(p));
    }

    @Test
    public void tacticalPurgeBlanksPermanentlyAndItsEtbShieldExpires() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        Card creature = addCard("Caring Siren", p);
        Card auraCard = addCardToZone("Tactical Purge", p, ZoneType.Hand);
        SpellAbility castAura = auraCard.getFirstSpellAbility();
        castAura.setActivatingPlayer(p); castAura.getTargets().add(creature);
        castAura.setHostCard(game.getAction().moveToStack(auraCard, castAura));
        AbilityUtils.resolve(castAura);
        Card aura = castAura.getHostCard();
        assertTrue(aura.isInZone(ZoneType.Battlefield), "Aura must be on battlefield");
        assertTrue(aura.isAttachedToEntity(creature), "Aura must attach before ETB resolution");
        game.getAction().checkStaticAbilities(); resolveTriggers(game);
        assertFalse(creature.hasKeyword("Flying"));
        assertTrue(creature.hasKeyword("Indestructible"));
        game.getEndOfTurn().executeUntil(); game.getAction().checkStaticAbilities();
        assertFalse(creature.hasKeyword("Flying"));
        assertFalse(creature.hasKeyword("Indestructible"));
        game.getAction().moveTo(ZoneType.Graveyard, aura, null, null);
        game.getAction().checkStaticAbilities();
        assertTrue(creature.hasKeyword("Flying"));
    }

    @Test
    public void overdoseRevivalAddsFinalityAndRequiresManaOnEachUpkeep() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        Card dead = addCardToZone("Couchpotato", p, ZoneType.Graveyard);
        SpellAbility revive = spell("Overdose Revival", p); revive.getTargets().add(dead);
        AbilityUtils.resolve(revive);
        Card returned = p.getCreaturesInPlay().getFirst();
        assertEquals(returned.getCounters(CounterEnumType.FINALITY), 1);
        phaseEvent(game, p, PhaseType.UPKEEP);
        assertTrue(returned.isInZone(ZoneType.Exile));
    }

    @Test
    public void pumpkinSacrificesOnlyDrunkenCrewAndCreatesSeparateDamageTriggers() {
        Game game = customGame();
        Player p = game.getPlayers().get(0), other = game.getPlayers().get(1);
        other.setLife(10, null);
        Card catapult = enter("Pumpkin Catapult", p); resolveTriggers(game);
        Card first = addCard("Couchpotato", p), second = addCard("Couchpotato", p), outsider = addCard("Couchpotato", p);
        counters(first, CounterEnumType.DRUNKEN, 1); counters(second, CounterEnumType.DRUNKEN, 2); counters(outsider, CounterEnumType.DRUNKEN, 1);
        PlayerController choices = spy(p.getController());
        doReturn(true).when(choices).confirmAction(any(), any(), anyString(), any());
        doAnswer(invocation -> {
            forge.game.card.CardCollectionView eligible = invocation.getArgument(3);
            assertTrue(eligible.contains(first)); assertTrue(eligible.contains(second)); assertFalse(eligible.contains(outsider));
            return new forge.game.card.CardCollection(java.util.List.of(first, second));
        }).when(choices).choosePermanentsToSacrifice(any(), anyInt(), anyInt(), any(), anyString());
        java.util.concurrent.atomic.AtomicInteger damages = new java.util.concurrent.atomic.AtomicInteger();
        doAnswer(invocation -> {
            SpellAbility damage = invocation.getArgument(0);
            if (damage.usesTargeting()) {
                damages.incrementAndGet(); damage.getTargets().add(other); return true;
            }
            return invocation.callRealMethod();
        }).when(choices).chooseTargetsFor(any());
        p.dangerouslySetController(choices);
        game.getAction().checkStateEffects(true);
        java.util.Map<AbilityKey, Object> event = AbilityKey.mapFromCard(catapult);
        event.put(AbilityKey.Crew, new forge.game.card.CardCollection(java.util.List.of(first, second)));
        game.getTriggerHandler().runTrigger(TriggerType.BecomesCrewed, event, false);
        resolveTriggers(game);
        assertTrue(first.isInZone(ZoneType.Graveyard), "Crew trigger must sacrifice the selected crew");
        game.getAction().checkStateEffects(true);
        resolveTriggers(game);
        assertEquals(other.getLife(), 6, "Two independent 2-damage triggers must resolve");
        assertTrue(first.isInZone(ZoneType.Graveyard)); assertTrue(second.isInZone(ZoneType.Graveyard));
        assertTrue(outsider.isInZone(ZoneType.Battlefield));
    }

    @Test
    public void pumpkinDriverActuallyCrewsThreeAlone() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        Card catapult = enter("Pumpkin Catapult", p); resolveTriggers(game);
        Card driver = p.getCreaturesInPlay().stream().filter(c -> c.getName().equals("Pumpkin Gang Driver")).findFirst().orElseThrow();
        assertEquals(driver.getNetPower(), 1);
        SpellAbility crew = catapult.getSpellAbilities().stream().filter(SpellAbility::isCrew).findFirst().orElseThrow();
        crew.setActivatingPlayer(p);
        assertTrue(ComputerUtil.playNoStack(p, crew, game, false));
        assertTrue(driver.isTapped());
        assertTrue(catapult.isCreature());
        assertEquals(catapult.getNetPower(), 7);
    }

    @Test
    public void youthWardDrawCostNamesTheCreatureControllerNotTheAuraOwner() {
        Game game = customGame();
        Player p = game.getPlayers().get(0), other = game.getPlayers().get(1);
        Card creature = addCard("Couchpotato", other), aura = addCard("Youthful Inner Heat", p);
        aura.attachToEntity(creature, null); game.getAction().checkStaticAbilities();
        assertTrue(creature.getKeywords().stream().anyMatch(k -> k.getOriginal().startsWith("Ward:")));
        SpellAbility ward = AbilityFactory.getAbility("DB$ Counter | Defined$ TriggeredSourceSA | UnlessCost$ Draw<1/Player.Activator> | UnlessPayer$ TriggeredSourceSAController", creature);
        ward.setActivatingPlayer(other);
        forge.game.cost.CostDraw draw = (forge.game.cost.CostDraw)new forge.game.cost.Cost("Draw<1/Player.Activator>", true).getCostParts().get(0);
        assertEquals(draw.getPotentialPlayers(p, ward).size(), 1);
        assertEquals(draw.getPotentialPlayers(p, ward).getFirst(), other);
        addCardToZone("Island", other, ZoneType.Library);
        forge.game.cost.PaymentDecision payment = forge.game.cost.PaymentDecision.players(java.util.List.of(other));
        payment.c = 1;
        assertTrue(draw.payAsDecided(p, payment, ward, true));
        assertEquals(other.getCardsIn(ZoneType.Hand).size(), 1);
    }

    @Test
    public void windtongueMakesThreeRealBlueBlackAjdeCardsAndAjdeHasZeroDeckLimit() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        enter("Distant Cousin Windtongue", p); resolveTriggers(game);
        assertEquals(p.getCardsIn(ZoneType.Library).size(), 3);
        for (Card card : p.getCardsIn(ZoneType.Library)) {
            assertEquals(card.getName(), "AJDE!"); assertFalse(card.isToken());
            assertEquals(card.getManaCost().getCMC(), 0);
            assertTrue(card.getColor().hasBlue()); assertTrue(card.getColor().hasBlack());
            assertEquals(card.getRules().getKeywordMagnitude("DeckLimit").intValue(), 0);
        }
    }

    @Test
    public void coldSplashRemovesOnlyStunAndObsessionAndNoMoreThanTwoTotal() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        Card target = addCard("Couchpotato", p);
        counters(target, CounterEnumType.STUN, 1); counters(target, CounterEnumType.OBSESSION, 1);
        counters(target, CounterEnumType.P1P1, 3); counters(target, CounterEnumType.DRUNKEN, 1);
        addCardToZone("Island", p, ZoneType.Library);
        PlayerController choices = spy(p.getController());
        doAnswer(i -> ((java.util.List<?>)i.getArgument(0)).get(0)).when(choices).chooseCounterType(anyList(), any(), anyString(), anyMap());
        doAnswer(i -> i.getArgument(3)).when(choices).chooseNumber(any(), anyString(), anyInt(), anyInt(), anyMap());
        p.dangerouslySetController(choices);
        SpellAbility splash = spell("Cold Splash", p); splash.getTargets().add(target);
        AbilityUtils.resolve(splash);
        assertEquals(target.getCounters(CounterEnumType.STUN), 0);
        assertEquals(target.getCounters(CounterEnumType.OBSESSION), 0);
        assertEquals(target.getCounters(CounterEnumType.P1P1), 3);
        assertEquals(target.getCounters(CounterEnumType.DRUNKEN), 1);
        assertEquals(p.getCardsIn(ZoneType.Hand).size(), 1);
    }

    @Test
    public void natalieUpkeepTaxCanBeReducedOrRemovedWithoutTaxingNatalie() {
        for (String helper : new String[] {"none", "Shady Herb Dealer", "Süüü, the Fourth Child"}) {
            Game game = customGame();
            Player p = game.getPlayers().get(0);
            addCard("Caring Siren", p);
            enter("Natalie, Mother Siren", p); resolveTriggers(game);
            if (!helper.equals("none")) addCard(helper, p);
            game.getAction().checkStaticAbilities();
            long before = p.getCreaturesInPlay().stream().filter(c -> c.getType().hasSubtype("Siren")).count();
            assertTrue(before >= 5);
            phaseEvent(game, p, PhaseType.UPKEEP);
            long after = p.getCreaturesInPlay().stream().filter(c -> c.getType().hasSubtype("Siren")).count();
            assertEquals(after, helper.equals("none") ? 1L : before, helper);
            assertTrue(p.getCreaturesInPlay().stream().anyMatch(c -> c.getName().equals("Natalie, Mother Siren")));
        }
    }

    @Test
    public void shadyDealerCanReduceAColoredSymbolButPreservesNonmanaCosts() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        addCard("Shady Herb Dealer", p);
        Card host = addCard("Couchpotato", p);
        PlayerController choices = spy(p.getController());
        doAnswer(i -> ((java.util.Collection<String>)i.getArgument(2)).stream().filter(x -> x.contains("B")).findFirst().orElseThrow())
                .when(choices).chooseSomeType(anyString(), any(), anyCollection(), anyBoolean());
        p.dangerouslySetController(choices);
        SpellAbility upkeep = AbilityFactory.getAbility("DB$ Sacrifice | Defined$ Self | UnlessCost$ 2 B PayLife<1> | UnlessPayer$ You | ChaosBrawlUpkeep$ True", host);
        upkeep.setActivatingPlayer(p);
        forge.game.cost.Cost reduced = AbilityUtils.calculateUnlessCost(upkeep, upkeep.getParam("UnlessCost"), true);
        assertEquals(reduced.getTotalMana().getCMC(), 2);
        assertEquals(reduced.getTotalMana().getGenericCost(), 2);
        assertTrue(reduced.getCostParts().stream().anyMatch(part -> part instanceof forge.game.cost.CostPayLife));
        forge.game.cost.Cost queried = AbilityUtils.calculateUnlessCost(upkeep, upkeep.getParam("UnlessCost"), false);
        assertEquals(queried.getTotalMana(), reduced.getTotalMana());
        game.getRules().setChaosBrawl(false);
        assertEquals(AbilityUtils.calculateUnlessCost(upkeep, upkeep.getParam("UnlessCost"), true).getTotalMana().getCMC(), 3);
    }

    @Test
    public void loanSyndicateManaIsRestrictedAndActuallyPaysResurrectionUpkeep() {
        Game game = customGame();
        Player p = game.getPlayers().get(0);
        Card loan = addCard("The Loan Syndicate", p); addCard("Swamp", p);
        SpellAbility mana = loan.getManaAbilities().stream().filter(sa -> "2".equals(sa.getParam("Amount"))).findFirst().orElseThrow();
        SpellAbility ordinary = spell("Buy a Round", p);
        assertFalse(mana.getManaPart().meetsManaRestrictions(ordinary));
        Card dead = addCardToZone("Couchpotato", p, ZoneType.Graveyard);
        SpellAbility revive = spell("Overdose Revival", p); revive.getTargets().add(dead); AbilityUtils.resolve(revive);
        Card creature = p.getCreaturesInPlay().getFirst();
        SpellAbility upkeep = AbilityFactory.getAbility("DB$ Sacrifice | Defined$ Self | UnlessCost$ 2 B | UnlessPayer$ You | ChaosBrawlUpkeep$ True", creature);
        upkeep.setActivatingPlayer(p);
        assertTrue(mana.getManaPart().meetsManaRestrictions(upkeep));
        phaseEvent(game, p, PhaseType.UPKEEP);
        assertTrue(creature.isInZone(ZoneType.Battlefield));
        assertTrue(loan.isTapped());
    }

    @Test
    public void loyalWatchdogOnlyBuffsItsOriginallyDeclaredAttackingPair() {
        Game game = customGame();
        Player p = game.getPlayers().get(0), other = game.getPlayers().get(1);
        Card dog = addCard("Loyal Watchdog", p), companion = addCard("Couchpotato", p), late = addCard("Weedfield Herbalist", p);
        for (Card c : new Card[] {dog, companion, late}) c.tap(true, null, p);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, p);
        Combat combat = new Combat(p); game.getPhaseHandler().setCombat(combat); combat.addAttacker(dog, other); combat.addAttacker(companion, other);
        game.getTriggerHandler().resetActiveTriggers();
        CombatUtil.checkDeclaredAttacker(game, dog, combat, true);
        combat.addAttacker(late, other);
        resolveTriggers(game);
        assertFalse(dog.isTapped()); assertFalse(companion.isTapped()); assertTrue(late.isTapped());
        assertEquals(dog.getNetPower(), 3); assertEquals(companion.getNetPower(), 1); assertEquals(late.getNetPower(), 1);
    }

    @Test
    public void goldenStethoscopeExilePermissionAndManaConversionSurviveEquipmentLeaving() {
        Game game = customGame();
        Player p = game.getPlayers().get(0), other = game.getPlayers().get(1);
        addCards("Island", 3, p);
        for (int n = 0; n < 2; n++) addCardToZone("Raging Alcoholic", other, ZoneType.Library);
        Card attacker = addCard("Couchpotato", p), equipment = addCard("Golden Stethoscope", p);
        equipment.attachToEntity(attacker, null);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, p);
        Combat combat = new Combat(p); game.getPhaseHandler().setCombat(combat); combat.addAttacker(attacker, other);
        game.getTriggerHandler().resetActiveTriggers(); CombatUtil.checkDeclaredAttacker(game, attacker, combat, true);
        resolveTriggers(game);
        assertEquals(other.getCardsIn(ZoneType.Library).size(), 1);
        Card exiled = game.getCardsIn(ZoneType.Exile).getFirst();
        assertFalse(exiled.mayPlay(p).isEmpty());
        game.getAction().moveTo(ZoneType.Graveyard, equipment, null, null); game.getAction().checkStaticAbilities();
        assertFalse(exiled.mayPlay(p).isEmpty());
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        SpellAbility cast = exiled.getFirstSpellAbility(); cast.setActivatingPlayer(p); cast.setMayPlay(exiled.mayPlay(p).get(0));
        assertTrue(ComputerUtil.playNoStack(p, cast, game, false), "Three Islands must pay {1}{R}{W} through the remembered-card mana conversion");
        assertTrue(p.getCreaturesInPlay().stream().anyMatch(c -> c.getName().equals("Raging Alcoholic")));
    }

    @Test
    public void hangoverRemovesEveryOldCounterBeforeAddingExactlyThreeStun() {
        Game game = customGame(); Player p = game.getPlayers().get(0);
        Card target = addCard("Couchpotato", p); counters(target, CounterEnumType.OBSESSION, 2); counters(target, CounterEnumType.P1P1, 3);
        SpellAbility hangover = spell("Hangover", p); hangover.getTargets().add(target); AbilityUtils.resolve(hangover);
        assertTrue(target.isTapped()); assertEquals(target.getCounters(CounterEnumType.STUN), 3);
        assertEquals(target.getCounters(CounterEnumType.OBSESSION), 0); assertEquals(target.getCounters(CounterEnumType.P1P1), 0);
    }

    @Test
    public void blightborneColossusPaysItsCounterUpkeepAndUntamableEntersStunned() {
        Game game = customGame(); Player p = game.getPlayers().get(0);
        Card blight = enter("Blightborne Colossus", p);
        PlayerController choices = spy(p.getController());
        doAnswer(i -> new forge.game.cost.CostPayment(i.getArgument(0), i.getArgument(1))
                .payComputerCosts(new forge.ai.AiCostDecision(p, i.getArgument(1), true)))
                .when(choices).payCostToPreventEffect(any(), any(), anyBoolean(), any());
        p.dangerouslySetController(choices);
        phaseEvent(game, p, PhaseType.UPKEEP);
        assertTrue(blight.isInZone(ZoneType.Battlefield)); assertEquals(blight.getCounters(CounterEnumType.M1M1), 1);
        Card grove = enter("Untamable Grove Colossus", p);
        assertTrue(grove.isTapped()); assertEquals(grove.getCounters(CounterEnumType.STUN), 3);
        assertEquals(grove.getNetPower(), 8);
    }

    @Test
    public void stragglerEntersTappedThroughARealReplacementEffect() {
        Game game = customGame();
        Card straggler = enter("Straggler", game.getPlayers().get(0));
        assertTrue(straggler.isTapped());
    }

    @Test
    public void betaNameCollisionsPreserveOfficialRules() {
        Game game = customGame(); Player p = game.getPlayers().get(0);
        Card recycle = createCard("Recycle", p), fallen = createCard("The Fallen", p), master = createCard("Dungeon Master", p);
        assertTrue(recycle.isSorcery()); assertEquals(recycle.getManaCost().getCMC(), 1);
        assertTrue(fallen.getType().hasSubtype("Orc")); assertEquals(fallen.getBasePower(), 3);
        assertTrue(master.isCreature()); assertFalse(master.isPlaneswalker());
        forge.item.PaperCard official = forge.StaticData.instance().getCommonCards().getAllCards("Recycle").stream()
                .filter(c -> c.getEdition().equals("TMP")).findFirst().orElseThrow();
        Card original = Card.fromPaperCard(official, p);
        assertTrue(original.isEnchantment()); assertEquals(original.getManaCost().getCMC(), 6);
        official = forge.StaticData.instance().getCommonCards().getAllCards("The Fallen").stream()
                .filter(c -> c.getEdition().equals("DRK")).findFirst().orElseThrow();
        original = Card.fromPaperCard(official, p);
        assertTrue(original.getType().hasSubtype("Zombie")); assertEquals(original.getBasePower(), 2);
    }

    @Test
    public void homemadeParanoiaPutsEveryRevealedLandTappedAndMillsTheRest() {
        Game game = customGame(); Player p = game.getPlayers().get(0);
        addCardToZone("Forest", p, ZoneType.Library); addCardToZone("Swamp", p, ZoneType.Library);
        addCardToZone("Critical (S)hit", p, ZoneType.Library);
        AbilityUtils.resolve(spell("Homemade Paranoia", p));
        assertEquals(p.getCardsIn(ZoneType.Battlefield).size(), 2);
        assertTrue(p.getCardsIn(ZoneType.Battlefield).stream().allMatch(c -> c.isLand() && c.isTapped()));
        assertEquals(p.getCardsIn(ZoneType.Graveyard).size(), 1); assertEquals(p.getCardsIn(ZoneType.Library).size(), 0);
    }

    @Test
    public void homemadeParanoiaWithACreatureKeepsOneCreatureAndMillsEvenLands() {
        Game game = customGame(); Player p = game.getPlayers().get(0);
        addCardToZone("Forest", p, ZoneType.Library); addCardToZone("Couchpotato", p, ZoneType.Library);
        addCardToZone("Critical (S)hit", p, ZoneType.Library);
        AbilityUtils.resolve(spell("Homemade Paranoia", p));
        assertEquals(p.getCardsIn(ZoneType.Hand).size(), 1); assertTrue(p.getCardsIn(ZoneType.Hand).getFirst().isCreature());
        assertEquals(p.getCardsIn(ZoneType.Graveyard).size(), 2); assertTrue(p.getCardsIn(ZoneType.Battlefield).isEmpty());
    }

    @Test
    public void draftTapAddsOneCounterToTheSacrificedBeersActualTarget() {
        Game game = customGame(); Player p = game.getPlayers().get(0);
        Card tap = enter("Draft Tap", p); resolveTriggers(game);
        assertEquals(countCardsWithName(game, "Beer"), 3);
        Card target = addCard("Couchpotato", p), beer = findCardWithName(game, "Beer");
        addCard("Swamp", p); game.getAction().checkStateEffects(true);
        SpellAbility drink = beer.getSpellAbilities().stream().filter(a -> a.getApi() == ApiType.PutCounter).findFirst().orElseThrow();
        drink.setActivatingPlayer(p); drink.getTargets().add(target);
        assertTrue(ComputerUtil.playNoStack(p, drink, game, false)); resolveTriggers(game);
        assertEquals(target.getCounters(CounterEnumType.DRUNKEN), 2);
        resolveEffect("DB$ Sacrifice | SacValid$ Beer.YouCtrl | Amount$ 1", tap); resolveTriggers(game);
        assertEquals(target.getCounters(CounterEnumType.DRUNKEN), 2, "A Beer sacrificed without a targeting ability adds no counter");
    }

    @Test
    public void fillEmUpUsesTheTargetsOwnTolerance() {
        for (String name : new String[] {"Couchpotato", "Brüder Hopfenblüte"}) {
            Game game = customGame(); Player p = game.getPlayers().get(0);
            Card target = addCard(name, p), host = createCard("Fill Em Up", p);
            SpellAbility drink = db(host, "Drunken"); drink.getTargets().add(target); AbilityUtils.resolve(drink);
            assertEquals(target.getCounters(CounterEnumType.DRUNKEN), ChaosBrawlRules.drunkenTolerance(target) + 1);
        }
    }

    @Test
    public void driverUsesAMinimumCrewPowerRatherThanAddingThree() {
        Game game = customGame(); Player p = game.getPlayers().get(0);
        Card driver = addCard("Designated Driver", p), vehicle = addCard("Experimental Invention", p);
        SpellAbility crew = vehicle.getSpellAbilities().stream().filter(SpellAbility::isCrew).findFirst().orElseThrow();
        assertEquals(driver.getNetPower(), 6);
        assertEquals(forge.game.staticability.StaticAbilityTapPowerValue.getMod(driver, crew), 0);
        for (int n = 0; n < 5; n++) resolveEffect("DB$ Token | TokenScript$ chaosbrawl_beer", driver);
        game.getAction().checkStaticAbilities(); assertEquals(driver.getNetPower(), 1);
        assertEquals(forge.game.staticability.StaticAbilityTapPowerValue.getMod(driver, crew), 2);
    }

    @Test
    public void ediConvertsEveryDrunkenCounterAndPreservesExistingPlusCounters() {
        Game game = customGame(); Player p = game.getPlayers().get(0);
        Card edi = enter("Edi, the Beerpig", p);
        counters(edi, CounterEnumType.P1P1, 2); counters(edi, CounterEnumType.DRUNKEN, 3);
        assertEquals(edi.getCounters(CounterEnumType.DRUNKEN), 0); assertEquals(edi.getCounters(CounterEnumType.P1P1), 5);
        assertEquals(edi.getNetPower(), 8);
    }

    @Test
    public void gonerEntersTappedAndCannotBeExiledFromBattlefieldOrGraveyard() {
        Game game = customGame(); Player p = game.getPlayers().get(0);
        Card goner = enter("Goner", p); assertTrue(goner.isTapped());
        goner = game.getAction().moveTo(ZoneType.Exile, goner, null, null); assertTrue(goner.isInZone(ZoneType.Graveyard));
        goner = game.getAction().moveTo(ZoneType.Exile, goner, null, null); assertTrue(goner.isInZone(ZoneType.Graveyard));
    }

    @Test
    public void recycleOnlyCountsActuallyExiledCardsAndDrawsForXFour() {
        Game game = customGame(); Player p = game.getPlayers().get(0);
        addCardToZone("Goner", p, ZoneType.Graveyard); addCardToZone("Forest", p, ZoneType.Graveyard);
        addCardToZone("Swamp", p, ZoneType.Graveyard); addCardToZone("Couchpotato", p, ZoneType.Graveyard);
        addCardToZone("Island", p, ZoneType.Library);
        SpellAbility recycle = spell("Recycle", p); recycle.setXManaCostPaid(4); assertEquals(AbilityUtils.calculateAmount(recycle.getHostCard(), "X", recycle), 4);
        AbilityUtils.resolve(recycle);
        assertEquals(p.getCardsIn(ZoneType.Exile).size(), 3, "Exiled cards; grave=" + p.getCardsIn(ZoneType.Graveyard));
        assertEquals(p.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.isToken() && c.getType().hasSubtype("Treasure")).count(), 3L); assertEquals(p.getCardsIn(ZoneType.Hand).size(), 1);
        assertEquals(p.getCardsIn(ZoneType.Graveyard).size(), 1);
    }

    @Test
    public void frozenTapsPreventsTapCostsAndSacrificesAtTenArtifacts() {
        Game game = customGame(); Player p = game.getPlayers().get(0);
        Card frozen = enter("Frozen Taps", p), beerHost = addCard("Couchpotato", p);
        resolveEffect("DB$ Token | TokenScript$ chaosbrawl_beer | TokenAmount$ 9", beerHost);
        game.getAction().checkStateEffects(true); resolveTriggers(game);
        assertTrue(frozen.isInZone(ZoneType.Battlefield));
        Card beer = findCardWithName(game, "Beer"); assertFalse(beer.canTap(false));
        resolveEffect("DB$ Token | TokenScript$ chaosbrawl_beer", beerHost);
        game.getAction().checkStateEffects(true); resolveTriggers(game);
        assertTrue(frozen.isInZone(ZoneType.Graveyard)); assertTrue(beer.canTap(false));
    }

    @Test
    public void theFallenReplacesBothOrdinaryDiscardAndDiscardCostsWithOneCounter() {
        for (boolean cost : new boolean[] {false, true}) {
            Game game = customGame(); Player p = game.getPlayers().get(0);
            Card fallen = addCardToZone("The Fallen", p, ZoneType.Hand);
            Card host = addCard("Couchpotato", p); game.getAction().checkStateEffects(true);
            if (cost) {
                SpellAbility activation = AbilityFactory.getAbility("AB$ Draw | Cost$ Discard<1/Card> | NumCards$ 0", host);
                activation.setActivatingPlayer(p); assertTrue(ComputerUtil.playNoStack(p, activation, game, false));
            } else resolveEffect("DB$ Discard | Mode$ TgtChoose | Defined$ You | NumCards$ 1", host);
            Card returned = p.getCreaturesInPlay().stream().filter(c -> c.getName().equals("The Fallen")).findFirst().orElseThrow();
            assertTrue(returned.isTapped()); assertEquals(returned.getCounters(CounterEnumType.DRUNKEN), 1);
        }
    }

    @Test
    public void hoardBeastCountsArtifactTokensOnlyOnBothSides() {
        Game game = customGame(); Player p = game.getPlayers().get(0), other = game.getPlayers().get(1);
        Card beast = enter("Hoard Beast", p); resolveTriggers(game); assertEquals(beast.getNetPower(), 3);
        Card host = addCard("Couchpotato", other);
        resolveEffect("DB$ Token | TokenScript$ chaosbrawl_beer", host);
        resolveEffect("DB$ Token | TokenScript$ chaosbrawl_hangry_siren", host);
        addCard("Darksteel Myr", other); game.getAction().checkStaticAbilities();
        assertEquals(beast.getNetPower(), 4);
    }

    @Test
    public void theMindReturnsTheTwoChosenCardsOnlyOnSharedManaValueOrType() {
        for (boolean match : new boolean[] {false, true}) {
            Game game = customGame(); Player p = game.getPlayers().get(0), other = game.getPlayers().get(1);
            addCardToZone("Couchpotato", p, ZoneType.Library); addCardToZone("Couchpotato", p, ZoneType.Library);
            addCardToZone(match ? "Wixbirne" : "Forest", other, ZoneType.Library);
            SpellAbility mind = spell("The Mind", p); mind.getTargets().add(other); AbilityUtils.resolve(mind);
            assertEquals(p.getCardsIn(ZoneType.Hand).size(), match ? 2 : 1);
            assertEquals(other.getCardsIn(ZoneType.Hand).size(), match ? 1 : 0);
        }
    }

    @Test
    public void ediPaysTwoRealBeerSacrificesAndEntersWithFourCounters() {
        Game game = customGame(); Player p = game.getPlayers().get(0);
        Card host = addCard("Couchpotato", p); resolveEffect("DB$ Token | TokenScript$ chaosbrawl_beer | TokenAmount$ 2", host);
        addCards("Plains", 4, p); Card edi = addCardToZone("Edi, the Beerpig", p, ZoneType.Hand);
        SpellAbility cast = edi.getFirstSpellAbility(); cast.setActivatingPlayer(p);
        PlayerController choices = spy(p.getController());
        doReturn(2).when(choices).chooseNumberForKeywordCost(any(), any(), any(), anyString(), anyInt());
        p.dangerouslySetController(choices);
        game.getAction().checkStateEffects(true);
        assertTrue(ComputerUtil.playNoStack(p, cast, game, false));
        Card played = p.getCreaturesInPlay().stream().filter(c -> c.getName().equals("Edi, the Beerpig")).findFirst().orElseThrow();
        assertEquals(countCardsWithName(game, "Beer"), 0); assertEquals(played.getCounters(CounterEnumType.P1P1), 4);
    }

    @Test
    public void grinderPaysLandSacrificeAndProducesExactlyTwoGreenMana() {
        Game game = customGame(); Player p = game.getPlayers().get(0);
        Card grinder = addCard("Grinder", p), land = addCard("Forest", p); game.getAction().checkStateEffects(true);
        SpellAbility mana = grinder.getSpellAbilities().stream().filter(SpellAbility::isManaAbility).findFirst().orElseThrow();
        mana.setActivatingPlayer(p); assertTrue(ComputerUtil.playNoStack(p, mana, game, false));
        assertTrue(grinder.isTapped()); assertTrue(land.isInZone(ZoneType.Graveyard)); assertEquals(p.getManaPool().totalMana(), 2);
    }

    @Test
    public void rotbornOnlyGetsUpkeepBonusFromATokenFoodAndDiesWithoutPayment() {
        for (int foodType = 0; foodType < 3; foodType++) {
            Game game = customGame(); Player p = game.getPlayers().get(0);
            Card rot = enter("Rotborn Colossus", p);
            if (foodType == 1) resolveEffect("DB$ Token | TokenScript$ c_a_food_sac", rot);
            if (foodType == 2) addCard("Couchpotato", p);
            PlayerController choices = spy(p.getController());
            doAnswer(i -> new forge.game.cost.CostPayment(i.getArgument(0), i.getArgument(1))
                    .payComputerCosts(new forge.ai.AiCostDecision(p, i.getArgument(1), true)))
                    .when(choices).payCostToPreventEffect(any(), any(), anyBoolean(), any());
            p.dangerouslySetController(choices); game.getAction().checkStateEffects(true);
            phaseEvent(game, p, PhaseType.UPKEEP);
            assertEquals(rot.isInZone(ZoneType.Battlefield), foodType > 0);
            if (foodType > 0) assertEquals(rot.getCounters(CounterEnumType.P1P1), foodType == 1 ? 2 : 0);
        }
    }

    @Test
    public void obsessionRemovalDrainsOncePerEventRatherThanPerCounter() {
        Game game = customGame(); Player p = game.getPlayers().get(0), other = game.getPlayers().get(1);
        Card duo = enter("Devious Duo", p), target = addCard("Couchpotato", other);
        counters(target, CounterEnumType.OBSESSION, 3); int life = other.getLife();
        SpellAbility remove = AbilityFactory.getAbility("DB$ RemoveCounter | ValidTgts$ Creature | CounterType$ OBSESSION | CounterNum$ All", duo);
        remove.setActivatingPlayer(p); remove.getTargets().add(target); AbilityUtils.resolve(remove); resolveTriggers(game);
        assertEquals(other.getLife(), life - 1);
    }

    @Test
    public void austrianSlapDealsFourOnlyWhenTheTargetIsDrunken() {
        for (boolean drunk : new boolean[] {false, true}) {
            Game game = customGame(); Player p = game.getPlayers().get(0);
            Card target = addCard("Couchpotato", p); counters(target, CounterEnumType.P1P1, 5);
            if (drunk) counters(target, CounterEnumType.DRUNKEN, 1);
            SpellAbility slap = spell("Austrian Slap", p); slap.getTargets().add(target); AbilityUtils.resolve(slap);
            assertEquals(target.getDamage(), drunk ? 4 : 2);
        }
    }

    @Test
    public void lecherousPatronTriggersForEachCounterButPuffMamaOnlyOncePerPlacement() {
        Game game = customGame(); Player p = game.getPlayers().get(0);
        Card patron = enter("Lecherous Patron", p), mama = enter("Puff Mama", p);
        counters(patron, CounterEnumType.DRUNKEN, 3); resolveTriggers(game);
        assertEquals(patron.getNetPower(), 11); // printed 2 + drunken 3 + three +2 triggers
        counters(patron, CounterEnumType.OBSESSION, 3); resolveTriggers(game);
        assertEquals(p.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.isToken() && c.getType().hasSubtype("Treasure")).count(), 1L);
    }

    @Test
    public void jollyBrewbellyPaysOneDrunkenCounterOrIsSacrificed() {
        Game game = customGame(); Player p = game.getPlayers().get(0);
        Card dwarf = enter("Jolly Brewbelly", p); assertEquals(dwarf.getCounters(CounterEnumType.DRUNKEN), 3);
        PlayerController choices = spy(p.getController());
        doAnswer(i -> new forge.game.cost.CostPayment(i.getArgument(0), i.getArgument(1))
                .payComputerCosts(new forge.ai.AiCostDecision(p, i.getArgument(1), true)))
                .when(choices).payCostToPreventEffect(any(), any(), anyBoolean(), any());
        p.dangerouslySetController(choices);
        phaseEvent(game, p, PhaseType.UPKEEP); assertEquals(dwarf.getCounters(CounterEnumType.DRUNKEN), 2);
        resolveEffect("DB$ RemoveCounter | Defined$ Self | CounterType$ DRUNKEN | CounterNum$ All", dwarf);
        phaseEvent(game, p, PhaseType.UPKEEP); assertTrue(dwarf.isInZone(ZoneType.Graveyard));
    }

    @Test
    public void locationAndDrunkenKeywordsUpdateContinuously() {
        Game game = customGame(); Player p = game.getPlayers().get(0);
        Card bird = addCard("Big Ball Bluebird", p), home = addCard("Devoted Homebody", p), drunk = addCard("Bad Drunk", p);
        assertFalse(bird.hasKeyword("Vigilance")); assertFalse(drunk.hasKeyword("Flying"));
        assertFalse(forge.game.staticability.StaticAbilityCantAttackBlock.canAttackDefender(home, game.getPlayers().get(1)));
        Card location = enter("The Blessed Hideaway", p); game.getAction().checkStaticAbilities();
        assertTrue(bird.hasKeyword("Vigilance")); assertTrue(forge.game.staticability.StaticAbilityCantAttackBlock.canAttackDefender(home, game.getPlayers().get(1)));
        counters(drunk, CounterEnumType.DRUNKEN, 1); game.getAction().checkStaticAbilities(); assertTrue(drunk.hasKeyword("Flying"));
    }

    @Test
    public void paidOverstimulatedUpkeepGrowsOnlyOnSuccessfulPayment() {
        Game game = customGame(); Player p = game.getPlayers().get(0);
        Card flora = enter("Overstimulated Flora", p); addCards("Forest", 2, p);
        PlayerController choices = spy(p.getController());
        doAnswer(i -> new forge.game.cost.CostPayment(i.getArgument(0), i.getArgument(1))
                .payComputerCosts(new forge.ai.AiCostDecision(p, i.getArgument(1), true)))
                .when(choices).payCostToPreventEffect(any(), any(), anyBoolean(), any()); p.dangerouslySetController(choices);
        phaseEvent(game, p, PhaseType.UPKEEP); assertEquals(flora.getCounters(CounterEnumType.P1P1), 1);
        phaseEvent(game, p, PhaseType.UPKEEP); assertTrue(flora.isInZone(ZoneType.Graveyard));
    }

    @Test
    public void copycatCopiesTheEventCounterMapRatherThanAllExistingCounters() {
        Game game = customGame(); Player p = game.getPlayers().get(0);
        Card copy = enter("Copycat", p), other = addCard("Couchpotato", p);
        assertTrue(copy.getType().isLegendary()); counters(other, CounterEnumType.P1P1, 2); resolveTriggers(game);
        assertEquals(copy.getCounters(CounterEnumType.P1P1), 2);
        GameEntityCounterTable table = new GameEntityCounterTable();
        other.addCounter(CounterEnumType.STUN, 2, p, table); other.addCounter(CounterEnumType.OBSESSION, 3, p, table);
        table.replaceCounterEffect(game, null); resolveTriggers(game);
        assertEquals(copy.getCounters(CounterEnumType.P1P1), 2);
        assertEquals(copy.getCounters(CounterEnumType.STUN), 2); assertEquals(copy.getCounters(CounterEnumType.OBSESSION), 3);
    }

    @Test
    public void sofaPizzaReturnPermissionTracksTheGraveyardObject() {
        Game game = customGame(); Player p = game.getPlayers().get(0);
        Card pizza = addCardToZone("Sofa Pizza", p, ZoneType.Hand); addCards("Swamp", 2, p);
        pizza = game.getAction().moveTo(ZoneType.Graveyard, pizza, null, null); resolveTriggers(game);
        SpellAbility back = pizza.getSpellAbilities().stream().filter(a -> a.getApi() == ApiType.ChangeZone).findFirst().orElseThrow();
        back.setActivatingPlayer(p); assertTrue(back.canPlay());
        assertTrue(ComputerUtil.playNoStack(p, back, game, false)); assertEquals(p.getCardsIn(ZoneType.Hand).size(), 1);
        assertEquals(p.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.isLand() && c.isTapped()).count(), 2L);
    }

    @Test
    public void honestLaborerRequiresAnActualAttackThisTurn() {
        Game game = customGame(); Player p = game.getPlayers().get(0), other = game.getPlayers().get(1);
        Card laborer = addCard("Honest Laborer", p); laborer.setSickness(false);
        SpellAbility beer = laborer.getSpellAbilities().stream().filter(c -> c.getApi() == ApiType.Token).findFirst().orElseThrow();
        beer.setActivatingPlayer(p); assertFalse(beer.canPlay());
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, p);
        Combat combat = new Combat(p); game.getPhaseHandler().setCombat(combat); combat.addAttacker(laborer, other);
        CombatUtil.checkDeclaredAttacker(game, laborer, combat, true);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p); assertTrue(beer.canPlay());
        assertTrue(ComputerUtil.playNoStack(p, beer, game, false)); assertEquals(countCardsWithName(game, "Beer"), 1);
    }

    @Test
    public void saunaElementalGrowsOnOnlyTheSecondInstantOrSorcery() {
        Game game = customGame(); Player p = game.getPlayers().get(0);
        Card sauna = enter("Sauna Elemental", p), target = addCard("Couchpotato", p);
        for (int n = 1; n <= 3; n++) {
            Card hand = addCardToZone("Take a Sniff", p, ZoneType.Hand);
            SpellAbility cast = hand.getFirstSpellAbility(); cast.setActivatingPlayer(p); cast.getTargets().add(target);
            cast.setHostCard(game.getAction().moveToStack(hand, cast)); game.getStack().add(cast); resolveTriggers(game);
            assertEquals(sauna.getCounters(CounterEnumType.P1P1), n < 2 ? 0 : 1);
            assertEquals(sauna.hasKeyword("Menace"), n >= 2);
        }
    }

}
