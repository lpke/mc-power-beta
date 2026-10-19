package local.luke.power.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.gameplay.*;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class SwimmingRendererMixin {
  @Shadow private BipedEntityModel bipedModel,field_295,field_296;

  @WrapMethod(method="render(Lnet/minecraft/entity/player/PlayerEntity;DDDFF)V")
  private void power$models(PlayerEntity player,double x,double y,double z,float yaw,float delta,Operation<Void> original) {
    float amount=ModernSwimming.amount(player,delta);
    boolean compact=ModernSwimming.compact(player);
    power$models(amount,compact);
    try { original.call(player,x,y,z,yaw,delta); }
    finally { power$models(0,false); }
  }

  @Unique private void power$models(float amount,boolean compact) {
    ((SwimmingModel)bipedModel).power$swimPose(amount,compact);
    ((SwimmingModel)field_295).power$swimPose(amount,compact);
    ((SwimmingModel)field_296).power$swimPose(amount,compact);
  }

  @Inject(method="method_824(Lnet/minecraft/entity/player/PlayerEntity;FFF)V",at=@At("TAIL"))
  private void power$body(PlayerEntity player,float age,float yaw,float delta,CallbackInfo ci) {
    float amount=ModernSwimming.amount(player,delta);
    if(amount<=0 || player.method_943())return;
    float pitch=player.prevPitch+(player.pitch-player.prevPitch)*delta;
    GL11.glRotatef(amount*(player.method_1334()?-90-pitch:-90),1,0,0);
    if(ModernSwimming.compact(player))GL11.glTranslatef(0,-1,.3f);
  }
}
