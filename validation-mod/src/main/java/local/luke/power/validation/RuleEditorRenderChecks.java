package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.file.*;
import java.util.Arrays;
import local.luke.power.rules.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.PowerOptionsScreen;
import net.minecraft.client.Minecraft;

/** Standalone editors have no music panel. Their actual render path must stay usable. */
final class RuleEditorRenderChecks {
  static void run(Minecraft mc) throws Exception {
    failures = 0; check(mc.world == null, "return to the title before standalone rule checks");
    var previous = mc.currentScreen; byte[] preferences = Files.readAllBytes(PowerConfig.path());
    try {
      test("Custom creation and saved-world editors render with no music panel at narrow and wide sizes", () -> {
        var draft = new CreationRules(RulePresets.survival(), RulePresets.cheats()); draft.cycle(-1);
        PowerOptionsScreen.openCreationRules(mc, previous, draft); render(mc, "custom");
        check(draft.custom().equals(RulePresets.survival()), "rendering changed future-world rules");
        try(var folders = Files.list(Path.of("saves"))) {
          var saved = folders.filter(p -> Files.isRegularFile(p.resolve("level.dat"))).findFirst().orElseThrow();
          byte[] metadata = Files.readAllBytes(saved.resolve("level.dat"));
          PowerOptionsScreen.openSavedWorldRules(mc, previous, saved.getFileName().toString()); render(mc, "saved");
          check(Arrays.equals(metadata, Files.readAllBytes(saved.resolve("level.dat"))), "rendering changed saved metadata");
        }
      });
      check(Arrays.equals(preferences, Files.readAllBytes(PowerConfig.path())), "standalone render checks wrote global settings");
    } finally { mc.setScreen(previous); }
    log("RULE EDITOR RENDER failures=" + failures);
  }
  private static void render(Minecraft mc, String name) throws Exception {
    var editor = (PowerOptionsScreen)mc.currentScreen;
    for(int[] size : new int[][]{{320,240},{640,360},{960,540}}) {
      editor.init(mc,size[0],size[1]);
      SettingsSnapshot.render(mc,editor,size[0],size[1],Path.of("power-beta-data/reports/rule-editor-"+name+"-"+size[0]+".png"));
      check(UiChecks.field(editor,"library")==null,"standalone editor unexpectedly owns a music panel");
    }
  }
}
