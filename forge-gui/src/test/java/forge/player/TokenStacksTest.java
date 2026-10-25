package forge.player;

import forge.card.GamePieceType;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.gui.TokenStacks;
import forge.trackable.TrackableProperty;
import org.testng.annotations.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class TokenStacksTest {
    @Test
    public void realTokensWithSeparateIdentitiesCanGroup() {
        HumanControlledGame g = new HumanControlledGame();
        for (String types : new String[] {"Artifact Food", "Artifact Beer", "Creature Goblin"}) {
            Card a = g.card(g.human, "Token", "Types:" + types, "PT:1/1", "K:Trample", "A:AB$ GainLife | Cost$ 2 T Sac<1/CARDNAME> | LifeAmount$ 3 | SpellDescription$ You gain 3 life.");
            Card b = g.card(g.human, "Token", "Types:" + types, "PT:1/1", "K:Trample", "A:AB$ GainLife | Cost$ 2 T Sac<1/CARDNAME> | LifeAmount$ 3 | SpellDescription$ You gain 3 life.");
            a.setGamePieceType(GamePieceType.TOKEN);
            b.setGamePieceType(GamePieceType.TOKEN);
            assertThat(a.getId()).isNotEqualTo(b.getId());
            assertThat(TokenStacks.compatible(a.getView(), b.getView())).as(types).isTrue();
            b.setTapped(true);
            assertThat(TokenStacks.compatible(a.getView(), b.getView())).isFalse();
        }
    }
    @Test
    public void thresholdAndDisableAreBounded() {
        assertThat(TokenStacks.parseThreshold("0")).isZero();
        assertThat(TokenStacks.parseThreshold("invalid")).isEqualTo(3);
        assertThat(TokenStacks.parseThreshold("1")).isEqualTo(2);
        assertThat(TokenStacks.parseThreshold("1000")).isEqualTo(100);
        assertThat(TokenStacks.shouldCollapse(2, 3)).isFalse();
        assertThat(TokenStacks.shouldCollapse(3, 3)).isTrue();
        assertThat(TokenStacks.shouldCollapse(1000, 0)).isFalse();
    }
    @Test
    public void largeTokenGroupingMeasurement() {
        HumanControlledGame g = new HumanControlledGame();
        java.util.List<CardView> views = new java.util.ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            Card card = g.card(g.human, "Beer", "Types:Artifact", "PT:0/0");
            card.setGamePieceType(GamePieceType.TOKEN);
            views.add(card.getView());
        }
        long start = System.nanoTime();
        int matches = 1;
        for (int i = 1; i < views.size(); i++) {
            if (TokenStacks.compatible(views.get(0), views.get(i))) { matches++; }
        }
        long elapsed = System.nanoTime() - start;
        assertThat(matches).isEqualTo(1000);
        System.out.printf("Token grouping: %d real views, %.3f ms; visible cards 1000 -> 1 at threshold 3%n", matches, elapsed / 1_000_000.0);
    }
    private CardView token(int id) {
        CardView view = new CardView(id, null, "Beer");
        view.set(TrackableProperty.Token, true);
        return view;
    }
    @Test
    public void changesToRelevantPropertiesSplitAndCanRegroup() {
        for (TrackableProperty property : new TrackableProperty[] {
                TrackableProperty.Tapped, TrackableProperty.Sickness, TrackableProperty.Detained,
                TrackableProperty.ExertedThisTurn, TrackableProperty.PhasedOut,
                TrackableProperty.Attacking, TrackableProperty.Blocking}) {
            CardView a = token(1), b = token(2);
            assertThat(TokenStacks.compatible(a, b)).isTrue();
            b.set(property, property != TrackableProperty.Sickness);
            assertThat(TokenStacks.compatible(a, b)).as(property.toString()).isFalse();
        }
        CardView a = token(1), b = token(2);
        b.set(TrackableProperty.Damage, 1);
        assertThat(TokenStacks.compatible(a, b)).isFalse();
        b.set(TrackableProperty.Damage, 0);
        assertThat(TokenStacks.compatible(a, b)).isTrue();
        b.getCurrentState().set(TrackableProperty.AbilityText, "Temporary effect");
        assertThat(TokenStacks.compatible(a, b)).isFalse();
    }
    @Test
    public void countersAttachmentsAndChosenValuesRemainDistinct() {
        CardView a = token(1), b = token(2);
        b.set(TrackableProperty.Counters, java.util.Map.of(forge.game.card.CounterEnumType.P1P1, 1));
        assertThat(TokenStacks.compatible(a, b)).isFalse();
        b.set(TrackableProperty.Counters, null);
        b.set(TrackableProperty.ChosenNumber, "7");
        assertThat(TokenStacks.compatible(a, b)).isFalse();
        b.set(TrackableProperty.ChosenNumber, null);
        b.set(TrackableProperty.EntityAttachedTo, a);
        assertThat(TokenStacks.compatible(a, b)).isFalse();
        b.set(TrackableProperty.EntityAttachedTo, null);
        b.getCurrentState().set(TrackableProperty.Power, 4);
        assertThat(TokenStacks.compatible(a, b)).isFalse();
        b.getCurrentState().set(TrackableProperty.Power, 0);
        b.set(TrackableProperty.Token, false);
        assertThat(TokenStacks.compatible(a, b)).isFalse();
    }
}
