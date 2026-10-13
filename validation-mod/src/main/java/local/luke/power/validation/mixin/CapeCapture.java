package local.luke.power.validation.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import local.luke.power.validation.CapeChecks;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value=PlayerEntityRenderer.class,priority=500)
public abstract class CapeCapture {
  @WrapOperation(method="method_827",
      slice=@Slice(from=@At(value="FIELD",target="Lnet/minecraft/entity/player/PlayerEntity;playerCapeUrl:Ljava/lang/String;"),
          to=@At(value="INVOKE",target="Lnet/minecraft/client/render/entity/model/BipedEntityModel;renderCape(F)V")),
      at=@At(value="INVOKE",target="Lorg/lwjgl/opengl/GL11;glRotatef(FFFF)V"))
  private void power$capture(float angle,float x,float y,float z,Operation<Void> original) {
    if(CapeChecks.recording) CapeChecks.angles.add(angle);
    original.call(angle,x,y,z);
  }
}
