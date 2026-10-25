package forge.gui;

import forge.game.card.CardView;
import forge.model.FModel;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.trackable.TrackableProperty;
import java.util.EnumMap;
import java.util.Map;
import forge.card.CardTypeView;
import forge.trackable.TrackableObject;

/** Visual grouping only. Never changes engine permanents or their identities. */
public final class TokenStacks {
    private TokenStacks() { }
    /** Zero disables stacking; malformed preferences fall back to three. */
    public static int threshold() {
        return parseThreshold(FModel.getPreferences().getPref(FPref.UI_TOKEN_STACK_THRESHOLD));
    }
    public static int parseThreshold(String preference) {
        try {
            int value = Integer.parseInt(preference);
            return value == 0 ? 0 : Math.max(2, Math.min(100, value));
        } catch (NumberFormatException ex) {
            return 3;
        }
    }
    public static boolean shouldCollapse(int count, int threshold) {
        return threshold > 0 && count >= threshold;
    }
    public static boolean compatible(CardView a, CardView b) {
        if (!a.isToken() || !b.isToken() || a.isFaceDown() || b.isFaceDown()
                || a.hasCardAttachments() || b.hasCardAttachments()
                || a.getPreparedSpell() != null || b.getPreparedSpell() != null
                || !a.getUntilLeavesBattlefield().isEmpty() || !b.getUntilLeavesBattlefield().isEmpty()
                || a.isAttacking() || b.isAttacking() || a.isBlocking() || b.isBlocking()
                || a.getAttachedTo() != null || b.getAttachedTo() != null) {
            return false;
        }
        return properties(a).equals(properties(b))
                && a.getCurrentState().getState() == b.getCurrentState().getState()
                && stateProperties(a.getCurrentState()).equals(stateProperties(b.getCurrentState()));
    }
    private static Map<TrackableProperty, Object> stateProperties(TrackableObject state) {
        Map<TrackableProperty, Object> result = new EnumMap<>(TrackableProperty.class);
        result.putAll(state.<Map<TrackableProperty, Object>>getProps());
        Object type = result.get(TrackableProperty.Type);
        if (type instanceof CardTypeView) {
            // CardType has identity equality. Its display string includes all types,
            // but also preserve excluded subtypes and the all-creature-types flag.
            CardTypeView t = (CardTypeView) type;
            java.util.List<String> excluded = new java.util.ArrayList<>();
            t.getExcludedCreatureSubTypes().forEach(excluded::add);
            result.put(TrackableProperty.Type, java.util.List.of(t.toString(), excluded, t.hasAllCreatureTypes()));
        }
        for (TrackableProperty property : new TrackableProperty[] {TrackableProperty.ManaCost, TrackableProperty.OriginalManaCost}) {
            if (result.containsKey(property)) { result.put(property, result.get(property).toString()); }
        }
        return result;
    }
    private static Map<TrackableProperty, Object> properties(CardView card) {
        Map<TrackableProperty, Object> result = new EnumMap<>(TrackableProperty.class);
        result.putAll(card.<Map<TrackableProperty, Object>>getProps());
        // State views have permanent-specific identity. Compare their contents above.
        result.remove(TrackableProperty.CurrentState);
        // Alternate faces need independent inspection; conservatively exclude them by identity.
        return result;
    }
}
