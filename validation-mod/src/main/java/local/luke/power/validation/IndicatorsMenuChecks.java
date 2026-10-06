package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import com.google.gson.JsonPrimitive;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.building.config.Config;
import local.luke.power.config.*;
import local.luke.power.input.TweakIndicators;
import local.luke.power.mixin.SoundManagerAccessor;
import local.luke.power.permissions.CheatWorld;
import local.luke.power.status.*;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.class_585;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

final class IndicatorsMenuChecks {
  private static double position;
  private static final String SONG = "music:calm1.ogg";
  static void run(Minecraft mc, String action) throws Exception {
    switch (action) {
      case "hud-state" -> log("ACTIVE TWEAKS " + ActiveTweaks.lines(StatusConfig.current()));
      case "hud" -> {
        test("active tweaks reflect live states and every inclusion is independent", () -> {
          var before = Config.current().copy(); boolean cinematic = mc.options.cinematicMode;
          try {
            mc.setScreen(null);
            var settings = before.copy(); settings.sneak.enabled = true; settings.placement.enabled = true;
            settings.placement.restrictionEnabled = true;
            settings.placement.restrictionMode = local.luke.power.fastplace.RestrictionMode.PLANE;
            settings.slabs.enabled = true; Config.preview(settings); mc.options.cinematicMode = true;
            var display = new StatusSettings();
            var lines = ActiveTweaks.lines(display);
            check(lines.containsAll(List.of("Fake sneak", "Fast placement", "Placement restriction: Plane", "Cinematic camera")), "missing live state " + lines);
            check(!lines.contains("Slab completion") && !lines.contains("Auto-walk") && !lines.contains("Free look"), "availability mistaken for active state");
            display.slabCompletion = true; check(ActiveTweaks.lines(display).contains("Slab completion"), "slab inclusion ignored");
            display.fastPlacement = false; check(!ActiveTweaks.lines(display).contains("Fast placement"), "exclusion ignored");
            check(Config.current().placement.enabled, "display switch changed gameplay");
            settings.placement.enabled = false; Config.preview(settings);
            check(!ActiveTweaks.lines(new StatusSettings()).contains("Placement restriction: Plane"), "inactive restriction shown without fast placement");
            settings.placement.enabled = true; Config.preview(settings);
            display.opacity = 0; check(ActiveTweaks.lines(display).isEmpty(), "transparent HUD queried states");
            display.opacity = 100; display.enabled = false; check(ActiveTweaks.lines(display).isEmpty(), "disabled HUD visible");
            settings.freeLookToggle = true; settings.freeLook = true; Config.preview(settings);
            local.luke.power.building.camera.FreeLook.begin(mc);
            check(ActiveTweaks.lines(new StatusSettings()).contains("Free look"), "latched look missing");
            local.luke.power.building.camera.FreeLook.reset(mc);
            Object camera = Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
            Object cameraConfig = Class.forName("local.luke.power.camera.FreecamConfig").getField("config").get(null);
            var enabled = cameraConfig.getClass().getField("enabled"); Object was = enabled.get(cameraConfig); enabled.set(cameraConfig,true);
            try {
              camera.getClass().getMethod("setActive",boolean.class).invoke(camera,true);
              field(camera,"allowPlayerMovement",true);
              check(ActiveTweaks.lines(new StatusSettings()).contains("Freecam player movement"), "freecam body movement missing");
              camera.getClass().getMethod("setActive",boolean.class).invoke(camera,false);
              check(!ActiveTweaks.lines(new StatusSettings()).contains("Freecam player movement"), "freecam flag stale");
            } finally { enabled.set(cameraConfig,was); }
            boolean blend = GL11.glIsEnabled(GL11.GL_BLEND), depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            for (int anchor = 0; anchor < 9; anchor++) {
              display = new StatusSettings(); display.position = anchor; display.opacity = 10;
              StatusConfig.preview(display); ActiveTweaks.render(mc);
              check(GL11.glIsEnabled(GL11.GL_BLEND) == blend && GL11.glIsEnabled(GL11.GL_DEPTH_TEST) == depth, "HUD changed GL state");
            }
            check(GL11.glGetError()==0,"HUD GL error");
          } finally { Config.preview(before); mc.options.cinematicMode = cinematic; StatusConfig.preview(new StatusSettings()); }
        });
        test("HUD settings preview and Cancel restore without writing or toggling gameplay", () -> {
          var session = SettingsRegistry.open(mc);
          var setting = find(session,"activeTweaks.textColor"); setting.value = new JsonPrimitive("#123456");
          find(session,"activeTweaks.opacity").value = new JsonPrimitive(43); session.preview();
          check(StatusConfig.current().argb()==0x6e123456,"HUD preview not applied");
          session.discard(); check(StatusConfig.current().textColor.equals("#FFFFFF"),"Cancel did not restore HUD");
          for (var item : session.settings()) if(item.id.startsWith("power_controls:recipes.") && !item.id.endsWith("enableRecipes"))
            check(!item.description.isBlank() && !item.label.contains("("),"recipe copy unclear " + item.id);
          check(find(session,"power_controls:recipes.obtainable.craftableGrassBlocks").description.contains("1 dirt and 1 seed"),"old tooltip still overrides recipe");
        });
      }
      case "creative" -> test("creative has eight rows, upward growth and unchanged hotbar/tab transfers", () -> {
        ((CheatWorld)mc.world.method_262()).power$cheatsEnabled(true);
        ChatChecks.submit(mc,"/gamemode creative");
        var screen = new class_585(mc.player); mc.setScreen(screen); field(screen,"creative_normalGUI",false);
        List<ItemStack> items = (List<ItemStack>)field(screen,"creative_items");
        check(items.size()>56 && items.size()<=64,"full blocks count " + items.size());
        check((int)field(screen,"creative_maxIndex")==0,"full blocks still scroll");
        ItemStack[] inventory = mc.player.inventory.main.clone();
        ItemStack cursor = mc.player.inventory.getCursorStack();
        try {
          for(int[] size : new int[][]{{320,240},{480,270},{854,480}}) {
            screen.init(mc,size[0],size[1]); screen.render(-1,-1,0);
            int x=(size[0]-176)/2,y=(size[1]-166)/2;
            for(int i : new int[]{0,55,56,items.size()-1}) {
              mc.player.inventory.setCursorStack(null);
              ((ScreenInput)screen).power$click(x+8+(i%8)*18+8,y-18+14+(i/8)*18+8,0);
              ItemStack actual=mc.player.inventory.getCursorStack(),expected=items.get(i);
              check(actual!=null && actual.itemId==expected.itemId && actual.getDamage()==expected.getDamage(),"wrong catalogue item " + i + " at " + size[0]);
            }
            mc.player.inventory.main[0]=null;
            ItemStack held=mc.player.inventory.getCursorStack().clone();
            ((ScreenInput)screen).power$click(x+16,y+150,0);
            ItemStack received=mc.player.inventory.main[0];
            check(received!=null && received.itemId==held.itemId && received.getDamage()==held.getDamage() && received.count==held.count && mc.player.inventory.getCursorStack()==null,"hotbar moved or transfer failed: slot=" + received + ", cursor=" + mc.player.inventory.getCursorStack());
            mc.player.inventory.setCursorStack(new ItemStack(267,1,37)); held=mc.player.inventory.getCursorStack();
            ((ScreenInput)screen).power$click(x+180,y+145,0);
            check((boolean)field(screen,"creative_normalGUI") && mc.player.inventory.getCursorStack()==held,"survival tab dropped cursor");
            ((ScreenInput)screen).power$click(x+180,y+120,0);
            check(!(boolean)field(screen,"creative_normalGUI") && mc.player.inventory.getCursorStack()==held,"creative tab dropped cursor");
            check(GL11.glGetError()==0,"inventory GL error");
          }
        } finally { System.arraycopy(inventory,0,mc.player.inventory.main,0,inventory.length);mc.player.inventory.setCursorStack(cursor);mc.setScreen(screen); }
      });
      case "mix", "world", "custom" -> {
        AudioSettings s=AudioConfig.copy();s.menuMusic=action.equals("mix")?AudioSettings.MenuMusic.MIX:action.equals("world")?AudioSettings.MenuMusic.WORLD:AudioSettings.MenuMusic.CUSTOM;
        s.menuControls=true;s.menuControlsMainMenu=true;s.gapMinSeconds=600;s.gapMaxSeconds=1200;s.waitBetweenTracks=true;
        AudioConfig.preview(s);mc.options.musicVolume=.1f;AudioStartChecks.run("reset");AudioController.playNow(SONG);
      }
      case "exit", "exit-paused" -> {
        position=AudioController.position();check(position>0,"stream not started");
        if(action.equals("exit-paused")) AudioController.pause();
        mc.setWorld(null);mc.setScreen(new TitleScreen());
      }
      case "playing", "restored" -> {
        AudioStartChecks.run(action.equals("restored") ? "resume-background" : "background");
        test("world/menu transition retains the actual stream and progress",()->{
          check(AudioController.currentTrackId().equals(SONG),"track lost");
          check(AudioController.position()>position,"progress restarted");
          check(AudioController.status().startsWith("Playing:"),"status desynchronised");
        });
      }
      case "preview" -> { AudioStartChecks.run("reset"); AudioController.previewSound("music:calm2.ogg"); }
      case "preview-playing" -> {
        AudioStartChecks.run("preview");
        test("preview track survives world/menu transition", () -> {
          check(AudioController.previewing("music:calm2.ogg") && AudioController.status().startsWith("Previewing:"), "preview lost");
          AudioController.previewSound("music:calm2.ogg");
        });
      }
      case "paused" -> test("world/menu transition preserves paused track",()->{
        check(!AudioController.musicPlaying() && !SoundManagerAccessor.power$system().playing("BgMusic"),"pause lost");
        check(AudioController.status().startsWith("Paused:") && AudioController.currentTrackId().equals(SONG),"paused metadata lost");
        check(Math.abs(AudioController.position()-position)<.3,"paused position changed");
        AudioController.togglePause();
      });
      case "stopped" -> test("menu-folder mode stops world soundtrack on exit",()->{
        check(!AudioController.musicPlaying() && AudioController.currentTrackId().isEmpty(),"separate menu mode kept world track");
      });
      case "menu" -> test("title controls render, follow visibility settings and open the current song",()->{
        var settings=AudioConfig.copy(); settings.menuControls=true;settings.menuControlsMainMenu=true;
        var title=new TitleScreen();mc.setScreen(title);
        for(boolean scrub : new boolean[]{false,true}) for(int[] size : new int[][]{{320,240},{480,270},{854,480}})
          for(var anchor:AudioSettings.MenuControlsPosition.values()) {
            settings.menuControlsPosition=anchor;settings.menuControlsScrub=scrub;AudioConfig.preview(settings);
            title.init(mc,size[0],size[1]);title.render(-1,-1,0);
            var panel=(PauseMenuMusic)field(title,"power$music");var bounds=(MenuMusicLayout)field(panel,"bounds");
            check(panel.visible() && bounds.y()>=0 && bounds.y()+bounds.height()<=size[1],"title controls clipped");
            if (anchor == AudioSettings.MenuControlsPosition.MENU_BOTTOM) {
              int buttonBottom = ((ScreenInput)title).power$buttons().stream().filter(b -> b.visible).mapToInt(b -> b.y + 20).max().orElse(0);
              check(bounds.y() >= buttonBottom + 8, "music panel overlaps menu buttons");
            }
            check(GL11.glGetError()==0,"title GL error");
          }
        settings.menuControlsPosition=AudioSettings.MenuControlsPosition.MENU_BOTTOM;settings.menuControlsScrub=false;
        settings.menuControlsMainMenu=false;AudioConfig.preview(settings);title.render(-1,-1,0);
        check(!((PauseMenuMusic)field(title,"power$music")).visible(),"title switch ignored");
        settings.menuControlsMainMenu=true;AudioConfig.preview(settings);mc.setScreen(title);title.render(-1,-1,0);
        var panel=(PauseMenuMusic)field(title,"power$music");var bounds=(MenuMusicLayout)field(panel,"bounds");
        ((ScreenInput)title).power$click(bounds.x()+10,bounds.y()+25,0);
        check(mc.currentScreen instanceof PowerOptionsScreen,"track link failed");
        check((boolean)field(mc.currentScreen,"libraryOpen"),"track link missed library");
      });
      default -> throw new IllegalArgumentException(action);
    }
  }
}
