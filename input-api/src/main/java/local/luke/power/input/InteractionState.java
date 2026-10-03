package local.luke.power.input;

/** Single-player transfers temporarily reserve the normal use/mine actions. */
public final class InteractionState {
  public static boolean carryingContainer, containerCarryEnabled;
  private InteractionState() {}
}
