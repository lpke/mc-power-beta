package local.luke.power.inventory.tools;

import java.util.function.BiConsumer;

/** Selection clicks leave protection unchanged. Painting starts on leaving the pressed cell. */
public final class SortingProtectionDrag {
  private int start = -1, button = -1;
  private boolean active, protect;
  public void begin(int slot, int button, boolean protectedSlot) {
    this.start = slot; this.button = button; protect = !protectedSlot; active = false;
  }
  public void move(int slot, BiConsumer<Integer, Boolean> paint) {
    if (start < 0) return;
    if (!active && slot != start) { active = true; paint.accept(start, protect); }
    if (active && slot >= 9 && slot < 36) paint.accept(slot, protect);
  }
  public void release(int slot, int button, BiConsumer<Integer, Boolean> paint) {
    if (this.button != button || start < 0) return;
    move(slot, paint);
    if (!active && button == 1) paint.accept(start, protect);
    cancel();
  }
  public int button() { return button; }
  public void cancel() { start = -1; button = -1; active = false; }
}
