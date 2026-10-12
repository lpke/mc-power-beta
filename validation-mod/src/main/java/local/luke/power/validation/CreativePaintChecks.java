package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.*;
import local.luke.power.permissions.CheatWorld;
import local.luke.power.validation.mixin.ScreenInput;
import local.luke.power.validation.mixin.ContainerInput;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.input.Mouse;

/** Exercise installed container hooks, native slot rules and StationAPI item data. */
final class CreativePaintChecks {
  private static final String TAG = "creative-paint-check";
  private static Minecraft client;
  private static ContainerScreen physical;

  static void run(Minecraft mc, String action) throws Exception {
    client = mc;
    check(mc.world != null && !mc.world.isRemote, "run new-world first");
    if (action.equals("saved")) {
      stack(mc.player.inventory.main[7], 35, 64, 5, "saved-inventory");
      stack(((Inventory)mc.world.method_1777(3, 101, 0)).getStack(0), 267, 1, 83, "saved-chest");
      log("PASS creative-painted counts, damage and NBT survived restart"); return;
    }
    if (action.equals("setup")) {
      ((CheatWorld)mc.world.method_262()).power$cheatsEnabled(true);
      CreativeVehicleChecks.mode(mc, "CREATIVE"); physical = player(true, 320, 225);
      mc.setScreen(physical);
      mc.player.inventory.setCursorStack(tagged(35, 7, 5, "physical"));
      log("PHYSICAL PAINT main slots 9/10/11, GUI " + physical.width + "x" + physical.height
          + ", origin " + (physical.width-176)/2 + "," + (physical.height-166)/2); return;
    }
    if (action.equals("physical-check") || action.equals("focus-check") || action.equals("physical-preview")) {
      for (int id : new int[] {9, 10, 11}) {
        ItemStack item = physical.container.method_2084(id).getStack();
        if (action.equals("focus-check") || action.equals("physical-preview")) check(item == null, "preview/cancellation wrote inventory");
        else stack(item, 35, 64, 5, "physical");
        if(action.equals("physical-preview"))stack(preview(physical,id),35,64,5,"physical");
      }
      stack(mc.player.inventory.getCursorStack(), 35, 7, 5, "physical");
      log("PASS " + action + " preserves the cursor and expected native slot contents"); return;
    }
    failures = 0;
    java.nio.file.Files.createDirectories(Path.of("power-beta-data/reports/creative-paint"));
    mc.player.closeScreen();
    var inventory = mc.player.inventory.main.clone(); var armor = mc.player.inventory.armor.clone();
    var properties = (CheatWorld)mc.world.method_262(); boolean cheats = properties.power$cheatsEnabled();
    String mode = Class.forName("local.luke.power.creative.api.ModePlayer").getMethod("power_mode").invoke(mc.player).toString();
    byte previous = buttons().get(2);
    var settings=local.luke.power.config.SettingsRegistry.open(mc);
    try {
      Release110Checks.find(settings,"power_compat:config.STACK_SIZE_CONFIG.stackableEmptyBuckets").parse("false");
      settings.preview(true);
      properties.power$cheatsEnabled(true); CreativeVehicleChecks.mode(mc, "CREATIVE");
      mc.player.method_1340(.5, 103, -2.5);
      for (int x=-4;x<=4;x++) for (int z=-4;z<=4;z++) {
        mc.world.method_200(x,100,z,1); for(int y=101;y<106;y++)mc.world.method_200(x,y,z,0);
      }
      if (action.equals("rollback")) { rollback(); return; }
      for(int[] size:new int[][]{{320,240},{640,420}}) {
        test("catalogue previews are read-only; normal limits and hidden slots, width " + size[0], () -> {
          for(int[] item:new int[][]{{35,64,5},{332,16,0},{267,1,83},{325,1,0},{326,1,0},{327,1,0},{335,1,0}}) {
            var screen=player(false,size[0],size[1]);
            ItemStack cursor=tagged(item[0],1,item[2],"catalogue"); mc.player.inventory.setCursorStack(cursor);
            start(screen,36); hover(screen,9); hover(screen,1); hover(screen,5); hover(screen,36); hover(screen,37);
            stack(preview(screen,36),item[0],item[1],item[2],"catalogue");
            check(screen.container.method_2084(36).getStack()==null,"hover wrote inventory");
            for(int frame=0;frame<3;frame++) SettingsSnapshot.render(mc,screen,size[0],size[1],
                Path.of("power-beta-data/reports/creative-paint/preview-"+size[0]+"-"+item[0]+".png"));
            check(screen.container.method_2084(36).getStack()==null,"render wrote inventory"); finish(screen);
            for(int id:new int[]{36,37})stack(screen.container.method_2084(id).getStack(),item[0],item[1],item[2],"catalogue");
            for(int id:new int[]{9,1,5})check(screen.container.method_2084(id).getStack()==null,"painted hidden slot");
            check(mc.player.inventory.getCursorStack()==cursor,"cursor replaced");
            stack(cursor,item[0],1,item[2],"catalogue");
          }
        });
        test("normal inventory tops up matches and skips different items, damage, NBT and outputs, width " + size[0], () -> {
          var screen=player(true,size[0],size[1]); var c=screen.container;
          c.method_2084(9).setStack(tagged(35,7,5,"match"));
          c.method_2084(10).setStack(tagged(35,7,5,"other"));
          c.method_2084(11).setStack(tagged(35,7,4,"match"));
          c.method_2084(12).setStack(tagged(4,7,0,"match"));
          c.method_2084(13).setStack(tagged(35,65,5,"match"));
          c.method_2084(14).setStack(tagged(35,64,5,"match"));
          ItemStack cursor=tagged(35,7,5,"match"); mc.player.inventory.setCursorStack(cursor);
          start(screen,9); for(int id:new int[]{9,10,11,12,13,14,15,36,0,5})hover(screen,id);
          finish(screen);
          for(int id:new int[]{9,14,15,36})stack(c.method_2084(id).getStack(),35,64,5,"match");
          stack(c.method_2084(10).getStack(),35,7,5,"other"); stack(c.method_2084(11).getStack(),35,7,4,"match");
          stack(c.method_2084(12).getStack(),4,7,0,"match"); stack(c.method_2084(13).getStack(),35,65,5,"match");
          check(c.method_2084(0).getStack()==null && c.method_2084(5).getStack()==null,"filled output/armor with wool");
          check(mc.player.inventory.getCursorStack()==cursor,"cursor identity changed"); stack(cursor,35,7,5,"match");
          tag(c.method_2084(15).getStack()).putString(TAG,"changed");
          stack(c.method_2084(9).getStack(),35,64,5,"match"); stack(cursor,35,7,5,"match");
        });
      }
      test("live bucket preference paints 16 empty buckets; filled buckets remain one", () -> {
        var session=local.luke.power.config.SettingsRegistry.open(mc);
        var row=Release110Checks.find(session,"power_compat:config.STACK_SIZE_CONFIG.stackableEmptyBuckets");
        try {
          row.parse("true"); session.preview(true);
          var screen=player(true,320,240); mc.player.inventory.setCursorStack(new ItemStack(325,1,0));
          start(screen,9); hover(screen,9); hover(screen,10); finish(screen);
          check(screen.container.method_2084(9).getStack().count==16 && screen.container.method_2084(10).getStack().count==16,"bucket limit ignored");
        } finally {session.discard();}
      });
      test("armor respects acceptance and its one-item slot limit", () -> {
        var screen=player(true,320,240); mc.player.inventory.setCursorStack(tagged(310,1,19,"helmet"));
        start(screen,5); hover(screen,5); hover(screen,6); hover(screen,9); finish(screen);
        stack(screen.container.method_2084(5).getStack(),310,1,19,"helmet");
        check(screen.container.method_2084(6).getStack()==null,"helmet filled chest armor slot");
      });
      test("chest, crafting table, furnace and dispenser use native limits and output restrictions", () -> {
        for(int block:new int[]{54,58,61,23}) {
          var screen=container(block,0); var c=screen.container;
          mc.player.inventory.setCursorStack(tagged(4,1,0,"container-"+block)); start(screen,c.slots.size()-1);
          for(Object slot:c.slots)hover(screen,((Slot)slot).id);
          finish(screen);
          for(Object value:c.slots){Slot slot=(Slot)value;
            if(slot.canInsert(new ItemStack(4,1,0)))stack(slot.getStack(),4,Math.min(64,slot.getMaxItemCount()),0,"container-"+block);
            else check(slot.getStack()==null,"painted restricted output "+block+"/"+slot.id);
          }
        }
      });
      test("cancellation preserves slots after cursor changes, resize, screen closure and other buttons", () -> {
        for(String cancel:new String[]{"count","nbt","replace","resize","close","button","release","container","screen","world"}) {
          var screen=player(true,320,240); ItemStack cursor=tagged(35,7,5,"cancel"); mc.player.inventory.setCursorStack(cursor);
          var world=mc.world;
          start(screen,9); hover(screen,9);
          switch(cancel) {
            case "count" -> cursor.count--;
            case "nbt" -> tag(cursor).putString(TAG,"changed");
            case "replace" -> mc.player.inventory.setCursorStack(cursor.clone());
            case "resize" -> screen.init(mc,640,420);
            case "close" -> screen.removed();
            case "button" -> ((ScreenInput)screen).power$click(point(screen,9)[0],point(screen,9)[1],0);
            case "release" -> ((ScreenInput)screen).power$release(0,0,1);
            case "container" -> mc.player.container=null;
            case "screen" -> mc.currentScreen=null;
            case "world" -> mc.world=null;
          }
          finish(screen); check(screen.container.method_2084(9).getStack()==null,"cancel wrote slot: "+cancel);
          mc.world=world;
          if(cancel.equals("container"))mc.player.container=screen.container;
        }
      });
      test("slot contents and ownership are rechecked before a release commits", () -> {
        var screen=player(true,320,240);var c=screen.container;
        mc.player.inventory.setCursorStack(tagged(35,1,5,"recheck"));start(screen,9);hover(screen,9);hover(screen,10);
        c.method_2084(9).setStack(tagged(264,3,0,"changed"));
        Slot original=c.method_2084(10);Slot replacement=new Slot(mc.player.inventory,10,original.x,original.y);replacement.id=10;
        c.slots.set(10,replacement);
        try {finish(screen);stack(c.method_2084(9).getStack(),264,3,0,"changed");check(replacement.getStack()==null,"stale slot ownership committed");}
        finally {c.slots.set(10,original);}
      });
      test("survival, spectator, cheats, remote and dead-player changes cancel without writes", () -> {
        for(String reason:new String[]{"SURVIVAL","SPECTATOR","cheats","remote","dead"}) {
          CreativeVehicleChecks.mode(mc,"CREATIVE"); properties.power$cheatsEnabled(true);
          var screen=player(true,320,240); mc.player.inventory.setCursorStack(tagged(35,7,5,"guard"));
          start(screen,9); hover(screen,9); int health=mc.player.health;
          if(reason.equals("cheats"))properties.power$cheatsEnabled(false);
          else if(reason.equals("remote"))mc.world.isRemote=true;
          else if(reason.equals("dead"))mc.player.health=0;
          else CreativeVehicleChecks.mode(mc,reason);
          try {finish(screen);check(screen.container.method_2084(9).getStack()==null,"guard wrote slot: "+reason);}
          finally {mc.world.isRemote=false;mc.player.health=health;properties.power$cheatsEnabled(true);}
        }
        CreativeVehicleChecks.mode(mc,"CREATIVE");
      });
      test("ordinary left/right clicks still pick up, split and place after a creative drag", () -> {
        var screen=player(true,320,240); mc.player.inventory.setCursorStack(new ItemStack(4,1,0));
        start(screen,9);hover(screen,9);finish(screen);mc.player.inventory.setCursorStack(null);
        check(screen.container.method_2084(9).getStack()!=null,"paint before right pickup missing");
        click(screen,9,1);check(screen.container.method_2084(9).getStack()!=null && mc.player.inventory.getCursorStack()!=null,
            "right pickup slot="+screen.container.method_2084(9).getStack()+" cursor="+mc.player.inventory.getCursorStack());
        check(screen.container.method_2084(9).getStack().count==32 && mc.player.inventory.getCursorStack().count==32,"right pickup counts changed");
        ((ScreenInput)screen).power$release(0,0,1);click(screen,10,0);((ScreenInput)screen).power$release(0,0,0);
        check(screen.container.method_2084(10).getStack()!=null && screen.container.method_2084(10).getStack().count==32 && mc.player.inventory.getCursorStack()==null,"left placement changed");
      });
      test("switching from right-drag to middle-drag preserves counts in both release orders", () -> {
        for(boolean middleFirst:new boolean[]{false,true}) {
          var screen=player(true,320,240);mc.player.inventory.setCursorStack(new ItemStack(4,20,0));
          buttons().put(1,(byte)1);click(screen,9,1);
          int[] p=point(screen,10);((ScreenInput)screen).power$release(p[0],p[1],-1);
          int before=total(screen);start(screen,12);hover(screen,12);
          p=point(screen,12);
          if(middleFirst)finish(screen);
          buttons().put(1,(byte)0);((ScreenInput)screen).power$release(p[0],p[1],1);
          if(!middleFirst)finish(screen);
          check(total(screen)==before+(middleFirst?64:0),"release order changed existing items");
        }
      });
      test("a mouse-bound inventory key closes normally and cancels a pending paint", () -> {
        var screen=player(true,320,240);mc.player.inventory.setCursorStack(new ItemStack(4,7,0));
        start(screen,9);hover(screen,9);int code=mc.options.inventoryKey.code;
        try {mc.options.inventoryKey.code=-98;click(screen,9,2);
          check(mc.currentScreen==null && screen.container.method_2084(9).getStack()==null,"binding did not close/cancel");
        } finally {mc.options.inventoryKey.code=code;}
      });
    } finally {
      buttons().put(2,previous); mc.world.isRemote=false; mc.player.inventory.setCursorStack(null); mc.player.closeScreen();
      System.arraycopy(inventory,0,mc.player.inventory.main,0,inventory.length);
      System.arraycopy(armor,0,mc.player.inventory.armor,0,armor.length);
      CreativeVehicleChecks.mode(mc,mode); properties.power$cheatsEnabled(cheats);
      settings.discard();
    }
    if(failures==0) {
      properties.power$cheatsEnabled(true); CreativeVehicleChecks.mode(mc,"CREATIVE");
      var screen=container(54,3); mc.player.inventory.setCursorStack(tagged(267,1,83,"saved-chest"));
      start(screen,0);hover(screen,0);finish(screen);
      mc.player.inventory.setCursorStack(tagged(35,1,5,"saved-inventory"));
      int saved=screen.container.slots.size()-2;start(screen,saved);hover(screen,saved);finish(screen);
      mc.player.inventory.setCursorStack(null);mc.player.closeScreen();
      CreativeVehicleChecks.mode(mc,mode); properties.power$cheatsEnabled(cheats);
    }
    log("CREATIVE PAINT FAILURES " + failures);
  }

