package local.luke.power.commands.util;

import java.util.*;

/** Registry paths are usable without a namespace when they identify one item. */
public final class ItemNames {
  private final Map<String, Integer> identifiers = new TreeMap<>();
  private final Map<String, Map<Integer, List<String>>> paths = new TreeMap<>();

  public ItemNames(Map<String, Integer> registered) {
    registered.forEach((name, id) -> {
      String identifier = name.toLowerCase(Locale.ROOT);
      identifiers.put(identifier, id);
      String path = identifier.substring(identifier.indexOf(':') + 1);
      paths.computeIfAbsent(path, ignored -> new TreeMap<>())
          .computeIfAbsent(id, ignored -> new ArrayList<>()).add(identifier);
    });
  }

  public int resolve(String name) {
    String key = name.toLowerCase(Locale.ROOT);
    if (key.contains(":")) return identifiers.getOrDefault(key, -1);
    Map<Integer, List<String>> matches = paths.get(key);
    if (matches == null) return -1;
    if (matches.size() > 1) {
      String choices = matches.values().stream().flatMap(Collection::stream).sorted()
          .reduce((a, b) -> a + ", " + b).orElseThrow();
      throw new IllegalArgumentException("Ambiguous item '" + name + "'. Use " + choices + ".");
    }
    return matches.keySet().iterator().next();
  }

  public String[] suggest(String input) {
    Collection<String> choices = input.contains(":") ? identifiers.keySet() : paths.keySet();
    return choices.stream()
        .filter(choice -> choice.regionMatches(true, 0, input, 0, input.length()))
        .map(choice -> choice.substring(input.length())).toArray(String[]::new);
  }
}
