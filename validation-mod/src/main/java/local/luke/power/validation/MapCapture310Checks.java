package local.luke.power.validation;
import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import local.luke.power.config.*;
import local.luke.power.map.*;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
final class MapCapture310Checks {
  private static WorldMapScreen map;
  private static PowerOptionsScreen panel;
  private static Setting key;
  private static Screen previous;
  private static MapSettings settings;
  private static boolean auto, fullscreen;
  static void run(Minecraft mc,String action)throws Exception {
    if(action.equals("capture-arm")) {
      check(mc.world!=null && map==null,"open disposable world; finish any previous capture");
      previous=mc.currentScreen;settings=MapConfig.copy();auto=MenuPreferences.current().autoApply;fullscreen=Display.isFullscreen();
      MenuPreferences.current().autoApply=false;var enabled=MapConfig.copy();enabled.worldMap=true;MapConfig.preview(enabled);Maps.tick(mc);
      map=new WorldMapScreen();mc.setScreen(map);map.init(mc,960,540);call(map,"toggleSettings");panel=(PowerOptionsScreen)field(map,"settingsPanel");
      key=panel.session().settings().stream().filter(s->s.kind==Setting.Kind.KEY&&s.group.equals("Maps")).findFirst().orElseThrow();
      field(panel,"capture",key);check(map.capturingBinding(),"host did not expose embedded capture");
      log("310 CAPTURE ARMED; send physical F11, then release-310 capture-check");return;
    }
    check(map!=null,"arm capture first");
    try {
      check(!Display.isFullscreen() == !fullscreen,"F11 toggled fullscreen while binding in sidecar");
      check(!map.capturingBinding()&&key.value.getAsInt()==Keyboard.KEY_F11,"F11 was not captured as the binding");
      check(mc.currentScreen==map,"capture replaced map host");log("PASS 310 physical F11 captured without fullscreen toggle");
    } finally {
      panel.session().discard();MenuPreferences.current().autoApply=auto;MapConfig.preview(settings);mc.setScreen(previous);map=null;
    }
  }
}
