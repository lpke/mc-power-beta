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
            new Setting("interface.blinkingChatCursor", "interface", "Interface", "Chat", "Blinking chat cursor",
                "Blink the chat cursor. Off keeps the beam visible while editing.", Setting.Kind.BOOLEAN,
                new JsonPrimitive(MenuPreferences.blinkingChatCursor()), new JsonPrimitive(false), 0, 1, 1, List.of(), false),
            new Setting("interface.debugTextColor", "interface", "Interface", "Debug information", "Text colour",
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
      } else if ((!e.getKey().equals("interface.pauseToOptions") && !e.getKey().equals("interface.blinkingChatCursor"))
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
    if (values.containsKey("interface.blinkingChatCursor"))
      MenuPreferences.previewBlink(values.get("interface.blinkingChatCursor").getAsBoolean());
    if (values.containsKey("interface.debugTextColor"))
      MenuPreferences.previewDebugColor(values.get("interface.debugTextColor").getAsString());
  }

  public void apply(Map<String, JsonElement> values) throws Exception {
    validate(values);
    MenuPreferences.update(v -> {
      if (values.containsKey("interface.pauseToOptions")) v.pauseToOptions = values.get("interface.pauseToOptions").getAsBoolean();
      if (values.containsKey("interface.blinkingChatCursor")) v.blinkingChatCursor = values.get("interface.blinkingChatCursor").getAsBoolean();
      if (values.containsKey("interface.debugTextColor")) v.debugTextColor = values.get("interface.debugTextColor").getAsString();
    });
    preview(values);
  }
}
