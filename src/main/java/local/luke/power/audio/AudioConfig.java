package local.luke.power.audio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import local.luke.power.storage.PowerConfig;
import local.luke.power.PowerBeta;
import local.luke.power.config.Catalog;

public final class AudioConfig {
  private static String loadProblem = "";
  private static AudioSettings current = load();

  public static Path path() {
    return PowerConfig.path();
  }

  private static AudioSettings load() {
    try {
      if (!Files.exists(path())) return new AudioSettings();
      AudioSettings s = PowerConfig.readAudio(AudioSettings.class);
      s.validate();
      loadProblem = "";
      return s;
    } catch (Exception e) {
      loadProblem = "Audio settings could not be loaded. Repair config/power-beta.json and "
          + "power-beta/music.json or restore verified backups, then restart Minecraft before saving.";
      PowerBeta.LOG.error("Could not load audio settings; preserving the file", e);
      return new AudioSettings();
    }
  }

  public static AudioSettings copy() {
    return Catalog.JSON.fromJson(Catalog.JSON.toJson(current), AudioSettings.class);
  }

  public static AudioSettings current() {
    return current;
  }

  public static void preview(AudioSettings next) {
    next.validate();
    boolean library = !current.musicDirectories.equals(next.musicDirectories)
        || !current.disabledMusicDirectories.equals(next.disabledMusicDirectories)
        || current.recursive != next.recursive;
    boolean mode = current.menuMusic != next.menuMusic;
    boolean timing = current.gapMinSeconds != next.gapMinSeconds || current.gapMaxSeconds != next.gapMaxSeconds;
    current = Catalog.JSON.fromJson(Catalog.JSON.toJson(next), AudioSettings.class);
    if (library) AudioController.settingsChanged();
    if (mode) AudioController.rotationChanged();
    AudioController.rulesChanged();
    if (timing) AudioController.resetDelay();
  }

  public static void save(AudioSettings next) throws IOException {
    if (!loadProblem.isEmpty()) throw new IOException(loadProblem);
    next.validate();
    PowerConfig.saveAudio(next);
    preview(next);
  }
}
