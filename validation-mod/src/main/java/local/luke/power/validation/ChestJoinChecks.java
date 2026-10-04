package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.visual.*;
import local.luke.power.worldedit.MinecraftWorld;
import local.luke.power.worldedit.carry.*;
import local.luke.power.worldedit.core.*;
import net.minecraft.class_27;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.math.Vec3d;

/** Exercises native chest pairing and complete inventories through the real carry path. */
public final class ChestJoinChecks {
  private static final int[][] SIDES = {{-1, 0, 5}, {1, 0, 4}, {0, -1, 3}, {0, 1, 2}};

  private static void aim(Minecraft mc, Pos pos, int face) {
    mc.field_2823 =
        new class_27(
            pos.x(),
            pos.y(),
            pos.z(),
            face,
            Vec3d.createCached(pos.x() + .5, pos.y() + .5, pos.z() + .5));
  }

  private static Inventory fill(Minecraft mc, MinecraftWorld blocks, Pos p, int seed) {
    blocks.set(p, new BlockValue(54, 2));
    Inventory inv = (Inventory) mc.world.method_1777(p.x(), p.y(), p.z());
    for (int i = 0; i < 27; i++)
      inv.setStack(
          i,
          new ItemStack(
              i % 2 == 0 ? 264 : 257, i % 2 == 0 ? i + seed : 1, i % 2 == 0 ? 0 : i + seed * 10));
    inv.markDirty();
    return inv;
  }

  private static BlockValue relocated(BlockValue value, Pos p) throws IOException {
    NbtCompound nbt = NbtIo.read(new DataInputStream(new ByteArrayInputStream(value.nbt())));
    nbt.putInt("x", p.x());
    nbt.putInt("y", p.y());
    nbt.putInt("z", p.z());
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    NbtIo.write(nbt, new DataOutputStream(output));
    return new BlockValue(value.id, value.meta, output.toByteArray());
  }

  public static void restart(Minecraft mc, boolean finish) throws Exception {
    mc.setScreen(null);
    MinecraftWorld blocks = new MinecraftWorld(mc.world);
    Path expectedFile = Path.of("power-beta-split-restart.json");
    if (!finish) {
      VisualSettings options = VisualConfig.copy(); options.containerCarry = true; VisualConfig.preview(options);
      Pos source = new Pos((int)Math.floor(mc.player.x) + 2, 100, (int)Math.floor(mc.player.z));
      for (int x = -1; x <= 2; x++) for (int z = -1; z <= 4; z++) {
        Pos p = source.add(x, 0, z); mc.world.method_214(p.x() >> 4, p.z() >> 4); blocks.set(p, BlockValue.AIR);
      }
      Pos neighbor = source.add(1, 0, 0);
      fill(mc, blocks, source, 2); fill(mc, blocks, neighbor, 3);
      new CarryJournal(source, mc.player.dimensionId, blocks.get(source), neighbor, blocks.get(neighbor)).write(expectedFile.toAbsolutePath());
      mc.player.inventory.main[mc.player.inventory.selectedSlot] = null;
      mc.player.method_1340(source.x() - 1.5, 101, source.z() + 1.5);
      mc.player.field_161.field_2536 = true; aim(mc, source, 1);
      check(ContainerCarry.click(mc, 1) && ContainerCarry.carriedCount() == 1, "restart fixture pickup failed");
      mc.player.field_161.field_2536 = false;
    } else {
      failures = 0;
      test("split chest survives full client restart with both inventories intact", () -> {
        CarryJournal expected = CarryJournal.read(expectedFile.toAbsolutePath());
        ContainerCarry.tick(mc);
        check(ContainerCarry.carriedCount() == 1, "held half not recovered");
        check(blocks.get(expected.source).id == 0, "source duplicated");
        check(blocks.get(expected.second.source).same(expected.value(1)), "neighbor inventory changed after restart");
        Pos target = expected.source.add(0, 0, 3);
        blocks.set(target.add(0, -1, 0), new BlockValue(1, 0));
        mc.player.method_1340(target.x() - 1.5, 101, target.z() - 1.5);
        aim(mc, target.add(0, -1, 0), 1);
        check(ContainerCarry.click(mc, 1) && !ContainerCarry.carrying(), "recovered half placement failed");
        check(blocks.get(target).same(relocated(expected.value(), target)), "recovered inventory changed");
        check(blocks.get(expected.second.source).same(expected.value(1)), "remaining chest changed on placement");
      });
      log("SPLIT RESTART FAILURES " + failures);
    }
  }

