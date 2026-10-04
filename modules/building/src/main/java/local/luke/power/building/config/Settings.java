package local.luke.power.building.config;

public final class Settings {
  public local.luke.power.fastplace.config.Settings placement =
      new local.luke.power.fastplace.config.Settings();
  public local.luke.power.flexible.config.Settings flexible = new local.luke.power.flexible.config.Settings();
  public local.luke.power.fakesneak.config.Settings sneak = new local.luke.power.fakesneak.config.Settings();
  public local.luke.power.slabplacement.config.Settings slabs =
      new local.luke.power.slabplacement.config.Settings();
  public local.luke.power.building.hotbar.HotbarSettings hotbar =
      new local.luke.power.building.hotbar.HotbarSettings();
  public boolean freeLookFollowThirdPerson = true;
  public boolean autoWalkHoldToWalk = true;
  public int autoWalkHoldMillis = 350;
  public boolean autoWalk = true, freeLook = true, freeLookToggle = false;
  public local.luke.power.building.camera.Perspective freeLookPerspective =
      local.luke.power.building.camera.Perspective.FIRST_PERSON;
  public boolean boatSpeed = false, boatProtection = false;
  public boolean boatSteering = true, fastMinecarts = true, clickMining = true;

  public Settings copy() {
    Settings s = new Settings();
    s.placement = placement.copy();
    s.flexible = flexible.copy();
    s.sneak = sneak.copy();
    s.slabs = slabs.copy();
    s.hotbar = hotbar.copy();
    s.freeLookFollowThirdPerson = freeLookFollowThirdPerson;
    s.autoWalk = autoWalk;
    s.autoWalkHoldToWalk = autoWalkHoldToWalk;
    s.autoWalkHoldMillis = autoWalkHoldMillis;
    s.freeLook = freeLook;
    s.freeLookToggle = freeLookToggle;
    s.freeLookPerspective = freeLookPerspective;
    s.boatSteering = boatSteering;
    s.boatSpeed = boatSpeed;
    s.boatProtection = boatProtection;
    s.fastMinecarts = fastMinecarts;
    s.clickMining = clickMining;
    return s;
  }
}
