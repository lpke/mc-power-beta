package local.luke.worldedit.core;

import java.util.function.Function;

public final class Shapes {
  private Shapes() {}

  public static void fill(
      Editor e, Pos origin, String pattern, double radius, int depth, boolean recursive) {
    Function<Pos, BlockValue> paint = e.blocks.pattern(pattern);
    FillIterator scan =
        new FillIterator(
            origin,
            radius,
            depth,
            recursive,
            Math.max(e.limit * 16, 65536),
            p -> e.engine.read(p).id == 0);
    e.edit(scan, p -> e.engine.read(p).id == 0 ? paint.apply(p) : null);
  }

  public static void generate(
      Editor e,
      Pos origin,
      String kind,
      String pattern,
      double rx,
      double ry,
      double rz,
      int height,
      boolean raised) {
    Function<Pos, BlockValue> paint = e.blocks.pattern(pattern);
    boolean cylinder = kind.endsWith("cyl"), hollow = kind.startsWith("h");
    if (raised && !cylinder) origin = origin.add(0, (int) Math.ceil(ry), 0);
    final Pos center = origin;
    int x = (int) Math.ceil(rx),
        y = cylinder ? height - 1 : (int) Math.ceil(ry),
        z = (int) Math.ceil(rz);
    Region bounds = Region.between(center.add(-x, cylinder ? 0 : -y, -z), center.add(x, y, z));
    e.bounded(bounds);
    e.edit(
        bounds,
        p -> {
          double dx = p.x() - center.x(),
              dy = cylinder ? 0 : p.y() - center.y(),
              dz = p.z() - center.z();
          double rr =
              (dx * dx) / ((rx + .5) * (rx + .5))
                  + (dy * dy) / ((ry + .5) * (ry + .5))
                  + (dz * dz) / ((rz + .5) * (rz + .5));
          if (rr > 1) return null;
          if (hollow) {
            double inner =
                (dx * dx) / (Math.max(.1, rx - .5) * Math.max(.1, rx - .5))
                    + (dy * dy) / (Math.max(.1, ry - .5) * Math.max(.1, ry - .5))
                    + (dz * dz) / (Math.max(.1, rz - .5) * Math.max(.1, rz - .5));
            if (inner < 1) return null;
          }
          return paint.apply(p);
        });
  }

  public static void nearby(
      Editor e, Pos origin, int radius, String from, String to, int up, int down) {
    Region r =
        Region.between(
            origin.add(-radius, -Math.min(down, origin.y()), -radius),
            origin.add(radius, Math.min(up, 127 - origin.y()), radius));
    e.bounded(r);
    var mask = e.blocks.mask(from);
    var paint = e.blocks.pattern(to);
    e.edit(r, p -> mask.test(e.engine.read(p)) ? paint.apply(p) : null);
  }
}
