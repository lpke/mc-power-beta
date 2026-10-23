package local.luke.power.validation;

import local.luke.power.render.*;

import java.util.Arrays;

/** Bounded reusable columns for one render pass. Primitive keys avoid diagonal hash collisions. */
final class LegacyWaterColumnCache430 {
  private static final int LIMIT = 4096, MASK = LIMIT * 2 - 1;
  private final long[] keys = new long[MASK + 1];
  private final int[] stamps = new int[MASK + 1];
  private final byte[][] values = new byte[MASK + 1][];
  private int stamp = 1, size;

  void clear() {
    size = 0;
    if (++stamp == 0) { Arrays.fill(stamps, 0); stamp = 1; }
  }

  byte[] get(long key) {
    int slot = slot(key);
    return stamps[slot] == stamp ? values[slot] : null;
  }

  int generation() { return stamp; }

  /** Borrow scratch space without exposing an incomplete column to a nested read. */
  byte[] prepare(long key) {
    int slot = slot(key);
    if (stamps[slot] == stamp) return values[slot];
    if (size == LIMIT) return new byte[128];
    if (values[slot] == null) values[slot] = new byte[128];
    return values[slot];
  }

  void publish(long key, byte[] column, int generation) {
    if (stamp != generation || size == LIMIT) return;
    int slot = slot(key);
    if (stamps[slot] != stamp) size++;
    keys[slot] = key;
    stamps[slot] = stamp;
    values[slot] = column;
  }

  private int slot(long key) {
    long hash = key;
    hash = (hash ^ (hash >>> 33)) * 0xff51afd7ed558ccdL;
    hash = (hash ^ (hash >>> 33)) * 0xc4ceb9fe1a85ec53L;
    int slot = (int)(hash ^ (hash >>> 33)) & MASK;
    while (stamps[slot] == stamp && keys[slot] != key) slot = (slot + 1) & MASK;
    return slot;
  }
}
