package local.luke.power.permissions;

/** Saved on world metadata, shared across the two Minecraft mapping sets. */
public interface CheatWorld {
  String TAG = "PowerBetaCheatsEnabled";

  boolean power$cheatsEnabled();

  void power$cheatsEnabled(boolean enabled);
}
