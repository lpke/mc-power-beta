package local.luke.power.worldedit.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BlockNamesTest {
  private final BlockParser parser = new BlockParser(id -> id <= 96);

  @Test
  void plainAndExplicitNamesPreserveMetadataAndStates() {
    assertTrue(parser.block("dirt").same(parser.block("minecraft:dirt")));
    assertTrue(parser.block("wool:14").same(parser.block("minecraft:wool:14")));
    assertTrue(parser.block("35:14").same(parser.block("wool:14")));
    assertTrue(parser.block("fern").same(new BlockValue(31, 2)));
    assertTrue(parser.block("tall_grass").same(new BlockValue(31, 1)));
    assertTrue(parser.block("fern").same(parser.block("power_creative_inventory:fern")));
    assertTrue(parser.block("tall_grass").same(parser.block("power_creative_inventory:tall_grass")));
    assertTrue(parser.block("power_creative_inventory:fern:1").same(new BlockValue(31, 1)));
    assertTrue(parser.block("minecraft:rail[shape=east_west]").same(new BlockValue(66, 1)));
  }

  @Test
  void namespaceNormalizationCannotRewriteUnknownIdentifiers() {
    for (String bad : new String[] {"other:dirt", "power_creative_inventory:dirt", "minecraft:minecraft:dirt", "dirtminecraft:"})
      assertThrows(IllegalArgumentException.class, () -> parser.block(bad), bad);
  }

  @Test
  void namesStayPlainAndMasksRetainVariantMatching() {
    assertTrue(parser.names().contains("fern"));
    assertTrue(parser.names().stream().noneMatch(name -> name.contains(":")));
    var mask = parser.mask("power_creative_inventory:fern");
    assertTrue(mask.test(new BlockValue(31, 2)));
    assertFalse(mask.test(new BlockValue(31, 1)));
    assertTrue(parser.mask("minecraft:wool").test(new BlockValue(35, 14)));
    assertFalse(parser.mask("minecraft:wool:1").test(new BlockValue(35, 14)));
  }
}
