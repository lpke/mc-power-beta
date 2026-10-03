package local.luke.worldedit.core;

public record Pos(int x, int y, int z) {
  public Pos add(int dx, int dy, int dz) {
    return new Pos(Math.addExact(x, dx), Math.addExact(y, dy), Math.addExact(z, dz));
  }

  public Pos add(Pos d) {
    return add(d.x, d.y, d.z);
  }

  public Pos subtract(Pos d) {
    return add(-d.x, -d.y, -d.z);
  }

  public boolean valid() {
    return x > -32000000 && x < 32000000 && z > -32000000 && z < 32000000 && y >= 0 && y < 128;
  }

  @Override
  public String toString() {
    return x + "," + y + "," + z;
  }
}
