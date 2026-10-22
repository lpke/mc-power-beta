package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.Field;
import java.util.*;
import local.luke.power.building.config.Config;
import local.luke.power.building.config.Settings;
import local.luke.power.building.hotbar.Hotbars;
import local.luke.power.debug.*;
import local.luke.power.death.DeathItems;
import local.luke.power.input.*;
import local.luke.power.inventory.InventoryActions;
import local.luke.power.inventory.NativeInventoryPlan;
import local.luke.power.storage.PowerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

/** Watch actual native key batches, including transient row publication between driver checks. */
final class DebugKeys420Checks {
  private static Run active;
  private DebugKeys420Checks() {}
  static void run(Minecraft mc,String command)throws Exception {
    if(command.equals("debug-hotbar-stop")){finish();return;}
    if(command.equals("debug-hotbar-check")) {
      check(active!=null,"arm debug-hotbar first");active.verify();
      check(active.ticks>0,"no native ticks observed");
      check(!Bindings.physical(Keyboard.KEY_F3)&&!Bindings.physical(Keyboard.KEY_Q),"release F3 and Q before checking");
      check(!DebugInput.held()&&!DebugInput.claimed(Keyboard.KEY_Q),"debug chord retained ownership");
      if(active.ordinary)check(active.published,"ordinary Q did not publish its prepared row on release");
      else {
        check(DebugInput.entryGeneration()!=active.entry,"no native debug entry observed");
        check(active.preheld||!mc.options.debugHud,"debug chord toggled plain F3");
      }
      log("PASS DEBUG420 native F3+Q bytes/row publication ticks="+active.ticks+" checks="+(++active.checks));return;
    }
    check(Set.of("debug-hotbar","debug-hotbar-preheld","debug-hotbar-ordinary").contains(command),"Use debug-hotbar, debug-hotbar-preheld, debug-hotbar-ordinary, debug-hotbar-check or debug-hotbar-stop");
    check(active==null,"debug-hotbar already armed");
    check(mc.world!=null&&!mc.world.isRemote&&mc.player!=null&&!mc.player.dead&&mc.player.health>0
        &&mc.currentScreen==null&&mc.field_2778&&Display.isActive()&&!DetachedCamera.isActive(),"focus a disposable living singleplayer world outside menus/freecam");
    check(mc.options.musicVolume==0&&mc.options.soundVolume==0,"mute the disposable instance first");
    check(!Bindings.physical(Keyboard.KEY_F3)&&!Bindings.physical(Keyboard.KEY_Q),"release F3 and Q before arming");
    check(!NativeInventoryPlan.busy()&&!InventoryActions.busy()&&transaction("pending")==null,"finish pending inventory actions first");
    check(mc.player.container==mc.player.playerContainer&&mc.player.inventory.getCursorStack()==null,"close containers and clear the cursor first");
    failures=0;active=new Run(mc,command);log("DEBUG420 armed "+command+"; use native keys, then debug-hotbar-check and debug-hotbar-stop");
  }
  static void tick(Minecraft mc) {
    if(active==null)return;
    try{active.verify();active.ticks++;}
    catch(Exception|AssertionError error) {
      try{test("DEBUG420 native F3+Q never publishes an ordinary row",()->{throw new Exception(error);});}
      catch(Exception failed){failed.printStackTrace();}
      try{finish();}catch(Exception restore){throw new RuntimeException(restore);}
    }
  }
  private static void finish()throws Exception {
    var run=active;active=null;if(run!=null)run.close();log("DEBUG420 stopped failures="+failures);
  }
  private static Object transaction(String name)throws Exception {
    Field field=Class.forName("local.luke.power.building.hotbar.HotbarTransaction").getDeclaredField(name);field.setAccessible(true);return field.get(null);
  }
  private static final class Run implements AutoCloseable {
    final Minecraft mc;final Object world,player;final PlayerInventory inventory;
    final ItemStack[] main,armor;final int selected;final boolean debug,overlay;
    final Settings building=Config.current().copy();final com.google.gson.JsonObject document=PowerConfig.section("building");
    final Map<String,Integer> modifiers=Bindings.modifiers();final Map<KeyBinding,Integer> codes=new LinkedHashMap<>();
    final String bytes,swappedBytes;final ItemStack[] owned;final boolean preheld,ordinary;final long entry;
    int ticks,checks,releaseTick=-1;boolean published,wasDown;
    Run(Minecraft mc,String command)throws Exception {
      this.mc=mc;world=mc.world;player=mc.player;inventory=mc.player.inventory;
      preheld=command.equals("debug-hotbar-preheld");ordinary=command.equals("debug-hotbar-ordinary");
      main=inventory.main;armor=inventory.armor;selected=inventory.selectedSlot;debug=DebugConfig.enabled();overlay=mc.options.debugHud;
      try {
      for(KeyBinding binding:Hotbars.ALL){codes.put(binding,binding.code);binding.code=binding==Hotbars.SCROLL?Keyboard.KEY_Q:0;}
      codes.put(DebugKeys.MODIFIER,DebugKeys.MODIFIER.code);DebugKeys.MODIFIER.code=Keyboard.KEY_F3;
      codes.put(GameplayKeys.GRAPH,GameplayKeys.GRAPH.code);GameplayKeys.GRAPH.code=Keyboard.KEY_F3;
      var chords=new HashMap<>(modifiers);chords.remove(DebugKeys.MODIFIER.translationKey);chords.remove(Hotbars.SCROLL.translationKey);
      chords.put(GameplayKeys.GRAPH.translationKey,Chord.SHIFT);Bindings.configure(chords);Bindings.register(mc.options.allKeys);
      var settings=building.copy();settings.hotbar.selectedRow=1;settings.hotbar.rememberRow=true;settings.hotbar.overlay=true;settings.hotbar.numberRowKeys=false;Config.preview(settings);
      inventory.main=new ItemStack[36];inventory.armor=new ItemStack[4];inventory.selectedSlot=0;
      for(int index=0;index<36;index++)inventory.main[index]=DeathChecks.tagged(35,index+1,index%16,"debug420-slot-"+index);
      inventory.armor[0]=DeathChecks.tagged(301,1,8,"debug420-armor");owned=inventory.main;
      DebugConfig.preview(true);DebugInput.clear();mc.options.debugHud=false;Hotbars.tick(mc);
      check(Hotbars.eligible(mc),"native hotbar eligibility failed");bytes=snapshot();entry=DebugInput.entryGeneration();
      inventory.main=local.luke.power.building.hotbar.InventoryRows.swapped(owned,1);
      try{swappedBytes=snapshot();}finally{inventory.main=owned;}
      } catch(Exception|Error error){close();throw error;}
    }
    String snapshot()throws Exception{return DeathItems.inventory(DeathItems.snapshot(inventory,mc.player.playerContainer));}
    void verify()throws Exception {
      check(mc.world==world&&mc.player==player,"debug fixture changed world/player");
      String current=snapshot();Object pending=transaction("pending");
      boolean entered=DebugInput.entryGeneration()!=entry;
      if(ordinary) {
        check(!entered,"ordinary positive control entered Debug");
        if(swappedBytes.equals(current))published=true;
        else check(!published&&inventory.main==owned&&bytes.equals(current),"ordinary Q changed unexpected native bytes or repeated the row");
        boolean down=Bindings.physical(Keyboard.KEY_Q);
        if(wasDown&&!down&&releaseTick<0)releaseTick=ticks;
        if(releaseTick>=0&&ticks>releaseTick+1)check(published,"ordinary Q waited beyond the native release tick");
        wasDown=down;
      } else check(inventory.main==owned&&bytes.equals(current),"native F3+Q changed full inventory bytes or published a row");
      boolean allowedPreview=ordinary||preheld&&!entered;
      if(pending!=null&&!allowedPreview) {
        var cancelled=pending.getClass().getDeclaredField("cancelled");cancelled.setAccessible(true);
        check(cancelled.getBoolean(pending),"native F3+Q prepared or queued an ordinary hotbar row");
      }
      check(((Deque<?>)transaction("rows")).isEmpty(),"native key queued duplicate hotbar rows");
      check(Config.current().hotbar.selectedRow==1,"native F3+Q saved a selected hotbar row");
    }
    public void close()throws Exception {
      Hotbars.cancelPending();inventory.main=main;inventory.armor=armor;inventory.selectedSlot=selected;
      for(var entry:codes.entrySet())entry.getKey().code=entry.getValue();Bindings.configure(modifiers);Bindings.register(mc.options.allKeys);
      Config.preview(building);PowerConfig.saveDeferred("building",document);DebugConfig.preview(debug);DebugInput.clear();mc.options.debugHud=overlay;
    }
  }
}
