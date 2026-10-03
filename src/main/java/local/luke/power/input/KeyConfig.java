package local.luke.power.input;

import com.google.gson.*;
import java.nio.file.*;
import local.luke.power.storage.PowerConfig;
import java.util.*;
import local.luke.power.PowerBeta;
import local.luke.power.config.FileTransaction;
import net.fabricmc.loader.api.FabricLoader;

public final class KeyConfig {
  private static boolean loaded;
  private KeyConfig() {}
  public static Path file() {
    return PowerConfig.path();
  }
  public static void load() {
    if (loaded) return;
    loaded = true;
    if (!Files.exists(file())) return;
    try {
      JsonObject json = PowerConfig.section("bindings");
      Map<String,Integer> values = new HashMap<>();
      for (var entry : json.entrySet()) values.put(entry.getKey(), entry.getValue().getAsBigDecimal().intValueExact());
      Bindings.configure(values);
    } catch (Exception e) {
      PowerBeta.LOG.error("Could not load modifier bindings; preserving file and using plain keys", e);
    }
  }
  public static void save(Map<String,Integer> values) throws java.io.IOException {
    Bindings.configure(values);
    PowerConfig.save("bindings", values);
  }
}
