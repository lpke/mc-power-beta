package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.JsonPrimitive;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import local.luke.power.mixin.SoundManagerAccessor;
import local.luke.power.mixin.SoundPoolAccessor;
import local.luke.power.storage.PowerConfig;
import net.minecraft.class_466;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

/** Native action and sound events in a muted disposable client. */
public final class Release300ExtraSoundsChecks {
  private static final String[] KEYS = {"seedPlantingSounds", "hoeTillingSounds", "bucketSounds", "lavaBucketSounds", "boneMealSounds"};
  private static final int X = 44, Y = 111, Z = 44;
  private record Sound(String id, float volume, float pitch) {}
  private record Block(int x, int y, int z, int id, int metadata) {}
  private static final List<Sound> sounds = new ArrayList<>();
  private static boolean recording;

  public static void capture(double x, double y, double z, String id, float volume, float pitch) {
    if (recording) sounds.add(new Sound(id, volume, pitch));
  }

  static void run(Minecraft mc) throws Exception {
    failures = 0;
    var original = AudioConfig.copy(); var document = PowerConfig.document();
    var inventory = mc.player.inventory.main.clone(); int slot = mc.player.inventory.selectedSlot;
    String mode = Class.forName("local.luke.power.creative.api.ModePlayer").getMethod("power_mode").invoke(mc.player).toString();
    double px = mc.player.x, py = mc.player.y, pz = mc.player.z;
    double ox = mc.player.prevX, oy = mc.player.prevY, oz = mc.player.prevZ;
    float yaw = mc.player.yaw, pitch = mc.player.pitch, oldYaw = mc.player.prevYaw, oldPitch = mc.player.prevPitch;
    boolean remote = mc.world.isRemote;
    List<Block> blocks = new ArrayList<>();
    for (int x = X - 1; x <= X + 1; x++) for (int z = Z - 1; z <= Z + 1; z++) for (int y = Y - 1; y <= Y + 5; y++)
      blocks.add(new Block(x, y, z, mc.world.getBlockId(x, y, z), mc.world.method_1778(x, y, z)));
    try {
      mc.setScreen(null); mc.player.inventory.selectedSlot = 0;
      for (Block block : blocks) mc.world.method_154(block.x, block.y, block.z, block.y == Y - 1 ? 1 : 0, 0);
      test("new sound defaults are exclusive to Power Beta; preview and Cancel do not save", () -> {
        var session = SettingsRegistry.open(mc); byte[] before = Files.readAllBytes(PowerConfig.path());
        for (String key : KEYS) {
          var row = find(session, "audio." + key);
          check(row.page.equals("Audio") && row.group.equals("Extra sounds") && !row.restart, "wrong row: " + key);
          for (var base : DefaultBases.Base.values())
            check(DefaultBases.value(base, row.id, new JsonPrimitive(false)).getAsBoolean()
                == (base == DefaultBases.Base.POWER_BETA), "wrong base: " + base + " / " + key);
          row.parse("true");
        }
        session.preview(true);
        check(AudioConfig.current().seedPlantingSounds && AudioConfig.current().hoeTillingSounds
            && AudioConfig.current().bucketSounds && AudioConfig.current().boneMealSounds, "preview missing");
        check(Arrays.equals(before, Files.readAllBytes(PowerConfig.path())), "preview wrote settings");
        session.discard();
        check(AudioConfig.current().seedPlantingSounds == original.seedPlantingSounds
            && AudioConfig.current().hoeTillingSounds == original.hoeTillingSounds
            && AudioConfig.current().bucketSounds == original.bucketSounds
            && AudioConfig.current().boneMealSounds == original.boneMealSounds, "Cancel lost originals");
      });
      test("all indexed variants load into the native positional sound pool", () -> {
        var pool = ((SoundManagerAccessor) mc.soundManager).power$sounds();
        var groups = ((SoundPoolAccessor) pool).power$groups();
        for (var entry : Map.of("item.crop.plant", 6, "item.hoe.till", 4, "item.bucket.fill", 3,
            "item.bucket.fill_lava", 3, "item.bucket.empty", 3, "item.bucket.empty_lava", 3,
            "entity.cow.milk", 3, "item.bone_meal.use", 5).entrySet())
          check(groups.containsKey("power_beta:" + entry.getKey())
              && groups.get("power_beta:" + entry.getKey()).size() == entry.getValue(), "missing sound: " + entry);
      });
      for (String testMode : new String[] {"SURVIVAL", "CREATIVE"}) {
        CreativeVehicleChecks.mode(mc, testMode);
        test("planting, tilling and bone meal sound once after successful " + testMode + " actions", () -> {
          enable(true);
          soil(mc, 60, 0); onBlock(mc, new ItemStack(295, 4, 0), 1); one("item.crop.plant");
          check(mc.world.getBlockId(X, Y, Z) == 59, "seeds not planted");
          check(Math.abs(sounds.get(0).volume - .45f) < .00001, "crop sound lost official volume");
          onBlock(mc, new ItemStack(295, 4, 0), 1); noExtra();
          soil(mc, 1, 0); onBlock(mc, new ItemStack(295, 4, 0), 1); noExtra();
          soil(mc, 3, 0); onBlock(mc, new ItemStack(290, 1, 0), 1); one("item.hoe.till");
          check(mc.world.getBlockId(X, Y - 1, Z) == 60, "soil not tilled");
          soil(mc, 1, 0); onBlock(mc, new ItemStack(290, 1, 0), 1); noExtra();
          soil(mc, 60, 0); mc.world.method_154(X, Y, Z, 59, 0);
          onBlockAt(mc, new ItemStack(351, 4, 15), Y, 1); one("item.bone_meal.use");
          check(mc.world.method_1778(X, Y, Z) == 7, "bone meal did not grow wheat");
          soil(mc, 1, 0); onBlock(mc, new ItemStack(351, 4, 15), 1); noExtra();
          enable(false);
          soil(mc, 60, 0); onBlock(mc, new ItemStack(295, 4, 0), 1); noExtra();
          soil(mc, 3, 0); onBlock(mc, new ItemStack(290, 1, 0), 1); noExtra();
          check(sounds.size() == 1 && sounds.get(0).id.equals("step.gravel"), "disabled hoe lost native sound: " + sounds);
          soil(mc, 60, 0); mc.world.method_154(X, Y, Z, 59, 0);
          onBlockAt(mc, new ItemStack(351, 4, 15), Y, 1); noExtra();
        });
        test("water and lava bucket actions sound once in " + testMode, () -> {
          enable(true); pose(mc);
          for (int fluid : new int[] {9, 11}) {
            soil(mc, 1, 0); mc.world.method_154(X, Y, Z, fluid, 0);
            bucket(mc, 325); one(fluid == 9 ? "item.bucket.fill" : "item.bucket.fill_lava");
            check(mc.world.getBlockId(X, Y, Z) == 0, "source not collected");
            bucket(mc, fluid == 9 ? 326 : 327); one(fluid == 9 ? "item.bucket.empty" : "item.bucket.empty_lava");
            check(mc.world.getBlockId(X, Y, Z) == (fluid == 9 ? 8 : 10), "fluid not placed");
          }
          soil(mc, 1, 0); mc.world.method_154(X, Y, Z, 8, 3); bucket(mc, 325); noExtra();
          soil(mc, 1, 0); bucket(mc, 325); noExtra();
          enable(false); mc.world.method_154(X, Y, Z, 9, 0); bucket(mc, 325); noExtra();
          bucket(mc, 326); noExtra();
        });
        test("water and lava bucket toggles remain independent in " + testMode, () -> {
          pose(mc);
          for (boolean water : new boolean[]{false, true}) for (boolean lava : new boolean[]{false, true}) {
            var settings = AudioConfig.copy(); settings.bucketSounds = water; settings.lavaBucketSounds = lava; AudioConfig.preview(settings);
            for (int fluid : new int[]{9, 11}) {
              soil(mc, 1, 0); mc.world.method_154(X, Y, Z, fluid, 0);
              bucket(mc, 325);
              if (fluid == 9 ? water : lava) one(fluid == 9 ? "item.bucket.fill" : "item.bucket.fill_lava"); else noExtra();
              bucket(mc, fluid == 9 ? 326 : 327);
              if (fluid == 9 ? water : lava) one(fluid == 9 ? "item.bucket.empty" : "item.bucket.empty_lava"); else noExtra();
            }
            mc.player.inventory.main[0] = new ItemStack(325, 1, 0);
            record(() -> new class_466(mc.world).method_1323(mc.player));
            if (water) one("entity.cow.milk"); else noExtra();
          }
        });
        test("cow milking sounds once and failed or disabled interaction stays silent in " + testMode, () -> {
          enable(true); var cow = new class_466(mc.world);
          mc.player.inventory.main[0] = new ItemStack(325, 1, 0);
          record(() -> check(cow.method_1323(mc.player), "milking failed")); one("entity.cow.milk");
          mc.player.inventory.main[0] = new ItemStack(1, 1, 0);
          record(() -> check(!cow.method_1323(mc.player), "unexpected cow interaction")); noExtra();
          enable(false); mc.player.inventory.main[0] = new ItemStack(325, 1, 0);
          record(() -> check(cow.method_1323(mc.player), "disabled milking changed gameplay")); noExtra();
        });
      }
      CreativeVehicleChecks.mode(mc, "SURVIVAL");
      test("armor and tool break each obey only their own toggle and sound once", () -> {
        var session = SettingsRegistry.open(mc);
        try {
          for (boolean tools : new boolean[] {false, true}) for (boolean armor : new boolean[] {false, true}) {
            find(session, "power_controls:features.toolBreakSounds").parse(Boolean.toString(tools));
            find(session, "power_controls:features.armorBreakSounds").parse(Boolean.toString(armor)); session.preview(true);
            for (int item : new int[] {257, 310}) {
              var stack = new ItemStack(item, 1, 0); stack.setDamage(stack.getMaxDamage());
              record(() -> stack.damage(1, mc.player));
              check(stack.count == 0, "item did not break");
              long count = sounds.stream().filter(s -> s.id.equals("power_controls:random.break")).count();
              check(count == ((item == 310 ? armor : tools) ? 1 : 0), "wrong break ownership: " + item + " / " + sounds);
            }
          }
        } finally { session.discard(); }
      });
      test("remote actions add no client-side sounds", () -> {
        enable(true); mc.world.isRemote = true;
        try {
          soil(mc, 60, 0); onBlock(mc, new ItemStack(295, 4, 0), 1); noExtra();
          pose(mc); soil(mc, 1, 0); mc.world.method_154(X, Y, Z, 9, 0); bucket(mc, 325); noExtra();
          mc.player.inventory.main[0] = new ItemStack(325, 1, 0);
          record(() -> new class_466(mc.world).method_1323(mc.player)); noExtra();
        } finally { mc.world.isRemote = remote; }
      });
      test("Apply stores all independent controls", () -> {
        var session = SettingsRegistry.open(mc);
        for (String key : KEYS) find(session, "audio." + key).parse("true");
        session.save(Path.of(".").toAbsolutePath());
        for (String key : KEYS) check(PowerConfig.section("audio").get(key).getAsBoolean(), "missing saved control: " + key);
      });
    } finally {
      recording = false; mc.world.isRemote = remote;
      for (Block block : blocks) mc.world.method_154(block.x, block.y, block.z, block.id, block.metadata);
      System.arraycopy(inventory, 0, mc.player.inventory.main, 0, inventory.length);
      mc.player.inventory.selectedSlot = slot; CreativeVehicleChecks.mode(mc, mode);
      mc.player.method_1340(px, py, pz); mc.player.prevX = ox; mc.player.prevY = oy; mc.player.prevZ = oz;
      mc.player.yaw = yaw; mc.player.pitch = pitch; mc.player.prevYaw = oldYaw; mc.player.prevPitch = oldPitch;
      AudioConfig.preview(original); PowerConfig.write(document); mc.setScreen(null);
    }
    log("RELEASE 300 EXTRA SOUNDS FAILURES " + failures);
  }

