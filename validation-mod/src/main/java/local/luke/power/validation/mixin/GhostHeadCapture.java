package local.luke.power.validation.mixin;

import local.luke.power.render.GhostHead;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=GhostHead.class,remap=false)
public class GhostHeadCapture {
  @Inject(method="draw",at=@At("HEAD"))
  private static void draw(Minecraft mc,float delta,double x,double y,double z,float yaw,float pitch,
      float opacity,boolean throughBlocks,CallbackInfo ci) {
    local.luke.power.validation.Release220Checks.head(x,y,z,opacity);
  }
  @Inject(method="render",at=@At("HEAD"))
  private static void before(Minecraft mc,float delta,CallbackInfo ci){local.luke.power.validation.GhostProbe.before();}
  @Inject(method="render",at=@At("RETURN"))
  private static void after(Minecraft mc,float delta,CallbackInfo ci){local.luke.power.validation.GhostProbe.after();}
}
