package local.luke.power.commands.util;

import java.util.*;

public final class CommandSuggestions {
  private CommandSuggestions() {}

  public static String[] suffix(String input, Collection<String> choices) {
    return local.luke.power.chat.ChatCompletion.suffixes(input, choices);
  }

  public static String[] targets(SharedCommandSource source, String input) {
    List<String> choices = new ArrayList<>(List.of("@s", "@p", "@a", "@r", "@e"));
    if (source.getPlayer() != null) choices.add(source.getPlayer().name);
    return suffix(input, choices);
  }

  public static String[] targetsAndCoordinates(SharedCommandSource source, String input) {
    List<String> result = new ArrayList<>(Arrays.asList(targets(source, input)));
    result.addAll(Arrays.asList(suffix(input, List.of("~", "^"))));
    return result.toArray(new String[0]);
  }
}
