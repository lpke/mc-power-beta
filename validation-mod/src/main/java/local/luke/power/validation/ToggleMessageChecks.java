package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.nio.file.*;
import java.util.*;
import local.luke.power.building.config.Config;
import local.luke.power.config.*;
import local.luke.power.input.*;
import local.luke.power.input.TweakIndicators.Tweak;
import local.luke.power.status.*;
import local.luke.power.storage.PowerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.TitleScreen;

final class ToggleMessageChecks {
  private static final Map<Tweak, String> IDS = Map.ofEntries(
      Map.entry(Tweak.FAKE_SNEAK, "tweaks.sneak.announceToggle"),
      Map.entry(Tweak.SLAB_COMPLETION, "tweaks.slabs.announceToggle"),
      Map.entry(Tweak.FAST_PLACEMENT, "tweaks.placement.announceToggle"),
      Map.entry(Tweak.PLACEMENT_RESTRICTION, "tweaks.placement.announceRestrictionToggle"),
      Map.entry(Tweak.AUTO_WALK, "tweaks.autoWalkAnnounceToggle"),
      Map.entry(Tweak.AUTO_MINE, "tweaks.autoMineAnnounceToggle"),
      Map.entry(Tweak.FREE_LOOK, "tweaks.freeLookAnnounceToggle"),
      Map.entry(Tweak.CINEMATIC_CAMERA, "activeTweaks.cinematicCameraMessages"),
      Map.entry(Tweak.FREECAM_PLAYER_MOVEMENT, "power_camera:config.playerMovementMessages"),
      Map.entry(Tweak.HIDDEN_ENTITIES, "visual.entityRenderingMessages"),
      Map.entry(Tweak.FULL_BRIGHT, "visual.fullBrightMessages"));

  static void run(Minecraft mc, String action) throws Exception {
    var session = SettingsRegistry.open(mc);
    if (action.equals("save") || action.equals("reload")) {
      for (String id : IDS.values()) {
        var setting = find(session, id);
        if (action.equals("reload")) check(setting.value.getAsBoolean(), "restart lost " + id);
        setting.parse(Boolean.toString(action.equals("save")));
      }
      session.save(Path.of(".").toAbsolutePath());
      log("TOGGLE MESSAGES " + action + " PASS: all preferences"); return;
    }
    failures = 0;
    Map<String, com.google.gson.JsonElement> originalMessages = new HashMap<>();
    for (String id : IDS.values()) originalMessages.put(id, find(session, id).value.deepCopy());
    var document = PowerConfig.document();
    var visual = local.luke.power.visual.VisualConfig.copy();
    var properties = (local.luke.power.permissions.CheatWorld)mc.world.method_262();
    boolean cheats = properties.power$cheatsEnabled();
    String mode = mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString();
    var saved = Config.current().copy();
    var hud = StatusConfig.copy();
    boolean cinematic = mc.options.cinematicMode;
    var screen = mc.currentScreen;
    Object camera = Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
    Object cameraConfig = Class.forName("local.luke.power.camera.FreecamConfig").getField("config").get(null);
    var enabled = cameraConfig.getClass().getField("enabled");
    Object cameraEnabled = enabled.get(cameraConfig);
    Object cameraMessages = cameraConfig.getClass().getField("playerMovementMessages").get(cameraConfig);
    try {
      mc.setScreen(null);
      properties.power$cheatsEnabled(true); CreativeVehicleChecks.mode(mc,"CREATIVE");
      local.luke.power.autowalk.AutoWalk.tick(mc);
      test("all message switches default off and preview/Cancel preserve the file", () -> {
        check(IDS.size() == Tweak.values().length, "missing toggle");
        byte[] before = Files.readAllBytes(PowerConfig.path());
        for (var entry : IDS.entrySet()) {
          var setting = find(session, entry.getValue());
          check(!setting.defaultValue.getAsBoolean(), "default is on: " + setting.id);
          check(!setting.description.isBlank() && !SettingScope.perWorld(setting), "missing help/scope");
          check(!setting.group.equals("Active tweaks"), "message control mixed with HUD inclusion");
          setting.parse("true");
        }
        session.preview(true);
        for (Tweak tweak : IDS.keySet()) check(TweakIndicators.announces(tweak), "preference not connected: " + tweak);
        check(Arrays.equals(before, Files.readAllBytes(PowerConfig.path())), "preview saved");
        session.discard();
        for (Tweak tweak : IDS.keySet()) check(TweakIndicators.announces(tweak) == originalMessages.get(IDS.get(tweak)).getAsBoolean(), "Cancel did not restore: " + tweak);
      });
      for (String id : IDS.values()) find(session, id).parse("true");
      session.preview(true);
      for (Tweak tweak : Tweak.values()) test("live " + tweak + " sends one on/off message independently of the HUD", () -> {
        var display = StatusConfig.copy(); display.enabled = false; StatusConfig.preview(display);
        setState(mc, tweak, false, camera, enabled, cameraConfig);
        var messages = new TweakToggleMessages(); messages.update(false);
        setState(mc, tweak, true, camera, enabled, cameraConfig);
        check(messages.update(true).equals(List.of(tweak.label + ": \u00a7aON\u00a7r")), "on message missing or duplicated");
        check(messages.update(true).isEmpty(), "held state repeats");
        check(ActiveTweaks.lines(StatusConfig.current()).isEmpty(), "HUD was enabled by messages");
        setState(mc, tweak, false, camera, enabled, cameraConfig);
        check(messages.update(true).equals(List.of(tweak.label + ": \u00a7cOFF\u00a7r")), "off message missing or duplicated");
      });
      test("tick emits chat once and menu previews are silent", () -> {
        check(mc.world != null && mc.player != null && mc.player.health > 0 && !mc.paused
            && org.lwjgl.opengl.Display.isActive(), "message check requires focused, unpaused gameplay");
        mc.options.cinematicMode = false;
        TweakMessages.tick(mc); TweakMessages.tick(mc);
        var chat = ChatChecks.record(() -> {
          mc.options.cinematicMode = true;
          TweakMessages.tick(mc); TweakMessages.tick(mc);
        });
        check(chat.equals(List.of("Cinematic camera: \u00a7aON\u00a7r")), "chat pipeline: " + chat);
        chat = ChatChecks.record(() -> {
          mc.setScreen(new TitleScreen()); TweakMessages.tick(mc);
          mc.options.cinematicMode = false; TweakMessages.tick(mc);
          mc.setScreen(null); TweakMessages.tick(mc); TweakMessages.tick(mc);
        });
        check(chat.isEmpty(), "menu preview leaked chat " + chat);
      });
      test("Apply persists all switches and reopening retains them", () -> {
        session.save(Path.of(".").toAbsolutePath());
        var reopened = SettingsRegistry.open(mc);
        for (String id : IDS.values()) check(find(reopened, id).value.getAsBoolean(), "Apply lost " + id);
      });
    } finally {
      local.luke.power.autowalk.AutoWalk.stop();
      local.luke.power.building.camera.FreeLook.reset(mc);
      camera.getClass().getMethod("setActive", boolean.class).invoke(camera, false);
      var cleanup = SettingsRegistry.open(mc);
      originalMessages.forEach((id, value) -> find(cleanup, id).value = value);
      cleanup.preview(true);
      enabled.set(cameraConfig, cameraEnabled);
      cameraConfig.getClass().getField("playerMovementMessages").set(cameraConfig, cameraMessages);
      Config.preview(saved); StatusConfig.preview(hud); mc.options.cinematicMode = cinematic;
      local.luke.power.visual.VisualConfig.preview(visual);
      CreativeVehicleChecks.mode(mc,mode); properties.power$cheatsEnabled(cheats);
      local.luke.power.render.RenderControls.tick(mc);
      PowerConfig.write(document); mc.setScreen(screen);
    }
    log("TOGGLE MESSAGE FAILURES " + failures);
  }

