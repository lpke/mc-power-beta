package local.luke.power.autowalk.mixin;

import local.luke.power.input.InventoryGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ContainerScreen.class)
public abstract class ContainerGuiMixin extends Screen implements InventoryGui {
  @Shadow protected int backgroundWidth;
  @Shadow protected int backgroundHeight;

  public boolean power$containsGuiSpace(int x, int y) {
    int left = (width - backgroundWidth) / 2 + local.luke.power.input.InventoryPanels.offset(this), top = (height - backgroundHeight) / 2;
    if (x >= left && x < left + backgroundWidth && y >= top && y < top + backgroundHeight
        || power$extraGuiSpace(x, y) || local.luke.power.input.InventoryPanels.contains(this, x, y)) return true;
    for (Object value : buttons) {
      if (value instanceof ButtonWidget button && button.visible
          && x >= button.x && x < button.x + ((ButtonGuiAccessor)button).power$guiWidth()
          && y >= button.y && y < button.y + ((ButtonGuiAccessor)button).power$guiHeight()) return true;
    }
    return false;
  }
}
