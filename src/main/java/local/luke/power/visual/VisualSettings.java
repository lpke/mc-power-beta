package local.luke.power.visual;

public final class VisualSettings {
  public boolean softRain, softSnow, oldCobble, oldBricks, redstonePowerLevels, lowFire;
  public boolean slashChat, containerCarry, containerPreview;
  public boolean inventoryInPortals;
  public boolean shiftClickIntoCraftingGrid;
  public boolean damageCameraShake = true, fireDamageCameraShake = true;
  public boolean swapEquipment = true;
  public double thirdPersonDistance = 4;

  public void validate() {
    if (!Double.isFinite(thirdPersonDistance) || thirdPersonDistance < 1 || thirdPersonDistance > 12)
      throw new IllegalArgumentException("Camera distance must be between 1 and 12 blocks");
  }
}
