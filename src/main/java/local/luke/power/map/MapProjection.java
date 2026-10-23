package local.luke.power.map;

import java.util.*;

/** Shared screen/map transform and convex clipping, including rotated square maps. */
public final class MapProjection {
  public record Point(double x, double y, double u, double v) {}

  private static final double[] CIRCLE_X = new double[48], CIRCLE_Y = new double[48];
  private static final double[] SQUARE_X = new double[4], SQUARE_Y = new double[4];
  static {
    for (int side = 0; side < 48; side++) {
      double angle = side * 2 * Math.PI / 48;
      CIRCLE_X[side] = Math.cos(angle); CIRCLE_Y[side] = Math.sin(angle);
    }
    for (int side = 0; side < 4; side++) {
      double angle = side * Math.PI / 2;
      SQUARE_X[side] = Math.cos(angle); SQUARE_Y[side] = Math.sin(angle);
    }
  }

  public final double x, z, scale, left, top, width, height, angle;
  private final double cos, sin;

  public MapProjection(
      double x,
      double z,
      double scale,
      double left,
      double top,
      double width,
      double height,
      double angle) {
    this.x = x;
    this.z = z;
    this.scale = scale;
    this.left = left;
    this.top = top;
    this.width = width;
    this.height = height;
    this.angle = angle;
    cos = Math.cos(angle);
    sin = Math.sin(angle);
  }

  public Point screen(double wx, double wz) {
    double dx = (wx - x) * scale, dz = (wz - z) * scale;
    return new Point(
        left + width / 2 + dx * cos - dz * sin, top + height / 2 + dx * sin + dz * cos, 0, 0);
  }

  void screen(double wx, double wz, double[] target, int offset) {
    double dx = (wx - x) * scale, dz = (wz - z) * scale;
    target[offset] = left + width / 2 + dx * cos - dz * sin;
    target[offset + 1] = top + height / 2 + dx * sin + dz * cos;
  }

  static double normalX(boolean circle, int side) { return circle ? CIRCLE_X[side] : SQUARE_X[side]; }
  static double normalY(boolean circle, int side) { return circle ? CIRCLE_Y[side] : SQUARE_Y[side]; }

  public Point world(double sx, double sy) {
    double dx = (sx - left - width / 2) / scale, dz = (sy - top - height / 2) / scale;
    return new Point(x + dx * cos + dz * sin, z - dx * sin + dz * cos, 0, 0);
  }

  public boolean contains(double sx, double sy, boolean circle) {
    return contains(sx, sy, circle, 0);
  }

  public boolean contains(double sx, double sy, boolean circle, double inset) {
    if (sx < left + inset
        || sx > left + width - inset
        || sy < top + inset
        || sy > top + height - inset) return false;
    double dx = (sx - left - width / 2) / (width / 2 - inset),
        dy = (sy - top - height / 2) / (height / 2 - inset);
    return !circle || dx * dx + dy * dy <= 1;
  }

  /** A waypoint beyond the map remains visible at its bearing along the inside edge. */
  public Point edge(Point point, boolean circle, double inset) {
    double cx = left + width / 2, cy = top + height / 2, dx = point.x - cx, dy = point.y - cy;
    double rx = Math.max(1, width / 2 - inset), ry = Math.max(1, height / 2 - inset);
    double extent =
        circle ? Math.hypot(dx / rx, dy / ry) : Math.max(Math.abs(dx) / rx, Math.abs(dy) / ry);
    double factor = extent > 1 ? 1 / extent : 1;
    return new Point(cx + dx * factor, cy + dy * factor, point.u, point.v);
  }

  public List<Point> clip(List<Point> polygon, boolean circle) {
    if (polygon.stream().allMatch(p -> contains(p.x, p.y, circle))) return polygon;
    int sides = circle ? 48 : 4;
    var first = new ArrayList<Point>(polygon.size() + sides);
    var second = new ArrayList<Point>(polygon.size() + sides);
    for (int side = 0; side < sides && !polygon.isEmpty(); side++) {
      double nx = circle ? CIRCLE_X[side] : SQUARE_X[side];
      double ny = circle ? CIRCLE_Y[side] : SQUARE_Y[side];
      double radius = circle || side % 2 == 0 ? width / 2 : height / 2;
      var output = polygon == first ? second : first;
      output.clear();
      Point previous = polygon.get(polygon.size() - 1);
      double pd = distance(previous, nx, ny, radius);
      for (Point next : polygon) {
        double nd = distance(next, nx, ny, radius);
        if ((pd <= 0) != (nd <= 0)) {
          double t = pd / (pd - nd);
          output.add(
              new Point(
                  previous.x + (next.x - previous.x) * t,
                  previous.y + (next.y - previous.y) * t,
                  previous.u + (next.u - previous.u) * t,
                  previous.v + (next.v - previous.v) * t));
        }
        if (nd <= 0) output.add(next);
        previous = next;
        pd = nd;
      }
      polygon = output;
    }
    return polygon;
  }

  private double distance(Point p, double nx, double ny, double radius) {
    return (p.x - left - width / 2) * nx + (p.y - top - height / 2) * ny - radius;
  }
}
