package local.luke.power.visual;

public final class VisualSettings {
  public boolean hideEntities;
  public boolean hideItems = true;
  public boolean hideMobs, hidePlayers, hideVehicles, hideProjectiles, hideTnt, hideOther;
  public boolean entityRenderingMessages, fullBrightMessages, spectatorFullBright;
  public boolean removeUnderwaterOverlay;
  public int underwaterFogDensity = 100;
  public int underwaterDarkness = 100;
  public int underwaterDarknessOutside = 100;
  public boolean narrowUnderwaterFov = true;
  public boolean softRain, softSnow, oldCobble, oldBricks, redstonePowerLevels, lowFire;
  public boolean containerCarry = true;
  public boolean inventoryInPortals = true;
  public boolean shiftClickIntoCraftingGrid = true;
  public boolean damageCameraShake = true, fireDamageCameraShake = true;
  public boolean swapEquipment = true;
  public boolean limitCapeRotation = true;
  public double thirdPersonDistance = 4;

  public void validate() {
    if (underwaterDarknessOutside < 0 || underwaterDarknessOutside > 100)
      throw new IllegalArgumentException("Water darkness from outside must be between 0 and 100 percent");
    if (underwaterDarkness < 0 || underwaterDarkness > 100)
      throw new IllegalArgumentException("Underwater darkness must be between 0 and 100 percent");
    if (underwaterFogDensity < 0 || underwaterFogDensity > 100)
      throw new IllegalArgumentException("Underwater fog density must be between 0 and 100 percent");
    if (!Double.isFinite(thirdPersonDistance) || thirdPersonDistance < 1 || thirdPersonDistance > 12)
      throw new IllegalArgumentException("Camera distance must be between 1 and 12 blocks");
  }
}
