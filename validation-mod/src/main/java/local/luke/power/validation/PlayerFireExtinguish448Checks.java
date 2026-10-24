package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.JsonPrimitive;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import local.luke.power.gameplay.GameplayConfig;
import local.luke.power.mixin.SoundManagerAccessor;
import local.luke.power.storage.PowerConfig;
import local.luke.power.subtitles.SubtitleCatalog;
import local.luke.power.validation.mixin.FireState448Access;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ClientPlayerEntity;

/** Exercises the native water fire-clear and movement fizz in a silent disposable world. */
public final class PlayerFireExtinguish448Checks {
  private static final String ID = "power_beta:entity.generic.extinguish_fire";
  private record Sound(String id, float volume, float pitch, double x, double y, double z) {}
  private static final List<Sound> SOUNDS = new ArrayList<>();
  private static boolean recording;

  public static void capture(double x, double y, double z, String id, float volume, float pitch) {
    if (recording && (id.equals(ID) || id.equals("random.fizz") || id.equals("random.splash")))
      SOUNDS.add(new Sound(id, volume, pitch, x, y, z));
  }

  private static Field field(Class<?> type, String name) throws Exception {
    for (Class<?> owner = type; owner != null; owner = owner.getSuperclass())
      try { Field f = owner.getDeclaredField(name); f.setAccessible(true); return f; }
      catch (NoSuchFieldException next) {}
    throw new NoSuchFieldException(name);
  }

  private static ClientPlayerEntity player(Minecraft mc, int x, int y, int z) {
    var player = new ClientPlayerEntity(mc, mc.world, mc.session, mc.player.dimensionId);
    player.method_1340(x + .5, y + .2 + player.field_1631, z + .5);
    mc.player = player; mc.field_2807 = player;
    return player;
  }

  private static List<Sound> fizz() {
    return SOUNDS.stream().filter(s -> s.id.equals(ID) || s.id.equals("random.fizz")).toList();
  }

