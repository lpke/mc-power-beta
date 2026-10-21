package local.luke.power.world;

import local.luke.power.storage.PowerConfig;

public final class WorldToolsConfig {
  private static WorldToolsSettings value;
  private WorldToolsConfig() {}
  public static synchronized WorldToolsSettings current() {
    if (value == null) value = PowerConfig.read("worldTools", WorldToolsSettings.class);
    return value;
  }
  public static synchronized WorldToolsSettings copy() {
    WorldToolsSettings source = current(), copy = new WorldToolsSettings();
    copy.backup = source.backup; copy.duplicate = source.duplicate; copy.recreate = source.recreate;
    return copy;
  }
  public static synchronized void save(WorldToolsSettings next) throws java.io.IOException {
    PowerConfig.save("worldTools", next); value = next;
  }
}
