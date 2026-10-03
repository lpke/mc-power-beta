package local.luke.power.worldedit.core;

import java.util.Arrays;

public final class BlockValue {
  public static final BlockValue AIR = new BlockValue(0, 0, null);
  public final int id, meta;
  private final byte[] nbt;

  public BlockValue(int id, int meta, byte[] nbt) {
    this.id = id;
    this.meta = meta;
    this.nbt = nbt == null ? null : nbt.clone();
  }

  public BlockValue(int id, int meta) {
    this(id, meta, null);
  }

  public byte[] nbt() {
    return nbt == null ? null : nbt.clone();
  }

  public int bytes() {
    return 48 + (nbt == null ? 0 : nbt.length);
  }

  public BlockValue withMeta(int value) {
    return new BlockValue(id, value, nbt);
  }

  public boolean same(BlockValue b) {
    return id == b.id && meta == b.meta && Arrays.equals(nbt, b.nbt);
  }

  public boolean satisfiedBy(BlockValue existing) {
    return id == existing.id
        && meta == existing.meta
        && (nbt == null || Arrays.equals(nbt, existing.nbt));
  }
}
