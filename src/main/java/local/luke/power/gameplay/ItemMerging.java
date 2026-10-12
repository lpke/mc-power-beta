package local.luke.power.gameplay;

import java.io.*;
import java.util.*;
import net.minecraft.class_142;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.world.World;
import local.luke.power.input.DetachedCamera;
import net.minecraft.client.Minecraft;

/** Resumable spatial sweep: every item gets a turn within a bounded tick budget. */
public final class ItemMerging {
  private ItemMerging() {}
  public interface Access { Sweep power$itemSweep(); }
  private record Cell(int x, int y, int z, String item) {}
  public static String identity(ItemStack stack) throws IOException {
    NbtCompound tag = stack.writeNbt(new NbtCompound());
    tag.putByte("Count", (byte)1);
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    NbtIo.write(tag, new DataOutputStream(bytes));
    return Base64.getEncoder().encodeToString(bytes.toByteArray());
  }
  public static int merge(World world, Minecraft mc) {
    return ((Access)world).power$itemSweep().run(world, mc);
  }
  public static final class Sweep {
    private Object[] snapshot;
    private int cursor, visible, mergedInSweep, idleTicks;
    private boolean merging;
    private final Map<Cell, class_142> cells = new HashMap<>();
    private final Set<Entity> removed = Collections.newSetFromMap(new IdentityHashMap<>());
    private final IdentityHashMap<class_142, String> identities = new IdentityHashMap<>();
    public void removed(Entity entity) { if (snapshot != null) removed.add(entity); }
    public void clear() {
      snapshot = null; cursor = visible = mergedInSweep = idleTicks = 0; merging = false;
      cells.clear(); removed.clear(); identities.clear();
    }
    public void tick(World world, Minecraft mc) {
      if (!GameplayConfig.current().mergeItems) { clear(); return; }
      if (idleTicks > 0) { idleTicks--; return; }
      run(world, mc);
    }
    private boolean eligible(class_142 item) {
      return !item.dead && !removed.contains(item) && item.field_564 != null && item.field_564.count > 0;
    }
    private String identityOf(class_142 item) throws IOException {
      String key = identities.get(item);
      if (key == null) { key = identity(item.field_564); identities.put(item, key); }
      return key;
    }
    public int run(World world, Minecraft mc) {
      GameplaySettings settings = GameplayConfig.current();
      if (!settings.mergeItems || world.isRemote || mc.world != world || mc.player == null) { clear(); return 0; }
      List<?> entities = world.method_291();
      if (entities.size() < settings.mergeThreshold) { clear(); idleTicks = 9; return 0; }
      long start = System.nanoTime();
      if (snapshot == null) snapshot = entities.toArray();
      identities.clear(); // Item data may have changed since the previous tick.
      var camera = mc.field_2807 == null ? mc.player : mc.field_2807;
      double cameraX = DetachedCamera.isActive() ? DetachedCamera.x() : camera.x;
      double cameraZ = DetachedCamera.isActive() ? DetachedCamera.z() : camera.z;
      double distance = local.luke.power.controls.util.ModOptions.getRenderDistanceChunks() * 16;
      int merged = 0, visits = 0;
      while (cursor < snapshot.length && visits++ < 4096) {
        if (visits > 16 && System.nanoTime() - start >= 1_500_000L) break;
        Object entry = snapshot[cursor++];
        if (!(entry instanceof class_142 item) || !eligible(item)) continue;
        if (!merging) {
          if (Math.abs(item.x - cameraX) <= distance && Math.abs(item.z - cameraZ) <= distance
              && ++visible >= settings.mergeThreshold) { merging = true; cursor = 0; }
          continue;
        }
        if (item.field_564.count >= item.field_564.getMaxCount()) continue;
        try {
          int x = (int)Math.floor(item.x / 2), y = (int)Math.floor(item.y / 2), z = (int)Math.floor(item.z / 2);
          String key = identityOf(item);
          for (int dx = -1; dx <= 1 && !item.dead; dx++)
            for (int dy = -1; dy <= 1 && !item.dead; dy++)
              for (int dz = -1; dz <= 1 && !item.dead; dz++) {
                class_142 target = cells.get(new Cell(x + dx, y + dy, z + dz, key));
                if (target == null || target == item || !eligible(target)
                    || Math.abs(target.x - item.x) > 2 || Math.abs(target.y - item.y) > 2 || Math.abs(target.z - item.z) > 2) continue;
                int count = Math.min(item.field_564.count, target.field_564.getMaxCount() - target.field_564.count);
                if (count <= 0 || !key.equals(identityOf(target))) continue;
                target.field_564.count += count;
                item.field_564.count -= count;
                var health = (local.luke.power.mixin.ItemHealthAccessor)target;
                health.power$health(Math.min(health.power$health(), ((local.luke.power.mixin.ItemHealthAccessor)item).power$health()));
                target.field_565 = Math.min(target.field_565, item.field_565);
                target.field_566 = Math.max(target.field_566, item.field_566);
                if (item.field_564.count == 0) { item.markDead(); merged++; }
              }
          if (!item.dead && item.field_564.count < item.field_564.getMaxCount()) cells.put(new Cell(x, y, z, key), item);
        } catch (IOException e) {
          // Unsupported item data stays on its original entity.
        }
      }
      mergedInSweep += merged;
      if (cursor == snapshot.length) {
        boolean idle = mergedInSweep == 0;
        clear();
        if (idle) idleTicks = 9;
      }
      return merged;
    }
  }
}
