package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import local.luke.power.mixin.SoundManagerAccessor;
import local.luke.power.storage.PowerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import paulscode.sound.SoundSystemConfig;

/** Native head crossings and silent playback of the verified modern assets. */
public final class WaterSounds432Checks {
  private record Sound(String id, float volume, float pitch) {}
  private static final List<Sound> EVENTS = new ArrayList<>();
  private static final List<Sound> BODY = new ArrayList<>();
  private static boolean recording;
  public static void capture(String id, float volume, float pitch) {
    if (recording && id.startsWith(UnderwaterSoundState.PREFIX)) EVENTS.add(new Sound(id, volume, pitch));
    if (recording && id.startsWith("power_beta:entity.player.")) BODY.add(new Sound(id, volume, pitch));
  }
  private static Field field(Class<?> type, String name) throws Exception {
    for (Class<?> owner = type; owner != null; owner = owner.getSuperclass())
      try { Field value = owner.getDeclaredField(name); value.setAccessible(true); return value; }
      catch (NoSuchFieldException next) {}
    throw new NoSuchFieldException(name);
  }
  private static Object water() throws Exception { return field(AudioController.class, "UNDERWATER").get(null); }
  private static List<?> sources() throws Exception { return (List<?>)field(water().getClass(), "sources").get(water()); }
  private static String event(Object source) throws Exception { return (String)field(source.getClass(), "event").get(source); }
  private static float volume(Object source) throws Exception { return field(source.getClass(), "volume").getFloat(source); }
  private static void clear() throws Exception {
    var method = water().getClass().getDeclaredMethod("clear"); method.setAccessible(true); method.invoke(water()); EVENTS.clear();
  }
  private static void tick(Minecraft mc) { AudioController.waterSample(mc.player); AudioController.waterTick(mc.player); }
  private static void nativeHeadTick(Minecraft mc) { AudioController.waterTick(mc.player); mc.player.baseTick(); }
  private static void eyes(Minecraft mc, int x, int z, double eye) {
    mc.player.field_1631 = 1.62f; mc.player.field_1640 = 0;
    mc.player.method_1340(x + .5, eye, z + .5);
    mc.player.velocityX = mc.player.velocityY = mc.player.velocityZ = 0;
    mc.player.field_1631 = 1.62f; mc.player.field_1640 = 0;
  }
  private static void transitions(boolean enabled) {
    var next = AudioConfig.copy(); next.underwaterEnterSounds = next.underwaterExitSounds = enabled;
    next.underwaterAmbience = next.underwaterAdditions = next.underwaterRareAdditions = next.underwaterUltraRareAdditions = false;
    AudioConfig.preview(next);
  }
  private static void one(String suffix, float gain) {
    check(EVENTS.size() == 1 && EVENTS.get(0).id.equals(UnderwaterSoundState.PREFIX + suffix), "wrong crossings " + EVENTS);
    check(EVENTS.get(0).volume == gain && EVENTS.get(0).pitch == 1, "wrong crossing gain/pitch " + EVENTS);
    EVENTS.clear();
  }
  private static long seedFor(String suffix) {
    for (long seed = 0; seed < 1_000_000; seed++)
      if (Objects.equals(UnderwaterSoundState.addition(new Random(seed).nextFloat()), UnderwaterSoundState.PREFIX + suffix)) return seed;
    throw new AssertionError("No deterministic random seed");
  }
  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && !mc.world.isRemote && mc.player != null && mc.currentScreen == null, "open a disposable singleplayer world");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the clone before starting");
    SilentAudioChecks.ready(mc); failures = 0;
    var original = AudioConfig.copy(); byte[] disk = Files.readAllBytes(PowerConfig.path());
    var rules = AccessRules.copy(mc);
    String mode = Class.forName("local.luke.power.creative.api.ModePlayer").getMethod("power_mode").invoke(mc.player).toString();
    Map<Field, Object> saved = new LinkedHashMap<>();
    for (Class<?> owner = mc.player.getClass(); owner != null; owner = owner.getSuperclass())
      for (Field f : owner.getDeclaredFields()) if (f.getType().isPrimitive() && !Modifier.isStatic(f.getModifiers()) && !Modifier.isFinal(f.getModifiers())) {
        f.setAccessible(true); saved.put(f, f.get(mc.player));
      }
    double px = mc.player.x, py = mc.player.y, pz = mc.player.z;
    int x = ((int)Math.floor(px) >> 4) * 16 + 8, z = ((int)Math.floor(pz) >> 4) * 16 + 8, y = 112;
    List<int[]> blocks = new ArrayList<>();
    check(mc.world.method_155(x - 2, y - 2, z - 2, x + 2, y + 3, z + 2), "scene chunk must be loaded");
    for (int bx = x - 2; bx <= x + 2; bx++) for (int bz = z - 2; bz <= z + 2; bz++) for (int by = y - 2; by <= y + 3; by++) {
      blocks.add(new int[]{bx, by, bz, mc.world.getBlockId(bx, by, bz), mc.world.method_1778(bx, by, bz)});
      mc.world.method_154(bx, by, bz, by <= y ? 9 : 0, 0);
    }
    boolean third = mc.options.thirdPerson;
    try {
      AccessRules.restore(mc, local.luke.power.rules.RuleSet.cheats()); CreativeVehicleChecks.mode(mc, "SURVIVAL");
      mc.paused = false; mc.options.soundVolume = .2f;
      var audible = AudioConfig.copy(); audible.master = 100; audible.categories.put("ambient", 100); AudioConfig.preview(audible); recording = true; transitions(true); clear();
      test("WATER432 six independent Audio rows preview and Cancel without touching disk", () -> {
        var session = SettingsRegistry.open(mc);
        for (String key : List.of("underwaterEnterSounds", "underwaterExitSounds", "underwaterAmbience",
            "underwaterAdditions", "underwaterRareAdditions", "underwaterUltraRareAdditions")) {
          var row = session.settings().stream().filter(s -> s.id.equals("audio." + key)).findFirst().orElseThrow();
          check(row.page.equals("Audio") && row.group.equals("Extra sounds") && !row.restart, "wrong row " + key);
          row.parse("true");
        }
        session.preview(true); check(AudioConfig.current().underwaterAmbience && AudioConfig.current().underwaterUltraRareAdditions, "missing preview");
        session.discard(); check(Arrays.equals(disk, Files.readAllBytes(PowerConfig.path())), "preview wrote settings"); transitions(true); clear();
      });
      test("WATER432 native baseTick emits one entry/exit at source and flowing eye surfaces in both perspectives", () -> {
        for (boolean perspective : new boolean[]{false, true}) for (int level : new int[]{0, 1, 7, 8}) {
          mc.options.thirdPerson = perspective; mc.world.method_154(x, y, z, 9, level); clear();
          double surface = y + 1 - net.minecraft.class_385.method_1218(level);
          eyes(mc, x, z, surface + .002); nativeHeadTick(mc); check(EVENTS.isEmpty(), "body-only contact emitted a head sound");
          eyes(mc, x, z, surface - .002); nativeHeadTick(mc); check(EVENTS.isEmpty(), "head entry bypassed modern cached contact");
          nativeHeadTick(mc); one("enter", .5f);
          nativeHeadTick(mc); check(EVENTS.isEmpty(), "remaining submerged repeated entry");
          eyes(mc, x, z, surface + .002); nativeHeadTick(mc); check(EVENTS.isEmpty(), "head exit bypassed modern cached contact");
          nativeHeadTick(mc); one("exit", .3f);
        }
        mc.world.method_154(x, y, z, 9, 0);
      });
      test("WATER432 body splashes use modern contact depth without changing native water physics", () -> {
        clear(); transitions(false); var next = AudioConfig.copy(); next.waterSplashSounds = true; AudioConfig.preview(next);
        double surface = y + 1 - net.minecraft.class_385.method_1218(0);
        eyes(mc, x, z, surface + .002 + 1.62f); mc.player.baseTick(); BODY.clear();
        eyes(mc, x, z, surface - .002 + 1.62f); mc.player.velocityY = -.2; mc.player.baseTick();
        check(BODY.size() == 1 && BODY.get(0).id.equals("power_beta:entity.player.splash"), "shallow contact did not splash once " + BODY);
        check(Math.abs(BODY.get(0).volume - .04f) < 1e-6, "wrong contact gain");
        check(((FluidSoundContact)mc.player).power$audioInWater(), "modern audio contact did not publish");
        BODY.clear(); mc.player.baseTick(); check(BODY.isEmpty(), "shallow contact repeated a splash");
        eyes(mc, x, z, y + 1.1 + 1.62f); mc.player.baseTick(); BODY.clear();
        eyes(mc, x, z, y + .4 + 1.62f); mc.player.velocityY = -2; mc.player.baseTick();
        check(BODY.size() == 1 && BODY.get(0).id.equals("power_beta:entity.player.splash.high_speed"), "deep entry emitted duplicate or wrong splash " + BODY);
        check(BODY.get(0).pitch >= .6f && BODY.get(0).pitch <= 1.4f, "wrong splash pitch");
        transitions(true); next = AudioConfig.copy(); next.waterSplashSounds = false; AudioConfig.preview(next); clear();
      });
      test("WATER432 ambience reaches full gain in 40 ticks and fades out for 20 without replaying entry", () -> {
        clear(); var next = AudioConfig.copy(); next.underwaterAmbience = true; AudioConfig.preview(next);
        eyes(mc, x, z, y + .5);
        for (int i = 1; i <= 40; i++) { tick(mc); check(sources().size() == 1, "duplicate loop");
          check(Math.abs(volume(sources().get(0)) - .65f * i / 40) < 1e-6, "wrong fade-in at " + i); }
        String source = (String)field(sources().get(0).getClass(), "source").get(sources().get(0));
        Thread.sleep(150);
        var engine = SoundManagerAccessor.power$system();
        check(engine.playing(source), "native underwater stream did not start");
        check(Math.abs(engine.getVolume(source) - .13f) < 1e-5, "native stream ignored asset or category gain");
        one("enter", .5f); eyes(mc, x, z, y + 1.1);
        for (int i = 1; i <= 20; i++) { tick(mc); check(sources().size() == 1, "early fade stop");
          check(Math.abs(volume(sources().get(0)) - .65f * (40 - i * 2) / 40) < 1e-6, "wrong fade-out at " + i); }
        one("exit", .3f); tick(mc); check(sources().size() == 1, "modern zero-volume stop boundary changed");
        tick(mc); check(sources().isEmpty(), "loop leaked after fade");
      });
      test("WATER432 additions keep exclusive random bands and stop immediately on surfacing or disabling", () -> {
        clear(); transitions(false); var next = AudioConfig.copy();
        next.underwaterAdditions = next.underwaterRareAdditions = next.underwaterUltraRareAdditions = true; AudioConfig.preview(next);
        Random random = (Random)field(water().getClass(), "random").get(water());
        for (String suffix : List.of("loop.additions", "loop.additions.rare", "loop.additions.ultra_rare")) {
          clear(); eyes(mc, x, z, y + .5); random.setSeed(seedFor(suffix)); tick(mc);
          check(sources().size() == 1 && event(sources().get(0)).equals(UnderwaterSoundState.PREFIX + suffix), "wrong random band " + suffix);
          eyes(mc, x, z, y + 1.1); tick(mc); check(sources().isEmpty(), "addition continued above water");
        }
        next = AudioConfig.copy(); next.underwaterRareAdditions = next.underwaterUltraRareAdditions = false; AudioConfig.preview(next);
        eyes(mc, x, z, y + .5); random.setSeed(seedFor("loop.additions.rare")); tick(mc);
        check(sources().isEmpty(), "disabled rare event fell through to common");
        random.setSeed(seedFor("loop.additions")); tick(mc); check(sources().size() == 1, "common sound absent");
        transitions(false); check(sources().isEmpty(), "disabling did not stop owned sources immediately");
      });
      test("WATER432 repeated quick crossings keep one ambience stream and stable volume", () -> {
        clear(); transitions(true); var next = AudioConfig.copy(); next.underwaterAmbience = true; AudioConfig.preview(next);
        for (int i = 0; i < 40; i++) { eyes(mc, x, z, y + .5); tick(mc); }
        for (int i = 0; i < 100; i++) {
          eyes(mc, x, z, y + 1.1); tick(mc);
          eyes(mc, x, z, y + .5); tick(mc);
          check(sources().size() == 1 && volume(sources().get(0)) <= .65f, "rapid re-entry stacked streams");
        }
        clear();
      });
      test("WATER432 Spectator skips transitions, Creative retains them, multiplayer and pauses stay quiet", () -> {
        transitions(true); clear(); CreativeVehicleChecks.mode(mc, "SPECTATOR");
        eyes(mc, x, z, y + .5); tick(mc); check(EVENTS.isEmpty() && sources().isEmpty(), "Spectator entry emitted a cue");
        eyes(mc, x, z, y + 1.1); tick(mc); CreativeVehicleChecks.mode(mc, "CREATIVE");
        eyes(mc, x, z, y + .5); tick(mc); one("enter", .5f);
        eyes(mc, x, z, y + 1.1); mc.paused = true; tick(mc); check(EVENTS.isEmpty(), "paused tick changed underwater state");
        mc.paused = false; tick(mc); one("exit", .3f);
        mc.world.isRemote = true; eyes(mc, x, z, y + .5); tick(mc); check(EVENTS.isEmpty() && sources().isEmpty(), "remote world emitted audio");
        mc.world.isRemote = false; CreativeVehicleChecks.mode(mc, "SURVIVAL"); clear();
      });
      test("WATER432 all 45 modern water variants retain every sample in registered and streaming decodes", () -> {
        var manifest = JsonParser.parseReader(new java.io.InputStreamReader(WaterSounds432Checks.class.getResourceAsStream("/assets/powerbeta/extra-sounds.json"))).getAsJsonObject();
        var groups = ((local.luke.power.mixin.SoundPoolAccessor)((SoundManagerAccessor)mc.soundManager).power$sounds()).power$groups();
        Path output = Path.of("power-beta-validation-pcm"); Files.createDirectories(output);
        int count = 0;
        for (var value : manifest.getAsJsonArray("assets")) {
          var asset = value.getAsJsonObject(); if (!asset.has("source") || !asset.get("source").getAsString().equals("waterSource")) continue;
          String file = asset.get("file").getAsString(); var url = WaterSounds432Checks.class.getResource("/" + file);
          String event = "power_beta:" + asset.get("event").getAsString();
          String suffix = file.substring("assets/power_beta/stationapi/sounds/sound/".length());
          var track = groups.get(event).stream().filter(t -> t.field_2126.replace('\\', '/').endsWith(suffix)).findFirst().orElseThrow();
          var registered = SoundSystemConfig.getCodec(file);
          check(registered.initialize(track.field_2127), "registered codec init " + file);
          try { var decoded = registered.readAll();
            check(decoded != null && decoded.audioData.length / decoded.audioFormat.getFrameSize() == asset.get("frames").getAsLong(),
                "registered decoder lost frames " + file);
            Files.write(output.resolve(file.replace('/', '_') + ".pcm"), decoded.audioData);
          } finally { registered.cleanup(); }
          var codec = SoundSystemConfig.getCodec(file); check(codec.initialize(AudioResource.streaming(url)), "codec init " + file);
          long bytes = 0; int reads = 0;
          try { while (!codec.endOfStream() && reads++ < 100000) { var chunk = codec.read(); if (chunk != null && chunk.audioData != null) bytes += chunk.audioData.length; }
            check(codec.endOfStream() && bytes / codec.getAudioFormat().getFrameSize() == asset.get("frames").getAsLong(), "truncated/empty stream " + file); }
          finally { codec.cleanup(); }
          count++;
        }
        check(count == 45, "wrong modern water asset count " + count);
      });
      test("WATER432 live mute, unmute and world departure clean up only owned water sources", () -> {
        clear(); transitions(true); var next = AudioConfig.copy(); next.underwaterAmbience = true; AudioConfig.preview(next);
        eyes(mc, x, z, y + .5); tick(mc); check(sources().size() == 1, "missing loop");
        next = AudioConfig.copy(); next.categories.put("ambient", 0); AudioConfig.preview(next); check(sources().isEmpty(), "muted ambient source remained");
        next.categories.put("ambient", 100); AudioConfig.preview(next); tick(mc); check(sources().size() == 1, "unmute did not resume ambience");
        var world = mc.world; mc.world = null;
        try { AudioController.tick(mc); check(sources().isEmpty(), "world departure retained an old stream"); }
        finally { mc.world = world; }
      });
      log("WATER432 FAILURES " + failures); check(failures == 0, "water sound checks failed");
    } finally {
      recording = false; clear(); mc.world.isRemote = false; mc.paused = false;
      for (int[] block : blocks) mc.world.method_154(block[0], block[1], block[2], block[3], block[4]);
      AccessRules.restore(mc, local.luke.power.rules.RuleSet.cheats()); CreativeVehicleChecks.mode(mc, mode);
      AccessRules.restore(mc, rules);
      for (var entry : saved.entrySet()) entry.getKey().set(mc.player, entry.getValue());
      mc.player.method_1340(px, py, pz); mc.options.thirdPerson = third; mc.options.soundVolume = 0;
      AudioConfig.preview(original); check(Arrays.equals(disk, Files.readAllBytes(PowerConfig.path())), "fixture changed settings on disk");
    }
  }
}
