package local.luke.power.ui;

/** Match the Options tooltip delay without depending on render frequency. */
final class HoverDelay {
  private String key = "";
  private int ticks;
  void tick() { if (ticks < 13) ticks++; }
  boolean ready(String next) {
    if (!key.equals(next)) { key = next; ticks = 0; }
    return !key.isEmpty() && ticks > 12;
  }
  void clear() { key = ""; ticks = 0; }
}
