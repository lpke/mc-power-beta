package local.luke.power.world;

/** Saved per-world preferences. The cheat gate is applied only when spawning. */
public interface WorldSpawning {
  boolean power$hostileSpawning();
  void power$hostileSpawning(boolean enabled);
  boolean power$passiveSpawning();
  void power$passiveSpawning(boolean enabled);
}
