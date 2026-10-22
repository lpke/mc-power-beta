package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.util.*;
import local.luke.power.autowalk.AutoWalk;
import local.luke.power.building.config.Config;
import local.luke.power.building.config.Settings;
import local.luke.power.building.mining.AutoMine;
import local.luke.power.debug.*;
import local.luke.power.fastplace.FastPlace;
import local.luke.power.fakesneak.FakeSneak;
import local.luke.power.input.*;
import local.luke.power.storage.PowerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.opengl.Display;

/** Native driver watcher for immediate activation and debug-owned activation denial. */
final class DebugToggles420Checks {
  private static Run active;
  static void run(Minecraft mc,String command)throws Exception {
    if(command.equals("debug-toggles-stop")){finish();return;}
    if(command.equals("debug-toggles-check")) {
      check(active!=null,"arm debug-toggles first");active.verify();
      for(int code:new int[]{61,114,115,116,117})check(!Bindings.physical(code),"release F3 and F20-F23 before checking");
      if(active.ordinary)check(active.seen==15,"ordinary native activation callbacks missing: "+active.seen);
      else check(DebugInput.entryGeneration()!=active.entry,"no native debug entry observed");
      log("PASS DEBUG420 native toggle ownership ordinary="+active.ordinary+" immediate="+active.seen+" ticks="+active.ticks);return;
    }
    check(command.equals("debug-toggles")||command.equals("debug-toggles-ordinary"),"Use debug-toggles[-ordinary], debug-toggles-check or debug-toggles-stop");
    check(active==null,"debug-toggles already armed");
    check(mc.world!=null&&!mc.world.isRemote&&mc.player!=null&&mc.player.health>0&&!mc.player.dead
        &&mc.currentScreen==null&&mc.field_2778&&Display.isActive()&&!DetachedCamera.isActive(),"focus a disposable living singleplayer world");
    check(mc.options.musicVolume==0&&mc.options.soundVolume==0,"mute the disposable instance first");
    for(int code:new int[]{61,114,115,116,117})check(!Bindings.physical(code),"release F3 and F20-F23 first");
    active=new Run(mc,command.endsWith("-ordinary"));
    Bindings.onKeyPress("validation.debug420",code->{if(active!=null)active.pressed(code);});
    log("DEBUG420 armed "+command+"; F20 AutoMine, F21 AutoWalk, F22 FastPlace, F23 FakeSneak");
  }
  static void tick(Minecraft mc) {
    if(active==null)return;
    try{active.verify();active.ticks++;}
    catch(Exception|AssertionError error){
      try{test("DEBUG420 native activation edges",()->{throw new Exception(error);});}catch(Exception failed){failed.printStackTrace();}
      try{finish();}catch(Exception restore){throw new RuntimeException(restore);}
    }
  }
  private static void finish()throws Exception{var run=active;active=null;if(run!=null)run.close();log("DEBUG420 toggle watcher stopped");}
  private static final class Run implements AutoCloseable {
    final Minecraft mc;final Object world,player;final boolean ordinary,debug,overlay;
    final Settings building=Config.current().copy();final com.google.gson.JsonObject document=PowerConfig.section("building");
    final Map<String,Integer> modifiers=Bindings.modifiers();final Map<KeyBinding,Integer> codes=new LinkedHashMap<>();final long entry;
    int ticks,seen;String problem;
    Run(Minecraft mc,boolean ordinary){
      this.mc=mc;this.ordinary=ordinary;world=mc.world;player=mc.player;debug=DebugConfig.enabled();overlay=mc.options.debugHud;
      for(var binding:mc.options.allKeys)if(binding.code>=114&&binding.code<=117){codes.put(binding,binding.code);binding.code=0;}
      KeyBinding[] keys={AutoMine.KEY,AutoWalk.KEY,FastPlace.KEY,local.luke.power.fakesneak.Keys.ALL[0]};
      for(int i=0;i<keys.length;i++){codes.putIfAbsent(keys[i],keys[i].code);keys[i].code=114+i;}
      codes.putIfAbsent(DebugKeys.MODIFIER,DebugKeys.MODIFIER.code);DebugKeys.MODIFIER.code=61;
      codes.putIfAbsent(GameplayKeys.GRAPH,GameplayKeys.GRAPH.code);GameplayKeys.GRAPH.code=61;
      var chords=new HashMap<>(modifiers);for(var key:keys)chords.remove(key.translationKey);
      chords.remove(DebugKeys.MODIFIER.translationKey);chords.put(GameplayKeys.GRAPH.translationKey,Chord.SHIFT);
      Bindings.configure(chords);Bindings.register(mc.options.allKeys);
      var settings=building.copy();settings.placement.setEnabled(false);settings.sneak.enabled=false;settings.autoWalkHoldToWalk=false;Config.preview(settings);
      AutoMine.tick(mc);AutoWalk.tick(mc);FastPlace.tickInput(mc);FakeSneak.tick(mc);
      AutoMine.stop();AutoWalk.stop();AutoMine.tick(mc);AutoWalk.tick(mc);
      DebugConfig.preview(true);DebugInput.clear();mc.options.debugHud=false;entry=DebugInput.entryGeneration();
    }
    int states(){return (AutoMine.active()?1:0)|(AutoWalk.isWalking()?2:0)|(Config.current().placement.enabled?4:0)|(Config.current().sneak.enabled?8:0);}
    void pressed(int code){
      if(code<114||code>117)return;int bit=1<<(code-114),state=states();
      if(ordinary){if((state&bit)==0)problem="native press deferred activation code="+code+" state="+state;seen|=bit;}
      else if(state!=0)problem="debug-owned native press activated code="+code+" state="+state;
      // Observe initial activation at dispatch, then prevent automatic terrain/movement side effects.
      if(code==114)AutoMine.stop();if(code==115)AutoWalk.stop();
    }
    void verify(){check(mc.world==world&&mc.player==player,"toggle fixture changed world/player");check(problem==null,problem);if(!ordinary)check(states()==0,"debug polling activated a toggle: "+states());}
    public void close(){AutoMine.stop();AutoWalk.stop();FastPlace.release();for(var code:codes.entrySet())code.getKey().code=code.getValue();Bindings.configure(modifiers);Bindings.register(mc.options.allKeys);Config.preview(building);PowerConfig.saveDeferred("building",document);DebugConfig.preview(debug);DebugInput.clear();mc.options.debugHud=overlay;}
  }
}
