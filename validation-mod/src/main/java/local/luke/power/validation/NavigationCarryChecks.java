package local.luke.power.validation;

import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.Validation.*;

import com.google.gson.JsonPrimitive;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import local.luke.power.visual.*;
import local.luke.power.worldedit.*;
import local.luke.power.worldedit.carry.*;
import local.luke.power.worldedit.core.*;
import net.minecraft.class_27;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public final class NavigationCarryChecks {
  private static Object invoke(Object o, String name, Class<?> type, Object arg) throws Exception {
    Method m = o.getClass().getDeclaredMethod(name, type);
    m.setAccessible(true);
    return m.invoke(o, arg);
  }

  private static List<Setting> visible(PowerOptionsScreen screen) throws Exception {
    List<Setting> result = new ArrayList<>();
    for (Object row : (List<?>) field(screen, "rows")) {
      Setting s = (Setting) call(row, "setting");
      if (s != null) result.add(s);
    }
    return result;
  }

  public static void menu(Minecraft mc) throws Exception {
    failures = 0;
    var parent = mc.currentScreen;
    PowerOptionsScreen s = new PowerOptionsScreen(parent);
    mc.setScreen(s);
    try {
      Setting feature = find(s.session(), "power_camera:config.enabled");
      feature.value = new JsonPrimitive(false);
      Setting key = find(s.session(), "keys.Toggle Freecam");
      field(s, "page", "Camera");
      field(s, "scroll", 75d);
      field(s, "sideScroll", 44d);
      field(s, "showDisabled", false);
      call(s, "layout");
      double scroll = (double) field(s, "scroll"), side = (double) field(s, "sideScroll");
      test(
          "settings links expose every associated disabled binding",
          () -> {
            invoke(s, "linkedControls", Setting.class, feature);
            check(field(s, "page").equals("Controls"), "not Controls");
            check(visible(s).contains(key), "disabled related control hidden");
            check(
                visible(s).size() == ControlLinks.controls(s.session(), feature).size(),
                "extra controls included");
            check(!(boolean) field(s, "showDisabled"), "global disabled filter changed");
          });
      test(
          "nested navigation Back restores settings and controls positions",
          () -> {
            invoke(s, "related", Setting.class, key);
            check(field(s, "page").equals("Camera"), "not Camera");
            check(visible(s).size() > 1 && visible(s).stream().allMatch(v -> v.id.startsWith("power_camera:config.")),
                "unrelated camera settings are visible");
            check(visible(s).contains(find(s.session(), "power_camera:config.speed")), "related speed missing");
            check(visible(s).contains(find(s.session(), "power_camera:config.collision")), "related collisions missing");
            ((ScreenInput) (Object) s).power$key('\0', Keyboard.KEY_ESCAPE);
            check(
                field(s, "page").equals("Controls")
                    && !((List<?>) field(s, "relatedIds")).isEmpty(),
                "related list lost");
            int x = (int) call(s, "left");
            ((ScreenInput) (Object) s).power$click(x + 20, 35, 0);
            check(
                field(s, "page").equals("Camera")
                    && field(s, "scroll").equals(scroll)
                    && field(s, "sideScroll").equals(side),
                "original position lost");
            check(((Deque<?>) field(s, "history")).isEmpty(), "history not popped");
          });
      test(
          "Back preserves search collapse state and unsaved edits",
          () -> {
            TextInput search = (TextInput) field(s, "search");
            search.setText("freecam");
            ((Set<String>) field(s, "collapsed")).add("Camera/Free look");
            call(s, "layout");
            invoke(s, "linkedControls", Setting.class, feature);
            key.value = new JsonPrimitive(0);
            call(s, "back");
            check(search.text().equals("freecam"), "query lost");
            check(
                ((Set<?>) field(s, "collapsed")).contains("Camera/Free look"),
                "collapsed group lost");
            check(key.value.getAsInt() == 0, "draft edit lost");
          });
      test(
          "Back preserves fixed conflict filters",
          () -> {
            field(s, "page", "Controls");
            ((TextInput) field(s, "search")).setText("");
            List<String> fixed = List.of(key.id, "keys.key.forward");
            field(s, "conflictIds", fixed);
            call(s, "layout");
            invoke(s, "related", Setting.class, key);
            call(s, "back");
            check(field(s, "conflictIds").equals(fixed), "conflicts changed");
          });
      test("related settings match their reverse links and exclude unrelated rows", () -> {
        for (Setting binding : s.session().settings()) {
          if (binding.kind != Setting.Kind.KEY || ControlLinks.related(s.session(), binding) == null) continue;
          invoke(s, "related", Setting.class, binding);
          List<Setting> listed = visible(s);
          check(!listed.isEmpty(), "empty related settings: " + binding.id);
          check(new HashSet<>(listed).equals(new HashSet<>(ControlLinks.settings(s.session(),binding))), "incorrect filtered rows");
          for (Setting setting : listed) check(ControlLinks.controls(s.session(),setting).contains(binding), "asymmetric link");
          check(new HashSet<>((List<?>)call(s,"resetTargets")).equals(new HashSet<>(listed)), "reset touches hidden settings");
          call(s,"back");
        }
      });
      test("related reset preserves unrelated drafts and Back restores the filter", () -> {
        invoke(s,"related",Setting.class,key);
        Setting related = find(s.session(),"power_camera:config.enabled");
        Setting unrelated = find(s.session(),"tweaks.freeLook");
        var oldRelated = related.value.deepCopy(); var oldUnrelated = unrelated.value.deepCopy();
        related.value = new JsonPrimitive(!related.defaultValue.getAsBoolean());
        unrelated.value = new JsonPrimitive(!unrelated.defaultValue.getAsBoolean());
        var draft = unrelated.value.deepCopy();
        ((ScreenInput)(Object)s).power$click((int)call(s,"origin")+12,(int)call(s,"footerY")+8,0);
        check((boolean)field(s,"confirmReset"),"reset dialog missing");
        ((ScreenInput)(Object)s).power$click(s.width/2-60,s.height/2+20,0);
        check(related.value.equals(related.defaultValue),"listed setting not reset");
        check(unrelated.value.equals(draft),"hidden setting reset");
        related.value=oldRelated;unrelated.value=oldUnrelated;
        call(s,"back");
      });
      test(
          "link hit areas and Back render at small and ultrawide sizes",
          () -> {
            for (int[] size : new int[][] {{320, 240}, {427, 240}, {854, 480}, {1280, 360}}) {
              PowerOptionsScreen small = new PowerOptionsScreen(parent);
              small.init(mc, size[0], size[1]);
              Setting target = find(small.session(), "tweaks.flexible.enabled");
              field(small, "page", target.page);
              call(small, "layout");
              Object row =
                  ((List<?>) field(small, "rows"))
                      .stream()
                          .filter(
                              r -> {
                                try {
                                  return call(r, "setting") == target;
                                } catch (Exception e) {
                                  throw new RuntimeException(e);
                                }
                              })
                          .findFirst()
                          .orElseThrow();
              field(small, "scroll", (double) (int) call(row, "y"));
              int x = (int) invoke(small, "controlLeft", row.getClass(), row) + 8;
              int y = (int) call(small, "top") + ((int) call(row, "height") == 40 ? 16 : 2) + 8;
              ((ScreenInput) (Object) small).power$click(x, y, 0);
              check(!((List<?>) field(small, "relatedIds")).isEmpty(), "link miss at " + size[0]);
              while (GL11.glGetError() != GL11.GL_NO_ERROR) {}
              small.render(-1, -1, 0);
              check(GL11.glGetError() == GL11.GL_NO_ERROR, "render error");
              ((ScreenInput) (Object) small).power$click((int) call(small, "left") + 15, 35, 0);
              check(field(small, "page").equals(target.page), "Back miss");
            }
          });
    } finally {
      s.session().discard();
      mc.setScreen(parent);
    }
    log("NAVIGATION FAILURES " + failures);
  }

  private static void target(Minecraft mc, Pos pos, int face) {
    mc.field_2823 =
        new class_27(
            pos.x(),
            pos.y(),
            pos.z(),
            face,
            Vec3d.createCached(pos.x() + .5, pos.y() + .5, pos.z() + .5));
  }

  private static BlockValue at(BlockValue value, Pos target) throws Exception {
    Method m = ContainerCarry.class.getDeclaredMethod("at", BlockValue.class, Pos.class);
    m.setAccessible(true);
    return (BlockValue) m.invoke(null, value, target);
  }

  private static Object carryField(String name) throws Exception {
    Field f = ContainerCarry.class.getDeclaredField(name);
    f.setAccessible(true);
    return f.get(null);
  }

  private static void reload(Minecraft mc) throws Exception {
    Field f = ContainerCarry.class.getDeclaredField("world");
    f.setAccessible(true);
    f.set(null, null);
    ContainerCarry.tick(mc);
    check(!(boolean) carryField("failed"), "recovery stopped");
  }

  private static Pos heldTarget;
  private static BlockValue heldValue;

  public static void hold(Minecraft mc, boolean release) throws Exception {
    mc.setScreen(null);
    MinecraftWorld blocks = new MinecraftWorld(mc.world);
    if (!release) {
      check(org.lwjgl.input.Mouse.isButtonDown(1), "physical Use must be held");
      VisualSettings options = VisualConfig.copy();
      options.containerCarry = true;
      VisualConfig.preview(options);
      mc.player.inventory.main[mc.player.inventory.selectedSlot] = null;
      mc.player.field_161.field_2536 = true;
      Pos source = new Pos((int) Math.floor(mc.player.x) + 2, 100, (int) Math.floor(mc.player.z));
      heldTarget = source.add(0, 0, 2);
      mc.player.method_1340(source.x() - 1.5, 101, source.z() + .5);
      for (int dx = -1; dx <= 1; dx++)
        for (int dz = -1; dz <= 3; dz++) {
          Pos p = source.add(dx, 0, dz);
          mc.world.method_214(p.x() >> 4, p.z() >> 4);
          blocks.set(p, BlockValue.AIR);
        }
      blocks.set(source, new BlockValue(54, 2));
      ((Inventory) mc.world.method_1777(source.x(), source.y(), source.z()))
          .setStack(0, new ItemStack(264, 31, 0));
      heldValue = blocks.get(source);
      target(mc, source, 1);
      test(
          "held Use picks up once without immediately placing",
          () -> {
            check(ContainerCarry.click(mc, 1) && ContainerCarry.carrying(), "pickup failed");
            blocks.set(heldTarget.add(0, -1, 0), new BlockValue(1, 0));
            target(mc, heldTarget.add(0, -1, 0), 1);
            for (int i = 0; i < 20; i++)
              check(
                  ContainerCarry.click(mc, 1) && ContainerCarry.carrying(),
                  "repeat placed container");
            check(blocks.get(heldTarget).id == 0, "repeat wrote target");
          });
    } else {
      check(!org.lwjgl.input.Mouse.isButtonDown(1), "physical Use must be released");
      test(
          "release permits the next placement with original inventory",
          () -> {
            target(mc, heldTarget.add(0, -1, 0), 1);
            check(
                ContainerCarry.click(mc, 1) && !ContainerCarry.carrying(),
                "release did not permit placement");
            check(blocks.get(heldTarget).same(at(heldValue, heldTarget)), "inventory differs");
          });
    }
    mc.player.field_161.field_2536 = false;
  }

  public static void resumed(Minecraft mc) throws Exception {
    failures = 0;
    mc.setScreen(null);
    ContainerCarry.tick(mc);
    test(
        "carried container survives complete client restart",
        () -> {
          CarryJournal saved = CarryJournal.read((Path) carryField("file"));
          check(ContainerCarry.carriedCount() == saved.size(), "saved carry not restored");
          check(saved.phase.equals("held"), "unexpected stage");
          MinecraftWorld blocks = new MinecraftWorld(mc.world);
          Pos destination = saved.source.add(0, 0, 3), other = destination.add(1, 0, 0);
          check(
              blocks.get(destination).id == 0 && blocks.get(other).id == 0, "test space occupied");
          blocks.set(destination.add(0, -1, 0), new BlockValue(1, 0));
          mc.player.method_1340(destination.x() - 1.5, destination.y() + 1, destination.z() - 1.5);
          mc.player.yaw = 0;
          target(mc, destination.add(0, -1, 0), 1);
          check(
              ContainerCarry.click(mc, 1) && !ContainerCarry.carrying(),
              "restored pair not placed");
          check(
              blocks.get(destination).same(at(saved.value(0), destination))
                  && (saved.size() == 1 || blocks.get(other).same(at(saved.value(1), other))),
              "saved inventory changed");
        });
    log("RESTART CARRY FAILURES " + failures);
  }

  public static void carry(Minecraft mc, boolean demo) throws Exception {
    failures = 0;
    check(mc.world != null, "world required");
    mc.setScreen(null);
    VisualSettings original = VisualConfig.copy(), enabled = VisualConfig.copy();
    enabled.containerCarry = true;
    VisualConfig.preview(enabled);
    double px = mc.player.x, py = mc.player.y, pz = mc.player.z;
    float yaw = mc.player.yaw;
    ItemStack[] inventory = mc.player.inventory.main.clone();
    int slot = mc.player.inventory.selectedSlot;
    int x = (((int) Math.floor(px)) >> 4) * 16 + 15, z = (int) Math.floor(pz);
    Pos a = new Pos(x, 100, z), target = new Pos(x, 100, z + 3);
    MinecraftWorld blocks = new MinecraftWorld(mc.world);
    Map<Pos, BlockValue> backup = new LinkedHashMap<>();
    for (int dx = -2; dx <= 3; dx++)
      for (int dz = -2; dz <= 6; dz++)
        for (int dy = -1; dy <= 1; dy++) {
          Pos p = a.add(dx, dy, dz);
          mc.world.method_214(p.x() >> 4, p.z() >> 4);
          backup.put(p, blocks.get(p));
          blocks.set(p, BlockValue.AIR);
        }
    mc.player.method_1340(x - 1.5, 101, z + 1.5);
    mc.player.inventory.selectedSlot = 0;
    mc.player.inventory.main[0] = null;
    mc.player.field_161.field_2536 = true;
    try {
      for (int axis = 0; axis < 2; axis++)
        for (int angle : new int[] {0, 90, 180, 270}) {
          Pos b = a.add(axis == 0 ? 1 : 0, 0, axis == 1 ? 1 : 0),
              t2 = target.add(angle % 180 == 0 ? 1 : 0, 0, angle % 180 == 0 ? 0 : 1);
          mc.player.yaw = angle;
          blocks.set(a, new BlockValue(54, 2));
          blocks.set(b, new BlockValue(54, 2));
          for (int half = 0; half < 2; half++) {
            Pos p = half == 0 ? a : b;
            Inventory chest = (Inventory) mc.world.method_1777(p.x(), p.y(), p.z());
            for (int i = 0; i < 27; i++)
              chest.setStack(
                  i,
                  new ItemStack(
                      i % 2 == 0 ? 264 : 257,
                      i % 2 == 0 ? i + 1 + half : 1,
                      i % 2 == 0 ? 0 : i + half * 30));
            chest.markDirty();
          }
          BlockValue first = blocks.get(a), second = blocks.get(b);
          test(
              "legacy double pickup recovery axis " + axis + " rotation " + angle,
              () -> {
                // Simulate a journal written before single-half pickup replaced pair pickup.
                ContainerCarry.tick(mc);
                new CarryJournal(a, mc.player.dimensionId, first, b, second)
                    .write((Path) carryField("file"));
                reload(mc);
                check(ContainerCarry.carriedCount() == 2, "legacy pair not recovered");
                check(blocks.get(a).id == 0 && blocks.get(b).id == 0, "half remained");
                CarryJournal journal = CarryJournal.read((Path) carryField("file"));
                check(
                    journal.value(0).same(first) && journal.value(1).same(second),
                    "journal differs");
              });
          test(
              "double placement refuses occupied second half",
              () -> {
                blocks.set(target.add(0, -1, 0), new BlockValue(1, 0));
                blocks.set(t2, new BlockValue(1, 0));
                target(mc, target.add(0, -1, 0), 1);
                check(ContainerCarry.click(mc, 1) && ContainerCarry.carrying(), "carry cleared");
                check(blocks.get(target).id == 0 && blocks.get(t2).id == 1, "partial placement");
                blocks.set(t2, BlockValue.AIR);
              });
          if (axis == 0 && angle == 0)
            test(
                "double carry survives reload and retains pose",
                () -> {
                  reload(mc);
                  check(ContainerCarry.carriedCount() == 2, "pair lost");
                  BipedEntityModel model = new BipedEntityModel();
                  ((CarryPose) model).power$carrying(true);
                  model.setAngles(0, 0, 0, 0, 0, .0625f);
                  check(
                      Math.abs(model.rightArm.pitch + .65) < .001
                          && Math.abs(model.leftArm.pitch + .65) < .001,
                      "pose missing");
                  while (GL11.glGetError() != GL11.GL_NO_ERROR) {}
                  CarryRenderer.firstPerson(mc, .5f);
                  CarryRenderer.thirdPerson(mc, mc.player, .5f);
                  check(GL11.glGetError() == GL11.GL_NO_ERROR, "carry GL error");
                });
          test(
              "all 54 slots preserved after rotation " + angle,
              () -> {
                target(mc, target.add(0, -1, 0), 1);
                check(ContainerCarry.click(mc, 1) && !ContainerCarry.carrying(), "not placed");
                check(
                    blocks.get(target).same(at(first, target))
                        && blocks.get(t2).same(at(second, t2)),
                    "NBT changed");
              });
          blocks.set(target, BlockValue.AIR);
          blocks.set(t2, BlockValue.AIR);
        }
      for (boolean removing : new boolean[] {true, false})
        test(
            "recover partial double " + (removing ? "pickup" : "placement"),
            () -> {
              Pos b = a.add(1, 0, 0), t2 = target.add(1, 0, 0);
              blocks.set(a, new BlockValue(54, 2));
              blocks.set(b, new BlockValue(54, 2));
              ((Inventory) mc.world.method_1777(a.x(), a.y(), a.z()))
                  .setStack(0, new ItemStack(264, 17, 0));
              ((Inventory) mc.world.method_1777(b.x(), b.y(), b.z()))
                  .setStack(26, new ItemStack(257, 1, 94));
              BlockValue one = blocks.get(a), two = blocks.get(b);
              CarryJournal journal = new CarryJournal(a, mc.player.dimensionId, one, b, two);
              Path file = (Path) carryField("file");
              if (removing) {
                journal.write(file);
                blocks.set(a, BlockValue.AIR);
              } else {
                blocks.set(a, BlockValue.AIR);
                blocks.set(b, BlockValue.AIR);
                journal.targets(target, t2, mc.player.dimensionId);
                journal.phase = "placing";
                journal.write(file);
                blocks.set(target, one);
              }
              mc.world.method_195(true, null);
              reload(mc);
              if (removing) {
                check(
                    ContainerCarry.carriedCount() == 2
                        && blocks.get(a).id == 0
                        && blocks.get(b).id == 0,
                    "partial pickup recovery failed");
                mc.player.yaw = 0;
                target(mc, target.add(0, -1, 0), 1);
                check(ContainerCarry.click(mc, 1), "place failed");
              }
              check(
                  !ContainerCarry.carrying()
                      && blocks.get(target).same(at(one, target))
                      && blocks.get(t2).same(at(two, t2)),
                  "partial recovery lost contents");
              blocks.set(target, BlockValue.AIR);
              blocks.set(t2, BlockValue.AIR);
            });
      if (demo) {
        blocks.set(a, new BlockValue(54, 2));
        blocks.set(a.add(1, 0, 0), new BlockValue(54, 2));
        target(mc, a, 1);
        ContainerCarry.click(mc, 1);
        mc.player.yaw = -90;
        mc.player.pitch = 0;
        mc.player.field_161.field_2536 = false;
      }
    } finally {
      if (!ContainerCarry.carrying())
        for (var e : backup.entrySet()) blocks.set(e.getKey(), e.getValue());
      if (!demo) {
        mc.player.method_1340(px, py, pz);
        mc.player.yaw = yaw;
        mc.player.inventory.main = inventory;
        mc.player.inventory.selectedSlot = slot;
        VisualConfig.preview(original);
      }
      mc.player.field_161.field_2536 = false;
    }
    log("DOUBLE CARRY FAILURES " + failures);
  }
}
