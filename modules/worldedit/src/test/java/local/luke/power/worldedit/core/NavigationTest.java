package local.luke.power.worldedit.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class NavigationTest {
  private Navigation floors(int... heights) {
    return new Navigation(
        new Navigation.Terrain() {
          public boolean loaded(Pos p) {
            return Math.abs(p.x()) < 16 && Math.abs(p.z()) < 16;
          }

          public boolean clear(Pos p) {
            for (int y : heights) if (p.y() == y || p.y() + 1 == y) return false;
            return true;
          }

          public boolean floor(Pos p) {
            for (int y : heights) if (p.y() == y + 1) return true;
            return false;
          }
        });
  }

  @Test
  void upStopsAtCeilingAndWorldLimit() {
    Navigation n = floors(60, 70);
    assertEquals(new Pos(0, 66, 0), n.up(new Pos(0, 61, 0), 5));
    assertThrows(IllegalArgumentException.class, () -> n.up(new Pos(0, 61, 0), 10));
    assertThrows(IllegalArgumentException.class, () -> n.up(new Pos(0, 125, 0), 2));
  }

  @Test
  void multipleFloorsAndMissingFloors() {
    Navigation n = floors(10, 20, 30);
    assertEquals(new Pos(0, 31, 0), n.floor(new Pos(0, 11, 0), 1, 2));
    assertEquals(new Pos(0, 11, 0), n.floor(new Pos(0, 31, 0), -1, 2));
    assertThrows(IllegalArgumentException.class, () -> n.floor(new Pos(0, 31, 0), 1, 1));
  }

  @Test
  void ceilingClearanceAndUnstuck() {
    Navigation n = floors(60, 70);
    assertEquals(new Pos(0, 68, 0), n.ceiling(new Pos(0, 61, 0), 0));
    assertEquals(new Pos(0, 66, 0), n.ceiling(new Pos(0, 61, 0), 2));
    assertEquals(new Pos(0, 61, 0), n.unstuck(new Pos(0, 60, 0)));
    assertFalse(n.safe(new Pos(16, 61, 0)));
  }
}
