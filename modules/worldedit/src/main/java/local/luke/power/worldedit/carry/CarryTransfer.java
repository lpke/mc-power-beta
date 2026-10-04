package local.luke.power.worldedit.carry;

import java.util.Map;
import local.luke.power.worldedit.core.*;

/** Replay a journaled transfer without overwriting unrelated blocks or inventories. */
public final class CarryTransfer {
  private CarryTransfer() {}

  public static void apply(
      WorldAccess world, Map<Pos, BlockValue> before, Map<Pos, BlockValue> after) {
    if (before.isEmpty() || !before.keySet().equals(after.keySet()))
      throw new IllegalArgumentException("Transfer positions differ");
    // Check all recorded positions before changing any block, including during recovery.
    for (Pos pos : before.keySet()) {
      if (!world.loaded(pos)) throw new IllegalStateException("Container chunk is unavailable");
      BlockValue found = world.get(pos);
      if (!found.same(before.get(pos)) && !found.same(after.get(pos)))
        throw new IllegalStateException("Container location changed; recovery record preserved");
    }
    for (Pos pos : before.keySet()) {
      if (!world.get(pos).same(after.get(pos))) {
        if (!world.get(pos).same(before.get(pos)))
          throw new IllegalStateException("Container changed during transfer");
        world.set(pos, after.get(pos));
      }
    }
    for (Pos pos : after.keySet())
      if (!world.get(pos).same(after.get(pos)))
        throw new IllegalStateException("Container verification failed");
  }
}
