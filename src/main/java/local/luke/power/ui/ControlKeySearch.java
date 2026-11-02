package local.luke.power.ui;

import java.util.Locale;

/** Literal binding search keeps key combinations separate from action names and descriptions. */
final class ControlKeySearch {
  private ControlKeySearch() {}

  static int score(String query, String label, int encoded, Integer exact) {
    if (exact != null) return exact == encoded ? 0 : FuzzySearch.NONE;
    String needle = normalize(query), binding = normalize(label);
    int index = binding.indexOf(needle);
    return index < 0 ? FuzzySearch.NONE : index;
  }

  private static String normalize(String value) {
    return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "")
        .replace("control", "ctrl");
  }
}
