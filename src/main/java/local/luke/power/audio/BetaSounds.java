package local.luke.power.audio;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** The launcher cache contains later-version assets too; those are not game sounds. */
public final class BetaSounds {
  private static final Set<String> VANILLA;
  static {
    try (var in = BetaSounds.class.getResourceAsStream("/assets/powerbeta/beta-sounds.txt")) {
      if (in == null) throw new IOException("Missing Beta sound list");
      VANILLA = Set.copyOf(new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList());
    } catch (IOException e) { throw new IllegalStateException(e); }
  }
  public static boolean contains(String id) {
    return VANILLA.contains(id) || id.startsWith("power_controls:");
  }
}
