package local.luke.power.gameplay;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class TntBudgetTest {
  private static final class Tnt { int fuse; boolean alive = true; int ticks; }
  @Test void explosionsAreBoundedFairAndNeverLost() {
    var budget = new TntBudget<Tnt>();
    var entities = new ArrayList<Tnt>();
    for (int i = 0; i < 400; i++) entities.add(new Tnt());
    for (int tick = 0; tick < 100; tick++) {
      budget.begin(); entities.stream().filter(e -> e.alive).forEach(budget::offer);
      int[] explosions = {0};
      budget.run(128, 4, 8_000_000, e -> e.alive, e -> e.fuse == 0,
          e -> { e.ticks++; e.alive = false; explosions[0]++; }, () -> 0);
      assertEquals(4, explosions[0]);
    }
    assertEquals(0, budget.size());
    assertTrue(entities.stream().allMatch(e -> e.ticks == 1 && !e.alive));
  }
  @Test void unloadedAndDeadEntitiesAreDiscardedAndTimeBudgetYields() {
    var budget = new TntBudget<Tnt>();
    var a = new Tnt(); var b = new Tnt(); var c = new Tnt();
    budget.begin(); budget.offer(a); budget.offer(b); budget.offer(c);
    budget.begin(); budget.offer(b); budget.offer(c); c.alive = false;
    AtomicLong clock = new AtomicLong();
    budget.run(128, 4, 1, e -> e.alive, e -> false,
        e -> { e.ticks++; clock.addAndGet(2); }, clock::get);
    assertEquals(0, a.ticks); assertEquals(1, b.ticks); assertEquals(0, c.ticks);
    budget.clear(); assertEquals(0, budget.size());
  }
  @Test void everyLiveTntMakesProgressUnderTickBudget() {
    var budget = new TntBudget<Tnt>();
    var entities = new ArrayList<Tnt>();
    for (int i = 0; i < 1000; i++) { var t = new Tnt(); t.fuse = 80; entities.add(t); }
    for (int tick = 0; tick < 16; tick++) {
      budget.begin(); entities.forEach(budget::offer);
      budget.run(128, 4, 8_000_000, e -> e.alive, e -> e.fuse == 0,
          e -> { e.ticks++; e.fuse--; }, () -> 0);
    }
    assertTrue(entities.stream().allMatch(e -> e.ticks >= 2 && e.fuse == 80 - e.ticks));
  }
}
