package local.luke.power.config.backend;

import com.google.gson.*;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.video.*;
import net.minecraft.client.Minecraft;

public final class VideoBackend implements Backend {
  private final Minecraft mc;

  private VideoBackend(Minecraft mc) {
    this.mc = mc;
  }

  public static void register(ConfigSession session, Minecraft mc) throws Exception {
    var entries = new ArrayList<Setting>();
    var current = VideoConfig.copy();
    for (String[] spec :
        new String[][] {
          {
            "leaves",
            "Transparent leaves",
            "Draw gaps between leaves. Turning this off makes leaf blocks opaque."
          },
          {
            "grass",
            "Detailed grass sides",
            "Blend biome-coloured grass over the sides of grass blocks."
          },
          {
            "clouds",
            "Three-dimensional clouds",
            "Use thick clouds instead of flat clouds. Clouds must also be enabled under Sky."
          },
          {
            "water",
            "Layered transparency",
            "Use an extra depth pass to draw overlapping water and translucent surfaces correctly."
          },
          {
            "weather",
            "Detailed weather",
            "Draw rain and snow farther away, with more splash particles."
          },
          {
            "shadows",
            "Entity shadows",
            "Draw ground shadows beneath players, creatures and dropped items."
          }
        }) {
      entries.add(
          new Setting(
              "video." + spec[0],
              "video",
              "Video",
              "Quality",
              spec[1],
              spec[2],
              Setting.Kind.BOOLEAN,
              new JsonPrimitive(VideoSettings.class.getField(spec[0]).getBoolean(current)),
              new JsonPrimitive(true),
              0,
              1,
              1,
              List.of(),
              false));
    }
    entries.add(
        new Setting(
            "video.fogCycle",
            "video",
            "Video",
            "Rendering",
            "Fog key distances",
            "Distances in chunks, in cycling order. F advances; Shift+F reverses. Beta defaults:"
                + " [12, 8, 4, 2]. Each value must be 2 to 32.",
            Setting.Kind.LIST,
            Catalog.JSON.toJsonTree(current.fogCycle),
            Catalog.JSON.toJsonTree(new VideoSettings().fogCycle),
            0,
            0,
            1,
            List.of(),
            false));
    session.add(new VideoBackend(mc), entries);
  }

  public String id() {
    return "video";
  }

  public List<Path> files() {
    return List.of(PowerConfig.path());
  }

  private VideoSettings draft(Map<String, JsonElement> changes) {
    JsonObject json = Catalog.JSON.toJsonTree(VideoConfig.copy()).getAsJsonObject();
    changes.forEach((key, value) -> json.add(key.substring(6), value));
    VideoSettings next = Catalog.JSON.fromJson(json, VideoSettings.class);
    next.validate();
    return next;
  }

  public void validate(Map<String, JsonElement> values) {
    draft(values);
  }

  public boolean previews(Setting s) {
    return true;
  }

  public void preview(Map<String, JsonElement> values) {
    VideoConfig.preview(draft(values), mc);
  }

  public void apply(Map<String, JsonElement> values) throws Exception {
    var next = draft(values);
    PowerConfig.save("video", next);
    VideoConfig.preview(next, mc);
  }
}
