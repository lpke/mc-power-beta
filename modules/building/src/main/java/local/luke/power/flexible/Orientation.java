package local.luke.power.flexible;

/** Metadata overrides are scoped to one native item call and applied before block updates. */
public final class Orientation {
  public record Request(int block, BlockPos pos, Direction facing, boolean reverse) {}

  private static Request current;

  private Orientation() {}

  public static Request begin(Request value) {
    Request previous = current;
    current = value;
    return previous;
  }

  public static void restore(Request previous) {
    current = previous;
  }

  public static boolean directional(int block) {
    return switch (block) {
      case 23, 29, 33, 53, 61, 62, 67, 86, 91, 93, 94 -> true;
      default -> false;
    };
  }

  public static boolean valid(int block, Direction facing) {
    return facing == null || !directional(block) || block == 29 || block == 33 || facing.y == 0;
  }

  public static int metadata(int block, int x, int y, int z, int original) {
    Request r = current;
    if (r == null
        || r.block != block
        || !r.pos.equals(new BlockPos(x, y, z))
        || !directional(block)) return original;
    Direction d = r.facing;
    if (d == null) {
      d =
          switch (block) {
            case 53, 67 ->
                switch (original & 3) {
                  case 0 -> Direction.EAST;
                  case 1 -> Direction.WEST;
                  case 2 -> Direction.SOUTH;
                  default -> Direction.NORTH;
                };
            case 86, 91, 93, 94 ->
                switch (original & 3) {
                  case 0 -> Direction.SOUTH;
                  case 1 -> Direction.WEST;
                  case 2 -> Direction.NORTH;
                  default -> Direction.EAST;
                };
            default -> Direction.values()[original & 7];
          };
    }
    if (r.reverse) d = d.opposite();
    if (!valid(block, d)) return original;
    return switch (block) {
      case 53, 67 ->
          switch (d) {
            case EAST -> 0;
            case WEST -> 1;
            case SOUTH -> 2;
            default -> 3;
          };
      case 86, 91, 93, 94 ->
          (original & ~3)
              | switch (d) {
                case SOUTH -> 0;
                case WEST -> 1;
                case NORTH -> 2;
                default -> 3;
              };
      default -> (original & ~7) | d.ordinal();
    };
  }
}
