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
}
