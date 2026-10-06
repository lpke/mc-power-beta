package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;

import java.util.concurrent.atomic.AtomicInteger;
import local.luke.power.audio.AudioController;
import local.luke.power.mixin.SoundManagerAccessor;

/** Counts actual OpenAL stream preloads, including restarts hidden by the UI clock. */
public final class AudioStartChecks {
  private static final AtomicInteger background = new AtomicInteger(), preview = new AtomicInteger();

  public static void preload(String source) {
    if (source.equals("BgMusic")) background.incrementAndGet();
    if (source.equals("PowerBetaMusicPreview")) preview.incrementAndGet();
  }

  public static void run(String action) throws Exception {
    switch (action) {
      case "reset" -> { background.set(0); preview.set(0); }
      case "background", "preview", "resume-background" -> test("OpenAL preload count for " + action, () -> {
        int count = !action.equals("preview") ? background.get() : preview.get();
        String source = !action.equals("preview") ? "BgMusic" : "PowerBetaMusicPreview";
        check(SoundManagerAccessor.power$system().playing(source), "source not playing");
        check(count == (action.equals("resume-background") ? 0 : 1), "preloaded " + count + " times");
        check(AudioController.position() > 0, "playback clock did not advance");
      });
      case "paused" -> test("paused seek does not play or preload before Resume", () -> {
        check(!AudioController.musicPlaying(), "paused seek resumed music");
        check(!SoundManagerAccessor.power$system().playing("BgMusic"), "paused source playing");
        check(background.get() == 0, "paused seek preloaded");
      });
      default -> throw new IllegalArgumentException(action);
    }
  }
}
