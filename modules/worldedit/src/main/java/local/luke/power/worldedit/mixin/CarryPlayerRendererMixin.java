package local.luke.power.worldedit.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import local.luke.power.worldedit.carry.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.player.*;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class CarryPlayerRendererMixin {
  @Shadow private BipedEntityModel bipedModel, field_295, field_296;
  @Unique private boolean power$holding;

  @WrapMethod(method = "render(Lnet/minecraft/entity/player/PlayerEntity;DDDFF)V")
  private void power$render(
      PlayerEntity player,
      double x,
      double y,
      double z,
      float yaw,
      float delta,
      Operation<Void> original) {
    Minecraft mc = (Minecraft) FabricLoader.getInstance().getGameInstance();
    power$holding = player == mc.player && ContainerCarry.visible(mc);
    ((CarryPose) bipedModel).power$carrying(power$holding);
    ((CarryPose) field_295).power$carrying(power$holding);
    ((CarryPose) field_296).power$carrying(power$holding);
    try {
      original.call(player, x, y, z, yaw, delta);
    } finally {
      power$holding = false;
      ((CarryPose) bipedModel).power$carrying(false);
      ((CarryPose) field_295).power$carrying(false);
      ((CarryPose) field_296).power$carrying(false);
    }
  }

  @Redirect(
      method = "method_827(Lnet/minecraft/entity/player/PlayerEntity;F)V",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/entity/player/PlayerInventory;getSelectedItem()Lnet/minecraft/item/ItemStack;"))
  private ItemStack power$hideItem(PlayerInventory inventory) {
    return power$holding ? null : inventory.getSelectedItem();
  }

  @Inject(method = "method_827(Lnet/minecraft/entity/player/PlayerEntity;F)V", at = @At("TAIL"))
  private void power$object(PlayerEntity player, float delta, CallbackInfo ci) {
    if (power$holding)
      CarryRenderer.thirdPerson(
          (Minecraft) FabricLoader.getInstance().getGameInstance(), player, delta);
  }
}
