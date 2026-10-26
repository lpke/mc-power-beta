package local.luke.power.ui;

import java.util.*;
import local.luke.power.audio.MusicPlaylist;
import local.luke.power.config.DefaultBases;
import local.luke.power.config.Setting;

/** One selection list for the toolbar and both playlist settings. */
final class MusicPlaylistPicker {
  static final Object TOOLBAR = new Object();
  private MusicPlaylistPicker() {}
  static void open(PowerOptionsScreen parent, SettingsDropdown popup, Object owner, MusicEdits edits,
      Setting setting, boolean menu, int x, int y, int width, int rowHeight) {
    var playlists = edits.presets();
    List<String> ids = new ArrayList<>(), labels = new ArrayList<>();
    ids.add(""); labels.add(playlists.stream().noneMatch(p -> !MusicPlaylist.reserved(p.id())) ? "Add playlist" : "Edit playlists");
    if (menu) { ids.add(MusicPlaylist.SAME_AS_WORLD); labels.add("Same as world"); }
    for (var playlist : playlists) { ids.add(playlist.id()); labels.add(playlist.name()); }
    String selected = setting.value.getAsString();
    String reset = DefaultBases.value(setting.id, setting.powerBetaDefault).getAsString();
    popup.open(owner, labels, List.of(), ids.indexOf(selected), ids.indexOf(reset), x, y, width, rowHeight,
        parent.width, parent.height, index -> {
          if (index == 0) { parent.editMusic(); return; }
          if (menu) edits.loadMenu(ids.get(index));
          else if (!ids.get(index).equals(setting.value.getAsString()))
            parent.changeWorldPlaylist(() -> edits.load(ids.get(index)));
        });
    popup.separatorsBefore(1);
  }
}
