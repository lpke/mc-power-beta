package local.luke.power.audio;

import java.util.*;

/** Separate from the sound engine so selection and repeat prevention can be tested. */
public final class TrackSelector {
  private String previous = "";
  private int sequence;

  public <T> T choose(
      List<T> tracks,
      boolean shuffle,
      boolean avoidRepeats,
      Random random,
      java.util.function.Function<T, String> key) {
    if (tracks.isEmpty()) return null;
    int index = shuffle ? random.nextInt(tracks.size()) : Math.floorMod(sequence++, tracks.size());
    if (avoidRepeats && tracks.size() > 1 && key.apply(tracks.get(index)).equals(previous))
      index = (index + 1 + (shuffle ? random.nextInt(tracks.size() - 1) : 0)) % tracks.size();
    T chosen = tracks.get(index);
    previous = key.apply(chosen);
    return chosen;
  }
}
