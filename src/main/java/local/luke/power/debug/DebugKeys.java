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
  static {
    for (var action:DebugAction.values()) KEYS.put(action,new KeyBinding(action.id,action.key));
    DebugInput.preflight(DebugKeys::preflight);
  }
  private DebugKeys() {}
  public static List<KeyBinding> all() { var all=new ArrayList<KeyBinding>(); all.add(MODIFIER); all.addAll(KEYS.values()); return all; }
  public static KeyBinding binding(DebugAction action) { return KEYS.get(action); }
  private static boolean active(Minecraft mc) {
    return DebugConfig.enabled() && mc.options!=null && mc.world!=null && mc.player!=null
        && (mc.currentScreen==null || mc.currentScreen instanceof net.minecraft.client.gui.screen.container.ContainerScreen
            && !InventoryTextInput.active(mc.currentScreen) && !InventoryPanels.modal(mc.currentScreen))
        && Display.isActive() && !mc.player.dead;
  }
  private static int preflight() {
    Minecraft mc=(Minecraft)FabricLoader.getInstance().getGameInstance();
    boolean active=mc!=null && active(mc);
    DebugInput.context(active);
    return active && Bindings.physical(MODIFIER.code) && Bindings.downUnclaimed(MODIFIER)
        && !(GameplayKeys.GRAPH.code==MODIFIER.code && Bindings.downUnclaimed(GameplayKeys.GRAPH)) ? MODIFIER.code : 0;
  }
  public static void event(int key, boolean down, boolean repeat) {
    if (!down) Bindings.released(key);
    Minecraft mc=(Minecraft)FabricLoader.getInstance().getGameInstance();
    if(mc==null) return;
    DebugInput.context(active(mc));
    if(!active(mc)) { crashStart=0; return; }
    try {
    if(!down) {
      if(key==MODIFIER.code && DebugInput.release()) { mc.options.debugHud=!mc.options.debugHud; GameplayKeys.debugText(); }
      DebugInput.released(key);
      return;
    }
    if(repeat) return;
    if(Bindings.matches(MODIFIER,key) && !Bindings.matches(GameplayKeys.GRAPH,key)) {
      boolean same=Arrays.stream(mc.options.allKeys).anyMatch(b -> b.translationKey.equals("key.power_controls.debug_hud")
          && b.code==key && Bindings.modifiers(b.translationKey)==Bindings.modifiers(MODIFIER.translationKey));
      DebugInput.press(same && mc.currentScreen==null); return;
    }
    if(!DebugInput.held()) {
      // Mouse events precede queued keyboard events. Preflight their native press
      // too when the physical debug hold has not reached its modifier event yet.
      DebugInput.allows("native",key,true);if(!DebugInput.actionHeld())return;
    }
    if(configured(mc,"power_creative.modifier",null) && configured(mc,"power_creative.picker",key)) {
      DebugInput.picker(key); return; // Existing mode picker owns its own rules and release gesture.
    }
    for(var entry:KEYS.entrySet()) {
      if (mc.currentScreen!=null && entry.getKey()!=DebugAction.TOOLTIPS) continue;
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
    // Own fresh presses immediately. Movement held before Debug remains held.
    DebugInput.claim(key);
    } finally { DebugInput.dispatched(key,down); }
  }
  private static boolean configured(Minecraft mc,String id,Integer event) {
    for(var binding:mc.options.allKeys) if(binding.translationKey.equals(id)) {
      int mask=Bindings.modifiers(id);
      return binding.code!=0 && (event==null?Bindings.physical(binding.code):binding.code==event)
          && (Bindings.heldModifiers()&mask)==mask;
    }
    return false;
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
