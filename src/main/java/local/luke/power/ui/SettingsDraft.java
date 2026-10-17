package local.luke.power.ui;

import com.google.gson.JsonElement;
import java.util.*;
import java.util.function.Predicate;
import local.luke.power.config.Setting;

/** Editor-owned copies. History and nested controls never mutate the Options session. */
public final class SettingsDraft {
  private final List<Setting> settings;
  private final Map<String, Setting> byId = new LinkedHashMap<>();
  private final Map<String, JsonElement> original;
  private final Deque<Map<String, JsonElement>> undo = new ArrayDeque<>();

  public SettingsDraft(Collection<Setting> source, Predicate<Setting> includes) {
    settings = source.stream().filter(includes).map(s -> new Setting(s.id, s.backend, s.page,
        s.group, s.label, s.description, s.kind, s.value, s.defaultValue, s.min, s.max,
        s.step, s.choices, s.restart)).toList();
    settings.forEach(s -> byId.put(s.id, s));
    original = snapshot();
  }
  public List<Setting> settings() { return settings; }
  public Setting get(String id) {
    Setting setting = byId.get(id);
    if (setting == null) throw new IllegalArgumentException("Missing editor setting " + id);
    return setting;
  }
  public Map<String, JsonElement> snapshot() {
    Map<String, JsonElement> values = new LinkedHashMap<>();
    settings.forEach(s -> values.put(s.id, s.value.deepCopy()));
    return values;
  }
  public boolean changed() { return !snapshot().equals(original); }
  public boolean canUndo() { return !undo.isEmpty(); }
  public void checkpoint() {
    Map<String, JsonElement> state = snapshot();
    if (!undo.isEmpty() && undo.peek().equals(state)) return;
    if (undo.size() == 64) undo.removeLast();
    undo.push(state);
  }
  public void restore(Map<String, JsonElement> values) {
    settings.forEach(s -> s.value = values.get(s.id).deepCopy());
  }
  public void undo() { if (!undo.isEmpty()) restore(undo.pop()); }
}
