package local.luke.power.offhand.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import local.luke.power.offhand.*;
import net.minecraft.*;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class OffhandPlayerRendererMixin extends LivingEntityRenderer {
  @Shadow private BipedEntityModel bipedModel, field_295, field_296;
  protected OffhandPlayerRendererMixin(net.minecraft.client.render.entity.model.EntityModel model,float shadow) { super(model,shadow); }
  @WrapMethod(method="render(Lnet/minecraft/entity/player/PlayerEntity;DDDFF)V")
  private void power$pose(PlayerEntity player,double x,double y,double z,float yaw,float delta,Operation<Void> original) {
    boolean a=bipedModel.leftArmPose,b=field_295.leftArmPose,c=field_296.leftArmPose;
    boolean held=Offhand.enabled(player)&&Offhand.get(player.inventory)!=null;
    bipedModel.leftArmPose=field_295.leftArmPose=field_296.leftArmPose=held;
    try (OffhandPose pose = new OffhandPose(player,delta)) { original.call(player,x,y,z,yaw,delta); }
    finally { bipedModel.leftArmPose=a;field_295.leftArmPose=b;field_296.leftArmPose=c; }
  }
  @Inject(method="method_827",at=@At("TAIL"))
  private void power$item(PlayerEntity player,float delta,CallbackInfo ci) {
    if(!Offhand.enabled(player))return;
    ItemStack stack=Offhand.get(player.inventory);if(stack==null)return;
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT|GL11.GL_CURRENT_BIT|GL11.GL_POLYGON_BIT|GL11.GL_LIGHTING_BIT|GL11.GL_TEXTURE_BIT);
    GL11.glPushMatrix();
    try {
      bipedModel.leftArm.transform(1f/16);
      GL11.glEnable(org.lwjgl.opengl.GL12.GL_RESCALE_NORMAL);
      HeldItemTransforms.thirdPersonHand(-1);
      HeldItemTransforms.render(player, dispatcher.field_2494, stack, false, -1);
    } finally { GL11.glPopMatrix();GL11.glPopAttrib(); }
  }
  @WrapOperation(method="method_827",at=@At(value="FIELD",target="Lnet/minecraft/entity/player/PlayerEntity;fishHook:Lnet/minecraft/entity/projectile/FishingBobberEntity;"))
  private FishingBobberEntity power$mainRodOnly(PlayerEntity player,Operation<FishingBobberEntity> original) {
    FishingBobberEntity hook=original.call(player);
    if(!Offhand.enabled(player))return hook;
    ItemStack main=player.inventory.getSelectedItem();
    return main!=null&&main.getItem() instanceof class_607?hook:null;
  }
}
