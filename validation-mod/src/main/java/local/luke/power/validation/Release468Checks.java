package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.death.DeathItems;
import local.luke.power.inventory.*;
import local.luke.power.visual.*;
import local.luke.power.ui.*;
import net.minecraft.class_196;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/** Native settings and full-data inventory checks on owned disposable arrays. */
final class Release468Checks {
  private static final Path REPORT=Path.of("power-beta-data/reports/release-468");
  private static final String TABLE="visual.shiftClickIntoCraftingGrid", PERSONAL="visual.shiftClickIntoPlayerCraftingGrid";
  static void run(Minecraft mc,String action)throws Exception{
    check(Set.of("capture","check").contains(action),"use release-468 capture|check");
    SilentAudioChecks.ready(mc);
    check(mc.world!=null&&!mc.world.isRemote&&Display.isActive(),"use focused disposable singleplayer");
    check(mc.options.musicVolume==0&&mc.options.soundVolume==0,"mute disposable client");
    check(!NativeInventoryPlan.busy()&&!NativeInventoryPlan.failed()&&!Mouse.isButtonDown(0),"finish inventory actions");
    Files.createDirectories(REPORT);failures=0;
    capture(mc);
    if(action.equals("check"))try(var state=new Inventory442Checks.State(mc)){
      test("RELEASE468 independent defaults and isolated settings drafts",()->settings(mc));
      test("RELEASE468 both settings control only their own native grid",()->{
        for(boolean table:new boolean[]{false,true})for(boolean personal:new boolean[]{false,true}){
          reset(state,table,personal);fullHotbar(mc);var source=tag(7,"independent");mc.player.inventory.main[9]=source;String data=bytes(source);
          mc.player.playerContainer.onSlotClick(9,0,true,mc.player);state.inventory.finish();
          check(personal?mc.player.inventory.main[9]==null&&data.equals(bytes(state.inventory.context().inputs.get(0).getStack()))
              :data.equals(bytes(mc.player.inventory.main[9]))&&empty(state),"personal transfer followed table toggle");
          reset(state,table,personal);var point=state.block(58);mc.player.method_484(point[0],point[1],point[2]);
          var handler=(class_196)mc.player.container;handler.method_2084(10).setStack(tag(7,"table"));
          handler.onSlotClick(10,0,true,mc.player);
          check(table?handler.field_708.getStack(0)!=null&&handler.field_708.getStack(0).count==7
              :handler.field_708.getStack(0)==null&&mc.player.inventory.main[0]!=null&&mc.player.inventory.main[0].count==7,"table transfer followed personal toggle");
        }
      });
      test("RELEASE468 partial hotbar and storage merges retain full-data leftovers",()->{
        for(boolean hotbarSource:new boolean[]{false,true})for(int room:new int[]{1,2,7}){
          reset(state,false,true);int sourceIndex=hotbarSource?0:9,destination=hotbarSource?35:8;
          if(hotbarSource)for(int i=9;i<36;i++)mc.player.inventory.main[i]=new ItemStack(267,1,0);else fullHotbar(mc);
          var source=tag(12,"partial");mc.player.inventory.main[sourceIndex]=source;mc.player.inventory.main[destination]=copy(source,64-room);
          int slot=hotbarSource?36:9;mc.player.playerContainer.onSlotClick(slot,0,true,mc.player);state.inventory.finish();
          check(bytes(copy(source,12-room)).equals(bytes(mc.player.inventory.main[sourceIndex]))
              &&bytes(copy(source,64)).equals(bytes(mc.player.inventory.main[destination]))&&empty(state)&&!NativeInventoryPlan.busy(),"recursive retry moved or changed a remainder");
          mc.player.playerContainer.onSlotClick(slot,0,true,mc.player);state.inventory.finish();
          check(mc.player.inventory.main[sourceIndex]==null&&bytes(copy(source,12-room)).equals(bytes(state.inventory.context().inputs.get(0).getStack())),"new intentional click retained old recursion suppression");
        }
      });
      test("RELEASE468 normal empty storage keeps priority",()->{
        reset(state,false,true);fullHotbar(mc);mc.player.inventory.main[4]=null;var source=tag(19,"space");String before=bytes(source);mc.player.inventory.main[9]=source;
        mc.player.playerContainer.onSlotClick(9,0,true,mc.player);state.inventory.finish();
        check(mc.player.inventory.main[9]==null&&before.equals(bytes(mc.player.inventory.main[4]))&&empty(state),"normal empty hotbar used grid");
      });
      test("RELEASE468 durable personal transfer uses only its own setting",()->{
        for(boolean keepPersonal:new boolean[]{true,false}){
          reset(state,true,true);fullHotbar(mc);var source=tag(11,"pending");mc.player.inventory.main[9]=source;String before=bytes(source);
          try(var slow=new AutoTools400State.SlowWriter()){
            mc.player.playerContainer.onSlotClick(9,0,true,mc.player);slow.checkBlocked();
            check(before.equals(bytes(mc.player.inventory.main[9]))&&empty(state),"grid wrote before journal flush");
            flags(!keepPersonal,keepPersonal);slow.release();state.inventory.finish();
          }
          check(keepPersonal?mc.player.inventory.main[9]==null&&before.equals(bytes(state.inventory.context().inputs.get(0).getStack()))
              :before.equals(bytes(mc.player.inventory.main[9]))&&empty(state),"wrong preference gated durable publication");
          check(!NativeInventoryPlan.failed(),"preference cancellation disabled inventory actions");
        }
      });
      test("RELEASE468 throwing slot restores the outer-click scope",()->{
        reset(state,false,true);fullHotbar(mc);mc.player.inventory.main[9]=tag(7,"throwing");var handler=mc.player.playerContainer;Slot old=handler.method_2084(36);
        Slot faulty=new Slot(mc.player.inventory,0,old.x,old.y){@Override public boolean canInsert(ItemStack value){throw new IllegalStateException("Owned fixture slot");}};faulty.id=36;handler.slots.set(36,faulty);
        try{boolean thrown=false;try{handler.onSlotClick(9,0,true,mc.player);}catch(IllegalStateException expected){thrown=true;}check(thrown&&mc.player.inventory.main[9].count==7&&empty(state),"throwing fixture changed inventory");}
        finally{handler.slots.set(36,old);}
        handler.onSlotClick(9,0,true,mc.player);state.inventory.finish();check(mc.player.inventory.main[9]==null&&!empty(state),"exception leaked native click depth");
      });
    }
    Files.writeString(REPORT.resolve(action+".json"),"{\"failures\":"+failures+"}");log("RELEASE468 "+action+" failures="+failures);check(failures==0,"release-468 failures="+failures);
  }
  private static void capture(Minecraft mc)throws Exception{
    check(Display.getWidth()>=1708&&Display.getHeight()>=960,"resize capture display to1708x960");
    try(var state=new Release462Checks.State(mc)){
      for(int[] size:new int[][]{{854,480},{320,240}})for(boolean personal:new boolean[]{false,true}){
        var options=state.open(size,local.luke.power.audio.MusicPlaylist.BETA,false);set(options,"page","Inventory");
        ((TextInput)get(options,"search")).setText("Shift-click into");
        find(options.session(),TABLE).parse("true");find(options.session(),PERSONAL).parse(Boolean.toString(personal));options.session().preview();call(options,"layout");
        SettingsSnapshot.render(mc,options,size[0],size[1],REPORT.resolve("settings-"+(personal?"enabled":"defaults")+"-"+size[0]+".png"),-1,-1);
      }
    }
  }
  private static void settings(Minecraft mc)throws Exception{
    var original=VisualConfig.copy();try{
      flags(false,false);var session=SettingsRegistry.open(mc);var table=find(session,TABLE);var personal=find(session,PERSONAL);
      check(table.defaultValue.getAsBoolean()&&!personal.defaultValue.getAsBoolean()&&new VisualSettings().shiftClickIntoCraftingGrid&&!new VisualSettings().shiftClickIntoPlayerCraftingGrid,"wrong independent defaults");
      table.parse("true");session.preview();check(VisualConfig.current().shiftClickIntoCraftingGrid&&!VisualConfig.current().shiftClickIntoPlayerCraftingGrid,"table preview toggled personal");
      table.parse("false");personal.parse("true");session.preview();check(!VisualConfig.current().shiftClickIntoCraftingGrid&&VisualConfig.current().shiftClickIntoPlayerCraftingGrid,"personal preview toggled table");
      session.discard();check(!VisualConfig.current().shiftClickIntoCraftingGrid&&!VisualConfig.current().shiftClickIntoPlayerCraftingGrid,"Cancel lost independent values");
    }finally{VisualConfig.preview(original);}
  }
  private static Setting find(ConfigSession s,String id){return s.settings().stream().filter(v->v.id.equals(id)).findFirst().orElseThrow();}
  private static void reset(Inventory442Checks.State s,boolean table,boolean personal)throws Exception{s.reset(false,false);flags(table,personal);}
  private static void flags(boolean table,boolean personal){var v=VisualConfig.copy();v.shiftClickIntoCraftingGrid=table;v.shiftClickIntoPlayerCraftingGrid=personal;VisualConfig.preview(v);}
  private static void fullHotbar(Minecraft mc){for(int i=0;i<9;i++)mc.player.inventory.main[i]=new ItemStack(267,1,0);}
  private static boolean empty(Inventory442Checks.State s){return s.inventory.context().inputs.stream().allMatch(v->v.getStack()==null);}
  private static ItemStack tag(int count,String name)throws Exception{return DeathChecks.tagged(35,count,4,name);}
  private static ItemStack copy(ItemStack s,int count){var c=ItemStack.clone(s);c.count=count;return c;}
  private static String bytes(ItemStack s)throws Exception{return s==null?null:DeathItems.encode(s);}
  private static Object get(Object o,String name)throws Exception{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
  private static void set(Object o,String name,Object v)throws Exception{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,v);}
  private static void call(Object o,String name)throws Exception{var m=o.getClass().getDeclaredMethod(name);m.setAccessible(true);m.invoke(o);}
}
