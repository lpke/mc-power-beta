package local.luke.power.mixin;

import local.luke.power.gameplay.GameplayConfig;
import local.luke.power.input.PlayerMode;
import net.minecraft.block.Block;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class TrapdoorClimbingMixin {
  @Inject(method = "method_932", at = @At("RETURN"), cancellable = true)
  private void power$trapdoorLadder(CallbackInfoReturnable<Boolean> ci) {
    var entity = (LivingEntity)(Object)this;
    if (ci.getReturnValueZ() || entity.world.isRemote || !GameplayConfig.current().modernTrapdoorClimbing
        || entity instanceof PlayerMode mode && mode.power$spectator()) return;
    int x = MathHelper.floot(entity.x), y = MathHelper.floot(entity.boundingBox.minY), z = MathHelper.floot(entity.z);
    if (y <= 0 || !entity.world.method_155(x, y - 1, z, x, y, z)
        || entity.world.getBlockId(x, y, z) != Block.field_1913.id) return;
    int trapdoor = entity.world.method_1778(x, y, z);
    // 26.3 requires OPEN plus the same FACING as the ladder immediately below.
    // Beta's four trapdoor walls 0..3 correspond to native ladder faces 2..5.
    if ((trapdoor & 4) != 0 && entity.world.getBlockId(x, y - 1, z) == Block.field_1854.id
        && entity.world.method_1778(x, y - 1, z) == (trapdoor & 3) + 2) ci.setReturnValue(true);
  }
}
