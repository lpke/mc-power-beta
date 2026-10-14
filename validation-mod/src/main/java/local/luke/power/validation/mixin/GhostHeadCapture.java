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
  @Inject(method="render",at=@At("HEAD"))
  private static void before(Minecraft mc,float delta,CallbackInfo ci){local.luke.power.validation.GhostProbe.before();}
  @Inject(method="render",at=@At("RETURN"))
  private static void after(Minecraft mc,float delta,CallbackInfo ci){local.luke.power.validation.GhostProbe.after();}
}
