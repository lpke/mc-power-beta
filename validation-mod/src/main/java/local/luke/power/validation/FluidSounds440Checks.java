package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import local.luke.power.input.PlayerMode;
import local.luke.power.storage.PowerConfig;
import net.minecraft.*;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;

/** Complete entity entry coverage and the native lava display/damage paths, always muted. */
public final class FluidSounds440Checks {
  private record Sound(String id, float volume, float pitch) {}
  private static final List<Sound> SOUNDS = new ArrayList<>();
  private static boolean recording;
  private static long nativeSplashCheckpoint, nativeDisplayCheckpoint;
  public static void capture(String id, float volume, float pitch) {
    if (recording && (id.startsWith("power_beta:") || id.equals("random.splash") || id.equals("random.fizz")))
      SOUNDS.add(new Sound(id, volume, pitch));
  }
  private static Field field(Class<?> type, String name) throws Exception {
    for (Class<?> owner = type; owner != null; owner = owner.getSuperclass())
      try { Field f = owner.getDeclaredField(name); f.setAccessible(true); return f; }
      catch (NoSuchFieldException next) {}
    throw new NoSuchFieldException(name);
  }
  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && !mc.world.isRemote && mc.player != null && mc.currentScreen == null, "open a disposable singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable clone before this fixture");
    SilentAudioChecks.ready(mc); failures = 0;
    var original = AudioConfig.copy(); var rules = AccessRules.copy(mc); byte[] disk = Files.readAllBytes(PowerConfig.path());
    int x = ((int)Math.floor(mc.player.x) >> 4) * 16 + 8, z = ((int)Math.floor(mc.player.z) >> 4) * 16 + 8, y = 108;
    check(mc.world.method_155(x - 5, y, z - 5, x + 5, y + 14, z + 5), "fixture requires a loaded chunk");
    List<int[]> blocks = new ArrayList<>();
    for (int bx = x - 4; bx <= x + 4; bx++) for (int bz = z - 4; bz <= z + 4; bz++) for (int by = y; by <= y + 13; by++)
      blocks.add(new int[]{bx, by, bz, mc.world.getBlockId(bx, by, bz), mc.world.method_1778(bx, by, bz)});
    double vx = mc.player.velocityX, vy = mc.player.velocityY, vz = mc.player.velocityZ;
    Field displayRandom = field(FluidSounds.class, "displayRandom"); Object priorRandom = displayRandom.get(null);
    try {
      var settings = AudioConfig.copy();
      settings.waterSplashSounds = true; settings.swimmingSounds = false;
      settings.underwaterEnterSounds = settings.underwaterExitSounds = settings.underwaterAmbience = false;
      settings.underwaterAdditions = settings.underwaterRareAdditions = settings.underwaterUltraRareAdditions = false;
      settings.lavaPopSounds = settings.lavaAmbientSounds = false;
      AudioConfig.preview(settings);
      for (int[] b : blocks) mc.world.method_154(b[0], b[1], b[2], b[1] == y ? 9 : 0, 0);
      test("FLUID440 every native species, item and vehicle splashes once with modern event and gain", () -> {
        List<Entity> entities = new ArrayList<>();
        for (String type : List.of("Cow", "Pig", "Sheep", "Chicken", "Wolf", "Squid", "Zombie", "PigZombie", "Creeper", "Skeleton", "Spider", "Slime", "Ghast", "Giant", "Arrow", "Snowball")) {
          Entity entity = class_206.method_732(type, mc.world); check(entity != null, "missing native entity " + type); entities.add(entity);
        }
        entities.add(new class_142(mc.world, x, y, z, new ItemStack(1, 1, 0)));
        entities.add(new class_113(mc.world, x, y, z)); entities.add(new class_549(mc.world, x, y, z, 0));
        for (Entity entity : entities) {
          pose(entity, x, y, z); entity.velocityY = -.2; SOUNDS.clear(); recording = true; entity.baseTick();
          check(SOUNDS.isEmpty(), "first tick splashed " + entity.getClass());
          entity.method_1340(x + .5, y + 3 + entity.field_1631, z + .5); entity.baseTick();
          for (double speed : new double[]{-.2, -2}) {
            pose(entity, x, y, z); entity.velocityY = speed; SOUNDS.clear(); entity.baseTick();
            String id = entity instanceof class_65 && speed == -.2 ? "entity.hostile.splash" : "entity.generic.splash";
            one(id); check(Math.abs(SOUNDS.get(0).volume - FluidSoundMath.volume(0, speed, 0, .2f)) < 1e-6, "wrong entry gain " + entity.getClass());
            SOUNDS.clear(); entity.baseTick(); check(SOUNDS.isEmpty(), "wet entity repeated splash " + entity.getClass());
            entity.method_1340(x + .5, y + 3 + entity.field_1631, z + .5); entity.baseTick();
          }
        }
      });
      test("FLUID440 audio leaves native entity/display randomness unchanged and bounds terrain reads", () -> {
        Field random = Arrays.stream(Entity.class.getDeclaredFields())
            .filter(value -> value.getType() == Random.class && !Modifier.isStatic(value.getModifiers())
                && !Modifier.isFinal(value.getModifiers())).findFirst().orElseThrow();
        random.setAccessible(true);
        for (boolean sounds : new boolean[]{false, true}) {
          Entity cow = new class_466(mc.world); pose(cow, x, y, z);
          field(Entity.class, "field_1649").setBoolean(cow, false);
          field(cow.getClass(), "power$contactReady").setBoolean(cow, true);
          Random nativeRandom = new Random(72); random.set(cow, nativeRandom);
          var s = AudioConfig.copy(); s.waterSplashSounds = sounds; AudioConfig.preview(s);
          cow.baseTick();
          if (!sounds) nativeSplashCheckpoint = nativeRandom.nextLong();
          else check(nativeSplashCheckpoint == nativeRandom.nextLong(), "audio changed native splash/particle randomness");
          s.lavaPopSounds = s.lavaAmbientSounds = sounds; AudioConfig.preview(s);
          nativeRandom = new Random(17); Block.STILL_LAVA.method_1617(mc.world, x, y, z, nativeRandom);
          if (!sounds) nativeDisplayCheckpoint = nativeRandom.nextLong();
          else check(nativeDisplayCheckpoint == nativeRandom.nextLong(), "audio changed native display randomness");
        }
        Entity cow = new class_466(mc.world); cow.method_1340(10_000_000, y, 10_000_000);
        check(!FluidSounds.bodyInWater(cow), "unloaded terrain admitted audio fluid contact");
        cow.boundingBox.set(Double.NaN, y, z, x + 1, y + 1, z + 1);
        check(!FluidSounds.bodyInWater(cow), "invalid bounding box admitted fluid contact");
        var s = AudioConfig.copy(); s.waterSplashSounds = true; s.lavaPopSounds = s.lavaAmbientSounds = false; AudioConfig.preview(s);
      });
      test("FLUID440 boat entry uses controlling passenger velocity and shields an above-water rider", () -> {
        var boat = new class_113(mc.world, x + .5, y + 3, z + .5); boat.baseTick();
        boat.field_1594 = mc.player; mc.player.velocityX = .5; mc.player.velocityY = -.4; mc.player.velocityZ = .3;
        pose(boat, x, y, z); boat.velocityY = -4; SOUNDS.clear(); boat.baseTick(); one("entity.generic.splash");
        check(Math.abs(SOUNDS.get(0).volume - FluidSoundMath.volume(.5, -.4, .3, .9f)) < 1e-6, "boat ignored controlling passenger motion");
        Entity rider = new class_466(mc.world); rider.field_1595 = boat;
        boat.method_1340(x + .5, y + .5 + boat.field_1631, z + .5);
        rider.method_1340(x + .5, y + .7 + rider.field_1631, z + .5);
        check(!FluidSounds.bodyInWater(rider), "above-water boat did not shield rider fluid contact");
        boat.method_1340(x + .5, y - .1 + boat.field_1631, z + .5);
        check(FluidSounds.bodyInWater(rider), "submerged boat kept shielding rider");
        boat.field_1594 = null;
      });
      test("FLUID440 disabled and remote splash additions retain native ownership", () -> {
        Entity cow = new class_466(mc.world); cow.method_1340(x + .5, y + 3, z + .5); cow.baseTick();
        var s = AudioConfig.copy(); s.waterSplashSounds = false; AudioConfig.preview(s);
        pose(cow, x, y, z); cow.velocityY = -.2; SOUNDS.clear(); cow.baseTick();
        check(SOUNDS.stream().noneMatch(sound -> sound.id.startsWith("power_beta:")), "disabled splash addition emitted");
        s.waterSplashSounds = true; AudioConfig.preview(s); mc.world.isRemote = true;
        try {
          cow.method_1340(x + .5, y + 3, z + .5); cow.baseTick(); pose(cow, x, y, z); SOUNDS.clear(); cow.baseTick();
          check(SOUNDS.stream().noneMatch(sound -> sound.id.startsWith("power_beta:")), "remote splash addition emitted");
        } finally { mc.world.isRemote = false; }
      });
      for (int[] b : blocks) mc.world.method_154(b[0], b[1], b[2], 0, 0);
      mc.world.method_154(x, y, z, 11, 0);
      test("FLUID440 lava pop and ambience have independent toggles, probability checks and exposed surfaces", () -> {
        for (boolean pop : new boolean[]{false, true}) for (boolean ambient : new boolean[]{false, true}) {
          displayRandom.set(null, new FixedRandom(0));
          var s = AudioConfig.copy(); s.lavaPopSounds = pop; s.lavaAmbientSounds = ambient; AudioConfig.preview(s);
          SOUNDS.clear(); recording = true; Block.STILL_LAVA.method_1617(mc.world, x, y, z, new FixedRandom(0));
          check(count("block.lava.pop") == (pop ? 1 : 0) && count("block.lava.ambient") == (ambient ? 1 : 0), "lava toggles not independent " + SOUNDS);
          for (Sound sound : SOUNDS) check(sound.volume == .3f && Math.abs(sound.pitch - .975f) < 1e-6, "wrong modern lava gain/pitch");
          displayRandom.set(null, new FixedRandom(1));
          SOUNDS.clear(); Block.STILL_LAVA.method_1617(mc.world, x, y, z, new FixedRandom(1)); check(SOUNDS.isEmpty(), "lava skipped probability check");
          displayRandom.set(null, new FixedRandom(0));
          mc.world.method_154(x, y + 1, z, 1, 0); SOUNDS.clear(); Block.STILL_LAVA.method_1617(mc.world, x, y, z, new FixedRandom(0)); check(SOUNDS.isEmpty(), "covered lava sounded");
          mc.world.method_154(x, y + 1, z, 0, 0); mc.world.isRemote = true;
          try { SOUNDS.clear(); Block.STILL_LAVA.method_1617(mc.world, x, y, z, new FixedRandom(0)); check(SOUNDS.isEmpty(), "remote lava sounded"); }
          finally { mc.world.isRemote = false; }
        }
      });
      test("FLUID440 new settings preview independently without writes", () -> {
        for (String key : List.of("bucketSounds", "lavaBucketSounds", "lavaPopSounds", "lavaAmbientSounds", "waterSplashSounds")) {
          String id = ExtraSounds.previewId("audio." + key); check(id != null && BetaSounds.contains(id), "unregistered preview " + key);
          AudioController.previewSound(id); check(AudioController.previewing(id), "preview did not start " + key);
          AudioController.previewSound(id); check(!AudioController.previewing(id), "preview did not stop " + key);
        }
        check(!ExtraSounds.previewId("audio.bucketSounds").equals(ExtraSounds.previewId("audio.lavaBucketSounds")), "bucket previews share event");
        check(Arrays.equals(disk, Files.readAllBytes(PowerConfig.path())), "fluid audio changed saved preferences");
      });
    } finally {
      recording = false; mc.world.isRemote = false; displayRandom.set(null, priorRandom);
      for (int[] b : blocks) mc.world.method_154(b[0], b[1], b[2], b[3], b[4]);
      mc.player.velocityX = vx; mc.player.velocityY = vy; mc.player.velocityZ = vz; AudioConfig.preview(original); AccessRules.restore(mc, rules);
    }
    log("FLUID SOUNDS 440 FAILURES " + failures); check(failures == 0, "fluid sound failures=" + failures);
  }
  private static void pose(Entity e, int x, int y, int z) { e.method_1340(x + .5, y + .2 + e.field_1631, z + .5); e.velocityX = e.velocityY = e.velocityZ = 0; }
  private static long count(String id) { return SOUNDS.stream().filter(s -> s.id.equals("power_beta:" + id)).count(); }
  private static void one(String id) { check(SOUNDS.size() == 1 && count(id) == 1, "expected one " + id + ", got " + SOUNDS); }
  private static final class FixedRandom extends Random {
    private final int value;
    FixedRandom(int value) { this.value = value; }
    @Override public int nextInt(int bound) { return value % bound; }
    @Override public float nextFloat() { return .5f; }
  }
}
