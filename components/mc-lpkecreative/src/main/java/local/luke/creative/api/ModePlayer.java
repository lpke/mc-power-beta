package local.luke.creative.api;

/** Local gamemode state. BHCreative's CreativePlayer API remains available alongside this. */
public interface ModePlayer {
  GameMode lpke_mode();

  GameMode lpke_previousMode();

  void lpke_applyMode(GameMode mode);

  default boolean lpke_isSpectator() {
    return lpke_mode() == GameMode.SPECTATOR;
  }

  float lpke_spectatorSpeed();

  void lpke_spectatorSpeed(float speed);
}
