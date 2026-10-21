package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.Catalog;
import local.luke.power.death.DeathItems;
import local.luke.power.input.*;
import local.luke.power.inventory.*;
import local.luke.power.inventory.tools.*;
import local.luke.power.offhand.Offhand;
import local.luke.power.storage.PowerConfig;
import local.luke.power.world.WorldCompatibility;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.*;
import org.lwjgl.opengl.Display;

/** XTest drives every press. This fixture only prepares state, observes and checks outcomes. */
public final class Input400Checks {
  static final Set<String> CASES = Set.of("game-save", "game-load", "inventory-save", "inventory-load", "game-plain",
      "row-save", "row-load", "scroll-save", "offhand-game", "offhand-inventory", "offhand-text",
      "mouse-slot", "mouse-header", "mouse-outside");
  private static Input400State active;
  private Input400Checks() {}

  public static void run(Minecraft mc, String command) throws Exception {
    String[] parts = command.trim().split("\\s+"); String phase = parts[0];
    if (phase.equals("prepare")) {
      check(active == null, "stop the previous input-400 fixture before preparing another");
      check(parts.length == 2 && CASES.contains(parts[1]), "unknown input-400 case: " + command);
      check(mc.world != null && mc.player != null && !mc.world.isRemote && WorldCompatibility.ready(mc.world)
          && mc.currentScreen == null && mc.player.container == mc.player.playerContainer && mc.field_2807 == mc.player
          && !DetachedCamera.isActive() && !InteractionState.carryingContainer && mc.player.health > 0 && !mc.player.dead,
          "close menus and use a living disposable singleplayer camera");
      check(Display.isActive() && mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "focus the muted disposable clone");
      check(!InventoryActions.busy() && !NativeInventoryPlan.busy() && PowerConfig.problem().isEmpty(), "finish inventory actions and repair settings first");
      released();
      var state = new Input400State(mc, parts[1]); active = state;
      try { state.prepare(); state.writeState(); log("INPUT400 READY " + state.scenario + " report=" + state.report); }
      catch (Exception | Error failure) { abort(failure); throw failure; }
      return;
    }
    if (phase.equals("stop")) { stop(); return; }
    check(active != null && active.mc == mc && active.owner == mc.player && active.world == mc.world, "prepare an input-400 fixture in this world first");
    try {
      switch (phase) {
        case "state" -> active.writeState();
        case "check-held", "check-repeat", "check-partial", "check" -> verify(phase);
        default -> throw new IllegalArgumentException("input-400 prepare CASE|state|check-held|check-repeat|check-partial|check|stop");
      }
      log("INPUT400 " + phase.toUpperCase(Locale.ROOT) + " " + active.scenario + " PASS");
    } catch (Exception | Error failure) { abort(failure); throw failure; }
  }

