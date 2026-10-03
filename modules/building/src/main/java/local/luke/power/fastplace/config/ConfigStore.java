package local.luke.power.fastplace.config;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import local.luke.power.fastplace.RestrictionMode;

/** Configuration writes are explicit and atomic; nothing writes during placement. */
public final class ConfigStore {
  private final Path path;

  public ConfigStore(Path path) {
    this.path = path;
  }

  public Settings load() throws IOException {
    Settings s = new Settings();
    if (!Files.exists(path)) return s;
    if (Files.size(path) > 65536) throw new IOException("Fast Place config exceeds 64 KiB");
    Properties p = new Properties();
    try (Reader r = Files.newBufferedReader(path)) {
      p.load(r);
    }
    try {
      s.enabled = bool(p, "enabled", s.enabled);
      s.attemptsPerTick =
          Math.max(
              1, Math.min(16, Integer.parseInt(p.getProperty("fastBlockPlacementCount", "2"))));
      s.slabMode = local.luke.power.fastplace.SlabMode.valueOf(p.getProperty("slabMode", "CONTINUOUS"));
      s.rememberOrientation = bool(p, "fastPlacementRememberOrientation", true);
      s.restrictionEnabled = bool(p, "placementRestriction", true);
      s.restrictionTiedToFast = bool(p, "placementRestrictionTiedToFast", true);
      s.restrictionMode =
          RestrictionMode.valueOf(p.getProperty("placementRestrictionMode", "FACE"));
      s.listMode =
          Settings.ListMode.valueOf(p.getProperty("fastPlacementItemListType", "BLACKLIST"));
      s.blacklist = Settings.parseFilters(p.getProperty("fastPlacementItemBlackList", ""));
      s.whitelist = Settings.parseFilters(p.getProperty("fastPlacementItemWhiteList", ""));
      return s;
    } catch (IllegalArgumentException e) {
      throw new IOException("Invalid Fast Place config", e);
    }
  }

  private static boolean bool(Properties p, String key, boolean fallback) {
    String value = p.getProperty(key, Boolean.toString(fallback));
    if (!value.equals("true") && !value.equals("false"))
      throw new IllegalArgumentException("Invalid " + key);
    return Boolean.parseBoolean(value);
  }

  public void save(Settings s) throws IOException {
    Properties p = new Properties();
    p.setProperty("enabled", "" + s.enabled);
    p.setProperty("fastBlockPlacementCount", "" + s.attemptsPerTick);
    p.setProperty("slabMode", s.slabMode.name());
    p.setProperty("fastPlacementRememberOrientation", "" + s.rememberOrientation);
    p.setProperty("placementRestriction", "" + s.restrictionEnabled);
    p.setProperty("placementRestrictionTiedToFast", "" + s.restrictionTiedToFast);
    p.setProperty("placementRestrictionMode", s.restrictionMode.name());
    p.setProperty("fastPlacementItemListType", s.listMode.name());
    p.setProperty("fastPlacementItemBlackList", Settings.formatFilters(s.blacklist));
    p.setProperty("fastPlacementItemWhiteList", Settings.formatFilters(s.whitelist));
    Files.createDirectories(path.getParent());
    Path temp = Files.createTempFile(path.getParent(), "beta-fastplace-", ".tmp");
    try {
      try (Writer w = Files.newBufferedWriter(temp)) {
        p.store(w, "Beta Fast Place; hotkey is saved in Minecraft options.txt");
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
