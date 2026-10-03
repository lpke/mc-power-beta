package local.luke.power;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import local.luke.power.config.*;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

/** First-run defaults are installed before config libraries and recipe registries initialize. */
public final class Bootstrap implements PreLaunchEntrypoint {
  private static boolean prepared;

  public void onPreLaunch() {
    prepare();
  }

  public static synchronized void prepare() {
    if (prepared) return;
    Path game = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
    try {
      FileTransaction.recover(game);
      try (var in = Bootstrap.class.getResourceAsStream("/assets/powerbeta/defaults/index.txt")) {
        if (in == null) throw new IOException("Missing defaults index");
        for (String name : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
          if (name.isBlank()) continue;
          Path path = game.resolve(name).normalize();
          if (!path.startsWith(game)) throw new IOException("Invalid default path");
          if (Files.exists(path)) continue;
          try (var source =
              Bootstrap.class.getResourceAsStream("/assets/powerbeta/defaults/" + name)) {
            if (source == null) throw new IOException("Missing defaults: " + name);
            FileTransaction.atomicWrite(path, source.readAllBytes());
          }
        }
      }
      prepared = true;
    } catch (Exception e) {
      throw new IllegalStateException("Power Beta could not safely prepare its configuration", e);
    }
  }
}
