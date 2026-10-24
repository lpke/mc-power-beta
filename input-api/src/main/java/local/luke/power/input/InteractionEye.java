package local.luke.power.input;

import java.util.Objects;

/** Logical eye offset above the native position, shared across Minecraft mappings. */
public final class InteractionEye {
  @FunctionalInterface
  public interface Provider {
    double offset(Object actor, float delta, double nativeFallback);
  }

  private static Provider source = (actor, delta, fallback) -> fallback;
  private InteractionEye() {}

  public static void register(Provider provider) { source = Objects.requireNonNull(provider); }

  public static double offset(Object actor, float delta, double nativeFallback) {
    if (actor == null || !Float.isFinite(delta)) return nativeFallback;
    double offset = source.offset(actor, delta, nativeFallback);
    return Double.isFinite(offset) ? offset : nativeFallback;
  }
}
