package local.luke.power.map;

/** Clip original line segments without adding artificial edges along the map frame. */
final class MapLine {
  private MapLine() {}

  static boolean clip(MapProjection view, boolean circle, double x1, double y1,
      double x2, double y2, double[] output) {
    double cx = view.left + view.width / 2, cy = view.top + view.height / 2;
    double ax = (x1 - cx) / (view.width / 2), ay = (y1 - cy) / (view.height / 2);
    double dx = (x2 - x1) / (view.width / 2), dy = (y2 - y1) / (view.height / 2);
    double low = 0, high = 1;
    if (circle) {
      double a = dx * dx + dy * dy, b = ax * dx + ay * dy, c = ax * ax + ay * ay - 1;
      if (a == 0) { if (c > 0) return false; }
      else {
        double discriminant = b * b - a * c;
        if (discriminant < 0) return false;
        double root = Math.sqrt(discriminant);
        low = Math.max(0, (-b - root) / a);
        high = Math.min(1, (-b + root) / a);
      }
    } else {
      for (int side = 0; side < 4; side++) {
        double nx = MapProjection.normalX(false, side), ny = MapProjection.normalY(false, side);
        double distance = ax * nx + ay * ny - 1, direction = dx * nx + dy * ny;
        if (Math.abs(direction) < 1e-12) { if (distance > 1e-10) return false; }
        else if (direction < 0) low = Math.max(low, -distance / direction);
        else high = Math.min(high, -distance / direction);
      }
    }
    if (low > high) return false;
    output[0] = x1 + (x2 - x1) * low; output[1] = y1 + (y2 - y1) * low;
    output[2] = x1 + (x2 - x1) * high; output[3] = y1 + (y2 - y1) * high;
    return true;
  }
}
