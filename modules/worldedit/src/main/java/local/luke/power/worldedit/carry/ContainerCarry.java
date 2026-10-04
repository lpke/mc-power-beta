package local.luke.power.worldedit.carry;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.worldedit.*;
import local.luke.power.worldedit.core.*;
import local.luke.power.worldedit.mixin.CarryWorldAccess;
import net.minecraft.class_212;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Inventory;
import net.minecraft.nbt.*;
import net.minecraft.world.World;
import org.lwjgl.input.Mouse;

/** BTA's empty-hand carry interaction, with journaled Beta block-entity transfers. */
public final class ContainerCarry {
  private static World world;
  private static Path file;
  private static CarryJournal held;
  private static boolean failed, loaded, consumingUse;
  private static long lastMessage;

  public static boolean carrying() {
    return held != null && !held.phase.equals("complete");
  }

  public static int carriedBlock() {
    return carrying() ? held.block : 0;
  }

  public static int carriedCount() {
    return carrying() ? held.size() : 0;
  }

  public static int carriedMetadata() {
    return carrying() ? held.metadata : 0;
  }

  public static boolean visible(Minecraft mc) {
    return world == mc.world
        && mc.player != null
        && mc.player.health > 0
        && carrying()
        && !WorldEditor.freecam()
        && !CreativeAccess.spectator(mc.player);
  }

  public static void tick(Minecraft mc) {
    if (!Mouse.isButtonDown(1) || world != mc.world) consumingUse = false;
    local.luke.power.input.InteractionState.carryingContainer = world == mc.world && carrying();
    if (world == mc.world && loaded) return;
    if (world != mc.world) {
      world = mc.world;
      file = null;
      held = null;
      failed = false;
      loaded = false;
    }
    if (world == null || world.isRemote || mc.player == null) return;
    loaded = true;
    try {
      File data = ((CarryWorldAccess) world).power$storage().method_1736("power-beta-carry");
      if (data == null) return;
      file = data.toPath();
      if (Files.exists(file)) {
        held = CarryJournal.read(file);
        if (held.phase.equals("complete")) held = null;
        else recover(mc);
      }
    } catch (Exception e) {
      stop(mc, e);
    } finally {
      local.luke.power.input.InteractionState.carryingContainer = carrying();
    }
  }

  private static void recover(Minecraft mc) throws Exception {
    boolean removing = held.phase.equals("removing");
    if (!removing && !held.phase.equals("placing")) return;
    int dimension = removing ? held.sourceDimension : held.targetDimension;
    if (mc.player.dimensionId != dimension)
      throw new IOException("Return to the transfer dimension to recover the container");
    for (int i = 0; i < held.size(); i++) {
      Pos p = removing ? held.source(i) : held.target(i);
      world.method_214(p.x() >> 4, p.z() >> 4);
    }
    transfer(removing);
    if (removing) {
      world.method_195(true, null);
      held.phase = "held";
      held.write(file);
    } else complete();
  }

  private static void transfer(boolean removing) throws IOException {
    Map<Pos, BlockValue> before = new LinkedHashMap<>(), after = new LinkedHashMap<>();
    for (int i = 0; i < held.size(); i++) {
      Pos p = removing ? held.source(i) : held.target(i);
      BlockValue value = at(held.value(i), p);
      before.put(p, removing ? value : BlockValue.AIR);
      after.put(p, removing ? BlockValue.AIR : value);
    }
    CarryTransfer.apply(new MinecraftWorld(world), before, after);
  }

