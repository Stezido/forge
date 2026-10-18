import forge.*;
import forge.gui.GuiBase;
import forge.item.PaperCard;
import java.nio.file.*;
import java.util.*;
/** Audit the exact paths used by deck-builder image lookup; requires a built desktop JAR. */
public class VerifyPackageImages {
 public static void main(String[] args) throws Exception {
  Path root=Path.of(args[0]).toAbsolutePath();
  GuiBase.setInterface(new GuiDesktop());
  String res=root.resolve("res").toString()+"/";
  forge.util.Lang.createInstance("en-US");
  forge.util.Localizer.getInstance().initialize("en-US",res+"languages/");
  StaticData data=new StaticData(new CardStorageReader(res+"cardsfolder",null,false),null,res+"editions",res+"no-custom-editions",res+"blockdata","Latest Art All Editions",true,true);
  ImageKeys.initializeDirs(root.resolve("Cache/pics/cards").toString()+"/",Map.of(),root.resolve("Cache/pics/tokens").toString()+"/","","","","","","");
  var cards=data.getCommonCards().getAllCards(p->p.getEdition().equals("CBFRbeta"));
  int missing=0;
  for(PaperCard card:cards){
   Path image=root.resolve("Cache/pics/cards").resolve(card.getCardImageKey()+".png");
   if(!Files.isRegularFile(image) || javax.imageio.ImageIO.read(image.toFile())==null || ImageKeys.getImageFile(card.getCardImageKey())==null){System.out.println("MISSING "+card.getCollectorNumber()+" "+card.getName()+" -> "+card.getCardImageKey()+".png");missing++;}
  }
  int tokens=0;
  try(var paths=Files.list(root.resolve("Cache/pics/tokens"))){
   for(Path script:paths.filter(p->p.toString().endsWith(".png")).toList()){
    String name=script.getFileName().toString().replace(".png", "");
    Path image=root.resolve("Cache/pics/tokens").resolve(name+".png");
    var bitmap=Files.isRegularFile(image)?javax.imageio.ImageIO.read(image.toFile()):null;
    if(bitmap==null || bitmap.getHeight()<=bitmap.getWidth() || ImageKeys.getImageFile(ImageKeys.TOKEN_PREFIX+name)==null){
     System.out.println("MISSING OR NON-CARD TOKEN IMAGE "+name);missing++;
    }
    tokens++;
   }
  }
  System.out.println("AUDIT: "+tokens+" full-card bundled token images");
  System.out.println("AUDIT: "+cards.size()+" CBFRbeta printings; "+missing+" missing exact runtime image paths");
  int expected=args.length>1?Integer.parseInt(args[1]):292;
  System.exit(missing==0 && cards.size()==expected && tokens==17?0:1);
 }
}