  private static void setState(Minecraft mc, Tweak tweak, boolean active, Object camera,
      java.lang.reflect.Field cameraEnabled, Object cameraConfig) throws Exception {
    var s = Config.current().copy();
    switch (tweak) {
      case FAKE_SNEAK -> s.sneak.enabled = active;
      case FAST_PLACEMENT -> { s.placement.enabled = active; s.placement.restrictionEnabled = false; }
      case PLACEMENT_RESTRICTION -> { s.placement.enabled = true; s.placement.restrictionEnabled = active; }
      case SLAB_COMPLETION -> s.slabs.enabled = active;
      case FREE_LOOK -> {
        s.freeLook = true; s.freeLookToggle = true;
        if (active) local.luke.power.building.camera.FreeLook.begin(mc);
        else local.luke.power.building.camera.FreeLook.reset(mc);
      }
      case AUTO_WALK -> {
        s.autoWalk = true; Config.preview(s);
        var field = local.luke.power.autowalk.AutoWalk.class.getDeclaredField("TOGGLE"); field.setAccessible(true);
        var toggle = (local.luke.power.autowalk.WalkToggle) field.get(null);
        toggle.stop(); toggle.update(false, true);
        if (active) toggle.update(true, true);
      }
      case AUTO_MINE -> {
        s.autoMine = true; Config.preview(s);
        local.luke.power.building.mining.AutoMine.tick(mc);
        var field = local.luke.power.building.mining.AutoMine.class.getDeclaredField("TOGGLE"); field.setAccessible(true);
        var toggle = (local.luke.power.building.mining.AutoMineToggle) field.get(null);
        toggle.stop(); toggle.update(false, true);
        if (active) toggle.update(true, true);
      }
      case HIDDEN_ENTITIES -> {
        var visual = local.luke.power.visual.VisualConfig.copy(); visual.hideEntities = active;
        local.luke.power.visual.VisualConfig.preview(visual);
      }
      case FULL_BRIGHT -> {
        var field = local.luke.power.render.RenderControls.class.getDeclaredField("brightness");field.setAccessible(true);
        var state = (local.luke.power.render.FullBrightState)field.get(null);
        state.update(mc.world,false,false,false);state.update(mc.world,true,false,false);
        if(active)state.toggle();local.luke.power.render.RenderControls.tick(mc);
      }
      case CINEMATIC_CAMERA -> mc.options.cinematicMode = active;
      case FREECAM_PLAYER_MOVEMENT -> {
        cameraEnabled.set(cameraConfig, true);
        camera.getClass().getMethod("setActive", boolean.class).invoke(camera, true);
        camera.getClass().getField("allowPlayerMovement").setBoolean(camera, active);
      }
    }
    Config.preview(s);
  }
}
