package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;

import com.google.gson.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.input.*;
import local.luke.power.permissions.*;
import local.luke.power.ui.*;
import local.luke.power.world.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.WorldProperties;
import org.lwjgl.input.Keyboard;

/** Per-save difficulty and cheat visibility. Run only in disposable instances. */
final class WorldSettingsChecks {
  private static WorldDifficulty difficulty(Minecraft mc) { return (WorldDifficulty) mc.world.method_262(); }
  private static Path game() { return Path.of(".").toAbsolutePath(); }
  private static void change(Minecraft mc, int value) throws Exception {
    var s = SettingsRegistry.open(mc); find(s,"world.difficulty").parse(Integer.toString(value)); s.save(game());
  }
  private static void expectDifficulty(Minecraft mc, int value) {
    check(difficulty(mc).power$difficulty()==value && mc.world.field_213==value,"difficulty differs: " + difficulty(mc).power$difficulty()+" / "+mc.world.field_213);
  }
  private static List<Setting> rows(PowerOptionsScreen screen) throws Exception {
    var result = new ArrayList<Setting>();
    for (Object row : (List<?>)field(screen,"rows")) {
      Setting setting=(Setting)call(row,"setting"); if(setting!=null)result.add(setting);
    }
    return result;
  }
  private static void cleanView(PowerOptionsScreen screen, String page) throws Exception {
    field(screen,"page",page);field(screen,"relatedIds",List.of());field(screen,"conflictIds",List.of());
    field(screen,"changedOnly",false);((Set<?>)field(screen,"collapsed")).clear();search(screen,"");
  }

