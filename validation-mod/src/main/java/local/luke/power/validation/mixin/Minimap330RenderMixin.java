package local.luke.power.validation.mixin;

import local.luke.power.map.*;
import local.luke.power.validation.Inventory330Capture;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=Minimap.class,remap=false)
public abstract class Minimap330RenderMixin {
  @Inject(method="drawFrame(Lnet/minecraft/client/Minecraft;Llocal/luke/power/map/MapSettings;Llocal/luke/power/map/MinimapGeometry$Frame;ZF)V",at=@At("HEAD"))
  private static void map(Minecraft mc,MapSettings s,MinimapGeometry.Frame frame,boolean preview,float delta,CallbackInfo ci){Inventory330Capture.map();}
}
