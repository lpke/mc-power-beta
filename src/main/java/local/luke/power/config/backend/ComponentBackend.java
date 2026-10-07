package local.luke.power.config.backend;

import com.google.gson.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import net.fabricmc.loader.api.FabricLoader;

/** Uses validated component APIs; auto-apply previews stay in memory until slider release. */
public final class ComponentBackend implements Backend {
  private final String id, getter;
  private final Class<?> api, type;
  private final List<Setting> entries = new ArrayList<>();

  public ComponentBackend(String id, String apiName, String getter) throws Exception {
    this.id = id;
    this.getter = getter;
    api = Class.forName(apiName);
    Object current = api.getMethod(getter).invoke(null);
    type = current.getClass();
    visit("", current, type.getConstructor().newInstance());
  }

  public static void register(ConfigSession session) throws Exception {
    for (String[] spec :
        new String[][] {
          {"tweaks", "local.luke.power.building.config.Config", "current"},
          {"creative", "local.luke.power.creative.config.Config", "current"},
          {
            "worldedit",
            "local.luke.power.worldedit.WorldEditor",
            "settings"
          }
        }) {
      ComponentBackend b = new ComponentBackend(spec[0], spec[1], spec[2]);
      session.add(b, b.entries);
    }
  }

  private void visit(String prefix, Object current, Object defaults) throws Exception {
    for (Field f : current.getClass().getFields()) {
      if (Modifier.isStatic(f.getModifiers())) continue;
      String name = prefix + f.getName(), key = id + "." + name;
      Object v = f.get(current), d = f.get(defaults);
      Class<?> t = f.getType();
      if (!t.isPrimitive() && !t.isEnum() && t != List.class) {
        visit(name + ".", v, d);
        continue;
      }
      Setting.Kind kind =
          t == boolean.class
              ? Setting.Kind.BOOLEAN
              : t == int.class
                  ? Setting.Kind.INTEGER
                  : t.isEnum() ? Setting.Kind.CHOICE : Setting.Kind.TEXT;
      JsonElement value = encode(v), def = encode(d);
      String page =
          id.equals("creative")
              ? "Creative"
              : id.equals("worldedit") ? "World editing" : "Building";
      String group =
          id.equals("creative")
              ? "Creative controls"
              : id.equals("worldedit") ? "Editor" : "Placement";
      if (id.equals("tweaks")) {
        if (name.equals("inventoryWhileMoving")) {
          page = "Inventory";
          group = "Inventory management";
        } else if (name.startsWith("hotbar.")) {
          page = "Inventory";
          group = "Hotbar swapping";
        } else if (name.startsWith("freeLook")) {
          page = "Camera";
          group = "Free look";
        } else if (name.startsWith("autoWalk") || name.startsWith("sneak.")) {
          page = "Movement";
          group = "Walking";
        } else if (name.equals("boatSteering") || name.equals("fastMinecarts")) {
          page = "Movement";
          group = "Vehicles";
        } else if (name.startsWith("flexible.")) group = "Flexible placement";
        else if (name.startsWith("slabs.")) group = "Slab completion";
        else if (name.equals("clickMining") || name.equals("obsidianBreakingSpeed")) group = "Mining";
        if (Set.of(
                "autoWalk",
                "freeLook",
                "boatSteering",
                "fastMinecarts",
                "clickMining",
                "placement.enabled",
                "placement.restrictionEnabled",
                "flexible.enabled",
                "sneak.enabled",
                "hotbar.swap",
                "hotbar.scroll")
            .contains(name)) def = new JsonPrimitive(false);
      }
      if (id.equals("worldedit") && name.equals("enabled")) def = new JsonPrimitive(true);
      List<String> choices =
          t.isEnum()
              ? Arrays.stream(t.getEnumConstants()).map(this::enumLabel).toList()
              : List.of();
      if (name.equals("sprintToggle") || name.equals("freeLookToggle"))
        choices = List.of("Hold", "Toggle");
      if (name.equals("flexible.overlayColor")) {
        kind = Setting.Kind.CHOICE;
        choices = List.of("Blue", "Cyan", "Orange", "Green");
      }
      if (id.equals("worldedit") && name.equals("color")) {
        kind = Setting.Kind.CHOICE;
        choices = List.of("Cyan", "Green", "Orange", "Purple");
      }
      if (name.equals("hotbar.selectedRow")) {
        kind = Setting.Kind.CHOICE;
        choices = List.of("Top", "Middle", "Bottom");
      }
      if (id.equals("creative") && (name.equals("blockReach") || name.equals("entityReach"))) {
        kind = Setting.Kind.DECIMAL;
        value = new JsonPrimitive(((Number) v).doubleValue() / 10);
        def = new JsonPrimitive(((Number) d).doubleValue() / 10);
      }
      double[] bounds = bounds(name, id);
      String description =
          t == List.class ? "Item IDs separated by commas, for example 1, 44:2." : "";
      entries.add(
          Catalog.setting(
              key,
              id,
              page,
              group,
              Catalog.words(f.getName()),
              description,
              kind,
              value,
              def,
              bounds[0],
              bounds[1],
              bounds[2],
              choices,
              false));
    }
  }