  static void run(Minecraft mc, String action) throws Exception {
    failures=0;
    if(action.equals("primary")) {
      mc.setWorld(null);mc.interactionManager=new net.minecraft.SingleplayerInteractionManager(mc);
      mc.method_2120("Primary difficulty verification","Primary difficulty verification",173L);
      expectDifficulty(mc,3);mc.tick();expectDifficulty(mc,3);
      var session=SettingsRegistry.open(mc);
      check(find(session,"power_mechanics:config.INTERACTIVE_BLOCK_CONFIG.allowEditingSigns").value.getAsBoolean(),"primary sign preference changed");
      log("PREPARED PRIMARY PASS: Hard, sign editing enabled; "+session.settings().size()+" settings loaded");return;
    }
    if(action.equals("extra")) { extra(mc); return; }
    if(action.equals("scope")) { scope(mc); return; }
    if(action.equals("create")) { creation(mc); return; }
    if(action.equals("reload")) { reload(mc); return; }
    check(mc.world!=null&&!mc.world.isRemote,"requires singleplayer fixture world");
    var properties=mc.world.method_262(); var cheat=(CheatWorld)properties;
    int before=difficulty(mc).power$difficulty(), nativeBefore=mc.options.difficulty;
    boolean enabledBefore=cheat.power$cheatsEnabled(); var parent=mc.currentScreen;
    try {
      test("all four difficulties survive NBT and dimension copies with other metadata intact",()-> {
        for(int value=0;value<4;value++) {
          var fresh=new WorldProperties(173L,"Difficulty "+value); var rules=(WorldDifficulty)fresh;
          check(rules.power$difficulty()==2,"new save default");
          rules.power$difficulty(value); fresh.setTime(12345); fresh.setRaining(true);
          ((CheatWorld)fresh).power$cheatsEnabled(value%2==1);
          ((WorldCycles)fresh).power$daylightCycle(false);
          var loaded=new WorldProperties(fresh.asNbt()); var copied=new WorldProperties(loaded);
          check(((WorldDifficulty)copied).power$difficulty()==value,"copy/NBT lost value");
          check(copied.getTime()==12345&&copied.getRaining(),"unrelated metadata changed");
          check(((CheatWorld)copied).power$cheatsEnabled()==(value%2==1),"cheats changed");
          check(!((WorldCycles)copied).power$daylightCycle(),"cycle changed");
        }
        check(((WorldDifficulty)new WorldProperties(new NbtCompound())).power$difficulty()==2,"absent difficulty default");
        for(int invalid:new int[]{-1,4,Integer.MAX_VALUE}) {
          var tag=new NbtCompound();tag.putInt(WorldDifficulty.TAG,invalid);
          check(((WorldDifficulty)new WorldProperties(tag)).power$difficulty()==2,"invalid metadata fallback");
          boolean rejected=false;try { difficulty(mc).power$difficulty(invalid); } catch(IllegalArgumentException e) { rejected=true; }
          check(rejected,"invalid setter accepted");
        }
      });
      test("difficulty draft and Cancel never affect gameplay; Apply and native ticks use the world value",()-> {
        cheat.power$cheatsEnabled(false);
        byte[] config=Files.readAllBytes(local.luke.power.storage.PowerConfig.path());
        for(int value=0;value<4;value++) {
          int old=difficulty(mc).power$difficulty();
          var session=SettingsRegistry.open(mc); Setting setting=find(session,"world.difficulty");
          setting.parse(Integer.toString(value));session.preview(true);expectDifficulty(mc,old);
          session.discard();expectDifficulty(mc,old);
          setting.parse(Integer.toString(value)); session.save(game());expectDifficulty(mc,value);
          mc.options.difficulty=(value+1)%4;
          mc.setScreen(null);mc.tick();expectDifficulty(mc,value);
          check(mc.options.difficulty==(value+1)%4,"world difficulty overwrote native option");
          check(Arrays.equals(config,Files.readAllBytes(local.luke.power.storage.PowerConfig.path())),"world difficulty wrote global config");
        }
      });
      test("difficulty transaction rollback and stale world reject safely",()-> {
        change(mc,1); var session=SettingsRegistry.open(mc);
        find(session,"world.difficulty").value=new JsonPrimitive(3);
        var failure=new Setting("fixture.fail","fixture","General","Test","Failure","",Setting.Kind.BOOLEAN,
            new JsonPrimitive(false),new JsonPrimitive(false),0,1,1,List.of(),false);
        session.add(new Backend(){
          public String id(){return "fixture";} public List<Path> files(){return List.of();}
          public void validate(Map<String,JsonElement> values){}
          public void apply(Map<String,JsonElement> values){if(values.get("fixture.fail").getAsBoolean())throw new IllegalStateException("injected save failure");}
        },List.of(failure)); failure.value=new JsonPrimitive(true);
        boolean failed=false;try{session.save(game());}catch(IllegalStateException expected){failed=true;}
        check(failed,"save unexpectedly succeeded");expectDifficulty(mc,1);session.discard();
        var stale=SettingsRegistry.open(mc);find(stale,"world.difficulty").value=new JsonPrimitive(3);
        var world=mc.world;mc.world=null;failed=false;
        try{stale.save(game());}catch(IllegalArgumentException expected){failed=true;}finally{mc.world=world;}
        check(failed,"stale world save accepted");expectDifficulty(mc,1);
        world.isRemote=true;
        try{check(SettingsRegistry.open(mc).settings().stream().noneMatch(s->s.backend.equals("world")),"multiplayer exposes local world settings");}
        finally{world.isRemote=false;}
      });
      test("Peaceful removes hostile mobs and restores health; Easy preserves hostiles",()-> {
        var hostile=net.minecraft.class_206.method_732("Zombie",mc.world);
        hostile.method_1340(mc.player.x+2,mc.player.y,mc.player.z);
        change(mc,1);hostile.tick();check(!hostile.dead,"Easy removed hostile");
        change(mc,0);hostile.tick();check(hostile.dead,"Peaceful retained hostile");
        int health=mc.player.health; mc.player.health=10;
        try{for(int i=0;i<30;i++)mc.player.tick();check(mc.player.health>10,"Peaceful health regeneration stopped");}
        finally{mc.player.health=health;}
      });
      test("every setting has accurate explicit scope; command exemptions have correct help",()-> {
        var session=SettingsRegistry.open(mc);
        for(Setting s:session.settings()) {
          String tip=Tooltips.setting(s,"");
          check(tip.contains(SettingScope.note(s)),"missing scope: "+s.id);
          if(s.id.startsWith("commands.world.")&&!SettingAccess.cheat(s))
            check(tip.contains("do not restrict"),"misleading non-cheat command help: "+s.id);
        }
        check(SettingScope.perWorld(find(session,"world.difficulty")),"difficulty scope");
        check(!SettingScope.perWorld(find(session,"power_controls:general.autosaveInterval")),"autosave scope");
      });
      test("cheats off hides rows and empty tabs in normal search related conflicts and changes views",()-> {
        cheat.power$cheatsEnabled(false);
        var screen=new PowerOptionsScreen(null);mc.setScreen(screen);screen.init(mc,640,420);
        cleanView(screen,"Creative");
        check(!screen.visiblePages().contains("Creative")&&!screen.visiblePages().contains("World editing"),"empty cheat tabs visible");
        check(screen.visiblePages().contains("Commands"),"non-cheat Commands tab hidden");
        check(field(screen,"page").equals("General"),"hidden remembered page not repaired");
        for(String tab:screen.visiblePages()) {
          cleanView(screen,tab);
          for(Setting row:rows(screen))check(!SettingAccess.cheat(row),"cheat row visible "+row.id);
        }
        cleanView(screen,"Commands");
        for(var command:CommandPermissions.COMMANDS)if(!command.cheat()) {
          check(rows(screen).stream().anyMatch(s->s.id.equals("commands.rule."+command.name())),"missing non-cheat "+command.name());
          check(rows(screen).stream().anyMatch(s->s.id.equals("commands.world."+command.name())),"missing world non-cheat "+command.name());
        }
        for(String query:List.of("creative","teleport","wand","world editing","game mode")) {
          search(screen,query);for(Setting row:rows(screen))check(!SettingAccess.cheat(row),"search leak "+row.id);
        }
        var hidden=find(screen.session(),"creative.flight");hidden.cycle(1);
        search(screen,"");field(screen,"changedOnly",true);call(screen,"layout");
        for(Setting row:rows(screen))check(!SettingAccess.cheat(row),"changed filter leak");
        field(screen,"changedOnly",false);field(screen,"relatedIds",List.of("creative.flight","keys.power_creative.modifier","creative.sprintMultiplier"));call(screen,"layout");
        check(rows(screen).size()==1&&rows(screen).get(0).id.equals("creative.sprintMultiplier"),"related filter leak");
        field(screen,"relatedIds",List.of());field(screen,"conflictIds",List.of("keys.power_creative.picker","keys.key.forward"));call(screen,"layout");
        check(rows(screen).size()==1&&rows(screen).get(0).id.equals("keys.key.forward"),"conflict filter leak");
        cleanView(screen,"General");var toggle=find(screen.session(),"world.cheats");toggle.value=new JsonPrimitive(true);screen.session().link(toggle);call(screen,"layout");
        check(screen.visiblePages().containsAll(List.of("Creative","World editing")),"tabs did not return");
        check(!cheat.power$cheatsEnabled(),"draft enabled cheats before Apply");
        for(int width:new int[]{320,640}) {
          screen.init(mc,width,width==320?240:420);cleanView(screen,"General");
          field(screen,"sideScroll",0d);call(screen,"layout");
          int x=(int)call(screen,"origin")+8;
          ((ScreenInput)(Object)screen).power$click(x,24+22+8,0);
          check(field(screen,"page").equals("Video"),"tab click mismatch at "+width);
        }
        toggle.value=new JsonPrimitive(false);screen.session().link(toggle);screen.session().discard();
        cleanView(screen,"Controls");field(screen,"showDisabled",true);call(screen,"layout");
        check(rows(screen).stream().noneMatch(SettingAccess::cheat),"show disabled exposed cheats");
        check(rows(screen).stream().anyMatch(s->s.id.equals("keys.power_creative.sprint")),"shared freecam binding hidden");
        Setting a=find(screen.session(),"keys.key.forward"), b=find(screen.session(),"keys.key.back");
        var method=PowerOptionsScreen.class.getDeclaredMethod("conflictTip",Setting.class,List.class);method.setAccessible(true);
        check(((String)method.invoke(screen,a,List.of(b))).contains("\n\nThe matching binding"),"conflict notice not separated");
        mc.setScreen(null);
      });
      test("hidden creative bindings release modifier priority and mouse ownership",()-> {
        var keysField=Keyboard.class.getDeclaredField("keyDownBuffer");keysField.setAccessible(true);
        ByteBuffer keys=(ByteBuffer)keysField.get(null);byte k=keys.get(35),ctrl=keys.get(29);
        var saved=Bindings.modifiers();
        record Key(String id,int code) implements Binding {public String power$id(){return id;}public int power$code(){return code;}}
        var normal=new Key("fixture.normal",35);var hidden=new Key("power_creative.picker",35);var mouse=new Key("power_creative.modifier",-98);
        try {
          keys.put(35,(byte)0);keys.put(29,(byte)0);Bindings.register(new Object[]{normal,hidden,mouse});
          Bindings.configure(Map.of(hidden.id(),Chord.CTRL));
          keys.put(29,(byte)1);keys.put(35,(byte)1);
          for(boolean enabled:new boolean[]{false,true,false}) {
            cheat.power$cheatsEnabled(enabled);Bindings.register(new Object[]{normal,hidden,mouse});
            check(Bindings.down(normal)!=enabled,"hidden binding steals plain key");
            check(Bindings.down(hidden)==enabled,"cheat binding active gate differs");
            check(Bindings.claimsMouse(-98)==enabled,"mouse ownership differs");
          }
          keys.put(29,(byte)0);check(Bindings.down(normal),"modifier release locked normal key");
          keys.put(35,(byte)0);Bindings.register(new Object[]{normal,hidden,mouse});
          keys.put(35,(byte)1);check(Bindings.down(normal),"next plain press failed");
        } finally {keys.put(35,k);keys.put(29,ctrl);Bindings.configure(saved);Bindings.register(mc.options.allKeys);}
      });
      test("difficulty command updates only this world and respects cheat access",()-> {
        cheat.power$cheatsEnabled(true);var rules=CommandPermissions.copy();
        try{
          CommandPermissions.preview(new CommandPermissions.Settings());
          for(String choice:List.of("peaceful","easy","normal","hard")) {
            var feedback=ChatChecks.submit(mc,"/difficulty "+choice);check(feedback.stream().noneMatch(s->s.contains("§c")),"command error "+feedback);
            expectDifficulty(mc,List.of("peaceful","easy","normal","hard").indexOf(choice));mc.tick();
            expectDifficulty(mc,List.of("peaceful","easy","normal","hard").indexOf(choice));
          }
          cheat.power$cheatsEnabled(false);ChatChecks.submit(mc,"/difficulty peaceful");expectDifficulty(mc,3);
          change(mc,1);expectDifficulty(mc,1);
        }finally{CommandPermissions.preview(rules);}
      });
    } finally {
      mc.options.difficulty=nativeBefore;difficulty(mc).power$difficulty(before);mc.world.field_213=before;
      cheat.power$cheatsEnabled(enabledBefore);mc.setScreen(parent);
    }
    log("WORLD SETTINGS FAILURES "+failures);
  }

