package local.luke.power.ui;

/** Only consecutive presses on the same slot qualify; painting cancels the pair. */
public final class SlotDoubleClick {
  private int last = -1;
  private long at;
  public boolean press(int slot, int button, long nanos) {
    boolean twice = button == 0 && slot == last && nanos > at && nanos - at <= 250_000_000L;
    if (button != 0 || twice) cancel(); else { last = slot; at = nanos; }
    return twice;
  }
  public void cancel() { last = -1; at = 0; }
}
