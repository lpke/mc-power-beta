package local.luke.building.validation;

import static local.luke.building.validation.ValidationRun.*;

import java.lang.reflect.*;
import java.util.*;
import local.luke.tweaks.camera.*;
import local.luke.tweaks.hotbar.*;
import local.luke.worldedit.*;
import local.luke.worldedit.config.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.WorldProperties;

/** Executed only by the validation JAR in a cloned Prism instance. */
public final class FeatureValidation {
  private interface Check {
    void run() throws Exception;
  }

  private static int failures;

  private static void test(String name, Check action) {
    try {
      action.run();
      log("PASS features " + name);
    } catch (Throwable e) {
      failures++;
      log("FAIL features " + name + " " + e);
      e.printStackTrace();
    }
  }

  static Class<?> type(String name) throws Exception {
    return Class.forName("local.luke.creative." + name);
  }

  static Object call(String owner, String method, Class<?>[] types, Object... args)
      throws Exception {
    return type(owner).getMethod(method, types).invoke(null, args);
  }

  static void mode(Minecraft mc, String name) throws Exception {
    Class<?> modes = type("api.GameMode");
    Object value = Enum.valueOf((Class) modes, name);
    check(
        Boolean.TRUE.equals(
            call("Modes", "change", new Class[] {Minecraft.class, modes}, mc, value)),
        "mode change " + name);
  }

  private static Object state(Minecraft mc, String method) throws Exception {
    return type("api.ModePlayer").getMethod(method).invoke(mc.player);
  }

  static Object settings() throws Exception {
    return call("config.Config", "current", new Class[] {})
        .getClass()
        .getMethod("copy")
        .invoke(call("config.Config", "current", new Class[] {}));
  }

  static void apply(Object settings) throws Exception {
    call("config.Config", "apply", new Class[] {settings.getClass()}, settings);
  }

  static void value(Object settings, String key, Object value) throws Exception {
    settings.getClass().getField(key).set(settings, value);
  }

  private static void drain(Minecraft mc) {
    var editor = WorldEditBeta.editor(mc);
    for (int i = 0; i < 100 && editor.engine.busy(); i++) WorldEditBeta.tick(mc);
    check(!editor.engine.busy(), "edit completed");
  }

