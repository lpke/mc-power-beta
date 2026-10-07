package local.luke.power.config;

import com.google.gson.JsonElement;
import net.minecraft.Item;
import net.modificationstation.stationapi.api.registry.BlockRegistry;
import net.modificationstation.stationapi.api.util.Identifier;

/** Validate identifiers against the loaded game before publishing an editor draft. */
final class ItemSettings {
  static void attach(Setting setting) {
    switch (Catalog.text(Catalog.metadata(setting.id), "valueType", "")) {
      case "itemId" -> setting.validator(value -> item(value.getAsBigDecimal().intValueExact()));
      case "itemFilters" -> setting.validator(ItemSettings::filters);
      case "blockIdentifiers" -> setting.validator(ItemSettings::blocks);
      default -> { }
    }
  }

  private static void item(int id) {
    if (id <= 0 || id >= Item.ITEMS.length || Item.ITEMS[id] == null)
      throw new IllegalArgumentException("Unknown item ID: " + id);
  }

  private static void filters(JsonElement value) {
    String text = value.getAsString();
    if (text.isBlank()) return;
    for (String token : text.split(",", -1)) {
      String[] parts = token.trim().split(":", -1);
      try {
        if (parts.length > 2) throw new NumberFormatException();
        int id = Integer.parseInt(parts[0]);
        int damage = parts.length == 2 ? Integer.parseInt(parts[1]) : -1;
        if (damage < -1 || damage > 32767) throw new NumberFormatException();
        item(id);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Use item IDs with optional :metadata, for example 1, 35:4");
      }
    }
  }

  private static void blocks(JsonElement value) {
    if (!value.isJsonArray()) throw new IllegalArgumentException("Use a list of block identifiers");
    for (JsonElement entry : value.getAsJsonArray()) {
      String text = entry.getAsString();
      try {
        Identifier id = Identifier.of(text);
        var block = BlockRegistry.INSTANCE.get(id);
        if (block == null || !id.equals(BlockRegistry.INSTANCE.getId(block)))
          throw new IllegalArgumentException();
      } catch (RuntimeException e) { throw new IllegalArgumentException("Unknown block identifier: " + text); }
    }
  }
}
