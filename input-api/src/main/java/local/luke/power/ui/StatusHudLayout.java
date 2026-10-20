package local.luke.power.ui;

/** Native status positions shared across the HUD module's game mappings. */
public interface StatusHudLayout {
  default int power$staminaX() { return power$statusRight() - 81; }
  default int power$staminaOffsetX() { return 0; }
  default int power$staminaOffsetY() { return 0; }
  default int power$staminaY() { return power$statusY(); }
  default boolean power$staminaVisible() { return power$hotbarVisible(); }
  int power$statusRight();
  int power$statusY();
  int power$armorX();
  int power$armorY();
  boolean power$armorVisible();
  boolean power$hotbarVisible();
}
