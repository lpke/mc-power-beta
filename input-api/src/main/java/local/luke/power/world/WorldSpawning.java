package local.luke.power.world;

/** Saved natural-spawning rules. Existing mobs are unaffected. */
public interface WorldSpawning {
  boolean power$hostileSpawning();
  void power$hostileSpawning(boolean enabled);
  boolean power$passiveSpawning();
  void power$passiveSpawning(boolean enabled);
}
