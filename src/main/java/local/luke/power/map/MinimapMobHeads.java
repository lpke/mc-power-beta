package local.luke.power.map;

/** World-visit state only. Settings snapshots never contain this latch or held input. */
final class MinimapMobHeads {
  private boolean toggled, held, physicalDown;

  void toggle() { toggled = !toggled; }

  void press() {
    toggled = false;
    held = physicalDown = true;
  }

  void sample(boolean down, boolean allowed) {
    if (down && !physicalDown && allowed) press();
    physicalDown = down;
    held = down && allowed;
  }

  boolean held() { return held; }
  boolean toggled() { return toggled; }
  boolean shown() { return toggled || held; }

  void clear() { toggled = held = physicalDown = false; }
}
