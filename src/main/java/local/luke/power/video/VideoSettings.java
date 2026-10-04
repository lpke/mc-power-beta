package local.luke.power.video;

import java.util.*;

public final class VideoSettings {
  public boolean leaves = true,
      grass = true,
      clouds = true,
      water = true,
      weather = true,
      shadows = true;
  public List<Integer> fogCycle = new ArrayList<>(List.of(12, 8, 4, 2));

  public void validate() {
    if (fogCycle == null || fogCycle.isEmpty() || fogCycle.size() > 16)
      throw new IllegalArgumentException("Use 1 to 16 distances, for example [12, 8, 4, 2]");
    Set<Integer> seen = new HashSet<>();
    for (Integer n : fogCycle)
      if (n == null || n < 2 || n > 32 || !seen.add(n))
        throw new IllegalArgumentException(
            "Each distance must be unique and between 2 and 32 chunks");
  }
}