  private static void travel(Minecraft mc) throws Exception {
    // StationAPI portals supply a travel agent; calling the bare native hook omits it.
    mc.player.health=20;mc.player.dead=false;
    Object nether=Class.forName("net.modificationstation.stationapi.api.world.dimension.VanillaDimensions").getField("THE_NETHER").get(null);
    for(var method:Class.forName("net.modificationstation.stationapi.api.world.dimension.DimensionHelper").getMethods())
      if(method.getName().equals("switchDimension")) {method.invoke(null,mc.player,nether,.125,new net.minecraft.class_467());return;}
    throw new IllegalStateException("Missing dimension API");
  }

  private static void extra(Minecraft mc) throws Exception {
    failures=0;
    int before=difficulty(mc).power$difficulty();
    try {
      test("difficulty changed in Nether follows the save back through respawn",()-> {
        change(mc,1);travel(mc);expectDifficulty(mc,1);
        change(mc,3);mc.tick();expectDifficulty(mc,3);
        mc.player.health=0;mc.player.dead=true;mc.method_2122(false,0);expectDifficulty(mc,3);mc.tick();expectDifficulty(mc,3);
      });
      test("remote world keeps vanilla server difficulty and exposes no local world settings",()-> {
        change(mc,2);var world=mc.world;int option=mc.options.difficulty;
        world.isRemote=true;mc.options.difficulty=0;
        try {
          var session=SettingsRegistry.open(mc);
          check(session.settings().stream().noneMatch(s->s.backend.equals("world")),"remote world settings exposed");
          mc.tick();check(world.field_213==3,"remote difficulty no longer follows vanilla server boundary");
          check(difficulty(mc).power$difficulty()==2,"remote tick changed stored value");
        } finally {world.isRemote=false;mc.options.difficulty=option;world.field_213=2;}
      });
    } finally {change(mc,before);}
    log("WORLD EXTRA FAILURES "+failures);
  }

