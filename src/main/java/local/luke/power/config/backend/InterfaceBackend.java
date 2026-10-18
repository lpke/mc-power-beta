package local.luke.power.config.backend;

import com.google.gson.*;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.MenuPreferences;

public final class InterfaceBackend implements Backend {
  public static void register(ConfigSession session) {
    List<Setting> settings = new ArrayList<>(List.of(
            new Setting("interface.autoApply", "interface", "Interface", "General", "Auto apply", "", Setting.Kind.BOOLEAN,
                new JsonPrimitive(MenuPreferences.current().autoApply), new JsonPrimitive(true), 0, 1, 1, List.of(), false),
            new Setting("interface.debugPresets", "interface", "Interface", "Debug (F3) info", "F3 presets", "", Setting.Kind.LIST,
                MenuPreferences.current().debugPresets, new JsonArray(), 0, 0, 1, List.of(), false),
            new Setting("interface.debugOrder", "interface", "Interface", "Debug (F3) info", "Info order",
                "Arrange both columns of F3 lines in the preview. Drag between lines to insert, or into an empty row to leave a gap.", Setting.Kind.LIST,
                Catalog.JSON.toJsonTree(MenuPreferences.debugOrder()), Catalog.JSON.toJsonTree(local.luke.power.ui.DebugOrder.defaults()), 0, 0, 1, List.of(), false),
            new Setting("interface.debugRightOrder", "interface", "Interface", "Debug (F3) info", "Right info order", "",
                Setting.Kind.LIST, Catalog.JSON.toJsonTree(MenuPreferences.debugRightOrder()),
                Catalog.JSON.toJsonTree(local.luke.power.ui.DebugColumns.defaultRight()), 0, 0, 1, List.of(), false),
            new Setting("interface.debugTextSize", "interface", "Interface", "Debug (F3) info", "Text size",
                "Scale the F3 information in both columns.", Setting.Kind.DECIMAL,
                new JsonPrimitive(MenuPreferences.debugTextSize()), new JsonPrimitive(1), .5, 3, .1, List.of(), false),
            new Setting("interface.hidePowerBetaVersion", "interface", "Interface", "Title screen", "Hide Power Beta version",
                "Hide the Power Beta version beside the Minecraft version on the title screen.", Setting.Kind.BOOLEAN,
                new JsonPrimitive(MenuPreferences.hidePowerBetaVersion()), new JsonPrimitive(false), 0, 1, 1, List.of(), false),
            new Setting("interface.colours", "interface", "Interface", "General HUD", "Saved colours", "", Setting.Kind.LIST,
                Catalog.JSON.toJsonTree(MenuPreferences.current().colours), new JsonArray(), 0, 16, 1, List.of(), false),
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
                "Choose feet height or Beta eye height for Survival and Creative coordinates and coordinate-based commands, including during flight. Only Spectator and freecam report the base of the head.", Setting.Kind.BOOLEAN,
                new JsonPrimitive(MenuPreferences.useFeetYCoordinates()), new JsonPrimitive(true), 0, 1, 1, List.of(), false),
            new Setting("interface.debugTimeOfDay", "interface", "Interface", "Debug (F3) info", "Time of day",
                "Show the world's daylight time in F3, using a 12-hour clock.", Setting.Kind.BOOLEAN,
                new JsonPrimitive(MenuPreferences.debugTimeOfDay()), new JsonPrimitive(false), 0, 1, 1, List.of(), false),
            new Setting("interface.debugTextColor", "interface", "Interface", "Debug (F3) info", "Text colour",
                "Colour of all F3 debug text.", Setting.Kind.TEXT,
                new JsonPrimitive(MenuPreferences.debugTextColor()), new JsonPrimitive("#FFFFFF"), 0, 0, 1, List.of(), false)));
    for (var line : local.luke.power.ui.DebugLines.values()) if (line.ownsSetting())
      settings.add(new Setting(line.setting(), "interface", "Interface", "Debug (F3) info", line.label,
          "Include this line in the F3 information.", Setting.Kind.BOOLEAN,
          new JsonPrimitive(MenuPreferences.debugLine(line.id)), new JsonPrimitive(true), 0, 1, 1, List.of(), false));
    session.add(new InterfaceBackend(), settings);
  }

  public String id() {
    return "interface";
  }

  public List<Path> files() {
    return List.of(PowerConfig.path());
  }

  public void validate(Map<String, JsonElement> values) {
    if (values.containsKey("interface.debugOrder") || values.containsKey("interface.debugRightOrder"))
      new local.luke.power.ui.DebugColumns(values.containsKey("interface.debugOrder")
          ? local.luke.power.ui.DebugOrder.read(values.get("interface.debugOrder")) : MenuPreferences.debugOrder(),
          values.containsKey("interface.debugRightOrder") ? local.luke.power.ui.DebugOrder.read(values.get("interface.debugRightOrder")) : MenuPreferences.debugRightOrder());
    for (var e : values.entrySet()) {
      if (e.getKey().equals("interface.debugPresets")) {
        local.luke.power.ui.DebugPresets.checked(e.getValue());
      } else if ((e.getKey().equals("interface.debugOrder") || e.getKey().equals("interface.debugRightOrder"))) {
        local.luke.power.ui.DebugOrder.read(e.getValue());
      } else if (e.getKey().equals("interface.debugTextSize")) {
        double n = e.getValue().getAsDouble();
        if (!Double.isFinite(n) || n < .5 || n > 3) throw new IllegalArgumentException("F3 text size must be between 0.5 and 3.");
      } else if (e.getKey().startsWith("interface.debugLines.")) {
        var line = local.luke.power.ui.DebugLines.find(e.getKey().substring("interface.debugLines.".length()));
        if (line == null || !line.ownsSetting() || !e.getValue().isJsonPrimitive() || !e.getValue().getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException("Invalid F3 line visibility.");
      } else if (e.getKey().equals("interface.colours")) {
        if (!e.getValue().isJsonArray() || e.getValue().getAsJsonArray().size() > 16) throw new IllegalArgumentException("Save up to 16 colours");
        for (JsonElement value : e.getValue().getAsJsonArray()) {
          if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw new IllegalArgumentException("Invalid saved colour");
          local.luke.power.light.LightSettings.rgb(value.getAsString());
        }
      } else if (e.getKey().equals("interface.debugTextColor")) {
        local.luke.power.light.LightSettings.rgb(e.getValue().getAsString());
      } else if ((!e.getKey().equals("interface.autoApply") && !e.getKey().equals("interface.hidePowerBetaVersion") && !e.getKey().equals("interface.pauseToOptions") && !e.getKey().equals("interface.blinkingChatCursor") && !e.getKey().equals("interface.blinkingUiCursor") && !e.getKey().equals("interface.debugTimeOfDay") && !e.getKey().equals("interface.useFeetYCoordinates"))
          || !e.getValue().isJsonPrimitive() || !e.getValue().getAsJsonPrimitive().isBoolean())
        throw new IllegalArgumentException("Invalid interface preference");
    }
  }

  public boolean previews(Setting setting) {
    return true;
  }

  public void preview(Map<String, JsonElement> values) {
    validate(values);
    if (values.containsKey("interface.debugOrder")) MenuPreferences.previewDebugOrder(local.luke.power.ui.DebugOrder.read(values.get("interface.debugOrder")));
    if (values.containsKey("interface.debugRightOrder")) MenuPreferences.previewDebugRightOrder(local.luke.power.ui.DebugOrder.read(values.get("interface.debugRightOrder")));
    if (values.containsKey("interface.debugTextSize")) MenuPreferences.previewDebugSize(values.get("interface.debugTextSize").getAsDouble());
    values.forEach((id, value) -> { if (id.startsWith("interface.debugLines.")) MenuPreferences.previewDebugLine(id.substring("interface.debugLines.".length()), value.getAsBoolean()); });
    if (values.containsKey("interface.hidePowerBetaVersion"))
      MenuPreferences.previewPowerVersion(values.get("interface.hidePowerBetaVersion").getAsBoolean());
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
      if (values.containsKey("interface.autoApply")) v.autoApply = values.get("interface.autoApply").getAsBoolean();
      if (values.containsKey("interface.debugPresets")) v.debugPresets = local.luke.power.ui.DebugPresets.checked(values.get("interface.debugPresets"));
      if (values.containsKey("interface.debugOrder")) v.debugOrder = local.luke.power.ui.DebugOrder.read(values.get("interface.debugOrder"));
      if (values.containsKey("interface.debugRightOrder")) v.debugRightOrder = local.luke.power.ui.DebugOrder.read(values.get("interface.debugRightOrder"));
      if (values.containsKey("interface.debugTextSize")) v.debugTextSize = values.get("interface.debugTextSize").getAsDouble();
      values.forEach((id, value) -> { if (id.startsWith("interface.debugLines.")) v.debugLines.put(id.substring("interface.debugLines.".length()), value.getAsBoolean()); });
      if (values.containsKey("interface.hidePowerBetaVersion")) v.hidePowerBetaVersion = values.get("interface.hidePowerBetaVersion").getAsBoolean();
      if (values.containsKey("interface.colours")) {
        v.colours = new ArrayList<>(); values.get("interface.colours").getAsJsonArray().forEach(c -> v.colours.add(c.getAsString()));
      }
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
