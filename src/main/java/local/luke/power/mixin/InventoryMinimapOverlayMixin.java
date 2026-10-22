package local.luke.power.mixin;

import local.luke.power.map.MinimapQuickMenu;
import net.minecraft.class_555;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draw after the complete container, its recipe book, item tooltips and subclass renderer. */
@Mixin(class_555.class)
public abstract class InventoryMinimapOverlayMixin {
  @Inject(method="method_1844",at=@At("HEAD"))
  private void power$minimapMenuContext(float delta,CallbackInfo ci) {
    MinimapQuickMenu.beginFrame();
    MinimapQuickMenu.active((Minecraft)net.fabricmc.loader.api.FabricLoader.getInstance().getGameInstance());
  }
  @Inject(method="method_1844",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screen/Screen;render(IIF)V",shift=At.Shift.AFTER))
  private void power$minimapMenu(float delta,CallbackInfo ci) {
    Minecraft mc=(Minecraft)net.fabricmc.loader.api.FabricLoader.getInstance().getGameInstance();
    if(mc.currentScreen==null)return;
    int x=org.lwjgl.input.Mouse.getX()*mc.currentScreen.width/mc.displayWidth;
    int y=mc.currentScreen.height-org.lwjgl.input.Mouse.getY()*mc.currentScreen.height/mc.displayHeight-1;
    MinimapQuickMenu.renderMenu(mc,x,y);
  }
}
