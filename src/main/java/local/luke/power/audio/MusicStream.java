package local.luke.power.audio;

import java.net.*;
import paulscode.sound.Source;
import paulscode.sound.SoundSystem;
import paulscode.sound.SoundSystemConfig;

/** Creates a non-positional stream, sets its volume, then starts it exactly once. */
public final class MusicStream {
  record Observation(Object source, String url, Object request) {}
  private static volatile Observation background;
  private MusicStream() {}

  static Observation background() { return background; }

  /** Native FilenameURL retains this reference, identifying this attempt even for the same song. */
  static URL requestUrl(URL url) throws MalformedURLException {
    // The context preserves custom protocol handlers as well as the exact external URL.
    return new URL(url, url.toExternalForm());
  }

  /** Called after native Play on the command thread, never under a render-thread I/O lock. */
  public static void played(Source source) {
    if (source != null && "BgMusic".equals(source.sourcename) && source.filenameURL != null) {
      var url = source.filenameURL.getURL();
      if (url != null) background = new Observation(source, url.toExternalForm(), url);
    }
  }

  public static void removing(Source source) {
    Observation observed = background;
    if (observed != null && observed.source == source) background = null;
  }

  static void start(SoundSystem system, String source, URL url, String filename,
      float volume, boolean paused) {
    MusicSeeking.replacing(source, url);
    system.stop(source);
    system.removeSource(source);
    // backgroundMusic queues Play itself. A second Play can race stream preloading
    // and rewind its opening buffers; create an idle source instead.
    system.newStreamingSource(true, source, url, filename, false, 0, 0, 0,
        SoundSystemConfig.ATTENUATION_NONE, 0);
    system.setVolume(source, paused ? 0 : volume);
    if (!paused) system.play(source);
  }

  static void stop(SoundSystem system, String source) {
    MusicSeeking.discardSource(source);
    system.stop(source);
    system.removeSource(source);
  }

  /** Called on the native command thread after any earlier queued Play has run. */
  public static boolean canPause(Source source) {
    boolean music = "BgMusic".equals(source.sourcename) || "PowerBetaMenu".equals(source.sourcename)
        || MusicPreview.SOURCE.equals(source.sourcename);
    // Native pause marks an idle source paused despite having no buffers/channel.
    // Its first Play then takes the resume path and never preloads the decoder.
    return !source.toStream || !music || !source.stopped();
  }
}
