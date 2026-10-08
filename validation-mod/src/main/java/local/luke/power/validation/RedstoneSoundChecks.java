package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import local.luke.power.storage.PowerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

/** Exercises actual item placement and the world sound event, including failed placements. */
public final class RedstoneSoundChecks {
  public static boolean reject;
  private static boolean recording;
  private record Sound(double x, double y, double z, String id, float volume, float pitch) {}
  private static final List<Sound> sounds = new ArrayList<>();

  public static void capture(double x, double y, double z, String id, float volume, float pitch) {
    if (recording) sounds.add(new Sound(x, y, z, id, volume, pitch));
  }

  static void run(Minecraft mc) throws Exception {
    failures = 0;
    var document = PowerConfig.document(); var original = AudioConfig.copy();
    var inventory = mc.player.inventory.main.clone(); int selected = mc.player.inventory.selectedSlot;
    String mode = Class.forName("local.luke.power.creative.api.ModePlayer").getMethod("power_mode").invoke(mc.player).toString();
    var session = SettingsRegistry.open(mc); var setting = find(session, "audio.redstonePlacementSound");
    try {
      mc.setScreen(null); mc.player.inventory.selectedSlot = 0;
      test("placement sound defaults off in Audio > Extra sounds and Cancel restores without saving", () -> {
        check(!setting.defaultValue.getAsBoolean() && setting.page.equals("Audio") && setting.group.equals("Extra sounds"), "default/placement");
        byte[] before = Files.readAllBytes(PowerConfig.path());
        setting.parse("true"); session.preview(); check(AudioConfig.current().redstonePlacementSound, "preview missing");
        check(Arrays.equals(before, Files.readAllBytes(PowerConfig.path())), "preview saved");
        session.discard(); check(!AudioConfig.current().redstonePlacementSound, "Cancel did not restore");
      });
      for (String testMode : new String[] {"SURVIVAL", "CREATIVE"}) test("redstone sound is optional, once per placement in " + testMode, () -> {
        CreativeVehicleChecks.mode(mc, testMode);
        setting.parse("false"); session.preview(); place(mc, 331, true);
        check(sounds.isEmpty(), "disabled placement made sound");
        setting.parse("true"); session.preview(); place(mc, 331, true);
        check(sounds.size() == 1, "expected one sound: " + sounds);
        Sound redstone = sounds.get(0); place(mc, 1, true);
        check(sounds.size() == 1 && sounds.get(0).equals(redstone), "sound does not match stone placement: " + sounds + " / " + redstone);
        check(redstone.id.equals("step.stone") && redstone.x == 40.5 && redstone.y == 111.5 && redstone.z == 40.5, "wrong sound/location");
      });
      test("failed, unsupported and occupied placements remain silent", () -> {
        reject = true;
        try { place(mc, 331, true); check(sounds.isEmpty(), "rejected placement sounded"); }
        finally { reject = false; }
        place(mc, 331, false); check(sounds.isEmpty(), "unsupported placement sounded");
        mc.world.method_200(40, 111, 40, 1); recordUse(mc, 331);
        check(sounds.isEmpty(), "occupied placement sounded");
      });
      test("Apply persists and sound follows Blocks/master volume", () -> {
        setting.parse("true"); session.save(Path.of(".").toAbsolutePath());
        check(find(SettingsRegistry.open(mc), setting.id).value.getAsBoolean(), "Apply lost choice");
        check(PowerConfig.section("audio").get("redstonePlacementSound").getAsBoolean(), "not stored");
        AudioSettings s = AudioConfig.copy(); s.master = 50; s.categories.put("blocks", 20);
        check(Math.abs(s.gain("step.stone", false) - .1) < .00001, "wrong category");
      });
    } finally {
      recording = false; reject = false;
      mc.world.method_200(40, 111, 40, 0); mc.world.method_200(40, 110, 40, 0);
      System.arraycopy(inventory, 0, mc.player.inventory.main, 0, inventory.length);
      mc.player.inventory.selectedSlot = selected; CreativeVehicleChecks.mode(mc, mode);
      AudioConfig.preview(original); PowerConfig.write(document); mc.setScreen(null);
    }
    log("REDSTONE SOUND FAILURES " + failures);
  }

  private static void place(Minecraft mc, int item, boolean support) {
    mc.world.method_200(40, 111, 40, 0); mc.world.method_200(40, 110, 40, support ? 1 : 0);
    recordUse(mc, item);
    if (support && !reject) check(mc.world.getBlockId(40, 111, 40) == (item == 331 ? 55 : 1), "placement failed");
  }

  private static void recordUse(Minecraft mc, int item) {
    ItemStack stack = new ItemStack(item, 8, 0); mc.player.inventory.main[0] = stack;
    sounds.clear(); recording = true;
    try { mc.interactionManager.method_1713(mc.player, mc.world, stack, 40, 110, 40, 1); }
    finally { recording = false; }
  }
}
