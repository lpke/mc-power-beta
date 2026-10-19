package local.luke.power.gameplay;

/** Renderer-scoped state, also supplied to both native armor models. */
public interface SwimmingModel {
  void power$swimPose(float amount, boolean compact);
}
