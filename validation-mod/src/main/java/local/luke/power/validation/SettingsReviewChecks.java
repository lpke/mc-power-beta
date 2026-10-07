package local.luke.power.validation;

import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.Validation.*;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;

final class SettingsReviewChecks {
  static void run(Minecraft mc) throws Exception {
    failures = 0;
    var screen = new PowerOptionsScreen(mc.currentScreen);
    screen.init(mc, 640, 420);
    ConfigSession session = screen.session();
    var current = new HashMap<String, Setting>();
    session.settings().forEach(s -> current.put(s.id, s));
    Set<String> removed = Set.of("power_environment:config.forceDisplayActive",
        "power_controls:userinterface.improvedControlsMenu",
        "power_client_fixes:config.USE_ALTERNATE_RESOURCES_DOWNLOAD_URL",
        "power_client_fixes:config.RESOURCES_DOWNLOAD_URL",
        "power_client_fixes:config.ALTERNATE_RESOURCES_DOWNLOAD_URL",
        "power_client_fixes:config.enableSkinChanges", "power_client_fixes:config.renderCape",
        "power_client_fixes:config.raiseSlimSkinShoulders");
    var baseline = JsonParser.parseString(Files.readString(Path.of(
        "power-beta-data/reports/settings-review/before/catalog.json"))).getAsJsonArray();
    test("all baseline IDs remain except the eight documented removals", () -> {
      Set<String> expected = new HashSet<>();
      for (var entry : baseline) expected.add(entry.getAsJsonObject().get("id").getAsString());
      expected.removeAll(removed);
      check(expected.equals(current.keySet()), "Added or lost setting IDs");
    });
    test("every retained default, type, range, step and choice is unchanged", () -> {
      for (var entry : baseline) {
        JsonObject old = entry.getAsJsonObject();
        Setting setting = current.get(old.get("id").getAsString());
        if (setting == null) continue;
        JsonObject now = Catalog.JSON.toJsonTree(setting).getAsJsonObject();
        for (String key : List.of("backend", "kind", "defaultValue", "min", "max", "step", "choices"))
          check(old.get(key).equals(now.get(key)), setting.id + " changed " + key);
      }
    });
    test("every setting has an assigned group and useful help", () -> {
      for (Setting setting : session.settings()) {
        check(SettingsLayout.groups(setting.page).contains(setting.group), "Unassigned group: " + setting.id);
        check(!setting.description.isBlank(), "Missing help: " + setting.id);
      }
    });
    boolean showDisabled = (boolean)field(screen, "showDisabled");
    try {
      field(screen, "showDisabled", true);
      ((Set<?>)field(screen, "collapsed")).clear();
      ((TextInput)field(screen, "search")).setText("");
      field(screen, "changedOnly", false); field(screen, "relatedIds", List.of());
      field(screen, "conflictIds", List.of()); field(screen, "libraryOpen", false);
      test("all ordinary settings appear once across the sixteen tabs", () -> {
        Set<String> shown = new HashSet<>();
        for (String page : PowerOptionsScreen.PAGES) {
          field(screen, "page", page); call(screen, "layout");
          for (Object row : (List<?>)field(screen, "rows")) {
            Setting setting = (Setting)call(row, "setting");
            if (setting != null) check(shown.add(setting.id), "Duplicate row: " + setting.id);
          }
        }
        Set<String> expected = new HashSet<>();
        for (Setting setting : session.settings())
          if (!setting.group.equals("Music data") && !setting.group.equals("Track rotation")
              && !setting.id.startsWith("audio.sound.music:")) expected.add(setting.id);
        check(shown.equals(expected), "A setting disappeared from its tab");
      });
      test("binding groups collapse without changing bindings or modifier drafts", () -> {
        field(screen, "page", "Controls"); field(screen, "scroll", 0d); call(screen, "layout");
        var before = Catalog.JSON.toJson(session.settings());
        click(screen, (int)call(screen, "left") + 8, (int)call(screen, "top") + 8);
        check(((Set<?>)field(screen, "collapsed")).contains("Controls/Movement"), "Group did not collapse");
        check(((List<?>)field(screen, "rows")).stream().noneMatch(row -> {
          try { Setting s = (Setting)call(row, "setting"); return s != null && s.group.equals("Movement"); }
          catch (Exception e) { throw new RuntimeException(e); }
        }), "Collapsed bindings remain visible");
        check(before.equals(Catalog.JSON.toJson(session.settings())), "Collapse edited a binding");
        click(screen, (int)call(screen, "left") + 8, (int)call(screen, "top") + 8);
        check(!((Set<?>)field(screen, "collapsed")).contains("Controls/Movement"), "Group did not reopen");
      });
    } finally {
      field(screen, "showDisabled", showDisabled);
    }
    test("shared sprint preferences stay editable without cheats and retain related bindings", () -> {
      for (String id : List.of("creative.sprintToggle", "creative.sprintMultiplier")) {
        Setting s = current.get(id);
        check(SettingAccess.reason(session, s).isEmpty(), "Shared sprint is cheats-gated");
        check(ControlLinks.controls(session, s).stream().anyMatch(k -> k.id.equals("keys.power_creative.sprint")), "Missing sprint link");
      }
    });
    test("startup-only hooks require restart", () -> {
      for (String id : List.of("power_controls:bugfixes.bitDepthFix", "power_compat:config.slabPlacementFixesEnabled",
          "power_controls:userinterface.showQuitButton", "power_controls:general.disableControllerInit",
          "power_controls:general.resourceDownloadUrl"))
        check(current.get(id).restart, "Startup setting marked live: " + id);
    });
    log("SETTINGS REVIEW FAILURES " + failures);
  }
}
