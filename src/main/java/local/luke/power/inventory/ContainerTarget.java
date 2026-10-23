package local.luke.power.inventory;

import local.luke.power.input.DetachedCamera;
import local.luke.power.visual.VisualConfig;
import net.minecraft.class_27;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** A separate, read-only pick. Never replaces the player's interaction target. */
final class ContainerTarget {
  private ContainerTarget() {}

  static class_27 find(Minecraft mc) { return find(mc, 1); }

  static class_27 find(Minecraft mc, float delta) {
    if (mc.world == null || mc.world.isRemote || mc.player == null || mc.field_2807 == null) return null;
    var current = VisualConfig.current();
    double blocks = current.containerBlockReach, entities = current.containerEntityReach;
    Vec3d start, direction;
    if (DetachedCamera.isActive()) {
      start = Vec3d.createCached(DetachedCamera.x(), DetachedCamera.y(), DetachedCamera.z());
      double pitch = Math.toRadians(DetachedCamera.pitch()), yaw = Math.toRadians(DetachedCamera.yaw());
      direction = Vec3d.createCached(Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), -Math.cos(yaw) * Math.cos(pitch));
    } else {
      start = mc.field_2807.method_931(delta);
      direction = mc.field_2807.method_926(delta);
    }
    double x = start.x, y = start.y, z = start.z, reach = Math.max(blocks, entities);
    if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
        || !Double.isFinite(direction.x) || !Double.isFinite(direction.y) || !Double.isFinite(direction.z)
        || Math.abs(x) > 30_000_000 || Math.abs(z) > 30_000_000 || y < -32 || y > 160) return null;
    Vec3d end = start.add(direction.x * reach, direction.y * reach, direction.z * reach);
    // Native raycasting can load chunks. Reject a ray crossing unloaded terrain first.
    for (int cx = (int)Math.floor(Math.min(x, end.x)) >> 4; cx <= (int)Math.floor(Math.max(x, end.x)) >> 4; cx++)
      for (int cz = (int)Math.floor(Math.min(z, end.z)) >> 4; cz <= (int)Math.floor(Math.max(z, end.z)) >> 4; cz++)
        if (!mc.world.method_239(cx << 4, 64, cz << 4)) return null;
    class_27 target = mc.world.method_160(Vec3d.createCached(x, y, z), end);
    double nearest = target == null ? reach : start.distanceTo(target.field_1988);
    if (nearest > blocks) target = null;
    Box bounds = Box.create(Math.min(x, end.x), Math.min(y, end.y), Math.min(z, end.z),
        Math.max(x, end.x), Math.max(y, end.y), Math.max(z, end.z)).expand(1, 1, 1);
    // The native entity query reads only loaded chunks. Include non-containers as occluders.
    for (Object candidate : mc.world.getEntities(mc.player, bounds)) {
      Entity entity = (Entity)candidate;
      if (entity.dead || !entity.method_1356()) continue;
      double border = entity.method_1369();
      Box box = entity.boundingBox.expand(border, border, border);
      class_27 hit = box.method_89(start, end);
      double distance = box.method_88(start) ? 0 : hit == null ? Double.POSITIVE_INFINITY : start.distanceTo(hit.field_1988);
      if (distance < nearest) {
        nearest = distance;
        target = distance <= entities ? new class_27(entity) : null;
      }
    }
    return target;
  }
}
