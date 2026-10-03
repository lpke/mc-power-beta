package local.luke.worldedit.config;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

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
      for (var f : Settings.class.getFields()) {
        String v = p.getProperty(f.getName());
        if (v == null) continue;
        if (f.getType() == int.class) f.setInt(s, Integer.parseInt(v));
        else {
          if (!v.equals("true") && !v.equals("false"))
            throw new IllegalArgumentException(f.getName());
          f.setBoolean(s, Boolean.parseBoolean(v));
        }
      }
      validate(s);
    } catch (ReflectiveOperationException | IllegalArgumentException e) {
      throw new IOException("Invalid WorldEdit Beta settings", e);
    }
    return s;
  }

  public static void validate(Settings s) {
    if (s.wandItem < 1
        || s.wandItem > 32767
        || s.blockLimit < 1
        || s.blockLimit > 262144
        || s.blocksPerTick < 64
        || s.blocksPerTick > 8192
        || s.historySize < 1
        || s.historySize > 100
        || s.color < 0
        || s.color > 3
        || s.opacity < 10
        || s.opacity > 100
        || s.lineWidth < 1
        || s.lineWidth > 4) throw new IllegalArgumentException("Invalid WorldEdit Beta settings.");
  }

  public void save(Settings s) throws IOException {
    validate(s);
    Properties p = new Properties();
    try {
      for (var f : Settings.class.getFields()) p.setProperty(f.getName(), f.get(s).toString());
    } catch (IllegalAccessException e) {
      throw new IOException(e);
    }
    Files.createDirectories(path.getParent());
    Path temp = Files.createTempFile(path.getParent(), "worldedit-beta-", ".tmp");
    try {
      try (Writer w = Files.newBufferedWriter(temp)) {
        p.store(w, "WorldEdit Beta");
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
