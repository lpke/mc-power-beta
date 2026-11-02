package local.luke.power.ui;

import com.google.gson.JsonElement;
import java.util.*;
import local.luke.power.audio.MusicFolders;
import local.luke.power.audio.MusicLibrarySources;
import local.luke.power.audio.AudioConfig;
import local.luke.power.audio.AudioSettings;
import net.fabricmc.loader.api.FabricLoader;
import local.luke.power.config.Setting;
import org.lwjgl.input.Keyboard;

/** Source actions use the surrounding Options transaction, without another folder draft. */
final class MusicFolderActions extends UiScreen {
  private final PowerOptionsScreen parent;
  private final MusicEdits edits;
  private final Runnable changed;
  private static final Set<String> warnedMissing = new HashSet<>();
  private String removing = "", error = "";
  private boolean missing;
  private JsonElement removalValue;

  MusicFolderActions(PowerOptionsScreen parent, Runnable changed) {
    this.parent = parent;
    this.edits = new MusicEdits(parent);
    this.changed = changed;
  }

  private Setting setting() { return edits.setting("musicDirectories"); }
  String error() { return error; }
  boolean modal() { return !removing.isEmpty(); }
  void cancel() { removing = ""; removalValue = null; error = ""; missing = false; }
  void reload() { warnedMissing.clear(); local.luke.power.audio.AudioController.reload(); }
  void noticeMissing(Collection<MusicLibrarySources.Folder> sources) {
    if (modal()) return;
    for (var source : sources) if (source.status().equals("Unavailable") && warnedMissing.add(source.path())) {
      removing = source.path(); missing = true; removalValue = setting().value.deepCopy(); return;
    }
  }

  void add() {
    error = "";
    JsonElement expected = setting().value.deepCopy();
    var sources = MusicFolders.decode(expected);
    minecraft.setScreen(new DirectoryScreen(parent, additions -> {
      if (!expected.equals(setting().value))
        throw new IllegalArgumentException("Folders changed. Reopen Add folder.");
      List<String> paths = new ArrayList<>(sources);
      paths.addAll(additions);
      if (paths.size() > MusicFolders.MAX_FOLDERS || paths.size() != new HashSet<>(paths).size())
        throw new IllegalArgumentException("Use at most 32 unique folders. Nothing was added.");
      LinkedHashSet<String> order = new LinkedHashSet<>();
      additions.forEach(path -> order.add(MusicLibrarySources.id(path)));
      for (var id : edits.setting("libraryOrder").value.getAsJsonArray()) order.add(id.getAsString());
      edits.apply(Map.of("musicDirectories", MusicFolders.encode(paths),
          "libraryOrder", local.luke.power.config.Catalog.JSON.toJsonTree(order)));
      changed.run();
    }, sources));
  }

  void remove(String path) {
    if (!MusicFolders.decode(setting().value).contains(path)) {
      error = "Folder changed. Select it again.";
      return;
    }
    removing = path; missing = false;
    removalValue = setting().value.deepCopy();
    error = "";
  }

  void relink(String path) {
    error = "";
    Map<String, JsonElement> expected = new LinkedHashMap<>();
    for (String id : List.of("musicDirectories", "musicSourceIdentities", "libraryOrder", "libraryGroupVolumes"))
      expected.put(id, edits.setting(id).value.deepCopy());
    AudioSettings current = AudioConfig.copy();
    current.musicDirectories = MusicFolders.decode(expected.get("musicDirectories"));
    current.musicSourceIdentities = new LinkedHashMap<>();
    expected.get("musicSourceIdentities").getAsJsonObject().entrySet().forEach(entry -> current.musicSourceIdentities.put(entry.getKey(), entry.getValue().getAsString()));
    current.libraryOrder = new ArrayList<>();
    expected.get("libraryOrder").getAsJsonArray().forEach(id -> current.libraryOrder.add(id.getAsString()));
    current.libraryGroupVolumes = new LinkedHashMap<>();
    expected.get("libraryGroupVolumes").getAsJsonObject().entrySet().forEach(entry -> current.libraryGroupVolumes.put(entry.getKey(), entry.getValue().getAsInt()));
    cancel();
    minecraft.setScreen(new DirectoryScreen(parent, additions -> {
      if (additions.size() != 1) throw new IllegalArgumentException("Choose one replacement folder.");
      for (var entry : expected.entrySet()) if (!entry.getValue().equals(edits.setting(entry.getKey()).value))
        throw new IllegalArgumentException("Folders changed. Reopen Relink source path.");
      AudioSettings next;
      try { next = MusicFolders.relink(FabricLoader.getInstance().getGameDir(), current, path, additions.get(0)); }
      catch (java.io.IOException failure) { throw new IllegalArgumentException(failure.getMessage(), failure); }
      edits.apply(Map.of("musicDirectories", MusicFolders.encode(next.musicDirectories),
          "musicSourceIdentities", local.luke.power.config.Catalog.JSON.toJsonTree(next.musicSourceIdentities),
          "libraryOrder", local.luke.power.config.Catalog.JSON.toJsonTree(next.libraryOrder),
          "libraryGroupVolumes", local.luke.power.config.Catalog.JSON.toJsonTree(next.libraryGroupVolumes)));
      changed.run();
    }, current.musicDirectories));
  }

  void key(int key) { if (key == Keyboard.KEY_ESCAPE) cancel(); }

  private int dialogWidth() { return Math.min(312, width - 20); }
  private int dialogLeft() { return (width - dialogWidth()) / 2; }
  private int buttonWidth() { return (dialogWidth() - 4) / 2; }
  private int buttonY() { return height / 2 + 13; }

  void dialogClick(int x, int y, int button) {
    if (!modal() || button != 0) return;
    if (inside(x, y, dialogLeft() + buttonWidth() + 4, buttonY(), buttonWidth(), 20)) {
      click(); cancel(); return;
    }
    if (!inside(x, y, dialogLeft(), buttonY(), buttonWidth(), 20)) return;
    click();
    if (missing) { relink(removing); return; }
    try {
      if (!removalValue.equals(setting().value))
        throw new IllegalArgumentException("Folders changed. Cancel and select Remove again.");
      var sources = MusicFolders.decode(setting().value);
      List<String> paths = new ArrayList<>(sources);
      paths.remove(removing);
      var identities = edits.setting("musicSourceIdentities").value.deepCopy().getAsJsonObject();
      identities.remove(removing);
      edits.apply(Map.of("musicDirectories", MusicFolders.encode(paths), "musicSourceIdentities", identities));
      changed.run();
      cancel();
    } catch (RuntimeException failure) { error = Objects.toString(failure.getMessage(), "Could not remove this folder."); }
  }

  void renderDialog(int mx, int my) {
    if (!modal()) return;
    fill(0, 0, width, height, 0xc0000000);
    int x = dialogLeft(), w = dialogWidth();
    int span = Math.max(1, Math.min(w, width - 24));
    for (var line : missing
        ? MusicFolderWarning.layout(span, buttonY() - 12, "Music source unavailable", removing,
            "Cached tracks and playlist entries stay unchanged. Relink the source path if the folder moved, or remove it from Library.", error, this::textWidth)
        : MusicFolderWarning.layout(span, buttonY() - 12, removing, error, this::textWidth))
      centeredText(line.text(), line.y(), line.color());
    button(missing ? "Relink source path" : "Remove", x, buttonY(), buttonWidth(), 20, mx, my, true);
    button(missing ? "Close" : "Cancel", x + buttonWidth() + 4, buttonY(), buttonWidth(), 20, mx, my, true);
  }
}
