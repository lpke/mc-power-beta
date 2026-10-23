package local.luke.power.audio;

import java.util.*;
import local.luke.power.input.PlayerMode;
import net.minecraft.block.Material;
import net.minecraft.class_385;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ClientPlayerEntity;
import paulscode.sound.SoundSystem;
import paulscode.sound.SoundSystemConfig;

/** AudioController's tick-owned underwater sources. Decoding stays on the sound threads. */
final class UnderwaterAudio {
  private static final String LOOP = UnderwaterSoundState.PREFIX + "loop";
  private final Random random = new Random();
  private final List<Playing> sources = new ArrayList<>();
  private UnderwaterSoundState state = new UnderwaterSoundState();
  private ClientPlayerEntity owner;
  private Object world;
  private SoundSystem engine;
  private int serial;
  private boolean wet, sampledWet;
  private AudioSettings lastSettings;
  private float lastEffects = -1;

  private static final class Playing {
    final String source, event;
    final float gain;
    final UnderwaterSoundState.Fade fade;
    int remaining;
    float volume;
    Playing(String source, String event, WaterSoundCatalog.Variant variant, boolean loop) {
      this.source = source; this.event = event; gain = variant.volume(); remaining = variant.durationTicks();
      fade = loop ? new UnderwaterSoundState.Fade() : null; volume = loop ? 0 : gain;
    }
  }

  void context(Minecraft mc, SoundSystem system) {
    ClientPlayerEntity player = mc.world != null && !mc.world.isRemote && mc.player != null && !mc.player.dead ? mc.player : null;
    if (owner != player || world != mc.world) {
      clear(); owner = player; world = mc.world; engine = system;
    } else if (engine != system) {
      if (engine != null) for (Playing sound : sources) stop(sound);
      sources.clear(); engine = system;
    }
    var settings = AudioConfig.current();
    if (!UnderwaterSoundState.any(settings)) {
      if (!sources.isEmpty() || wet || sampledWet) clear();
      return;
    }
    if (lastSettings != settings || lastEffects != mc.options.soundVolume) {
      lastSettings = settings; lastEffects = mc.options.soundVolume; refresh();
    }
  }

  private void clear() {
    if (engine != null) for (Playing sound : sources) stop(sound);
    sources.clear(); state = new UnderwaterSoundState(); wet = sampledWet = false;
  }

  void refresh() {
    if (engine == null || sources.isEmpty()) return;
    for (var iterator = sources.iterator(); iterator.hasNext();) {
      Playing sound = iterator.next();
      if (!UnderwaterSoundState.enabled(AudioConfig.current(), sound.event) || AudioController.waterVolume(sound.event, 1) == 0) {
        stop(sound); iterator.remove();
      } else engine.setVolume(sound.source, AudioController.waterVolume(sound.event, sound.volume));
    }
  }

  void tick(Minecraft mc, SoundSystem system, ClientPlayerEntity player) {
    context(mc, system);
    var settings = AudioConfig.current();
    if (owner != player || !UnderwaterSoundState.any(settings)) return;
    // Modern Player.tick reads the preceding Entity fluid snapshot before its
    // next base tick refreshes fluid contact. Keep the same one-tick boundary.
    wet = sampledWet;
    boolean spectator = player instanceof PlayerMode mode && mode.power$spectator();
    int change = state.transition(wet, spectator);
    if (change != 0) {
      String event = UnderwaterSoundState.PREFIX + (change > 0 ? "enter" : "exit");
      if (UnderwaterSoundState.enabled(settings, event) && AudioController.waterVolume(event, 1) > 0)
        player.world.method_150(player.x, player.boundingBox.minY, player.z, event, change > 0 ? .5f : .3f, 1);
    }
    // Resume one fading stream on rapid re-entry. Stacking streams takes music
    // channels and makes repeated surface crossings progressively louder.
    if (wet && !spectator && settings.underwaterAmbience && !hasLoop()) start(LOOP, true);
    if (!wet && !sources.isEmpty()) {
      for (var iterator = sources.iterator(); iterator.hasNext();) {
        Playing sound = iterator.next();
        if (sound.fade == null) { stop(sound); iterator.remove(); }
      }
    }
    if (wet && (settings.underwaterAdditions || settings.underwaterRareAdditions || settings.underwaterUltraRareAdditions)) {
      String addition = UnderwaterSoundState.addition(random.nextFloat());
      // Keep the three mutually exclusive probability bands when any band is disabled.
      if (addition != null && UnderwaterSoundState.enabled(settings, addition)) start(addition, false);
    }
    if (sources.isEmpty()) return;
    for (var iterator = sources.iterator(); iterator.hasNext();) {
      Playing sound = iterator.next();
      if (sound.fade != null ? sound.fade.stopped() : --sound.remaining < 0) {
        stop(sound); iterator.remove(); continue;
      }
      if (sound.fade != null) sound.volume = sound.gain * sound.fade.tick(wet);
      engine.setVolume(sound.source, AudioController.waterVolume(sound.event, sound.volume));
    }
  }

  void sample(Minecraft mc, SoundSystem system, ClientPlayerEntity player) {
    context(mc, system);
    if (owner == player && UnderwaterSoundState.any(AudioConfig.current())) sampledWet = eyesInWater(player);
  }

  private void start(String event, boolean loop) {
    if (engine == null || AudioController.waterVolume(event, 1) == 0) return;
    var variant = WaterSoundCatalog.choose(event, random);
    var sound = new Playing("PowerBetaWater" + serial++, event, variant, loop);
    if (variant.stream()) engine.newStreamingSource(true, sound.source, variant.url(), variant.filename(), loop,
        0, 0, 0, SoundSystemConfig.ATTENUATION_NONE, 0);
    else engine.newSource(false, sound.source, variant.url(), variant.filename(), loop,
        0, 0, 0, SoundSystemConfig.ATTENUATION_NONE, 0);
    engine.setVolume(sound.source, AudioController.waterVolume(event, sound.volume));
    engine.play(sound.source); sources.add(sound);
  }

  private void stop(Playing sound) { engine.stop(sound.source); engine.removeSource(sound.source); }

  private boolean hasLoop() {
    for (Playing sound : sources) if (sound.fade != null && !sound.fade.stopped()) return true;
    return false;
  }

  /** Physical eye position, independent of perspective, free look and detached cameras. */
  static boolean eyesInWater(ClientPlayerEntity player) {
    double eye = player.boundingBox.minY + player.field_1631;
    if (!FluidSounds.loaded(player.world, player.x, eye, player.z)) return false;
    int x = (int)Math.floor(player.x), y = (int)Math.floor(eye), z = (int)Math.floor(player.z);
    if (player.world.method_1779(x, y, z) != Material.WATER) return false;
    int above = y < 127 ? player.world.getBlockId(x, y + 1, z) : 0;
    boolean covered = above > 0 && (player.world.method_1779(x, y + 1, z) == Material.WATER
        || player.world.method_1778(x, y, z) == 0 && net.minecraft.block.Block.BLOCKS[above].method_1620());
    double surface = covered ? y + 1
        : y + 1 - class_385.method_1218(player.world.method_1778(x, y, z));
    return eye <= surface;
  }
}
