package local.luke.power.map;

/** Physical-pixel detail and bounded rectangular storage, independent of the CPU tile cache. */
public record MapSurfacePlan(int width, int height, int level, double scale) {
  public static final long MAX_PAIR_BYTES = 128L * 1024 * 1024;
  public static MapSurfacePlan create(MapProjection view, double pixelsPerUnit, int maximum,
      boolean automaticDetail) {
    if (!Double.isFinite(pixelsPerUnit) || pixelsPerUnit <= 0) return null;
    int width = (int) Math.ceil(view.width * pixelsPerUnit) + 128;
    int height = (int) Math.ceil(view.height * pixelsPerUnit) + 128;
    if (width < 1 || height < 1 || width > maximum || height > maximum
        || (long) width * height * 8 > MAX_PAIR_BYTES) return null;
    double scale = view.scale * pixelsPerUnit;
    return new MapSurfacePlan(width, height, automaticDetail ? MapMath.level(scale) : 0, scale);
  }
}
