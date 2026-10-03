package local.luke.flexible;

/** The five regions and face coordinates used by MaLiLib's targeting overlay. */
public final class FaceGrid {
  public enum Part {
    CENTER,
    LEFT,
    RIGHT,
    BOTTOM,
    TOP
  }

  private FaceGrid() {}

  public static Part part(double u, double v) {
    double h = Math.abs(u - .5), w = Math.abs(v - .5);
    if (h <= .25 && w <= .25) return Part.CENTER;
    if (h > w) return u < .5 ? Part.LEFT : Part.RIGHT;
    return v < .5 ? Part.BOTTOM : Part.TOP;
  }

  public static double[] uv(Direction face, Direction forward, double x, double y, double z) {
    double u, v;
    if (face.y != 0) {
      u =
          switch (forward) {
            case NORTH -> x;
            case SOUTH -> 1 - x;
            case WEST -> 1 - z;
            default -> z;
          };
      v =
          switch (forward) {
            case NORTH -> 1 - z;
            case SOUTH -> z;
            case WEST -> 1 - x;
            default -> x;
          };
      if (face == Direction.DOWN) v = 1 - v;
    } else {
      u =
          switch (face) {
            case NORTH -> 1 - x;
            case SOUTH -> x;
            case WEST -> z;
            default -> 1 - z;
          };
      v = y;
    }
    return new double[] {u, v};
  }

  public static Direction selected(Direction face, Direction forward, Part part) {
    if (part == Part.CENTER) return face;
    if (face.y != 0)
      return switch (part) {
        case LEFT -> forward.clockwise().opposite();
        case RIGHT -> forward.clockwise();
        case TOP -> face == Direction.UP ? forward : forward.opposite();
        case BOTTOM -> face == Direction.UP ? forward.opposite() : forward;
        default -> face;
      };
    return switch (part) {
      case LEFT -> face.clockwise();
      case RIGHT -> face.clockwise().opposite();
      case TOP -> Direction.UP;
      case BOTTOM -> Direction.DOWN;
      default -> face;
    };
  }

  /** Inverse of uv; the normal coordinate lies on the clicked surface. */
  public static double[] point(
      Direction face, Direction forward, double u, double v, double plane) {
    if (face.y != 0) {
      if (face == Direction.DOWN) v = 1 - v;
      return switch (forward) {
        case NORTH -> new double[] {u, plane, 1 - v};
        case SOUTH -> new double[] {1 - u, plane, v};
        case WEST -> new double[] {1 - v, plane, 1 - u};
        default -> new double[] {v, plane, u};
      };
    }
    return switch (face) {
      case NORTH -> new double[] {1 - u, v, plane};
      case SOUTH -> new double[] {u, v, plane};
      case WEST -> new double[] {plane, v, u};
      default -> new double[] {plane, v, 1 - u};
    };
  }
}
