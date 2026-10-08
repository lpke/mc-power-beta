package local.luke.power.worldedit.core;

import static org.junit.jupiter.api.Assertions.*;
import local.luke.power.worldedit.chat.CommandCatalog;
import org.junit.jupiter.api.Test;

class BlockStatesTest {
  final BlockParser parser = new BlockParser(id -> id <= 96);
  void state(String input, int id, int meta) {
    BlockValue b = parser.block(input); assertEquals(id, b.id, input); assertEquals(meta, b.meta, input);
  }
  @Test void railsTorchesAndDirectionsMapToBetaStates() {
    state("rail[shape=east_west]", 66, 1);
    state("powered_rail[shape=ascending_north,powered=true]", 27, 12);
    state("powered_rail[powered=true,shape=ascending_north]", 27, 12);
    state("redstone_torch[lit=false]", 75, 5);
    state("redstone_wall_torch[facing=east,lit=false]", 75, 1);
    state("repeater[delay=4,facing=west,powered=true]", 94, 15);
    state("minecraft:furnace[facing=south,lit=true]", 62, 3);
    state("oak_stairs[facing=north,half=bottom]", 53, 3);
    state("oak_sign[rotation=13]", 63, 13);
  }
  @Test void masksOnlyMatchSpecifiedPropertiesAndPatternsKeepStateCommas() {
    var mask = parser.mask("redstone_torch[lit=false],powered_rail[powered=true]");
    assertTrue(mask.test(new BlockValue(75, 1))); assertFalse(mask.test(new BlockValue(76, 1)));
    assertTrue(mask.test(new BlockValue(27, 10))); assertFalse(mask.test(new BlockValue(27, 2)));
    assertTrue(parser.mask("redstone_torch").test(new BlockValue(76, 1)));
    var p = parser.pattern("50%powered_rail[shape=east_west,powered=true],50%rail[shape=north_south]");
    for (int i = 0; i < 50; i++) { var b = p.apply(new Pos(i, 65, 0)); assertTrue(b.id == 27 && b.meta == 9 || b.id == 66 && b.meta == 0); }
  }
  @Test void invalidAndUnavailableStatesAreRejectedBeforeEditing() {
    for (String input : new String[]{"rail[shape=north]", "rail[powered=true]", "powered_rail[shape=south_east]", "stone[lit=true]", "rail[shape=east_west,shape=north_south]", "rail[shape=east_west", "oak_stairs[half=top]", "redstone_torch[lit=yes]"})
      assertThrows(IllegalArgumentException.class, () -> parser.block(input), input);
    assertThrows(IllegalArgumentException.class, () -> parser.pattern("rail[shape=east_west]],stone"));
  }
  @Test void propertyNamesAndValuesComplete() {
    assertTrue(CommandCatalog.complete("//set rail[sh").contains("//set rail[shape=east_west]"));
    assertTrue(CommandCatalog.complete("//set powered_rail[shape=east_west,pow").contains("//set powered_rail[shape=east_west,powered=true]"));
    assertTrue(CommandCatalog.complete("//replace redstone_torch[lit=f").contains("//replace redstone_torch[lit=false]"));
  }
}
