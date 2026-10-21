package forge.gamesimulationtests;

import forge.StaticData;
import forge.ai.AITest;
import forge.card.CardRarity;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckSection;
import forge.gamemodes.limited.*;
import forge.item.BoosterSlot;
import forge.item.PaperCard;
import forge.item.generation.BoosterGenerator;
import forge.model.CardBlock;
import forge.model.FModel;
import forge.util.MyRandom;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;

import java.util.*;
import static org.testng.Assert.*;

/** Real booster/pool/draft generation; no mocked card ratings or AI deck builder. */
public class ChaosBrawlLimitedTest extends AITest {
    private static final String CODE="CBFRbeta";
    private static final String BLOCK="Chaos Brawl - The Foul Realms Beta";
    private static boolean modelInitialized;

    @Override @BeforeMethod
    public void initializeModel() {
        if(modelInitialized)return;
        forge.gui.GuiBase.setInterface(new forge.GuiDesktop());
        FModel.initialize(null,prefs->{
            prefs.setPref(forge.localinstance.properties.ForgePreferences.FPref.LOAD_CARD_SCRIPTS_LAZILY,false);
            prefs.setPref(forge.localinstance.properties.ForgePreferences.FPref.UI_LANGUAGE,"en-US");
            prefs.setPref(forge.localinstance.properties.ForgePreferences.FPref.ALLOW_CUSTOM_CARDS_IN_DECKS_CONFORMANCE,true);
            return null;
        });
        modelInitialized=true;
    }

    private List<PaperCard> pack() {
        return BoosterGenerator.getBoosterPack(StaticData.instance().getBoosters().get(CODE));
    }
    private void eligible(PaperCard card) {
        assertEquals(card.getEdition(),CODE);
        assertNotEquals(card.getName(),"AJDE!");
        assertTrue(card.getRarity()!=CardRarity.Special || card.getRules().getType().isPlaneswalker());
    }
    private void deck(Deck deck) {
        assertNotNull(deck);
        assertEquals(deck.getMain().countAll(),40);
        assertNull(DeckFormat.Limited.getDeckConformanceProblem(deck));
        for(PaperCard card:deck.getMain().toFlatList()) {
            if(!card.getRules().getType().isBasicLand())eligible(card);
        }
    }

    @Test
    public void eligibleSheetPreservesPrintingsAndFunctionalVariants() {
        List<PaperCard> cards=new ArrayList<>();
        for(String sheet:List.of("limited","lands","planeswalkers")) cards.addAll(StaticData.instance().getPrintSheets().get(CODE+" "+sheet).toFlatList());
        var expected=StaticData.instance().getCommonCards().getAllCards(c->c.getEdition().equals(CODE) && !c.getName().equals("AJDE!"));
        var missing=new HashSet<>(expected);missing.removeAll(cards);
        assertEquals(cards.size(),306,"Missing: "+missing.stream().map(c->c.getName()+" ["+c.getCollectorNumber()+"]").toList());
        assertEquals(new HashSet<>(cards).size(),306);
        cards.forEach(this::eligible);
        assertEquals(cards.stream().map(PaperCard::getName).distinct().count(),286L);
        for (String basic : List.of("Plains", "Island", "Swamp", "Mountain", "Forest")) {
            assertEquals(cards.stream().filter(c -> c.getName().equals(basic)).count(), 3L, basic + " art variants");
        }
        for(String name:List.of("Wedding Ring","Dungeon Master","Recycle","The Fallen")) {
            PaperCard card=cards.stream().filter(c->c.getName().equals(name)).findFirst().orElseThrow();
            assertEquals(card.getFunctionalVariant(),"ChaosBrawlBeta");
            PaperCard legacy=StaticData.instance().getCommonCards().getCard(name,"CBT");
            assertNotNull(legacy,"Legacy CBT deck reference must resolve: "+name);
            assertEquals(legacy.getEdition(),CODE);
            assertEquals(legacy.getFunctionalVariant(),"ChaosBrawlBeta");
        }
        CardBlock block=FModel.getBlocks().get(BLOCK);
        assertNotNull(block);assertEquals(block.getCntBoostersDraft(),3);assertEquals(block.getCntBoostersSealed(),8);
        assertEquals(block.getLandSet().getCode(),"M21");
    }

    @Test
    public void collationKeepsTwelveUniqueCardsWithHalfDualLandsAndTwoPercentWalkers() {
        MyRandom.setRandom(new Random(20261018));
        int mythics=0,lands=0,walkers=0;
        for(int n=0;n<2000;n++) {
            List<PaperCard> cards=pack();
            assertEquals(cards.size(),12);assertEquals(new HashSet<>(cards).size(),12);
            cards.forEach(this::eligible);
            assertEquals(cards.stream().filter(c->c.getRarity()==CardRarity.Uncommon).count() >= 3,true);
            long landCount=cards.stream().filter(c->c.getRules().getType().isLand() && c.getRarity()==CardRarity.BasicLand).count();
            long walkerCount=cards.stream().filter(c->c.getRules().getType().isPlaneswalker()).count();
            assertTrue(landCount<=1); assertTrue(walkerCount<=1);
            lands+=landCount;walkers+=walkerCount;
            if(cards.get(10).getRarity()==CardRarity.MythicRare)mythics++;
            if(cards.get(10).getRules().getType().isPlaneswalker()) assertEquals(walkerCount,1L);
            else assertTrue(Set.of(CardRarity.Rare,CardRarity.MythicRare).contains(cards.get(10).getRarity()));
        }
        assertTrue(lands>900 && lands<1100,"Dual land packs: "+lands);
        assertTrue(walkers>20 && walkers<65,"Planeswalker packs: "+walkers);
        assertTrue(mythics>190 && mythics<300,"Rare slot mythics: "+mythics);
    }

