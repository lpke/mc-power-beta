package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;

import com.google.gson.JsonPrimitive;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.permissions.CheatWorld;
import local.luke.power.ui.PowerOptionsScreen;
import local.luke.power.world.WorldCycles;
import net.minecraft.client.Minecraft;
import net.minecraft.class_585;
import net.minecraft.world.WorldProperties;
import org.lwjgl.opengl.GL11;

/** Current-format settings and world-cycle checks; disposable worlds only. */
final class CleanupChecks {
  static void run(Minecraft mc) throws Exception {
    failures = 0;
    var properties = mc.world.method_262();
    var cheats = (CheatWorld) properties;
    var cycles = (WorldCycles) properties;
    boolean beforeCheats = cheats.power$cheatsEnabled();
    boolean day = cycles.power$daylightCycle(), weather = cycles.power$weatherCycle();
    long daylight = cycles.power$daylightTime();
    int difficulty = mc.world.field_213;
    try {
      cheats.power$cheatsEnabled(true);
      mc.world.field_213 = 0;
      test("current world rules survive copies and NBT without altering simulation time", () -> {
        cycles.power$daylightCycle(false); cycles.power$weatherCycle(false);
        var restored = new WorldProperties(properties.asNbt());
        var copied = new WorldProperties(restored);
        check(!((WorldCycles) copied).power$daylightCycle() && !((WorldCycles) copied).power$weatherCycle(), "rules lost");
        check(((WorldCycles) copied).power$daylightTime() == daylight, "daylight lost");
        long simulation = copied.getTime();
        copied.setTime(simulation + 400);
        check(copied.getTime() == simulation + 400 && ((WorldCycles) copied).power$daylightTime() == daylight, "simulation frozen");
        ((CheatWorld) copied).power$cheatsEnabled(false);
        copied.setTime(simulation + 401);
        check(((WorldCycles) copied).power$daylightTime() == daylight + 1, "cheats-off did not suspend freeze");
      });
      test("world ticks and scheduled block updates continue with frozen daylight", () -> {
        cycles.power$daylightCycle(false);
        long time = mc.world.getTime(), sun = cycles.power$daylightTime();
        int x = (int)mc.player.x, z = (int)mc.player.z;
        mc.world.method_200(x, 100, z, 1);
        mc.world.method_216(x, 100, z, 1, 2);
        var pending = (Collection<?>) field(mc.world, "field_183");
        check(pending.stream().anyMatch(t -> { try { return (int)field(t,"field_1400")==x && (int)field(t,"field_1401")==100 && (int)field(t,"field_1402")==z; } catch(Exception e) { return false; } }), "scheduled update missing");
        for (int i = 0; i < 5; i++) mc.world.method_242();
        check(mc.world.getTime() == time + 5, "world clock stopped");
        check(cycles.power$daylightTime() == sun, "sun advanced");
        check(mc.world.method_198(0) == mc.world.method_198(1), "frozen sky jitters");
        check(pending.stream().noneMatch(t -> { try { return (int)field(t,"field_1400")==x && (int)field(t,"field_1401")==100 && (int)field(t,"field_1402")==z; } catch(Exception e) { return false; } }), "scheduled update stalled");
        cycles.power$daylightCycle(true); mc.world.method_242();
        check(cycles.power$daylightTime() == sun + 1, "daylight did not resume in place");
      });
      test("frozen weather retains flags and timers through ticks and sleep cleanup", () -> {
        boolean rain = properties.getRaining(), thunder = properties.getThundering();
        int rainTime = properties.getRainTime(), thunderTime = properties.getThunderTime();
        try {
          properties.setRaining(true); properties.setThundering(true);
          properties.setRainTime(1000); properties.getThunderTime(900);
          cycles.power$weatherCycle(false); field(mc.world,"field_209",2);
          call(mc.world,"method_245"); call(mc.world,"method_266");
          check(properties.getRaining() && properties.getThundering(), "sleep cleared frozen weather");
          check(properties.getRainTime()==1000 && properties.getThunderTime()==900, "frozen timers changed");
          check((int)field(mc.world,"field_209")==1,"lightning flash stopped expiring");
          cycles.power$weatherCycle(true); call(mc.world,"method_245");
          check(properties.getRainTime()==999 && properties.getThunderTime()==899,"weather timers did not resume");
        } finally { properties.setRaining(rain);properties.setThundering(thunder);properties.setRainTime(rainTime);properties.getThunderTime(thunderTime); }
      });
      test("sleep and accelerated sleep respect frozen cycles and resume at dawn", () -> {
        var session=SettingsRegistry.open(mc);
        String speed="power_environment:config.SLEEP_CONFIG.bedsSpeedUpNightRatherThanSkipIt";
        String resets="power_environment:config.WEATHER_CONFIG.sleepOnlyResetsWeatherWhenRaining";
        for(boolean fast:List.of(false,true)) for(boolean rainOnly:List.of(false,true)) {
          find(session,speed).value=new JsonPrimitive(fast);find(session,resets).value=new JsonPrimitive(rainOnly);session.preview(true);
          cycles.power$daylightCycle(false);cycles.power$weatherCycle(false);cycles.power$daylightTime(18000);
          properties.setRaining(true);properties.setThundering(true);properties.setRainTime(800);properties.getThunderTime(700);
          ((local.luke.power.validation.mixin.PlayerSleepAccess)mc.player).power$sleeping(true);((local.luke.power.validation.mixin.PlayerSleepAccess)mc.player).power$sleepTimer(100);mc.world.method_264();
          long before=mc.world.getTime();mc.world.method_242();
          check(!mc.player.method_943(),"sleep did not wake with frozen daylight");
          check(cycles.power$daylightTime()==18000 && mc.world.getTime()==before+1,"sleep advanced frozen daylight or accelerated simulation");
          check(properties.getRaining()&&properties.getThundering()&&properties.getRainTime()==800,"sleep changed frozen weather");
          cycles.power$daylightCycle(true);cycles.power$weatherCycle(true);cycles.power$daylightTime(23999);
          ((local.luke.power.validation.mixin.PlayerSleepAccess)mc.player).power$sleeping(true);((local.luke.power.validation.mixin.PlayerSleepAccess)mc.player).power$sleepTimer(100);mc.world.method_264();mc.world.method_242();
          check(!mc.player.method_943(),"sleep did not wake after unfreezing");
          check(cycles.power$daylightTime()>=24000 && cycles.power$daylightTime()<24100,"sleep resumed at wrong dawn");
          check(!properties.getRaining(),"normal sleep failed to clear rain after unfreezing");
        }
        session.discard();
      });
      test("sleep weather rule follows storm state while rain fades in or out", () -> {
        var session=SettingsRegistry.open(mc);
        try {
          find(session,"power_environment:config.WEATHER_CONFIG.sleepOnlyResetsWeatherWhenRaining").value=new JsonPrimitive(true);session.preview(true);
          cycles.power$weatherCycle(true);
          for(boolean storm:List.of(false,true)) {
            properties.setRaining(storm);properties.setThundering(storm);
            properties.setRainTime(800);properties.getThunderTime(700);
            field(mc.world,"field_206",storm?0f:1f);field(mc.world,"field_208",storm?0f:1f);
            call(mc.world,"method_266");
            check(!properties.getRaining()&&!properties.getThundering(),"sleep retained newly started storm");
            check(properties.getRainTime()==(storm?0:800)&&properties.getThunderTime()==(storm?0:700),"sleep used visual fade instead of weather state");
          }
        } finally {session.discard();}
      });
      test("weather freeze pauses weather suppression tweaks and resumes them normally", () -> {
        ConfigSession session=SettingsRegistry.open(mc);
        try {
          for(String name:List.of("disableRain","disableThunder"))find(session,"power_environment:config.WEATHER_CONFIG."+name).value=new JsonPrimitive(true);
          session.preview(true);properties.setRaining(true);properties.setThundering(true);
          properties.setRainTime(800);properties.getThunderTime(700);cycles.power$weatherCycle(false);
          call(mc.world,"method_245");
          check(properties.getRaining()&&properties.getThundering()&&properties.getRainTime()==800&&properties.getThunderTime()==700,"suppression changed frozen weather");
          cycles.power$weatherCycle(true);call(mc.world,"method_245");
          check(!properties.getRaining()&&!properties.getThundering(),"suppression failed to resume");
        } finally {session.discard();}
      });
      test("right-click edits standing and wall signs without consuming items; Shift and Off do not", () -> {
        ConfigSession session=SettingsRegistry.open(mc);
        Setting sign=find(session,"power_mechanics:config.INTERACTIVE_BLOCK_CONFIG.allowEditingSigns");
        var held=mc.player.inventory.getSelectedItem();int count=held==null?0:held.count;
        boolean sneak=mc.player.field_161.field_2536;
        try {
          for(int id:List.of(63,68)) {
            int x=(int)mc.player.x,z=(int)mc.player.z;
            mc.world.method_200(x,100,z,1);mc.world.method_200(x,101,z,id);
            for(boolean enabled:List.of(false,true)) for(boolean shift:List.of(false,true)) {
              sign.value=new JsonPrimitive(enabled);session.preview(true);mc.setScreen(null);mc.player.field_161.field_2536=shift;
              boolean used=net.minecraft.block.Block.BLOCKS[id].method_1608(mc.world,x,101,z,mc.player);
              check(used==(enabled&&!shift),"wrong sign interaction "+id+"/"+enabled+"/"+shift);
              check((mc.currentScreen instanceof net.minecraft.client.gui.screen.ingame.SignEditScreen)==(enabled&&!shift),"wrong sign editor visibility");
              check(held==null||held.count==count,"sign editing consumed an item");
            }
          }
        } finally {mc.player.field_161.field_2536=sneak;mc.setScreen(null);session.discard();}
      });
      test("spectator exits into solid blocks keep position for both destination modes", () -> {
        double x=mc.player.x,y=mc.player.y,z=mc.player.z;
        for (String mode : List.of("creative","survival")) {
          ChatChecks.submit(mc,"/gamemode spectator");
          int bx=(int)Math.floor(x), bz=(int)Math.floor(z);
          for(int yy=100;yy<104;yy++) mc.world.method_200(bx,yy,bz,1);
          mc.player.method_1340(bx+.5,101,bz+.5);
          double px=mc.player.x,py=mc.player.y,pz=mc.player.z;
          ChatChecks.submit(mc,"/gamemode "+mode);
          Object actual=Class.forName("local.luke.power.creative.api.ModePlayer").getMethod("power_mode").invoke(mc.player);
          check(actual.toString().equalsIgnoreCase(mode),"mode change rejected");
          check(mc.player.x==px && mc.player.y==py && mc.player.z==pz,"spectator exit teleported");
        }
        mc.player.method_1340(x,y,z);
      });
      test("item IDs and filter entries reject unknown items before publishing drafts", () -> {
        ConfigSession s=SettingsRegistry.open(mc);
        for(String id:List.of("worldedit.wandItem","tweaks.placement.blacklist","tweaks.placement.whitelist")) {
          Setting setting=find(s,id);var old=setting.value.deepCopy();
          try {setting.parse("32000");throw new AssertionError("invalid ID accepted: "+id);}
          catch(IllegalArgumentException expected) { check(setting.value.equals(old),"invalid draft published"); }
          setting.parse(id.equals("worldedit.wandItem")?"271":"1, 35:4");
        }
        check(!find(s,"worldedit.wandItem").slider(),"item ID still has slider");
        for(Setting setting:s.settings()) if(setting.numeric()) check(Tooltips.setting(setting,"").contains("Range: "),"missing limits "+setting.id);
        Setting duration=find(s,"creative.doubleTapTicks");duration.parse("0.35");check(duration.value.getAsInt()==7,"seconds changed timing");
      });
      test("General cycle rows follow cheats without leaving hidden unsaved changes", () -> {
        ConfigSession s=SettingsRegistry.open(mc);Setting gate=find(s,"world.cheats"), rule=find(s,"world.daylightCycle");
        check(SettingAccess.visible(s,rule),"cycle hidden with cheats on");
        rule.value=new JsonPrimitive(!rule.value.getAsBoolean());gate.value=new JsonPrimitive(false);s.link(gate);
        check(!SettingAccess.visible(s,rule) && !rule.changed(),"hidden cycle retains edit");
        var ids=SettingsLayout.ordered(s.settings()).stream().filter(v->v.page.equals("General")&&v.group.equals("Game")).map(v->v.id).toList();
        check(ids.indexOf("world.difficulty")<ids.indexOf("power_controls:general.autosaveInterval"),"autosave order");
        check(ids.indexOf("world.cheats")+1==ids.indexOf("world.daylightCycle") && ids.indexOf("world.daylightCycle")+1==ids.indexOf("world.weatherCycle"),"cycles not below cheats");
      });
      test("failed saves restore world cycles and the original cheats gate", () -> {
        cheats.power$cheatsEnabled(false);cycles.power$daylightCycle(true);cycles.power$weatherCycle(true);
        ConfigSession session=SettingsRegistry.open(mc);
        session.add(new Backend() {
          public String id(){return "fail-save";}
          public List<Path> files(){return List.of();}
          public void validate(Map<String,com.google.gson.JsonElement> values){}
          public void apply(Map<String,com.google.gson.JsonElement> values){if(values.get("fail-save").getAsBoolean())throw new IllegalStateException("Deliberate save failure");}
        },List.of(new Setting("fail-save","fail-save","General","Game","Failure","",Setting.Kind.BOOLEAN,new JsonPrimitive(false),new JsonPrimitive(false),0,1,1,List.of(),false)));
        for(String id:List.of("world.cheats","fail-save"))find(session,id).value=new JsonPrimitive(true);
        for(String id:List.of("world.daylightCycle","world.weatherCycle"))find(session,id).value=new JsonPrimitive(false);
        try{session.save(Path.of("."));throw new AssertionError("save unexpectedly succeeded");}
        catch(IllegalStateException expected){check(expected.getSuppressed().length==0,"rollback failed");}
        check(!cheats.power$cheatsEnabled()&&cycles.power$daylightCycle()&&cycles.power$weatherCycle(),"world values not restored");
        session.discard();cheats.power$cheatsEnabled(true);
      });
      screenshots(mc);
    } finally {
      cycles.power$daylightCycle(day);cycles.power$weatherCycle(weather);cycles.power$daylightTime(daylight);
      cheats.power$cheatsEnabled(beforeCheats);mc.world.field_213=difficulty;
    }
    log("CLEANUP FAILURES " + failures);
  }

  static void screenshots(Minecraft mc) throws Exception {
    Path dir=Path.of("power-beta-data/reports/cleanup");Files.createDirectories(dir);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPushMatrix();
    GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();
    try {
      ChatChecks.submit(mc,"/gamemode creative");
      for(int[] size:new int[][]{{640,420},{320,240}}) {
        var screen=new class_585(mc.player);mc.setScreen(screen);screen.init(mc,size[0],size[1]);
        field(screen,"creative_normalGUI",false);SettingsSnapshot.render(mc,screen,size[0],size[1],dir.resolve("creative-"+size[0]+".png"));
        field(screen,"creative_normalGUI",true);SettingsSnapshot.render(mc,screen,size[0],size[1],dir.resolve("survival-"+size[0]+".png"));
        var options=new PowerOptionsScreen(null);mc.setScreen(options);options.init(mc,size[0],size[1]);
        field(options,"page","General");((Set<?>)field(options,"collapsed")).clear();call(options,"layout");
        SettingsSnapshot.render(mc,options,size[0],size[1],dir.resolve("general-"+size[0]+".png"));
      }
    } finally {
      GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();GL11.glPopAttrib();mc.setScreen(mc.currentScreen);
    }
  }
}
