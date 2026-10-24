package local.luke.power.mixin;

import local.luke.power.gameplay.ModernSwimming;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Keep the native cast at the physical eye when the local player has a low pose. */
@Mixin(FishingBobberEntity.class)
public class FishingBobberPoseMixin {
  @ModifyArgs(method="<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;)V",
      at=@At(value="INVOKE",target="Lnet/minecraft/entity/projectile/FishingBobberEntity;method_1341(DDDFF)V"))
  private void power$physicalCastOrigin(Args args,World world,PlayerEntity player) {
    if (!(player instanceof ClientPlayerEntity) || world==null || world.isRemote
        || player.world!=world || player.dead || player.health<=0 || !ModernSwimming.lowPose(player)) return;
    Minecraft mc=(Minecraft)FabricLoader.getInstance().getGameInstance();
    if(mc!=null && mc.player==player && mc.world==world)
      args.set(1,player.y+player.method_1378());
  }
}
