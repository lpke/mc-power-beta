package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;
import local.luke.power.autowalk.*;
import local.luke.power.building.camera.FreeLook;
import local.luke.power.building.config.Config;
import local.luke.power.building.mining.*;
import local.luke.power.config.Catalog;
import local.luke.power.death.DeathItems;
import local.luke.power.input.*;
import local.luke.power.inventory.NativeInventoryPlan;
import local.luke.power.inventory.HandUse;
import local.luke.power.inventory.tools.*;
import local.luke.power.light.LightConfig;
import local.luke.power.render.*;
import local.luke.power.status.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.visual.VisualConfig;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.Display;

/** Native reset owners without inventory changes, world edits or persistent fixture settings. */
final class TweakLifecycle448Checks {
  private TweakLifecycle448Checks() {}
  /** Actual native exit. Deliberately finishes at the title screen for a fresh-world check. */
  static void boundary(Minecraft mc) throws Exception {
    check(mc.world != null && mc.player != null && !mc.world.isRemote && Display.isActive(), "focus a disposable singleplayer world");
    check(mc.currentScreen == null && !mc.paused, "close menus before the native exit check");
    check(mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "mute the disposable client");
    check(mc.player instanceof PlayerMode mode && !mode.power$creative() && !mode.power$spectator(), "use Survival without changing the world's mode");
    check(!InteractionState.carryingContainer && !NativeInventoryPlan.busy()
        && !local.luke.power.inventory.InventoryActions.busy() && !local.luke.power.mining.VeinMining.busy(),
        "finish carried containers and native inventory/mining jobs first");
    check(mc.player.container == mc.player.playerContainer && mc.player.inventory.getCursorStack() == null,
        "close native containers and empty the cursor first");
    check(((Map<?, ?>)field(Restock.class, "ready").get(null)).isEmpty()
        && field(Restock.class, "publishing").get(null) == null
        && !field(AutoToolSwitch.class, "waiting").getBoolean(null), "finish pending automation first");
    PowerConfig.flushPending();
    byte[] disk = Files.readAllBytes(PowerConfig.path());
    String preferences = preferences();
    var light = LightConfig.copy();
    var player = mc.player;
    String items = DeathItems.inventory(DeathItems.snapshot(player.inventory, player.playerContainer));
    Object camera = Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
    try {
      // The legacy overlay cleanup queues its saved switch. Keep this fixture scoped to
      // memory-only actions and restore the overlay preference after the native exit.
      var quiet = LightConfig.copy(); quiet.enabled = false; LightConfig.preview(quiet);
      InventoryAutomation.registerIndicators(mc);
      var tools = InventoryToolsConfig.copy(); tools.restock = tools.toolSwitch = true; InventoryToolsConfig.preview(tools);
      var building = Config.current().copy(); building.sneak.enabled = true; Config.preview(building);
      check(InventoryToolsConfig.current().restock && InventoryToolsConfig.current().toolSwitch
          && Config.current().sneak.enabled, "fixture did not arm temporary automation and fake sneak");
      mc.setWorld(null);
      mc.setScreen(new net.minecraft.client.gui.screen.TitleScreen());
      check(mc.world == null && mc.currentScreen instanceof net.minecraft.client.gui.screen.TitleScreen,
          "native exit did not reach the title screen");
      inactive(mc, camera);
      check(preferences.equals(preferences()), "native exit changed persistent preferences");
      check(items.equals(DeathItems.inventory(DeathItems.snapshot(player.inventory, player.playerContainer))),
          "native exit changed full native inventory data");
      PowerConfig.flushPending();
      check(Arrays.equals(disk, Files.readAllBytes(PowerConfig.path())), "native exit wrote temporary activation or changed saved preferences");
    } finally {
      LightConfig.preview(light);
    }
    log("PASS LIFECYCLE448 native world exit clears automation and fake sneak, preserving config bytes and native inventory; title screen ready");
  }
  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && mc.player != null && !mc.world.isRemote && Display.isActive(), "focus a disposable singleplayer world");
    check(mc.currentScreen == null && !mc.paused, "close menus before lifecycle checks");
    check(mc.options.soundVolume == 0 && mc.options.musicVolume == 0, "mute the disposable client");
    check(mc.player instanceof PlayerMode mode && !mode.power$creative() && !mode.power$spectator(), "use Survival without changing the world's mode");
    check(!InteractionState.carryingContainer && !NativeInventoryPlan.busy(), "finish carried containers and pending inventory actions first");
    PowerConfig.flushPending();
    byte[] disk = Files.readAllBytes(PowerConfig.path());
    var building = Config.current().copy(); var tools = InventoryToolsConfig.copy();
    var visual = VisualConfig.copy(); var light = LightConfig.copy(); var status = StatusConfig.copy();
    boolean third = mc.options.thirdPerson, cinematic = mc.options.cinematicMode;
    String items = DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer));
    var world = mc.world; var player = mc.player;
    Object camera = Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
    failures = 0;
    try {
      AutoWalk.tick(mc); AutoMine.tick(mc); HoldUse.tick(mc);
      InventoryAutomation.registerIndicators(mc);
      // Prevent an unrelated saved overlay preference from queuing a cleanup save.
      var quiet = LightConfig.copy(); quiet.enabled = false; LightConfig.preview(quiet);
      TweakLifecycle.reset();
      var options = Config.current().copy(); options.freeLookToggle = true; Config.preview(options);
      var include = StatusConfig.copy(); include.enabled = true; include.opacity = 100;
      for (var tweak : TweakIndicators.Tweak.values()) {
        Field field = StatusSettings.class.getField(statusField(tweak)); field.setBoolean(include, true);
      }
      StatusConfig.preview(include);
      for (var tweak : TweakIndicators.Tweak.values()) test("LIFECYCLE448 resets " + tweak, () -> {
        activate(mc, camera, tweak);
        check(TweakIndicators.label(tweak) != null, "fixture did not activate " + tweak);
        check(ActiveTweaks.lines(StatusConfig.current()).contains(TweakIndicators.label(tweak)), "HUD did not report " + tweak);
        TweakLifecycle.reset();
        inactive(mc, camera);
        String preferences = preferences();
        TweakLifecycle.reset();
        check(preferences.equals(preferences()), "repeated reset changed preferences");
      });
      test("LIFECYCLE448 automation drafts retain activation but disk snapshots and legacy values cannot restore it", () -> {
        var enabled = InventoryToolsConfig.copy(); enabled.restock = enabled.toolSwitch = true;
        enabled.restockThreshold = 3; enabled.toolWhitelist = java.util.List.of("257", "278");
        InventoryToolsConfig.preview(enabled);
        var draft = InventoryToolsConfig.copy(); draft.toolDurabilityReserve = 4; InventoryToolsConfig.preview(draft);
        check(InventoryToolsConfig.current().restock && InventoryToolsConfig.current().toolSwitch, "copy/preview lost transient fields");
        var saved = Catalog.JSON.toJsonTree(InventoryToolsConfig.current()).getAsJsonObject();
        check(!saved.has("restock") && !saved.has("toolSwitch"), "temporary activation reached a settings snapshot");
        var legacy = saved.deepCopy(); legacy.addProperty("restock", true); legacy.addProperty("toolSwitch", true);
        var restored = Catalog.JSON.fromJson(legacy, InventoryToolsSettings.class);
        check(!restored.restock && !restored.toolSwitch && restored.restockThreshold == 3
            && restored.toolDurabilityReserve == 4 && restored.toolWhitelist.equals(enabled.toolWhitelist), "restart changed activation or preferences");
        TweakLifecycle.reset();
        check(saved.equals(Catalog.JSON.toJsonTree(InventoryToolsConfig.current())), "cleanup changed inventory tuning or layouts");
      });
      test("LIFECYCLE448 pending native automation clears without deactivating on ordinary cancellation", () -> {
        var enabled = InventoryToolsConfig.copy(); enabled.restock = enabled.toolSwitch = true; InventoryToolsConfig.preview(enabled);
        field(AutoToolSwitch.class, "world").set(null, mc.world); field(AutoToolSwitch.class, "waiting").setBoolean(null, true);
        // A queued refill candidate owns no slots or durable journal yet.
        @SuppressWarnings("unchecked") var ready = (Map<HandUse.Hand, Restock.Action>)field(Restock.class, "ready").get(null);
        ready.put(HandUse.Hand.MAIN, null);
        InventoryAutomation.clear();
        check(ready.isEmpty() && !field(AutoToolSwitch.class, "waiting").getBoolean(null)
            && field(AutoToolSwitch.class, "world").get(null) == null, "cancellation retained pending automation");
        check(InventoryToolsConfig.current().restock && InventoryToolsConfig.current().toolSwitch, "menu/focus cancellation deactivated automation");
        InteractionState.carryingContainer = true;
        try {
          TweakLifecycle.reset();
          check(InteractionState.carryingContainer, "generic lifecycle cleanup revoked carried-container ownership");
        } finally { InteractionState.carryingContainer = false; }
      });
      test("LIFECYCLE448 cleanup preserves complete slabs, tuning, HUD inclusion and messages", () -> {
        for (boolean slabs : new boolean[]{false, true}) {
          var selected = Config.current().copy(); selected.slabs.enabled = slabs;
          selected.sneak.announceToggle = true; selected.placement.announceToggle = true;
          selected.autoWalkCancelOnLostFocus = false; selected.autoWalkHoldMillis = 725; Config.preview(selected);
          var expected = preferences(); TweakLifecycle.reset();
          check(expected.equals(preferences()), "cleanup changed persistent preferences");
          check(Config.current().slabs.enabled == slabs, "cleanup changed slab enablement");
        }
      });
      PowerConfig.flushPending();
      check(Arrays.equals(disk, Files.readAllBytes(PowerConfig.path())), "temporary actions or resets wrote configuration");
      check(items.equals(DeathItems.inventory(DeathItems.snapshot(mc.player.inventory, mc.player.playerContainer))), "lifecycle checks changed native inventory");
      check(world == mc.world && player == mc.player, "lifecycle checks replaced native owners");
      check(mc.player instanceof PlayerMode mode && !mode.power$creative() && !mode.power$spectator(), "lifecycle changed actual game mode");
    } finally {
      TweakLifecycle.reset();
      Config.preview(building); InventoryToolsConfig.preview(tools); VisualConfig.preview(visual);
      LightConfig.preview(light); StatusConfig.preview(status);
      mc.options.thirdPerson = third; mc.options.cinematicMode = cinematic;
    }
    log("LIFECYCLE448 failures=" + failures); check(failures == 0, "tweak-lifecycle-448 failures=" + failures);
  }
  private static void activate(Minecraft mc, Object camera, TweakIndicators.Tweak tweak) throws Exception {
    var building = Config.current().copy(); var tools = InventoryToolsConfig.copy();
    switch (tweak) {
      case RESTOCK -> { tools.restock = true; InventoryToolsConfig.preview(tools); }
      case TOOL_SWITCH -> { tools.toolSwitch = true; InventoryToolsConfig.preview(tools); }
      case FAKE_SNEAK -> { building.sneak.enabled = true; Config.preview(building); }
      case FAST_PLACEMENT -> { building.placement.enabled = true; Config.preview(building); }
      case PLACEMENT_RESTRICTION -> { building.placement.enabled = building.placement.restrictionEnabled = true; Config.preview(building); }
      case AUTO_WALK -> {
        var toggle = (WalkToggle)field(AutoWalk.class, "TOGGLE").get(null);
        toggle.poll(false, true, false, System.nanoTime(), 350); toggle.press(true, false, System.nanoTime(), false);
        ((LineLock)field(AutoWalk.class, "LINE_LOCK").get(null)).start(mc.player.x, mc.player.z, mc.player.yaw);
        check(AutoWalk.lineLocked(), "fixture did not activate line lock");
      }
      case AUTO_MINE -> {
        var toggle = (AutoMineToggle)field(AutoMine.class, "TOGGLE").get(null); toggle.release(); toggle.press(true, true);
      }
      case HOLD_USE -> { field(HoldUse.class, "active").setBoolean(null, true); UseInput.press(); }
      case CLICK_MINING -> ClickMining.toggle();
      case FREE_LOOK -> FreeLook.begin(mc);
      case CINEMATIC_CAMERA -> mc.options.cinematicMode = true;
      case FREECAM, FREECAM_PLAYER_MOVEMENT -> {
        camera.getClass().getMethod("setActive", boolean.class).invoke(camera, true);
        if (tweak == TweakIndicators.Tweak.FREECAM_PLAYER_MOVEMENT) camera.getClass().getField("allowPlayerMovement").setBoolean(camera, true);
      }
      case HIDDEN_ENTITIES -> { var v = VisualConfig.copy(); v.hideEntities = true; VisualConfig.preview(v); }
      case FULL_BRIGHT -> {
        RenderControls.tick(mc); ((FullBrightState)field(RenderControls.class, "brightness").get(null)).toggle(); RenderControls.tick(mc);
      }
    }
  }
  private static void inactive(Minecraft mc, Object camera) throws Exception {
    for (var tweak : TweakIndicators.Tweak.values()) check(TweakIndicators.label(tweak) == null, "reset retained " + tweak);
    check(!InventoryToolsConfig.current().restock && !InventoryToolsConfig.current().toolSwitch, "automation remained active");
    check(!AutoWalk.lineLocked() && !AttackInput.held() && !UseInput.held() && !UseInput.consumePress(), "native held action remained active");
    check(!camera.getClass().getField("allowPlayerMovement").getBoolean(camera), "freecam retained player movement");
    check(!ClickMining.inverted() && !mc.options.cinematicMode, "memory-only inversion/cinematic remained active");
  }
  private static String preferences() {
    return Catalog.JSON.toJson(Config.current()) + Catalog.JSON.toJson(InventoryToolsConfig.current())
        + Catalog.JSON.toJson(VisualConfig.current()) + Catalog.JSON.toJson(StatusConfig.current());
  }
  private static String statusField(TweakIndicators.Tweak tweak) {
    return switch (tweak) {
      case RESTOCK -> "restock"; case TOOL_SWITCH -> "toolSwitch"; case FAKE_SNEAK -> "fakeSneak";
      case FAST_PLACEMENT -> "fastPlacement"; case PLACEMENT_RESTRICTION -> "placementRestriction";
      case AUTO_WALK -> "autoWalk"; case AUTO_MINE -> "autoMine"; case HOLD_USE -> "holdUse";
      case CLICK_MINING -> "clickMining"; case FREE_LOOK -> "freeLook"; case CINEMATIC_CAMERA -> "cinematicCamera";
      case FREECAM -> "freecam"; case FREECAM_PLAYER_MOVEMENT -> "freecamPlayerMovement";
      case HIDDEN_ENTITIES -> "hiddenEntities"; case FULL_BRIGHT -> "fullBright";
    };
  }
  private static Field field(Class<?> type, String name) throws Exception {
    Field field = type.getDeclaredField(name); field.setAccessible(true); return field;
  }
}
