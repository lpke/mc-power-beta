package local.luke.power.recipes;

/** Reads the shared inventory wheel preferences without depending on its Minecraft mappings. */
final class RecipeWheelControls {
  private RecipeWheelControls() {}
  static int direction(int amount) {
    try {
      Object inventory = Class.forName("local.luke.power.inventory.Config").getField("INVENTORY_TWEAKS_CONFIG").get(null);
      Object mouse = inventory.getClass().getField("MOUSE_TWEAKS_CONFIG").get(inventory);
      Object wheel = mouse.getClass().getField("SCROLL_WHEEL_CONFIG").get(mouse);
      if (!Boolean.TRUE.equals(wheel.getClass().getField("enableScrollWheelTweaks").get(wheel))) return 0;
      boolean inverted = Boolean.TRUE.equals(wheel.getClass().getField("invertScrollCursorSlotDirection").get(wheel));
      return Integer.signum(amount) * (inverted ? -1 : 1);
    } catch (ReflectiveOperationException unavailable) { return 0; }
  }
}
