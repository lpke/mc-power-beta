package local.luke.power.input;

import java.util.EnumMap;
import java.util.function.Supplier;
import java.util.function.BooleanSupplier;

/** Read-only live state providers across modules with different Minecraft mappings. */
public final class TweakIndicators {
  public enum Tweak {
    FAKE_SNEAK("Fake sneak"), FAST_PLACEMENT("Fast placement"),
    PLACEMENT_RESTRICTION("Placement restriction"), AUTO_WALK("Auto-walk"),
    AUTO_MINE("Auto-mine"), FREE_LOOK("Free look"), CINEMATIC_CAMERA("Cinematic camera"),
    FREECAM_PLAYER_MOVEMENT("Freecam player movement"), SLAB_COMPLETION("Slab completion");

    public final String label;
    Tweak(String label) { this.label = label; }
  }

  private static final EnumMap<Tweak, Supplier<String>> providers = new EnumMap<>(Tweak.class);
  private static final EnumMap<Tweak, BooleanSupplier> messages = new EnumMap<>(Tweak.class);
  private TweakIndicators() {}
  /** Return null while inactive, an empty string when active, or a short state detail. */
  public static void register(Tweak tweak, Supplier<String> provider) {
    register(tweak, provider, () -> false);
  }
  public static void register(Tweak tweak, Supplier<String> provider, BooleanSupplier announce) {
    providers.put(tweak, java.util.Objects.requireNonNull(provider));
    messages.put(tweak, java.util.Objects.requireNonNull(announce));
  }
  public static boolean announces(Tweak tweak) {
    BooleanSupplier enabled = messages.get(tweak);
    return enabled != null && enabled.getAsBoolean();
  }
  public static String label(Tweak tweak) {
    Supplier<String> provider = providers.get(tweak);
    String detail = provider == null ? null : provider.get();
    return detail == null ? null : tweak.label + (detail.isEmpty() ? "" : ": " + detail);
  }
}
