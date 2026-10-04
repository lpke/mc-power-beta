package local.luke.power.creative.mixin;

import local.luke.power.creative.Modes;
import net.minecraft.block.Block;
import net.minecraft.entity.living.player.AbstractClientPlayer;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Preserve vanilla portal transfer and cooldown; change only the local mode's warm-up. */
@Mixin(AbstractClientPlayer.class)
public abstract class PortalMixin extends PlayerEntity {
  protected PortalMixin(Level level) { super(level); }

  @ModifyConstant(method = "processMovement", constant = @Constant(floatValue = 0.0125F))
  private float power$instantPortal(float original) {
    return !level.isRemote && Modes.protectedPlayer(this) ? 1F : original;
  }

  @Inject(method = "processMovement", at = @At("HEAD"))
  private void power$spectatorPortal(CallbackInfo ci) {
    if (level.isRemote || !Modes.spectator(this) || vehicle != null) return;
    if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
        || Math.abs(x) >= 31999998 || Math.abs(z) >= 31999998) return;
    // No-clip movement skips block callbacks. Check only the player's small bounding box.
    int minX = (int) Math.floor(boundingBox.minX + 0.001), maxX = (int) Math.floor(boundingBox.maxX - 0.001);
    int minY = Math.max(0, (int) Math.floor(boundingBox.minY + 0.001));
    int maxY = Math.min(127, (int) Math.floor(boundingBox.maxY - 0.001));
    int minZ = (int) Math.floor(boundingBox.minZ + 0.001), maxZ = (int) Math.floor(boundingBox.maxZ - 0.001);
    if (maxX - minX > 2 || maxY - minY > 3 || maxZ - minZ > 2) return;
    for (int x = minX; x <= maxX; x++) for (int y = minY; y <= maxY; y++) for (int z = minZ; z <= maxZ; z++)
      if (level.getBlockID(x, y, z) == Block.PORTAL.id) {
        Block.PORTAL.onEntityCollision(level, x, y, z, this);
        return;
      }
  }
}
