package local.luke.building.validation;

import static local.luke.building.validation.FeatureValidation.*;
import static local.luke.building.validation.ValidationRun.*;

import java.lang.reflect.Field;
import java.util.*;
import local.luke.building.validation.mixin.ScreenInvoker;
import local.luke.tweaks.camera.FreeLook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public final class FixValidation {
  @FunctionalInterface
  private interface Check {
    void run() throws Exception;
  }

  private static int passed, failed;

  private static void test(String name, Check action) {
    try {
      action.run();
      passed++;
      log("PASS fixes " + name);
    } catch (Throwable error) {
      failed++;
      log("FAIL fixes " + name + " " + error);
      error.printStackTrace();
    }
  }

  private static Object field(Object owner, String name) throws Exception {
    Field f = owner.getClass().getDeclaredField(name);
    f.setAccessible(true);
    return f.get(owner);
  }

  private static void cycle(Minecraft mc, Screen screen) throws Exception {
    mc.setScreen(screen);
    ScreenInvoker access = (ScreenInvoker) screen;
    int pages =
        screen instanceof local.luke.worldedit.config.SettingsScreen
            ? 4
            : ((List<?>) field(screen, "pages")).size();
    Set<String> prefixes =
        Set.of(
            "Blocks per tick",
            "Slabs",
            "Restriction mode",
            "Item filter",
            "Overlay colour",
            "Overlay opacity",
            "Activation",
            "Perspective",
            "Starting row",
            "Alignment",
            "Horizontal offset",
            "Vertical offset",
            "Flight speed",
            "Flight glide",
            "Double-tap window",
            "Sprint activation",
            "Sprint speed",
            "Starting spectator speed",
            "Scroll speed step",
            "Block reach",
            "Entity reach",
            "This world",
            "Colour",
            "Opacity",
            "Line width",
            "Block limit",
            "Undo history");
    int tested = 0;
    for (int page = 0; page < pages; page++) {
      for (ButtonWidget b : List.copyOf(access.validation$buttons())) {
        if (!prefixes.contains(b.text.split(":")[0])) continue;
        for (int i = 0; i < 24; i++) {
          ButtonWidget current =
              access.validation$buttons().stream()
                  .filter(v -> v.id == b.id)
                  .findFirst()
                  .orElseThrow();
          String before = current.text;
          access.validation$click(b.x + 5, b.y + 5, 1);
          String after =
              access.validation$buttons().stream()
                  .filter(v -> v.id == b.id)
                  .findFirst()
                  .orElseThrow()
                  .text;
          check(!before.equals(after), "right-click changes " + before);
          access.validation$click(b.x + 5, b.y + 5, 0);
          check(
              before.equals(
                  access.validation$buttons().stream()
                      .filter(v -> v.id == b.id)
                      .findFirst()
                      .orElseThrow()
                      .text),
              "opposite clicks restore " + before);
          access.validation$click(b.x + 5, b.y + 5, 0);
        }
        tested++;
      }
      access.validation$button(
          access.validation$buttons().stream().filter(v -> v.id == 101).findFirst().orElseThrow());
    }
    check(tested >= 7, "all option groups reached, count=" + tested);
    mc.setScreen(null);
    log("FIXES cycled " + tested + " settings in " + screen.getClass().getName());
  }

  public static void run(Minecraft mc) throws Exception {
    passed = failed = 0;
    reset(mc);
    mode(mc, "CREATIVE");
    test(
        "creative left/right settings cycles",
        () ->
            cycle(
                mc,
                (Screen)
                    type("config.SettingsScreen")
                        .getConstructor(Screen.class)
                        .newInstance(new Object[] {null})));
    test(
        "tweaks left/right settings cycles",
        () -> cycle(mc, new local.luke.tweaks.config.SettingsScreen(null)));
    test(
        "WorldEdit left/right settings cycles",
        () -> cycle(mc, new local.luke.worldedit.config.SettingsScreen(null)));
    mc.setScreen(null);
    test(
        "switching inventory tabs preserves cursor without throwing",
        () -> {
          Screen screen = new net.minecraft.class_585(mc.player);
          mc.setScreen(screen);
          ScreenInvoker access = (ScreenInvoker) screen;
          int x = (screen.width - 176) / 2, y = (screen.height - 166) / 2;
          ItemStack item = new ItemStack(1, 17, 0);
          mc.player.inventory.setCursorStack(item);
          for (int button = 0; button < 3; button++) {
            access.validation$click(x + 185, y + 150, button);
            check(
                mc.player.inventory.getCursorStack() == item && item.count == 17,
                "survival tab preserves cursor");
            access.validation$click(x + 185, y + 125, button);
            check(
                mc.player.inventory.getCursorStack() == item && item.count == 17,
                "creative tab preserves cursor");
          }
          mc.player.inventory.setCursorStack(null);
          mc.setScreen(null);
        });
    test(
        "flight walking pose advances then smoothly settles",
        () -> {
          Object cfg = settings();
          Object original = settings();
          try {
            value(cfg, "glide", 5);
            apply(cfg);
            reset(mc);
            mode(mc, "CREATIVE");
            flight(mc, true);
            mc.player.method_1341(.5, 110, .5, 0, 0);
            mc.player.velocityX = .4;
            mc.player.velocityY = mc.player.velocityZ = 0;
            mc.player.field_1049 = .1f;
            float phase = mc.player.field_1050;
            call("FlightController", "travel", new Class[] {PlayerEntity.class}, mc.player);
            check(
                mc.player.field_1049 > .1f && mc.player.field_1050 > phase, "moving limbs advance");
            check(mc.player.field_1048 == .1f, "previous limb pose retained for interpolation");
            value(cfg, "glide", 0);
            apply(cfg);
            float moving = mc.player.field_1049;
            call("FlightController", "travel", new Class[] {PlayerEntity.class}, mc.player);
            check(
                mc.player.field_1049 > 0 && mc.player.field_1049 < moving,
                "stop eases limb amplitude");
          } finally {
            apply(original);
          }
        });
    test(
        "free-look release has no return animation",
        () -> {
          reset(mc);
          mc.options.thirdPerson = false;
          FreeLook.begin(mc);
          FreeLook.turn(mc.player, 300, 100);
          var end = FreeLook.class.getDeclaredMethod("end", Minecraft.class);
          end.setAccessible(true);
          end.invoke(null, mc);
          check(
              !FreeLook.active() && !FreeLook.rendering(),
              "release immediately restores player view");
          check(
              FreeLook.yaw(mc.player.yaw) == mc.player.yaw
                  && FreeLook.pitch(mc.player.pitch) == mc.player.pitch,
              "no residual offsets");
        });
    reset(mc);
    mode(mc, "CREATIVE");
    flight(mc, true);
    log("FIXES DONE passed=" + passed + " failed=" + failed);
  }
}
