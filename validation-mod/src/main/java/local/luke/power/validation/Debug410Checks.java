package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.nio.file.*;
import local.luke.power.debug.*;
import local.luke.power.input.DebugInput;
import net.minecraft.client.Minecraft;

final class Debug410Checks {
  static void run(Minecraft mc,String mode) throws Exception {
    check(mc.world!=null&&mc.player!=null,"disposable world required");
    if(mode.equals("state")) {
      log("DEBUG410 state overlay="+mc.options.debugHud+" modifier="+DebugInput.held()+" hitboxes="+DebugActions.hitboxes+" borders="+DebugActions.borders+" screen="+(mc.currentScreen==null?"none":mc.currentScreen.getClass().getSimpleName()));return;
    }
    if(mode.equals("setup")) {mc.setScreen(null);DebugConfig.preview(true);DebugInput.clear();DebugActions.clear();mc.options.debugHud=false;mc.options.thirdPerson=true;return;}
    failures=0;
    var before=mc.currentScreen;
    try {
      test("DEBUG410 all editable bindings registered",()->{
        for(var key:DebugKeys.all())check(java.util.Arrays.asList(mc.options.allKeys).contains(key),"missing "+key.translationKey);
      });
      test("DEBUG410 runtime actions and bounded diagnostics",()->{
        for(var action:new DebugAction[]{DebugAction.HITBOXES,DebugAction.BORDERS,DebugAction.TOOLTIPS,DebugAction.FRAMES,DebugAction.PROFILER,DebugAction.NETWORK,DebugAction.LIGHT,DebugAction.VERSION,DebugAction.HELP})DebugActions.run(mc,action);
        DebugActions.run(mc,DebugAction.PROFILE);DebugActions.run(mc,DebugAction.PROFILE);
        try(var files=Files.list(Path.of("debug/profiling"))) {check(files.anyMatch(p->p.toString().endsWith(".jfr")),"no performance recording");}
      });
      test("DEBUG410 controls UI contains debug group",()->{
        local.luke.power.ui.PowerOptionsScreen.openDebug(mc);
        var screen=(local.luke.power.ui.PowerOptionsScreen)mc.currentScreen;
        SettingsSnapshot.render(mc,screen,640,360,Path.of("power-beta-data/reports/debug-410/controls.png"));
        check(screen.session().settings().stream().filter(s->s.group.equals("Debug")).count()>=DebugAction.values().length,"missing controls");
      });
    }finally {mc.setScreen(before);DebugInput.clear();DebugActions.clear();}
    check(failures==0,"debug checks failed="+failures);
  }
}
