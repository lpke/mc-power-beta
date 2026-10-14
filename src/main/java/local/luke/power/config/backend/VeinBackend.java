package local.luke.power.config.backend;

import com.google.gson.*;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.mining.*;
import local.luke.power.storage.PowerConfig;
import net.minecraft.Item;
import net.minecraft.block.Block;

public final class VeinBackend implements Backend {
  public static void register(ConfigSession session) throws Exception {
    List<Setting> entries = new ArrayList<>();
    var current = VeinConfig.copy(); var defaults = new VeinSettings();
    for (var field : VeinSettings.class.getFields()) {
      String id = "veinMining." + field.getName();
      var kind = field.getType() == boolean.class ? Setting.Kind.BOOLEAN : field.getType() == int.class
          ? Setting.Kind.INTEGER : field.getType() == double.class ? Setting.Kind.DECIMAL : Setting.Kind.LIST;
      Setting setting = Catalog.setting(id, "veinMining", "Building", "Vein mining", Catalog.words(field.getName()), "",
          kind, Catalog.JSON.toJsonTree(field.get(current)), Catalog.JSON.toJsonTree(field.get(defaults)), 0, 1, 1, List.of(), false);
      if (field.getName().equals("tools")) setting.validator(value -> {
        var checked = new VeinSettings();
        checked.tools = Catalog.JSON.fromJson(value, field.getGenericType()); checkTools(checked);
      });
      entries.add(setting);
    }
    session.add(new VeinBackend(), entries);
  }
  private static void checkTools(VeinSettings settings) {
    settings.validateTools(id -> id > 0 && id < Block.BLOCKS.length && Block.BLOCKS[id] != null,
        id -> id > 0 && id < Item.ITEMS.length && Item.ITEMS[id] != null);
  }
  public String id() { return "veinMining"; }
  public List<Path> files() { return List.of(PowerConfig.path()); }
  private VeinSettings draft(Map<String, JsonElement> values) throws Exception {
    var next = VeinConfig.copy();
    for (var e : values.entrySet()) {
      var field = VeinSettings.class.getField(e.getKey().substring("veinMining.".length()));
      field.set(next, Catalog.JSON.fromJson(e.getValue(), field.getGenericType()));
    }
    next.validate(); checkTools(next); return next;
  }
  public void validate(Map<String, JsonElement> values) throws Exception { draft(values); }
  public void apply(Map<String, JsonElement> values) throws Exception { VeinConfig.save(draft(values)); }
}
