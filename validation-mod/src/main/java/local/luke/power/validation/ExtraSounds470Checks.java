package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.gameplay.*;
import local.luke.power.input.*;
import local.luke.power.rules.*;
import local.luke.power.sprint.*;
import local.luke.power.storage.PowerConfig;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

/** Native ladder actions and real sprint jumps into one-block-deep water. Always muted. */
public final class ExtraSounds470Checks {
  private record Sound(String id, float volume, float pitch, double vx, double vy, double vz) {}
  private static final List<Sound> SOUNDS = new ArrayList<>();
  private static boolean recording;
  public static void capture(String id, float volume, float pitch) {
    if (!recording) return;
    var mc = (Minecraft)net.fabricmc.loader.api.FabricLoader.getInstance().getGameInstance();
    var p = mc.player;
    SOUNDS.add(new Sound(id, volume, pitch, p.velocityX, p.velocityY, p.velocityZ));
  }
  private static Field field(Class<?> type, String name) throws Exception {
    for (Class<?> owner = type; owner != null; owner = owner.getSuperclass())
      try { var f = owner.getDeclaredField(name); f.setAccessible(true); return f; }
      catch (NoSuchFieldException next) {}
    throw new NoSuchFieldException(name);
  }
  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && !mc.world.isRemote && mc.player != null && mc.currentScreen == null
        && !DetachedCamera.isActive(), "open a muted disposable world without a menu or freecam");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable clone");
    SilentAudioChecks.ready(mc); failures = 0;
    var p = mc.player; var saved = new NbtCompound(); p.write(saved);
    var input = p.field_161; var inventory = p.inventory.main.clone(); int slot = p.inventory.selectedSlot;
    var rules = AccessRules.copy(mc); var audio = AudioConfig.copy(); var gameplay = GameplayConfig.copy(); var sprint = SprintConfig.copy();
    String mode = Class.forName("local.luke.power.creative.api.ModePlayer").getMethod("power_mode").invoke(p).toString();
    byte[] disk = Files.readAllBytes(PowerConfig.path());
    int x = ((int)Math.floor(p.x) >> 4) * 16 + 8, z = ((int)Math.floor(p.z) >> 4) * 16 + 8, y = 89;
    var blocks = new ArrayList<int[]>();
    try {
      check(mc.world.method_155(x - 2, y - 1, z - 2, x + 31, y + 32, z + 2), "fixture requires loaded neighboring chunks");
      for (int bx = x - 2; bx <= x + 31; bx++) for (int bz = z - 2; bz <= z + 2; bz++) for (int by = y - 1; by <= y + 32; by++)
        blocks.add(new int[]{bx, by, bz, mc.world.getBlockId(bx, by, bz), mc.world.method_1778(bx, by, bz)});
      var allowed = RuleSet.cheats(); allowed.set(GameRule.ALLOW_SPRINTING, true); AccessRules.restore(mc, allowed);
      CreativeVehicleChecks.mode(mc, "SURVIVAL");
      var g = GameplayConfig.copy(); g.modernWaterMovement = false; g.modernCrawling = false; GameplayConfig.preview(g);
      var a = AudioConfig.copy(); a.ladderSounds = true; a.waterSplashSounds = true; a.swimmingSounds = false;
      a.underwaterEnterSounds = a.underwaterExitSounds = a.underwaterAmbience = false;
      a.underwaterAdditions = a.underwaterRareAdditions = a.underwaterUltraRareAdditions = false; AudioConfig.preview(a);
      var s = SprintConfig.copy(); s.stamina = s.noSprintInCombat = s.doubleTap = s.particles = false;
      s.activation = 1; s.speed = 130; SprintConfig.preview(s);
      clear(mc, blocks, y);
      test("AUDIO470 real sprint run-up and jump from 10/15 blocks into one-deep water use official current velocity", () -> {
        for (boolean modernWater : new boolean[]{false, true}) for (int height : new int[]{10, 15}) {
          g.modernWaterMovement = modernWater; GameplayConfig.preview(g);
          clear(mc, blocks, y);
          for (int bx = x - 2; bx <= x + 6; bx++) for (int bz = z - 2; bz <= z + 2; bz++)
            mc.world.method_154(bx, y + height - 1, bz, 1, 0);
          for (int bx = x + 7; bx <= x + 31; bx++) for (int bz = z - 2; bz <= z + 2; bz++)
            mc.world.method_154(bx, y, bz, 9, 0);
          reset(mc, x + .5, y + height, z + .5); p.yaw = 270;
          p.field_1623 = true; p.velocityY = -.0784; SprintController.reset(); SprintController.movement(p);
          ((SprintPlayer)p).power$sprint().land(SprintConfig.current(), true, 1, false, false, false);
          check(SprintController.active(p), "failed to activate real sprint");
          SOUNDS.clear(); recording = true; int ticks = 0;
          while (p.x < x + 5.5 && ticks++ < 40) { p.field_161.field_2533 = 1; p.tick(); }
          check(p.x >= x + 5.5 && ((SprintPlayer)p).power$sprint().sprinting, "run-up did not reach ledge while sprinting");
          p.field_161.field_2535 = true; p.field_161.field_2533 = 1; p.tick(); p.field_161.field_2535 = false;
          check(p.velocityY > 0, "native jump did not start");
          while (splashes().isEmpty() && ticks++ < 100) { p.field_161.field_2533 = 1; p.tick(); }
          var events = splashes(); check(events.size() == 1, "shallow entry failed to emit exactly one splash " + SOUNDS);
          var sound = events.get(0); float expected = FluidSoundMath.volume(sound.vx, sound.vy, sound.vz, .2f);
          check(expected > 0 && Math.abs(sound.volume - expected) < 1e-6, "shallow splash gain differs from official current-velocity formula");
          check(sound.id.equals(FluidSoundMath.splash(expected)), "shallow splash selected wrong official event");
          check(sound.pitch >= .6f && sound.pitch <= 1.4f, "shallow splash pitch outside official range");
          for (int tick = 0; tick < 3; tick++) { p.field_161.field_2533 = 0; p.tick(); }
          check(splashes().size() == 1, "remaining in shallow water repeated its entry");
          log("AUDIO470 sprint jump height=" + height + " modernWater=" + modernWater + " ticks=" + ticks + " event=" + sound.id + " gain=" + sound.volume
              + " motion=" + sound.vx + "," + sound.vy + "," + sound.vz + " feet=" + p.boundingBox.minY);
        }
      });
      g.modernWaterMovement = false; GameplayConfig.preview(g);
      clear(mc, blocks, y);
      for (int by = y; by <= y + 15; by++) { mc.world.method_154(x + 1, by, z, 1, 0); mc.world.method_154(x, by, z, 65, 4); }
      test("AUDIO470 ladder ascent/descent use modern three-axis stride, exact gain and no idle repeats", () -> {
        reset(mc, x + .5, y + .1, z + .5);
        field(p.getClass(), "power$soundDistance").setFloat(p, 0); field(p.getClass(), "power$nextSwimSound").setFloat(p, 1);
        SOUNDS.clear(); recording = true;
        for (int step = 0; step < 8; step++) p.move(0, .2, 0);
        check(ladders().isEmpty(), "ladder played before modern first stride");
        p.move(0, .2, 0); check(ladders().size() == 1, "ladder ascent did not emit one stride");
        for (int step = 0; step < 9; step++) p.move(0, -.2, 0);
        check(ladders().size() == 2, "ladder descent did not count vertical distance");
        for (var sound : ladders()) check(sound.id.equals("power_beta:block.ladder.step") && sound.volume == .15f && sound.pitch == 1,
            "ladder step differs from official gain/pitch " + sound);
        int count = ladders().size(); for (int step = 0; step < 20; step++) p.move(0, 0, 0);
        check(ladders().size() == count, "idle ladder repeated a step");
        var off = AudioConfig.copy(); off.ladderSounds = false; AudioConfig.preview(off);
        for (int step = 0; step < 20; step++) p.move(0, .2, 0);
        check(ladders().size() == count, "disabled ladder emitted a new sound");
        off.ladderSounds = true; AudioConfig.preview(off);
      });
      test("AUDIO470 successful native ladder placement, hit, break and fall select official action sounds", () -> {
        clear(mc, blocks, y); reset(mc, x + .5, y + 3, z + .5);
        mc.world.method_154(x + 3, y + 1, z, 1, 0); p.inventory.selectedSlot = 0; p.inventory.main[0] = new ItemStack(65, 4, 0);
        SOUNDS.clear(); recording = true;
        check(mc.interactionManager.method_1713(p, mc.world, p.inventory.main[0], x + 3, y + 1, z, 4), "native ladder placement failed");
        check(ladders().stream().anyMatch(sound -> sound.id.equals("power_beta:block.ladder.place") && sound.volume == 1 && sound.pitch == .8f), "wrong ladder placement event");
        mc.worldRenderer.method_1152(p, 2001, x + 2, y + 1, z, 65);
        check(ladders().stream().anyMatch(sound -> sound.id.equals("power_beta:block.ladder.break") && sound.volume == 1 && sound.pitch == .8f), "wrong ladder break event");
        var interaction = mc.interactionManager; mc.interactionManager = new SingleplayerInteractionManager(mc);
        try {
          mc.interactionManager.method_1707(x + 2, y + 1, z, 5);
          // The first held tick installs the new target; the second emits its first hit.
          mc.interactionManager.method_1721(x + 2, y + 1, z, 5);
          mc.interactionManager.method_1721(x + 2, y + 1, z, 5);
        }
        finally { mc.interactionManager = interaction; }
        check(ladders().stream().anyMatch(sound -> sound.id.equals("power_beta:block.ladder.hit") && sound.volume == .25f && sound.pitch == .5f), "wrong native ladder hit event " + ladders());
        mc.world.method_154(x, y, z, 65, 4); reset(mc, x + .5, y + .3, z + .5);
        // Fall callback is invoked directly after native collision in the game; reflection reaches its protected API.
        Method callback = net.minecraft.entity.LivingEntity.class.getDeclaredMethod("method_1389", float.class); callback.setAccessible(true);
        callback.invoke(p, 5f);
        check(ladders().stream().anyMatch(sound -> sound.id.equals("power_beta:block.ladder.fall") && sound.volume == .5f && sound.pitch == .75f), "wrong ladder fall event");
      });
      test("AUDIO470 disabled effects do no ladder terrain work and preview stays independent", () -> {
        var off = AudioConfig.copy(); off.ladderSounds = false; AudioConfig.preview(off);
        check(!LadderSounds.movement(p) && !LadderSounds.at(mc.world, Double.NaN, Double.NaN, Double.NaN), "disabled ladder remained active");
        check(ExtraSounds.previewId("audio.ladderSounds").equals("power_beta:block.ladder.step"), "missing ladder preview");
        check(Arrays.equals(disk, Files.readAllBytes(PowerConfig.path())), "audio fixture wrote settings");
      });
    } finally {
      recording = false; mc.world.isRemote = false;
      for (int[] b : blocks) mc.world.method_154(b[0], b[1], b[2], b[3], b[4]);
      AccessRules.restore(mc, RuleSet.cheats()); CreativeVehicleChecks.mode(mc, mode);
      AudioConfig.preview(audio); GameplayConfig.preview(gameplay); SprintConfig.preview(sprint); AccessRules.restore(mc, rules);
      p.read(saved); p.field_161 = input; System.arraycopy(inventory, 0, p.inventory.main, 0, inventory.length); p.inventory.selectedSlot = slot; SprintController.reset();
    }
    log("EXTRA SOUNDS 470 FAILURES " + failures); check(failures == 0, "extra sound failures=" + failures);
  }
  private static List<Sound> splashes() { return SOUNDS.stream().filter(sound -> sound.id.startsWith("power_beta:entity.player.splash")).toList(); }
  private static List<Sound> ladders() { return SOUNDS.stream().filter(sound -> sound.id.startsWith("power_beta:block.ladder.")).toList(); }
  private static void clear(Minecraft mc, List<int[]> blocks, int y) {
    recording = false;
    for (int[] b : blocks) mc.world.method_154(b[0], b[1], b[2], b[1] == y - 1 ? 1 : 0, 0);
  }
  private static void reset(Minecraft mc, double x, double y, double z) {
    var p = mc.player; var swim = ((SwimPlayer)p).power$swim(); swim.compact = swim.crouched = swim.swimming = false;
    p.spacingXZ = .6f; p.spacingY = 1.8f; p.field_1631 = 1.62f; p.field_1640 = 0;
    p.health = 20; p.dead = false; p.method_1341(x, y, z, 0, 0);
    p.velocityX = p.velocityY = p.velocityZ = 0; p.field_1623 = false; p.field_1624 = false;
    p.field_161 = new class_41(); p.field_161.field_2533 = 1;
    // Teleporting does not refresh the native water flag. Refresh it before sprint eligibility.
    p.baseTick();
  }
}
