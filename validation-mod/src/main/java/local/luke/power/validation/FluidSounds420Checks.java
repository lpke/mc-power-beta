package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.*;
import java.lang.reflect.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import local.luke.power.input.*;
import local.luke.power.mixin.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.subtitles.*;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import paulscode.sound.SoundSystemConfig;

/** Native display/movement hooks, complete official assets and muted previews in a disposable clone. */
public final class FluidSounds420Checks {
  private record Sound(String id, float volume, float pitch) {}
  private static final List<Sound> SOUNDS = new ArrayList<>();
  private static final List<String> PARTICLES = new ArrayList<>();
  private static boolean recording;
  public static void capture(String id, float volume, float pitch) {
    if (recording) SOUNDS.add(new Sound(id, volume, pitch));
  }
  public static void particle(String name) { if (recording) PARTICLES.add(name); }
  private static Field field(Class<?> type, String name) throws Exception {
    for (Class<?> owner = type; owner != null; owner = owner.getSuperclass())
      try { Field value = owner.getDeclaredField(name); value.setAccessible(true); return value; }
      catch (NoSuchFieldException next) {}
    throw new NoSuchFieldException(name);
  }
  static void run(Minecraft mc) throws Exception {
    check(mc.world != null && !mc.world.isRemote && mc.player != null && mc.currentScreen == null,
        "open a disposable singleplayer world and close menus");
    check(mc.options.musicVolume == 0 && mc.options.soundVolume == 0, "mute the disposable clone before this fixture");
    check(mc.player instanceof PlayerMode mode && !mode.power$spectator() && !mode.power$flying()
        && mc.player.field_1595 == null && !DetachedCamera.isActive(), "use the physical player outside flight and vehicles");
    SilentAudioChecks.ready(mc); failures = 0;
    var audio = AudioConfig.copy(); var subtitle = SubtitleConfig.copy();
    byte[] disk = Files.readAllBytes(PowerConfig.path());
    Map<Field,Object> primitives = new LinkedHashMap<>();
    for (Class<?> owner = mc.player.getClass(); owner != null; owner = owner.getSuperclass())
      for (Field value : owner.getDeclaredFields()) if (value.getType().isPrimitive()
          && !Modifier.isStatic(value.getModifiers()) && !Modifier.isFinal(value.getModifiers())) {
        value.setAccessible(true); primitives.put(value, value.get(mc.player));
      }
    Field randomField = Arrays.stream(Entity.class.getDeclaredFields()).filter(value -> value.getType() == Random.class).findFirst().orElseThrow();
    randomField.setAccessible(true); Object random = randomField.get(mc.player);
    int x = ((int)Math.floor(mc.player.x) >> 4) * 16 + 8, z = ((int)Math.floor(mc.player.z) >> 4) * 16 + 8, y = 112;
    check(mc.world.method_155(x - 3, y - 1, z - 3, x + 3, y + 3, z + 3), "fixture needs the loaded camera chunk");
    List<int[]> blocks = new ArrayList<>();
    for (int bx = x - 3; bx <= x + 3; bx++) for (int bz = z - 3; bz <= z + 3; bz++) for (int by = y - 1; by <= y + 3; by++)
      blocks.add(new int[]{bx, by, bz, mc.world.getBlockId(bx, by, bz), mc.world.method_1778(bx, by, bz)});
    try {
      randomField.set(mc.player, new Random(0));
      test("FLUID420 settings preview and Cancel keep the original file and selected-base reset targets", () -> {
        var session = SettingsRegistry.open(mc);
        for (String key : List.of("lavaPopSounds", "waterSplashSounds", "swimmingSounds")) {
          var row = session.settings().stream().filter(setting -> setting.id.equals("audio." + key)).findFirst().orElseThrow();
          check(row.page.equals("Audio") && row.group.equals("Extra sounds") && !row.restart, "wrong audio row " + key);
          for (var base : DefaultBases.Base.values()) check(DefaultBases.value(base, row.id, new JsonPrimitive(false)).getAsBoolean()
              == (base == DefaultBases.Base.POWER_BETA), "wrong base target " + row.id);
          row.parse("true");
        }
        session.preview(true); check(AudioConfig.current().lavaPopSounds && AudioConfig.current().waterSplashSounds
            && AudioConfig.current().swimmingSounds, "audio preview did not apply");
        session.discard(); check(Arrays.equals(disk, Files.readAllBytes(PowerConfig.path())), "preview changed settings on disk");
      });
      test("FLUID420 exposed lava pops once with native particles; roofs, disabled sounds and remote worlds stay quiet", () -> {
        scene(mc, blocks, y, false); enable(true);
        mc.world.method_154(x, y, z, 11, 0); mc.world.method_154(x, y + 1, z, 0, 0);
        SOUNDS.clear(); PARTICLES.clear(); recording = true; Block.STILL_LAVA.method_1617(mc.world, x, y, z, new FixedRandom(0));
        one("power_beta:block.lava.pop"); check(SOUNDS.get(0).volume >= .2f && SOUNDS.get(0).volume <= .4f
            && SOUNDS.get(0).pitch >= .9f && SOUNDS.get(0).pitch <= 1.05f, "lava gain or pitch outside modern range");
        check(PARTICLES.equals(List.of("lava")), "lava sound changed the native particle event");
        SOUNDS.clear(); Block.STILL_LAVA.method_1617(mc.world, x, y, z, new FixedRandom(1)); check(SOUNDS.isEmpty(), "lava ignored its native 1/100 check");
        mc.world.method_154(x, y + 1, z, 1, 0); SOUNDS.clear(); Block.STILL_LAVA.method_1617(mc.world, x, y, z, new FixedRandom(0)); check(SOUNDS.isEmpty(), "covered lava popped");
        mc.world.method_154(x, y + 1, z, 0, 0); enable(false); SOUNDS.clear(); Block.STILL_LAVA.method_1617(mc.world, x, y, z, new FixedRandom(0)); check(SOUNDS.isEmpty(), "disabled lava sound emitted");
        enable(true); mc.world.isRemote = true;
        try { SOUNDS.clear(); Block.STILL_LAVA.method_1617(mc.world, x, y, z, new FixedRandom(0)); check(SOUNDS.isEmpty(), "remote lava emitted a client sound"); }
        finally { mc.world.isRemote = false; }
      });
      test("FLUID420 native water entry chooses normal/heavy splash and never repeats while wet", () -> {
        scene(mc, blocks, y, true); enable(true);
        for (double speed : new double[]{-.2, -2}) {
          pose(mc, x, y, z);
          field(mc.player.getClass(), "power$audioWet").setBoolean(mc.player, false);
          field(mc.player.getClass(), "power$contactReady").setBoolean(mc.player, true); mc.player.velocityY = speed;
          field(Entity.class, "field_1612").setBoolean(mc.player, false); field(Entity.class, "field_1649").setBoolean(mc.player, false);
          SOUNDS.clear(); PARTICLES.clear(); recording = true; mc.player.baseTick();
          one(speed == -.2 ? "power_beta:entity.player.splash" : "power_beta:entity.player.splash.high_speed");
          check(Math.abs(SOUNDS.get(0).volume - Math.abs(speed) * .2) < 1e-6, "native splash gain changed");
          check(PARTICLES.contains("bubble") && PARTICLES.contains("splash"), "modern entry sound removed native water particles");
          SOUNDS.clear(); mc.player.baseTick(); check(SOUNDS.isEmpty(), "remaining in water repeated its splash");
        }
        enable(false); pose(mc, x, y, z); mc.player.velocityY = -.2;
        field(Entity.class, "field_1612").setBoolean(mc.player, false); SOUNDS.clear(); mc.player.baseTick(); one("random.splash");
      });
      test("FLUID420 native movement follows modern swim cadence and dry/idle exclusions", () -> {
        scene(mc, blocks, y, true); enable(true); pose(mc, x, y, z);
        field(Entity.class, "field_1612").setBoolean(mc.player, true);
        mc.player.baseTick();
        field(mc.player.getClass(), "power$soundDistance").setFloat(mc.player, 0);
        field(mc.player.getClass(), "power$nextSwimSound").setFloat(mc.player, 1);
        mc.player.velocityX = .2; SOUNDS.clear(); recording = true;
        for (int step = 0; step < 8; step++) mc.player.move(.2, 0, 0);
        check(SOUNDS.stream().noneMatch(sound -> sound.id.equals("power_beta:entity.player.swim")), "swim sound started before its first modern threshold");
        mc.player.move(.2, 0, 0); one("power_beta:entity.player.swim");
        check(Math.abs(SOUNDS.get(0).volume - FluidSoundMath.volume(.2, 0, 0, .35f)) < 1e-6, "swim gain did not use native velocity");
        SOUNDS.clear(); mc.player.move(0, .2, 0); check(SOUNDS.isEmpty(), "vertical water travel advanced the ordinary step threshold");
        field(Entity.class, "field_1612").setBoolean(mc.player, false);
        field(mc.player.getClass(), "power$audioWet").setBoolean(mc.player, false); SOUNDS.clear(); mc.player.move(.2, 0, 0);
        check(SOUNDS.stream().noneMatch(sound -> sound.id.equals("power_beta:entity.player.swim")), "dry movement emitted a swim sound");
      });
      test("FLUID420 every new variant is registered and matches an independent native-codec decode through EOS", () -> {
        var manifest = JsonParser.parseReader(new java.io.InputStreamReader(FluidSounds420Checks.class.getResourceAsStream("/assets/powerbeta/extra-sounds.json"))).getAsJsonObject();
        var groups = ((SoundPoolAccessor)((SoundManagerAccessor)mc.soundManager).power$sounds()).power$groups();
        for (var entry : Map.of("block.lava.pop", 1, "entity.player.swim", 14, "entity.player.splash", 2, "entity.player.splash.high_speed", 1).entrySet())
          check(groups.containsKey("power_beta:" + entry.getKey()) && groups.get("power_beta:" + entry.getKey()).size() == entry.getValue(), "wrong registered variants " + entry);
        int count = 0;
        for (var value : manifest.getAsJsonArray("assets")) {
          var asset = value.getAsJsonObject(); String event = asset.get("event").getAsString();
          if (!event.equals("block.lava.pop") && !event.startsWith("entity.player.")) continue;
          String file = asset.get("file").getAsString(); var url = FluidSounds420Checks.class.getResource("/" + file); check(url != null, file);
          byte[] data; try (var input = url.openStream()) { data = input.readAllBytes(); }
          int last = -1; for (int i = 0; i < data.length - 27; i++) if (data[i] == 'O' && data[i + 1] == 'g' && data[i + 2] == 'g' && data[i + 3] == 'S') last = i;
          check(last >= 0 && (data[last+5]&4)!=0, "missing final Ogg page " + file);
          long samples = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getLong(last + 6);check(samples>0,"empty source granule " + file);
          var codec = SoundSystemConfig.getCodec(file); check(codec != null && codec.initialize(url), "codec initialization failed " + file);
          var reference = new paulscode.sound.codecs.CodecJOrbis();
          try {
            var decoded = codec.readAll(); check(decoded != null && decoded.audioData != null && decoded.audioData.length > 0, "empty decoded audio " + file);
            check(reference.initialize(url), "independent native codec initialization failed " + file);
            var expected=reference.readAll();
            check(codec.endOfStream()&&reference.endOfStream()&&expected!=null&&Arrays.equals(decoded.audioData,expected.audioData)
                &&decoded.audioFormat.matches(expected.audioFormat), "registered decode differs from pinned native codec " + file);
            double seconds = decoded.audioData.length / (double)decoded.audioFormat.getFrameSize() / decoded.audioFormat.getSampleRate();
            double sourceSeconds=samples/decoded.audioFormat.getSampleRate();
            check(decoded.audioData.length / decoded.audioFormat.getFrameSize() == samples,
                "bundled extra sound lost samples or retained padding " + file);
            log("FLUID420 CODEC "+file+" codec="+codec.getClass().getName()+" bytesEqual=true eof=true sourceSeconds="+sourceSeconds
                +" decodedSeconds="+seconds+" legacyDelta="+(seconds-sourceSeconds));count++;
          } finally { codec.cleanup(); reference.cleanup(); }
        }
        check(count == 18, "did not decode all eighteen official variants");
      });
      test("FLUID420 disabled rows still preview and muted events retain registered subtitles", () -> {
        enable(false); var captions = SubtitleConfig.copy(); captions.enabled = true; SubtitleConfig.preview(captions);
        for (String key : List.of("lavaPopSounds", "waterSplashSounds", "swimmingSounds")) {
          String id = ExtraSounds.previewId("audio." + key); check(id != null && BetaSounds.contains(id), "missing preview " + key);
          AudioController.previewSound(id); check(AudioController.previewing(id), "preview did not start " + id);
          AudioController.previewSound(id); check(!AudioController.previewing(id), "preview did not stop " + id);
          Subtitles.clear(); Subtitles.tick(mc); Subtitles.positional(mc.world, mc.soundManager, id, mc.player.x, mc.player.y, mc.player.z, 1);
          check(Subtitles.pending() == 1 && SubtitleCatalog.resolve(id, null) != null, "muted new sound lost its registered caption " + id);
        }
        check(!AudioConfig.current().lavaPopSounds && !AudioConfig.current().waterSplashSounds && !AudioConfig.current().swimmingSounds,
            "previews enabled optional sounds");
        check(Arrays.equals(disk, Files.readAllBytes(PowerConfig.path())), "audio events or previews wrote preferences");
      });
    } finally {
      recording = false; mc.world.isRemote = false;
      for (int[] block : blocks) mc.world.method_154(block[0], block[1], block[2], block[3], block[4]);
      for (var entry : primitives.entrySet()) entry.getKey().set(mc.player, entry.getValue());
      mc.player.method_1340(mc.player.x, mc.player.y, mc.player.z); randomField.set(mc.player, random);
      AudioConfig.preview(audio); SubtitleConfig.preview(subtitle); Subtitles.clear();
    }
    log("FLUID SOUNDS 420 FAILURES " + failures); check(failures == 0, "fluid sound failures=" + failures);
  }
  private static void pose(Minecraft mc, int x, int y, int z) {
    mc.player.method_1340(x + .5, y + mc.player.field_1631, z + .5);
    mc.player.velocityX = mc.player.velocityY = mc.player.velocityZ = 0; mc.player.field_1623 = false; mc.player.fire = 0;
  }
  private static void scene(Minecraft mc, List<int[]> blocks, int floor, boolean water) {
    for (int[] block : blocks) mc.world.method_154(block[0], block[1], block[2], block[1] == floor - 1 ? 1 : water ? 9 : 0, 0);
  }
  private static void enable(boolean enabled) {
    var settings = AudioConfig.copy(); settings.lavaAmbientSounds = settings.lavaBurnSounds = false; settings.lavaPopSounds = settings.waterSplashSounds = settings.swimmingSounds = enabled;
    settings.underwaterEnterSounds = settings.underwaterExitSounds = settings.underwaterAmbience = false;
    settings.underwaterAdditions = settings.underwaterRareAdditions = settings.underwaterUltraRareAdditions = false;
    AudioConfig.preview(settings);
  }
  private static void one(String id) {
    check(SOUNDS.size() == 1 && SOUNDS.get(0).id.equals(id), "expected one " + id + ", got " + SOUNDS);
  }
  private static final class FixedRandom extends Random {
    private final int value;
    FixedRandom(int value) { this.value = value; }
    @Override public int nextInt(int bound) { return value % bound; }
    @Override public float nextFloat() { return .5f; }
  }
}
