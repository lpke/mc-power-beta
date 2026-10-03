package local.luke.tweaks.config;

public final class Settings {
  public local.luke.fastplace.config.Settings placement =
      new local.luke.fastplace.config.Settings();
  public local.luke.flexible.config.Settings flexible = new local.luke.flexible.config.Settings();
  public local.luke.fakesneak.config.Settings sneak = new local.luke.fakesneak.config.Settings();
  public local.luke.slabplacement.config.Settings slabs =
      new local.luke.slabplacement.config.Settings();
  public local.luke.tweaks.hotbar.HotbarSettings hotbar =
      new local.luke.tweaks.hotbar.HotbarSettings();
  public boolean freeLookFollowThirdPerson = true;
  public boolean autoWalk = true, freeLook = true, freeLookToggle = false;
  public local.luke.tweaks.camera.Perspective freeLookPerspective =
      local.luke.tweaks.camera.Perspective.FIRST_PERSON;
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
    s.freeLook = freeLook;
    s.freeLookToggle = freeLookToggle;
    s.freeLookPerspective = freeLookPerspective;
    s.boatSteering = boatSteering;
    s.fastMinecarts = fastMinecarts;
    s.clickMining = clickMining;
    return s;
  }
}
