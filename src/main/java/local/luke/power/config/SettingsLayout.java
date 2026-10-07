package local.luke.power.config;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Presentation order is independent of backend registration and reflection order. */
public final class SettingsLayout {
  private static final Map<String, List<String>> GROUPS = load();
  private static final List<String> PAGES = List.copyOf(GROUPS.keySet());

  private SettingsLayout() {}

  private static Map<String, List<String>> load() {
    try (var in = SettingsLayout.class.getResourceAsStream("/assets/powerbeta/settings-layout.json")) {
      if (in == null) throw new IOException("Missing settings layout");
      var data = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
      Map<String, List<String>> result = new LinkedHashMap<>();
      data.entrySet().forEach(e -> {
        List<String> groups = new ArrayList<>();
        e.getValue().getAsJsonArray().forEach(v -> groups.add(v.getAsString()));
        if (new HashSet<>(groups).size() != groups.size()) throw new IllegalStateException("Duplicate group: " + e.getKey());
        result.put(e.getKey(), List.copyOf(groups));
      });
      return Collections.unmodifiableMap(result);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public static List<String> groups(String page) { return GROUPS.getOrDefault(page, List.of()); }

  public static List<Setting> ordered(List<Setting> settings) {
    return settings.stream().sorted(Comparator
        .comparingInt((Setting s) -> index(PAGES, s.page))
        .thenComparingInt(s -> index(groups(s.page), s.group))
        .thenComparingDouble(s -> Catalog.number(Catalog.metadata(s.id), "order", Double.MAX_VALUE)))
        .toList();
  }

  private static int index(List<String> values, String value) {
    int index = values.indexOf(value);
    return index < 0 ? Integer.MAX_VALUE : index;
  }
}
