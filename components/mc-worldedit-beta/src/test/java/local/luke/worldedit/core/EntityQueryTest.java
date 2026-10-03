package local.luke.worldedit.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

class EntityQueryTest {
  @Test
  void removalFiltersAreSpecific() {
    var items = EntityQuery.parse("items", "32");
    assertTrue(items.matches("Item", false, false));
    assertFalse(items.matches("Zombie", true, false));
    var mobs = EntityQuery.parse("mobs", "32");
    assertTrue(mobs.matches("Zombie", true, false));
    assertFalse(mobs.matches("Item", false, false));
    assertFalse(mobs.matches("Wolf", true, true));
    assertTrue(EntityQuery.parse("pets", "32").matches("Wolf", true, true));
    assertTrue(EntityQuery.parse("all", "32").matches("Wolf", true, true));
    assertTrue(EntityQuery.parse("items,vehicles", "32").matches("Boat", false, false));
    assertFalse(EntityQuery.parse("animals", "32").matches("Wolf", true, true));
  }

  @Test
  void butcherDefaultsToHostileAndAddsOnlyRequestedGroups() {
    var hostile = EntityQuery.butcher(Set.of(), "32");
    assertTrue(hostile.matches("Creeper", true, false));
    assertFalse(hostile.matches("Cow", true, false));
    var animals = EntityQuery.butcher(Set.of('a'), "32");
    assertTrue(animals.matches("Cow", true, false));
    assertFalse(animals.matches("Wolf", true, true));
    assertFalse(animals.matches("Squid", true, false));
    var all = EntityQuery.butcher(Set.of('f'), "32");
    assertTrue(all.matches("Wolf", true, true));
    assertTrue(all.matches("Squid", true, false));
    assertFalse(all.matches("Item", false, false));
  }

  @Test
  void sphereIncludesBoundaryAndRejectsInvalidInput() {
    var q = EntityQuery.parse("items", "5");
    assertTrue(q.contains(3, 4, 0));
    assertFalse(q.contains(4, 4, 0));
    assertFalse(q.contains(Double.NaN, 0, 0));
    for (String radius : new String[] {"NaN", "Infinity", "0", "-1", "257"})
      assertThrows(IllegalArgumentException.class, () -> EntityQuery.parse("all", radius));
    for (String type : new String[] {"players", "items,", "", "unknown"})
      assertThrows(IllegalArgumentException.class, () -> EntityQuery.parse(type, "32"));
  }
}
