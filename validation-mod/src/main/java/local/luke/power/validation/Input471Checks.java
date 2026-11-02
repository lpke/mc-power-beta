package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.controls.tweaks.morekeybinds.KeyBindingListener;
import local.luke.power.debug.*;
import local.luke.power.input.*;
import local.luke.power.storage.PowerConfig;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

/** Observes real X keyboard input and restores every temporary preference on finish. */
final class Input471Checks {
  private static final Path REPORT = Path.of("power-beta-input-471.json");
  private static final Map<Integer,Integer> events = new TreeMap<>();
  private static Map<String,Integer> savedModifiers;
  private static JsonObject savedDocument;
  private static int savedHud, savedGraph, savedModifier;
  private static boolean savedDebug, savedOverlay, savedGraphVisible;
  private Input471Checks() {}

  static void run(Minecraft mc, String action) throws Exception {
    check(mc.world != null && mc.player != null && !mc.world.isRemote, "use a disposable singleplayer world");
    if (action.equals("setup")) {
      check(savedModifiers == null && Display.isActive(), "finish earlier input check and focus the disposable client");
      savedModifiers = Bindings.modifiers();
      savedDocument = PowerConfig.document();
      savedHud = KeyBindingListener.debugHud.code;
      savedGraph = GameplayKeys.GRAPH.code;
      savedModifier = DebugKeys.MODIFIER.code;
      savedDebug = DebugConfig.enabled();
      savedOverlay = mc.options.debugHud;
      savedGraphVisible = GameplayKeys.graph();
      Bindings.onKeyPress("input-471", key -> { if (savedModifiers != null) events.merge(key, 1, Integer::sum); });
      DebugKeys.MODIFIER.code = Keyboard.KEY_F3;
      DebugConfig.preview(false);
      bind(mc, Keyboard.KEY_F3, 0);
      mc.setScreen(null);
      log("INPUT471 ready for physical Ctrl+S and remapped debug keys");
      return;
    }
    check(savedModifiers != null, "run input-471 setup first");
    if (action.equals("finish")) {
      try {
        KeyBindingListener.debugHud.code = savedHud;
        GameplayKeys.GRAPH.code = savedGraph;
        DebugKeys.MODIFIER.code = savedModifier;
        DebugConfig.preview(savedDebug);
        Bindings.configure(savedModifiers);
        mc.options.debugHud = savedOverlay;
        GameplayKeys.debugText();
        if (savedGraphVisible) {
          var field = GameplayKeys.class.getDeclaredField("graph");
          field.setAccessible(true); field.setBoolean(null, true);
        }
        DebugInput.clear();
        Bindings.register(mc.options.allKeys);
        check(savedDocument.equals(PowerConfig.document()), "input fixture changed saved preferences");
      } finally { savedModifiers = null; events.clear(); }
      return;
    }
    if (action.startsWith("bind ")) {
      String[] parts = action.substring(5).split(" ");
      bind(mc, Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    } else if (action.equals("reset")) {
      mc.options.debugHud = false; GameplayKeys.debugText(); DebugInput.clear(); events.clear();
    } else if (action.startsWith("debug ")) {
      DebugConfig.preview(Boolean.parseBoolean(action.substring(6))); DebugInput.clear();
    } else if (action.equals("menu")) mc.setScreen(new net.minecraft.class_525());
    else if (action.equals("close")) mc.setScreen(null);
    else check(action.equals("state"), "unknown input-471 action");
    JsonObject state = new JsonObject();
    state.addProperty("overlay", mc.options.debugHud);
    state.addProperty("graph", GameplayKeys.graph());
    state.addProperty("focused", Display.isActive());
    state.addProperty("screen", mc.currentScreen == null ? "none" : mc.currentScreen.getClass().getSimpleName());
    state.addProperty("modifiers", Bindings.heldModifiers());
    state.addProperty("hudDown", Bindings.down(KeyBindingListener.debugHud));
    state.addProperty("hudEventCode", Bindings.eventCode(KeyBindingListener.debugHud));
    JsonObject physical = new JsonObject();
    for (int key : new int[]{17,29,31,42,43,54,61,100,101,102,103,104,105,113,114,115,116,117,118,157,184})
      physical.addProperty(Integer.toString(key), Bindings.physical(key));
    state.add("physical", physical);
    JsonObject received = new JsonObject();
    events.forEach((key,count) -> received.addProperty(Integer.toString(key), count));
    state.add("events", received);
    Files.writeString(REPORT, new Gson().toJson(state));
  }

  private static void bind(Minecraft mc, int key, int mask) {
    KeyBindingListener.debugHud.code = key;
    GameplayKeys.GRAPH.code = key;
    var modifiers = new HashMap<>(savedModifiers);
    modifiers.put(KeyBindingListener.debugHud.translationKey, mask);
    modifiers.put(GameplayKeys.GRAPH.translationKey, mask | Chord.CTRL);
    Bindings.configure(modifiers);
    DebugInput.clear();
    Bindings.register(mc.options.allKeys);
    mc.options.debugHud = false; GameplayKeys.debugText(); events.clear();
  }
}