  public static void run(Minecraft mc) throws Exception {
    failures = 0;
    check(mc.world != null && mc.player != null, "Test world required");
    check(!ContainerCarry.carrying(), "Finish previous carry first");
    mc.setScreen(null);
    VisualSettings original = VisualConfig.copy(), enabled = VisualConfig.copy();
    enabled.containerCarry = true;
    VisualConfig.preview(enabled);
    double px = mc.player.x, py = mc.player.y, pz = mc.player.z;
    float yaw = mc.player.yaw;
    ItemStack[] items = mc.player.inventory.main.clone();
    int slot = mc.player.inventory.selectedSlot;
    var oldHit = mc.field_2823;
    MinecraftWorld blocks = new MinecraftWorld(mc.world);
    Pos destination = new Pos((((int) Math.floor(px)) >> 4) * 16 + 15, 100, (int) Math.floor(pz)),
        source = destination.add(-2, 0, -3);
    Map<Pos, BlockValue> backup = new LinkedHashMap<>();
    for (int x = -4; x <= 4; x++)
      for (int z = -5; z <= 4; z++)
        for (int y = -1; y <= 1; y++) {
          Pos p = destination.add(x, y, z);
          mc.world.method_214(p.x() >> 4, p.z() >> 4);
          backup.put(p, blocks.get(p));
          blocks.set(p, BlockValue.AIR);
        }
    try {
      mc.player.inventory.selectedSlot = 0;
      mc.player.inventory.main[0] = null;
      mc.player.field_161.field_2536 = true;
      mc.player.method_1340(destination.x() - 2.5, 101, destination.z() - 1.5);
      for (int[] side : SIDES) {
        Pos existing = destination.add(side[0], 0, side[1]);
        fill(mc, blocks, source, 2);
        BlockValue carried = blocks.get(source);
        Inventory existingInventory = fill(mc, blocks, existing, 3);
        BlockValue existingValue = blocks.get(existing);
        ItemStack existingStack = existingInventory.getStack(0);
        test(
            "join single chest on side " + side[2] + " preserves both inventories",
            () -> {
              aim(mc, source, 1);
              check(
                  ContainerCarry.click(mc, 1) && ContainerCarry.carriedCount() == 1,
                  "single pickup failed");
              // Use the existing chest's face directly, rather than a supporting floor.
              aim(mc, existing, side[2]);
              check(ContainerCarry.click(mc, 1) && !ContainerCarry.carrying(), "joining rejected");
              check(
                  blocks.get(destination).same(relocated(carried, destination)),
                  "carried inventory changed");
              check(blocks.get(existing).same(existingValue), "existing inventory changed");
              check(
                  mc.world.method_1777(existing.x(), existing.y(), existing.z())
                          == existingInventory
                      && existingInventory.getStack(0) == existingStack,
                  "existing inventory was replaced");
            });
        for (Pos chosen : List.of(existing, destination)) {
          Pos remaining = chosen.equals(existing) ? destination : existing;
          test(
              "split targeted half " + chosen + " and rejoin without changing neighbor",
              () -> {
                BlockValue chosenValue = blocks.get(chosen), remainingValue = blocks.get(remaining);
                Inventory remainingInventory = (Inventory) mc.world.method_1777(remaining.x(), remaining.y(), remaining.z());
                ItemStack[] remainingItems = new ItemStack[27];
                for (int i = 0; i < 27; i++) remainingItems[i] = remainingInventory.getStack(i);
                aim(mc, chosen, 1);
                check(ContainerCarry.click(mc, 1) && ContainerCarry.carriedCount() == 1, "not holding targeted half");
                Field file = ContainerCarry.class.getDeclaredField("file");
                file.setAccessible(true);
                CarryJournal journal = CarryJournal.read((Path) file.get(null));
                check(journal.size() == 1 && journal.source.equals(chosen) && journal.value().same(chosenValue), "wrong inventory journaled");
                check(blocks.get(chosen).id == 0 && blocks.get(remaining).same(remainingValue), "wrong half removed");
                check(mc.world.method_1777(remaining.x(), remaining.y(), remaining.z()) == remainingInventory, "neighbor replaced");
                for (int i = 0; i < 27; i++) check(remainingItems[i] == remainingInventory.getStack(i), "neighbor slot replaced");
                // Reload while holding it, then place separately before joining it again.
                Field world = ContainerCarry.class.getDeclaredField("world");
                world.setAccessible(true); world.set(null, null); ContainerCarry.tick(mc);
                check(ContainerCarry.carriedCount() == 1, "half lost after reload");
                blocks.set(source.add(0, -1, 0), new BlockValue(1, 0));
                aim(mc, source.add(0, -1, 0), 1);
                check(ContainerCarry.click(mc, 1) && !ContainerCarry.carrying(), "separate placement failed");
                check(blocks.get(source).same(relocated(chosenValue, source)), "split inventory changed");
                check(blocks.get(remaining).same(remainingValue), "neighbor changed during placement");
                aim(mc, source, 1);
                check(ContainerCarry.click(mc, 1) && ContainerCarry.carriedCount() == 1, "re-pickup failed");
                blocks.set(chosen.add(0, -1, 0), new BlockValue(1, 0));
                aim(mc, chosen.add(0, -1, 0), 1);
                check(ContainerCarry.click(mc, 1) && !ContainerCarry.carrying(), "rejoin failed");
                check(blocks.get(chosen).same(chosenValue) && blocks.get(remaining).same(remainingValue), "rejoin changed inventory");
              });
        }
        blocks.set(destination, BlockValue.AIR);
        blocks.set(existing, BlockValue.AIR);
      }
      fill(mc, blocks, source, 4);
      BlockValue saved = blocks.get(source);
      aim(mc, source, 1);
      check(
          ContainerCarry.click(mc, 1) && ContainerCarry.carriedCount() == 1,
          "negative-case pickup failed");
      for (int[] side : SIDES)
        for (boolean connected : new boolean[] {true, false}) {
          Pos one = destination.add(side[0], 0, side[1]);
          Pos two =
              connected ? one.add(side[0], 0, side[1]) : destination.add(-side[0], 0, -side[1]);
          fill(mc, blocks, one, 2);
          fill(mc, blocks, two, 3);
          BlockValue a = blocks.get(one), b = blocks.get(two);
          test(
              "reject "
                  + (connected ? "existing double" : "two separate singles")
                  + " on side "
                  + side[2],
              () -> {
                aim(mc, one, side[2]);
                check(
                    ContainerCarry.click(mc, 1) && ContainerCarry.carriedCount() == 1,
                    "invalid join consumed carry");
                check(
                    blocks.get(destination).id == 0
                        && blocks.get(one).same(a)
                        && blocks.get(two).same(b),
                    "invalid join modified world");
              });
          blocks.set(one, BlockValue.AIR);
          blocks.set(two, BlockValue.AIR);
        }
      test(
          "rejected joins leave the single chest available for valid placement",
          () -> {
            aim(mc, destination.add(0, -1, 0), 1);
            check(
                ContainerCarry.click(mc, 1) && !ContainerCarry.carrying(),
                "valid placement failed after rejection");
            check(
                blocks.get(destination).same(relocated(saved, destination)),
                "rejection changed saved inventory");
          });
    } finally {
      if (!ContainerCarry.carrying())
        for (var e : backup.entrySet()) blocks.set(e.getKey(), e.getValue());
      mc.player.method_1340(px, py, pz);
      mc.player.yaw = yaw;
      mc.player.field_161.field_2536 = false;
      mc.player.inventory.main = items;
      mc.player.inventory.selectedSlot = slot;
      mc.field_2823 = oldHit;
      VisualConfig.preview(original);
    }
    log("CHEST JOIN FAILURES " + failures);
  }
}