  private static void verify(String phase) throws Exception {
    var s = active; boolean complete = phase.equals("check"), held = phase.equals("check-held") || phase.equals("check-repeat");
    check(Display.isActive(), "physical input lost client focus");
    if (complete) released();
    if (s.saved() || s.row()) {
      boolean number = Bindings.physical(s.number), activator = Bindings.physical(s.activator());
      if (held) check(number && activator, "hold both the activator and number before " + phase);
      if (phase.equals("check-partial")) check(number != activator, "release exactly one part of the physical chord first");
    }
    if (!s.scenario.equals("scroll-save") || complete) s.nativeState.finish();
    String[] wanted = s.expected.clone();
    if (s.scenario.equals("scroll-save") && !complete) wanted = s.beforeItems.clone();
    check(Arrays.equals(wanted, s.items()), "physical inventory mismatch for " + s.scenario + " at " + phase);
    check(s.mc.player.inventory.selectedSlot == (s.scenario.equals("game-plain") || s.scenario.equals("scroll-save") ? s.number - Keyboard.KEY_1 : 7),
        "number selection leaked or changed selected slot in " + s.scenario + " at " + phase);
    check(s.saves == (s.scenario.endsWith("save") && !s.row() ? 1 : 0) && s.loads == (s.scenario.endsWith("load") && !s.row() ? 1 : 0),
        "physical action ran the wrong number of times: saves=" + s.saves + " loads=" + s.loads + " " + s.events);
    String[][] expectedBars = s.seedBars();
    if (s.saved() && s.scenario.endsWith("save")) expectedBars[s.number - Keyboard.KEY_1] = Arrays.copyOf(s.beforeItems, 9);
    for (int row = 0; row < 9; row++) check(Arrays.equals(expectedBars[row], SavedCreativeHotbars.snapshot(row)), "physical action changed the wrong saved row " + row);
    if (s.saved()) {
      if (s.scenario.startsWith("game-")) check(s.events.values().stream().anyMatch(e -> e.numberDown && !e.repeat && e.reads >= 2 && e.controls && e.minecraft),
          "gameplay number event did not traverse both utility controls and Minecraft readers");
      else check(s.events.values().stream().anyMatch(e -> e.numberDown && !e.repeat && e.gui), "number event did not reach the native container overlay");
      if (phase.equals("check-repeat")) check(s.events.values().stream().anyMatch(e -> e.numberDown && e.repeat), "no native held-number repeat was delivered");
      check(s.actionRows.equals(List.of(s.number - Keyboard.KEY_1)), "physical action targeted the wrong preset or ran twice");
    }
    if (complete) {
      PowerConfig.flushPending();
      var persisted = SavedCreativeHotbarsData.read(PowerConfig.section("savedCreativeHotbars"));
      for (int row = 0; row < 9; row++) check(Arrays.equals(expectedBars[row], persisted.bar(row)), "physical saved rows differ after durable flush " + row);
      check(!NativeInventoryPlan.busy(), "physical action left a pending native plan");
    }
    s.writeState();
    Files.writeString(s.report.resolve(phase + ".json"), Catalog.JSON.toJson(s.json()) + "\n");
  }

  /** Called only by the validation mixin at real production handler entry points. */
  public static void observeKey(int key) {
    var s = active; if (s == null || key != s.number || s.events.size() >= 128) return;
    long time = Keyboard.getEventNanoseconds();
    var event = s.events.computeIfAbsent(time, ignored -> new Input400State.Event()); event.reads++;
    event.numberDown = Keyboard.getEventKeyState(); event.repeat = Keyboard.isRepeatEvent();
    for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
      if (frame.getClassName().equals("local.luke.power.controls.tweaks.morekeybinds.KeyPressedListener")) event.controls = true;
      if (frame.getClassName().equals(Minecraft.class.getName()) && frame.getMethodName().contains("power$hotbarNumber")) event.minecraft = true;
      if (frame.getClassName().equals("local.luke.power.inventory.tools.InventoryToolsOverlay")) event.gui = true;
    }
  }
  public static void observeAction(boolean load, int row) {
    var s = active; if (s == null) return;
    if (load) s.loads++; else s.saves++;
    if (s.actionRows.size() < 32) s.actionRows.add(row);
  }
  private static void released() {
    for (int key : new int[]{Keyboard.KEY_C, Keyboard.KEY_X, Keyboard.KEY_V, Keyboard.KEY_1, Keyboard.KEY_4,
        Keyboard.KEY_LCONTROL, Keyboard.KEY_RCONTROL, Keyboard.KEY_LSHIFT, Keyboard.KEY_RSHIFT, Keyboard.KEY_LMENU, Keyboard.KEY_RMENU})
      check(!Bindings.physical(key), "release fixture keys and modifiers before prepare/check/stop, still down=" + key);
    check(!Mouse.isButtonDown(0) && !Mouse.isButtonDown(1), "release mouse buttons first");
  }
  private static void abort(Throwable failure) {
    if (active == null) return;
    try { Files.writeString(active.report.resolve("failure.txt"), failure + "\n" + Catalog.JSON.toJson(active.json()) + "\n"); }
    catch (Exception evidence) { failure.addSuppressed(evidence); }
    try { stop(); } catch (Exception cleanup) { failure.addSuppressed(cleanup); }
  }
  private static void stop() throws Exception {
    if (active == null) { log("INPUT400 STOP idle"); return; }
    var state = active; active = null;
    try { state.close(); Files.writeString(state.report.resolve("restored.json"), "{\"restored\":true}\n"); }
    finally { log("INPUT400 STOP " + state.scenario + " report=" + state.report); }
  }
}
