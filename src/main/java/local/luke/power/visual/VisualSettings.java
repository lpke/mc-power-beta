package local.luke.power.visual;

public final class VisualSettings {
  public boolean hideEntities;
  public boolean hideItems = true;
  public boolean hideMobs, hidePlayers, hideVehicles, hideProjectiles, hideTnt, hideOther;
  public boolean entityRenderingMessages, fullBrightMessages, spectatorFullBright;
  public boolean spectatorBlockOutlines;
  public boolean removeUnderwaterOverlay;
  public int underwaterFogDensity = 100;
  public boolean softRain, softSnow, oldCobble, oldBricks, redstonePowerLevels, lowFire;
  public boolean slashChat, containerCarry, containerPreview;
  public boolean inventoryInPortals;
  public boolean shiftClickIntoCraftingGrid;
  public boolean damageCameraShake = true, fireDamageCameraShake = true;
  public boolean swapEquipment = true;
  public boolean limitCapeRotation = true;
  public double thirdPersonDistance = 4;

  public void validate() {
    if (underwaterFogDensity < 0 || underwaterFogDensity > 100)
      throw new IllegalArgumentException("Underwater fog density must be between 0 and 100 percent");
    if (!Double.isFinite(thirdPersonDistance) || thirdPersonDistance < 1 || thirdPersonDistance > 12)
      throw new IllegalArgumentException("Camera distance must be between 1 and 12 blocks");
  }
}
