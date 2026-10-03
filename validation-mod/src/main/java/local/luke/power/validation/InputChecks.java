package local.luke.power.validation;

import com.google.gson.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.input.*;
import local.luke.power.ui.PowerOptionsScreen;
import net.minecraft.client.Minecraft;

/** Disposable-instance fixture driven by physical XTest input from outside the game. */
public final class InputChecks {
  private static final Map<String, JsonElement> saved = new LinkedHashMap<>();
  private static void set(ConfigSession session, String id, JsonElement value) {
    Setting setting = Validation.find(session, id);
    saved.putIfAbsent(id, setting.value.deepCopy());
    setting.value = value;
  }
  private static void bind(ConfigSession session, String id, int code, int mask) {
    set(session, "keys." + id, new JsonPrimitive(new Chord(code, mask).encoded()));
  }
  public static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("setup")) {
      var session = SettingsRegistry.open(mc);
      bind(session, "Fast place (toggle)", 35, 0);
      bind(session, "Fake sneak (toggle)", 35, Chord.CTRL);
      bind(session, "key.inventory", 18, Chord.SHIFT);
      bind(session, "key.forward", 17, Chord.ALT);
      set(session, "tweaks.placement.enabled", new JsonPrimitive(false));
      set(session, "tweaks.sneak.enabled", new JsonPrimitive(false));
      session.save(Path.of(".").toAbsolutePath());
      mc.setScreen(null);
    } else if (action.equals("mouse")) {
      var session = SettingsRegistry.open(mc);
      bind(session, "key.inventory", -99, Chord.CTRL);
      bind(session, "Fake sneak (toggle)", -98, Chord.ALT);
      bind(session, "key.power_controls.hotbar_2", -100, Chord.SHIFT);
      set(session, "tweaks.sneak.enabled", new JsonPrimitive(false));
      session.save(Path.of(".").toAbsolutePath()); mc.setScreen(null);
    } else if (action.equals("restore")) {
      var session = SettingsRegistry.open(mc);
      for (var entry : saved.entrySet()) Validation.find(session, entry.getKey()).value = entry.getValue();
      session.save(Path.of(".").toAbsolutePath());
      saved.clear(); mc.setScreen(null);
    } else if (action.equals("capture")) {
      PowerOptionsScreen screen = new PowerOptionsScreen(null);
      mc.setScreen(screen);
      Field page = PowerOptionsScreen.class.getDeclaredField("page"); page.setAccessible(true); page.set(screen, "Controls");
      Field capture = PowerOptionsScreen.class.getDeclaredField("capture"); capture.setAccessible(true);
      capture.set(screen, Validation.find(screen.session(), "keys.key.inventory"));
    } else if (action.equals("close")) mc.setScreen(null);
    else if (action.equals("state")) {
      JsonObject json = new JsonObject();
      json.addProperty("held", Bindings.heldModifiers());
      json.addProperty("forward", Bindings.down(mc.options.forwardKey));
      json.addProperty("forwardInput", mc.player == null ? 0 : mc.player.field_161.field_2533);
      json.addProperty("screen", mc.currentScreen == null ? "none" : mc.currentScreen.getClass().getName());
      json.addProperty("slot", mc.player == null ? -1 : mc.player.inventory.selectedSlot);
      var session = SettingsRegistry.open(mc);
      for (String id : List.of("tweaks.placement.enabled", "tweaks.sneak.enabled", "keys.key.inventory", "keys.key.forward")) json.add(id, Validation.find(session, id).value);
      if (mc.currentScreen instanceof PowerOptionsScreen screen) {
        json.add("draftInventory", Validation.find(screen.session(), "keys.key.inventory").value);
        Field capture = PowerOptionsScreen.class.getDeclaredField("capture"); capture.setAccessible(true);
        json.addProperty("capturing", capture.get(screen) != null);
      }
      Files.writeString(Path.of("power-beta-input-state.json"), json.toString());
    }
  }
}
