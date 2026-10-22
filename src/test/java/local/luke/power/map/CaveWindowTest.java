package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class CaveWindowTest {
  @Test void contextValidationRetainsThePersistedGrammarWithoutRegexWorkPerTile() {
    for (int above = 0; above <= 64; above++) for (int below = 0; below <= 64; below++)
      for (int phase = 0; phase < 8; phase++) {
        String context = "cave-v2/a" + above + "-b" + below + "/p" + phase;
        assertEquals(context, new MapMath.Key(0, 0, 0, 0, 0, context).context());
      }
    assertEquals("cave-v2/a00-b01/p7", new MapMath.Key(0, 0, 0, 0, 0, "cave-v2/a00-b01/p7").context());
    for (String context : new String[]{"cave-v2/a-b1/p0", "cave-v2/a1-b/p0", "cave-v2/a000-b1/p0",
        "cave-v2/a1-b001/p0", "cave-v2/a65-b1/p0", "cave-v2/a1-b65/p0", "cave-v2/a1-b1/p8",
        "cave-v2/a1-b1/p00", "cave-v2/a+1-b1/p0", "cave-v2/a1-b1/p0/", "cave-v2/a1-b1/p0\n",
        "cave-v2/a１-b1/p0", "../cave-v2/a1-b1/p0", "cave-v3/a1-b1/p0"})
      assertThrows(IllegalArgumentException.class, () -> new MapMath.Key(0, 0, 0, 0, 0, context), context);
  }
  @Test void exactHeightAndSearchRangeOwnTheirSavedContext() {
    CaveWindow a = CaveWindow.at(64.9, 2, 8), b = CaveWindow.at(65.1, 2, 8);
    assertEquals(64, a.reference());
    assertEquals(66, a.top()); assertEquals(56, a.bottom());
    assertEquals(a.layer(), b.layer());
    assertNotEquals(a.context(), b.context());
    assertEquals("cave-v2/a2-b8/p0", a.context());
    assertEquals("cave-v2/a2-b8/p1", b.context());
    assertNotEquals(a.context(), CaveWindow.at(64.9, 3, 8).context());
    assertNotEquals(a.context(), CaveWindow.at(64.9, 2, 9).context());
    assertEquals(0, CaveWindow.at(-20, 2, 8).bottom());
    assertEquals(127, CaveWindow.at(140, 2, 8).top());
    assertThrows(IllegalArgumentException.class, () -> CaveWindow.at(Double.NaN, 2, 8));
    assertThrows(IllegalArgumentException.class, () -> CaveWindow.at(64, 2, -1));
  }
  @Test void aboveAndBelowAcceptTheSameZeroThrough64RangeAndPreserveLegacyContexts() {
    for (int above : new int[]{0, 1, 10, 32, 64}) for (int below : new int[]{0, 1, 8, 10, 64}) {
      var window = CaveWindow.at(64, above, below);
      var key = new MapMath.Key(0, window.layer(), 0, 0, 0, window.context());
      assertEquals(window.context(), key.context());
      assertEquals(Math.min(127, 64 + above), window.top());
      assertEquals(Math.max(0, 64 - below), window.bottom());
    }
    var single = CaveWindow.at(64, 0, 0);
    assertEquals(64, single.floor(y -> false, y -> { assertEquals(64, y); return true; }));
    assertEquals(63, single.floor(y -> false, y -> { assertEquals(64, y); return false; }));
    assertEquals("cave-v2/a0-b1/p0", CaveWindow.at(64, 0, 1).context());
    for (int invalid : new int[]{-1, 65}) {
      assertThrows(IllegalArgumentException.class, () -> CaveWindow.at(64, invalid, 10));
      assertThrows(IllegalArgumentException.class, () -> CaveWindow.at(64, 10, invalid));
    }
    var key = new MapMath.Key(0, 8, 0, 0, 0);
    assertThrows(IllegalArgumentException.class, () -> key.withContext("cave-v2/a65-b10/p0"));
    assertThrows(IllegalArgumentException.class, () -> key.withContext("cave-v2/a10-b65/p0"));
  }
  @Test void skipsCeilingAndSelectsOnlyFloorsInsideTheExactWindow() {
    CaveWindow window = CaveWindow.at(64, 2, 8);
    var reads = new ArrayList<Integer>();
    int floor = window.floor(y -> { reads.add(y); return y >= 65; }, y -> { reads.add(y); return y == 56; });
    assertEquals(56, floor);
    assertTrue(reads.stream().allMatch(y -> y >= 56 && y <= 66));
    assertTrue(reads.size() <= (window.top() - window.bottom() + 1) * 2);
    assertEquals(55, window.floor(y -> false, y -> y == 55));
  }
  @Test void solidAndFloorlessWindowsReportSurveyedVoidWithoutReadingBeyondTheirBounds() {
    CaveWindow window = CaveWindow.at(3, 2, 8);
    assertEquals(-1, window.floor(y -> {
      assertTrue(y >= 0 && y <= 5); return true;
    }, y -> { fail("A completely solid window has no navigable floor"); return false; }));
    assertEquals(-1, window.floor(y -> false, y -> {
      assertTrue(y >= 0 && y <= 5); return false;
    }));
  }
  @Test void transparentBodiesAndInterveningAirNeverDescendBelowTheWindow() {
    int bottom = 56;
    var reads = new ArrayList<Integer>();
    int afterWater = CaveWindow.descend(60, bottom, y -> { reads.add(y); return true; });
    assertEquals(bottom - 1, afterWater);
    assertEquals(java.util.List.of(60, 59, 58, 57, 56), reads);
    assertEquals(bottom - 1, CaveWindow.descend(afterWater, bottom, y -> {
      fail("A lower transparent surface outside the window must not be read"); return false;
    }));
    assertEquals(58, CaveWindow.descend(60, bottom, y -> y > 58));
  }

  @Test void legacySurfaceAndCavePathsRemainStableWhileNewWindowsAreDistinct() {
    var surface = new MapMath.Key(0, -1, 0, -1, 2);
    var legacy = new MapMath.Key(-1, 8, 0, -1, 2);
    var modern = legacy.withContext(CaveWindow.at(64, 2, 8).context());
    assertEquals("d0/l-1/z0/-1.2.pbmap", surface.file());
    assertEquals("d-1/l8/z0/-1.2.pbmap", legacy.file());
    assertEquals("cave-v2/a2-b8/p0/d-1/l8/z0/-1.2.pbmap", modern.file());
    assertEquals(modern.context(), modern.parent().context());
    assertNotEquals(legacy, modern);
    assertThrows(IllegalArgumentException.class, () -> surface.withContext(modern.context()));
    assertThrows(IllegalArgumentException.class, () -> legacy.withContext("../another-world"));
    assertThrows(IllegalArgumentException.class, () -> legacy.withContext("cave-v2/a2-b8/p8"));
  }
}
