package local.luke.fakesneak.mixin;

import local.luke.fakesneak.FakeSneak;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
  @Inject(method = "tick", at = @At("HEAD"))
  private void input(CallbackInfo ci) {
    FakeSneak.tick((Minecraft) (Object) this);
  }
}
