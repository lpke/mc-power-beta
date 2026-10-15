package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.util.*;
import local.luke.power.input.*;
import local.luke.power.render.RenderControls;
import local.luke.power.visual.*;
import net.minecraft.client.Minecraft;

final class Render150Checks {
  static void run(Minecraft mc,String action) throws Exception {
    var world=AccessRules.of(mc.world.method_262());
    switch(action) {
      case "setup", "chord" -> {
        world.allAllowed(true);CreativeVehicleChecks.mode(mc,"CREATIVE");
        RenderControls.FULL_BRIGHT.code=36;RenderControls.ENTITIES.code=37;
        var modifiers=new HashMap<>(Bindings.modifiers());
        modifiers.put("power_beta.fullBright",action.equals("chord") ? Chord.CTRL : 0);
        modifiers.put("power_beta.entities",action.equals("chord") ? Chord.CTRL : 0);
        Bindings.configure(modifiers);mc.setScreen(null);
        var s=VisualConfig.copy();s.hideEntities=false;s.entityRenderingMessages=s.fullBrightMessages=true;s.spectatorFullBright=true;VisualConfig.preview(s);
      }
      case "spectator" -> {CreativeVehicleChecks.mode(mc,"SPECTATOR");mc.setScreen(null);}
      case "creative" -> {CreativeVehicleChecks.mode(mc,"CREATIVE");mc.setScreen(null);}
      case "disable" -> {world.allAllowed(false);RenderControls.tick(mc);}
      case "menu" -> mc.setScreen(new local.luke.power.ui.PowerOptionsScreen(null));
      case "close" -> mc.setScreen(null);
      case "on" -> check(RenderControls.fullBright(),"full bright did not turn on");
      case "off" -> check(!RenderControls.fullBright(),"full bright did not turn off");
      case "hidden" -> check(VisualConfig.current().hideEntities,"entities not hidden");
      case "visible" -> check(!VisualConfig.current().hideEntities,"entities remain hidden");
      case "state" -> log("RENDER STATE fullBright="+RenderControls.fullBright()+" hidden="+VisualConfig.current().hideEntities);
      default -> throw new IllegalArgumentException(action);
    }
  }
}
