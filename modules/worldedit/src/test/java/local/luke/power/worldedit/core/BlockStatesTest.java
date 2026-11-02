package local.luke.power.worldedit.core;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import local.luke.power.worldedit.chat.CommandCatalog;
import org.junit.jupiter.api.Test;

class BlockStatesTest {
  @org.junit.jupiter.api.BeforeEach void allowCommands() {
    local.luke.power.permissions.CommandPermissions.context(() -> new local.luke.power.permissions.CommandPermissions.Context("test", false, local.luke.power.rules.RuleSet.cheats()));
  }

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
    assertTrue(CommandCatalog.complete("//set di").contains("//set dirt"));
    assertTrue(CommandCatalog.complete("//set fe").contains("//set fern"));
    assertTrue(CommandCatalog.complete("//set minecraft:di").contains("//set minecraft:dirt"));
    assertTrue(CommandCatalog.complete("//set power_creative_inventory:fe").contains("//set power_creative_inventory:fern"));
    assertTrue(CommandCatalog.complete("//set rail[sh").contains("//set rail[shape=east_west]"));
    assertTrue(CommandCatalog.complete("//set powered_rail[shape=east_west,pow").contains("//set powered_rail[shape=east_west,powered=true]"));
    assertTrue(CommandCatalog.complete("//replace redstone_torch[lit=f").contains("//replace redstone_torch[lit=false]"));
  }

  @Test void spawnerMobsUseSummonNamesAndStayDistinctInPatternsAndMasks() {
    for(String input:List.of("spawner[mob=zombie]","minecraft:spawner[mob=minecraft:zombie]","52[mob=Zombie]"))
      assertEquals("Zombie",parser.block(input).spawnerMob);
    for(String name:List.of("PigZombie","pig_zombie","zombie_pigman","minecraft:zombified_piglin"))
      assertEquals("PigZombie",parser.block("spawner[mob="+name+"]").spawnerMob);
    assertEquals("Squid",parser.block("spawner[mob=squid]").spawnerMob);
    var mask=parser.mask("spawner[mob=zombie]");
    assertTrue(mask.test(new BlockValue(52,0,null,"Zombie")));
    assertFalse(mask.test(new BlockValue(52,0,null,"Pig")));
    assertTrue(parser.mask("spawner").test(new BlockValue(52,0,null,"Pig")));
    var pattern=parser.pattern("50%spawner[mob=zombie],50%spawner[mob=skeleton]");
    for(int i=0;i<40;i++)assertTrue(Set.of("Zombie","Skeleton").contains(pattern.apply(new Pos(i,65,0)).spawnerMob));
    for(String mob:List.of("all","player","mob","monster","@e","item","boat","arrow","creaking","magma_cube"))
      assertThrows(IllegalArgumentException.class,()->parser.block("spawner[mob="+mob+"]"));
    assertThrows(IllegalArgumentException.class,()->parser.block("stone[mob=zombie]"));
  }

  @Test void spawnerMobValuesCompleteIncludingQualifiedNamesAndWeightedPatterns() {
    assertTrue(CommandCatalog.complete("//set spawner[mo").contains("//set spawner[mob=zombie]"));
    assertTrue(CommandCatalog.complete("//set spawner[mob=zo").contains("//set spawner[mob=zombie]"));
    assertTrue(CommandCatalog.complete("//set spawner[mob=zom").contains("//set spawner[mob=zombified_piglin]"));
    assertTrue(CommandCatalog.complete("//set spawner[mob=minecraft:sq").contains("//set spawner[mob=minecraft:squid]"));
    assertTrue(CommandCatalog.complete("//set 20%stone,80%spawner[mob=sk").contains("//set 20%stone,80%spawner[mob=skeleton]"));
    assertTrue(CommandCatalog.complete("//replace spawner[mob=co").contains("//replace spawner[mob=cow]"));
    assertFalse(CommandCatalog.complete("//set spawner[mob=").stream().anyMatch(value->value.contains("mob=player")||value.contains("mob=item")));
  }
}
