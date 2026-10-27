package local.luke.power.ui;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/** Stable music search ranks complete names and literal matches before fuzzy matches. */
public final class MusicSearch {
  public static final int NONE = FuzzySearch.NONE;
  private MusicSearch() {}

  private static String normalize(String value) {
    return Normalizer.normalize(value, Normalizer.Form.NFKD).replaceAll("\\p{M}", "")
        .strip().toLowerCase(Locale.ROOT);
  }

  public static int score(String query, String label) {
    String match = normalize(query), name = normalize(label);
    if (match.isEmpty() || name.equals(match)) return 0;
    if (name.startsWith(match)) return 1;
    if (name.contains(match)) return 2;
    int fuzzy = FuzzySearch.score(match, name, "");
    return fuzzy == NONE ? NONE : 100 + fuzzy;
  }

  /** Built-in aliases and audio filename stems are song names too. */
  public static int trackScore(String query, String label) {
    int best = score(query, label), alias = label.lastIndexOf(" (");
    String filename = alias >= 0 && label.endsWith(")") ? label.substring(0, alias) : label;
    if (!filename.equals(label)) best = Math.min(best, score(query, label.substring(alias + 2, label.length() - 1)));
    int dot = filename.lastIndexOf('.');
    if (dot > 0 && java.util.Set.of("ogg", "mp3", "wav", "flac").contains(filename.substring(dot + 1).toLowerCase(Locale.ROOT))) {
      best = Math.min(best, score(query, filename.substring(0, dot)));
      best = Math.min(best, score(query, filename.substring(0, dot).replace('_', ' ')));
    }
    return best;
  }

  public static <T> List<T> filter(List<T> source, String query, Function<T, String> label) {
    if (query.isBlank()) return source;
    return ranked(source, value -> score(query, label.apply(value)));
  }
  public static <T> List<T> tracks(List<T> source, String query, Function<T, String> label) {
    if (query.isBlank()) return source;
    return ranked(source, value -> trackScore(query, label.apply(value)));
  }
  private static <T> List<T> ranked(List<T> source, ToIntFunction<T> score) {
    record Match<T>(T value, int score) {}
    return source.stream().map(value -> new Match<>(value, score.applyAsInt(value)))
        .filter(match -> match.score() < NONE).sorted(Comparator.comparingInt(Match::score))
        .map(Match::value).toList();
  }
}
