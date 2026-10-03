package local.luke.power.fastplace;

/** One held click; deliberately stores no world, entity or inventory references. */
public final class PlacementSession {
  public final BlockPos first;
  public final int face, itemId, damage, slot;
  public final float yaw;
  public final boolean firstWasMerge;
  private BlockPos last;

  public PlacementSession(BlockPos first, int face, float yaw, int itemId, int damage, int slot) {
    this(first, face, yaw, itemId, damage, slot, false);
  }

  public PlacementSession(
      BlockPos first,
      int face,
      float yaw,
      int itemId,
      int damage,
      int slot,
      boolean firstWasMerge) {
    this.firstWasMerge = firstWasMerge;
    this.first = this.last = first;
    this.face = face;
    this.yaw = yaw;
    this.itemId = itemId;
    this.damage = damage;
    this.slot = slot;
  }

  public boolean allows(BlockPos position, int side, boolean restricted, RestrictionMode mode) {
    return allows(position, side, restricted, mode, false);
  }

  public boolean allows(
      BlockPos position,
      int side,
      boolean restricted,
      RestrictionMode mode,
      boolean completingSlab) {
    return (completingSlab || !position.equals(last))
        && (!restricted || mode.allows(first, face, position, side));
  }

  public void placed(BlockPos position) {
    last = position;
  }
}
