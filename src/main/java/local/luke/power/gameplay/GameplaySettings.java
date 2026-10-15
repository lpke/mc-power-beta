package local.luke.power.gameplay;

public final class GameplaySettings {
  public boolean mergeItems = true, limitTnt = false, flintIgnitesTnt = false;
  public boolean lavaProofItems = false, cactusProofItems = false;
  public boolean torchShovel = false;
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
