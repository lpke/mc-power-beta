package local.luke.power.flexible.config;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/** Atomic, explicit saves. Invalid files are preserved for recovery. */
public final class ConfigStore {
  private final Path path;

  public ConfigStore(Path path) {
    this.path = path;
  }

  public Settings load() throws IOException {
    Settings s = new Settings();
    if (!Files.exists(path)) return s;
    if (Files.size(path) > 65536) throw new IOException("Config exceeds 64 KiB");
    Properties p = new Properties();
    try (Reader r = Files.newBufferedReader(path)) {
      p.load(r);
    }
    try {
      s.enabled = bool(p, "enabled", s.enabled);
      s.showOverlay = bool(p, "showOverlay", s.showOverlay);
      s.showPreview = bool(p, "showPreview", s.showPreview);
      s.placeAgainstContainers = bool(p, "placeAgainstContainers", s.placeAgainstContainers);
      s.overlayColor = Integer.parseInt(p.getProperty("overlayColor", "0"));
      s.overlayOpacity = Integer.parseInt(p.getProperty("overlayOpacity", "60"));
      validate(s);
      return s;
    } catch (IllegalArgumentException e) {
      throw new IOException("Invalid config", e);
    }
  }

  private static boolean bool(Properties p, String key, boolean fallback) {
    String value = p.getProperty(key, Boolean.toString(fallback));
    if (!value.equals("true") && !value.equals("false"))
      throw new IllegalArgumentException("Invalid " + key);
    return Boolean.parseBoolean(value);
  }

  public static void validate(Settings s) {
    if (s.overlayColor < 0 || s.overlayColor > 3 || s.overlayOpacity < 10 || s.overlayOpacity > 90)
      throw new IllegalArgumentException("Invalid overlay settings");
  }

  public void save(Settings s) throws IOException {
    validate(s);
    Properties p = new Properties();
    p.setProperty("enabled", "" + s.enabled);
    p.setProperty("showOverlay", "" + s.showOverlay);
    p.setProperty("showPreview", "" + s.showPreview);
    p.setProperty("placeAgainstContainers", "" + s.placeAgainstContainers);
    p.setProperty("overlayColor", "" + s.overlayColor);
    p.setProperty("overlayOpacity", "" + s.overlayOpacity);
    Files.createDirectories(path.getParent());
    Path temp = Files.createTempFile(path.getParent(), "beta-flexible-placement-", ".tmp");
    try {
      try (Writer w = Files.newBufferedWriter(temp)) {
        p.store(w, "Beta Flexible Placement; keys are in Minecraft options.txt");
      }
      try {
        Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temp);
    }
  }
}
