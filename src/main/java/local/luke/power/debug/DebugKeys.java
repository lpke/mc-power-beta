package local.luke.power.debug;

import java.util.*;
import local.luke.power.input.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

/** Runs at the native event boundary, before gameplay listeners and native shortcuts. */
public final class DebugKeys {
  public static final KeyBinding MODIFIER = new KeyBinding("powerbeta.debug.modifier", Keyboard.KEY_F3);
  private static final Map<DebugAction,KeyBinding> KEYS = new LinkedHashMap<>();
  private static Object world;
  private static long crashStart;
  private static int warned;
  static { for (var action:DebugAction.values()) KEYS.put(action,new KeyBinding(action.id,action.key)); }
  private DebugKeys() {}
  public static List<KeyBinding> all() { var all=new ArrayList<KeyBinding>(); all.add(MODIFIER); all.addAll(KEYS.values()); return all; }
  public static KeyBinding binding(DebugAction action) { return KEYS.get(action); }
  private static boolean active(Minecraft mc) {
    return DebugConfig.enabled() && mc.options!=null && mc.world!=null && mc.player!=null
        && mc.currentScreen==null && Display.isActive() && !mc.player.dead;
  }
  public static void event(int key, boolean down, boolean repeat) {
    Minecraft mc=(Minecraft)FabricLoader.getInstance().getGameInstance();
    if(mc==null) return;
    DebugInput.context(active(mc));
    if(!active(mc)) { crashStart=0; return; }
    if(!down) {
      if(key==MODIFIER.code && DebugInput.release()) { mc.options.debugHud=!mc.options.debugHud; GameplayKeys.debugText(); }
      DebugInput.released(key);
      return;
    }
    if(repeat) return;
    if(Bindings.matches(MODIFIER,key) && Bindings.heldModifiers()==Bindings.modifiers(MODIFIER.translationKey)) {
      boolean same=Arrays.stream(mc.options.allKeys).anyMatch(b -> b.translationKey.equals("key.power_controls.debug_hud")
          && b.code==key && Bindings.modifiers(b.translationKey)==Bindings.modifiers(MODIFIER.translationKey));
      DebugInput.press(same); return;
    }
    if(!DebugInput.held()) return;
    if(Bindings.down("power_creative.modifier") && Bindings.matches("power_creative.picker",key)) {
      DebugInput.picker(key); return; // Existing mode picker owns its own rules and release gesture.
    }
    for(var entry:KEYS.entrySet()) {
      if(!Bindings.matches(entry.getValue(),key)) continue;
      DebugInput.claim(key);
      if(entry.getKey()==DebugAction.CRASH) continue;
      try { DebugActions.run(mc,entry.getKey()); }
      catch(Exception error) {
        local.luke.power.PowerBeta.LOG.error("Debug action failed: {}",entry.getKey(),error);
        DebugActions.message(mc,"Could not complete "+entry.getKey().label.toLowerCase(Locale.ROOT)+". See the game log.");
      }
      return;
    }
  }
  public static void tick(Minecraft mc) {
    if(world!=mc.world) { world=mc.world; DebugInput.clear(); DebugActions.clear(); }
    boolean active=active(mc); DebugInput.context(active);
    boolean crashing=active && DebugInput.held() && Bindings.down(binding(DebugAction.CRASH));
    if(!crashing) { crashStart=0; warned=0; }
    else {
      long now=System.nanoTime();
      if(crashStart==0) crashStart=now;
      int seconds=(int)((now-crashStart)/1_000_000_000L);
      if(seconds>=10) throw new RuntimeException("Manually triggered debug crash");
      if(seconds>=1 && seconds>warned) { warned=seconds; DebugActions.message(mc,"Manual crash in "+(10-seconds)+" seconds. Release either key to cancel."); }
    }
    DebugPerformance.tick(mc);
  }
}