  private String enumLabel(Object value) {
    try {
      return value.getClass().getField("label").get(value).toString();
    } catch (ReflectiveOperationException e) {
      return Catalog.words(value.toString());
    }
  }

  private JsonElement encode(Object value) throws Exception {
    if (value instanceof Enum<?> e) return new JsonPrimitive(e.ordinal());
    if (value instanceof List<?> list)
      return new JsonPrimitive(
          (String)
              Class.forName("local.luke.power.fastplace.config.Settings")
                  .getMethod("formatFilters", List.class)
                  .invoke(null, list));
    return Catalog.JSON.toJsonTree(value);
  }

  private static double[] bounds(String name, String id) {
    String leaf = name.substring(name.lastIndexOf('.') + 1);
    return switch (leaf) {
      case "obsidianBreakingSpeed" -> new double[] {0, 100, 1};
      case "autoWalkHoldMillis" -> new double[] {100, 3000, 50};
      case "attemptsPerTick" -> new double[] {1, 16, 1};
      case "overlayColor", "color" -> new double[] {0, 3, 1};
      case "overlayOpacity" -> new double[] {10, 90, 10};
      case "opacity" -> new double[] {10, 100, 10};
      case "selectedRow" -> new double[] {0, 2, 1};
      case "offsetX", "offsetY" -> new double[] {-4096, 4096, 4};
      case "flightSpeed" -> new double[] {25, 400, 5};
      case "sprintMultiplier" -> new double[] {100, 400, 10};
      case "glide" -> new double[] {0, 5, 1};
      case "doubleTapTicks" -> new double[] {2, 20, 1};
      case "spectatorSpeed" -> new double[] {0, 400, 5};
      case "spectatorScrollStep" -> new double[] {1, 100, 1};
      case "blockReach", "entityReach" -> new double[] {30, 100, 5};
      case "wandItem" -> new double[] {1, 32767, 1};
      case "blockLimit" -> new double[] {1, 262144, 4096};
      case "blocksPerTick" -> new double[] {64, 8192, 64};
      case "historySize" -> new double[] {1, 100, 1};
      case "lineWidth" -> new double[] {1, 4, 1};
      default -> new double[] {0, 1, 1};
    };
  }

  public String id() {
    return id;
  }

  public List<Path> files() {
    return List.of(local.luke.power.storage.PowerConfig.path());
  }

  private Object draft(Map<String, JsonElement> changes) throws Exception {
    Object current = api.getMethod(getter).invoke(null);
    Object draft = current.getClass().getMethod("copy").invoke(current);
    for (var e : changes.entrySet()) {
      String path = e.getKey().substring(id.length() + 1);
      String[] parts = path.split("\\.");
      Object object = draft;
      for (int i = 0; i < parts.length - 1; i++)
        object = object.getClass().getField(parts[i]).get(object);
      Field field = object.getClass().getField(parts[parts.length - 1]);
      Class<?> t = field.getType();
      Object value =
          t.isEnum()
              ? t.getEnumConstants()[e.getValue().getAsInt()]
              : t == List.class
                  ? Class.forName("local.luke.power.fastplace.config.Settings")
                      .getMethod("parseFilters", String.class)
                      .invoke(null, e.getValue().getAsString())
                  : Catalog.JSON.fromJson(e.getValue(), t);
      if (id.equals("creative") && (path.equals("blockReach") || path.equals("entityReach")))
        value = (int) Math.round(e.getValue().getAsDouble() * 10);
      field.set(object, value);
    }
    return draft;
  }

  public void validate(Map<String, JsonElement> changes) throws Exception {
    Object draft = draft(changes);
    if (id.equals("creative")) type.getMethod("validate").invoke(draft);
    else
      Class.forName(
              id.equals("tweaks")
                  ? "local.luke.power.building.config.SettingsValidator"
                  : "local.luke.power.worldedit.config.SettingsValidator")
          .getMethod("validate", type)
          .invoke(null, draft);
  }

  public boolean previewsAutomatically(Setting s) { return !s.restart; }

  public void preview(Map<String, JsonElement> changes) throws Exception {
    api.getMethod("preview", type).invoke(null, draft(changes));
  }

  public void apply(Map<String, JsonElement> changes) throws Exception {
    api.getMethod("apply", type).invoke(null, draft(changes));
  }
}
