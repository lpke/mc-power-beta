package local.luke.power.creative.config;

import java.io.IOException;
import local.luke.power.storage.PowerConfig;

public final class Config {
  public static final org.apache.logging.log4j.Logger LOG = org.apache.logging.log4j.LogManager.getLogger("Power Beta Creative");
  private static Settings settings = load();
  private static Settings load() {
    Settings value = PowerConfig.read("creative", Settings.class);
    value.validate(); return value;
  }
  public static Settings current() { return settings; }
  public static void preview(Settings next) { next.validate(); settings = next.copy(); }
  public static void apply(Settings next) throws IOException {
    next.validate(); PowerConfig.save("creative", next); preview(next);
  }
}
