package local.luke.power.input;

/** Mode queries shared by modules using different game mappings. */
public interface PlayerMode {
  boolean power$creative();
  boolean power$spectator();
  default boolean power$flying() { return false; }
}
