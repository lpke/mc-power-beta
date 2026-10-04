package local.luke.power.audio;

import java.util.*;

/** Bounded ordered requests. Repeated tracks are intentional; edits address row indices. */
public final class MusicQueue {
  public List<String> tracks = new ArrayList<>();

  public void validate() {
    if (tracks == null
        || tracks.size() > 256
        || tracks.stream()
            .anyMatch(id -> id == null || !id.startsWith("music:") || id.length() > 512))
      throw new IllegalArgumentException("Use at most 256 queued tracks");
  }

  public void add(String id) {
    if (tracks.size() >= 256) throw new IllegalArgumentException("Queue is full (256 tracks)");
    if (id == null || !id.startsWith("music:") || id.length() > 512) throw new IllegalArgumentException("Invalid track");
    tracks.add(id);
  }

  public void move(int index, int offset) {
    int next = index + offset;
    if (index >= 0 && index < tracks.size() && next >= 0 && next < tracks.size())
      Collections.swap(tracks, index, next);
  }
}
