package local.luke.flexible;

public record BlockPos(int x, int y, int z) {
  public BlockPos offset(Direction d) {
    return new BlockPos(x + d.x, y + d.y, z + d.z);
  }

  public boolean inBounds() {
    return y >= 0 && y < 128 && Math.abs((long) x) < 32000000 && Math.abs((long) z) < 32000000;
  }
}
