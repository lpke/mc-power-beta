package local.luke.tweaks.config;

import java.io.IOException;
import java.util.function.Consumer;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Config {
  public static final Logger LOG = LogManager.getLogger("LpkeTweaks");
  private static final ConfigStore STORE =
      new ConfigStore(FabricLoader.getInstance().getConfigDir().resolve("lpketweaks.properties"));
  private static Settings current = load();

  private static Settings load() {
    try {
      return STORE.load();
    } catch (IOException | IllegalArgumentException e) {
      LOG.error("Could not load LpkeTweaks settings; preserving file and using defaults", e);
      return new Settings();
    }
  }

  public static Settings current() {
    return current;
  }

  public static void apply(Settings settings) throws IOException {
    Settings next = settings.copy();
    STORE.save(next);
    current = next;
    if (!next.autoWalk) local.luke.autowalk.AutoWalk.stop();
  }

  public static void update(Consumer<Settings> edit) throws IOException {
    Settings next = current.copy();
    edit.accept(next);
    apply(next);
  }
}
