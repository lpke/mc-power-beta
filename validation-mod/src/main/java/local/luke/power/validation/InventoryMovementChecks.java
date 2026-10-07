package local.luke.power.validation;

import com.google.gson.JsonObject;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.autowalk.*;
import local.luke.power.building.config.Config;
import local.luke.power.config.*;
import local.luke.power.input.*;
import local.luke.power.ui.PowerOptionsScreen;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import static local.luke.power.validation.Validation.*;

/** Disposable world plus physical XTest key presses; never installed in user instances. */
public final class InventoryMovementChecks {
  public static void run(Minecraft mc,String action) throws Exception {
    if (action.equals("keys")) {
      test("Linux backslash and F13–F24 map to usable key codes",()->{
        Class<?> type=Class.forName("org.lwjgl.opengl.LinuxKeycodes");
        Method map=type.getDeclaredMethod("mapKeySymToLWJGLKeyCode",long.class);map.setAccessible(true);
        for(long symbol:new long[]{'\\','|',0xffca,0xffcc,0xffcd,0xffcf,0xffd0,0xffd1,0xffd5}) {
          int code=(int)map.invoke(null,symbol);
          check(code==ExtendedKeys.linux(symbol),"incorrect keysym "+symbol+" -> "+code);
          check(Keyboard.getKeyName(code)!=null,"missing key name "+code);
          check(Keyboard.getKeyIndex(Keyboard.getKeyName(code))==code,"name/code round trip");
        }
      });
      test("unknown keys cannot clear captured bindings",()->{
        PowerOptionsScreen screen=new PowerOptionsScreen(null);mc.setScreen(screen);
        Setting s=find(screen.session(),"keys.key.inventory");
        var before=s.value.deepCopy();
        Field field=PowerOptionsScreen.class.getDeclaredField("capture");field.setAccessible(true);field.set(screen,s);
        ((ScreenInput)(Object)screen).power$key('\0',0);
        check(before.equals(s.value)&&field.get(screen)==s,"unknown key cleared binding");
        ((ScreenInput)(Object)screen).power$key('\0',1);mc.setScreen(null);
      });
      return;
    }
    if(action.equals("setup")) {
      mc.setScreen(null);((local.luke.power.world.WorldDifficulty) mc.world.method_262()).power$difficulty(0);mc.world.field_213=0;
      for(int x=-15;x<=15;x++)for(int z=-15;z<=15;z++) {
        mc.world.method_200(x,100,z,1);
        for(int y=101;y<=105;y++)mc.world.method_200(x,y,z,0);
      }
      mc.player.method_1340(.5,103,-3.5);mc.player.yaw=0;mc.player.pitch=0;
      var s=Config.current().copy();s.inventoryWhileMoving=true;s.autoWalk=true;Config.preview(s);
      Bindings.configure(Map.of());mc.options.forwardKey.code=17;mc.options.backKey.code=31;
      mc.options.leftKey.code=30;mc.options.rightKey.code=32;mc.options.jumpKey.code=57;mc.options.sneakKey.code=42;
      AutoWalk.KEY.code=19;
      Bindings.register(mc.options.allKeys);
    } else if(action.equals("inventory")) mc.setScreen(new class_585(mc.player));
    else if(action.equals("shift-click")) {
      test("Shift-click transfers one whole stack without sneaking or losing durability",()->{
        mc.player.method_1340(.5,103,-2.5);mc.world.method_200(0,101,0,54);
        Inventory chest=(Inventory)mc.world.method_1777(0,101,0);
        for(int i=0;i<mc.player.inventory.main.length;i++)mc.player.inventory.main[i]=null;
        ItemStack stack=new ItemStack(267,1,37);chest.setStack(0,stack);
        mc.player.method_486(chest);ContainerScreen screen=(ContainerScreen)mc.currentScreen;
        var slot=(net.minecraft.screen.slot.Slot)screen.container.slots.get(0);
        ((ScreenInput)screen).power$click((screen.width-176)/2+slot.x+8,(screen.height-168)/2+slot.y+8,0);
        check(chest.getStack(0)==null,"Shift-click did not move stack");
        int count=0;for(ItemStack s:mc.player.inventory.main)if(s!=null&&s.itemId==267&&s.getDamage()==37)count+=s.count;
        check(count==1&&mc.player.inventory.getCursorStack()==null,"Shift-click lost or duplicated stack");
        check(!mc.player.field_161.field_2536,"Shift-click caused sneaking");mc.player.closeScreen();
      });
    }
    else if(action.equals("chest")) {
      mc.player.method_1340(.5,103,-3.5);mc.world.method_200(0,101,0,54);
      mc.player.method_486((Inventory)mc.world.method_1777(0,101,0));
    } else if(action.equals("close")) { if(mc.currentScreen instanceof ContainerScreen) mc.player.closeScreen();else mc.setScreen(null); }
    else if(action.equals("off")) { var s=Config.current().copy();s.inventoryWhileMoving=false;Config.preview(s); }
    else if(action.equals("on")) { var s=Config.current().copy();s.inventoryWhileMoving=true;Config.preview(s); }
    else if(action.equals("checks")) checks(mc);
    else if(action.equals("state")) {
      JsonObject s=new JsonObject();s.addProperty("screen",mc.currentScreen==null?"none":mc.currentScreen.getClass().getName());
      s.addProperty("allowed",MovementScreens.allows(mc.currentScreen));s.addProperty("walking",AutoWalk.isWalking());
      s.addProperty("forward",mc.player.field_161.field_2533);s.addProperty("side",mc.player.field_161.field_2532);
      s.addProperty("jump",mc.player.field_161.field_2535);s.addProperty("sneak",mc.player.field_161.field_2536);
      s.addProperty("x",mc.player.x);s.addProperty("y",mc.player.y);s.addProperty("z",mc.player.z);
      s.addProperty("backslash",Bindings.physical(43));s.addProperty("f15",Bindings.physical(102));s.addProperty("f20",Bindings.physical(114));
      s.addProperty("fullscreen",org.lwjgl.opengl.Display.isFullscreen());
      Files.writeString(Path.of("power-beta-movement-state.json"),s.toString());
    }
  }

