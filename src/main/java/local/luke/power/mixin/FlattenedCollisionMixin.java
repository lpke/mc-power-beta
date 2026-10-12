package local.luke.power.mixin;

import java.util.List;
import local.luke.power.gameplay.CollisionCandidates;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** StationAPI overrides chunk insertion and queries, but inherits native removal. */
@Mixin(targets = "net.modificationstation.stationapi.impl.world.chunk.FlattenedChunk", remap = false)
public class FlattenedCollisionMixin {
  @Shadow @Final public short firstBlock;
  @Inject(method = "method_868", at = @At("TAIL"))
  private void power$add(Entity entity, CallbackInfo ci) {
    ((CollisionCandidates.ChunkAccess)this).power$addCollider(entity);
  }
  @Inject(method = "method_870", at = @At("HEAD"), cancellable = true)
  private void power$query(Entity source, Box box, List<Entity> result, CallbackInfo ci) {
    if (!CollisionCandidates.active()) return;
    ((CollisionCandidates.ChunkAccess)this).power$queryColliders(source, box, result, firstBlock);
    ci.cancel();
  }
}
