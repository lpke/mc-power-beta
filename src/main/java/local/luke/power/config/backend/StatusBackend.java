package local.luke.power.config.backend;

import com.google.gson.JsonElement;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.status.*;
import local.luke.power.storage.PowerConfig;

public final class StatusBackend implements Backend {
  public static void register(ConfigSession session) throws Exception {
    List<Setting> entries = new ArrayList<>();
    StatusSettings current = StatusConfig.copy(), defaults = new StatusSettings();
    for (var field : StatusSettings.class.getFields()) {
      String name = field.getName();
      boolean toggle = field.getType() == boolean.class;
      String label = switch (name) {
        case "enabled" -> "Show active tweaks";
        case "offsetX" -> "Horizontal offset";
        case "offsetY" -> "Vertical offset";
        case "textColor" -> "Text colour";
        case "opacity" -> "Text opacity";
        case "autoWalk" -> "Auto-walk";
        default -> Catalog.words(name);
      };
      String help = switch (name) {
        case "enabled" -> "List active temporary tweaks on the HUD, one per line. Hidden in menus and when the HUD is hidden.";
        case "position" -> "Anchor the list to a corner, edge or centre of the screen. Bottom anchors grow upward.";
        case "offsetX" -> "Move right with positive values, left with negative values. Uses scaled GUI pixels.";
        case "offsetY" -> "Move down with positive values, up with negative values. Uses scaled GUI pixels.";
        case "textColor" -> "Colour of every entry. Opacity controls transparency separately.";
        case "opacity" -> "Text visibility from transparent at 0% to solid at 100%.";
        case "placementRestriction" -> "Show the current restriction rule while fast placement and its restriction are enabled.";
        case "freeLook" -> "Show while free look is latched on in Toggle mode. Hold mode is omitted.";
        case "freecamPlayerMovement" -> "Show while the detached camera allows movement of your actual player.";
        case "autoWalk" -> "Show while auto-walk is running, rather than whenever the feature is available.";
        default -> "Include " + label.toLowerCase(Locale.ROOT) + " while it is enabled.";
      };
      boolean offset = name.startsWith("offset");
      Setting.Kind kind = toggle ? Setting.Kind.BOOLEAN : name.equals("position") ? Setting.Kind.CHOICE
          : field.getType() == String.class ? Setting.Kind.TEXT : Setting.Kind.INTEGER;
      entries.add(new Setting("activeTweaks." + name, "activeTweaks", "Interface", "Active tweaks",
          label, help, kind, Catalog.JSON.toJsonTree(field.get(current)), Catalog.JSON.toJsonTree(field.get(defaults)),
          offset ? -4096 : 0, offset ? 4096 : name.equals("opacity") ? 100 : name.equals("position") ? 8 : 1,
          1, name.equals("position") ? List.of("Top left", "Top centre", "Top right", "Middle left",
              "Centre", "Middle right", "Bottom left", "Bottom centre", "Bottom right") : List.of(), false));
    }
    session.add(new StatusBackend(), entries);
  }
  public String id() { return "activeTweaks"; }
  public List<Path> files() { return List.of(PowerConfig.path()); }
  private StatusSettings draft(Map<String, JsonElement> values) throws Exception {
    StatusSettings next = StatusConfig.copy();
    for (var e : values.entrySet()) {
      var field = StatusSettings.class.getField(e.getKey().substring("activeTweaks.".length()));
      field.set(next, Catalog.JSON.fromJson(e.getValue(), field.getType()));
    }
    next.validate();
    return next;
  }
  public void validate(Map<String, JsonElement> values) throws Exception { draft(values); }
  public boolean previews(Setting setting) { return true; }
  public void preview(Map<String, JsonElement> values) throws Exception { StatusConfig.preview(draft(values)); }
  public void apply(Map<String, JsonElement> values) throws Exception { StatusConfig.save(draft(values)); }
}
