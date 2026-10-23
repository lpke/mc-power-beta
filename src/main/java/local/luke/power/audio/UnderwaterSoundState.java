package local.luke.power.audio;

/** Tick timing and strict probability boundaries from the verified Java 26.3 client. */
public final class UnderwaterSoundState {
  public static final String PREFIX = "power_beta:ambient.underwater.";
  private boolean wet;

  /** Spectator still updates its head state, but has no entry/exit sound. */
  public int transition(boolean underwater, boolean spectator) {
    int change = underwater == wet || spectator ? 0 : underwater ? 1 : -1;
    wet = underwater;
    return change;
  }

  public static String addition(float random) {
    return random < .0001f ? PREFIX + "loop.additions.ultra_rare"
        : random < .001f ? PREFIX + "loop.additions.rare"
        : random < .01f ? PREFIX + "loop.additions" : null;
  }

  public static boolean enabled(AudioSettings settings, String id) {
    return switch (id) {
      case PREFIX + "enter" -> settings.underwaterEnterSounds;
      case PREFIX + "exit" -> settings.underwaterExitSounds;
      case PREFIX + "loop" -> settings.underwaterAmbience;
      case PREFIX + "loop.additions" -> settings.underwaterAdditions;
      case PREFIX + "loop.additions.rare" -> settings.underwaterRareAdditions;
      case PREFIX + "loop.additions.ultra_rare" -> settings.underwaterUltraRareAdditions;
      default -> false;
    };
  }

  public static boolean any(AudioSettings s) {
    return s.underwaterEnterSounds || s.underwaterExitSounds || s.underwaterAmbience
        || s.underwaterAdditions || s.underwaterRareAdditions || s.underwaterUltraRareAdditions;
  }

  public static final class Fade {
    private int ticks;
    public boolean stopped() { return ticks < 0; }
    public float tick(boolean underwater) {
      if (!stopped()) ticks = Math.min(40, ticks + (underwater ? 1 : -2));
      return Math.max(0, Math.min(1, ticks / 40f));
    }
  }
}
