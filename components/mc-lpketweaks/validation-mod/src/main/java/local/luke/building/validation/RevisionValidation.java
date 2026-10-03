package local.luke.building.validation;

import static local.luke.building.validation.ValidationRun.*;

import java.util.*;
import local.luke.building.validation.mixin.*;
import local.luke.fastplace.*;
import local.luke.slabplacement.SlabPlacement;
import local.luke.tweaks.SlabPairs;
import local.luke.tweaks.camera.*;
import local.luke.tweaks.config.Config;
import local.luke.worldedit.*;
import local.luke.worldedit.chat.*;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;

public final class RevisionValidation {
  @FunctionalInterface
  private interface Case {
    void run() throws Exception;
  }

  private static int passed, failed;

  private static void test(String name, Case action) {
    try {
      action.run();
      passed++;
      log("PASS revision " + name);
    } catch (Throwable e) {
      failed++;
      log("FAIL revision " + name + " " + e);
      e.printStackTrace();
    }
  }

  private static void configure(Minecraft mc, SlabMode mode, boolean completion) throws Exception {
    reset(mc);
    var s = FastPlace.settings();
    s.setEnabled(true);
    s.slabMode = mode;
    s.attemptsPerTick = 1;
    FastPlace.apply(s);
    FastPlace.release();
    var slabs = SlabPlacement.settings();
    slabs.enabled = completion;
    SlabPlacement.apply(slabs);
    stack(mc, 44, 0);
  }

  private static boolean use(Minecraft mc, int x, int y, int z, int face) {
    return mc.interactionManager.method_1713(
        mc.player, mc.world, mc.player.inventory.getSelectedItem(), x, y, z, face);
  }

