package local.luke.power.audio;

import java.util.*;
import net.minecraft.class_267;
import net.minecraft.client.Minecraft;
import paulscode.sound.SoundSystem;

/** A streamed audition that leaves the playlist, history and current song position intact. */
final class MusicPreview {
  static final String SOURCE = "PowerBetaMusicPreview";
  private static String track = "";
  private static Object world;
  private static long started;
  private static boolean playing, paused;
  private static final MusicProgress progress = new MusicProgress();
  static double position() { return progress.seconds(); }
  static void seeked(double offset) { progress.reset(offset,System.nanoTime());playing=false;started=System.nanoTime(); }
  private static final List<String> resume = new ArrayList<>();

  static boolean active() { return !track.isEmpty(); }
  static String track() { return track; }
  static boolean paused() { return paused; }
  static void resume(SoundSystem system) {
    if (!active()) return;
    paused = false; playing = false; started = System.nanoTime();
    system.setVolume(SOURCE, AudioController.musicVolume(track)); system.play(SOURCE);
  }

  static void start(Minecraft mc, SoundSystem system, class_267 selected, boolean backgroundStarting) {
    String id = AudioController.trackId(selected);
    if (track.equals(id)) { stop(system, true); return; }
    if (!active()) {
      resume.clear();
      for (String name : List.of("BgMusic", "PowerBetaMenu", "streaming")) {
        if (system.playing(name) || name.equals("BgMusic") && backgroundStarting) resume.add(name);
        // Pause even a queued source that the sound thread has not started yet.
        system.setVolume(name, 0);
        system.pause(name);
      }
    }
    MusicSeeking.cancel(); progress.reset(0,System.nanoTime());
    system.stop(SOURCE);
    system.removeSource(SOURCE);
    track = id; world = mc.world; playing = false; paused = false; started = System.nanoTime();
    system.backgroundMusic(SOURCE, selected.field_2127, selected.field_2126, false);
    system.setVolume(SOURCE, AudioController.musicVolume(track));
    system.play(SOURCE);
  }

  static void tick(Minecraft mc, SoundSystem system) {
    if (!active()) return;
    progress.update(!paused && system.playing(SOURCE),System.nanoTime());
    if (mc.world != world) { stop(system, false); return; }
    // The sound thread may start a queued song after the first pause command.
    // It was muted before queuing the preview; catch that late start here.
    for (String name : List.of("BgMusic", "PowerBetaMenu", "streaming")) {
      if (system.playing(name)) {
        if (!resume.contains(name)) resume.add(name);
        system.pause(name);
      }
    }
    if (paused) { system.setVolume(SOURCE, 0); if (system.playing(SOURCE)) system.pause(SOURCE); return; }
    system.setVolume(SOURCE, AudioController.musicVolume(track));
    if (system.playing(SOURCE)) playing = true;
    // SoundSystem starts streams asynchronously. Give decoding time before treating it as finished.
    else if (playing || System.nanoTime() - started > 5_000_000_000L) stop(system, true);
  }

  static boolean stop(SoundSystem system, boolean restore) {
    return stop(system, restore, restore);
  }

  static boolean pause(SoundSystem system) {
    if (!active()) return false;
    paused = true; system.setVolume(SOURCE, 0); system.pause(SOURCE);
    return true;
  }

  private static boolean stop(SoundSystem system, boolean restoreMusic, boolean restoreRecords) {
    if (!active()) return false;
    boolean hadMusic = resume.contains("BgMusic") || resume.contains("PowerBetaMenu");
    MusicSeeking.cancel();
    system.stop(SOURCE); system.removeSource(SOURCE);
    track = ""; world = null; paused = false;
    if (restoreMusic && hadMusic) AudioController.resuming();
    AudioController.refresh();
    for (String name : resume)
      if (name.equals("streaming") ? restoreRecords : restoreMusic)
        system.play(name);
    resume.clear();
    return hadMusic;
  }
}
