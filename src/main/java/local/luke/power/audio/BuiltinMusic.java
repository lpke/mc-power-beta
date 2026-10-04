package local.luke.power.audio;

import java.util.Map;

/** C418's published track titles; filenames remain the stable setting/queue identifiers. */
public final class BuiltinMusic {
  private BuiltinMusic() {}
  private static final Map<String, String> TITLES = Map.ofEntries(
      Map.entry("calm1", "Minecraft"), Map.entry("calm2", "Clark"), Map.entry("calm3", "Sweden"),
      Map.entry("hal1", "Subwoofer Lullaby"), Map.entry("hal2", "Living Mice"),
      Map.entry("hal3", "Haggstrom"), Map.entry("hal4", "Danny"),
      Map.entry("nuance1", "Key"), Map.entry("nuance2", "Oxygène"),
      Map.entry("piano1", "Dry Hands"), Map.entry("piano2", "Wet Hands"), Map.entry("piano3", "Mice on Venus"));

  public static String label(String filename) {
    int dot = filename.lastIndexOf('.');
    String title = TITLES.get(dot < 0 ? filename : filename.substring(0, dot));
    return title == null ? filename : filename + " (" + title + ")";
  }
}
