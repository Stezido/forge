import forge.StaticData;
import forge.ai.ComputerUtil;
import forge.ai.LobbyPlayerAi;
import forge.deck.Deck;
import forge.game.*;
import forge.game.ability.*;
import forge.game.card.*;
import forge.game.phase.PhaseType;
import forge.game.player.*;
import forge.game.zone.ZoneType;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiBase;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import java.lang.reflect.Proxy;
import java.util.*;

/** Exercise the shipped engine/resources without test classes or forced custom rules. */
public class VerifyCustomMechanics {
    static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    static Card add(String name, Player p) {
        var cards = StaticData.instance().getCommonCards().getAllCards(name);
        var paper = cards.stream().filter(c -> c.getEdition().equals("CBFRbeta")).findFirst()
                .orElseGet(() -> StaticData.instance().getCommonCards().getCard(name));
        Card c = Card.fromPaperCard(paper, p);
        c.setGameTimestamp(p.getGame().getNextTimestamp());
        p.getZone(ZoneType.Battlefield).add(c);
        return c;
    }
    static void resolve(Game game) {
        for (int n = 0; n < 20; n++) {
            game.getTriggerHandler().runWaitingTriggers();
            game.getStack().addAllTriggeredAbilitiesToStack();
            if (game.getStack().isEmpty()) return;
            game.getStack().resolveStack();
        }
        throw new AssertionError("Trigger loop");
    }
    public static void main(String[] args) {
        require(Boolean.getBoolean("forge.chaosbrawl"), "Launcher did not enable custom mechanics");
        GuiBase.setInterface((IGuiBase)Proxy.newProxyInstance(IGuiBase.class.getClassLoader(), new Class[]{IGuiBase.class}, (o,m,a) -> {
            if (m.getName().equals("getAssetsDir")) return "./";
            if (m.getName().equals("getCurrentVersion")) return "chaosbrawl-release-check";
            if (m.getName().equals("showBugReportDialog")) throw new AssertionError("Unexpected GUI error");
            if (a != null) for (Object value : a) if (value instanceof Runnable task) task.run();
            if (m.getReturnType() == boolean.class) return m.getName().equals("isGuiThread") || m.getName().equals("isRunningOnDesktop");
            if (m.getReturnType() == int.class) return 0;
            if (m.getReturnType() == float.class) return 1f;
            if (m.getReturnType() == String.class) return "release-check";
            return null;
        }));
        FModel.initialize(null, prefs -> { prefs.setPref(FPref.LOAD_CARD_SCRIPTS_LAZILY, false); prefs.setPref(FPref.UI_LANGUAGE, "en-US"); return null; });
        int checks = 0;
        for (String kind : List.of("beer", "expired_beer", "bock_beer", "dark_beer", "pils_beer", "wheat_beer", "zwickl_beer")) {
            for (int taps : List.of(0, 1, 2)) {
                var players = new ArrayList<RegisteredPlayer>();
                for (int i = 0; i < 2; i++) players.add(new RegisteredPlayer(new Deck()).setPlayer(new LobbyPlayerAi("release-" + i, null)));
                var rules = new GameRules(GameType.Constructed);
                require(rules.isChaosBrawl(), "Constructed match did not inherit the launcher flag");
                var match = new Match(rules, players, "Release mechanics audit");
                var game = new Game(players, rules, match);
                game.setAge(GameStage.Play);
                Player p = game.getPlayers().getFirst();
                game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
                if (taps == 2) add("Mirror Gallery", p);
                for (int n = 0; n < taps; n++) add("Draft Tap", p);
                Card target = add("Couchpotato", p);
                add("Swamp", p);
                var create = AbilityFactory.getAbility("DB$ Token | TokenScript$ chaosbrawl_" + kind + " | TokenAmount$ 1", target);
                create.setActivatingPlayer(p);
                AbilityUtils.resolve(create);
                Card beer = p.getCardsIn(ZoneType.Battlefield).stream().filter(Card::isToken).findFirst().orElseThrow();
                game.getAction().checkStaticAbilities();
                var drink = beer.getSpellAbilities().stream().filter(a -> a.getApi() == ApiType.PutCounter).findFirst().orElseThrow();
                drink.setActivatingPlayer(p);
                drink.getTargets().add(target);
                require(ComputerUtil.playStack(drink, p, game), "Beer activation failed: " + kind);
                require(drink.getTargets().isEmpty(), "Normal-stack target clearing was not exercised");
                resolve(game);
                int expected = (1 + taps) * (kind.equals("bock_beer") ? 2 : 1);
                require(target.getCounters(CounterEnumType.DRUNKEN) == expected, kind + " with " + taps + " Draft Taps: expected " + expected + ", got " + target.getCounters(CounterEnumType.DRUNKEN));
                checks++;
            }
        }
        System.out.println("PACKAGED_CUSTOM_MECHANICS_PASSED: " + checks + " normal-stack Beer/Draft Tap cases");
        System.exit(0);
    }
}
