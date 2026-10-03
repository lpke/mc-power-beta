package local.luke.building.validation;

import static local.luke.building.validation.ValidationRun.*;

import local.luke.power.fastplace.*;
import local.luke.power.worldedit.*;
import local.luke.power.worldedit.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

public final class ExtendedValidation {
  interface Test {
    void run() throws Exception;
  }

  private static int failures;

  private static void test(String name, Test test) {
    try {
      test.run();
      log("PASS " + name);
    } catch (Throwable e) {
      failures++;
      log("FAIL " + name + " " + e);
      e.printStackTrace();
    }
  }

  public static void run(Minecraft mc) throws Exception {
    failures = 0;
    test(
        "slabs continuous mode climbs through multiple half/full layers",
        () -> {
          reset(mc);
          slabs(mc, SlabMode.CONTINUOUS, 64);
          click(mc, 0, 100, 0, 1);
          aim(mc, .5, 103, 1.5, .5, 101.49, .5);
          FastPlace.placeBatch(mc);
          check(mc.world.getBlockId(0, 101, 0) == 43, "first layer completes without release");
          aim(mc, .5, 104, 1.5, .5, 101.99, .5);
          FastPlace.beginTick(mc, true, false, true);
          FastPlace.placeBatch(mc);
          check(
              mc.world.getBlockId(0, 102, 0) == 43,
              "next layer places and completes without release");
          check(count(mc) == 60, "four slabs consumed");
        });
    test(
        "slabs double mode consumes two for a new block",
        () -> {
          reset(mc);
          slabs(mc, SlabMode.DOUBLE, 64);
          click(mc, 0, 100, 0, 1);
          check(
              mc.world.getBlockId(0, 101, 0) == 43 && count(mc) == 62, "one full block per click");
        });
    test(
        "slabs double mode rejects insufficient survival inventory",
        () -> {
          reset(mc);
          slabs(mc, SlabMode.DOUBLE, 1);
          click(mc, 0, 100, 0, 1);
          check(
              mc.world.getBlockId(0, 101, 0) == 0 && count(mc) == 1, "one slab cannot pay for two");
        });
    test(
        "slabs double mode supports creative count one",
        () -> {
          reset(mc);
          creative(mc, true);
          slabs(mc, SlabMode.DOUBLE, 1);
          click(mc, 0, 100, 0, 1);
          check(
              mc.world.getBlockId(0, 101, 0) == 43 && count(mc) == 1, "creative one item retained");
        });
    test(
        "slabs double mode consumes only missing half",
        () -> {
          reset(mc);
          set(mc.world, 0, 101, 0, 44, 2);
          slabs(mc, SlabMode.DOUBLE, 1);
          mc.player.inventory.main[0].setDamage(2);
          click(mc, 0, 101, 0, 1);
          check(
              mc.world.getBlockId(0, 101, 0) == 43
                  && mc.world.method_1778(0, 101, 0) == 2
                  && count(mc) == 0,
              "matching variant completion costs one");
        });
    test(
        "slabs match-first preserves completion action",
        () -> {
          reset(mc);
          set(mc.world, 0, 101, 0, 44, 0);
          set(mc.world, 1, 101, 0, 44, 0);
          slabs(mc, SlabMode.MATCH_FIRST, 64);
          click(mc, 0, 101, 0, 1);
          aim(mc, 1.5, 103, 1.5, 1.5, 101.49, .5);
          FastPlace.placeBatch(mc);
          check(mc.world.getBlockId(1, 101, 0) == 43, "held completes adjacent half");
          aim(mc, 2.5, 103, 1.5, 2.5, 100.99, .5);
          FastPlace.beginTick(mc, true, false, true);
          FastPlace.placeBatch(mc);
          check(mc.world.getBlockId(2, 101, 0) == 0, "does not switch to placing empty halves");
        });
    test(
        "all slab modes preserve native collision checks",
        () -> {
          reset(mc);
          slabs(mc, SlabMode.DOUBLE, 64);
          mc.player.method_1340(.5, 101 + mc.player.field_1631, .5);
          click(mc, 0, 100, 0, 1);
          check(mc.world.getBlockId(0, 101, 0) == 0 && count(mc) == 64, "no block inside player");
        });
    test(
        "WorldEdit set and undo retain Beta slab metadata",
        () -> {
          reset(mc);
          select(mc, 0, 101, 0, 2, 103, 2);
          command(mc, "//set 44:3");
          check(
              mc.world.getBlockId(1, 102, 1) == 44 && mc.world.method_1778(1, 102, 1) == 3,
              "slab metadata: id=" + mc.world.getBlockId(1, 102, 1) + " meta=" + mc.world.method_1778(1, 102, 1) + " available=" + WorldEditor.available(mc));
          command(mc, "//undo");
          check(mc.world.getBlockId(1, 102, 1) == 0, "undo air");
          command(mc, "//redo");
          check(mc.world.getBlockId(1, 102, 1) == 44, "redo");
        });
    test(
        "WorldEdit chest inventory undo without dropped duplicates",
        () -> {
          reset(mc);
          WorldEditor.leaveWorld();
          set(mc.world, 0, 101, 0, 54, 2);
          Inventory chest = (Inventory) mc.world.method_1777(0, 101, 0);
          chest.setStack(0, new ItemStack(264, 17, 0));
          int entities = mc.world.field_198.size();
          select(mc, 0, 101, 0, 0, 101, 0);
          command(mc, "//set air");
          check(mc.world.getBlockId(0, 101, 0) == 0, "chest removed successfully");
          check(mc.world.field_198.size() == entities, "no item drops");
          command(mc, "//undo");
          chest = (Inventory) mc.world.method_1777(0, 101, 0);
          check(
              chest.getStack(0).itemId == 264 && chest.getStack(0).count == 17,
              "contents restored");
          check(mc.world.field_198.size() == entities, "no duplicate item entities");
        });
    test(
        "WorldEdit clipboard preserves chest contents and new coordinates",
        () -> {
          reset(mc);
          WorldEditor.leaveWorld();
          set(mc.world, 0, 101, 0, 54, 2);
          ((Inventory) mc.world.method_1777(0, 101, 0)).setStack(1, new ItemStack(44, 13, 2));
          select(mc, 0, 101, 0, 0, 101, 0);
          var e = WorldEditor.editor(mc);
          e.copy(new Pos(0, 101, 0), false);
          drain(e);
          e.paste(new Pos(3, 101, 0), false, false, false);
          drain(e);
          var be = mc.world.method_1777(3, 101, 0);
          check(be.x == 3 && be.y == 101 && be.z == 0, "new coordinates");
          check(
              ((Inventory) be).getStack(1).count == 13
                  && ((Inventory) be).getStack(1).getDamage() == 2,
              "copied variant and count");
        });
    test(
        "WorldEdit wand selects without destroying or placing",
        () -> {
          reset(mc);
          WorldEditor.leaveWorld();
          stack(mc, 271, 0);
          aim(mc, .5, 103, 3.5, .5, 100.99, .5);
          mc.field_2818.method_1838(1);
          check(WorldEditor.click(mc, 0), "left consumed");
          check(WorldEditor.click(mc, 1), "right consumed");
          check(
              WorldEditor.editor(mc).pos1 != null && WorldEditor.editor(mc).pos2 != null,
              "both corners");
          check(mc.world.getBlockId(0, 100, 0) == 1, "target intact");
        });
    test(
        "WorldEdit world bounds and unknown flags reject before mutation",
        () -> {
          reset(mc);
          WorldEditor.leaveWorld();
          select(mc, 0, 101, 0, 1, 101, 1);
          command(mc, "//set -z stone");
          check(mc.world.getBlockId(0, 101, 0) == 0, "invalid flag no mutation");
          command(mc, "//pos2 1,128,1");
          check(
              WorldEditor.editor(mc).pos2.y() == 101,
              "invalid coordinate does not change selection");
        });
    test(
        "WorldEdit limits and cancellation restore earlier state",
        () -> {
          reset(mc);
          WorldEditor.leaveWorld();
          select(mc, 0, 101, 0, 5, 105, 5);
          var e = WorldEditor.editor(mc);
          e.engine.configure(10, 20);
          command(mc, "//set stone");
          check(mc.world.getBlockId(0, 101, 0) == 0, "limit preflight");
          e.engine.configure(65536, 20);
          WorldEditor.command(mc, "//set glass");
          e.engine.tick(220);
          check(e.engine.busy(), "partially applied");
          e.engine.cancel();
          drain(e);
          check(mc.world.getBlockId(0, 101, 0) == 0, "rollback");
        });
    test(
        "WorldEdit singleplayer and freecam gates",
        () -> {
          reset(mc);
          WorldEditor.leaveWorld();
          mc.world.isRemote = true;
          check(!WorldEditor.available(mc), "remote disabled");
          mc.world.isRemote = false;
          mc.field_2807 = null;
          check(!WorldEditor.available(mc), "different camera disabled");
          mc.field_2807 = mc.player;
        });
    test(
        "WorldEdit outline restores GL state",
        () -> {
          reset(mc);
          select(mc, 0, 101, 0, 2, 103, 2);
          boolean depth = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
          SelectionRenderer.render(mc, 1);
          check(
              depth == org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_DEPTH_TEST),
              "depth state restored");
          check(org.lwjgl.opengl.GL11.glGetError() == 0, "no GL errors");
        });
    test(
        "WorldEdit unload rolls back pending edits and clears history",
        () -> {
          reset(mc);
          WorldEditor.leaveWorld();
          select(mc, 0, 101, 0, 5, 105, 5);
          var e = WorldEditor.editor(mc);
          WorldEditor.command(mc, "//set stone");
          e.engine.tick(220);
          WorldEditor.leaveWorld();
          check(mc.world.getBlockId(0, 101, 0) == 0, "unload rollback");
          check(WorldEditor.current() == null, "no world retained");
        });
    test(
        "Click Mining Forever toggles native break delay",
        () -> {
          reset(mc);
          for (boolean enabled : new boolean[] {false, true}) {
            local.luke.power.building.config.Config.update(s -> s.clickMining = enabled);
            set(mc.world, 0, 101, 0, 20, 0);
            mc.interactionManager.method_1705();
            mc.interactionManager.method_1707(0, 101, 0, 1);
            for (int n = 0; n < 100 && mc.world.getBlockId(0, 101, 0) != 0; n++)
              mc.interactionManager.method_1721(0, 101, 0, 1);
            check(mc.world.getBlockId(0, 101, 0) == 0, "block broken");
            int delay =
                ((local.luke.building.validation.mixin.MiningAccessor) mc.interactionManager)
                    .delay();
            check(enabled ? delay == 0 : delay > 0, "delay follows setting");
          }
        });
    test(
        "WorldEdit held wand cannot mine selected blocks",
        () -> {
          reset(mc);
          stack(mc, 271, 0);
          aim(mc, .5, 103, 3.5, .5, 100.99, .5);
          mc.field_2818.method_1838(1);
          var invoker = (local.luke.building.validation.mixin.ClickInvoker) mc;
          invoker.validate$click(0);
          for (int n = 0; n < 100; n++) invoker.validate$held(0, true);
          check(mc.world.getBlockId(0, 100, 0) == 1, "held wand leaves target intact");
          check(mc.player.inventory.getSelectedItem().getDamage() == 0, "wand has no damage");
        });
    test(
        "Free look resets camera on menus",
        () -> {
          reset(mc);
          boolean previous = mc.options.thirdPerson;
          local.luke.power.building.camera.FreeLook.begin(mc);
          check(
              mc.options.thirdPerson
                  == (local.luke.power.building.config.Config.current().freeLookPerspective
                      == local.luke.power.building.camera.Perspective.THIRD_PERSON),
              "configured camera perspective active");
          mc.setScreen(new local.luke.power.building.config.SettingsScreen(null));
          check(
              !local.luke.power.building.camera.FreeLook.active() && mc.options.thirdPerson == previous,
              "camera restored on menu");
          mc.setScreen(null);
        });
    test(
        "Unrelated setting toggles preserve held flexible controls",
        () -> {
          reset(mc);
          local.luke.power.flexible.FlexiblePlacement.beginTick(
              mc, false, local.luke.power.flexible.Modes.NONE, true);
          local.luke.power.flexible.FlexiblePlacement.beginTick(
              mc, false, new local.luke.power.flexible.Modes(true, false, false, false, false), true);
          var s = local.luke.power.fakesneak.FakeSneak.settings();
          s.enabled = true;
          local.luke.power.fakesneak.FakeSneak.apply(s);
          check(
              local.luke.power.flexible.FlexiblePlacement.modes().active(),
              "fake sneak toggle keeps placement modifiers");
        });
    log("EXTENDED COMPLETE failures=" + failures);
    check(failures == 0, "extended failures: " + failures);
  }

  private static int count(Minecraft mc) {
    ItemStack s = mc.player.inventory.getSelectedItem();
    return s == null ? 0 : s.count;
  }

  private static void slabs(Minecraft mc, SlabMode mode, int count) throws Exception {
    var s = FastPlace.settings();
    s.setEnabled(true);
    s.slabMode = mode;
    s.attemptsPerTick = 2;
    FastPlace.apply(s);
    FastPlace.release();
    FastPlace.beginTick(mc, true, false, true);
    stack(mc, 44, 0);
    mc.player.inventory.main[0].count = count;
  }

  private static void select(Minecraft mc, int x, int y, int z, int a, int b, int c) {
    var e = WorldEditor.editor(mc);
    e.select(Region.between(new Pos(x, y, z), new Pos(a, b, c)));
  }

  private static void command(Minecraft mc, String s) {
    check(WorldEditor.command(mc, s), "command handled");
    drain(WorldEditor.editor(mc));
  }

  private static void drain(Editor e) {
    int ticks = 0;
    while (e.engine.busy()) {
      e.engine.tick(64);
      check(++ticks < 10000, "bounded job");
    }
  }
}
