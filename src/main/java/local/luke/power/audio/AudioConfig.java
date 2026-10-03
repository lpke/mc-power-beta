package local.luke.power.audio;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import local.luke.power.PowerBeta;
import local.luke.power.config.*;
import net.fabricmc.loader.api.FabricLoader;

public final class AudioConfig {
  private static AudioSettings current = load();

  public static Path path() {
    return FabricLoader.getInstance().getConfigDir().resolve("power-beta/audio.json");
  }

  private static AudioSettings load() {
    try {
      if (!Files.exists(path())) return new AudioSettings();
      if (Files.size(path()) > 262144) throw new IOException("Audio settings exceed 256 KiB");
      AudioSettings s = Catalog.JSON.fromJson(Files.readString(path()), AudioSettings.class);
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

  public static void save(AudioSettings next) throws IOException {
    next.validate();
    FileTransaction.atomicWrite(path(), Catalog.JSON.toJson(next).getBytes(StandardCharsets.UTF_8));
    current = Catalog.JSON.fromJson(Catalog.JSON.toJson(next), AudioSettings.class);
    AudioController.settingsChanged();
  }
}