  public static void run(Minecraft mc) throws Exception {
    failures = 0;
    Object original = settings();
    test(
        "BHCreative alias and native controls",
        () -> {
          check(FabricLoader.getInstance().isModLoaded("lpkecreative"), "fork loaded");
          check(FabricLoader.getInstance().isModLoaded("bhcreative"), "compatibility alias loaded");
          for (String name : List.of("SPRINT", "PICKER", "MODIFIER")) {
            Object key = type("Keys").getField(name).get(null);
            check(Arrays.asList(mc.options.allKeys).contains(key), "key registered " + name);
          }
          check(
              net.minecraft.client.resource.language.TranslationStorage.getInstance()
                  .get("key.omnilook.toggle")
                  .equals("Free Look"),
              "translated controls label");
        });
    test(
        "creative and survival use separate reach",
        () -> {
          reset(mc);
          mode(mc, "SURVIVAL");
          check(mc.interactionManager.method_1715() == 4, "Beta survival reach");
          mode(mc, "CREATIVE");
          check(mc.interactionManager.method_1715() == 5, "modern creative reach");
          Object s = settings();
          value(s, "blockReach", 65);
          apply(s);
          check(mc.interactionManager.method_1715() == 6.5f, "configured reach");
          apply(original);
        });
    test(
        "independent entity reach and block occlusion",
        () -> {
          reset(mc);
          mode(mc, "CREATIVE");
          flight(mc, true);
          aim(mc, .5, 105, .5, .5, 105, 8);
          Object cfg = settings();
          value(cfg, "blockReach", 30);
          value(cfg, "entityReach", 65);
          apply(cfg);
          var cow = net.minecraft.class_206.method_732("Cow", mc.world);
          cow.method_1340(.5, 104, 5.5);
          mc.world.method_210(cow);
          try {
            mc.field_2818.method_1838(1);
            check(
                mc.field_2823 != null && mc.field_2823.field_1989 == cow,
                "entity beyond block reach selectable");
            set(mc.world, 0, 105, 4, 1, 0);
            mc.field_2818.method_1838(1);
            check(
                mc.field_2823 == null,
                "out of reach wall blocks entity without becoming selectable");
            set(mc.world, 0, 105, 4, 0, 0);
            value(cfg, "entityReach", 30);
            apply(cfg);
            mc.field_2818.method_1838(1);
            check(mc.field_2823 == null, "entity beyond configured reach rejected");
          } finally {
            cow.markDead();
            apply(original);
          }
        });
    test(
        "spectator noclip damage and interactions",
        () -> {
          reset(mc);
          mc.player.method_1341(.5, 101, .5, 0, 0);
          mode(mc, "SPECTATOR");
          check(mc.player.field_1642 && (Boolean) state(mc, "lpke_isSpectator"), "noclip enabled");
          int health = mc.player.health;
          mc.player.damage(null, 10);
          check(mc.player.health == health, "damage prevented");
          set(mc.world, 1, 101, 0, 1, 0);
          set(mc.world, 1, 102, 0, 1, 0);
          mc.player.move(2, 0, 0);
          check(mc.player.x > 2, "moved through wall");
          stack(mc, 1, 0);
          int count = mc.player.inventory.main[0].count;
          check(
              !mc.interactionManager.method_1713(
                  mc.player, mc.world, mc.player.inventory.main[0], 0, 100, 0, 1),
              "placement rejected");
          check(mc.player.inventory.main[0].count == count, "inventory preserved");
          mode(mc, "CREATIVE");
          check(!mc.player.field_1642, "noclip removed");
        });
    test(
        "spectator exit inside a wall finds a clear position",
        () -> {
          reset(mc);
          mc.player.method_1341(.5, 101, .5, 0, 0);
          mode(mc, "SPECTATOR");
          set(mc.world, 3, 101, 0, 1, 0);
          set(mc.world, 3, 102, 0, 1, 0);
          mc.player.method_1341(3.5, 101, .5, 0, 0);
          mode(mc, "SURVIVAL");
          check(mc.player.x < 1, "returned to safe entry");
          check(
              !mc.player.field_1642
                  && mc.world.method_190(mc.player, mc.player.boundingBox).isEmpty(),
              "safe collision box");
        });
    test(
        "gamemode and spectator speed survive NBT",
        () -> {
          reset(mc);
          mode(mc, "SPECTATOR");
          type("api.ModePlayer")
              .getMethod("lpke_spectatorSpeed", float.class)
              .invoke(mc.player, .12f);
          NbtCompound tag = new NbtCompound();
          mc.player.write(tag);
          check(tag.getString("LpkeGameMode").equals("SPECTATOR"), "saved spectator mode");
          mode(mc, "CREATIVE");
          mc.player.read(tag);
          check(
              (Boolean) state(mc, "lpke_isSpectator") && mc.player.field_1642,
              "restored spectator");
          check(
              Math.abs((Float) state(mc, "lpke_spectatorSpeed") - .12f) < .00001, "restored speed");
          mode(mc, "CREATIVE");
        });
    test(
        "native flight movement and zero glide",
        () -> {
          reset(mc);
          mode(mc, "CREATIVE");
          flight(mc, true);
          mc.player.method_1341(.5, 105, .5, 0, 0);
          mc.player.field_161.field_2532 = 0;
          mc.player.field_161.field_2533 = 1;
          mc.player.field_161.field_2535 = false;
          mc.player.field_161.field_2536 = false;
          check(
              Boolean.TRUE.equals(
                  call("FlightController", "travel", new Class[] {PlayerEntity.class}, mc.player)),
              "flight hook active");
          check(mc.player.z > .54 && mc.player.velocityZ > 0, "forward motion");
          Object s = settings();
          value(s, "glide", 0);
          apply(s);
          mc.player.field_161.field_2533 = 0;
          mc.player.velocityY = .2;
          double z = mc.player.z, y = mc.player.y;
          call("FlightController", "travel", new Class[] {PlayerEntity.class}, mc.player);
          check(
              mc.player.z == z
                  && mc.player.y == y
                  && mc.player.velocityZ == 0
                  && mc.player.velocityY == 0,
              "no residual movement");
          apply(original);
        });
    test(
        "destroy slot deletes cursor and shift clears full inventory",
        () -> {
          reset(mc);
          mode(mc, "CREATIVE");
          mc.player.inventory.setCursorStack(new ItemStack(1, 12, 0));
          call(
              "InventoryActions",
              "destroy",
              new Class[] {PlayerEntity.class, boolean.class},
              mc.player,
              false);
          check(
              mc.player.inventory.getCursorStack() == null && mc.player.inventory.main[0] != null,
              "only cursor removed");
          mc.player.inventory.armor[0] = new ItemStack(301, 1, 0);
          call(
              "InventoryActions",
              "destroy",
              new Class[] {PlayerEntity.class, boolean.class},
              mc.player,
              true);
          check(
              Arrays.stream(mc.player.inventory.main).allMatch(Objects::isNull),
              "main inventory cleared");
          check(
              Arrays.stream(mc.player.inventory.armor).allMatch(Objects::isNull), "armor cleared");
          mode(mc, "SURVIVAL");
          stack(mc, 1, 0);
          call(
              "InventoryActions",
              "destroy",
              new Class[] {PlayerEntity.class, boolean.class},
              mc.player,
              true);
          check(mc.player.inventory.main[0] != null, "survival protected");
        });
    test(
        "gamemode commands support modern names and local selectors",
        () -> {
          reset(mc);
          call(
              "command.CreativeCommands",
              "execute",
              new Class[] {String.class},
              "/gamemode spectator @s");
          check((Boolean) state(mc, "lpke_isSpectator"), "spectator command");
          call(
              "command.CreativeCommands",
              "execute",
              new Class[] {String.class},
              "/gamemode adventure");
          check((Boolean) state(mc, "lpke_isSpectator"), "invalid mode leaves state intact");
          call(
              "command.CreativeCommands",
              "execute",
              new Class[] {String.class},
              "/gamemode creative");
          check(state(mc, "lpke_mode").toString().equals("CREATIVE"), "creative command");
        });
    test(
        "WorldEdit master world and creative precedence",
        () -> {
          reset(mc);
          Settings s = WorldEditBeta.settings();
          s.creativeOnly = true;
          WorldEditBeta.apply(s);
          mode(mc, "SURVIVAL");
          check(!WorldEditBeta.available(mc), "survival gated");
          mode(mc, "CREATIVE");
          check(WorldEditBeta.available(mc), "creative allowed");
          mode(mc, "SPECTATOR");
          check(!WorldEditBeta.available(mc), "spectator gated");
          WorldEditBeta.worldOverride(mc, WorldOverride.ENABLED);
          check(WorldEditBeta.available(mc), "world override takes priority");
          s.enabled = false;
          WorldEditBeta.apply(s);
          check(!WorldEditBeta.available(mc), "master has highest priority");
          s.enabled = true;
          s.creativeOnly = false;
          WorldEditBeta.apply(s);
          WorldEditBeta.worldOverride(mc, WorldOverride.DISABLED);
          check(!WorldEditBeta.available(mc), "world disable still applies");
          reset(mc);
        });
    test(
        "WorldEdit override saves and copies with world properties",
        () -> {
          reset(mc);
          WorldEditBeta.worldOverride(mc, WorldOverride.DISABLED);
          WorldProperties props = mc.world.method_262();
          WorldProperties saved = new WorldProperties(props.asNbt());
          check(
              ((WorldSettings) saved).worldedit$override() == WorldOverride.DISABLED,
              "saved override");
          check(
              ((WorldSettings) new WorldProperties(props)).worldedit$override()
                  == WorldOverride.DISABLED,
              "dimension copy");
          reset(mc);
        });
    test(
        "disabling WorldEdit rolls back an unfinished edit",
        () -> {
          reset(mc);
          var editor = WorldEditBeta.editor(mc);
          editor.engine.submit(
              List.of(
                  new local.luke.worldedit.core.Pos(0, 101, 0),
                  new local.luke.worldedit.core.Pos(1, 101, 0)),
              p -> new local.luke.worldedit.core.BlockValue(5, 0, null));
          editor.engine.tick(4);
          check(mc.world.getBlockId(0, 101, 0) == 5, "partially written");
          Settings s = WorldEditBeta.settings();
          s.enabled = false;
          WorldEditBeta.apply(s);
          WorldEditBeta.tick(mc);
          check(
              mc.world.getBlockId(0, 101, 0) == 0 && !editor.engine.busy(),
              "rollback completed despite disabled access");
          reset(mc);
        });
    test(
        "up places an undoable platform and ascend descend find floors",
        () -> {
          reset(mc);
          mc.player.method_1341(.5, 101, .5, 0, 0);
          WorldEditBeta.command(mc, "//up 3");
          drain(mc);
          check(mc.world.getBlockId(0, 103, 0) == 20, "glass platform");
          check(Math.abs(mc.player.boundingBox.minY - 104) < .01, "teleported upward");
          WorldEditBeta.command(mc, "//undo");
          drain(mc);
          check(mc.world.getBlockId(0, 103, 0) == 0, "platform undo");
          set(mc.world, 0, 105, 0, 1, 0);
          mc.player.method_1341(.5, 101, .5, 0, 0);
          WorldEditBeta.command(mc, "/ascend");
          check(Math.abs(mc.player.boundingBox.minY - 106) < .01, "ascend floor");
          WorldEditBeta.command(mc, "/descend");
          check(Math.abs(mc.player.boundingBox.minY - 101) < .01, "descend floor");
          WorldEditBeta.command(mc, "//up 127");
          check(Math.abs(mc.player.boundingBox.minY - 101) < .01, "height rejected safely");
        });
    test(
        "hotbar swap preserves stacks and protects container cursors",
        () -> {
          reset(mc);
          for (int i = 0; i < 36; i++)
            mc.player.inventory.main[i] = new ItemStack(35, i + 1, i % 16);
          ItemStack[] before = mc.player.inventory.main.clone();
          Hotbars.swap(mc, 1);
          for (int i = 0; i < 9; i++)
            check(mc.player.inventory.main[i] == before[18 + i], "row stack reference " + i);
          check(mc.player.inventory.selectedSlot == 0, "selected slot unchanged");
          mc.player.inventory.setCursorStack(new ItemStack(1, 1, 0));
          ItemStack keep = mc.player.inventory.main[0];
          Hotbars.swap(mc, 2);
          check(mc.player.inventory.main[0] == keep, "cursor blocks swap");
          mc.player.inventory.setCursorStack(null);
        });
    test(
        "creative item-use rollback handles nesting and exceptions",
        () -> {
          reset(mc);
          mode(mc, "CREATIVE");
          ItemStack stack = new ItemStack(280, 25, 0);
          ItemUseFault.enabled = true;
          try {
            stack.method_701(mc.player, mc.world, 0, 100, 0, 1);
            check(stack.count == 25, "outer stack preserved independently of nested stack");
            ItemUseFault.throwing = true;
            boolean caught = false;
            try {
              stack.method_701(mc.player, mc.world, 0, 100, 0, 1);
            } catch (IllegalStateException expected) {
              caught = true;
            }
            check(caught && stack.count == 25, "exception restores creative stack");
          } finally {
            ItemUseFault.enabled = false;
            ItemUseFault.throwing = false;
          }
        });
    test(
        "hotbar transactions preserve native custom NBT and save data",
        () -> {
          reset(mc);
          ItemStack special = new ItemStack(278, 1, 35);
          NbtCompound custom =
              (NbtCompound) special.getClass().getMethod("getStationNbt").invoke(special);
          custom.putString("lpke_validation", "Keep this exact custom payload");
          custom.putLong("number", 1234567890123L);
          mc.player.inventory.main[18] = special;
          ItemStack armor = new ItemStack(301, 1, 8);
          mc.player.inventory.armor[0] = armor;
          long start = System.nanoTime();
          Hotbars.swap(mc, 1);
          check(mc.player.inventory.main[0] == special, "same stack reference after atomic commit");
          NbtCompound after =
              (NbtCompound) special.getClass().getMethod("getStationNbt").invoke(special);
          check(
              after.getLong("number") == 1234567890123L && special.getDamage() == 35,
              "custom NBT and damage intact");
          check(mc.player.inventory.armor[0] == armor, "armor untouched");
          NbtCompound player = new NbtCompound();
          mc.player.write(player);
          mc.player.read(player);
          ItemStack restored = mc.player.inventory.main[0];
          NbtCompound saved =
              (NbtCompound) restored.getClass().getMethod("getStationNbt").invoke(restored);
          check(
              saved.getString("lpke_validation").equals("Keep this exact custom payload"),
              "native inventory save restores custom NBT");
          log(
              "HOTBAR backed-up swap and native save elapsed_ms="
                  + (System.nanoTime() - start) / 1000000);
        });
    test(
        "first-person free look follows third-person camera by default",
        () -> {
          reset(mc);
          var settings = local.luke.tweaks.config.Config.current().copy();
          settings.freeLookPerspective = Perspective.FIRST_PERSON;
          settings.freeLookFollowThirdPerson = true;
          local.luke.tweaks.config.Config.apply(settings);
          mc.options.thirdPerson = true;
          FreeLook.begin(mc);
          check(mc.options.thirdPerson, "third person retained");
          FreeLook.reset(mc);
          check(mc.options.thirdPerson, "third person restored");
          mc.options.thirdPerson = false;
          FreeLook.begin(mc);
          check(!mc.options.thirdPerson, "first person retained");
          FreeLook.reset(mc);
        });
    apply(original);
    reset(mc);
    mode(mc, "CREATIVE");
    flight(mc, true);
    log("FEATURES DONE failures=" + failures);
  }