    @Test
    public void allEligibleCardsHaveFiniteDraftScoresAndRemovalBeatsNarrowSelection() {
        for(PaperCard card:StaticData.instance().getPrintSheets().get(CODE+" limited").toFlatList()) {
            double score=CardRanker.getRawScore(card);
            assertTrue(Double.isFinite(score) && score>=0 && score<=100,card.getName()+": "+score);
        }
        var db=StaticData.instance().getCommonCards();
        assertTrue(CardRanker.getRawScore(db.getCard("Wasted",CODE))>CardRanker.getRawScore(db.getCard("Gimme the herbs",CODE)));
    }

    @Test
    public void confirmedOwnerRatingsReplaceProvisionalPickOrder() {
        var db=StaticData.instance().getCommonCards();
        assertTrue(CardRanker.getRawScore(db.getCard("Brüder Hopfenblüte",CODE))>
                CardRanker.getRawScore(db.getCard("Sasa, the Big Brother",CODE)), "Confirmed 100 must outrank 99.");
        assertTrue(CardRanker.getRawScore(db.getCard("Wasted",CODE))>
                CardRanker.getRawScore(db.getCard("Food Coma",CODE)), "Confirmed 83 must outrank 80.");
        assertTrue(CardRanker.getRawScore(db.getCard("Brie-lliant Trapper",CODE))>
                CardRanker.getRawScore(db.getCard("Food Coma",CODE)), "Newly supplied Trapper 90 must outrank Food Coma 80.");
        assertTrue(CardRanker.getRawScore(db.getCard("Gimme the herbs",CODE))>
                CardRanker.getRawScore(db.getCard("Happy Hour",CODE)), "Confirmed zero must remain a valid bottom rating.");
    }

    @Test
    public void foodTokenGeneratorsAndMilkReceiveActualAiSynergyBonuses() {
        var db=StaticData.instance().getCommonCards();
        var payoff=db.getCard("Rotten Cellar",CODE);
        var creator=db.getCard("Chaotic Kitchen",CODE);
        var removal=db.getCard("Wasted",CODE);
        var scores=CardRanker.getScores(List.of(payoff,creator,removal));
        double creatorScore=scores.stream().filter(p->p.getRight().equals(creator)).findFirst().orElseThrow().getLeft();
        double removalScore=scores.stream().filter(p->p.getRight().equals(removal)).findFirst().orElseThrow().getLeft();
        assertTrue(creatorScore>CardRanker.getRawScore(creator),"Food token creation must match the Food payoff hint.");
        assertEquals(removalScore,CardRanker.getRawScore(removal),"Unrelated removal should not get a Food bonus.");
        var cereal=db.getCard("Cini Minis",CODE);
        var milk=db.getCard("Milk",CODE);
        double milkScore=CardRanker.getScores(List.of(cereal,milk)).stream().filter(p->p.getRight().equals(milk)).findFirst().orElseThrow().getLeft();
        assertTrue(milkScore>CardRanker.getRawScore(milk),"Milk must receive its named Cini Minis synergy bonus.");
    }

    @Test
    public void eightPackSealedPoolsBuildLegalAiDecks() {
        for(int seed=0;seed<8;seed++) {
            MyRandom.setRandom(new Random(20261018+seed));
            List<PaperCard> pool=new ArrayList<>();
            for(int n=0;n<8;n++)pool.addAll(pack());
            assertEquals(pool.size(),96);
            deck(new SealedDeckBuilder(pool).buildDeck("M21"));
        }
    }

    @Test
    public void completeEightSeatThreePackDraftBuildsSevenAiDecks() {
        MyRandom.setRandom(new Random(20261018));
        BoosterDraft draft=BoosterDraft.createDraft(LimitedPoolType.Block,FModel.getBlocks().get(BLOCK),new String[]{CODE,CODE,CODE});
        int picks=0;
        while(draft.hasNextChoice()) {
            List<PaperCard> choice=draft.nextChoice().toFlatList();
            assertFalse(choice.isEmpty());
            PaperCard selected=CardRanker.getOrderedRawScores(choice).get(0);
            draft.setChoice(selected,DeckSection.Main);
            assertTrue(++picks<=36,"Draft failed to terminate.");
        }
        assertEquals(picks,36);assertEquals(draft.getOpposingPlayers().length,7);
        for(LimitedPlayer player:draft.getOpposingPlayers())assertEquals(player.getDeck().get(DeckSection.Sideboard).countAll(),36);
        for(Deck deck:draft.getComputerDecks())deck(deck);
    }

    @Test
    public void ordinaryUpstreamBoosterAndDefaultSlotRulesRemainUnchanged() {
        assertFalse(BoosterSlot.parseSlot("Normal",List.of("Base=Common")).excludesAlreadySelected());
        List<PaperCard> ordinary=BoosterGenerator.getBoosterPack(StaticData.instance().getBoosters().get("M11"));
        assertEquals(ordinary.size(),15);
        assertTrue(ordinary.stream().allMatch(c->c.getEdition().equals("M11")));
    }
}
