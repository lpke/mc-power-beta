package local.luke.power.gameplay;

/** Java 1.21.1 HumanoidModel's 26-unit stroke cycle, in radians. */
public final class SwimAnimation {
  private SwimAnimation() {}

  public static float armPitch(float phase) {
    phase %= 26;
    if (phase < 14) return 0;
    if (phase < 22) return (float)Math.PI / 2 * ((phase - 14) / 8);
    return (float)Math.PI / 2 * (1 - (phase - 22) / 4);
  }

  public static float leftArmRoll(float phase) {
    phase %= 26;
    if (phase < 14) return (float)Math.PI + 1.8707964f * (-65 * phase + phase * phase) / -714;
    if (phase < 22) return 5.012389f - 1.8707964f * ((phase - 14) / 8);
    return (float)Math.PI;
  }

  public static float mix(float amount,float from,float to) { return from + amount * (to - from); }

  public static float angle(float amount,float from,float to) {
    float delta=(to-from)%(2*(float)Math.PI);
    if(delta < -(float)Math.PI)delta += 2*(float)Math.PI;
    if(delta >= (float)Math.PI)delta -= 2*(float)Math.PI;
    return from+amount*delta;
  }
}
