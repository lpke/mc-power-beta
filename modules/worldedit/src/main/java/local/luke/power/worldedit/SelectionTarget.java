package local.luke.power.worldedit;

import local.luke.power.input.DetachedCamera;
import local.luke.power.worldedit.core.BlockRay;
import local.luke.power.worldedit.core.Pos;
import net.minecraft.class_212;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.Vec3d;

/** A selection-only ray from the current view, without replacing the interaction target. */
final class SelectionTarget {
  private SelectionTarget() {}
  static Pos find(Minecraft mc) {
    Vec3d start, direction;
    if (DetachedCamera.isActive()) {
      start = Vec3d.createCached(DetachedCamera.x(), DetachedCamera.y(), DetachedCamera.z());
      double pitch = Math.toRadians(DetachedCamera.pitch()), yaw = Math.toRadians(DetachedCamera.yaw());
      direction = Vec3d.createCached(Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), -Math.cos(yaw) * Math.cos(pitch));
    } else {
      start = mc.player.method_931(1);
      direction = mc.player.method_926(1);
    }
    double reach = mc.interactionManager.method_1715();
    for (Pos p : BlockRay.cells(start.x, start.y, start.z, direction.x, direction.y, direction.z, reach))
      if (!p.valid() || !mc.world.method_239(p.x(), p.y(), p.z())) return null;
    var hit = mc.world.method_160(start, start.add(direction.x * reach, direction.y * reach, direction.z * reach));
    return hit != null && hit.field_1983 == class_212.TILE
        ? new Pos(hit.field_1984, hit.field_1985, hit.field_1986) : null;
  }
}
