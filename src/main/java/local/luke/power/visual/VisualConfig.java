package local.luke.power.visual;

import java.nio.file.*;
import local.luke.power.storage.PowerConfig;
import java.nio.charset.StandardCharsets;
import local.luke.power.PowerBeta;
import local.luke.power.config.*;
import net.fabricmc.loader.api.FabricLoader;

public final class VisualConfig {
  private static VisualSettings current = load();
  static { local.luke.power.input.InteractionState.containerCarryEnabled=current.containerCarry; }
  public static Path path() { return PowerConfig.path(); }
  private static VisualSettings load() {
    try {
      if (!Files.exists(path())) return new VisualSettings();
      VisualSettings s = PowerConfig.read("visual", VisualSettings.class);
      s.validate();
      return s;
    } catch (Exception e) { PowerBeta.LOG.error("Could not load visual settings", e); return new VisualSettings(); }
  }
  public static VisualSettings current() { return current; }
  public static VisualSettings copy() { return Catalog.JSON.fromJson(Catalog.JSON.toJson(current), VisualSettings.class); }
  public static void preview(VisualSettings s) { s.validate(); current = Catalog.JSON.fromJson(Catalog.JSON.toJson(s), VisualSettings.class); local.luke.power.input.InteractionState.containerCarryEnabled=current.containerCarry; }
  public static void save(VisualSettings s) throws Exception {
    s.validate();
    PowerConfig.save("visual", s);
    preview(s);
  }
}
