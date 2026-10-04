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
  private static boolean playing;
  private static final List<String> resume = new ArrayList<>();

  static boolean active() { return !track.isEmpty(); }
  static String track() { return track; }

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
    system.stop(SOURCE);
    system.removeSource(SOURCE);
    track = id; world = mc.world; playing = false; started = System.nanoTime();
    system.backgroundMusic(SOURCE, selected.field_2127, selected.field_2126, false);
    system.setVolume(SOURCE, AudioController.musicVolume(track));
    system.play(SOURCE);
  }

  static void tick(Minecraft mc, SoundSystem system) {
    if (!active()) return;
    if (mc.world != world) { stop(system, false); return; }
    // The sound thread may start a queued song after the first pause command.
    // It was muted before queuing the preview; catch that late start here.
    for (String name : List.of("BgMusic", "PowerBetaMenu", "streaming")) {
      if (system.playing(name)) {
        if (!resume.contains(name)) resume.add(name);
        system.pause(name);
      }
    }
    system.setVolume(SOURCE, AudioController.musicVolume(track));
    if (system.playing(SOURCE)) playing = true;
    // SoundSystem starts streams asynchronously. Give decoding time before treating it as finished.
    else if (playing || System.nanoTime() - started > 5_000_000_000L) stop(system, true);
  }

  static boolean stop(SoundSystem system, boolean restore) {
    return stop(system, restore, restore);
  }

  static boolean pause(SoundSystem system) {
    // Do not enqueue play followed by pause: SoundSystem defers queued plays
    // until after its other commands, which would restart the paused music.
    return stop(system, false, true);
  }

  private static boolean stop(SoundSystem system, boolean restoreMusic, boolean restoreRecords) {
    if (!active()) return false;
    boolean hadMusic = resume.contains("BgMusic") || resume.contains("PowerBetaMenu");
    system.stop(SOURCE); system.removeSource(SOURCE);
    track = ""; world = null;
    AudioController.refresh();
    for (String name : resume)
      if (name.equals("streaming") ? restoreRecords : restoreMusic && !AudioController.rules().disabled())
        system.play(name);
    resume.clear();
    return hadMusic;
  }
}
