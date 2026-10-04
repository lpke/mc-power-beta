package local.luke.power.validation;

import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.Validation.*;

import com.google.gson.JsonPrimitive;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.commands.CommandContext;
import local.luke.power.config.*;
import local.luke.power.mixin.SoundManagerAccessor;
import local.luke.power.permissions.CommandPermissions;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;

public final class CommandsLibraryChecks {
  private static PowerOptionsScreen options;
  private static CommandPermissions.Settings permissions;
  private static String preview;

  private static void click(PowerOptionsScreen screen, int x, int y, int button) {
    ((ScreenInput) (Object) screen).power$click(x, y, button);
  }

  public static void run(Minecraft mc, String action) throws Exception {
    switch (action) {
      case "commands" -> {
        failures = 0;
        permissions = CommandPermissions.copy();
        var defaults = new CommandPermissions.Settings();
        CommandPermissions.preview(defaults);
        ChatChecks.submit(mc, "/gamemode creative");
        var warps = Class.forName("local.luke.power.commands.api.PlayerWarps");
        String before = (String) warps.getMethod("spc$getWarpString").invoke(mc.player);
        try {
          ChatChecks.submit(mc, "/warp set command-policy-test");
          String saved = (String) warps.getMethod("spc$getWarpString").invoke(mc.player);
          check(!saved.equals(before), "warp fixture missing");
          ChatChecks.submit(mc, "/gamemode survival");
          test(
              "creative rule blocks command through the real chat path",
              () -> {
                var lines = ChatChecks.submit(mc, "/warp set denied-test");
                check(
                    lines.size() == 1 && lines.get(0).contains("requires creative"),
                    lines.toString());
                check(
                    warps.getMethod("spc$getWarpString").invoke(mc.player).equals(saved),
                    "blocked command changed warps");
              });
          var world = CommandContext.world(mc);
          defaults.worlds.put(
              world, new LinkedHashMap<>(Map.of("warp", CommandPermissions.Override.ALLOW)));
          CommandPermissions.preview(defaults);
          test(
              "world allow bypasses creative rule",
              () -> {
                var lines = ChatChecks.submit(mc, "/warp list");
                check(
                    lines.stream().noneMatch(s -> s.contains("requires creative")),
                    lines.toString());
                check(CommandPermissions.allowed("warp"), "override ignored");
              });
          defaults.enabled = false;
          CommandPermissions.preview(defaults);
          test(
              "master blocks world allow and aliases without deleting data",
              () -> {
                check(
                    !CommandPermissions.allowed("warp") && !CommandPermissions.allowed("gm"),
                    "master bypassed");
                var lines = ChatChecks.submit(mc, "/gm creative");
                check(lines.size() == 1 && lines.get(0).contains("disabled"), lines.toString());
                check(!CommandContext.current(mc).creative(), "denied mode changed player");
                check(
                    warps.getMethod("spc$getWarpString").invoke(mc.player).equals(saved),
                    "master erased warp");
              });
          test(
              "permission UI rejects a world change before saving",
              () -> {
                ConfigSession session = SettingsRegistry.open(mc);
                Setting s = find(session, "commands.world.warp");
                s.value = new JsonPrimitive(2);
                var worldBefore = mc.world;
                try {
                  mc.world = null;
                  try {
                    session.save(java.nio.file.Path.of(".").toAbsolutePath());
                    throw new AssertionError("saved for wrong world");
                  } catch (IllegalArgumentException expected) {
                    check(expected.getMessage().contains("world changed"), expected.toString());
                  }
                } finally {
                  mc.world = worldBefore;
                }
              });
        } finally {
          warps.getMethod("spc$setWarpString", String.class).invoke(mc.player, before);
          CommandPermissions.preview(permissions);
          ChatChecks.submit(mc, "/gamemode creative");
        }
        options = MenuUpdateChecks.open(mc, "Commands");
        test(
            "command page includes a separate override for each command",
            () -> {
              check(
                  options.session().settings().stream()
                          .filter(s -> s.id.startsWith("commands.world."))
                          .count()
                      == CommandPermissions.COMMANDS.size(),
                  "missing world rule");
              options.render(-1, -1, 0);
            });
        log("COMMAND ACCESS FAILURES " + failures);
      }
      case "persist-start" -> {
        var original = CommandPermissions.copy();
        var json = new com.google.gson.JsonObject();
        json.addProperty("world", CommandContext.world(mc));
        json.add("settings", new com.google.gson.Gson().toJsonTree(original));
        java.nio.file.Files.writeString(
            java.nio.file.Path.of("permissions-validation.json"), json.toString());
        original
            .worlds
            .computeIfAbsent(CommandContext.world(mc), k -> new LinkedHashMap<>())
            .put("warp", CommandPermissions.Override.BLOCK);
        CommandPermissions.save(original);
        mc.world.method_195(true, null);
      }
      case "persist-finish" -> {
        var json =
            com.google.gson.JsonParser.parseString(
                    java.nio.file.Files.readString(
                        java.nio.file.Path.of("permissions-validation.json")))
                .getAsJsonObject();
        test(
            "world identity and its command override persist across restart",
            () -> {
              check(
                  json.get("world").getAsString().equals(CommandContext.world(mc)),
                  "world identity changed");
              check(
                  CommandPermissions.current().override(CommandContext.world(mc), "warp")
                      == CommandPermissions.Override.BLOCK,
                  "world rule lost");
              check(!CommandPermissions.allowed("warp"), "persisted rule not enforced");
            });
        CommandPermissions.save(
            new com.google.gson.Gson()
                .fromJson(json.get("settings"), CommandPermissions.Settings.class));
        java.nio.file.Files.delete(java.nio.file.Path.of("permissions-validation.json"));
      }
      case "inventory" -> {
        var screen = mc.currentScreen;
        test(
            "creative tab and survival view persist without altering inventory",
            () -> {
              var items = mc.player.inventory.main.clone();
              var armour = mc.player.inventory.armor.clone();
              var cursor = mc.player.inventory.getCursorStack();
              int x = (screen.width - 176) / 2, y = (screen.height - 166) / 2;
              ((ScreenInput) (Object) screen).power$click(x + 4 + 24 + 8, y - 12, 0);
              int selected = (int) field(screen, "creative_tabIndex");
              check(selected == 1, "fixture failed to select second tab");
              ((ScreenInput) (Object) screen).power$click(x + 180, y + 146, 0);
              check((boolean) field(screen, "creative_normalGUI"), "survival view not selected");
              check(
                  Arrays.equals(items, mc.player.inventory.main)
                      && Arrays.equals(armour, mc.player.inventory.armor)
                      && cursor == mc.player.inventory.getCursorStack(),
                  "tab switch moved inventory contents");
              mc.setScreen(null);
              var ctor = screen.getClass().getConstructors()[0];
              var reopened = (net.minecraft.client.gui.screen.Screen) ctor.newInstance(mc.player);
              mc.setScreen(reopened);
              check((boolean) field(reopened, "creative_normalGUI"), "survival view lost");
              ((ScreenInput) (Object) reopened).power$click(x + 180, y + 122, 0);
              check((int) field(reopened, "creative_tabIndex") == selected, "creative tab lost");
              mc.setScreen(null);
            });
      }
      case "ui" -> {
        failures = 0;
        MenuPreferences.update(v -> v.autoApply = false);
        options = MenuUpdateChecks.open(mc, "Audio");
        field(options, "libraryOpen", false);
        test(
            "active sidebar tab scrolls to the top",
            () -> {
              field(options, "scroll", 100d);
              call(options, "layout");
              click(
                  options,
                  (int) call(options, "origin") + 30,
                  24
                      + PowerOptionsScreen.PAGES.indexOf("Audio") * 22
                      + 10
                      - (int) (double) field(options, "sideScroll"),
                  0);
              check((double) field(options, "scroll") == 0, "scroll not reset");
            });
        test(
            "content scrollbar track click moves the viewport",
            () -> {
              var t = (ScrollBar.Track) call(options, "contentTrack");
              click(options, t.x(), t.top() + t.height() - 2, 0);
              check((double) field(options, "scroll") > 0, "scrollbar ignores click");
            });
        test(
            "music library opens inside the same Options screen",
            () -> {
              musicClick(options,AudioToolbar.Action.LIBRARY);
              check(
                  mc.currentScreen == options && (boolean) field(options, "libraryOpen"),
                  "opened separate screen");
              options.render(-1, -1, 0);
            });
        MusicLibraryScreen library = (MusicLibraryScreen) field(options, "library");
        test(
            "folder filter supports all custom and specific folders in both directions",
            () -> {
              var panel = (MusicLibraryScreen) field(options, "library");
              panel.restore(new MusicLibraryScreen.State(false, "", "", 0, 0));
              call(panel, "rebuild");
              var cycle = MusicLibraryScreen.class.getDeclaredMethod("cycleFolder", int.class);
              cycle.setAccessible(true);
              cycle.invoke(panel, 1);
              check(panel.state().folder().equals("custom"), "missing custom filter");
              var folders = (List<?>) field(panel, "folders");
              if (!folders.isEmpty()) {
                cycle.invoke(panel, 1);
                String folder = panel.state().folder();
                check(folders.contains(folder), "missing individual folder");
                for (String id : (List<String>) field(panel, "tracks"))
                  check(
                      AudioController.customTracks().stream()
                          .anyMatch(
                              t ->
                                  t.id().equals(id)
                                      && t.path().startsWith(java.nio.file.Path.of(folder))),
                      "filter contains another folder");
                cycle.invoke(panel, -1);
                check(panel.state().folder().equals("custom"), "reverse filter failed");
              }
            });
        library.restore(new MusicLibraryScreen.State(false, "custom", "test", 20, 0));
        call(library, "rebuild");
        var state = library.state();
        mc.setScreen(null);
        options = new PowerOptionsScreen(null);
        mc.setScreen(options);
        test(
            "embedded library filter query and scroll survive reopen",
            () -> {
              check((boolean) field(options, "libraryOpen"), "library not remembered");
              check(
                  ((MusicLibraryScreen) field(options, "library")).state().equals(state),
                  "library view lost");
              options.render(-1, -1, 0);
            });
        library = (MusicLibraryScreen) field(options, "library");
        library.restore(new MusicLibraryScreen.State(false, "", "", 0, 0));
        call(library, "rebuild");
        test(
            "hex swatch rows render at compact and wide scales",
            () -> {
              field(options, "libraryOpen", false);
              search(options, "light overlay color");
              options.render(-1, -1, 0);
              check(
                  options.session().settings().stream().anyMatch(ColourScreen::accepts),
                  "no colour options");
            });
        search(options, "");
        field(options, "page", "Audio");
        field(options, "libraryOpen", true);
        call(options, "layout");
        test("search section navigation opens audio settings rather than a remembered library", () -> {
          field(options,"page","Video"); field(options,"libraryOpen",true); search(options,"master volume");
          Setting master = find(options.session(),"audio.master");
          var open = PowerOptionsScreen.class.getDeclaredMethod("openGroup", Setting.class); open.setAccessible(true); open.invoke(options,master);
          check(field(options,"page").equals("Audio") && !(boolean)field(options,"libraryOpen"), "library hid target settings");
          options.render(-1,-1,0);
        });
        field(options,"libraryOpen",true); search(options,"");
        log("COMMANDS LIBRARY UI FAILURES " + failures);
      }
      case "rapid-pause" -> {
        AudioController.next(); AudioController.pause();
        test("Pause state wins immediately after Next", () -> check(!AudioController.musicPlaying(), "Pause still offers Pause instead of Play"));
      }
      case "rapid-paused" -> {
        test("late asynchronous music starts remain paused", () -> check(!SoundManagerAccessor.power$system().playing("BgMusic"), "late start escaped Pause"));
        AudioController.togglePause();
      }
      case "rapid-resumed" -> test("Play resumes after rapid Next Pause", () -> check(SoundManagerAccessor.power$system().playing("BgMusic") && AudioController.musicPlaying(), "resume failed"));
      case "sound" -> {
        AudioController.previewSound("mob.cow");
        test(
            "effect preview becomes active immediately",
            () -> check(AudioController.previewing("mob.cow"), "preview state absent"));
      }
      case "sound-stop" -> {
        AudioController.previewSound("random.explode");
        check(AudioController.previewing("random.explode"), "effect did not start");
        AudioController.previewSound("random.explode");
        test(
            "second effect preview click clears active state",
            () -> check(!AudioController.previewing("random.explode"), "still marked active"));
      }
      case "sound-stopped" ->
          test(
              "second click stops the sound source",
              () ->
                  check(
                      !SoundManagerAccessor.power$system().playing("PowerBetaPreview"),
                      "effect still audible"));
      case "music" -> {
        preview =
            AudioController.music(mc).stream()
                .filter(id -> !id.startsWith("music:custom/"))
                .findFirst()
                .orElseThrow();
        AudioController.previewSound(preview);
        test(
            "music preview lights its own speaker",
            () ->
                check(
                    AudioController.previewing(preview) && AudioController.musicPlaying(),
                    "music state missing"));
      }
      case "music-stop" -> {
        test(
            "preview streams while embedded menu stays open",
            () ->
                check(
                    SoundManagerAccessor.power$system().playing("PowerBetaMusicPreview")
                        && mc.currentScreen instanceof PowerOptionsScreen,
                    "stream not playing"));
        AudioController.previewSound(preview);
        test(
            "second preview click clears speaker state",
            () -> check(!AudioController.previewing(preview), "speaker still active"));
      }
      case "queue" -> {
        AudioController.pause();
        String first = AudioController.music(mc).iterator().next();
        String second =
            AudioController.music(mc).stream()
                .filter(id -> !id.equals(first))
                .findFirst()
                .orElseThrow();
        MusicRequests.edit(
            q -> {
              q.tracks.clear();
              q.add(first);
              q.add(second);
              q.add(first);
            });
        options = MenuUpdateChecks.open(mc, "Audio");
        field(options, "libraryOpen", true);
        var panel = (MusicLibraryScreen) field(options, "library");
        panel.restore(new MusicLibraryScreen.State(true, "", "", 0, 0));
        call(panel, "rebuild");
        options.render(-1, -1, 0);
        int right = (int) call(panel, "right");
        test(
            "queue row arrow reorders exactly one request",
            () -> {
              click(options, right - 125, 120, 0);
              check(
                  MusicRequests.tracks().equals(List.of(second, first, first)), "wrong queue edit");
            });
        options.render(-1, -1, 0);
        MusicRequests.edit(q -> q.tracks.remove(0));
        var before = MusicRequests.tracks();
        test(
            "stale queue click never removes a different request",
            () -> {
              click(options, right - 25, 120, 0);
              check(MusicRequests.tracks().equals(before), "stale click removed wrong row");
            });
        MusicRequests.edit(q -> q.tracks.clear());
        log("QUEUE UI FAILURES " + failures);
      }
    }
  }
}
