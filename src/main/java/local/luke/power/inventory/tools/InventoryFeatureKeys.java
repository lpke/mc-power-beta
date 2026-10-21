package local.luke.power.inventory.tools;

import net.minecraft.client.Minecraft;

/** Unavailable actions never reserve mouse buttons or compete with another input owner. */
public final class InventoryFeatureKeys {
  private InventoryFeatureKeys() {}
  public static boolean available(Minecraft mc, String id) {
    return switch (id) {
      case "powerbeta.swapHands" -> local.luke.power.offhand.OffhandInput.available(mc);
      case "powerbeta.restock", "powerbeta.toolSwitch" -> InventoryAutomation.survival(mc);
      case "powerbeta.sortInventory" -> InventoryToolsConfig.current().sorting && SortingSlots.eligible(mc) && local.luke.power.offhand.OffhandInput.container(mc);
      case "powerbeta.depositMatching" -> InventoryToolsConfig.current().depositMatching && SortingSlots.eligible(mc) && local.luke.power.offhand.OffhandInput.container(mc)
          && (mc.currentScreen.getClass() == net.minecraft.class_264.class || mc.currentScreen.getClass() == net.minecraft.class_235.class);
      case "powerbeta.saveCreativeHotbar", "powerbeta.loadCreativeHotbar" -> SavedCreativeHotbars.eligible(mc);
      case "powerbeta.recipeBook" -> recipeBook(mc);
      default -> true;
    };
  }
  private static boolean recipeBook(Minecraft mc) {
    if (mc == null || mc.player == null || mc.world == null || mc.player.dead || mc.player.health <= 0
        || !org.lwjgl.opengl.Display.isActive() || local.luke.power.input.DetachedCamera.isActive()
        || local.luke.power.input.InteractionState.carryingContainer) return false;
    var settings = local.luke.power.recipes.RecipeBookConfig.current(); if (!settings.enabled) return false;
    if (mc.currentScreen == null) {
      boolean creative = mc.player instanceof local.luke.power.input.PlayerMode mode && mode.power$creative();
      return settings.inventory && !creative || settings.pauseMenu;
    }
    if (!(mc.currentScreen instanceof net.minecraft.client.gui.screen.container.ContainerScreen)
        || org.lwjgl.input.Mouse.isGrabbed() || local.luke.power.input.InventoryTextInput.active(mc.currentScreen)
        || local.luke.power.input.InventoryPanels.modal(mc.currentScreen)) return false;
    var book = local.luke.power.recipes.RecipeBooks.book(mc.currentScreen); return book != null && book.enabled();
  }
}
