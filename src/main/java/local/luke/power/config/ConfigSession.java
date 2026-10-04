package local.luke.power.config;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

public final class ConfigSession {
  private final List<Setting> settings = new ArrayList<>();
  private final Map<String, Backend> backends = new LinkedHashMap<>();

  public void add(Backend backend, Collection<Setting> entries) {
    if (backends.putIfAbsent(backend.id(), backend) != null)
      throw new IllegalArgumentException("Duplicate backend " + backend.id());
    for (Setting s : entries) {
      if (settings.stream().anyMatch(v -> v.id.equals(s.id)))
        throw new IllegalArgumentException("Duplicate setting " + s.id);
      settings.add(s);
    }
  }

  /** Append newly discovered options without replacing existing drafts or preview state. */
  public boolean addDiscovered(Collection<Setting> entries) {
    for (Setting setting : entries)
      if (!backends.containsKey(setting.backend)) throw new IllegalArgumentException("Unknown backend " + setting.backend);
    boolean added = false;
    Set<String> ids = new HashSet<>();
    settings.forEach(s -> ids.add(s.id));
    for (Setting setting : entries) if (ids.add(setting.id)) { settings.add(setting); added = true; }
    return added;
  }

  public List<Setting> settings() {
    return Collections.unmodifiableList(settings);
  }

  public long changes() {
    return settings.stream().filter(Setting::changed).count();
  }

  public boolean restartRequired() {
    return settings.stream().anyMatch(s -> s.changed() && s.restart);
  }

  public void link(Setting changed) {
    if (changed.id.equals("tweaks.placement.enabled")) {
      boolean tied =
          settings.stream()
              .filter(s -> s.id.equals("tweaks.placement.restrictionTiedToFast"))
              .findFirst()
              .map(s -> s.value.getAsBoolean())
              .orElse(false);
      if (tied)
        settings.stream()
            .filter(s -> s.id.equals("tweaks.placement.restrictionEnabled"))
            .forEach(s -> s.value = changed.value.deepCopy());
    }
  }

  private final Map<String, JsonElement> previewed = new HashMap<>();

  public void preview() throws Exception { preview(false); }

  public void preview(boolean automatic) throws Exception {
    Map<String, Map<String, JsonElement>> changes = new LinkedHashMap<>(), old = new LinkedHashMap<>();
    for (Setting s : settings) {
      Backend b = backends.get(s.backend);
      JsonElement before = previewed.getOrDefault(s.id, s.original());
      if (!s.restart && (previewed.containsKey(s.id) || (automatic ? b.previewsAutomatically(s) : b.previews(s))) && !s.value.equals(before)) {
        s.validate(s.value);
        changes.computeIfAbsent(s.backend, k -> new LinkedHashMap<>()).put(s.id, s.value.deepCopy());
        old.computeIfAbsent(s.backend, k -> new LinkedHashMap<>()).put(s.id, before.deepCopy());
      }
    }
    for (var e : changes.entrySet()) backends.get(e.getKey()).validate(e.getValue());
    List<String> attempted = new ArrayList<>();
    try {
      for (var e : changes.entrySet()) {
        attempted.add(e.getKey());
        backends.get(e.getKey()).preview(e.getValue());
      }
    } catch (Exception failure) {
      Collections.reverse(attempted);
      for (String id : attempted) try { backends.get(id).preview(old.get(id)); }
      catch (Exception rollback) { failure.addSuppressed(rollback); }
      throw failure;
    }
    changes.values().forEach(previewed::putAll);
  }

  public void discard() throws Exception {
    for (Setting s : settings) s.value = s.original();
    preview();
  }

  public void save(Path gameDir) throws Exception {
    Map<String, Map<String, JsonElement>> changes = new LinkedHashMap<>(),
        old = new LinkedHashMap<>();
    for (Setting s : settings)
      if (s.changed()) {
        s.validate(s.value);
        changes
            .computeIfAbsent(s.backend, k -> new LinkedHashMap<>())
            .put(s.id, s.value.deepCopy());
        old.computeIfAbsent(s.backend, k -> new LinkedHashMap<>()).put(s.id, s.original());
      }
    if (changes.isEmpty()) return;
    for (var e : changes.entrySet()) backends.get(e.getKey()).validate(e.getValue());
    List<Path> files =
        changes.keySet().stream()
            .flatMap(k -> backends.get(k).files().stream())
            .distinct()
            .toList();
    try (FileTransaction transaction = FileTransaction.begin(gameDir, files)) {
      List<String> attempted = new ArrayList<>();
      try {
        for (var e : changes.entrySet()) {
          attempted.add(e.getKey());
          backends.get(e.getKey()).apply(e.getValue());
        }
        transaction.commit();
      } catch (Exception failure) {
        Collections.reverse(attempted);
        for (String id : attempted)
          try {
            backends.get(id).apply(old.get(id));
          } catch (Exception rollback) {
            failure.addSuppressed(rollback);
          }
        for (Setting s : settings) if (attempted.contains(s.backend)) previewed.remove(s.id);
        throw failure;
      }
    }
    settings.forEach(Setting::accept);
    previewed.clear();
  }
}
