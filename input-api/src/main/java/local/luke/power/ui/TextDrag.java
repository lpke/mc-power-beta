package local.luke.power.ui;

import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

/** One pointer selection owns its press through release, even outside the field. */
public final class TextDrag {
  private static Object screen, owner;
  private static BooleanSupplier eligible;
  private static IntConsumer move;
  private TextDrag() {}
  public static Object screen() { return screen; }
  public static void context(Object current, boolean focused) {
    screen = current;
    if (!focused || owner != current || eligible != null && !eligible.getAsBoolean()) cancel();
  }
  public static void begin(Object current, BooleanSupplier active, IntConsumer selection) {
    owner = current; eligible = active; move = selection;
  }
  public static void event(Object current, int x, int button, boolean pressed, boolean focused) {
    context(current, focused);
    if (move == null) return;
    if (button == 0 && pressed) return;
    move.accept(x);
    if (button == 0) cancel();
  }
  public static void cancel() { owner = null; eligible = null; move = null; }
}
