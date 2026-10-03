package local.luke.creative.config;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.*;

public final class Config {
  public static final Logger LOG = LogManager.getLogger("LpkeCreative");
  private static final Path PATH =
      FabricLoader.getInstance().getConfigDir().resolve("lpkecreative.properties");
  private static Settings settings = load();

  private Config() {}

  public static Settings current() {
    return settings;
  }

  private static Settings load() {
    Settings s = new Settings();
    if (!Files.exists(PATH)) return s;
    try {
      if (Files.size(PATH) > 65536) throw new IOException("Config exceeds 64 KiB");
      Properties p = new Properties();
      try (Reader r = Files.newBufferedReader(PATH)) {
        p.load(r);
      }
      for (var f : Settings.class.getFields()) {
        String value = p.getProperty(f.getName());
        if (value == null) continue;
        if (f.getType() == int.class) f.setInt(s, Integer.parseInt(value));
        else {
          if (!value.equals("true") && !value.equals("false"))
            throw new IllegalArgumentException(f.getName());
          f.setBoolean(s, Boolean.parseBoolean(value));
        }
      }
      s.validate();
      return s;
    } catch (IOException | ReflectiveOperationException | IllegalArgumentException e) {
      LOG.error("Could not load settings; preserving the file", e);
      return new Settings();
    }
  }

  public static void apply(Settings next) throws IOException {
    next.validate();
    Properties p = new Properties();
    try {
      for (var f : Settings.class.getFields()) p.setProperty(f.getName(), f.get(next).toString());
    } catch (IllegalAccessException e) {
      throw new IOException(e);
    }
    Files.createDirectories(PATH.getParent());
    Path temp = Files.createTempFile(PATH.getParent(), "lpkecreative-", ".tmp");
    try {
      try (Writer w = Files.newBufferedWriter(temp)) {
        p.store(w, "LpkeCreative; keys are saved in options.txt");
      }
      try {
        Files.move(temp, PATH, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(temp, PATH, StandardCopyOption.REPLACE_EXISTING);
      }
      settings = next.copy();
    } finally {
      Files.deleteIfExists(temp);
    }
  }
}
