package local.luke.power.building.camera;

/** Render-only offsets: both ends of Minecraft's interpolation get the same offset. */
public final class LookAngles {
  private float yaw, pitch;

  public void rotate(float bodyPitch, float dx, float dy) {
    if (!Float.isFinite(dx) || !Float.isFinite(dy) || !Float.isFinite(bodyPitch)) return;
    yaw = (yaw + dx * .15F) % 360F;
    pitch = clamp(bodyPitch + pitch - dy * .15F) - bodyPitch;
  }

  public float yaw(float original) {
    return original + yaw;
  }

  public float pitch(float original) {
    return clamp(original + pitch);
  }

  public void clear() {
    yaw = pitch = 0;
  }

  private static float clamp(float value) {
    return Math.max(-90F, Math.min(90F, value));
  }
}
