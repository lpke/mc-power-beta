package local.luke.power.mixin;

import local.luke.power.bow.ModernBow;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class ModernBowClientMixin {
  @Inject(method="tick",at=@At("HEAD"))
  private void power$drawTick(CallbackInfo ci) { ModernBow.tick((Minecraft)(Object)this); }
  @Inject(method="setScreen",at=@At("HEAD"))
  private void power$menu(net.minecraft.client.gui.screen.Screen screen,CallbackInfo ci) { if(screen!=null)ModernBow.cancel(); }
  @Inject(method="method_2107",at=@At("HEAD"),cancellable=true)
  private void power$drawAttack(int button,CallbackInfo ci) {
    if(button==0 && ModernBow.ownsAttack((Minecraft)(Object)this)) {
      ((Minecraft)(Object)this).interactionManager.method_1705();
      ci.cancel();
    }
  }
  @Inject(method="method_2110",at=@At("HEAD"),cancellable=true)
  private void power$drawMining(int button,boolean held,CallbackInfo ci) {
    if(button==0 && ModernBow.ownsAttack((Minecraft)(Object)this)) {
      ((Minecraft)(Object)this).interactionManager.method_1705();
      ci.cancel();
    }
  }
}
