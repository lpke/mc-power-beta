package local.luke.power.commands.api;

/** Stored in player NBT; disabling commands never clears it. Sleeping sets a normal bed spawn. */
public interface ForcedSpawn {
  boolean power$forcedSpawn();

  float power$spawnAngle();

  void power$forcedSpawn(boolean enabled, float angle);
}
