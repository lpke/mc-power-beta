package local.luke.power.inventory;

import local.luke.power.input.DetachedCamera;
import net.minecraft.class_27;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** A separate, read-only pick. Never replaces the player's interaction target. */
final class ContainerTarget {
  private ContainerTarget() {}

  static class_27 find(Minecraft mc) {
    if (!DetachedCamera.isActive()) return mc.field_2823;
    double x = DetachedCamera.x(), y = DetachedCamera.y(), z = DetachedCamera.z();
    double pitch = Math.toRadians(DetachedCamera.pitch()), yaw = Math.toRadians(DetachedCamera.yaw());
    double reach = Math.min(32, mc.interactionManager.method_1715());
    if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
        || !Double.isFinite(pitch) || !Double.isFinite(yaw) || !Double.isFinite(reach) || reach <= 0
        || Math.abs(x) > 30_000_000 || Math.abs(z) > 30_000_000 || y < -32 || y > 160) return null;
    Vec3d start = Vec3d.createCached(x, y, z);
    Vec3d end = start.add(Math.sin(yaw) * Math.cos(pitch) * reach,
        -Math.sin(pitch) * reach, -Math.cos(yaw) * Math.cos(pitch) * reach);
    // Native raycasting can load chunks. Reject a ray crossing unloaded terrain first.
    for (int cx = (int)Math.floor(Math.min(x, end.x)) >> 4; cx <= (int)Math.floor(Math.max(x, end.x)) >> 4; cx++)
      for (int cz = (int)Math.floor(Math.min(z, end.z)) >> 4; cz <= (int)Math.floor(Math.max(z, end.z)) >> 4; cz++)
        if (!mc.world.method_239(cx << 4, 64, cz << 4)) return null;
    class_27 target = mc.world.method_160(Vec3d.createCached(x, y, z), end);
    double nearest = target == null ? reach : start.distanceTo(target.field_1988);
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
        target = new class_27(entity);
      }
    }
    return target;
  }
}
