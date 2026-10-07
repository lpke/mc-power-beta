package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import local.luke.power.building.config.Config;
import local.luke.power.config.*;
import local.luke.power.permissions.CheatWorld;
import local.luke.power.storage.PowerConfig;
import net.minecraft.SingleplayerInteractionManager;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.class_142;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;

/** Mutates only a disposable test world; restores player inventory and configuration. */
final class ObsidianMiningChecks {
  private static final String ID = "tweaks.obsidianBreakingSpeed";

  static void run(Minecraft mc) throws Exception {
    failures = 0;
    check(mc.world != null && !mc.world.isRemote, "requires a disposable singleplayer world");
    var saved = Config.current().copy();
    var document = PowerConfig.document();
    var inventory = mc.player.inventory.main.clone();
    int selected = mc.player.inventory.selectedSlot;
    boolean grounded = mc.player.field_1623;
    var cheats = (CheatWorld) mc.world.method_262();
    boolean hadCheats = cheats.power$cheatsEnabled();
    var manager = mc.interactionManager;
    double x = mc.player.x, y = mc.player.y, z = mc.player.z;
    try {
      mc.setScreen(null);
      mc.player.method_1340(.5, 104, .5);
      mc.player.field_1623 = true;
      mc.player.inventory.selectedSlot = 0;
      mc.player.inventory.main[0] = new ItemStack(278, 1, 0);
      cheats.power$cheatsEnabled(false);
      speed(0);
      float vanilla = progress(mc, Block.field_1890);
      log("OBSIDIAN VANILLA PROGRESS " + vanilla);
      test("slider is global, defaults off, and remains available without cheats", () -> {
        var session = SettingsRegistry.open(mc);
        var setting = find(session, ID);
        check(setting.slider() && setting.min == 0 && setting.max == 100, "slider range");
        check(setting.defaultValue.getAsInt() == 0 && setting.display().equals("Off (vanilla)"), "off default/label");
        check(setting.page.equals("Building") && setting.group.equals("Mining"), "placement");
        check(SettingAccess.visible(session, setting) && !SettingScope.perWorld(setting), "scope/access");
        setting.slide(1); check(setting.value.getAsInt() == 100, "slider maximum");
        setting.slide(0); check(setting.value.getAsInt() == 0, "slider minimum");
      });
      test("Cancel restores previews and Apply persists through PowerConfig", () -> {
        byte[] before = Files.readAllBytes(PowerConfig.path());
        var session = SettingsRegistry.open(mc);
        find(session, ID).parse("100"); session.preview(true);
        check(Config.current().obsidianBreakingSpeed == 100, "preview not applied");
        check(Arrays.equals(before, Files.readAllBytes(PowerConfig.path())), "preview wrote configuration");
        session.discard(); check(Config.current().obsidianBreakingSpeed == 0, "Cancel did not restore");
        find(session, ID).parse("61"); session.save(Path.of(".").toAbsolutePath());
        check(find(SettingsRegistry.open(mc), ID).value.getAsInt() == 61, "Apply not retained");
        check(PowerConfig.section("building").get("obsidianBreakingSpeed").getAsInt() == 61, "Apply not saved");
      });
      test("every slider value increases speed up to the modern 31-tick endpoint", () -> {
        float previous = vanilla;
        for (int value = 1; value <= 100; value++) {
          speed(value);
          float progress = progress(mc, Block.field_1890);
          check(progress > previous, "non-increasing speed at " + value);
          previous = progress;
        }
        check(Math.abs(previous - 49.0f / 50 / 30) < 1e-7, "wrong maximum: " + previous);
        speed(0); check(progress(mc, Block.field_1890) == vanilla, "disabled changes vanilla");
      });
      test("other blocks, hands and non-diamond tools retain vanilla mining", () -> {
        for (int tool : new int[] {0, 270, 274, 257, 285, 278}) {
          mc.player.inventory.main[0] = tool == 0 ? null : new ItemStack(tool, 1, 0);
          for (Block block : new Block[] {Block.STONE, Block.BEDROCK, Block.field_1890}) {
            if (tool == 278 && block == Block.field_1890) continue;
            speed(0); float before = progress(mc, block);
            speed(100); check(progress(mc, block) == before, "changed block/tool " + block.id + "/" + tool);
          }
        }
      });
      test("multiplayer, water and airborne penalties remain intact", () -> {
        mc.player.inventory.main[0] = new ItemStack(278, 1, 0);
        speed(100);
        float full = progress(mc, Block.field_1890);
        mc.player.field_1623 = false;
        try { check(Math.abs(progress(mc, Block.field_1890) - full / 5) < 1e-7, "airborne penalty"); }
        finally { mc.player.field_1623 = true; }
        mc.world.method_200(0, 104, 0, 9);
        mc.world.method_200(0, 105, 0, 9);
        try {
          check(mc.player.isInFluid(net.minecraft.block.Material.WATER), "player is not submerged");
          check(Math.abs(progress(mc, Block.field_1890) - full / 5) < 1e-7, "water penalty");
          mc.player.field_1623 = false;
          check(Math.abs(progress(mc, Block.field_1890) - full / 25) < 1e-7, "combined penalties");
        } finally {
          mc.player.field_1623 = true;
          mc.world.method_200(0, 104, 0, 0);
          mc.world.method_200(0, 105, 0, 0);
        }
        mc.world.isRemote = true;
        try { check(progress(mc, Block.field_1890) == vanilla, "client overrides multiplayer mining"); }
        finally { mc.world.isRemote = false; }
      });
      for (int value : new int[] {0, 50, 100}) test("real mining preserves drops and durability at " + value, () -> {
        speed(value);
        mc.interactionManager = new SingleplayerInteractionManager(mc);
        mc.player.inventory.main[0] = new ItemStack(278, 1, 0);
        mc.world.method_200(0, 101, 0, 49);
        var box = Box.getOrCreate(-2, 99, -2, 3, 104, 3);
        int before = drops(mc, box);
        mc.interactionManager.method_1707(0, 101, 0, 1);
        mc.interactionManager.method_1721(0, 101, 0, 1); // Select the target before accumulating damage.
        int ticks = 0;
        while (mc.world.getBlockId(0, 101, 0) == 49 && ticks < 500) {
          mc.interactionManager.method_1721(0, 101, 0, 1); ticks++;
        }
        check(mc.world.getBlockId(0, 101, 0) == 0, "block was not mined");
        check(ticks == (value == 0 ? 301 : value == 50 ? 56 : 31), "unexpected tick count " + ticks);
        check(mc.player.inventory.main[0].getDamage() == 1, "tool damage differs");
        check(drops(mc, box) == before + 1, "obsidian drop lost or duplicated");
        log("OBSIDIAN " + value + " BREAK TICKS " + ticks);
      });
    } finally {
      mc.world.isRemote = false;
      mc.interactionManager = manager;
      System.arraycopy(inventory, 0, mc.player.inventory.main, 0, inventory.length);
      mc.player.inventory.selectedSlot = selected;
      mc.player.method_1340(x, y, z);
      mc.player.field_1623 = grounded;
      cheats.power$cheatsEnabled(hadCheats);
      PowerConfig.write(document);
      Config.preview(saved);
    }
    log("OBSIDIAN MINING FAILURES " + failures);
  }

  private static float progress(Minecraft mc, Block block) throws Exception {
    Object state = Block.class.getMethod("getDefaultState").invoke(block);
    var method = Arrays.stream(state.getClass().getMethods())
        .filter(m -> m.getName().equals("calcBlockBreakingDelta")).findFirst().orElseThrow();
    Object position = method.getParameterTypes()[2].getConstructor(int.class, int.class, int.class)
        .newInstance(0, 101, 0);
    return (float) method.invoke(state, mc.player, mc.world, position);
  }

  private static void speed(int value) {
    var settings = Config.current().copy(); settings.obsidianBreakingSpeed = value; Config.preview(settings);
  }

  private static int drops(Minecraft mc, Box box) {
    int count = 0;
    for (Object value : mc.world.method_175(class_142.class, box)) {
      ItemStack stack = ((class_142) value).field_564;
      if (stack.itemId == 49) count += stack.count;
    }
    return count;
  }
}
