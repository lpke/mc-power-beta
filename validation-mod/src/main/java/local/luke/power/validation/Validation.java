package local.luke.power.validation;
import local.luke.power.config.*;
import local.luke.power.audio.*;
import local.luke.power.ui.*;
import local.luke.power.mixin.*;
import com.google.gson.*;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.opengl.GL11;
public final class Validation {
  private static final Path COMMAND=Path.of("power-beta-validation.command"),REPORT=Path.of("power-beta-validation.log");
  private static int ticks,failures;
  interface Check{void run()throws Exception;}
  static void log(String s)throws Exception{Files.writeString(REPORT,s+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);System.out.println("POWER VALIDATION "+s);}
  static void test(String name,Check c)throws Exception{try{c.run();log("PASS "+name);}catch(Throwable e){failures++;log("FAIL "+name+" "+e);e.printStackTrace();}}
  static void check(boolean okay,String message){if(!okay)throw new AssertionError(message);}
  public static void tick(Minecraft mc){if(++ticks%5!=0||!Files.exists(COMMAND))return;try{String command=Files.readString(COMMAND).trim();Files.delete(COMMAND);
    if(command.equals("audit"))audit(mc);
    else if(command.equals("roundtrip"))roundtrip(mc);
    else if(command.equals("screens"))screens(mc);
    else if(command.startsWith("options")){PowerOptionsScreen s=new PowerOptionsScreen(mc.currentScreen);if(command.length()>8){Field p=PowerOptionsScreen.class.getDeclaredField("page");p.setAccessible(true);p.set(s,command.substring(8));}mc.setScreen(s);}
    else if(command.equals("audio"))audio(mc);
    else if(command.equals("pause"))mc.setScreen(new net.minecraft.class_525());
    else if(command.equals("defaults")){ConfigSession s=SettingsRegistry.open(mc);s.settings().forEach(Setting::reset);s.save(Path.of(".").toAbsolutePath());log("DEFAULTS SAVED "+s.settings().size());}
    else if(command.equals("dump"))Files.writeString(Path.of("power-beta-full-catalog.json"),Catalog.JSON.toJson(SettingsRegistry.open(mc).settings()));
    else if(command.equals("title")){mc.setWorld(null);mc.setScreen(new net.minecraft.client.gui.screen.TitleScreen());}
    log("COMMAND DONE "+command);
  }catch(Throwable e){try{log("COMMAND FAILED "+e);}catch(Exception ignored){}e.printStackTrace();}}
  static void audit(Minecraft mc)throws Exception{
    failures=0;ConfigSession session=SettingsRegistry.open(mc);Set<String> ids=new HashSet<>();int n=0;
    for(Setting s:session.settings()){
      test("schema "+s.id,()->{check(ids.add(s.id),"duplicate");s.validate(s.defaultValue);s.validate(s.value);check(PowerOptionsScreen.PAGES.contains(s.page),"unknown page");check(!s.label.matches(".*(UniTweaks|MojangFix|Lpke|key\\.).*"),"untranslated label");});n++;
      if(s.kind==Setting.Kind.BOOLEAN||s.kind==Setting.Kind.CHOICE||s.kind==Setting.Kind.INTEGER||s.kind==Setting.Kind.DECIMAL){JsonElement old=s.value.deepCopy();s.cycle(1);s.cycle(-1);check(s.value.equals(old)||s.value.getAsDouble()==old.getAsDouble(),"inverse cycle "+s.id);s.value=old;}
    }
    log("AUDIT TOTAL "+n+" FAILURES "+failures);
  }
  static Setting find(ConfigSession s,String id){return s.settings().stream().filter(e->e.id.equals(id)).findFirst().orElseThrow();}
  static void roundtrip(Minecraft mc)throws Exception{
    failures=0;ConfigSession s=SettingsRegistry.open(mc);Map<String,JsonElement> old=new LinkedHashMap<>();
    Map<String,JsonElement> edits=new LinkedHashMap<>();edits.put("native.music",new JsonPrimitive(43));edits.put("audio.category.blocks",new JsonPrimitive(35));edits.put("creative.glide",new JsonPrimitive(2));edits.put("tweaks.freeLook",new JsonPrimitive(true));edits.put("worldedit.opacity",new JsonPrimitive(70));edits.put("hudtweaks:config.chatHistorySize",new JsonPrimitive(101));edits.put("logo.logo.animation.enabled",new JsonPrimitive(false));
    Object[] inventory=mc.player==null?null:mc.player.inventory.main.clone();
    for(var e:edits.entrySet()){Setting a=find(s,e.getKey());old.put(e.getKey(),a.value.deepCopy());a.value=e.getValue();}
    try{s.save(Path.of(".").toAbsolutePath());ConfigSession reread=SettingsRegistry.open(mc);for(var e:edits.entrySet())test("save and reload "+e.getKey(),()->{JsonElement actual=find(reread,e.getKey()).value;check(actual.equals(e.getValue())||actual.isJsonPrimitive()&&actual.getAsJsonPrimitive().isNumber()&&actual.getAsDouble()==e.getValue().getAsDouble(),"Saved value differs: "+actual);});
      if(inventory!=null)test("menu edits preserve inventory object identity",()->{for(int i=0;i<inventory.length;i++)check(inventory[i]==mc.player.inventory.main[i],"Inventory changed");});
    }finally{ConfigSession restore=SettingsRegistry.open(mc);old.forEach((id,value)->find(restore,id).value=value);restore.save(Path.of(".").toAbsolutePath());}
    log("ROUNDTRIP FAILURES "+failures);
  }
  static void screens(Minecraft mc)throws Exception{
    failures=0;Screen old=mc.currentScreen;
    for(int[] size:new int[][]{{320,240},{427,240},{550,380},{854,480}})for(String page:PowerOptionsScreen.PAGES)test("render "+page+" "+size[0]+"x"+size[1],()->{
      PowerOptionsScreen screen=new PowerOptionsScreen(old);Field p=PowerOptionsScreen.class.getDeclaredField("page");p.setAccessible(true);p.set(screen,page);screen.init(mc,size[0],size[1]);while(GL11.glGetError()!=GL11.GL_NO_ERROR){}screen.render(-1,-1,0);check(GL11.glGetError()==GL11.GL_NO_ERROR,"OpenGL error");screen.removed();
    });mc.setScreen(old);log("SCREEN FAILURES "+failures);
  }
  static void audio(Minecraft mc)throws Exception{
    failures=0;AudioSettings old=AudioConfig.copy();
    test("cached sound resources loaded",()->check(AudioController.sounds(mc).size()>50,"Only "+AudioController.sounds(mc).size()+" sounds"));
    test("cached music resources loaded",()->check(AudioController.music(mc).size()>=3,"No vanilla playlist"));
    try{AudioSettings draft=AudioConfig.copy();draft.master=50;draft.categories.put("hostile",40);draft.sounds.put("mob.zombie",25);AudioConfig.save(draft);
      test("runtime category and individual mixing",()->check(Math.abs(AudioController.mix("validation","mob.zombie",1,false)-.05)<.00001,"Incorrect gain"));
      draft.master=0;AudioConfig.save(draft);test("master mute is exact",()->check(AudioController.mix("validation","mob.zombie",1,false)==0,"Not muted"));
    }finally{AudioConfig.save(old);}
    log("AUDIO FAILURES "+failures);
  }
}
