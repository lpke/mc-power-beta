package local.luke.power.mixin;

import local.luke.power.render.GhostHead;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.client.Minecraft;
import net.minecraft.class_555;
import net.minecraft.client.render.WorldRenderer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(class_555.class)
public class GhostHeadMixin {
  @Shadow private Minecraft field_2349;
  // After clouds, weather and translucent terrain, before the hand resets depth, for each eye.
  @WrapOperation(method = "method_1841", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/client/render/WorldRenderer;method_1552(F)V"))
  private void power$head(WorldRenderer renderer, float delta, Operation<Void> original) {
    if (!GhostHead.visible(field_2349)) { original.call(renderer, delta); return; }
    // Fancy clouds leave their 12x horizontal scale in the model-view matrix.
    GL11.glPushMatrix();
    try { original.call(renderer, delta); }
    finally { GL11.glPopMatrix(); }
    GhostHead.render(field_2349, delta);
  }
}
