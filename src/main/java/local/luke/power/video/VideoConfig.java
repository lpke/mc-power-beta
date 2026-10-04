package local.luke.power.video;

import local.luke.power.PowerBeta;
import local.luke.power.config.Catalog;
import local.luke.power.controls.util.ModOptions;
import local.luke.power.storage.PowerConfig;
import net.minecraft.client.Minecraft;

public final class VideoConfig {
  private static VideoSettings current;

  public static VideoSettings current() {
    if (current == null) {
      try {
        current = PowerConfig.read("video", VideoSettings.class);
        // Keep existing Fast/Fancy preferences during the first migration.
        if (PowerConfig.section("video").size() == 0) {
          Minecraft mc =
              net.fabricmc.loader.api.FabricLoader.getInstance().getGameInstance()
                      instanceof Minecraft game
                  ? game
                  : null;
          boolean fancy = mc == null || mc.options == null || mc.options.fancyGraphics;
          current.leaves =
              current.grass =
                  current.clouds = current.water = current.weather = current.shadows = fancy;
        }
        current.validate();
      } catch (Exception e) {
        PowerBeta.LOG.error("Could not load video settings", e);
        current = new VideoSettings();
      }
      ModOptions.renderDistanceCycle =
          current.fogCycle.stream().mapToInt(Integer::intValue).toArray();
    }
    return current;
  }

  public static VideoSettings copy() {
    return Catalog.JSON.fromJson(Catalog.JSON.toJson(current()), VideoSettings.class);
  }

  public static void preview(VideoSettings next, Minecraft mc) {
    next.validate();
    boolean rebuild = next.leaves != current().leaves || next.grass != current().grass;
    current = Catalog.JSON.fromJson(Catalog.JSON.toJson(next), VideoSettings.class);
    ModOptions.renderDistanceCycle =
        current.fogCycle.stream().mapToInt(Integer::intValue).toArray();
    if (rebuild && mc.worldRenderer != null) mc.worldRenderer.method_1537();
  }
}