  private static void enable(boolean enabled) {
    var settings = AudioConfig.copy();
    settings.seedPlantingSounds = settings.hoeTillingSounds = settings.bucketSounds = settings.lavaBucketSounds = settings.boneMealSounds = enabled;
    AudioConfig.preview(settings);
  }

  private static void soil(Minecraft mc, int block, int metadata) {
    mc.world.method_154(X, Y, Z, 0, 0); mc.world.method_154(X, Y - 1, Z, block, metadata);
  }

  private static void onBlock(Minecraft mc, ItemStack stack, int face) { onBlockAt(mc, stack, Y - 1, face); }

  private static void onBlockAt(Minecraft mc, ItemStack stack, int y, int face) {
    mc.player.inventory.main[0] = stack;
    record(() -> mc.interactionManager.method_1713(mc.player, mc.world, stack, X, y, Z, face));
  }

  private static void pose(Minecraft mc) {
    mc.player.method_1340(X + .5, Y + 2, Z + .5);
    mc.player.prevX = mc.player.x; mc.player.prevY = mc.player.y; mc.player.prevZ = mc.player.z;
    mc.player.yaw = mc.player.prevYaw = 0; mc.player.pitch = mc.player.prevPitch = 90;
  }

  private static void bucket(Minecraft mc, int item) {
    var stack = new ItemStack(item, 1, 0); mc.player.inventory.main[0] = stack;
    record(() -> stack.method_698(mc.world, mc.player));
  }

  private static void record(Runnable action) {
    sounds.clear(); recording = true;
    try { action.run(); } finally { recording = false; }
  }

  private static void one(String event) {
    check(sounds.size() == 1 && sounds.get(0).id.equals("power_beta:" + event), "expected one " + event + ": " + sounds);
  }

  private static void noExtra() {
    check(sounds.stream().noneMatch(sound -> sound.id.startsWith("power_beta:")), "unexpected extra sound: " + sounds);
  }
}
