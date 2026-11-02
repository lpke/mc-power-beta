package local.luke.power.worldedit.core;

import java.util.Arrays;

public final class BlockValue {
  public static final BlockValue AIR = new BlockValue(0, 0, null);
  public final int id, meta;
  private final byte[] nbt;
  public final String spawnerMob;

  public BlockValue(int id, int meta, byte[] nbt) {
    this(id,meta,nbt,null);
  }

  public BlockValue(int id, int meta, byte[] nbt, String spawnerMob) {
    if(spawnerMob!=null && id!=52)throw new IllegalArgumentException("Only spawners accept a mob.");
    this.id = id;
    this.meta = meta;
    this.nbt = nbt == null ? null : nbt.clone();
    this.spawnerMob=spawnerMob;
  }

  public BlockValue(int id, int meta) {
    this(id, meta, null);
  }

  public byte[] nbt() {
    return nbt == null ? null : nbt.clone();
  }

  public int bytes() {
    return 48 + (nbt == null ? 0 : nbt.length) + (spawnerMob==null?0:spawnerMob.length()*2);
  }

  public BlockValue withMeta(int value) {
    return new BlockValue(id, value, nbt, spawnerMob);
  }

  public boolean same(BlockValue b) {
    return id == b.id && meta == b.meta && Arrays.equals(nbt, b.nbt)
        && (nbt!=null || java.util.Objects.equals(spawnerMob,b.spawnerMob));
  }

  public boolean satisfiedBy(BlockValue existing) {
    return id == existing.id
        && meta == existing.meta
        && (nbt == null || Arrays.equals(nbt, existing.nbt))
        && (spawnerMob==null || spawnerMob.equals(existing.spawnerMob));
  }
}