  public static void run(Minecraft mc) {
    passed = failed = 0;
    for (var mode : SlabMode.values())
      for (boolean completion : new boolean[] {false, true}) {
        test(
            mode + " adjacent completion " + completion,
            () -> {
              configure(mc, mode, completion);
              for (int meta = 0; meta < 4; meta++) {
                stack(mc, 44, meta);
                set(mc.world, 0, 104, 0, 44, meta);
                set(mc.world, -1, 104, 0, 1, 0);
                boolean result = use(mc, -1, 104, 0, 5);
                check(result == completion, "result follows toggle");
                check(
                    mc.world.getBlockId(0, 104, 0) == (completion ? 43 : 44),
                    "only enabled completion merges");
                check(
                    mc.player.inventory.getSelectedItem().count == (completion ? 63 : 64),
                    "one slab consumed for completion");
                set(mc.world, 0, 104, 0, 44, meta);
                check(use(mc, 0, 104, 0, 1), "native top always completes");
                check(mc.world.getBlockId(0, 104, 0) == 43, "native double");
              }
            });
        test(
            mode + " mismatched completion " + completion,
            () -> {
              configure(mc, mode, completion);
              set(mc.world, 0, 104, 0, 44, 1);
              set(mc.world, -1, 104, 0, 1, 0);
              check(!use(mc, -1, 104, 0, 5), "mismatched variant rejected");
              check(
                  mc.world.getBlockId(0, 104, 0) == 44 && mc.world.method_1778(0, 104, 0) == 1,
                  "original remains");
            });
      }
    test(
        "double inventory collision and height guards",
        () -> {
          configure(mc, SlabMode.DOUBLE, true);
          var stack = mc.player.inventory.getSelectedItem();
          stack.count = 1;
          check(!SlabPairs.canPlace(mc, stack, 0, 100, 0, 1), "one slab cannot form new pair");
          check(!use(mc, 0, 100, 0, 1), "insufficient inventory does not place a half");
          stack.count = 2;
          check(SlabPairs.canPlace(mc, stack, 0, 100, 0, 1), "two slabs accepted");
          check(use(mc, 0, 100, 0, 1) && stack.count == 0, "pair consumes exactly two");
          stack(mc, 44, 0);
          set(mc.world, 0, 126, 0, 1, 0);
          check(
              !SlabPairs.canPlace(mc, mc.player.inventory.getSelectedItem(), 0, 126, 0, 1),
              "height guard");
          set(mc.world, 0, 101, 0, 44, 0);
          mc.player.method_1340(.5, 101.5 + mc.player.field_1631, .5);
          check(
              !SlabPairs.canPlace(mc, mc.player.inventory.getSelectedItem(), 0, 101, 0, 1),
              "no suffocation");
        });
    test(
        "double creative one-item stack",
        () -> {
          configure(mc, SlabMode.DOUBLE, true);
          creative(mc, true);
          var stack = mc.player.inventory.getSelectedItem();
          stack.count = 1;
          check(use(mc, 0, 100, 0, 1), "creative pair succeeds");
          check(
              stack.count == 1 && mc.world.getBlockId(0, 101, 0) == 43, "creative preserves stack");
        });
    test(
        "double excluded slabs retain half preview and native placement",
        () -> {
          configure(mc, SlabMode.DOUBLE, true);
          var s = FastPlace.settings();
          s.blacklist = local.luke.fastplace.config.Settings.parseFilters("44");
          s.listMode = local.luke.fastplace.config.Settings.ListMode.BLACKLIST;
          FastPlace.apply(s);
          FastPlace.release();
          var t = PlacementTarget.at(mc.world, mc.player.inventory.getSelectedItem(), 0, 100, 0, 1);
          check(t.expectedBlock() == 44, "excluded prediction stays half");
          check(use(mc, 0, 100, 0, 1), "native place");
          check(mc.world.getBlockId(0, 101, 0) == 44, "excluded remains half");
        });
    test(
        "double roof face rejects top and continues side",
        () -> {
          configure(mc, SlabMode.DOUBLE, true);
          set(mc.world, 0, 102, 0, 1, 0);
          var stack = mc.player.inventory.getSelectedItem();
          FastPlace.beginTick(mc, true, false, true);
          var target = PlacementTarget.at(mc.world, stack, 0, 102, 0, 3);
          check(use(mc, 0, 102, 0, 3), "initial roof pair");
          FastPlace.firstPlacement(mc, stack, 0, 3, mc.player.yaw, target, true);
          aim(mc, .5, 105.5, 2.5, .5, 102.99, 1.5);
          FastPlace.beginTick(mc, true, false, true);
          FastPlace.placeBatch(mc);
          check(mc.world.getBlockId(0, 103, 1) == 0, "roof did not climb");
          aim(mc, .5, 103.2, 4.5, .5, 102.5, 1.99);
          FastPlace.beginTick(mc, true, false, true);
          FastPlace.placeBatch(mc);
          check(mc.world.getBlockId(0, 102, 2) == 43, "roof continues along original face");
        });
    test(
        "chat registered completion and native history",
        () -> {
          reset(mc);
          WorldEditBeta.leaveWorld();
          var registry =
              (List<?>)
                  Class.forName("com.matthewperiut.retrocommands.util.RetroChatUtil")
                      .getField("commands")
                      .get(null);
          var name = Class.forName("com.matthewperiut.retrocommands.api.Command").getMethod("name");
          boolean found = false;
          for (Object c : registry) if (name.invoke(c).equals("/pos1")) found = true;
          check(found, "double slash registered for color and suggestions");
          ChatScreen chat = new ChatScreen();
          mc.setScreen(chat);
          ChatInvoker access = (ChatInvoker) chat;
          access.revision$text("//pos");
          ChatAccess.setText("//pos");
          access.revision$key('\t', 15);
          check(ChatAccess.text(access.revision$text()).equals("//pos1"), "Tab completes pos1");
          access.revision$key('\t', 15);
          check(ChatAccess.text(access.revision$text()).equals("//pos2"), "Tab cycles pos2");
          access.revision$text("//pos1 1,104,2");
          ChatAccess.setText("//pos1 1,104,2");
          access.revision$key('\r', 28);
          check(mc.currentScreen == null, "Enter closes chat");
          check(
              WorldEditBeta.current().pos1.equals(new local.luke.worldedit.core.Pos(1, 104, 2)),
              "command executed");
          mc.setScreen(new ChatScreen());
          access = (ChatInvoker) mc.currentScreen;
          access.revision$key('\0', 200);
          check(
              ChatAccess.text(access.revision$text()).equals("//pos1 1,104,2"),
              "Up recalls submitted WorldEdit command");
          mc.setScreen(null);
        });
    test(
        "entity filters radius pets and occupied mounts",
        () -> {
          reset(mc);
          WorldEditBeta.leaveWorld();
          List<Entity> created = new ArrayList<>();
          try {
            double x = mc.player.x, y = mc.player.boundingBox.minY, z = mc.player.z;
            Entity near = new class_142(mc.world, x + 1, y, z, new ItemStack(1, 1, 0));
            created.add(near);
            mc.world.method_210(near);
            Entity far = new class_142(mc.world, x + 10, y, z, new ItemStack(1, 1, 0));
            created.add(far);
            mc.world.method_210(far);
            Entity cow = class_206.method_732("Cow", mc.world);
            cow.method_1340(x + 1, y, z + 1);
            created.add(cow);
            mc.world.method_210(cow);
            class_121 wolf = (class_121) class_206.method_732("Wolf", mc.world);
            wolf.method_431(true);
            wolf.method_1340(x - 1, y, z);
            created.add(wolf);
            mc.world.method_210(wolf);
            Entity boat = class_206.method_732("Boat", mc.world);
            boat.method_1340(x, y, z);
            created.add(boat);
            mc.world.method_210(boat);
            boat.field_1594 = mc.player;
            mc.player.field_1595 = boat;
            net.minecraft.class_549 cart =
                (net.minecraft.class_549) class_206.method_732("Minecart", mc.world);
            cart.method_1340(x + 1, y, z);
            cart.setStack(0, new ItemStack(1, 20, 0));
            created.add(cart);
            mc.world.method_210(cart);
            long before = mc.world.field_198.stream().filter(o -> o instanceof class_142).count();
            WorldEditBeta.command(mc, "/remove minecarts 3");
            check(cart.dead && cart.getStack(0) == null, "storage cart removed");
            check(
                mc.world.field_198.stream().filter(o -> o instanceof class_142).count() == before,
                "storage cart removal creates no drops");
            WorldEditBeta.command(mc, "//countentities items 3");
            check(!near.dead, "count does not remove");
            WorldEditBeta.command(mc, "/remove items 3");
            check(near.dead && !far.dead && !cow.dead, "type and radius respected");
            WorldEditBeta.command(mc, "/butcher -a 3");
            check(cow.dead && !wolf.dead, "animals without pets");
            WorldEditBeta.command(mc, "/remove pets 3");
            check(wolf.dead, "explicit pets removed");
            WorldEditBeta.command(mc, "/remove all 3");
            check(!boat.dead && !mc.player.dead, "occupied mount and player protected");
          } finally {
            mc.player.field_1595 = null;
            for (Entity entity : created) {
              entity.field_1594 = null;
              entity.markDead();
            }
          }
        });
    test(
        "free look camera and native key lifecycle",
        () -> {
          reset(mc);
          check(Arrays.asList(mc.options.allKeys).contains(FreeLook.KEY), "native binding");
          check(
              mc.options
                  .getKeybindName(Arrays.asList(mc.options.allKeys).indexOf(FreeLook.KEY))
                  .equals("Free Look"),
              "native binding label");
          for (Perspective p : Perspective.values())
            for (boolean previous : new boolean[] {false, true}) {
              var s = Config.current().copy();
              s.freeLookPerspective = p;
              Config.apply(s);
              mc.options.thirdPerson = previous;
              float yaw = mc.player.yaw,
                  pitch = mc.player.pitch,
                  py = mc.player.prevYaw,
                  pp = mc.player.prevPitch;
              FreeLook.begin(mc);
              check(
                  mc.options.thirdPerson
                      == (p == Perspective.THIRD_PERSON || s.freeLookFollowThirdPerson && previous),
                  "chosen perspective");
              check(!FreeLook.turn(mc.player, 100, -100), "input redirected");
              check(
                  mc.player.yaw == yaw
                      && mc.player.pitch == pitch
                      && mc.player.prevYaw == py
                      && mc.player.prevPitch == pp,
                  "body fields unchanged");
              check(FreeLook.yaw(yaw) != yaw, "camera yaw changes");
              mc.setScreen(new local.luke.tweaks.config.SettingsScreen(null));
              check(
                  !FreeLook.rendering() && mc.options.thirdPerson == previous,
                  "menu restores prior perspective");
              mc.setScreen(null);
            }
          check(org.lwjgl.opengl.GL11.glGetError() == 0, "GL clean");
        });
    log("REVISION RESULT passed=" + passed + " failed=" + failed);
  }

