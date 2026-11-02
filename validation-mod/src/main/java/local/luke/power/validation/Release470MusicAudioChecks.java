package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.Catalog;
import local.luke.power.mixin.SoundManagerAccessor;
import net.minecraft.client.Minecraft;
import paulscode.sound.*;

/** Commands yield to actual decoder and sound threads between volume observations. */
final class Release470MusicAudioChecks {
  private static final String SONG = "music:calm1.ogg", PREVIEW = "PowerBetaMusicPreview";
  private static final Path REPORT = Path.of("power-beta-data/reports/release-470/music-audio.json");
  private static AudioSettings previous;
  private static float music, effects;
  private static final List<Map<String, Object>> observations = new ArrayList<>();
  private Release470MusicAudioChecks() {}

  static void run(Minecraft mc, String action) throws Exception {
    try {
      SilentAudioChecks.ready(mc);
      SoundSystem system = SoundManagerAccessor.power$system();
      if (action.equals("start")) {
        check(previous == null && mc.options.musicVolume == 0 && mc.options.soundVolume == 0,
            "start from a muted disposable client with no active fade fixture");
        previous = AudioConfig.copy(); music = mc.options.musicVolume; effects = mc.options.soundVolume;
        observations.clear();
        AudioController.quiet();
        AudioSettings settings = new AudioSettings(); settings.musicFadeMillis = 5000;
        AudioConfig.preview(settings); mc.options.musicVolume = 1;
        AudioController.playNow(SONG);
        reset(false, 1); AudioController.refresh();
      } else {
        check(previous != null, "start the music fade fixture first");
        switch (action) {
          case "opening" -> verify(system, "BgMusic", false, true);
          case "ending-arm" -> {
            double duration = AudioController.duration(); check(duration > 10, "wait for duration discovery");
            reset(false, duration - 4); AudioController.refresh();
          }
          case "ending" -> verify(system, "BgMusic", false, true);
          case "preview-arm" -> { AudioController.previewSound("music:calm2.ogg"); reset(true, 1); AudioController.refresh(); }
          case "preview" -> verify(system, PREVIEW, true, true);
          case "pause" -> AudioController.pause();
          case "paused" -> {
            Source source = source(system, PREVIEW);
            check(source != null && source.sourceVolume == 0 && !system.playing(PREVIEW), "paused preview must be silent");
          }
          case "off" -> {
            AudioSettings settings = AudioConfig.copy(); settings.musicFadeMillis = 0; AudioConfig.preview(settings);
            AudioController.togglePause(); reset(true, 0); AudioController.refresh();
          }
          case "off-check" -> verify(system, PREVIEW, true, false);
          case "stop" -> stop(mc);
          default -> throw new IllegalArgumentException("Unknown music fade action " + action);
        }
      }
      observations.add(Map.of("action", action, "result", "pass"));
      log("PASS RELEASE470 MUSIC AUDIO " + action);
    } catch (Throwable failure) {
      observations.add(Map.of("action", action, "result", failure.toString()));
      stop(mc);
      if (failure instanceof Exception exception) throw exception;
      throw (Error)failure;
    } finally {
      Files.createDirectories(REPORT.getParent()); Files.writeString(REPORT, Catalog.JSON.toJson(observations));
    }
  }

  private static void stop(Minecraft mc) {
    if (previous == null) return;
    AudioController.quiet(); mc.options.musicVolume = music; mc.options.soundVolume = effects;
    AudioConfig.preview(previous); previous = null;
  }
  private static void reset(boolean preview, double seconds) throws Exception {
    Class<?> owner = preview ? Class.forName("local.luke.power.audio.MusicPreview") : AudioController.class;
    ((MusicProgress)field(owner, "progress").get(null)).reset(seconds, System.nanoTime());
  }
  private static void verify(SoundSystem system, String name, boolean preview, boolean fading) throws Exception {
    Source source = source(system, name);
    check(source != null && system.playing(name), "wait for native streaming source " + name);
    float expected = AudioController.musicVolume(AudioController.currentTrackId());
    check(Math.abs(source.sourceVolume - expected) < .12, "native source gain disagrees with fade envelope");
    if (fading) check(source.sourceVolume > 0 && source.sourceVolume < .98, "expected an audible partial fade");
    else check(source.sourceVolume == 1, "zero fade must restore full native source gain");
    observations.add(Map.of("source", name, "preview", preview, "gain", source.sourceVolume,
        "expected", expected, "position", AudioController.position()));
  }
  private static Source source(SoundSystem system, String name) throws Exception {
    synchronized (SoundSystemConfig.THREAD_SYNC) {
      Object library = field(system.getClass(), "soundLibrary").get(system);
      return (Source)((Map<?, ?>)library.getClass().getMethod("getSources").invoke(library)).get(name);
    }
  }
  private static Field field(Class<?> type, String name) throws Exception {
    for (; type != null; type = type.getSuperclass()) try {
      Field field = type.getDeclaredField(name); field.setAccessible(true); return field;
    } catch (NoSuchFieldException ignored) { }
    throw new NoSuchFieldException(name);
  }
}
