package local.luke.power.chat;

import java.util.Collection;

/** Match without changing the spelling or length of a displayed completion. */
public final class ChatCompletion {
  private ChatCompletion() {}

  public static boolean startsWith(String choice, String input) {
    return choice.regionMatches(true, 0, input, 0, input.length());
  }

  public static String[] suffixes(String input, Collection<String> choices) {
    return choices.stream().distinct().filter(value -> startsWith(value, input))
        .map(value -> value.substring(input.length())).sorted().toArray(String[]::new);
  }

  /** Full tokens, best first. The chat list draws its first entry nearest the input. */
  public static String[] ranked(String input, Collection<String> choices) {
    record Match(String value, int score) {}
    return choices.stream().distinct()
        .map(value -> new Match(value, score(input, value)))
        .filter(match -> match.score() < local.luke.power.ui.FuzzySearch.NONE)
        .sorted(java.util.Comparator.comparingInt(Match::score)
            .thenComparingInt(match -> match.value().length())
            .thenComparing(Match::value, String.CASE_INSENSITIVE_ORDER))
        .map(Match::value).toArray(String[]::new);
  }

  private static int score(String input, String choice) {
    int colon = input.indexOf(':');
    if (colon >= 0) {
      if (!startsWith(choice, input.substring(0, colon + 1))) return local.luke.power.ui.FuzzySearch.NONE;
      input = input.substring(colon + 1);
      choice = choice.substring(colon + 1);
    } else if (input.startsWith("/") && choice.startsWith("/")) {
      input = input.substring(1); choice = choice.substring(1);
    }
    return local.luke.power.ui.FuzzySearch.score(input, choice, "");
  }
}
