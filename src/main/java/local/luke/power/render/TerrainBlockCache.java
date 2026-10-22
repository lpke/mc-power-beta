package local.luke.power.render;

import java.util.Arrays;

/** Reusable IDs for one 16-cube terrain mesh and its one-block sampling border. */
public final class TerrainBlockCache {
  private static final int SIDE = 18;
  private final int[] ids = new int[SIDE * SIDE * SIDE];
  private Object world;
  private Thread thread;
  private int x, y, z, depth;

  public void begin(Object world, int sectionX, int sectionY, int sectionZ) {
    if (depth != 0 && thread != Thread.currentThread()) return;
    if (depth++ != 0) return;
    this.world = world; thread = Thread.currentThread();
    x = sectionX - 1; y = sectionY - 1; z = sectionZ - 1;
    Arrays.fill(ids, -1);
  }

  public void end() {
    if (thread != Thread.currentThread() || depth == 0) return;
    if (--depth == 0) { world = null; thread = null; }
  }

  public int get(Object world, int x, int y, int z) {
    int index = index(world, x, y, z);
    return index < 0 ? -1 : ids[index];
  }

  public void put(Object world, int x, int y, int z, int id) {
    int index = index(world, x, y, z);
    if (index >= 0) ids[index] = id;
  }

  /** A block callback may edit terrain inside a custom renderer. Never cache through it. */
  public void invalidate(Object world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    if (this.world != world || depth == 0 || thread != Thread.currentThread()
        || maxX < x || maxY < y || maxZ < z || minX >= x + SIDE || minY >= y + SIDE || minZ >= z + SIDE) return;
    Arrays.fill(ids, -1);
  }

  private int index(Object world, int x, int y, int z) {
    if (depth != 1 || this.world != world || thread != Thread.currentThread()) return -1;
    int dx = x - this.x, dy = y - this.y, dz = z - this.z;
    if (dx < 0 || dx >= SIDE || dy < 0 || dy >= SIDE || dz < 0 || dz >= SIDE) return -1;
    return (dy * SIDE + dz) * SIDE + dx;
  }
}
