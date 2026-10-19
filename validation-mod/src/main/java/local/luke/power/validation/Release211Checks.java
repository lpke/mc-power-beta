package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.input.*;
import local.luke.power.rules.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.world.*;
import local.luke.power.validation.mixin.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.widget.*;
import net.minecraft.nbt.*;
import org.lwjgl.input.Keyboard;

/** Fixtures run only in a disposable, muted client. */
final class Release211Checks {
  static void run(Minecraft mc) throws Exception {
    failures = 0;
    var rules = AccessRules.copy(mc); var saved = new NbtCompound(); mc.player.write(saved);
    var cycles = (WorldCycles)mc.world.method_262(); long day = cycles.power$daylightTime();
    int difficulty = mc.world.field_213, skyDarkness = mc.world.field_202;
    var screen = mc.currentScreen;
    try {
      mc.setScreen(null); DeathChecks.alive(mc); mc.world.field_213 = 0;
      test("bed respawn works by day with sleep on or off and only night sleep occupies beds", () -> {
        var bed = Arrays.stream(net.minecraft.block.Block.BLOCKS).filter(b -> b instanceof net.minecraft.class_485).findFirst().orElseThrow();
        var sleep = (PlayerSleepAccess)mc.player;
        for (boolean allowed : new boolean[]{true, false}) for (boolean night : new boolean[]{false, true}) {
          AccessRules.set(mc, GameRule.ALLOW_SLEEP, allowed); cycles.power$daylightTime(night ? 18000 : 6000);
          mc.world.field_202 = night ? 11 : 0;
          int x = 42, y = 110, z = 42;
          for (int dx=-2;dx<=2;dx++) for (int dz=-2;dz<=2;dz++) {
            mc.world.method_200(x+dx,y-1,z+dz,1);
            for(int dy=0;dy<4;dy++) mc.world.method_200(x+dx,y+dy,z+dz,0);
          }
          mc.world.method_154(x,y,z,bed.id,8);
          mc.player.method_1340(x+.5,y+mc.player.field_1631,z+.5);
          sleep.power$sleeping(false);sleep.power$sleepTimer(0);mc.player.method_506(null);
          bed.method_1608(mc.world,x,y,z,mc.player);
          var spawn=mc.player.method_505();
          check(spawn!=null&&spawn.x==x&&spawn.y==y&&spawn.z==z,"respawn missing: "+allowed+" / "+night);
          check(mc.player.method_943()==(allowed&&night),"sleep state: "+allowed+" / "+night);
          check(((mc.world.method_1778(x,y,z)&4)!=0)==(allowed&&night),"respawn-only use occupied the bed");
          if (mc.player.method_943()) mc.player.method_497(true,true,false);
          sleep.power$sleeping(false);sleep.power$sleepTimer(0);
        }
      });
      test("HUD includes follow display controls and full bright messages follow brightness", () -> {
        var session = SettingsRegistry.open(mc);
        var rows = SettingsLayout.ordered(session.settings());
        boolean include = false; int toggles=0;
        for (var setting:rows) if(setting.page.equals("Interface")&&setting.group.equals("Active tweaks HUD")) {
          if(setting.label.startsWith("Include ")) {include=true;toggles++;}
          else check(!include,"display control after includes: "+setting.id);
        }
        check(toggles==11,"HUD include count");
        var lighting=rows.stream().filter(s->s.page.equals("Video")&&s.group.equals("Lighting")).map(s->s.id).toList();
        check(lighting.indexOf("visual.fullBrightMessages")==lighting.indexOf("native.brightness")+1,"brightness order");
        check(session.settings().stream().noneMatch(s->s.id.endsWith("panoramaConfig.enablePanorma")),"duplicate panorama toggle");
      });
      test("picker previews selections immediately, keeps drafts and restores the original dirt choice", () -> {
        boolean auto = MenuPreferences.current().autoApply;
        var original = PowerConfig.section("power_controls:userinterface");
        Files.createDirectories(Path.of("power-beta-data/reports/211"));
        try {
          for(boolean apply:new boolean[]{false,true}) {
            MenuPreferences.update(v->v.autoApply=apply);
            var options=new PowerOptionsScreen(null);mc.setScreen(options);
            Setting choice=find(options.session(),"power_controls:userinterface.panoramaConfig.panoramaFolder");
            Setting blur=find(options.session(),"power_controls:userinterface.panoramaConfig.blurBackground");
            var picker=new PanoramaScreen(options,choice);mc.setScreen(picker);
            selectPanorama(picker,"26_3");
            check(mc.currentScreen==picker&&choice.value.getAsString().equals("26_3"),"selection did not stay in picker");
            if(apply) {
              var config=PowerConfig.section("power_controls:userinterface").getAsJsonObject("panoramaConfig");
              check(config.get("enablePanorma").getAsBoolean()&&config.get("panoramaFolder").getAsString().equals("26_3"),"linked panorama save");
              picker.init(mc,320,240);
              SettingsSnapshot.render(mc,picker,320,240,Path.of("power-beta-data/reports/211/panorama-selected.png"));
            }
            for(int[] size:new int[][]{{320,240},{640,420}}) {
              picker.init(mc,size[0],size[1]); field(picker,"scroll",0d);
              int left=(int)call(picker,"left"), span=(int)call(picker,"span");
              check(Math.abs(left*2+span-size[0])<=1,"panorama buttons are not centered");
              check(PanoramaCatalog.folder("b1_8_1").equals(PanoramaCatalog.folder("1_0")),"classic panorama source");
              selectPanorama(picker,"b1_8_1");
              check(choice.value.getAsString().equals("b1_8_1"),"right edge of centered button did not select");
              SettingsSnapshot.render(mc,picker,size[0],size[1],Path.of("power-beta-data/reports/211/panorama-classic-"+apply+"-"+size[0]+".png"));
              field(picker,"scroll",0d);
              SettingsSnapshot.render(mc,picker,size[0],size[1],Path.of("power-beta-data/reports/211/panorama-"+apply+"-"+size[0]+".png"));
            }
            boolean previousBlur=blur.value.getAsBoolean();
            ((ScreenInput)(Object)picker).power$click(picker.width/2,picker.height-42,0);
            check(blur.value.getAsBoolean()!=previousBlur,"picker blur did not update the shared setting");
            if(apply) check(PowerConfig.section("power_controls:userinterface").getAsJsonObject("panoramaConfig").get("blurBackground").getAsBoolean()==!previousBlur,"blur was not saved immediately");
            SettingsSnapshot.render(mc,picker,640,420,Path.of("power-beta-data/reports/211/panorama-blur-"+apply+".png"));
            selectPanorama(picker,PanoramaCatalog.DIRT);
            check(choice.value.getAsString().equals(PanoramaCatalog.DIRT),"dirt choice");
            if(apply) check(!PowerConfig.section("power_controls:userinterface").getAsJsonObject("panoramaConfig").get("enablePanorma").getAsBoolean(),"dirt kept panorama enabled");
            ((ScreenInput)(Object)picker).power$key('\0',Keyboard.KEY_ESCAPE);
            check(mc.currentScreen==options,"Escape did not return");
            if(!apply) options.session().discard();
          }
        } finally {
          PowerConfig.put("power_controls:userinterface",original);
          // Reload the live adapter too; future settings sessions must see the restored pair.
          for(var handler:net.glasslauncher.mods.gcapi3.impl.GCCore.MOD_CONFIGS.entrySet())
            if(handler.getKey().equals("power_controls:userinterface"))
              net.glasslauncher.mods.gcapi3.impl.GCCore.loadModConfig(handler.getValue().configRoot(),handler.getValue().modContainer(),handler.getValue().configCategoryHandler().parentField,handler.getKey(),null);
          MenuPreferences.update(v->v.autoApply=auto);
        }
      });
      test("saved-world editor refuses a currently loaded world", () -> {
        try { PowerOptionsScreen.openSavedWorldRules(mc,null,"Power Beta release check"); throw new AssertionError("opened while loaded"); }
        catch(IOException expected) {check(mc.world!=null,"loaded world changed");}
      });
    } finally {
      if (mc.player.method_943()) mc.player.method_497(true,true,false);
      AccessRules.restore(mc,rules); cycles.power$daylightTime(day); mc.world.field_213=difficulty; mc.world.field_202=skyDarkness;
      mc.player.read(saved); mc.setScreen(screen);
    }
    log("RELEASE 211 FAILURES "+failures);
  }