  private static void scope(Minecraft mc) throws Exception {
    var session=SettingsRegistry.open(mc);var entries=new ArrayList<Map<String,Object>>();
    int maximum=0;String longest="";
    for(Setting setting:session.settings()) {
      String help=Tooltips.setting(setting,"");
      entries.add(Map.of("id",setting.id,"perWorld",SettingScope.perWorld(setting),"cheat",SettingAccess.cheat(setting),"tooltip",help));
      int lines=0;
      for(String paragraph:help.split("\n")) {
        String line="";
        for(String word:paragraph.split(" ")) {
          if(!line.isEmpty()&&mc.textRenderer.getWidth(line+" "+word)>280) {lines++;line="";}
          line+=(line.isEmpty()?"":" ")+word;
        }
        lines++;
      }
      if(lines>maximum){maximum=lines;longest=setting.id;}
    }
    Files.writeString(Path.of("power-beta-setting-scopes.json"),Catalog.JSON.toJson(entries));
    log("SCOPES "+entries.size()+" MAX TOOLTIP LINES "+maximum+" "+longest);
    var original=mc.currentScreen;
    org.lwjgl.opengl.GL11.glPushAttrib(org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS);
    org.lwjgl.opengl.GL11.glMatrixMode(org.lwjgl.opengl.GL11.GL_PROJECTION);org.lwjgl.opengl.GL11.glPushMatrix();
    org.lwjgl.opengl.GL11.glMatrixMode(org.lwjgl.opengl.GL11.GL_MODELVIEW);org.lwjgl.opengl.GL11.glPushMatrix();
    try {
      Path dir=Path.of("power-beta-data/reports/world-settings");Files.createDirectories(dir);
      for(int[] size:new int[][]{{640,420},{320,240}}) {
        var create=new net.minecraft.class_180(null);create.init(mc,size[0],size[1]);
        SettingsSnapshot.render(mc,create,size[0],size[1],dir.resolve("creation-"+size[0]+".png"));
        for(String id:List.of("world.difficulty","power_controls:general.autosaveInterval","commands.world.seed","keys.key.forward")) {
          var options=new PowerOptionsScreen(null);options.init(mc,size[0],size[1]);
          cleanView(options,find(options.session(),id).page);search(options,find(options.session(),id).label);
          for(Object row:(List<?>)field(options,"rows")) {
            Setting setting=(Setting)call(row,"setting");if(setting==null||!setting.id.equals(id))continue;
            field(options,"scroll",(double)(int)call(row,"y"));call(options,"layout");
            int y=(int)call(options,"top")+(int)call(row,"y")-((Double)field(options,"scroll")).intValue()+8;
            int x=(int)call(options,"left")+10;
            SettingsSnapshot.render(mc,options,size[0],size[1],dir.resolve(id.replace(':','-')+"-"+size[0]+".png"),x,y);
            break;
          }
        }
      }
    } finally {
      org.lwjgl.opengl.GL11.glMatrixMode(org.lwjgl.opengl.GL11.GL_MODELVIEW);org.lwjgl.opengl.GL11.glPopMatrix();
      org.lwjgl.opengl.GL11.glMatrixMode(org.lwjgl.opengl.GL11.GL_PROJECTION);org.lwjgl.opengl.GL11.glPopMatrix();org.lwjgl.opengl.GL11.glPopAttrib();
      mc.setScreen(original);
    }
  }