  private static Sound one(String id) {
    var fizz = fizz(); check(fizz.size() == 1 && fizz.get(0).id.equals(id), "expected one " + id + ", got " + SOUNDS);
    return fizz.get(0);
  }

  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && !mc.world.isRemote && mc.player != null && mc.currentScreen == null,
        "open a disposable singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable clone");
    SilentAudioChecks.ready(mc); failures = 0;
    var audio = AudioConfig.copy(); var gameplay = GameplayConfig.copy();
    var owner = mc.player; var camera = mc.field_2807;
    byte[] disk = Files.readAllBytes(PowerConfig.path());
    int x = ((int)Math.floor(owner.x) >> 4) * 16 + 8, z = ((int)Math.floor(owner.z) >> 4) * 16 + 8, y = 108;
    check(mc.world.method_155(x - 3, y - 1, z - 3, x + 3, y + 5, z + 3), "fixture chunk must be loaded");
    var blocks = new ArrayList<int[]>();
    for (int bx = x - 2; bx <= x + 2; bx++) for (int bz = z - 2; bz <= z + 2; bz++) for (int by = y - 1; by <= y + 4; by++)
      blocks.add(new int[]{bx, by, bz, mc.world.getBlockId(bx, by, bz), mc.world.method_1778(bx, by, bz)});
    for (int[] b : blocks) check(mc.world.method_1777(b[0], b[1], b[2]) == null, "fixture intersects a block entity");
    try {
      for (int[] b : blocks) mc.world.method_154(b[0], b[1], b[2], b[1] == y ? 9 : 0, 0);
      var settings = AudioConfig.copy();
      settings.playerFireExtinguish = true; settings.waterSplashSounds = settings.swimmingSounds = false;
      settings.underwaterEnterSounds = settings.underwaterExitSounds = settings.underwaterAmbience = false;
      settings.underwaterAdditions = settings.underwaterRareAdditions = settings.underwaterUltraRareAdditions = false;
      AudioConfig.preview(settings); recording = true;
      test("EXTINGUISH448 native water clear sounds once, after splash, with both water movement settings", () -> {
        for (boolean modern : new boolean[]{false, true}) for (int level : new int[]{0, 1, 7, 8}) {
          var movement = GameplayConfig.copy(); movement.modernWaterMovement = modern; GameplayConfig.preview(movement);
          mc.world.method_154(x, y, z, 9, level);
          var p = player(mc, x, y - 1, z);
          p.method_1340(x + .5, y + 3 + p.field_1631, z + .5); p.baseTick();
          p.method_1340(x + .5, y + .05 + p.field_1631, z + .5);
          p.fire = 80; SOUNDS.clear(); p.baseTick();
          Sound sound = one(ID);
          check(p.fire == 0 && p.method_1334(), "audio changed native water extinguishing");
          check(sound.volume == .7f && sound.pitch >= 1.2f && sound.pitch <= 2,
              "wrong modern extinguish gain/pitch " + sound);
          check(sound.x == p.x && sound.y == p.boundingBox.minY && sound.z == p.z, "extinguish ignored player position");
          check(SOUNDS.get(0).id.equals("random.splash") && SOUNDS.get(SOUNDS.size() - 1).id.equals(ID),
              "extinguish did not follow the native splash " + SOUNDS);
          SOUNDS.clear(); p.baseTick(); p.move(0, 0, 0); check(fizz().isEmpty(), "wet player repeated extinguish");
          p.fire = 80; SOUNDS.clear(); p.baseTick(); one(ID);
        }
        mc.world.method_154(x, y, z, 9, 0);
      });
      test("EXTINGUISH448 disabled, dry, expired, immune, mob and remote paths preserve native behavior", () -> {
        var next = AudioConfig.copy(); next.playerFireExtinguish = false; AudioConfig.preview(next);
        var p = player(mc, x, y, z); p.fire = 80; SOUNDS.clear(); p.baseTick();
        check(p.fire == 0 && fizz().isEmpty(), "disabled addition changed native water clear");
        next.playerFireExtinguish = true; AudioConfig.preview(next);
        p = player(mc, x, y, z); p.method_1340(x + .5, y + 3 + p.field_1631, z + .5);
        p.fire = 1; SOUNDS.clear(); p.baseTick(); check(p.fire == 0 && fizz().isEmpty(), "natural fire expiry sounded");
        p.fire = 80; SOUNDS.clear(); p.baseTick(); check(p.fire == 79 && fizz().isEmpty(), "dry burning player sounded");
        p = player(mc, x, y, z); ((FireState448Access)p).power$fireImmune(true);
        p.fire = 80; SOUNDS.clear(); p.baseTick(); check(p.fire == 0 && fizz().isEmpty(), "immune player sounded");
        Entity cow = new net.minecraft.class_466(mc.world); cow.method_1340(x + .5, y + .2 + cow.field_1631, z + .5);
        cow.fire = 80; SOUNDS.clear(); cow.baseTick(); check(cow.fire == 0 && fizz().isEmpty(), "mob got player-only addition");
        p = player(mc, x, y, z); p.fire = 80; mc.world.isRemote = true;
        try { SOUNDS.clear(); p.baseTick(); check(p.fire == 0 && fizz().isEmpty(), "remote client added an authoritative sound"); }
        finally { mc.world.isRemote = false; }
      });
      test("EXTINGUISH448 water native fizz stays single and rain-style wet fizz keeps its original event", () -> {
        for (boolean enabled : new boolean[]{false, true}) {
          var next = AudioConfig.copy(); next.playerFireExtinguish = enabled; AudioConfig.preview(next);
          var p = player(mc, x, y, z); p.baseTick(); p.fire = 80;
          SOUNDS.clear(); p.move(0, 0, 0); Sound sound = one(enabled ? ID : "random.fizz");
          check(sound.volume == .7f && sound.pitch >= 1.2f && sound.pitch <= 2 && p.fire < 0,
              "native water fizz changed " + sound);
          SOUNDS.clear(); p.baseTick(); check(fizz().isEmpty(), "native fizz doubled on the next tick");
          var rain = new ClientPlayerEntity(mc, mc.world, mc.session, p.dimensionId) {
            @Override public boolean method_1333() { return true; }
          };
          rain.method_1340(x + .5, y + 3 + rain.field_1631, z + .5); mc.player = rain; mc.field_2807 = rain;
          rain.fire = 80; SOUNDS.clear(); rain.move(0, 0, 0); one("random.fizz");
          check(!rain.method_1334() && rain.fire < 0, "rain-style wet query became water contact");
        }
      });
      test("EXTINGUISH448 added audio never consumes the native entity random sequence", () -> {
        Long checkpoint = null;
        for (boolean enabled : new boolean[]{false, true}) {
          var next = AudioConfig.copy(); next.playerFireExtinguish = enabled; AudioConfig.preview(next);
          var p = player(mc, x, y, z); ((FireState448Access)p).power$firstTick(false);
          var nativeRandom = ((FireState448Access)p).power$random(); nativeRandom.setSeed(448); p.fire = 80; p.baseTick();
          if (!enabled) checkpoint = nativeRandom.nextLong();
          else check(checkpoint == nativeRandom.nextLong(), "extinguish changed native splash/particle randomness");
        }
      });
      test("EXTINGUISH448 settings row previews, cancels and resets to each base without writes", () -> {
        var priorBase = DefaultBases.current(); Field current = field(DefaultBases.class, "current");
        try {
          for (var base : DefaultBases.Base.values()) {
            current.set(null, base);
            var session = SettingsRegistry.open(mc);
            var row = session.settings().stream().filter(s -> s.id.equals("audio.playerFireExtinguish")).findFirst().orElseThrow();
            check(row.page.equals("Audio") && row.group.equals("Extra sounds") && row.label.equals("Player fire extinguish") && !row.restart,
                "wrong extinguish settings row");
            row.reset(); check(row.value.getAsBoolean() == (base == DefaultBases.Base.POWER_BETA), "wrong base reset");
            check(DefaultBases.value(base, row.id, row.powerBetaDefault).equals(row.defaultValue), "Load and reset defaults disagree");
            boolean original = AudioConfig.current().playerFireExtinguish;
            row.value = new JsonPrimitive(!original); session.preview(true);
            check(AudioConfig.current().playerFireExtinguish != original, "setting did not preview");
            session.discard(); check(AudioConfig.current().playerFireExtinguish == original, "Cancel did not restore setting");
          }
        } finally { current.set(null, priorBase); }
        check(Arrays.equals(disk, Files.readAllBytes(PowerConfig.path())), "audio fixture wrote preferences");
      });
      test("EXTINGUISH448 silent preview uses official gain/pitch, Players category, individual volume and subtitle", () -> {
        var next = AudioConfig.copy(); next.master = 50; next.categories.put("players", 40);
        next.categories.put("passive", 0); next.sounds.put(ID, 25); AudioConfig.preview(next);
        mc.options.soundVolume = .2f;
        check(ID.equals(ExtraSounds.previewId("audio.playerFireExtinguish")), "unregistered preview");
        check(SubtitleCatalog.resolve(ID, null).label().equals("Fire extinguishes"), "missing subtitle");
        AudioController.previewSound(ID); Thread.sleep(150);
        var engine = SoundManagerAccessor.power$system();
        check(AudioController.previewing(ID) && Math.abs(engine.getVolume("PowerBetaPreview") - .007f) < 1e-6,
            "preview ignored source, master, category or individual gain");
        check(Math.abs(engine.getPitch("PowerBetaPreview") - 1.6f) < 1e-6, "preview ignored extinguish pitch");
        next = AudioConfig.copy(); next.categories.put("players", 20); AudioConfig.preview(next); Thread.sleep(150);
        check(Math.abs(engine.getVolume("PowerBetaPreview") - .0035f) < 1e-6, "live category edit changed preview source gain");
        AudioController.previewSound(ID); check(!AudioController.previewing(ID), "preview did not stop");
      });
    } finally {
      recording = false; mc.options.soundVolume = 0; mc.world.isRemote = false;
      if (AudioController.previewing(ID)) AudioController.previewSound(ID);
      mc.player = owner; mc.field_2807 = camera;
      for (int[] b : blocks) mc.world.method_154(b[0], b[1], b[2], b[3], b[4]);
      AudioConfig.preview(audio); GameplayConfig.preview(gameplay);
    }
    log("PLAYER FIRE EXTINGUISH 448 FAILURES " + failures); check(failures == 0, "player fire extinguish failures=" + failures);
  }
}