  static void editor(Minecraft mc) throws Exception {
    failures=0;check(mc.world==null,"leave the disposable world first");
    Path game=Path.of(".").toAbsolutePath().normalize(),folder=game.resolve("saves/Power Beta 211 editor");
    Files.createDirectories(folder);
    var root=WorldRuleFiles.read(game.resolve("saves/Power Beta release check/level.dat"));
    root.getCompound("Data").putString("LevelName","211 editor fixture");
    root.getCompound("Data").getCompound("Player").putString("PowerBetaGameMode","SURVIVAL");
    write(folder.resolve("level.dat"),root);Files.copy(folder.resolve("level.dat"),folder.resolve("level.dat_old"),StandardCopyOption.REPLACE_EXISTING);
    byte[] original=Files.readAllBytes(folder.resolve("level.dat")); byte[] player=encode(root.getCompound("Data").getCompound("Player"));
    boolean auto=MenuPreferences.current().autoApply;
    try {
      MenuPreferences.update(v->v.autoApply=false);
      byte[] config=Files.readAllBytes(PowerConfig.path());
      test("select world Edit opens rules, confirms drafts and preserves name and player data", () -> {
        var select=new net.minecraft.client.gui.screen.world.SelectWorldScreen(new TitleScreen());mc.setScreen(select);
        var worlds=(List<net.minecraft.class_591>)field(select,"field_2436");
        int index=-1;for(int i=0;i<worlds.size();i++) if(worlds.get(i).method_1956().equals(folder.getFileName().toString())) index=i;
        check(index>=0,"fixture missing from native world list");field(select,"field_2435",index);
        var edit=((ScreenInput)select).power$buttons().stream().filter(b->b.id==6).findFirst().orElseThrow();edit.active=true;
        ((ScreenInput)select).power$click(edit.x+5,edit.y+5,0);
        check(mc.currentScreen.getClass().getName().equals("net.modificationstation.stationapi.api.client.gui.screen.EditWorldScreen"),
            "StationAPI Edit menu missing: " + mc.currentScreen.getClass().getName());
        var menu=mc.currentScreen;
        for(int[] size:new int[][]{{320,240},{640,420}}) {
          menu.init(mc,size[0],size[1]);
          SettingsSnapshot.render(mc,menu,size[0],size[1],Path.of("power-beta-data/reports/211/edit-world-"+size[0]+".png"));
        }
        pressRules(menu);
        check(mc.currentScreen instanceof PowerOptionsScreen,"rule editor did not open");
        var options=(PowerOptionsScreen)mc.currentScreen;options.tick();
        check(options.session().settings().size()==GameRule.values().length,"unrelated settings in saved-world editor");
        var creative=find(options.session(),"rules.world.allow_creative_mode");creative.value=new JsonPrimitive(true);options.changed(creative);
        check(Arrays.equals(original,Files.readAllBytes(folder.resolve("level.dat"))),"manual draft touched world");
        ((ScreenInput)(Object)options).power$key('\0',Keyboard.KEY_ESCAPE);
        check((Boolean)field(options,"confirmClose"),"dirty exit has no confirmation");
        ((ScreenInput)(Object)options).power$click(options.width/2-95,options.height/2+18,0);
        check(mc.currentScreen==menu,"Save did not return to Edit");
        ((ScreenInput)menu).power$key('\0',Keyboard.KEY_ESCAPE);
        check(mc.currentScreen==select,"Edit menu Escape did not return to Select world");
        var after=WorldRuleFiles.read(folder.resolve("level.dat")).getCompound("Data");
        check(WorldRuleData.read(after).enabled(GameRule.ALLOW_CREATIVE_MODE),"rule save missing");
        check(Arrays.equals(player,encode(after.getCompound("Player"))),"rule edit changed player data or game mode");
        check(after.getString("LevelName").equals("211 editor fixture"),"rules changed the world name");
        check(Arrays.equals(original,Files.readAllBytes(folder.resolve("level.dat_old"))),"previous metadata changed");
        check(Arrays.equals(config,Files.readAllBytes(PowerConfig.path())),"world edit changed global preferences");
      });
      test("unsupported saved rules open a recovery screen when editing game rules", () -> {
        mc.setScreen(new TitleScreen());
        var future=WorldRuleFiles.read(folder.resolve("level.dat"));future.getCompound("Data").getCompound("GameRules").putInt("version",999);
        write(folder.resolve("level.dat"),future);byte[] bytes=Files.readAllBytes(folder.resolve("level.dat"));
        var info=new net.minecraft.class_591(folder.getFileName().toString(),"211 editor fixture",0,0,false);
        var menu=(Screen)Class.forName("net.modificationstation.stationapi.api.client.gui.screen.EditWorldScreen")
            .getConstructor(Screen.class,net.minecraft.class_591.class).newInstance(new TitleScreen(),info);
        mc.setScreen(menu); pressRules(menu);
        check(mc.currentScreen instanceof DataProblemScreen,"future metadata did not block Edit safely");
        check(Arrays.equals(bytes,Files.readAllBytes(folder.resolve("level.dat"))),"future metadata overwritten");
      });
    } finally {MenuPreferences.update(v->v.autoApply=auto);mc.setScreen(new TitleScreen());}
    log("RELEASE 211 EDITOR FAILURES "+failures);
  }
  private static void pressRules(Screen menu)throws Exception {
    var buttons=(List<?>)field(field(menu,"buttonMenu"),"editButtons");
    for(Object context:buttons) {
      var button=(ButtonWidget)context.getClass().getMethod("button").invoke(context);
      if(!button.text.equals("Edit game rules")) continue;
      check(button.x>=0&&button.x+200<=menu.width,"rule button outside menu");
      Object action=context.getClass().getMethod("action").invoke(context);
      Class.forName("net.modificationstation.stationapi.api.client.gui.widget.PressAction")
          .getMethod("onPress",ButtonWidget.class).invoke(action,button);
      return;
    }
    throw new AssertionError("Edit game rules button missing");
  }
  private static byte[] encode(NbtCompound root)throws Exception {var output=new ByteArrayOutputStream();NbtIo.writeCompressed(root,output);return output.toByteArray();}
  private static void write(Path path,NbtCompound root)throws Exception {Files.write(path,encode(root));}
}
