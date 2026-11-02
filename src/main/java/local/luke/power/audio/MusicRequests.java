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
      var value = PowerConfig.readMusicQueue(MusicQueue.class);
      value.validate();
      value.tracks = List.copyOf(value.tracks);
      return value;
    } catch (Exception e) {
      PowerBeta.LOG.error("Could not load music queue", e);
      var empty = new MusicQueue();
      empty.tracks = List.of();
      return empty;
    }
  }

  public static List<String> tracks() {
    return queue.tracks;
  }

  public static void edit(java.util.function.Consumer<MusicQueue> edit) throws java.io.IOException {
    MusicQueue next = Catalog.JSON.fromJson(Catalog.JSON.toJson(queue), MusicQueue.class);
    edit.accept(next);
    next.validate();
    next.tracks = List.copyOf(next.tracks);
    PowerConfig.saveMusicQueueDeferred(next);
    queue = next;
  }

  /** A delayed playback acknowledgement must never remove a newly edited row. */
  static boolean consume(List<String> snapshot) throws java.io.IOException {
    if (tracks() != snapshot || snapshot.isEmpty()) return false;
    edit(next -> next.tracks.remove(0));
    return true;
  }
}
