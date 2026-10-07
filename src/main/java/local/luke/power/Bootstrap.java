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
      ConfigDefaults.prepare(game);
      local.luke.power.audio.Mp3Converter.probe();
      prepared = true;
    } catch (Exception e) {
      throw new IllegalStateException("Power Beta could not safely prepare its configuration", e);
    }
  }
}
