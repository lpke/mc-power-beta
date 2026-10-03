package local.luke.fastplace;

public record BlockPos(int x, int y, int z) {
  public BlockPos offset(int face) {
    return switch (face) {
      case 0 -> new BlockPos(x, y - 1, z);
      case 1 -> new BlockPos(x, y + 1, z);
      case 2 -> new BlockPos(x, y, z - 1);
      case 3 -> new BlockPos(x, y, z + 1);
      case 4 -> new BlockPos(x - 1, y, z);
      case 5 -> new BlockPos(x + 1, y, z);
      default -> throw new IllegalArgumentException("Invalid block face");
    };
  }
}
