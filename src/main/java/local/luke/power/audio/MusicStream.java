package local.luke.power.audio;

import java.net.URL;
import paulscode.sound.SoundSystem;
import paulscode.sound.SoundSystemConfig;

/** Creates a non-positional stream, sets its volume, then starts it exactly once. */
final class MusicStream {
  private MusicStream() {}

  static void start(SoundSystem system, String source, URL url, String filename,
      float volume, boolean paused) {
    system.stop(source);
    system.removeSource(source);
    // backgroundMusic queues Play itself. A second Play can race stream preloading
    // and rewind its opening buffers; create an idle source instead.
    system.newStreamingSource(true, source, url, filename, false, 0, 0, 0,
        SoundSystemConfig.ATTENUATION_NONE, 0);
    system.setVolume(source, paused ? 0 : volume);
    if (!paused) system.play(source);
  }
}
