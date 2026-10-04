package local.luke.power.building.config;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;

/** One atomic properties file. Old files are read only during the first migration. */
public final class ConfigStore {
  private final Path path;

  public ConfigStore(Path path) {
    this.path = path;
  }

  public Settings load() throws IOException {
    if (!Files.exists(path)) return migrate(path.getParent());
    if (Files.size(path) > 65536) throw new IOException("Config exceeds 64 KiB");
    Properties p = new Properties();
    try (Reader r = Files.newBufferedReader(path)) {
      p.load(r);
    }
    Settings s = new Settings();
    try {
      read(p, "", s);
      validate(s);
    } catch (ReflectiveOperationException | IllegalArgumentException e) {
      throw new IOException("Invalid BuildingFeatures settings", e);
    }
    return s;
  }

  private Settings migrate(Path dir) throws IOException {
    Settings s = new Settings();
    s.placement =
        new local.luke.power.fastplace.config.ConfigStore(dir.resolve("beta-fastplace.properties"))
            .load();
    s.flexible =
        new local.luke.power.flexible.config.ConfigStore(
                dir.resolve("beta-flexible-placement.properties"))
            .load();
    s.sneak =
        new local.luke.power.fakesneak.config.ConfigStore(dir.resolve("beta-fake-sneak.properties"))
            .load();
    s.slabs =
        new local.luke.power.slabplacement.config.ConfigStore(
                dir.resolve("beta-slab-placement.properties"))
            .load();
    Path omni = dir.resolve("free_look.properties");
    if (Files.exists(omni)) {
      if (Files.size(omni) > 65536) throw new IOException("Free look config exceeds 64 KiB");
      Properties p = new Properties();
      try (Reader r = Files.newBufferedReader(omni)) {
        p.load(r);
      }
      String v = p.getProperty("toggleMode", "false");
      if (!v.equals("true") && !v.equals("false"))
        throw new IOException("Invalid Free look toggle mode");
      s.freeLookToggle = Boolean.parseBoolean(v);
    }
    validate(s);
    return s;
  }

  private static void read(Properties p, String prefix, Object target)
      throws ReflectiveOperationException {
    for (Field f : target.getClass().getFields()) {
      String key = prefix + f.getName(), value = p.getProperty(key);
      Class<?> type = f.getType();
      if (type == boolean.class && value != null) {
        if (!value.equals("true") && !value.equals("false"))
          throw new IllegalArgumentException(key);
        f.setBoolean(target, Boolean.parseBoolean(value));
      } else if (type == int.class && value != null) f.setInt(target, Integer.parseInt(value));
      else if (type.isEnum() && value != null) f.set(target, enumValue(type, value));
      else if (type == List.class && value != null)
        f.set(target, local.luke.power.fastplace.config.Settings.parseFilters(value));
      else if (!type.isPrimitive() && !type.isEnum() && type != List.class)
        read(p, key + ".", f.get(target));
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Object enumValue(Class type, String v) {
    return Enum.valueOf(type, v);
  }

  private static void write(Properties p, String prefix, Object source)
      throws IllegalAccessException {
    for (Field f : source.getClass().getFields()) {
      Object value = f.get(source);
      String key = prefix + f.getName();
      if (value instanceof List<?> list)
        p.setProperty(key, String.join(",", list.stream().map(Object::toString).toList()));
      else if (f.getType().isPrimitive() || value instanceof Enum<?>)
        p.setProperty(key, value.toString());
      else write(p, key + ".", value);
    }
  }

  public static void validate(Settings s) {
    s.hotbar.validate();
    if (s.autoWalkHoldMillis < 100 || s.autoWalkHoldMillis > 3000)
      throw new IllegalArgumentException("Auto-walk hold time must be 100 to 3000 ms");
    if (s.freeLookPerspective == null)
      throw new IllegalArgumentException("Choose a free look perspective");
    if (s.placement.attemptsPerTick < 1
        || s.placement.attemptsPerTick > 16
        || s.placement.slabMode == null)
      throw new IllegalArgumentException("Placement rate must be 1 to 16");
    local.luke.power.flexible.config.ConfigStore.validate(s.flexible);
    local.luke.power.fastplace.config.Settings.parseFilters(
        local.luke.power.fastplace.config.Settings.formatFilters(s.placement.blacklist));
    local.luke.power.fastplace.config.Settings.parseFilters(
        local.luke.power.fastplace.config.Settings.formatFilters(s.placement.whitelist));
  }

  public void save(Settings s) throws IOException {
    validate(s);
    Properties p = new Properties();
    try {
      write(p, "", s);
    } catch (IllegalAccessException e) {
      throw new IOException(e);
    }
    Files.createDirectories(path.getParent());
    Path temp = Files.createTempFile(path.getParent(), "power_building-", ".tmp");
    try {
      try (Writer w = Files.newBufferedWriter(temp)) {
        p.store(w, "BuildingFeatures; hotkeys are saved in Minecraft options.txt");
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
