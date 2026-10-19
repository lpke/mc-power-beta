package local.luke.power.map;

/** Chunk-granularity viewport interest, intersected with the existing loaded sampling radius. */
record MapSamplingFocus(int minX, int maxX, int minZ, int maxZ, int centerX, int centerZ) {
  static MapSamplingFocus of(MapProjection view, int playerX, int playerZ, int radius) {
    if (view == null) return new MapSamplingFocus(playerX - 1, playerX + 1,
        playerZ - 1, playerZ + 1, playerX, playerZ);
    double dx = view.width / (2 * view.scale), dz = view.height / (2 * view.scale);
    return new MapSamplingFocus(Math.max(playerX - radius, chunk(view.x - dx)),
        Math.min(playerX + radius, chunk(view.x + dx)),
        Math.max(playerZ - radius, chunk(view.z - dz)), Math.min(playerZ + radius, chunk(view.z + dz)),
        chunk(view.x), chunk(view.z));
  }
  boolean contains(int x, int z) { return x >= minX && x <= maxX && z >= minZ && z <= maxZ; }
  int tier(int x, int z, int playerX, int playerZ, boolean surveyed) {
    return (surveyed ? 1 : 0) + (contains(x, z) ? 0
        : Math.abs((long)x - playerX) <= 1 && Math.abs((long)z - playerZ) <= 1 ? 2 : 4);
  }
  long distance(int x, int z, int playerX, int playerZ) {
    long dx = (long)x - (contains(x, z) ? centerX : playerX);
    long dz = (long)z - (contains(x, z) ? centerZ : playerZ);
    return dx * dx + dz * dz;
  }
  private static int chunk(double x) { return (int)Math.floor(x) >> 4; }
}
