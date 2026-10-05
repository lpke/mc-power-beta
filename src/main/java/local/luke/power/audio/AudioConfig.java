package local.luke.power.audio;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.PowerBeta;
import local.luke.power.config.*;
import net.fabricmc.loader.api.FabricLoader;

public final class AudioConfig {
  private static AudioSettings current = load();

  public static Path path() {
    return PowerConfig.path();
  }

  private static AudioSettings load() {
    try {
      if (!Files.exists(path())) return new AudioSettings();
      AudioSettings s = PowerConfig.read("audio", AudioSettings.class);
      s.validate();
      return s;
    } catch (Exception e) {
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
        || !current.menuDirectories.equals(next.menuDirectories) || current.recursive != next.recursive;
    boolean mode = current.musicMode != next.musicMode || current.customMusic != next.customMusic
        || current.menuMusic != next.menuMusic || !current.preset.equals(next.preset);
    boolean timing = current.gapMinSeconds != next.gapMinSeconds || current.gapMaxSeconds != next.gapMaxSeconds;
    boolean folders = !current.disabledMusicDirectories.equals(next.disabledMusicDirectories)
        || !current.disabledMenuDirectories.equals(next.disabledMenuDirectories);
    current = Catalog.JSON.fromJson(Catalog.JSON.toJson(next), AudioSettings.class);
    if (library) AudioController.settingsChanged();
    if (mode || folders) AudioController.rotationChanged();
    AudioController.rulesChanged();
    if (timing) AudioController.resetDelay();
  }

  public static void save(AudioSettings next) throws IOException {
    next.validate();
    PowerConfig.save("audio", next);
    preview(next);
  }
}
