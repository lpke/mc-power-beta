package local.luke.power.worldedit.core;

import static org.junit.jupiter.api.Assertions.*;

import local.luke.power.worldedit.config.WorldOverride;
import org.junit.jupiter.api.Test;

class AccessPolicyTest {
  @Test
  void precedenceAcrossEveryCombination() {
    for (boolean master : new boolean[] {false, true})
      for (WorldOverride world : WorldOverride.values())
        for (boolean integrate : new boolean[] {false, true})
          for (boolean installed : new boolean[] {false, true})
            for (boolean creative : new boolean[] {false, true}) {
              boolean expected =
                  master
                      && world != WorldOverride.DISABLED
                      && (world == WorldOverride.ENABLED || !integrate || !installed || creative);
              assertEquals(
                  expected, AccessPolicy.allows(master, world, integrate, installed, creative));
            }
  }

  @Test
  void unknownSavedValuesInherit() {
    assertEquals(WorldOverride.INHERIT, WorldOverride.decode(-1));
    assertEquals(WorldOverride.INHERIT, WorldOverride.decode(100));
  }
}