  public static boolean click(Minecraft mc, int button) {
    tick(mc);
    if (world == null
        || world.isRemote
        || mc.player == null
        || mc.player.health <= 0
        || mc.currentScreen != null) return false;
    if (button == 1 && consumingUse) return true;
    if (failed) return carrying();
    boolean carrying = carrying();
    if (carrying && button != 1) return true;
    if (!carrying && !local.luke.power.input.InteractionState.containerCarryEnabled) return false;
    if (button != 1
        || (!carrying
            && (!(mc.player.field_161 != null && mc.player.field_161.field_2536)
                || mc.player.inventory.getSelectedItem() != null))) return false;
    if (CreativeAccess.spectator(mc.player)
        || mc.player.field_1594 != null
        || mc.player.field_1595 != null) return carrying;
    if (WorldEditor.freecam() || WorldEditor.editing()) {
      if (carrying) message(mc, "Finish the current edit or return to your player before placing.");
      return carrying;
    }
    var hit = mc.field_2823;
    if (hit == null || hit.field_1983 != class_212.TILE) return carrying;
    Pos pos = new Pos(hit.field_1984, hit.field_1985, hit.field_1986);
    MinecraftWorld blocks = new MinecraftWorld(world);
    if (!blocks.loaded(pos) || file == null) return carrying;
    // Never allow synthetic or stale ray targets outside normal interaction reach.
    double dx = mc.player.x - pos.x() - .5,
        dy = mc.player.y - pos.y() - .5,
        dz = mc.player.z - pos.z() - .5;
    double reach = mc.interactionManager.method_1715() + 1;
    if (dx * dx + dy * dy + dz * dz > reach * reach) return carrying;
    try {
      if (!carrying) {
        BlockValue value = blocks.get(pos);
        if (!Set.of(23, 54, 61, 62).contains(value.id)
            || !(world.method_1777(pos.x(), pos.y(), pos.z()) instanceof Inventory)) return false;
        if (value.nbt() == null) throw new IOException("Container data is unavailable");
        try {
          if (value.id == 54) validateChestLayout(blocks, pos);
        } catch (IOException e) {
          message(mc, e.getMessage());
          return true;
        }
        // Each chest block owns its own 27 slots. Never snapshot or remove its neighbor.
        held = new CarryJournal(pos, mc.player.dimensionId, value);
        held.write(file); // This must succeed before the source can be touched.
        local.luke.power.input.InteractionState.carryingContainer = true;
        transfer(true);
        world.method_195(true, null);
        held.phase = "held";
        held.write(file);
        consumingUse = Mouse.isButtonDown(1);
        mc.player.method_500();
        message(mc, "Carrying container. Use a block face to place it.");
      } else {
        int[][] offsets = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
        if (hit.field_1987 < 0 || hit.field_1987 >= offsets.length) return true;
        int[] side = offsets[hit.field_1987];
        Pos target = new Pos(pos.x() + side[0], pos.y() + side[1], pos.z() + side[2]);
        boolean alongX = Math.abs(Math.sin(Math.toRadians(mc.player.yaw))) < Math.sqrt(.5);
        // Keep placement support for double-chest journals saved by earlier versions.
        Pos other = held.size() == 2 ? target.add(alongX ? 1 : 0, 0, alongX ? 0 : 1) : null;
        List<Pos> targets = other == null ? List.of(target) : List.of(target, other);
        for (Pos p : targets) {
          if (!blocks.loaded(p)
              || blocks.get(p).id != 0
              || held.block == 54 && !canPlaceChest(blocks, p, targets)
              || !world.method_156(held.block, p.x(), p.y(), p.z(), false, hit.field_1987)) {
            message(
                mc,
                other != null
                    ? "Choose two empty spaces clear of entities and other chests."
                    : held.block == 54
                        ? "Choose an empty space clear of entities. Chests can only join one single"
                              + " chest."
                        : "Choose an empty space clear of entities.");
            return true;
          }
        }
        held.targets(target, other, mc.player.dimensionId);
        held.phase = "placing";
        held.write(file);
        transfer(false);
        consumingUse = Mouse.isButtonDown(1);
        mc.player.method_500();
        complete();
        message(mc, "Container placed.");
      }
      return true;
    } catch (Exception e) {
      stop(mc, e);
      return true;
    }
  }

  private static BlockValue at(BlockValue value, Pos pos) throws IOException {
    NbtCompound nbt = NbtIo.read(new DataInputStream(new ByteArrayInputStream(value.nbt())));
    nbt.putInt("x", pos.x());
    nbt.putInt("y", pos.y());
    nbt.putInt("z", pos.z());
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    NbtIo.write(nbt, new DataOutputStream(out));
    return new BlockValue(value.id, value.meta, out.toByteArray());
  }

  private static final int[][] NEIGHBORS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

  private static void validateChestLayout(MinecraftWorld blocks, Pos p) throws IOException {
    List<Pos> pair = new ArrayList<>();
    pair.add(p);
    for (int[] d : NEIGHBORS) {
      Pos neighbor = p.add(d[0], 0, d[1]);
      if (!blocks.loaded(neighbor))
        throw new IOException("Load the neighboring chunk before carrying this chest");
      if (blocks.get(neighbor).id == 54) pair.add(neighbor);
    }
    if (pair.size() > 2) throw new IOException("Invalid chest arrangement");
    for (Pos part : pair) {
      if (!(world.method_1777(part.x(), part.y(), part.z()) instanceof Inventory)
          || hasOutsideChest(blocks, part, pair)) throw new IOException("Chest pair is incomplete");
    }
  }

  private static boolean canPlaceChest(MinecraftWorld blocks, Pos target, List<Pos> targets) {
    if (targets.size() == 2) return !hasOutsideChest(blocks, target, targets);
    Pos joining = null;
    for (int[] offset : NEIGHBORS) {
      Pos neighbor = target.add(offset[0], 0, offset[1]);
      if (!blocks.loaded(neighbor)) return false;
      if (world.getBlockId(neighbor.x(), neighbor.y(), neighbor.z()) != 54) continue;
      // Only join one intact single chest. Check its neighboring chunks too, so
      // an unloaded half cannot be mistaken for an empty space in a double chest.
      if (joining != null
          || !(world.method_1777(neighbor.x(), neighbor.y(), neighbor.z()) instanceof Inventory)
          || hasOutsideChest(blocks, neighbor, List.of(target))) return false;
      joining = neighbor;
    }
    return true;
  }

  private static boolean hasOutsideChest(MinecraftWorld blocks, Pos p, List<Pos> pair) {
    for (int[] d : NEIGHBORS) {
      Pos neighbor = p.add(d[0], 0, d[1]);
      if (!blocks.loaded(neighbor) || !pair.contains(neighbor) && blocks.get(neighbor).id == 54)
        return true;
    }
    return false;
  }

  private static void complete() throws IOException {
    world.method_195(true, null);
    held.phase = "complete";
    // Retain a full recovery copy permanently; never reduce inventories to item drops.
    held.write(file.resolveSibling("power-beta-carried-" + held.id + ".json"));
    held.write(file);
    held = null;
    local.luke.power.input.InteractionState.carryingContainer = false;
  }

  private static void stop(Minecraft mc, Exception failure) {
    failed = true;
    WorldEditor.LOG.error("Container movement stopped; recovery data preserved", failure);
    message(
        mc,
        "Container movement stopped safely. Recovery data is preserved in this world's data"
            + " folder.");
  }

  private static void message(Minecraft mc, String text) {
    if (System.currentTimeMillis() - lastMessage < 750) return;
    lastMessage = System.currentTimeMillis();
    mc.inGameHud.addChatMessage("§e" + text);
  }
}
