package local.luke.power.map;

/** Render-thread scratch for one convex quad clipped against at most 48 planes. */
final class MapPolygon {
  private double[] vertices = new double[64 * 4], scratch = new double[64 * 4];
  private int size;

  int size() { return size; }
  double value(int vertex, int component) { return vertices[vertex * 4 + component]; }

  MapPolygon rectangle(double x0, double y0, double x1, double y1) {
    size = 0;
    add(vertices, size++, x0, y0, 0, 0);
    add(vertices, size++, x1, y0, 1, 0);
    add(vertices, size++, x1, y1, 1, 1);
    add(vertices, size++, x0, y1, 0, 1);
    return this;
  }

  MapPolygon project(MapProjection view, double x0, double z0, double x1, double z1,
      double u0, double v0, double u1, double v1) {
    size = 0;
    point(view, x0, z0, u0, v0); point(view, x1, z0, u1, v0);
    point(view, x1, z1, u1, v1); point(view, x0, z1, u0, v1);
    return this;
  }

  private void point(MapProjection view, double x, double z, double u, double v) {
    view.screen(x, z, vertices, size * 4);
    vertices[size * 4 + 2] = u; vertices[size * 4 + 3] = v; size++;
  }

  MapPolygon clip(MapProjection view, boolean circle) {
    boolean inside = true;
    for (int i = 0; i < size; i++)
      if (!view.contains(value(i, 0), value(i, 1), circle)) { inside = false; break; }
    if (inside) return this;
    int sides = circle ? 48 : 4;
    for (int side = 0; side < sides && size > 0; side++) {
      double nx = MapProjection.normalX(circle, side), ny = MapProjection.normalY(circle, side);
      double radius = circle || side % 2 == 0 ? view.width / 2 : view.height / 2;
      int previous = (size - 1) * 4, count = 0;
      double pd = distance(view, previous, nx, ny, radius);
      for (int i = 0; i < size; i++) {
        int next = i * 4;
        double nd = distance(view, next, nx, ny, radius);
        if ((pd <= 0) != (nd <= 0)) {
          double t = pd / (pd - nd);
          for (int component = 0; component < 4; component++)
            scratch[count * 4 + component] = vertices[previous + component]
                + (vertices[next + component] - vertices[previous + component]) * t;
          count++;
        }
        if (nd <= 0) { System.arraycopy(vertices, next, scratch, count * 4, 4); count++; }
        previous = next; pd = nd;
      }
      double[] swap = vertices; vertices = scratch; scratch = swap; size = count;
    }
    return this;
  }

  private double distance(MapProjection view, int offset, double nx, double ny, double radius) {
    return (vertices[offset] - view.left - view.width / 2) * nx
        + (vertices[offset + 1] - view.top - view.height / 2) * ny - radius;
  }

  private static void add(double[] target, int index, double x, double y, double u, double v) {
    int offset = index * 4;
    target[offset] = x; target[offset + 1] = y; target[offset + 2] = u; target[offset + 3] = v;
  }
}
