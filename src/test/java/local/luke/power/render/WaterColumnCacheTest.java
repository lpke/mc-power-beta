package local.luke.power.render;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class WaterColumnCacheTest {
  @Test void collidingSignedCoordinatesRemainDistinctAndInvalidateTogether() {
    var cache = new WaterColumnCache();
    for (int x = -2048; x < 2048; x++) {
      long key = ((long)x << 32) ^ (x & 0xffffffffL);
      byte[] column = cache.prepare(key);
      column[0] = (byte)x; column[127] = (byte)(x >> 8);
      cache.publish(key, column, cache.generation());
    }
    for (int x = -2048; x < 2048; x++) {
      byte[] column = cache.get(((long)x << 32) ^ (x & 0xffffffffL));
      assertEquals((byte)x, column[0]); assertEquals((byte)(x >> 8), column[127]);
    }
    long overflow = 0x123456789abcdefL;
    assertNotNull(cache.prepare(overflow));
    assertNull(cache.get(overflow));
    cache.clear();
    assertNull(cache.get(0));
    assertNull(cache.get(-1));
    byte[] column = cache.prepare(overflow);
    cache.publish(overflow, column, cache.generation());
    assertSame(column, cache.get(overflow));
  }

  @Test void laterFramesReuseStorageButNeverReturnUnpreparedColumns() {
    var cache = new WaterColumnCache();
    byte[] first = cache.prepare(Long.MIN_VALUE);
    first[47] = 99;
    assertNull(cache.get(Long.MIN_VALUE));
    int generation = cache.generation();
    cache.clear();
    assertNull(cache.get(Long.MIN_VALUE));
    cache.publish(Long.MIN_VALUE, first, generation);
    assertNull(cache.get(Long.MIN_VALUE), "invalidated scratch must not become a cached column");
    assertSame(first, cache.prepare(Long.MIN_VALUE));
    assertNotSame(first, cache.prepare(Long.MAX_VALUE));
  }
}
