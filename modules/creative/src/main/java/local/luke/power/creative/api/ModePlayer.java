package local.luke.power.creative.api;

/** Local gamemode state. CreativeInventory's CreativePlayer API remains available alongside this. */
public interface ModePlayer extends local.luke.power.input.PlayerMode {
  default boolean power$creative() { return power_mode() == GameMode.CREATIVE; }
  default boolean power$spectator() { return power_isSpectator(); }
  default boolean power$flying() {
    return local.luke.power.creative.Modes.flying((net.minecraft.entity.living.player.PlayerEntity)this);
  }
  int power_flightOverride();

  void power_flightOverride(int value);

  GameMode power_mode();

  GameMode power_previousMode();

  void power_applyMode(GameMode mode);

  default boolean power_isSpectator() {
    return power_mode() == GameMode.SPECTATOR;
  }

  float power_spectatorSpeed();

  void power_spectatorSpeed(float speed);
}