  private static void checks(Minecraft mc) throws Exception {
    test("inventory tweak defaults off and is exposed with help",()->{
      check(!new local.luke.power.building.config.Settings().inventoryWhileMoving,"wrong default");
      Setting s=find(SettingsRegistry.open(mc),"tweaks.inventoryWhileMoving");
      check(s.page.equals("Inventory")&&!s.description.isBlank(),"missing setting/help");
      Setting light=find(SettingsRegistry.open(mc),"lightOverlay.checksPerTick");
      check(light.defaultValue.getAsInt()==2048,"changed scan default");
    });
    test("unsupported screens, disabled feature and multiplayer reject movement",()->{
      check(!InventoryMovement.supports(new net.minecraft.client.gui.screen.ChatScreen()),"chat allows movement");
      check(!InventoryMovement.supports(new class_585(mc.player){}),"unknown text-capable subclass allowed");
      var saved=Config.current().copy();var s=saved.copy();s.inventoryWhileMoving=false;Config.preview(s);
      check(!InventoryMovement.supports(new class_585(mc.player)),"disabled still supported");Config.preview(saved);
      boolean remote=mc.world.isRemote;mc.world.isRemote=true;
      try {check(!InventoryMovement.allows(mc,new class_585(mc.player)),"multiplayer enabled");}
      finally {mc.world.isRemote=remote;}
    });
    for(int block:new int[]{54,61,23,58}) test("reach closure preserves cursor and inventory for block "+block,()->{
      AutoWalk.stop();if(mc.currentScreen instanceof ContainerScreen)mc.player.closeScreen();
      mc.world.method_200(0,101,0,block);mc.player.method_1340(.5,103,-2.5);
      Inventory target=mc.world.method_1777(0,101,0) instanceof Inventory i?i:null;
      ItemStack stored=new ItemStack(267,1,42),cursor=new ItemStack(264,17,0);
      if(target!=null) target.setStack(0,stored);
      if(block==54)mc.player.method_486(target);
      if(block==61)mc.player.method_487((class_138)target);
      if(block==23)mc.player.method_485((class_137)target);
      if(block==58)mc.player.method_484(0,101,0);
      Screen screen=mc.currentScreen;InventoryMovement.closeOutOfReach(mc);check(mc.currentScreen==screen,"closed in reach");
      mc.player.inventory.setCursorStack(cursor);
      ItemStack craft=new ItemStack(4,11,0);
      if(block==58)((class_196)((ContainerScreen)screen).container).field_708.setStack(0,craft);
      mc.player.method_1340(7.5,103,-2.5);
      InventoryMovement.closeOutOfReach(mc);
      check(mc.currentScreen==null,"not closed beyond reach");
      check(mc.player.container==mc.player.playerContainer,"handler not reset");
      if(target!=null)check(target.getStack(0)==stored&&stored.count==1&&stored.getDamage()==42,"container changed");
      check(countReference(mc,cursor)==1&&cursor.count==17,"cursor lost or duplicated");
      if(block==58)check(countReference(mc,craft)==1&&craft.count==11,"crafting contents lost or duplicated");
      InventoryMovement.closeOutOfReach(mc);check(countReference(mc,cursor)==1,"repeated closure duplicated cursor");
    });
    test("double chests accept either half in reach",()->{
      mc.world.method_200(0,101,0,54);mc.world.method_200(1,101,0,54);
      Inventory a=(Inventory)mc.world.method_1777(0,101,0),b=(Inventory)mc.world.method_1777(1,101,0);
      ItemStack left=new ItemStack(264,13,0),right=new ItemStack(267,1,73);a.setStack(0,left);b.setStack(0,right);
      mc.player.method_1340(5.5,102,0.5);mc.player.method_486(new class_320("Chest",a,b));
      Screen screen=mc.currentScreen;InventoryMovement.closeOutOfReach(mc);check(mc.currentScreen==screen,"right half not reachable");
      mc.player.method_1340(9.5,103,0.5);InventoryMovement.closeOutOfReach(mc);
      check(mc.currentScreen==null&&a.getStack(0)==left&&b.getStack(0)==right,"double chest closure altered contents");
    });
    test("player inventory has no world reach limit",()->{
      mc.setScreen(new class_585(mc.player));mc.player.method_1340(12.5,103,12.5);
      Screen screen=mc.currentScreen;InventoryMovement.closeOutOfReach(mc);check(mc.currentScreen==screen,"player inventory closed");
      mc.player.closeScreen();
    });
    mc.player.method_1340(.5,103,-3.5);
  }
  private static long countReference(Minecraft mc,ItemStack stack) {
    long n=mc.player.inventory.getCursorStack()==stack?1:0;
    for(ItemStack s:mc.player.inventory.main)if(s==stack)n++;
    for(Object e:mc.world.field_198)if(e instanceof class_142 item&&!item.dead&&item.field_564==stack)n++;
    return n;
  }
}