  public static void camera(Minecraft mc, String value) throws Exception {
    reset(mc);
    creative(mc, true);
    flight(mc, true);
    stack(mc, 1, 0);
    aim(mc, .5, 103.5, 4.5, .5, 101, .5);
    for (int x = -3; x <= 3; x++) set(mc.world, x, 101, 0, 35, (x + 3) * 2);
    var s = Config.current().copy();
    s.freeLookPerspective = Perspective.valueOf(value);
    s.freeLook = true;
    s.freeLookToggle = false;
    Config.apply(s);
    mc.options.thirdPerson = false;
    log("CAMERA READY " + value + " key=" + FreeLook.KEY.code);
  }

  public static void state(Minecraft mc) {
    log(
        "CAMERA STATE active="
            + FreeLook.active()
            + " rendering="
            + FreeLook.rendering()
            + " third="
            + mc.options.thirdPerson
            + " yaw="
            + mc.player.yaw
            + " pitch="
            + mc.player.pitch
            + " viewYaw="
            + FreeLook.yaw(mc.player.yaw)
            + " viewPitch="
            + FreeLook.pitch(mc.player.pitch)
            + " GL="
            + org.lwjgl.opengl.GL11.glGetError());
    if (mc.currentScreen instanceof ChatScreen)
      log("CHAT TEXT " + ChatAccess.text(((ChatInvoker) mc.currentScreen).revision$text()));
  }
}
