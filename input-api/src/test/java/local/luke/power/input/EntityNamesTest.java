package local.luke.power.input;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class EntityNamesTest {
  @Test void nativeRegistrySpellingsAndExistingSummonAliasesKeepTheirModernNames() {
    for(String name:new String[]{"PigZombie","pig_zombie","ZombiePigman","minecraft:zombie_pigman","zombified_piglin"})
      assertEquals("zombified_piglin",EntityNames.modern(name));
    assertEquals("zombie",EntityNames.modern("minecraft:Zombie"));
    assertEquals("falling_block",EntityNames.modern("FallingSand"));
    assertEquals("magma_cube",EntityNames.modern("LavaSlime"));
  }
}
