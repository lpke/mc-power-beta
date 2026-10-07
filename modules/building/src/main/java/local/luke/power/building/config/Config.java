package local.luke.power.building.config;

import java.io.IOException;
import java.util.function.Consumer;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Config {
  public static final Logger LOG = LogManager.getLogger("BuildingFeatures");
  private static Settings current = load();

  private static Settings load() {
    try {
      Settings value = local.luke.power.storage.PowerConfig.read("building", Settings.class);
      SettingsValidator.validate(value);
      return value;
    } catch (IllegalArgumentException e) {
      LOG.error("Could not load BuildingFeatures settings; preserving file and using defaults", e);
      return new Settings();
    }
  }

  public static Settings current() {
    return current;
  }

  public static void apply(Settings settings) throws IOException {
    Settings next = settings.copy();
    SettingsValidator.validate(next);
    local.luke.power.storage.PowerConfig.save("building", next);
    preview(next);
  }

  public static void preview(Settings settings) {
    Settings next = settings.copy();
    SettingsValidator.validate(next);
    current = next;
    if (!next.autoWalk) local.luke.power.autowalk.AutoWalk.stop();
  }

  public static void update(Consumer<Settings> edit) throws IOException {
    Settings next = current.copy();
    edit.accept(next);
    apply(next);
  }
}
