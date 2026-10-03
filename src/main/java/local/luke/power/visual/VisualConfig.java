package local.luke.power.visual;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import local.luke.power.PowerBeta;
import local.luke.power.config.*;
import net.fabricmc.loader.api.FabricLoader;

public final class VisualConfig {
  private static VisualSettings current = load();
  public static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("power-beta/visual.json"); }
  private static VisualSettings load() {
    try {
      if (!Files.exists(path())) return new VisualSettings();
      VisualSettings s = Catalog.JSON.fromJson(Files.readString(path()), VisualSettings.class);
      s.validate();
      return s;
    } catch (Exception e) { PowerBeta.LOG.error("Could not load visual settings", e); return new VisualSettings(); }
  }
  public static VisualSettings current() { return current; }
  public static VisualSettings copy() { return Catalog.JSON.fromJson(Catalog.JSON.toJson(current), VisualSettings.class); }
  public static void preview(VisualSettings s) { s.validate(); current = Catalog.JSON.fromJson(Catalog.JSON.toJson(s), VisualSettings.class); }
  public static void save(VisualSettings s) throws Exception {
    s.validate();
    FileTransaction.atomicWrite(path(), Catalog.JSON.toJson(s).getBytes(StandardCharsets.UTF_8));
    preview(s);
  }
}
