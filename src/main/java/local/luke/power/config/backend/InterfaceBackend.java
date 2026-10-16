package local.luke.power.config.backend;

import com.google.gson.*;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.MenuPreferences;

public final class InterfaceBackend implements Backend {
  public static void register(ConfigSession session) {
    session.add(
        new InterfaceBackend(),
        List.of(
            new Setting(
                "interface.pauseToOptions",
                "interface",
                "General",
                "Interface",
                "Pause opens options",
                "Open Options directly when pausing. Menu returns to the full pause menu; Done"
                    + " returns to the game.",
                Setting.Kind.BOOLEAN,
                new JsonPrimitive(MenuPreferences.pauseToOptions()),
                new JsonPrimitive(false),
                0,
                1,
                1,
                List.of(),
                false),
            new Setting("interface.blinkingUiCursor", "interface", "Interface", "Menus", "Blinking UI cursor",
                "Blink the cursor in text inputs. Off keeps the beam visible while editing. Chat has its own setting.", Setting.Kind.BOOLEAN,
                new JsonPrimitive(MenuPreferences.blinkingUiCursor()), new JsonPrimitive(true), 0, 1, 1, List.of(), false),
            new Setting("interface.blinkingChatCursor", "interface", "Interface", "Chat", "Blinking chat cursor",
                "Blink the chat cursor. Off keeps the beam visible while editing.", Setting.Kind.BOOLEAN,
                new JsonPrimitive(MenuPreferences.blinkingChatCursor()), new JsonPrimitive(true), 0, 1, 1, List.of(), false),
            new Setting("interface.useFeetYCoordinates", "interface", "Interface", "Debug (F3) info", "Use feet Y coordinates",
                "Use feet height in F3 and custom HUD coordinates, as in modern Minecraft. Off uses Beta's eye height. Freecam and spectator always report the base of the head.", Setting.Kind.BOOLEAN,
                new JsonPrimitive(MenuPreferences.useFeetYCoordinates()), new JsonPrimitive(true), 0, 1, 1, List.of(), false),
            new Setting("interface.debugTimeOfDay", "interface", "Interface", "Debug (F3) info", "Time of day",
                "Show the world's daylight time in F3, using a 12-hour clock.", Setting.Kind.BOOLEAN,
                new JsonPrimitive(MenuPreferences.debugTimeOfDay()), new JsonPrimitive(false), 0, 1, 1, List.of(), false),
            new Setting("interface.debugTextColor", "interface", "Interface", "Debug (F3) info", "Text colour",
                "Colour of all F3 debug text.", Setting.Kind.TEXT,
                new JsonPrimitive(MenuPreferences.debugTextColor()), new JsonPrimitive("#FFFFFF"), 0, 0, 1, List.of(), false)));
  }

  public String id() {
    return "interface";
  }

  public List<Path> files() {
    return List.of(PowerConfig.path());
  }

  public void validate(Map<String, JsonElement> values) {
    for (var e : values.entrySet()) {
      if (e.getKey().equals("interface.debugTextColor")) {
        local.luke.power.light.LightSettings.rgb(e.getValue().getAsString());
      } else if ((!e.getKey().equals("interface.pauseToOptions") && !e.getKey().equals("interface.blinkingChatCursor") && !e.getKey().equals("interface.blinkingUiCursor") && !e.getKey().equals("interface.debugTimeOfDay") && !e.getKey().equals("interface.useFeetYCoordinates"))
          || !e.getValue().isJsonPrimitive() || !e.getValue().getAsJsonPrimitive().isBoolean())
        throw new IllegalArgumentException("Invalid interface preference");
    }
  }

  public boolean previews(Setting setting) {
    return true;
  }

  public void preview(Map<String, JsonElement> values) {
    validate(values);
    if (values.containsKey("interface.pauseToOptions"))
      MenuPreferences.previewPause(values.get("interface.pauseToOptions").getAsBoolean());
    if (values.containsKey("interface.blinkingUiCursor"))
      MenuPreferences.previewUiBlink(values.get("interface.blinkingUiCursor").getAsBoolean());
    if (values.containsKey("interface.blinkingChatCursor"))
      MenuPreferences.previewBlink(values.get("interface.blinkingChatCursor").getAsBoolean());
    if (values.containsKey("interface.debugTextColor"))
      MenuPreferences.previewDebugColor(values.get("interface.debugTextColor").getAsString());
    if (values.containsKey("interface.debugTimeOfDay"))
      MenuPreferences.previewDebugTime(values.get("interface.debugTimeOfDay").getAsBoolean());
    if (values.containsKey("interface.useFeetYCoordinates"))
      MenuPreferences.previewFeetY(values.get("interface.useFeetYCoordinates").getAsBoolean());
  }

  public void apply(Map<String, JsonElement> values) throws Exception {
    validate(values);
    MenuPreferences.update(v -> {
      if (values.containsKey("interface.pauseToOptions")) v.pauseToOptions = values.get("interface.pauseToOptions").getAsBoolean();
      if (values.containsKey("interface.blinkingUiCursor")) v.blinkingUiCursor = values.get("interface.blinkingUiCursor").getAsBoolean();
      if (values.containsKey("interface.blinkingChatCursor")) v.blinkingChatCursor = values.get("interface.blinkingChatCursor").getAsBoolean();
      if (values.containsKey("interface.debugTextColor")) v.debugTextColor = values.get("interface.debugTextColor").getAsString();
      if (values.containsKey("interface.debugTimeOfDay")) v.debugTimeOfDay = values.get("interface.debugTimeOfDay").getAsBoolean();
      if (values.containsKey("interface.useFeetYCoordinates")) v.useFeetYCoordinates = values.get("interface.useFeetYCoordinates").getAsBoolean();
    });
    preview(values);
  }
}
