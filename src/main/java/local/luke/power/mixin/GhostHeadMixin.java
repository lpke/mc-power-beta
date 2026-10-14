package local.luke.power.mixin;

import local.luke.power.render.GhostHead;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class GhostHeadMixin {
  @Shadow private Minecraft field_1814;
  @Inject(method = "method_1544", at = @At("TAIL"))
  private void power$head(Vec3d view, @Coerce Object culler, float delta, CallbackInfo ci) {
    GhostHead.render(field_1814, delta);
  }
}
