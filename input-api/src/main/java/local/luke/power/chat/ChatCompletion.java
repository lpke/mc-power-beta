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
}
