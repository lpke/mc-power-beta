package local.luke.creative.config;

public final class Settings {
  public boolean flight = true, landStopsFlight = true, sprintFlight = true, sprintToggle = true;
  public int flightSpeed = 100, sprintMultiplier = 200, glide = 5, doubleTapTicks = 7;
  public boolean modePicker = true, spectatorScroll = true, showSpectatorSpeed = true;
  public int spectatorSpeed = 100, spectatorScrollStep = 10;
  public int blockReach = 50, entityReach = 50;
  public boolean destroySlot = true, shiftClearsInventory = true;

  public Settings copy() {
    Settings s = new Settings();
    try {
      for (var f : Settings.class.getFields()) f.set(s, f.get(this));
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
    return s;
  }

  public void validate() {
    if (flightSpeed < 25
        || flightSpeed > 400
        || sprintMultiplier < 100
        || sprintMultiplier > 400
        || glide < 0
        || glide > 5
        || doubleTapTicks < 2
        || doubleTapTicks > 20
        || spectatorScrollStep < 1
        || spectatorScrollStep > 100
        || spectatorSpeed < 0
        || spectatorSpeed > 400
        || blockReach < 30
        || blockReach > 100
        || entityReach < 30
        || entityReach > 100)
      throw new IllegalArgumentException("Settings are outside the supported range.");
  }
}
