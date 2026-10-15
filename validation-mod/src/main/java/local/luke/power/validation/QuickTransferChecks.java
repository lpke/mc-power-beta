package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.death.DeathItems;
import local.luke.power.input.InventoryDrag;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

/** Exercises the actual click path and native handlers, including full serialized item data. */
final class QuickTransferChecks {
  private static final String SETTING="power_inventory:config.MODERN_MINECRAFT_CONFIG.ShiftDoubleClickTransfer";
  private static ContainerScreen physicalScreen;
  private static ItemStack physicalCursor;
  private static String physicalCursorData;
  private static int physicalOrigin;
  static void run(Minecraft mc, String action) throws Exception {
    if (!action.isEmpty()) { physical(mc, action); return; }
    failures=0;
    var oldRules=AccessRules.copy(mc);String mode=mc.player.getClass().getMethod("power_mode").invoke(mc.player).toString();
    var main=mc.player.inventory.main.clone();var cursor=mc.player.inventory.getCursorStack();
    var keysField=Keyboard.class.getDeclaredField("keyDownBuffer");keysField.setAccessible(true);
    var keys=(ByteBuffer)keysField.get(null);byte shift=keys.get(42);
    var settings=SettingsRegistry.open(mc);
    try {
      mc.player.closeScreen();DeathChecks.alive(mc);CreativeVehicleChecks.mode(mc,"SURVIVAL");
      check(Display.isActive(),"focus the isolated test window before inventory checks");
      test("Shift-double-click defaults on and menu edits never move inventory items",()->{
        var row=find(settings,SETTING);check(row.defaultValue.getAsBoolean()&&row.value.getAsBoolean(),"default off");
        ItemStack before=mc.player.inventory.getCursorStack();row.parse("false");settings.preview(true);settings.discard();
        check(before==mc.player.inventory.getCursorStack(),"settings moved cursor");
      });
      keys.put(42,(byte)1);
      test("two real clicks transfer matching stacks from the clicked inventory and keep the held stack",()->{
        for(boolean fromPlayer:new boolean[]{true,false}) {
          var chest=new TestInventory(27,64);var screen=open(mc,chest);var c=screen.container;
          int start=fromPlayer?27:0;
          c.method_2084(start).setStack(item(4,7,0,"same"));c.method_2084(start+1).setStack(item(4,13,0,"same"));
          c.method_2084(start+2).setStack(item(4,11,0,"different"));c.method_2084(start+3).setStack(item(4,9,1,"same"));
          var held=item(4,5,0,"same");mc.player.inventory.setCursorStack(held);String before=DeathItems.encode(held);
          click(screen,start);check(c.method_2084(start).getStack()==null,"first native Shift-click did not move source");
          check(c.method_2084(start+1).getStack()!=null,"first click moved all matches");
          release(screen,start);
          ((ScreenInput)screen).power$key('\0',Keyboard.KEY_LSHIFT);
          click(screen,start);release(screen,start);
          check(c.method_2084(start+1).getStack()==null,"second click did not collect remaining stacks");
          check(c.method_2084(start+2).getStack().count==11&&c.method_2084(start+3).getStack().count==9,"different NBT/damage moved");
          check(total(c,4,0,"same")==20,"total count changed");
          check(mc.player.inventory.getCursorStack()==held&&before.equals(DeathItems.encode(held)),"cursor identity/data changed");
        }
      });
      test("full and partial containers retain leftovers and normal slot limits",()->{
        for(int room:new int[]{0,3,20}) {
          var chest=new TestInventory(9,16);var screen=open(mc,chest);var c=screen.container;
          for(int i=0;i<9;i++)chest.setStack(i,item(4,16,0,"same"));
          chest.getStack(0).count=16-Math.min(room,16);if(room>16)chest.getStack(1).count-=room-16;
          c.method_2084(9).setStack(item(4,7,0,"same"));c.method_2084(10).setStack(item(4,13,0,"same"));
          var held=item(4,5,0,"same");mc.player.inventory.setCursorStack(held);
          click(screen,9);release(screen,9);click(screen,9);release(screen,9);
          int remaining=0;for(int i=9;i<c.slots.size();i++)if(c.method_2084(i).getStack()!=null)remaining+=c.method_2084(i).getStack().count;
          check(remaining==20-room,"wrong remainder "+room+": "+remaining);
          for(int i=0;i<9;i++)check(chest.getStack(i).count<=16,"slot limit exceeded");
          check(total(c,4,0,"same")==164-room&&held.count==5,"lost items under capacity pressure");
        }
      });
      test("cursor identity changes and explicit cancellation prevent a stale second click",()->{
        for(boolean replace:new boolean[]{true,false}) {
          var screen=open(mc,new TestInventory(27,64));var c=screen.container;
          c.method_2084(27).setStack(item(4,7,0,"same"));c.method_2084(28).setStack(item(4,13,0,"same"));
          mc.player.inventory.setCursorStack(item(4,5,0,"same"));click(screen,27);release(screen,27);
          if(replace)mc.player.inventory.setCursorStack(item(4,5,0,"same"));else((InventoryDrag)screen).power$cancelItemDrag();
          click(screen,27);release(screen,27);check(c.method_2084(28).getStack().count==13,"cancelled gesture transferred remaining items");
        }
      });
      test("distinct custom names stay separate during native Shift-click insertion",()->{
        var chest=new TestInventory(9,64);var screen=open(mc,chest);var c=screen.container;
        ItemStack named=item(4,12,0,"same"),other=item(4,7,0,"same");
        named.getClass().getMethod("spc$setStr",String.class).invoke(named,"A");
        other.getClass().getMethod("spc$setStr",String.class).invoke(other,"B");
        chest.setStack(0,named);c.method_2084(9).setStack(other);
        c.onSlotClick(9,0,true,mc.player);
        check(chest.getStack(0).count==12&&chest.getStack(1).count==7,"different names merged");
        check("B".equals(chest.getStack(1).getClass().getMethod("spc$getStr").invoke(chest.getStack(1))),"split lost name");
      });
      test("furnace matching transfer respects input and fuel limits without borrowing the cursor",()->{
        for(int id:new int[]{15,263}) {
          clear(mc);mc.world.method_200(42,109,42,61);mc.player.method_1340(42.5,110.62,43.5);
          var furnace=(class_138)mc.world.method_1777(42,109,42);mc.setScreen(new class_524(mc.player.inventory,furnace));
          var screen=(ContainerScreen)mc.currentScreen;var c=screen.container;
          c.method_2084(3).setStack(item(id,45,0,"same"));c.method_2084(4).setStack(item(id,37,0,"same"));
          var held=item(id,5,0,"same");mc.player.inventory.setCursorStack(held);
          click(screen,3);release(screen,3);click(screen,3);release(screen,3);
          check(furnace.getStack(id==15?0:1).count==64&&c.method_2084(4).getStack().count==18,"furnace capacity mismatch");
          check(mc.player.inventory.getCursorStack()==held&&held.count==5,"furnace borrowed cursor");
        }
      });
      test("dispenser batches use native container ordering and retain held data",()->{
        clear(mc);mc.world.method_200(42,109,42,23);mc.player.method_1340(42.5,110.62,43.5);
        var dispenser=(class_137)mc.world.method_1777(42,109,42);mc.setScreen(new class_235(mc.player.inventory,dispenser));
        var screen=(ContainerScreen)mc.currentScreen;var c=screen.container;
        c.method_2084(9).setStack(item(4,45,0,"same"));c.method_2084(10).setStack(item(4,37,0,"same"));
        var held=item(4,5,0,"same");mc.player.inventory.setCursorStack(held);
        click(screen,9);release(screen,9);click(screen,9);release(screen,9);
        check(dispenser.getStack(0).count==64&&dispenser.getStack(1).count==18,"dispenser insertion mismatch");
        check(mc.player.inventory.getCursorStack()==held&&held.count==5,"dispenser changed cursor");
      });
      test("plain double-click gathering still works after Shift-double-click gestures",()->{
        keys.put(42,(byte)0);var screen=open(mc,new TestInventory(27,64));var c=screen.container;
        c.method_2084(0).setStack(item(4,7,0,"same"));c.method_2084(1).setStack(item(4,13,0,"same"));
        c.method_2084(2).setStack(item(4,11,0,"different"));
        click(screen,0);release(screen,0);click(screen,0);release(screen,0);
        check(mc.player.inventory.getCursorStack()!=null&&mc.player.inventory.getCursorStack().count==20,"normal gathering failed");
        check(c.method_2084(2).getStack().count==11,"gathering merged different native data");
      });
      test("disabled batches, changed sources and changed held data never sweep other slots",()->{
        for(String change:List.of("disabled","source","count","nbt","expired","container")) {
          keys.put(42,(byte)1);var screen=open(mc,new TestInventory(27,64));var c=screen.container;
          c.method_2084(27).setStack(item(4,7,0,"same"));c.method_2084(28).setStack(item(4,13,0,"same"));
          var held=item(4,5,0,"same");mc.player.inventory.setCursorStack(held);
          if(change.equals("disabled")){find(settings,SETTING).parse("false");settings.preview(true);}
          click(screen,27);release(screen,27);
          switch(change) {
            case "source" -> c.method_2084(27).setStack(item(3,1,0,"other"));
            case "count" -> held.count++;
            case "nbt" -> DeathChecks.tag(held).putString("changed","after first click");
            case "expired" -> Thread.sleep(270);
            case "container" -> mc.player.container=mc.player.playerContainer;
          }
          click(screen,27);release(screen,27);
          check(c.method_2084(28).getStack()!=null&&c.method_2084(28).getStack().count==13,"stale batch after "+change);
          mc.player.container=c;settings.discard();
        }
      });
      test("only native writable container slots can participate, including multiplayer boundaries",()->{
        var screen=open(mc,new TestInventory(27,64));
        Class<?> transfers=Class.forName("local.luke.power.inventory.QuickTransfers");
        Method eligible=transfers.getMethod("eligible",Minecraft.class,Container.class);
        Method writable=transfers.getMethod("writable",Slot.class,ItemStack.class);
        var held=item(4,5,0,"same");
        check(!(boolean)writable.invoke(null,new Slot(new TestInventory(1,64),0,0,0){},held),"custom slot admitted");
        for(String value:List.of("SURVIVAL","CREATIVE","SPECTATOR")) {
          CreativeVehicleChecks.mode(mc,value);
          check((boolean)eligible.invoke(null,mc,screen.container)==!value.equals("SPECTATOR"),"mode boundary "+value);
        }
        CreativeVehicleChecks.mode(mc,"SURVIVAL");mc.world.isRemote=true;
        try {check((boolean)eligible.invoke(null,mc,screen.container),"native chest packets unavailable");}
        finally {mc.world.isRemote=false;}
        clear(mc);mc.world.method_200(42,109,42,61);mc.player.method_1340(42.5,110.62,43.5);
        mc.setScreen(new class_524(mc.player.inventory,(class_138)mc.world.method_1777(42,109,42)));
        var furnace=((ContainerScreen)mc.currentScreen).container;
        check(!(boolean)writable.invoke(null,furnace.method_2084(2),held),"furnace output swept");
        mc.world.isRemote=true;
        try {check(!(boolean)eligible.invoke(null,mc,furnace),"vanilla server furnace batch admitted");}
        finally {mc.world.isRemote=false;}
        mc.player.closeScreen();mc.setScreen(new class_585(mc.player));
        check(!(boolean)eligible.invoke(null,mc,((ContainerScreen)mc.currentScreen).container),"player crafting/catalogue admitted");
      });
      test("ordinary left and right drags remain immediate and preserve counts and native data",()->{
        keys.put(42,(byte)0);var mouse=mouseButtons();
        try {
          for(int button:new int[]{0,1}) {
            var screen=open(mc,new TestInventory(27,64));var c=screen.container;
            var held=item(35,17,5,"drag");mc.player.inventory.setCursorStack(held);
            c.method_2084(2).setStack(item(35,11,5,"different"));
            mouse.put(button,(byte)1);var p=point(screen,0);((ScreenInput)screen).power$click(p[0],p[1],button);
            p=point(screen,1);((ScreenInput)screen).power$release(p[0],p[1],-1);
            check(c.method_2084(0).getStack()!=null&&c.method_2084(1).getStack()!=null,"drag waited for release "+button);
            p=point(screen,2);((ScreenInput)screen).power$release(p[0],p[1],-1);
            mouse.put(button,(byte)0);((ScreenInput)screen).power$release(p[0],p[1],button);
            check(c.method_2084(2).getStack().count==11,"drag replaced different native data");
            int count=total(c,35,5,"drag");var remainder=mc.player.inventory.getCursorStack();if(remainder!=null)count+=remainder.count;
            check(count==17,"drag lost or duplicated native stacks "+button+": "+count);
          }
        } finally {mouse.put(0,(byte)0);mouse.put(1,(byte)0);}
      });
      test("drag cancellation never overwrites changed cursor or visited native slot data",()->{
        keys.put(42,(byte)0);var mouse=mouseButtons();
        try {
          for(int button:new int[]{0,1})for(String change:List.of("cursor","slot","opposite")) {
            var screen=open(mc,new TestInventory(27,64));var c=screen.container;
            mc.player.inventory.setCursorStack(item(35,17,5,"drag"));
            mouse.put(button,(byte)1);var p=point(screen,0);((ScreenInput)screen).power$click(p[0],p[1],button);
            p=point(screen,1);((ScreenInput)screen).power$release(p[0],p[1],-1);
            if(change.equals("cursor")) mc.player.inventory.setCursorStack(item(264,3,0,"new cursor"));
            else if(change.equals("slot")) c.method_2084(0).setStack(item(264,9,0,"external slot"));
            String cursorBefore=encoded(mc.player.inventory.getCursorStack());
            String slotBefore=DeathItems.encode(c.method_2084(0).getStack());
            p=point(screen,2);
            if(change.equals("opposite")) {
              ((ScreenInput)screen).power$click(p[0],p[1],1-button);
              check(mc.player.inventory.getCursorStack().count==17&&c.method_2084(0).getStack()==null&&c.method_2084(1).getStack()==null,"owned rollback failed "+button);
            } else {
              ((ScreenInput)screen).power$release(p[0],p[1],-1);
              check(cursorBefore.equals(encoded(mc.player.inventory.getCursorStack())),"drag replaced changed cursor "+button);
              check(slotBefore.equals(DeathItems.encode(c.method_2084(0).getStack())),"drag replaced changed slot "+button);
              check(c.method_2084(2).getStack()==null,"cancelled drag restarted "+button);
            }
            mouse.put(button,(byte)0);((ScreenInput)screen).power$release(p[0],p[1],button);
          }
        } finally {mouse.put(0,(byte)0);mouse.put(1,(byte)0);}
      });
      test("dragging skips full and oversized native stacks without changing their counts",()->{
        keys.put(42,(byte)0);var mouse=mouseButtons();
        try {
          for(int button:new int[]{0,1}) {
            var screen=open(mc,new TestInventory(27,16));var c=screen.container;
            mc.player.inventory.setCursorStack(item(35,17,5,"drag"));
            c.method_2084(1).setStack(item(35,70,5,"drag"));
            mouse.put(button,(byte)1);var p=point(screen,0);((ScreenInput)screen).power$click(p[0],p[1],button);
            for(int i:new int[]{1,2}) {p=point(screen,i);((ScreenInput)screen).power$release(p[0],p[1],-1);}
            mouse.put(button,(byte)0);((ScreenInput)screen).power$release(p[0],p[1],button);
            check(c.method_2084(1).getStack().count==70,"oversized stack altered "+button);
            int count=total(c,35,5,"drag");var remaining=mc.player.inventory.getCursorStack();if(remaining!=null)count+=remaining.count;
            check(count==87,"oversized drag lost items "+button+": "+count);
          }
        } finally {mouse.put(0,(byte)0);mouse.put(1,(byte)0);}
      });
    } finally {
      keys.put(42,shift);mc.player.inventory.setCursorStack(null);mc.player.closeScreen();settings.discard();
      System.arraycopy(main,0,mc.player.inventory.main,0,main.length);mc.player.inventory.setCursorStack(cursor);
      AccessRules.restore(mc,oldRules);CreativeVehicleChecks.mode(mc,mode);mc.setScreen(null);
    }
    log("QUICK TRANSFER FAILURES "+failures);
  }
  private static void physical(Minecraft mc,String action)throws Exception {
    if(action.startsWith("setup ")) {
      String[] args=action.split(" ");
      DeathChecks.alive(mc);CreativeVehicleChecks.mode(mc,args[2]);
      clear(mc);
      for(int x=47;x<=51;x++)for(int z=47;z<=51;z++) {
        mc.world.method_154(x,108,z,1,0);
        for(int y=109;y<=112;y++)if(x!=48||y!=109||z!=48)mc.world.method_154(x,y,z,0,0);
      }
      mc.world.method_154(48,109,48,54,0);mc.player.field_1640=0;
      mc.player.method_1341(48.5,109,50.5,180,0);
      mc.player.velocityX=mc.player.velocityY=mc.player.velocityZ=0;
      mc.player.method_486((Inventory)mc.world.method_1777(48,109,48));
      physicalScreen=(ContainerScreen)mc.currentScreen;
      for(int i=0;i<27;i++)physicalScreen.container.method_2084(i).setStack(null);
      physicalOrigin=args[1].equals("player")?27:0;
      var c=physicalScreen.container;
      c.method_2084(physicalOrigin).setStack(item(4,7,0,"same"));
      c.method_2084(physicalOrigin+1).setStack(item(4,13,0,"same"));
      c.method_2084(physicalOrigin+2).setStack(item(4,11,0,"different"));
      c.method_2084(physicalOrigin+3).setStack(item(4,9,1,"same"));
      physicalCursor=item(4,5,0,"same");physicalCursorData=DeathItems.encode(physicalCursor);
      mc.player.inventory.setCursorStack(physicalCursor);
    } else if(action.equals("replace-cursor")) {
      physicalCursor=item(4,5,0,"same");mc.player.inventory.setCursorStack(physicalCursor);
    } else if(action.equals("close")) {
      mc.player.closeScreen();mc.setScreen(null);physicalScreen=null;return;
    } else if(!action.equals("state")) throw new IllegalArgumentException(action);
    check(physicalScreen!=null,"physical setup missing");
    var c=physicalScreen.container;var out=new com.google.gson.JsonObject();
    var p=point(physicalScreen,physicalOrigin);
    out.addProperty("x",p[0]*mc.displayWidth/physicalScreen.width);
    out.addProperty("y",p[1]*mc.displayHeight/physicalScreen.height);
    var slots=new com.google.gson.JsonArray();
    for(int i=0;i<4;i++){var stack=c.method_2084(physicalOrigin+i).getStack();slots.add(stack==null?0:stack.count);}
    out.add("source",slots);out.addProperty("total",total(c,4,0,"same"));
    var cursor=mc.player.inventory.getCursorStack();
    out.addProperty("cursorUnchanged",cursor==physicalCursor&&physicalCursorData.equals(DeathItems.encode(cursor)));
    out.addProperty("focused",Display.isActive());
    out.addProperty("screenOpen",mc.currentScreen==physicalScreen);
    out.addProperty("playerY",mc.player.y);out.addProperty("feet",mc.player.boundingBox.minY);
    out.addProperty("floor",mc.world.getBlockId(48,108,50));
    var gestureField=ContainerScreen.class.getDeclaredField("powerInventory$shiftDouble");gestureField.setAccessible(true);
    Object gesture=gestureField.get(physicalScreen);
    out.addProperty("pending",gesture.getClass().getMethod("pending").invoke(gesture).equals(true));
    out.addProperty("released",(boolean)UiChecks.field(gesture,"released"));
    out.addProperty("shift",Keyboard.isKeyDown(42));
    java.nio.file.Files.writeString(java.nio.file.Path.of("power-beta-transfer-state.json"),out.toString());
    log("TRANSFER STATE "+out);
  }
  private static Setting find(ConfigSession s,String id) {return s.settings().stream().filter(v->v.id.equals(id)).findFirst().orElseThrow();}
  private static String encoded(ItemStack stack)throws Exception{return stack==null?"empty":DeathItems.encode(stack);}
  private static ItemStack item(int id,int count,int damage,String name)throws Exception{return DeathChecks.tagged(id,count,damage,name);}
  private static void clear(Minecraft mc) {mc.player.inventory.setCursorStack(null);mc.player.closeScreen();Arrays.fill(mc.player.inventory.main,null);}
  private static ContainerScreen open(Minecraft mc,Inventory inventory){clear(mc);var screen=new class_264(mc.player.inventory,inventory);mc.setScreen(screen);return screen;}
  private static int total(Container c,int id,int damage,String name)throws Exception{
    var probe=item(id,1,damage,name);var same=Class.forName("local.luke.power.inventory.InventoryStacks").getMethod("same",ItemStack.class,ItemStack.class);int count=0;
    for(Object entry:c.slots){var stack=((Slot)entry).getStack();if(stack!=null&&(boolean)same.invoke(null,probe,stack))count+=stack.count;}return count;
  }
  private static int[] point(ContainerScreen screen,int index)throws Exception{
    var geometry=(local.luke.power.validation.mixin.ContainerInput)screen;
    var slot=screen.container.method_2084(index);
    return new int[]{(screen.width-geometry.power$width())/2+slot.x+8,(screen.height-geometry.power$height())/2+slot.y+8};
  }
  private static void click(ContainerScreen screen,int index)throws Exception{var p=point(screen,index);((ScreenInput)screen).power$click(p[0],p[1],0);}
  private static void release(ContainerScreen screen,int index)throws Exception{var p=point(screen,index);((ScreenInput)screen).power$release(p[0],p[1],0);}
  private static ByteBuffer mouseButtons()throws Exception{var f=org.lwjgl.input.Mouse.class.getDeclaredField("buttons");f.setAccessible(true);return (ByteBuffer)f.get(null);}
  private static final class TestInventory implements Inventory {
    final ItemStack[] stacks;final int limit;
    TestInventory(int size,int limit){stacks=new ItemStack[size];this.limit=limit;}
    public int size(){return stacks.length;}public ItemStack getStack(int i){return stacks[i];}
    public ItemStack removeStack(int i,int count){var stack=stacks[i];if(stack==null)return null;if(count>=stack.count){stacks[i]=null;return stack;}return stack.split(count);}
    public void setStack(int i,ItemStack stack){stacks[i]=stack;}public String getName(){return "Transfer fixture";}
    public int getMaxCountPerStack(){return limit;}public void markDirty(){}public boolean canPlayerUse(PlayerEntity p){return true;}
  }
}
