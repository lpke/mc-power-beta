package local.luke.power.hud;

import java.util.*;

/** Cached world values and editor samples use the same labels and line composition. */
public record InfoHudData(String coordinates, String direction, String biome, String time,
    String day, String chunk, String fps) {
  public static InfoHudData sample() {
    return new InfoHudData(HudText.coordinates(128, 64, -32), HudText.direction(0), "Forest",
        HudText.time(4500), "12", "8 / -2", "120");
  }
  public List<String> lines(InfoHudSettings.Cluster c) {
    List<String> lines = new ArrayList<>(7);
    if (c.coordinates || c.direction) add(lines, HudText.location(coordinates, direction, c.coordinates, c.direction));
    if (c.biome) add(lines, labelled("Biome", biome));
    if (c.timeOfDay) add(lines, labelled("Time", time));
    if (c.day) add(lines, labelled("Day", day));
    if (c.chunk) add(lines, labelled("Chunk", chunk));
    if (c.fps) add(lines, labelled("FPS", fps));
    return lines;
  }
  private static String labelled(String label, String value) { return value.isEmpty() ? "" : label + ": " + value; }
  private static void add(List<String> lines, String line) { if (!line.isEmpty()) lines.add(line); }
}
