package local.luke.power.slabplacement.mixin;

import local.luke.power.slabplacement.SlabPlacement;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
  @Inject(method = "tick", at = @At("HEAD"))
  private void input(CallbackInfo ci) {
    SlabPlacement.tick((Minecraft) (Object) this);
  }
}
