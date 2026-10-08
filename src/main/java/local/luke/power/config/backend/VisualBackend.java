package local.luke.power.config.backend;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.visual.*;
import net.minecraft.client.Minecraft;

public final class VisualBackend implements Backend {
  private final Minecraft mc;
  private VisualBackend(Minecraft mc) { this.mc = mc; }
  public static void register(ConfigSession session, Minecraft mc) throws Exception {
    VisualBackend b = new VisualBackend(mc);
    VisualSettings current = VisualConfig.copy(), defaults = new VisualSettings();
    List<Setting> entries = new ArrayList<>();
    for (var f : VisualSettings.class.getFields()) {
      boolean distance = f.getName().equals("thirdPersonDistance");
      entries.add(Catalog.setting("visual." + f.getName(), b.id(), distance ? "Camera" : "Video",
          distance ? "Third person" : "Texture overrides", distance ? "Camera distance" : Catalog.words(f.getName()),
          distance ? "Distance behind the player in blocks. Walls can move the camera closer; 4 is vanilla."
              : "Overrides only this texture in the selected pack. Can be combined with the other overrides.",
          distance ? Setting.Kind.DECIMAL : Setting.Kind.BOOLEAN,
          Catalog.JSON.toJsonTree(f.get(current)), Catalog.JSON.toJsonTree(f.get(defaults)),
          distance ? 1 : 0, distance ? 12 : 1, distance ? .25 : 1, List.of(), false));
    }
    session.add(b, entries);
  }
  public String id() { return "visual"; }
  public List<Path> files() { return List.of(VisualConfig.path()); }
  private VisualSettings draft(Map<String, JsonElement> values) throws Exception {
    VisualSettings s = VisualConfig.copy();
    for (var e : values.entrySet()) {
      var f = VisualSettings.class.getField(e.getKey().substring(7));
      f.set(s, Catalog.JSON.fromJson(e.getValue(), f.getType()));
    }
    s.validate(); return s;
  }
  public void validate(Map<String, JsonElement> values) throws Exception { draft(values); }
  public boolean previews(Setting s) { return true; }
  public void preview(Map<String, JsonElement> values) throws Exception {
    VisualSettings before = VisualConfig.copy();
    VisualConfig.preview(draft(values));
    VisualSettings after = VisualConfig.current();
    if (before.redstonePowerLevels != after.redstonePowerLevels && mc.world != null)
      mc.worldRenderer.method_1537();
    if (before.softRain != after.softRain || before.softSnow != after.softSnow
        || before.oldCobble != after.oldCobble || before.oldBricks != after.oldBricks) {
      TextureOverrides.clear();
      mc.textureManager.method_1096();

    }
  }
  public void apply(Map<String, JsonElement> values) throws Exception {
    preview(values);
    VisualConfig.save(VisualConfig.current());
  }
}
