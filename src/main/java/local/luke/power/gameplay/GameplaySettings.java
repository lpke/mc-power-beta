package local.luke.power.gameplay;

public final class GameplaySettings {
  public boolean mergeItems, explosionProofItems, limitTnt, flintIgnitesTnt;
  public boolean lavaProofItems;
  public int mergeThreshold = 256;
  public int tntPerTick = 4;
  public int tntMillis = 8;
  public boolean leavesDropSaplingsInCreative = true;
  public boolean fastItemCollisions = true;

  public void validate() {
    if (mergeThreshold < 16 || mergeThreshold > 16384 || tntPerTick < 1 || tntPerTick > 64
        || tntMillis < 1 || tntMillis > 50) throw new IllegalArgumentException("Invalid entity processing limit");
  }
}
