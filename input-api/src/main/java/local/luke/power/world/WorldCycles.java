package local.luke.power.world;

/** Per-world rules and the daylight clock, independent of the simulation clock. */
public interface WorldCycles {
  boolean power$daylightCycle();
  void power$daylightCycle(boolean enabled);
  boolean power$weatherCycle();
  void power$weatherCycle(boolean enabled);
  boolean power$daylightFrozen();
  boolean power$weatherFrozen();
  long power$daylightTime();
  void power$daylightTime(long time);
}
