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
    if(mode.equals("controls420")) { controls420(mc); return; }
    if(mode.equals("minimap420")) { minimap420(mc); return; }
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
        Files.createDirectories(Path.of("power-beta-data/reports/debug-410"));
        SettingsSnapshot.render(mc,screen,640,360,Path.of("power-beta-data/reports/debug-410/controls.png"));
        check(screen.session().settings().stream().filter(s->s.group.equals("Debug")).count()>=DebugAction.values().length,"missing controls");
      });
    }finally {mc.setScreen(before);DebugInput.clear();DebugActions.clear();}
    check(failures==0,"debug checks failed="+failures);
  }
  private static void controls420(Minecraft mc) throws Exception {
    failures=0;
    var before=mc.currentScreen;
    Object tweaks=Class.forName("local.luke.power.controls.ControlFeatures").getField("TWEAKS_CONFIG").get(null);
    var expandedHitbox=tweaks.getClass().getField("expandChickenHitbox");boolean chickenSetting=(Boolean)expandedHitbox.get(tweaks);
    try {
      test("CONTROLS420 temporary controls registered and hold-use indicator enabled",()->{
        for(String id:new String[]{"Hold use (toggle)","Auto-walk line lock (toggle)"})
          check(java.util.Arrays.stream(mc.options.allKeys).anyMatch(key->key.translationKey.equals(id)),"missing "+id);
        check(new local.luke.power.status.StatusSettings().holdUse,"hold-use indicator default off");
      });
      test("CONTROLS420 chicken boxes follow expanded preference live",()->{
        var chicken=new net.minecraft.class_313(mc.world);
        chicken.method_1340(mc.player.x,mc.player.y,mc.player.z);
        for(boolean expanded:new boolean[]{true,false}) {
          expandedHitbox.set(tweaks,expanded);
          chicken.method_937();
          double width=chicken.boundingBox.maxX-chicken.boundingBox.minX;
          double height=chicken.boundingBox.maxY-chicken.boundingBox.minY;
          check(Math.abs(width-(expanded?.4:.3))<.00001,"stale chicken width "+width);
          check(Math.abs(height-(expanded?.7:.4))<.00001,"stale chicken height "+height);
        }
      });
      test("CONTROLS420 binding prompt and narrow layout capture",()->{
        local.luke.power.ui.PowerOptionsScreen.openDebug(mc);
        var screen=(local.luke.power.ui.PowerOptionsScreen)mc.currentScreen;
        var binding=screen.session().settings().stream().filter(setting->setting.id.equals("keys.Hold use (toggle)")).findFirst().orElseThrow();
        var capture=screen.getClass().getDeclaredField("capture"); capture.setAccessible(true); capture.set(screen,binding);
        Path output=Path.of("power-beta-data/reports/controls-420"); Files.createDirectories(output);
        SettingsSnapshot.render(mc,screen,480,270,output.resolve("binding-narrow.png"));
        SettingsSnapshot.render(mc,screen,960,540,output.resolve("binding-wide.png"));
        ((local.luke.power.validation.mixin.ScreenInput)(Object)screen).power$key('\0',org.lwjgl.input.Keyboard.KEY_DELETE);
        check(!screen.capturingBinding()&&binding.value.getAsInt()==0,"Delete did not clear binding");
      });
    } finally {
      expandedHitbox.set(tweaks,chickenSetting);
      mc.setScreen(before); DebugInput.clear();
    }
    check(failures==0,"controls420 checks failed="+failures);
  }
  private static void minimap420(Minecraft mc) throws Exception {
    failures=0;
    var before=mc.currentScreen;var settings=local.luke.power.map.MapConfig.copy();boolean hideHud=mc.options.hideHud;
    try {
      var draft=local.luke.power.map.MapConfig.copy();draft.minimap=draft.showInInventory=true;draft.hideWithDebug=false;draft.size=64;draft.corner=1;
      local.luke.power.map.MapConfig.preview(draft);mc.options.hideHud=false;
      var inventory=new net.minecraft.class_585(mc.player);mc.setScreen(inventory);inventory.init(mc,480,270);
      test("MINIMAP420 gear and menu retain native container and own pointer/keyboard",()->{
        var geometry=local.luke.power.map.MinimapQuickMenuLayout.at(local.luke.power.map.Minimap.layout(draft,480,270,false),draft.circular,480,270,mc.textRenderer.getWidth("Show in-game waypoints"));
        int x=geometry.gear().x(),y=geometry.gear().y();
        check(local.luke.power.map.MinimapQuickMenu.mouse(mc,x,y,0,true),"gear did not open");
        check(local.luke.power.map.MinimapQuickMenu.active(mc),"menu inactive");
        check(local.luke.power.input.InventoryPanels.modal(inventory),"menu did not claim modal ownership");
        check(local.luke.power.map.MinimapQuickMenu.mouse(mc,x,y,0,false),"gear release leaked");
        check(local.luke.power.map.MinimapQuickMenu.mouse(mc,1,1,-1,false),"wheel or pointer motion leaked");
        check(local.luke.power.map.MinimapQuickMenu.key(mc,org.lwjgl.input.Keyboard.KEY_W,true),"movement key leaked");
        check(mc.currentScreen==inventory,"menu replaced native inventory");
        Path output=Path.of("power-beta-data/reports/controls-420");Files.createDirectories(output);
        var capture=new net.minecraft.client.gui.screen.Screen(){@Override public void render(int mx,int my,float delta){inventory.render(mx,my,delta);local.luke.power.map.MinimapQuickMenu.renderMenu(mc,mx,my);}};
        SettingsSnapshot.render(mc,capture,480,270,output.resolve("minimap-quick-menu.png"),x,y);
        var image=javax.imageio.ImageIO.read(output.resolve("minimap-quick-menu.png").toFile());int scale=image.getWidth()/480;
        for(int row=0;row<4;row++) {
          var bounds=geometry.row(row);int bright=0;
          for(int py=(bounds.y()+5)*scale;py<(bounds.y()+15)*scale;py++)
            for(int px=(bounds.x()+19)*scale;px<(bounds.x()+bounds.width()-2)*scale;px++) {
              int rgb=image.getRGB(px,py);if((rgb>>16&255)>160&&(rgb>>8&255)>160&&(rgb&255)>160)bright++;
            }
          check(bright>20,"minimap menu row "+row+" text did not render: pixels="+bright);
        }
        check(local.luke.power.map.MinimapQuickMenu.key(mc,org.lwjgl.input.Keyboard.KEY_ESCAPE,true),"Escape leaked");
        check(!local.luke.power.map.MinimapQuickMenu.active(mc)&&!local.luke.power.input.InventoryPanels.modal(inventory),"Escape retained modal owner");
      });
    } finally {
      mc.setScreen(before);local.luke.power.map.MinimapQuickMenu.active(mc);
      local.luke.power.map.MapConfig.queued(settings);mc.options.hideHud=hideHud;
    }
    check(failures==0,"minimap420 checks failed="+failures);
  }
}
