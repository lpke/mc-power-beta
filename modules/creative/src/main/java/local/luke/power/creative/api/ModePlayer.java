package local.luke.power.creative.api;

/** Local gamemode state. CreativeInventory's CreativePlayer API remains available alongside this. */
public interface ModePlayer {
  GameMode power_mode();

  GameMode power_previousMode();

  void power_applyMode(GameMode mode);

  default boolean power_isSpectator() {
    return power_mode() == GameMode.SPECTATOR;
  }

  float power_spectatorSpeed();

  void power_spectatorSpeed(float speed);
}
