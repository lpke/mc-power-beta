package local.luke.power.audio;

import java.util.*;
import java.util.function.Predicate;

/** Dedicated menu sources replace world rotation before per-track eligibility is applied. */
final class MusicRotation {
  private MusicRotation() {}

  static <T> List<T> menu(List<T> world, List<T> availableMenu, boolean overrides,
      Predicate<T> eligible) {
    List<T> tracks = new ArrayList<>();
    if (!overrides || availableMenu.isEmpty()) tracks.addAll(world);
    for (T track : availableMenu) if (eligible.test(track)) tracks.add(track);
    return tracks;
  }
}