  public static boolean command(Minecraft mc, String text) throws Exception {
    if (text.equals("fixes")) {
      FixValidation.run(mc);
      return true;
    }
    if (text.startsWith("sprint-mode ")) {
      Object cfg = settings();
      value(cfg, "sprintToggle", text.endsWith("toggle"));
      apply(cfg);
      log("SPRINT MODE " + text.substring(12));
      return true;
    }
    if (text.equals("features")) {
      run(mc);
      return true;
    }
    if (text.startsWith("mode ")) {
      mode(mc, text.substring(5).toUpperCase(Locale.ROOT));
      return true;
    }
    if (text.equals("config-creative")) {
      Screen screen =
          (Screen)
              type("config.SettingsScreen")
                  .getConstructor(Screen.class)
                  .newInstance(new Object[] {null});
      mc.setScreen(screen);
      return true;
    }
    if (text.startsWith("creative ")) {
      call("command.CreativeCommands", "execute", new Class[] {String.class}, text.substring(9));
      return true;
    }
    if (text.startsWith("flight-input ")) {
      reset(mc);
      mode(mc, "CREATIVE");
      flight(mc, true);
      mc.options.thirdPerson = false;
      mc.options.debugHud = false;
      mc.player.method_1341(.5, 105, .5, 0, 0);
      mc.player.prevYaw = mc.player.yaw;
      mc.player.prevPitch = 0;
      Object cfg = settings();
      value(cfg, "glide", Integer.parseInt(text.substring(13)));
      apply(cfg);
      log("FLIGHT INPUT READY");
      return true;
    }
    if (text.equals("hotbar-input")) {
      reset(mc);
      local.luke.tweaks.config.Config.update(s -> {
        s.hotbar.swap = true;
        s.hotbar.scroll = true;
        s.hotbar.numberRowKeys = true;
      });
      mode(mc, "CREATIVE");
      flight(mc, true);
      mc.options.thirdPerson = false;
      mc.player.method_1341(.5, 105, .5, 0, 0);
      for (int i = 0; i < 36; i++)
        mc.player.inventory.main[i] = new ItemStack(35, i / 9 + 1, i / 9 * 4);
      Hotbars.BASE.code = org.lwjgl.input.Keyboard.KEY_H;
      Hotbars.SCROLL.code = org.lwjgl.input.Keyboard.KEY_J;
      Hotbars.ROWS[0].code = org.lwjgl.input.Keyboard.KEY_K;
      Hotbars.ROWS[1].code = org.lwjgl.input.Keyboard.KEY_L;
      Hotbars.ROWS[2].code = org.lwjgl.input.Keyboard.KEY_O;
      log("HOTBAR INPUT READY");
      return true;
    }
    if (text.equals("hotbar-mouse")) {
      Hotbars.ROWS[0].code = -99;
      log("HOTBAR MOUSE READY");
      return true;
    }
    if (text.equals("hotbar-state")) {
      log(
          "HOTBAR STATE "
              + java.util.stream.IntStream.range(0, 4)
                  .mapToObj(
                      i -> {
                        var stack = mc.player.inventory.main[i * 9];
                        return stack == null ? "empty" : stack.count + ":" + stack.getDamage();
                      })
                  .toList()
              + " slot="
              + mc.player.inventory.selectedSlot
              + " row="
              + Hotbars.row());
      return true;
    }
    if (text.equals("feature-state")) {
      log(
          "FEATURE STATE mode="
              + state(mc, "lpke_mode")
              + " pos="
              + mc.player.x
              + ","
              + mc.player.y
              + ","
              + mc.player.z
              + " velocity="
              + mc.player.velocityX
              + ","
              + mc.player.velocityY
              + ","
              + mc.player.velocityZ
              + " noclip="
              + mc.player.field_1642
              + " speed="
              + state(mc, "lpke_spectatorSpeed")
              + " flying="
              + Class.forName("paulevs.bhcreative.interfaces.CreativePlayer")
                  .getMethod("creative_isFlying")
                  .invoke(mc.player)
              + " ground="
              + mc.player.field_1623
              + " screen="
              + (mc.currentScreen == null ? "none" : mc.currentScreen.getClass().getName())
              + " limb="
              + mc.player.field_1049
              + " phase="
              + mc.player.field_1050
              + " debug="
              + mc.options.debugHud);
      return true;
    }
    return false;
  }
}
