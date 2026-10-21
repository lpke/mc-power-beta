package local.luke.power.inventory.tools.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.inventory.tools.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ContainerScreen.class, priority = 1200)
public abstract class InventoryToolsContainerMixin extends Screen implements InventoryToolsHost {
  @Shadow protected int backgroundWidth, backgroundHeight;
  @Unique private InventoryToolsOverlay power$inventoryTools;
  @Inject(method = "init", at = @At("TAIL"))
  private void power$toolsInit(CallbackInfo ci) { power$installInventoryTools(); }
  public void power$installInventoryTools() {
    if (power$inventoryTools != null) power$inventoryTools.close();
    power$inventoryTools = new InventoryToolsOverlay();
    power$inventoryTools.attach(minecraft, this, width, height, backgroundWidth, backgroundHeight, buttons);
  }
  public InventoryToolsLayout.Rect power$recipeButtonBounds() { return power$inventoryTools == null ? null : power$inventoryTools.recipeButtonBounds(); }
  public boolean power$inventoryToolsModal() { return power$inventoryTools != null && power$inventoryTools.modal(); }
  @Inject(method = "render", at = @At("HEAD"))
  private void power$toolsPrepare(int x, int y, float delta, CallbackInfo ci) { if (power$inventoryTools != null) power$inventoryTools.prepare(); }
  @Inject(method = "render", at = @At("TAIL"))
  private void power$toolsRender(int x, int y, float delta, CallbackInfo ci) { if (power$inventoryTools != null) power$inventoryTools.render(x, y, delta); }
  // The overlay owns dispatch before native gesture guards cancel clicks inside shared panels.
  @WrapMethod(method = "mouseClicked")
  private void power$toolsPress(int x, int y, int button, Operation<Void> original) {
    if (power$inventoryTools == null || !power$inventoryTools.press(x, y, button)) original.call(x, y, button);
  }
  @WrapMethod(method = "mouseReleased")
  private void power$toolsRelease(int x, int y, int button, Operation<Void> original) {
    if (power$inventoryTools == null || !power$inventoryTools.release(button)) original.call(x, y, button);
  }
  @WrapMethod(method = "keyPressed")
  private void power$toolsKey(char c, int key, Operation<Void> original) {
    if (power$inventoryTools == null || !power$inventoryTools.key(c, key)) original.call(c, key);
  }
  @Inject(method = "removed", at = @At("HEAD"))
  private void power$toolsClose(CallbackInfo ci) { if (power$inventoryTools != null) power$inventoryTools.close(); }
}
