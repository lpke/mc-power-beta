package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import local.luke.power.config.Catalog;
import local.luke.power.config.SettingsRegistry;
import local.luke.power.input.Bindings;
import local.luke.power.input.Chord;
import local.luke.power.visual.VisualConfig;
import net.minecraft.client.Minecraft;

public final class Release174Checks {
  private static boolean capture;
  private static int shownFrames, hiddenFrames;

  public static void frame(Minecraft mc) {
    if (capture) { if (mc.options.debugHud) shownFrames++; else hiddenFrames++; }
  }

  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("fog")) { UnderwaterFogChecks.run(mc,"fog"); return; }
    if (action.equals("fog-save") || action.equals("fog-saved")) {
      var session=SettingsRegistry.open(mc);var setting=find(session,"visual.underwaterFogDensity");
      if (action.equals("fog-saved")) {
        check(setting.value.getAsInt()==25 && VisualConfig.current().underwaterFogDensity==25,"fog did not survive restart");
        setting.parse("0");session.preview(true);session.discard();
        check(VisualConfig.current().underwaterFogDensity==25,"Cancel lost saved fog after restart");
      }
      setting.parse(action.equals("fog-save")?"25":"100");session.save(Path.of(".").toAbsolutePath());
      log("FOG PERSISTENCE PASS "+action);return;
    }
    if (action.equals("keys-state")) {
      var state=new java.util.LinkedHashMap<String,Object>();
      state.put("debug",mc.options.debugHud);state.put("shownFrames",shownFrames);state.put("hiddenFrames",hiddenFrames);
      state.put("pending",Class.forName("local.luke.power.creative.DebugKey").getMethod("deferred").invoke(null));
      state.put("screen",mc.currentScreen==null?"":mc.currentScreen.getClass().getName());
      state.put("x",mc.player.x);state.put("z",mc.player.z);
      Files.writeString(Path.of("power-beta-174-keys.json"),Catalog.JSON.toJson(state));return;
    }
    if (action.equals("keys-close")) { mc.setScreen(null); return; }
    if (!action.startsWith("keys ")) throw new IllegalArgumentException(action);
    String kind=action.substring(5);
    capture=false;mc.setScreen(null);mc.paused=false;DeathChecks.alive(mc);FreecamPreviewChecks.active(false);
    (AccessRules.of(mc.world.method_262())).allAllowed(!kind.equals("no-cheats"));
    var session=SettingsRegistry.open(mc);
    find(session,"power_controls:general.pauseOnLostFocus").parse("false");session.preview(true);
    for (var key:mc.options.allKeys) switch(key.translationKey) {
      case "key.power_controls.debug_hud" -> key.code=kind.equals("debug-remap")?64:61;
      case "power_creative.modifier" -> key.code=kind.equals("modifier-remap")?64:61;
      case "power_creative.picker" -> key.code=kind.equals("disabled")?0:kind.equals("cycle-remap")?64:62;
      default -> {}
    }
    Bindings.configure(kind.equals("chord")?Map.of("key.power_controls.debug_hud",Chord.CTRL):Map.of());
    Bindings.register(mc.options.allKeys);
    Class.forName("local.luke.power.creative.DebugKey").getMethod("cancel").invoke(null);
    mc.options.debugHud=kind.equals("shown");mc.options.hideHud=false;
    shownFrames=hiddenFrames=0;capture=true;
    log("DEBUG INPUT READY "+kind);
  }
}
