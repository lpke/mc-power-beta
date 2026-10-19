package local.luke.power.mixin;

import local.luke.power.gameplay.ModernSwimming;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Entity.class)
public class SwimmingPositionMixin {
  @ModifyArgs(method="write",at=@At(value="INVOKE",target="Lnet/minecraft/entity/Entity;method_1329([D)Lnet/minecraft/nbt/NbtList;",ordinal=0))
  private void power$nativeSavedPosition(Args args) {
    Entity entity=(Entity)(Object)this;
    if (!(entity instanceof ClientPlayerEntity player) || player.world.isRemote
        || player.health<=0 || !ModernSwimming.lowPose(entity)) return;
    double[] position=args.get(0);
    position[1] += 1.62f-entity.field_1631;
  }
}
