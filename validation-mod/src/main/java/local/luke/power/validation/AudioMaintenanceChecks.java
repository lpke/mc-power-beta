package local.luke.power.validation;

import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.Validation.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;

final class AudioMaintenanceChecks {
  static void run(Minecraft mc) throws Exception {
    failures = 0;
    try (var preserved = new MusicPlaylists460Checks.State(mc)) {
      MenuPreferences.update(v -> v.autoApply = false);
      AudioSettings config = new AudioSettings();
      config.playlists.add(new MusicPlaylist("one", "One", List.of("music:calm1.ogg"), Map.of()));
      MusicPlaylist.load(config, "one");
      AudioConfig.preview(config);
      PowerOptionsScreen options = MenuUpdateChecks.open(mc, "Audio");
      test("saved playlist membership survives global favourites edits and rollback", () -> {
        var draft = new MusicPresetDraft(config.playlists.get(0), List.of("music:calm1.ogg", "music:calm2.ogg"));
        draft.include("music:calm2.ogg", true);
        check(MusicFixtureSupport.tracks(AudioConfig.current()).equals(List.of("music:calm1.ogg")), "unsaved membership leaked");
        Setting favourite = find(options.session(), "audio.favourites");
        favourite.value = Catalog.JSON.toJsonTree(Set.of("music:calm1.ogg")); options.changed(favourite);
        check(draft.tracks().contains("music:calm2.ogg"), "global edit replaced isolated draft");
        check(MusicFixtureSupport.tracks(AudioConfig.current()).equals(List.of("music:calm1.ogg")), "global edit expanded saved playlist");
        options.session().discard();
      });
      MusicFolderChecks.run(mc);
      test("library sections start collapsed and pause scrub option defaults off", () -> {
        check(!new AudioSettings().menuControlsScrub,"scrubber enabled by default");
        mc.setScreen(options);field(options,"libraryOpen",false);call(options,"layout");
        var library = MusicPlaylists460Checks.library(options);
        MusicPlaylists460Checks.tab(options,library,"library");
        check(library.sectionsCollapsed(),"library starts expanded");
      });
      test("ffmpeg was discovered without a conversion process", () -> check(Mp3Converter.available(),"ffmpeg probe failed"));
    }
    log("AUDIO MAINTENANCE FAILURES " + failures);
  }
}
