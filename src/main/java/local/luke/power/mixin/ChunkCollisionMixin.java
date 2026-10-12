package local.luke.power.mixin;

import java.util.*;
import local.luke.power.gameplay.CollisionCandidates;
import net.minecraft.class_43;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep collision queries out of dense item/TNT lists without changing other queries. */
@Mixin(class_43.class)
public class ChunkCollisionMixin implements CollisionCandidates.ChunkAccess {
  @Shadow public List[] field_965;
  @Unique private List<Entity>[] power$colliders;
  @Unique @SuppressWarnings("unchecked")
  private List<Entity>[] power$sections() {
    if (power$colliders == null) {
      power$colliders = new List[field_965.length];
      for (int i = 0; i < power$colliders.length; i++) power$colliders[i] = new ArrayList<>();
    }
    return power$colliders;
  }
  @Inject(method = "method_868", at = @At("TAIL"))
  private void power$add(Entity entity, CallbackInfo ci) {
    power$addCollider(entity);
  }
  @Override public void power$addCollider(Entity entity) {
    if (!CollisionCandidates.simple(entity)) power$sections()[entity.field_1620].add(entity);
  }
  @Inject(method = "method_869", at = @At("TAIL"))
  private void power$remove(Entity entity, int section, CallbackInfo ci) {
    if (power$colliders != null && !CollisionCandidates.simple(entity))
      power$colliders[Math.max(0, Math.min(power$colliders.length - 1, section))].remove(entity);
  }
  @Inject(method = "method_870", at = @At("HEAD"), cancellable = true)
  private void power$query(Entity source, Box box, List<Entity> result, CallbackInfo ci) {
    if (!CollisionCandidates.active()) return;
    power$queryColliders(source, box, result, 0);
    ci.cancel();
  }
  @Override public void power$queryColliders(Entity source, Box box, List<Entity> result, int bottom) {
    if (power$colliders == null) return;
    int min = Math.max(0, MathHelper.floot((box.minY - bottom - 2) / 16));
    int max = Math.min(power$colliders.length - 1, MathHelper.floot((box.maxY - bottom + 2) / 16));
    for (int section = min; section <= max; section++)
      for (Entity entity : power$colliders[section])
        if (entity != source && entity.boundingBox.intersects(box)) result.add(entity);
  }
}
