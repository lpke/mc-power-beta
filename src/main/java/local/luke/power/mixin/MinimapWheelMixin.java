package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import local.luke.power.map.Minimap;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Insert before the existing flight and row-wheel transforms, so each event has one owner.
@Mixin(value = Minecraft.class, priority = 1600)
public class MinimapWheelMixin {
  @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE",
      target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I", remap = false))
  private int power$expandedMapWheel(int wheel) {
    return Minimap.expandedWheel((Minecraft)(Object)this, wheel) ? 0 : wheel;
  }
}
