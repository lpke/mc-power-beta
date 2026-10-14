package local.luke.power.mining;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static local.luke.power.mining.VeinSearch.Position;

class VeinSearchTest {
  @Test void cornersConnectButIsolatedBlocksAndUnloadedPositionsDoNot() {
    var origin = new Position(0, 64, 0);
    var ore = Set.of(origin, new Position(1,65,1), new Position(2,65,1), new Position(4,65,1));
    var search = new VeinSearch(origin, 64, 4096, 16);
    while (!search.complete()) search.step(7, p -> ore.contains(p) && p.x() < 2);
    assertEquals(List.of(origin, new Position(1,65,1)), search.blocks());
  }
  @Test void giantVolumesRespectEveryBudgetAndNeverVisitTwice() {
    for (int maxBlocks : new int[]{1,8,64}) for (int maxChecks : new int[]{1,17,300}) {
      var search = new VeinSearch(new Position(0,0,0), maxBlocks, maxChecks, 2);
      var seen = new HashSet<Position>();
      while (!search.complete()) {
        int before = search.checks();
        search.step(3, p -> {
          assertTrue(seen.add(p)); assertTrue(p.y() >= 0 && p.y() <= 2);
          assertTrue(Math.abs(p.x()) <= 2 && Math.abs(p.z()) <= 2); return true;
        });
        assertTrue(search.checks() - before <= 3);
      }
      assertTrue(search.blocks().size() <= maxBlocks); assertTrue(search.checks() <= maxChecks);
    }
  }
  @Test void incrementalAndSingleBatchSearchHaveTheSameDeterministicOrder() {
    var origin = new Position(-17,127,-1);
    var a = new VeinSearch(origin,128,4096,4); var b = new VeinSearch(origin,128,4096,4);
    while (!a.complete()) a.step(1,p -> p.x() == -17);
    b.step(4096,p -> p.x() == -17);
    assertEquals(a.blocks(), b.blocks()); assertEquals(a.checks(), b.checks());
  }
}
