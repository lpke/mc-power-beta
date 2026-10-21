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
  public InventoryToolsLayout.Rect power$inventoryToolsPlayerArea() {
    if (minecraft.player == null) return null;
    var screen = (ContainerScreen)(Object)this;
    int left = Integer.MAX_VALUE, top = Integer.MAX_VALUE, right = Integer.MIN_VALUE, bottom = Integer.MIN_VALUE;
    for (Object entry : screen.container.slots) if (entry instanceof net.minecraft.screen.slot.Slot slot) {
      var access = (local.luke.power.mixin.DeathSlotAccessor)(Object)slot;
      if (access.power$deathInventory() != minecraft.player.inventory || access.power$deathIndex() < 0 || access.power$deathIndex() >= 36) continue;
      left = Math.min(left, slot.x - 1); top = Math.min(top, slot.y - 1);
      right = Math.max(right, slot.x + 17); bottom = Math.max(bottom, slot.y + 17);
    }
    return left == Integer.MAX_VALUE ? null : new InventoryToolsLayout.Rect(
        (width - backgroundWidth) / 2 + local.luke.power.input.InventoryPanels.offset(this) + left,
        (height - backgroundHeight) / 2 + top, right - left, bottom - top);
  }
  public boolean power$inventoryToolsModal() { return power$inventoryTools != null && power$inventoryTools.modal(); }
  public boolean power$openSortingLayout() { return power$inventoryTools != null && power$inventoryTools.openSortingLayout(); }
  public void power$inventoryToolsWheel(int delta) { if (power$inventoryTools != null) power$inventoryTools.wheel(delta); }
  public boolean power$drawInventoryTools(int x, int y, float delta) {
    if (power$inventoryTools == null) return false;
    power$inventoryTools.prepare();
    if (!power$inventoryTools.modal()) return false;
    power$inventoryTools.render(x, y, delta); return true;
  }
  @WrapMethod(method = "render")
  private void power$hideInventoryUnderTools(int x, int y, float delta, Operation<Void> original) {
    if (!power$drawInventoryTools(x, y, delta)) original.call(x, y, delta);
  }
  @Inject(method = "render", at = @At("HEAD"))
  private void power$toolsPrepare(int x, int y, float delta, CallbackInfo ci) { if (power$inventoryTools != null) power$inventoryTools.prepare(); }
  @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/container/ContainerScreen;drawBackground(F)V", shift = At.Shift.AFTER))
  private void power$headerBehindItems(int x, int y, float delta, CallbackInfo ci) {
    if (power$inventoryTools != null) power$inventoryTools.renderHeader();
  }
  @Inject(method = "render", at = @At("TAIL"))
  private void power$toolsRender(int x, int y, float delta, CallbackInfo ci) { if (power$inventoryTools != null) power$inventoryTools.render(x, y, delta); }
  // The overlay owns dispatch before native gesture guards cancel clicks inside shared panels.
  @WrapMethod(method = "mouseClicked")
  private void power$toolsPress(int x, int y, int button, Operation<Void> original) {
    if (power$inventoryTools == null || !power$inventoryTools.press(x, y, button)) original.call(x, y, button);
  }
  @WrapMethod(method = "mouseReleased")
  private void power$toolsRelease(int x, int y, int button, Operation<Void> original) {
    if (power$inventoryTools == null || !power$inventoryTools.release(x, y, button)) original.call(x, y, button);
  }
  @WrapMethod(method = "keyPressed")
  private void power$toolsKey(char c, int key, Operation<Void> original) {
    if (power$inventoryTools == null || !power$inventoryTools.key(c, key)) original.call(c, key);
  }
  @Inject(method = "removed", at = @At("HEAD"))
  private void power$toolsClose(CallbackInfo ci) { if (power$inventoryTools != null) power$inventoryTools.close(); }
}
