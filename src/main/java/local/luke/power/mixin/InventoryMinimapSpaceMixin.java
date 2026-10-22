package local.luke.power.mixin;

import local.luke.power.input.InventoryGui;
import local.luke.power.map.Minimap;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import org.spongepowered.asm.mixin.Mixin;

/** The free-pointer minimap owns its controls before inventory looking can capture a press. */
@Mixin(ContainerScreen.class)
public abstract class InventoryMinimapSpaceMixin extends Screen implements InventoryGui {
  @Override public boolean power$ownsPointerControl(int x, int y) {
    return local.luke.power.map.MinimapQuickMenu.owns(minecraft,x,y) || Minimap.inventoryContains(minecraft, x, y);
  }
}