  private static void creation(Minecraft mc) throws Exception {
    mc.setWorld(null);
    for(int chosen=0;chosen<4;chosen++) {
      final int value=chosen;
      test("new-world choice, Cancel, resize, save and reload: "+value,()-> {
        var screen=new net.minecraft.class_180(null);mc.setScreen(screen);screen.init(mc,320,240);
        var input=(ScreenInput)screen;
        var button=input.power$buttons().stream().filter(b->b.id==3).findFirst().orElseThrow();
        var cheats=input.power$buttons().stream().filter(b->b.id==2).findFirst().orElseThrow();
        check(button.text.equals("Difficulty: Normal"),"creation inherited another world");
        check(button.x>=cheats.x+88&&button.x+108<=320,"creation choices overlap or clip");
        for(int i=0;i<(value+2)%4;i++)input.power$click(button.x+5,button.y+5,0);
        check(button.text.endsWith(List.of("Peaceful","Easy","Normal","Hard").get(value)),"left click cycle");
        input.power$click(button.x+5,button.y+5,1);input.power$click(button.x+5,button.y+5,0);
        if(value%2==1)input.power$click(cheats.x+5,cheats.y+5,0);
        screen.init(mc,640,420);button=input.power$buttons().stream().filter(b->b.id==3).findFirst().orElseThrow();
        check(button.text.endsWith(List.of("Peaceful","Easy","Normal","Hard").get(value)),"resize lost choice");
        String name="World difficulty "+value;
        ((net.minecraft.client.gui.widget.TextFieldWidget)field(screen,"field_632")).setText(name);
        ((net.minecraft.client.gui.widget.TextFieldWidget)field(screen,"field_633")).setText("17320261007");
        input.power$key('\0',Keyboard.KEY_RIGHT);String folder=(String)field(screen,"field_634");
        var create=input.power$buttons().stream().filter(b->b.id==0).findFirst().orElseThrow();input.power$click(create.x+5,create.y+5,0);
        expectDifficulty(mc,value);check(((CheatWorld)mc.world.method_262()).power$cheatsEnabled()==(value%2==1),"cheat choice lost");
        mc.world.method_195(true,null);mc.setWorld(null);
        mc.interactionManager=new net.minecraft.SingleplayerInteractionManager(mc);mc.method_2120(folder,name,17320261007L);
        expectDifficulty(mc,value);mc.tick();expectDifficulty(mc,value);
        Files.writeString(Path.of("power-beta-difficulty-"+value+".txt"),folder);
        mc.setWorld(null);
      });
    }
    var canceled=new net.minecraft.class_180(null);mc.setScreen(canceled);var input=(ScreenInput)canceled;
    var d=input.power$buttons().stream().filter(b->b.id==3).findFirst().orElseThrow();input.power$click(d.x+5,d.y+5,0);
    mc.setScreen(new net.minecraft.class_180(null));
    check(((ScreenInput)mc.currentScreen).power$buttons().stream().filter(b->b.id==3).findFirst().orElseThrow().text.equals("Difficulty: Normal"),"Cancel changed default");
    mc.setScreen(null);log("WORLD CREATION FAILURES "+failures);
  }

  private static void reload(Minecraft mc) throws Exception {
    for(int value:new int[]{3,0,2,1,3}) {
      final int expected=value;
      test("world switch/restart preserves difficulty "+value,()-> {
        mc.setWorld(null);mc.interactionManager=new net.minecraft.SingleplayerInteractionManager(mc);
        String folder=Files.readString(Path.of("power-beta-difficulty-"+expected+".txt"));mc.method_2120(folder,folder,17320261007L);
        expectDifficulty(mc,expected);mc.tick();expectDifficulty(mc,expected);
        if(expected==1) {
          travel(mc);expectDifficulty(mc,expected);mc.tick();expectDifficulty(mc,expected);
          travel(mc);expectDifficulty(mc,expected);
        }
      });
    }
    log("WORLD RELOAD FAILURES "+failures);
  }
}
