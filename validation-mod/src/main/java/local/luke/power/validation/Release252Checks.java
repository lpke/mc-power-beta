package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.FreecamPreviewChecks.*;
import java.nio.ByteBuffer;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.death.DeathItems;
import local.luke.power.hud.HudCoordinates;
import local.luke.power.input.*;
import local.luke.power.render.RiddenMinecart;
import local.luke.power.rules.*;
import local.luke.power.validation.mixin.*;
import local.luke.power.worldedit.WorldEditor;
import local.luke.power.worldedit.core.Pos;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.*;
import org.lwjgl.input.*;

/** Selection, rendering and native inventory gestures in a muted disposable world. */
public final class Release252Checks {
  public static boolean captureCarts;
  public static boolean checkingUnloaded;
  public static int chunkRequests;
  public static Object callbackHandler;
  public static int callbackSlot;
  public static Runnable callback;
  public static final Set<Entity> renderedCarts = new HashSet<>();
  private static final String DRAG = "power_inventory:config.MOUSE_TWEAKS_CONFIG.LMBTweakShiftClickAny";
  private static final String EQUIP = "power_inventory:config.MODERN_MINECRAFT_CONFIG.EnableShiftClickingItemsIntoArmorSlots";
  private static ByteBuffer keys, buttons;
  private static void command(Minecraft mc,String text){mc.player.sendChatMessage(text);}
  private static void clear(Minecraft mc){
    mc.player.inventory.setCursorStack(null);mc.player.closeScreen();
    Arrays.fill(mc.player.inventory.main,null);Arrays.fill(mc.player.inventory.armor,null);
  }
  private static ContainerScreen inventory(Minecraft mc){var screen=new class_585(mc.player);mc.setScreen(screen);return screen;}
  private static void event(ContainerScreen screen,int id,int button,boolean press){
    Slot slot=screen.container.method_2084(id);var geometry=(ContainerInput)screen;
    int x=(screen.width-geometry.power$width())/2+slot.x+8,y=(screen.height-geometry.power$height())/2+slot.y+8;
    if(press)((ScreenInput)screen).power$click(x,y,button);else ((ScreenInput)screen).power$release(x,y,button);
  }
  private static void press(ContainerScreen s,int id){buttons.put(0,(byte)1);event(s,id,0,true);}
  private static void drag(ContainerScreen s,int id){event(s,id,-1,false);}
  private static void release(ContainerScreen s,int id){buttons.put(0,(byte)0);event(s,id,0,false);}
  private static Map<String,Integer> contents(Minecraft mc)throws java.io.IOException{
    var result=new TreeMap<String,Integer>();var stacks=new ArrayList<ItemStack>();
    stacks.addAll(Arrays.asList(mc.player.inventory.main));stacks.addAll(Arrays.asList(mc.player.inventory.armor));stacks.add(mc.player.inventory.getCursorStack());
    for(var stack:stacks)if(stack!=null){var one=stack.clone();one.count=1;result.merge(DeathItems.encode(one),stack.count,Integer::sum);}
    return result;
  }
  private static ItemStack armor(int part)throws Exception{return DeathChecks.tagged(310+part,1,7+part,"armour-"+part);}
  static void run(Minecraft mc)throws Exception{
    failures=0;DeathChecks.alive(mc);mc.setScreen(null);active(false);mc.field_2807=mc.player;
    var oldRules=AccessRules.copy(mc);var saved=new net.minecraft.nbt.NbtCompound();mc.player.write(saved);
    var original=SettingsRegistry.open(mc);var building=local.luke.power.building.config.Config.current().copy();
    var visual=local.luke.power.visual.VisualConfig.copy();var input=mc.player.field_161;
    var keyField=Keyboard.class.getDeclaredField("keyDownBuffer");keyField.setAccessible(true);keys=(ByteBuffer)keyField.get(null);
    var mouseField=Mouse.class.getDeclaredField("buttons");mouseField.setAccessible(true);buttons=(ByteBuffer)mouseField.get(null);
    byte oldShift=keys.get(42),oldButton=buttons.get(0);
    var created=new ArrayList<Entity>();
    try{
      AccessRules.restore(mc,RuleSet.cheats());
      test("Spectator and freecam selection uses camera coordinates, direction, targets and the current chunk",()->{
        for(String mode:List.of("SURVIVAL","CREATIVE","SPECTATOR"))for(boolean detached:new boolean[]{false,true}){
          active(false);CreativeVehicleChecks.mode(mc,mode);mc.player.method_1341(12.25,105,14.75,90,0);
          if(detached){pose(32.25,111.75,35.75,0,180);active(true);}
          var expected=ReportedPosition.current(mc.player,mc.player.x,mc.player.y,mc.player.z,mc.player.yaw,mc.player.pitch);
          command(mc,"//pos1");command(mc,"//pos2");
          var editor=WorldEditor.current();Pos p=new Pos((int)Math.floor(expected.x()),(int)Math.floor(expected.y()),(int)Math.floor(expected.z()));
          check(editor.pos1.equals(p)&&editor.pos2.equals(p),"selection uses body in "+mode+" detached="+detached);
          command(mc,"//shift 2 forward");
          check(editor.pos1.equals(p.add(detached?0:-2,0,detached?2:0)),"selection direction differs from view");
          command(mc,"//chunk");check(editor.pos1.x()==((p.x()>>4)<<4)&&editor.pos1.z()==((p.z()>>4)<<4),"wrong camera chunk");
          check(!detached||!WorldEditor.commandAvailable(mc,"set"),"freecam grants world writes");
          active(false);
        }
        CreativeVehicleChecks.mode(mc,"SPECTATOR");mc.player.method_1341(32.5,109,35.5,180,0);
        for(int x=30;x<=34;x++)for(int y=109;y<=113;y++)for(int z=30;z<=36;z++)mc.world.method_154(x,y,z,0,0);
        int targetY=(int)Math.floor(mc.player.method_931(1).y);mc.world.method_154(32,targetY,32,1,0);
        var stale=new class_27(1,1,1,1,Vec3d.createCached(1,1,1));mc.field_2823=stale;
        command(mc,"//hpos1");check(WorldEditor.current().pos1.equals(new Pos(32,targetY,32)),"Spectator targeted stale body hit");
        pose(32.5,targetY+.5,35.5,0,0);active(true);command(mc,"//hpos2");
        check(WorldEditor.current().pos2.equals(new Pos(32,targetY,32)),"freecam target differs");check(mc.field_2823==stale,"selection replaced interaction target");
        pose(31_999_900,110,31_999_900,0,0);chunkRequests=0;checkingUnloaded=true;
        try{command(mc,"//hpos2");}finally{checkingUnloaded=false;}
        check(WorldEditor.current().pos2.equals(new Pos(32,targetY,32)),"unloaded ray changed selection");
        check(chunkRequests==0,"selection invoked chunk loading");active(false);
        AccessRules.restore(mc,new RuleSet());command(mc,"//pos1");
        check(WorldEditor.commandAvailable(mc,"pos1")&&!WorldEditor.commandAvailable(mc,"set"),"selection widened access rules");
        mc.world.isRemote=true;try{check(!WorldEditor.commandAvailable(mc,"pos1"),"local WorldEdit opened on multiplayer");}finally{mc.world.isRemote=false;}
        AccessRules.restore(mc,RuleSet.cheats());
      });
      test("the ridden minecart survives frustum and occlusion samples while moving and looking backwards",()->{
        active(false);CreativeVehicleChecks.mode(mc,"SURVIVAL");mc.player.field_161=new class_41();
        for(int x=32;x<=96;x++){mc.world.method_154(x,99,32,1,0);mc.world.method_154(x,100,32,66,1);for(int y=101;y<=104;y++)mc.world.method_154(x,y,32,0,0);}
        for(boolean fast:new boolean[]{false,true}){
          var settings=building.copy();settings.fastMinecarts=fast;local.luke.power.building.config.Config.preview(settings);
          var cart=new class_549(mc.world,40.5,100.5,32.5,0);created.add(cart);mc.world.method_210(cart);mc.player.method_1376(cart);cart.velocityX=.35;
          boolean frustumFlag=cart.field_1622;double start=cart.x;
          for(int tick=0;tick<24;tick++){
            cart.tick();mc.player.method_1384();cart.method_1382();mc.player.yaw=mc.player.prevYaw=90;
            culled(cart);
            check(RiddenMinecart.visible(cart)&&!(boolean)cart.getClass().getMethod("isCulled").invoke(cart),"ridden cart was occlusion culled");
            for(float partial:new float[]{0,.25f,.5f,.75f,1}){
              render(mc,partial,false);check(renderedCarts.contains(cart),"cart flickered fast="+fast+" tick="+tick+" partial="+partial);
            }
          }
          check(cart.x>start+1,"fixture cart did not travel");check(cart.field_1622==frustumFlag,"render fix changed saved entity flag");
          var hidden=local.luke.power.visual.VisualConfig.copy();hidden.hideEntities=true;hidden.hideVehicles=true;local.luke.power.visual.VisualConfig.preview(hidden);render(mc,.5f,false);
          check(!renderedCarts.contains(cart),"vehicle visibility preference ignored");local.luke.power.visual.VisualConfig.preview(visual);
          mc.player.method_1376(null);culled(cart);
          check(!RiddenMinecart.visible(cart)&&(boolean)cart.getClass().getMethod("isCulled").invoke(cart),"dismount kept a culling override");render(mc,1,false);check(!renderedCarts.contains(cart),"dismounted cart bypassed frustum");
          cart.markDead();cart.method_1340(140,110,140);
        }
      });
      keys.put(42,(byte)1);CreativeVehicleChecks.mode(mc,"SURVIVAL");
      find(original,DRAG).parse("true");find(original,EQUIP).parse("true");original.preview(true);
      test("one held Shift-drag equips and removes all four native armour pieces with full item data",()->{
        clear(mc);var screen=inventory(mc);for(int i=0;i<4;i++)screen.container.method_2084(9+i).setStack(armor(i));var before=contents(mc);
        press(screen,9);for(int i=1;i<4;i++)drag(screen,9+i);release(screen,12);
        for(int i=0;i<4;i++)check(screen.container.method_2084(5+i).hasStack(),"armour not equipped: "+i);
        check(contents(mc).equals(before)&&mc.player.inventory.getCursorStack()==null,"equip lost native data/counts");
        press(screen,5);for(int i=1;i<4;i++)drag(screen,5+i);release(screen,8);
        for(int i=0;i<4;i++)check(!screen.container.method_2084(5+i).hasStack(),"armour not removed: "+i);
        check(contents(mc).equals(before)&&mc.player.inventory.getCursorStack()==null,"unequip lost native data/counts");
      });
      test("armour clicks retain held cursor data, oversized leftovers, pumpkin limits and occupied destinations",()->{
        clear(mc);var screen=inventory(mc);var held=DeathChecks.tagged(4,7,0,"held");mc.player.inventory.setCursorStack(held);String data=DeathItems.encode(held);
        screen.container.method_2084(9).setStack(armor(0));var before=contents(mc);press(screen,9);release(screen,9);
        check(mc.player.inventory.getCursorStack()==held&&DeathItems.encode(held).equals(data)&&contents(mc).equals(before),"held cursor changed");
        clear(mc);screen=inventory(mc);var oversized=armor(1);oversized.count=3;screen.container.method_2084(9).setStack(oversized);
        screen.container.method_2084(10).setStack(DeathChecks.tagged(86,64,0,"pumpkins"));before=contents(mc);
        press(screen,9);drag(screen,10);release(screen,10);
        check(screen.container.method_2084(6).getStack().count==1&&screen.container.method_2084(9).getStack().count==2,"armour slot limit/oversized leftovers changed");
        check(screen.container.method_2084(5).getStack().count==1&&screen.container.method_2084(10).getStack().count==63,"pumpkin split changed");
        check(contents(mc).equals(before)&&mc.player.inventory.getCursorStack()==null,"split lost counts or NBT");
        var occupied=screen.container.method_2084(5).getStack();screen.container.method_2084(11).setStack(armor(0));before=contents(mc);
        press(screen,11);release(screen,11);check(screen.container.method_2084(5).getStack()==occupied&&contents(mc).equals(before),"occupied helmet was replaced");
      });
      test("disabled drag, full inventory, cancellation and Spectator leave armour protected",()->{
        clear(mc);var screen=inventory(mc);for(int i=0;i<4;i++)screen.container.method_2084(9+i).setStack(armor(i));
        find(original,DRAG).parse("false");original.preview(true);press(screen,9);drag(screen,10);release(screen,10);
        check(screen.container.method_2084(5).hasStack()&&!screen.container.method_2084(6).hasStack(),"disabled drag equipped another piece");
        find(original,DRAG).parse("true");original.preview(true);press(screen,10);((InventoryDrag)screen).power$cancelItemDrag();drag(screen,11);release(screen,11);
        check(!screen.container.method_2084(7).hasStack(),"cancelled drag restarted while held");
        find(original,EQUIP).parse("false");original.preview(true);clear(mc);screen=inventory(mc);
        screen.container.method_2084(9).setStack(armor(0));screen.container.method_2084(10).setStack(armor(1));var disabled=contents(mc);
        press(screen,9);drag(screen,10);release(screen,10);
        check(!screen.container.method_2084(5).hasStack()&&!screen.container.method_2084(6).hasStack()&&contents(mc).equals(disabled),"disabled armour shortcut still equipped");
        find(original,EQUIP).parse("true");original.preview(true);
        clear(mc);screen=inventory(mc);for(int i=9;i<45;i++)screen.container.method_2084(i).setStack(new ItemStack(4,64,0));
        screen.container.method_2084(5).setStack(armor(0));var before=contents(mc);press(screen,5);drag(screen,6);release(screen,6);
        check(screen.container.method_2084(5).hasStack()&&contents(mc).equals(before),"full inventory lost armour");
        CreativeVehicleChecks.mode(mc,"SPECTATOR");before=contents(mc);press(screen,5);release(screen,5);check(contents(mc).equals(before),"Spectator moved armour");
      });
      test("native callbacks cannot make an armour transfer overwrite changed slots or a new owner",()->{
        CreativeVehicleChecks.mode(mc,"SURVIVAL");
        for(String change:List.of("source","destination","owner")){
          clear(mc);var screen=inventory(mc);var source=screen.container.method_2084(9);var target=screen.container.method_2084(5);
          source.setStack(armor(0));var inserted=armor(0);inserted.setDamage(45);String before=DeathItems.encode(source.getStack());
          callbackHandler=screen.container;callbackSlot=9;
          callback=()->{if(change.equals("source"))source.setStack(inserted);else if(change.equals("destination"))target.setStack(inserted);else mc.player.container=null;};
          try{press(screen,9);}finally{callback=null;callbackHandler=null;mc.player.container=screen.container;}
          check(mc.player.inventory.getCursorStack()!=null&&DeathItems.encode(mc.player.inventory.getCursorStack()).equals(before),"callback changed picked armour "+change);
          check(change.equals("destination")?target.getStack()==inserted:!target.hasStack(),"callback destination overwritten "+change);
          if(change.equals("source"))check(source.getStack()==inserted,"callback source overwritten");
          release(screen,9);
        }
      });
    }finally{
      callback=null;callbackHandler=null;captureCarts=false;active(false);mc.world.isRemote=false;keys.put(42,oldShift);buttons.put(0,oldButton);
      if(mc.player.field_1595!=null)mc.player.method_1376(null);for(Entity e:created)e.markDead();
      original.discard();local.luke.power.building.config.Config.preview(building);local.luke.power.visual.VisualConfig.preview(visual);
      mc.player.field_161=input;clear(mc);mc.player.read(saved);AccessRules.restore(mc,oldRules);
    }
    log("RELEASE 2.5.2 FAILURES "+failures);
  }
  private static void culled(Entity entity)throws Exception{
    entity.getClass().getMethod("setCulled",boolean.class).invoke(entity,true);
    var timeout=Entity.class.getDeclaredField("lastTime");timeout.setAccessible(true);timeout.setLong(entity,0);
  }
  private static void render(Minecraft mc,float delta,boolean frustum)throws Exception{
    UiChecks.field(mc.worldRenderer,"field_1783",0);renderedCarts.clear();captureCarts=true;
    try{mc.worldRenderer.method_1544(Vec3d.createCached(mc.player.x,mc.player.y,mc.player.z),new class_68(){
      public boolean method_2007(Box box){return frustum;}
      public void method_2006(double x,double y,double z){}
    },delta);}finally{captureCarts=false;}
  }
}
