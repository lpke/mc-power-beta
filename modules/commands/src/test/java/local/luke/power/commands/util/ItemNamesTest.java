package local.luke.power.commands.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ItemNamesTest {
  @Test void displayNamesCompleteAndResolveWithoutNamespaces() {
    var translated = new ItemNames(Map.of("minecraft:planks", 5));
    translated.alias("Wooden Planks", 5);
    assertArrayEquals(new String[]{"den_planks"}, translated.suggest("woo"));
    assertArrayEquals(new String[]{"s"}, translated.suggest("wooden_plank"));
    assertArrayEquals(new String[]{""}, translated.suggest("wooden_planks"));
    assertEquals(5, translated.resolve("WOODEN_PLANKS"));
    assertEquals(5, translated.resolve("planks"));
    assertEquals(5, translated.resolve("minecraft:planks"));
  }
  @Test void displayAliasesKeepCollisionsAndUnknownTranslationKeysSafe() {
    var translated = new ItemNames(Map.of("minecraft:planks", 5, "example:boards", 500));
    translated.alias("Wooden Planks", 5); translated.alias("Wooden Planks", 500);
    var error = assertThrows(IllegalArgumentException.class, () -> translated.resolve("wooden_planks"));
    assertTrue(error.getMessage().contains("minecraft:planks"));
    assertTrue(error.getMessage().contains("example:boards"));
    translated.alias("tile.wood.name", 5);
    assertArrayEquals(new String[0], translated.suggest("tile_"));
  }
  @Test void registeredPathsKeepPriorityOverAnotherItemsTranslatedAlias() {
    var indexed = new ItemNames(Map.of("minecraft:tall_grass", 31, "power_creative_inventory:fern", 500));
    indexed.alias("Fern", 31);
    assertEquals(500, indexed.resolve("fern"));
    assertEquals(31, indexed.resolve("minecraft:tall_grass"));
  }
  private final ItemNames names = new ItemNames(Map.of(
      "minecraft:dirt", 3,
      "minecraft:diamond_shovel", 277,
      "power_creative_inventory:fern", 500,
      "other:crystal", 501));

  @Test
  void plainPathsAndExplicitIdentifiersResolveTheSameItems() {
    assertEquals(3, names.resolve("dirt"));
    assertEquals(3, names.resolve("minecraft:dirt"));
    assertEquals(500, names.resolve("fern"));
    assertEquals(500, names.resolve("power_creative_inventory:fern"));
    assertEquals(501, names.resolve("crystal"));
    assertEquals(277, names.resolve("DIAMOND_SHOVEL"));
    assertEquals(-1, names.resolve("unknown:dirt"));
    assertEquals(-1, names.resolve("minecraft:fern"));
  }

  @Test
  void normalSuggestionsCompleteNamesDirectlyAndKeepExplicitPrefixes() {
    assertArrayEquals(new String[] {"amond_shovel", "rt"}, names.suggest("di"));
    assertArrayEquals(new String[] {"rn"}, names.suggest("fe"));
    assertArrayEquals(new String[] {"amond_shovel", "rt"}, names.suggest("minecraft:di"));
    assertArrayEquals(new String[] {"rn"}, names.suggest("power_creative_inventory:fe"));
    assertArrayEquals(new String[] {"amond_shovel", "rt"}, names.suggest("DI"));
    for (String suggestion : names.suggest("")) assertFalse(suggestion.contains(":"));
  }

  @Test
  void collisionsRequireAnExplicitIdentifierButAliasesOfOneItemDoNot() {
    ItemNames collision = new ItemNames(Map.of("minecraft:dirt", 3, "other:dirt", 500));
    String error = assertThrows(IllegalArgumentException.class, () -> collision.resolve("dirt")).getMessage();
    assertTrue(error.contains("minecraft:dirt"));
    assertTrue(error.contains("other:dirt"));
    assertEquals(3, collision.resolve("minecraft:dirt"));
    assertEquals(500, collision.resolve("other:dirt"));
    assertArrayEquals(new String[] {"rt"}, collision.suggest("di"));
    ItemNames aliases = new ItemNames(Map.of("minecraft:dirt", 3, "legacy:dirt", 3));
    assertEquals(3, aliases.resolve("dirt"));
  }
}
