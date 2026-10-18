import forge.StaticData;
import forge.adventure.data.AdventureEventData;
import forge.adventure.util.AdventureEventController;
import forge.adventure.util.Config;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckSection;
import forge.gamemodes.limited.CardRanker;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiBase;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.localinstance.properties.ForgePreferences.FPref;
import java.io.*;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.util.*;

/** Real Adventure event checks, run from the prepared isolated working directory. */
public class VerifyAdventureEvents {
 static int checks;
 static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static void pass(String message){checks++;System.out.println("PASS: "+message);}
 static void deck(Deck deck){
  require(deck!=null && deck.getMain().countAll()==40,"AI event deck must have 40 cards");
  require(DeckFormat.Limited.getDeckConformanceProblem(deck)==null,"Illegal AI event deck");
  for(PaperCard card:deck.getMain().toFlatList())require(card.getRules().getType().isBasicLand() || (card.getEdition().equals("CBT") && !card.getRules().getType().isPlaneswalker() && !card.getName().equals("AJDE!")),"Ineligible event card: "+card.getName());
 }
 static void participants(AdventureEventData event)throws Exception{
  var constructor=AdventureEventData.AdventureEventParticipant.class.getDeclaredConstructor();constructor.setAccessible(true);
  event.participants=new AdventureEventData.AdventureEventParticipant[8];
  for(int i=0;i<7;i++)event.participants[i]=constructor.newInstance();
  event.participants[7]=event.getHumanPlayer();
 }
 static AdventureEventData roundTrip(AdventureEventData event)throws Exception{
  var bytes=new ByteArrayOutputStream();try(var out=new ObjectOutputStream(bytes)){out.writeObject(event);}
  try(var in=new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))){return (AdventureEventData)in.readObject();}
 }
 static void games(String format, Deck[] decks)throws Exception{
  for(int i=0;i<2;i++){
   var rules=new forge.game.GameRules(forge.game.GameType.AdventureEvent);rules.setGamesPerMatch(1);
   var players=new ArrayList<forge.game.player.RegisteredPlayer>();
   for(int seat=0;seat<2;seat++)players.add(new forge.game.player.RegisteredPlayer(decks[2*i+seat]).setPlayer(forge.player.GamePlayerUtil.createAiPlayer(format+"-"+i+"-"+seat)));
   var match=new forge.game.Match(rules,players,"Chaos Brawl Adventure limited smoke");
   var game=match.createGame();game.setNoGUIUser();
   var executor=java.util.concurrent.Executors.newSingleThreadExecutor(task->{Thread thread=new Thread(task,"limited-match-check");thread.setDaemon(true);return thread;});
   long started=System.currentTimeMillis();
   var diagnostics=new ByteArrayOutputStream();var previousError=System.err;
   try(var capturedError=new PrintStream(diagnostics)){
    System.setErr(capturedError);
    try{executor.submit(()->match.startGame(game)).get(120,java.util.concurrent.TimeUnit.SECONDS);}finally{executor.shutdownNow();System.setErr(previousError);}
   }
   String errors=diagnostics.toString();if(!errors.isBlank())previousError.print(errors);
   require(!errors.contains("Exception") && !errors.contains("Error"),"AI emitted a caught exception in "+format+" game "+i);
   require(game.isGameOver() && game.getOutcome()!=null && !game.getOutcome().isDraw(),"No completed decisive "+format+" game");
   Files.createDirectories(Path.of("../match-logs"));
   Files.writeString(Path.of("../match-logs/"+format+"-"+i+".txt"),game.getGameLog().getLogEntries(null).toString());
   System.out.println("MATCH_PASS: "+format+" "+i+" turn="+game.getPhaseHandler().getTurn()+" winner="+game.getOutcome().getWinningLobbyPlayer().getName()+" elapsedMs="+(System.currentTimeMillis()-started));
  }
  pass("two complete real-engine Adventure "+format+" games from generated AI decks (headless AI vs AI)");
 }
 public static void main(String[] args)throws Exception{
  require(Files.isRegularFile(Path.of("forge.profile.properties")),"Run only in prepared isolated Adventure directory");
  GuiBase.setInterface((IGuiBase)Proxy.newProxyInstance(IGuiBase.class.getClassLoader(),new Class[]{IGuiBase.class},(object,method,arguments)->{
   if(method.getName().equals("getAssetsDir"))return "./";
   if(method.getName().equals("getCurrentVersion"))return "chaosbrawl-adventure-local-check";
   if(method.getName().equals("showBugReportDialog"))throw new AssertionError("Unexpected GUI error while checking model");
   if(arguments!=null)for(Object value:arguments)if(value instanceof Runnable task)task.run();
   if(method.getReturnType()==boolean.class)return method.getName().equals("isGuiThread")||method.getName().equals("isRunningOnDesktop");
   if(method.getReturnType()==int.class)return 0;
   if(method.getReturnType()==float.class)return 1f;
   if(method.getReturnType()==String.class)return "local-check";
   return null;
  }));
  FModel.initialize(null,prefs->{prefs.setPref(FPref.LOAD_CARD_SCRIPTS_LAZILY,false);prefs.setPref(FPref.UI_LANGUAGE,"en-US");prefs.setPref(FPref.ALLOW_CUSTOM_CARDS_IN_DECKS_CONFORMANCE,true);prefs.setPref(FPref.DECKGEN_CARDBASED,false);return null;});
  Config config=Config.instance();config.loadResources();
  require(config.getPlane().equals("ChaosBrawl"),"Wrong isolated plane");
  require(Arrays.equals(config.getConfigData().allowedEvents,new String[]{"CBT"}),"Wrong event whitelist");
  require(Arrays.equals(config.getConfigData().eventFormats,new String[]{"Draft","Sealed"}),"Wrong event formats");
  require(new forge.game.GameRules(forge.game.GameType.AdventureEvent).isChaosBrawl(),"Custom mechanics not enabled for Adventure matches");
  pass("isolated plane/configuration and custom Adventure match rules");
  var edition=StaticData.instance().getEditions().get("CBT");
  var blocks=AdventureEventData.getValidDraftBlocks(List.of(edition));
  require(blocks.size()==1 && blocks.get(0).getName().equals("Chaos Brawl - The Foul Realms Beta"),"Custom Adventure block not eligible");
  pass("real Adventure draft/sealed block eligibility");
  for(int wins:List.of(0,20))for(int seed=0;seed<100;seed++){
   Random legacy=new Random(seed);
   var expected=wins<10&&legacy.nextInt(10)<=2?AdventureEventController.EventFormat.Jumpstart:legacy.nextInt(4)==3?AdventureEventController.EventFormat.Sealed:AdventureEventController.EventFormat.Draft;
   require(AdventureEventController.selectEventFormat(new Random(seed),wins,null)==expected,"Changed ordinary plane event rolls");
   require(AdventureEventController.selectEventFormat(new Random(seed),wins,config.getConfigData().eventFormats)!=AdventureEventController.EventFormat.Jumpstart,"Unsupported Jumpstart on custom plane");
  }
  try{AdventureEventController.selectEventFormat(new Random(1),0,new String[]{"Constructed"});throw new AssertionError("Invalid configured event format accepted");}catch(IllegalArgumentException expected){}
  pass("configured Draft/Sealed and unchanged legacy event rolls");
  var sealed=new AdventureEventData(20261018L,AdventureEventController.EventFormat.Sealed);participants(sealed);sealed.generateSealedPool();
  require(sealed.packConfiguration.length==8,"Wrong sealed pack count");
  require(sealed.registeredDeck.get(DeckSection.Sideboard).countAll()==96,"Wrong human sealed pool");
  require(sealed.eventStatus==AdventureEventController.EventStatus.Entered,"Sealed entry did not finish");
  for(int i=0;i<7;i++)deck(sealed.participants[i].getDeck());
  pass("actual Adventure eight-pack Sealed entry and seven legal AI decks");
  var restored=roundTrip(sealed);
  require(restored.getCardBlock()!=null && restored.getCardBlock().getName().equals(sealed.cardBlockName),"Saved event block failed to restore");
  require(restored.registeredDeck.get(DeckSection.Sideboard).countAll()==96,"Saved sealed pool changed");
  for(int i=0;i<7;i++)deck(restored.participants[i].getDeck());
  pass("Adventure event serialization restores custom block, pool and AI decks");
  var draft=new AdventureEventData(20261018L,AdventureEventController.EventFormat.Draft);participants(draft);
  var process=draft.getDraft();int picks=0;
  while(process.hasNextChoice()){
   var choice=process.nextChoice().toFlatList();require(!choice.isEmpty(),"Empty draft choice");
   process.setChoice(CardRanker.getOrderedRawScores(choice).get(0),DeckSection.Main);
   require(++picks<=36,"Draft did not terminate");
  }
  require(picks==36 && process.getOpposingPlayers().length==7,"Wrong Adventure draft dimensions");
  for(Deck d:process.getComputerDecks())deck(d);
  pass("actual Adventure three-pack eight-seat Draft and seven legal AI decks");
  if(Arrays.asList(args).contains("--games")){
   games("sealed",Arrays.stream(sealed.participants).limit(7).map(p->p.getDeck()).toArray(Deck[]::new));
   games("draft",process.getComputerDecks());
  }
  System.out.println("ADVENTURE_EVENT_CHECKS_PASSED: "+checks);
  System.exit(0);
 }
}
