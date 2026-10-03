package local.luke.flexible;

public enum Direction {
  DOWN(0, -1, 0),
  UP(0, 1, 0),
  NORTH(0, 0, -1),
  SOUTH(0, 0, 1),
  WEST(-1, 0, 0),
  EAST(1, 0, 0);
  public final int x, y, z;

  Direction(int x, int y, int z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public Direction opposite() {
    return values()[ordinal() ^ 1];
  }

  public Direction clockwise() {
    return switch (this) {
      case NORTH -> EAST;
      case EAST -> SOUTH;
      case SOUTH -> WEST;
      case WEST -> NORTH;
      default -> this;
    };
  }

  public static Direction horizontal(float yaw) {
    return new Direction[] {SOUTH, WEST, NORTH, EAST}[((int) Math.floor(yaw / 90.0 + 0.5)) & 3];
  }
}
