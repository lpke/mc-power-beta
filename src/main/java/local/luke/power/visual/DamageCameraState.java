package local.luke.power.visual;

/** Transient damage attribution; never changes health or persisted entity data. */
public interface DamageCameraState {
  boolean power$isFireHurt();
  void power$recordHurt();
}
