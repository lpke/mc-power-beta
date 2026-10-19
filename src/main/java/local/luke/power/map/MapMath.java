package local.luke.power.map;

/** All map coordinates use floor division, including west/north of the origin. */
public final class MapMath {
  public static final int SIZE = 64, PIXELS = SIZE * SIZE, MAX_LEVEL = 14;
  public static final int BORDER = 32000000;

  private MapMath() {}

  public record Key(int dimension, int layer, int level, int x, int z) {
    public Key {
      if (dimension < -1024
          || dimension > 1024
          || layer < -1
          || layer > 15
          || level < 0
          || level > MAX_LEVEL
          || Math.abs((long) x) > BORDER / SIZE + 1
          || Math.abs((long) z) > BORDER / SIZE + 1)
        throw new IllegalArgumentException("Invalid map tile");
    }

    public int span() {
      return SIZE << level;
    }

    public long minX() {
      return (long) x * span();
    }

    public long minZ() {
      return (long) z * span();
    }

    public Key parent() {
      return new Key(dimension, layer, level + 1, Math.floorDiv(x, 2), Math.floorDiv(z, 2));
    }

    public String file() {
      return "d" + dimension + "/l" + layer + "/z" + level + "/" + x + "." + z + ".pbmap";
    }
  }

  public static Key key(int dimension, int layer, int level, int x, int z) {
    int span = SIZE << level;
    return new Key(dimension, layer, level, Math.floorDiv(x, span), Math.floorDiv(z, span));
  }

  public static int index(int x, int z) {
    return Math.floorMod(z, SIZE) * SIZE + Math.floorMod(x, SIZE);
  }

  public static int level(double pixelsPerBlock) {
    if (!Double.isFinite(pixelsPerBlock) || pixelsPerBlock <= 0)
      throw new IllegalArgumentException("Invalid map scale");
    return Math.min(
        MAX_LEVEL, Math.max(0, (int) Math.floor(Math.log(1 / pixelsPerBlock) / Math.log(2))));
  }

  public static double bounded(double value, double min, double max) {
    if (!Double.isFinite(value))
      throw new IllegalArgumentException("A finite coordinate is required");
    return Math.max(min, Math.min(max, value));
  }

  public static boolean slime(long seed, int x, int z) {
    // Beta's chunk RNG deliberately uses overflowing int products before widening.
    return new java.util.Random(
                seed
                        + (long) (x * x * 4987142)
                        + (long) (x * 5947611)
                        + (long) (z * z) * 4392871L
                        + (long) (z * 389711)
                    ^ 987234911L)
            .nextInt(10)
        == 0;
  }

  public static int shade(int rgb, double factor) {
    return Math.min(255, Math.max(0, (int) ((rgb >> 16 & 255) * factor))) << 16
        | Math.min(255, Math.max(0, (int) ((rgb >> 8 & 255) * factor))) << 8
        | Math.min(255, Math.max(0, (int) ((rgb & 255) * factor)));
  }

  public static int mix(int a, int b, double part) {
    double p = Math.max(0, Math.min(1, part));
    return (int) ((a >> 16 & 255) * (1 - p) + (b >> 16 & 255) * p) << 16
        | (int) ((a >> 8 & 255) * (1 - p) + (b >> 8 & 255) * p) << 8
        | (int) ((a & 255) * (1 - p) + (b & 255) * p);
  }

  /** Cursor-centred zoom keeps the block beneath the pointer fixed. */
  public static final class View {
    public double x, z, scale = 1;

    public View(double x, double z) {
      this.x = x;
      this.z = z;
    }

    public double worldX(double screen, double centre) {
      return x + (screen - centre) / scale;
    }

    public double worldZ(double screen, double centre) {
      return z + (screen - centre) / scale;
    }

    public void zoom(double multiplier, double dx, double dz) {
      double next = bounded(scale * multiplier, 1.0 / 128, 16);
      x = bounded(x + dx / scale - dx / next, -BORDER, BORDER);
      z = bounded(z + dz / scale - dz / next, -BORDER, BORDER);
      scale = next;
    }

    public void pan(double dx, double dz) {
      x = bounded(x - dx / scale, -BORDER, BORDER);
      z = bounded(z - dz / scale, -BORDER, BORDER);
    }
  }
}
