package local.luke.power.input;

import java.util.function.Predicate;

/** Cross-mapping bridge for movement in explicitly supported inventory screens. */
public final class MovementScreens {
  private static Predicate<Object> policy = screen -> false;
  private MovementScreens() {}
  public static void register(Predicate<Object> value) { policy = value; }
  public static boolean allows(Object screen) { return screen != null && policy.test(screen); }

  public static boolean keyboardDown(Object binding) {
    return binding instanceof Binding b && movementKey(b.power$code()) && Bindings.down(binding);
  }

  // Mouse clicks and Shift belong to the inventory, even if rebound to movement.
  public static boolean movementKey(int code) { return code > 0 && code != 42 && code != 54; }
}
