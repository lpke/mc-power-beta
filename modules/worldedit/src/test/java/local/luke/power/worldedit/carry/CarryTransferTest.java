package local.luke.power.worldedit.carry;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import local.luke.power.worldedit.core.*;
import org.junit.jupiter.api.Test;

class CarryTransferTest {
  private final Pos a = new Pos(15, 64, 0), b = new Pos(16, 64, 0);

  private Map<Pos, BlockValue> chests() {
    Map<Pos, BlockValue> values = new LinkedHashMap<>();
    values.put(a, new BlockValue(54, 2, new byte[] {1, 2, 3}));
    values.put(b, new BlockValue(54, 3, new byte[] {4, 5, 6}));
    return values;
  }

  private Map<Pos, BlockValue> air() {
    return Map.of(a, BlockValue.AIR, b, BlockValue.AIR);
  }

  private static final class Memory implements WorldAccess {
    final Map<Pos, BlockValue> blocks = new HashMap<>();
    int writes, failAt;
    Pos unavailable;

    Memory(Map<Pos, BlockValue> initial, int failAt) {
      blocks.putAll(initial);
      this.failAt = failAt;
    }

    public boolean loaded(Pos pos) {
      return !pos.equals(unavailable);
    }

    public BlockValue get(Pos pos) {
      return blocks.get(pos);
    }

    public void set(Pos pos, BlockValue state) {
      blocks.put(pos, state);
      if (++writes == failAt) throw new IllegalStateException("Interrupted write");
    }
  }

  @Test
  void resumesEveryPartialPickupAndPlacementWithoutDuplicatingOrLosingEitherHalf() {
    for (boolean pickup : new boolean[] {true, false})
      for (int interrupted = 1; interrupted <= 2; interrupted++) {
        var before = pickup ? chests() : air();
        var after = pickup ? air() : chests();
        Memory world = new Memory(before, interrupted);
        assertThrows(IllegalStateException.class, () -> CarryTransfer.apply(world, before, after));
        world.failAt = 0;
        CarryTransfer.apply(world, before, after);
        after.forEach((p, v) -> assertTrue(v.same(world.get(p))));
        int writes = world.writes;
        CarryTransfer.apply(world, before, after);
        assertEquals(writes, world.writes);
      }
  }

  @Test
  void splittingEitherHalfAndReplayingInterruptedWritesNeverTouchesItsNeighbor() {
    for (Pos chosen : List.of(a, b))
      for (boolean pickup : new boolean[] {true, false}) {
        Pos neighbor = chosen.equals(a) ? b : a;
        BlockValue original = chests().get(chosen), untouched = chests().get(neighbor);
        var before = Map.of(chosen, pickup ? original : BlockValue.AIR);
        var after = Map.of(chosen, pickup ? BlockValue.AIR : original);
        Memory world = new Memory(chests(), 1);
        world.blocks.putAll(before);
        assertThrows(IllegalStateException.class, () -> CarryTransfer.apply(world, before, after));
        assertTrue(untouched.same(world.get(neighbor)));
        world.failAt = 0;
        CarryTransfer.apply(world, before, after);
        assertTrue(after.get(chosen).same(world.get(chosen)));
        assertTrue(untouched.same(world.get(neighbor)));
        int writes = world.writes;
        CarryTransfer.apply(world, before, after);
        assertEquals(writes, world.writes);
      }
  }

  @Test
  void mismatchedSecondHalfCannotChangeFirstHalf() {
    Memory world = new Memory(chests(), 0);
    world.blocks.put(b, new BlockValue(54, 3, new byte[] {7}));
    assertThrows(IllegalStateException.class, () -> CarryTransfer.apply(world, chests(), air()));
    assertEquals(0, world.writes);
    assertTrue(chests().get(a).same(world.get(a)));
  }

  @Test
  void unloadedSecondChunkCannotChangeFirstHalf() {
    Memory world = new Memory(chests(), 0);
    world.unavailable = b;
    assertThrows(IllegalStateException.class, () -> CarryTransfer.apply(world, chests(), air()));
    assertEquals(0, world.writes);
  }

  @Test
  void occupiedSecondDestinationCannotPlaceFirstHalf() {
    Memory world = new Memory(air(), 0);
    world.blocks.put(b, new BlockValue(1, 0));
    assertThrows(IllegalStateException.class, () -> CarryTransfer.apply(world, air(), chests()));
    assertEquals(0, world.writes);
  }
}