  private static class_585 player(boolean normal,int width,int height) throws Exception {
    client.player.inventory.setCursorStack(null);client.player.closeScreen();
    Arrays.fill(client.player.inventory.main,null);Arrays.fill(client.player.inventory.armor,null);
    var screen=new class_585(client.player);client.setScreen(screen);screen.init(client,width,height);
    field(screen,"creative_normalGUI",normal);return screen;
  }
  private static ContainerScreen container(int block,int x) {
    client.player.inventory.setCursorStack(null);client.player.closeScreen();Arrays.fill(client.player.inventory.main,null);
    client.world.method_200(x,101,0,block);client.player.method_1340(x+.5,103,-2.5);
    var b=(Inventory)client.world.method_1777(x,101,0);
    if(b!=null)for(int i=0;i<b.size();i++)b.setStack(i,null);
    net.minecraft.block.Block.BLOCKS[block].method_1608(client.world,x,101,0,client.player);
    check(client.currentScreen instanceof ContainerScreen,"container did not open: "+block);
    return (ContainerScreen)client.currentScreen;
  }
  private static int[] point(ContainerScreen screen,int id) throws Exception {
    Slot slot=screen.container.method_2084(id);
    var dimensions=(ContainerInput)screen;
    return new int[]{(screen.width-dimensions.power$width())/2+slot.x+8,
        (screen.height-dimensions.power$height())/2+slot.y+8};
  }
  private static void click(ContainerScreen screen,int id,int button) throws Exception {
    int[] p=point(screen,id);((ScreenInput)screen).power$click(p[0],p[1],button);
  }
  private static void start(ContainerScreen screen,int id) throws Exception {buttons().put(2,(byte)1);click(screen,id,2);}
  private static void hover(ContainerScreen screen,int id) throws Exception {
    int[] p=point(screen,id);((ScreenInput)screen).power$release(p[0],p[1],-1);
  }
  private static void finish(ContainerScreen screen) throws Exception {buttons().put(2,(byte)0);((ScreenInput)screen).power$release(0,0,2);}
  private static ByteBuffer buttons() throws Exception {var f=Mouse.class.getDeclaredField("buttons");f.setAccessible(true);return (ByteBuffer)f.get(null);}
  private static ItemStack preview(ContainerScreen screen,int id) throws Exception {
    Slot slot=screen.container.method_2084(id);
    return (ItemStack)screen.getClass().getMethod("power$creativePaintPreview",Slot.class,ItemStack.class).invoke(screen,slot,slot.getStack());
  }
  private static NbtCompound tag(ItemStack item) throws Exception {return (NbtCompound)item.getClass().getMethod("getStationNbt").invoke(item);}
  private static ItemStack tagged(int id,int count,int damage,String name) throws Exception {
    var item=new ItemStack(id,count,damage);tag(item).putString(TAG,name);return item;
  }
  private static void stack(ItemStack item,int id,int count,int damage,String name) throws Exception {
    check(item!=null && item.itemId==id && item.count==count && item.getDamage()==damage
        && tag(item).getString(TAG).equals(name),"count/damage/NBT changed: "+name+" "+item);
  }
  private static int total(ContainerScreen screen) {
    int total=client.player.inventory.getCursorStack()==null?0:client.player.inventory.getCursorStack().count;
    for(Object value:screen.container.slots){ItemStack item=((Slot)value).getStack();if(item!=null)total+=item.count;}
    return total;
  }
  private static void rollback() throws Exception {
    var screen=player(true,320,240);var c=screen.container;
    c.method_2084(9).setStack(tagged(35,7,5,"rollback"));
    Slot original=c.method_2084(10);Slot faulty=new Slot(client.player.inventory,10,original.x,original.y){
      boolean first=true;
      @Override public void setStack(ItemStack stack){super.setStack(stack);if(first){first=false;throw new IllegalStateException("Injected creative paint rollback check");}}
    };
    faulty.id=10;c.slots.set(10,faulty);
    try {
      client.player.inventory.setCursorStack(tagged(35,1,5,"rollback"));start(screen,9);hover(screen,9);hover(screen,10);finish(screen);
      stack(c.method_2084(9).getStack(),35,7,5,"rollback");check(faulty.getStack()==null,"partial transaction survived");
      stack(client.player.inventory.getCursorStack(),35,1,5,"rollback");
      check(!((Boolean)screen.getClass().getMethod("power$creativePaintClick",int.class,int.class,int.class).invoke(screen,0,0,2)),"failed feature still active");
      log("PASS creative drag restores a partial write and disables itself until restart");
    } finally {c.slots.set(10,original);}
  }
}
