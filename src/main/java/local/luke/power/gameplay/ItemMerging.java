package local.luke.power.gameplay;

import java.io.*;
import java.util.*;
import net.minecraft.class_142;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.world.World;
import net.minecraft.util.math.Vec3d;
import local.luke.power.input.DetachedCamera;
import net.minecraft.client.Minecraft;

/** Spatial hashing keeps a crowded pile linear, with bounded work each pass. */
public final class ItemMerging {
  private ItemMerging() {}
  private record Cell(int x, int y, int z, String item) {}
  public static String identity(ItemStack stack) throws IOException {
    NbtCompound tag = stack.writeNbt(new NbtCompound());
    tag.putByte("Count", (byte)1);
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    NbtIo.write(tag, new DataOutputStream(bytes));
    return Base64.getEncoder().encodeToString(bytes.toByteArray());
  }
  public static int merge(World world, Minecraft mc) {
    GameplaySettings settings = GameplayConfig.current();
    if (!settings.mergeItems || world.isRemote || mc.world != world || mc.player == null) return 0;
    List<?> entities = world.method_291();
    if (entities.size() < settings.mergeThreshold) return 0;
    var camera = mc.field_2807 == null ? mc.player : mc.field_2807;
    Vec3d position = DetachedCamera.isActive()
        ? Vec3d.createCached(DetachedCamera.x(), DetachedCamera.y(), DetachedCamera.z())
        : Vec3d.createCached(camera.x, camera.y, camera.z);
    int visible = 0;
    for (Object entry : entities) if (entry instanceof class_142 item && !item.dead && item.method_1367(position)
        && ++visible >= settings.mergeThreshold) break;
    if (visible < settings.mergeThreshold) return 0;
    Map<Cell, class_142> cells = new HashMap<>();
    int merged = 0, visited = 0;
    long start = System.nanoTime();
    for (Object entry : entities) {
      if (!(entry instanceof class_142 item) || item.dead || item.field_564 == null
          || item.field_564.count <= 0 || item.field_564.count >= item.field_564.getMaxCount()) continue;
      if (++visited > 8192 || visited > 32 && System.nanoTime() - start > 3_000_000L) break;
      try {
        int x = (int)Math.floor(item.x / 2), y = (int)Math.floor(item.y / 2), z = (int)Math.floor(item.z / 2);
        String identity = identity(item.field_564);
        for (int dx = -1; dx <= 1 && item.field_564.count > 0; dx++)
          for (int dy = -1; dy <= 1 && item.field_564.count > 0; dy++)
            for (int dz = -1; dz <= 1 && item.field_564.count > 0; dz++) {
              class_142 target = cells.get(new Cell(x + dx, y + dy, z + dz, identity));
              if (target == null || target.dead || Math.abs(target.x - item.x) > 2
                  || Math.abs(target.y - item.y) > 2 || Math.abs(target.z - item.z) > 2) continue;
              int count = Math.min(item.field_564.count, target.field_564.getMaxCount() - target.field_564.count);
              if (count <= 0) continue;
              target.field_564.count += count;
              item.field_564.count -= count;
              var targetHealth = (local.luke.power.mixin.ItemHealthAccessor)target;
              targetHealth.power$health(Math.min(targetHealth.power$health(), ((local.luke.power.mixin.ItemHealthAccessor)item).power$health()));
              target.field_565 = Math.min(target.field_565, item.field_565);
              target.field_566 = Math.max(target.field_566, item.field_566);
              if (item.field_564.count == 0) { item.markDead(); merged++; }
            }
        if (!item.dead) cells.put(new Cell(x, y, z, identity), item);
      } catch (IOException e) {
        // Unsupported item data stays on its original entity.
      }
    }
    return merged;
  }
}
