package local.luke.power.audio;

import java.util.*;
import local.luke.power.PowerBeta;
import local.luke.power.config.Catalog;
import local.luke.power.storage.PowerConfig;

/** Queue persistence is independent of the open settings draft. */
public final class MusicRequests {
  private static MusicQueue queue = load();

  private static MusicQueue load() {
    try {
      var value = PowerConfig.read("musicQueue", MusicQueue.class);
      value.validate();
      return value;
    } catch (Exception e) {
      PowerBeta.LOG.error("Could not load music queue", e);
      return new MusicQueue();
    }
  }

  public static List<String> tracks() {
    return List.copyOf(queue.tracks);
  }

  public static void edit(java.util.function.Consumer<MusicQueue> edit) throws java.io.IOException {
    MusicQueue next = Catalog.JSON.fromJson(Catalog.JSON.toJson(queue), MusicQueue.class);
    edit.accept(next);
    next.validate();
    PowerConfig.save("musicQueue", next);
    queue = next;
  }
}
