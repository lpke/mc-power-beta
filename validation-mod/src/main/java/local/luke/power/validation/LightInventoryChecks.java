package local.luke.power.validation;

import java.lang.reflect.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.input.GameplayKeys;
import local.luke.power.light.*;
import local.luke.power.visual.*;
import net.minecraft.*;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.container.ContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.opengl.GL11;
import static local.luke.power.validation.Validation.*;

public final class LightInventoryChecks {
  private static final class TestInventoryScreen extends class_585 {
    TestInventoryScreen(net.minecraft.entity.player.PlayerEntity player) { super(player); }
    void click(int x,int y,int button) { super.mouseClicked(x,y,button); super.mouseReleased(x,y,button); }
    int slotX(Slot slot) { return (width-backgroundWidth)/2+slot.x+8; }
    int slotY(Slot slot) { return (height-backgroundHeight)/2+slot.y+8; }
  }
  public static void inventory(Minecraft mc) throws Exception {
    VisualSettings before=VisualConfig.copy();
    ItemStack[] main=mc.player.inventory.main.clone(),armour=mc.player.inventory.armor.clone();
    ItemStack cursor=mc.player.inventory.getCursorStack();
    Screen previous=mc.currentScreen;
    mc.player.inventory.setCursorStack(null);
    try {
      VisualSettings settings=VisualConfig.copy();settings.swapEquipment=true;VisualConfig.preview(settings);
      TestInventoryScreen screen=new TestInventoryScreen(mc.player);mc.setScreen(screen);
      Slot slot=(Slot)mc.player.playerContainer.slots.get(36);
      int x=screen.slotX(slot),y=screen.slotY(slot);
      for(int item:new int[]{267,257,259,292,298,306,307,308,309,359}) for(int button:new int[]{0,1}) {
        test("inventory screen swaps item "+item+" button "+button+" without replacing stacks",()->{
          ItemStack a=new ItemStack(item,1,3),b=new ItemStack(item,1,27);
          slot.setStack(a);mc.player.inventory.setCursorStack(b);
          screen.click(x,y,button);
          check(slot.getStack()==b&&mc.player.inventory.getCursorStack()==a,"original stack references not exchanged");
          check(a.count==1&&b.count==1&&a.getDamage()==3&&b.getDamage()==27,"count/durability changed");
        });
      }
      test("armour slot swaps matching durability variants",()->{
        Slot helmet=(Slot)mc.player.playerContainer.slots.get(5);
        ItemStack a=new ItemStack(306,1,3),b=new ItemStack(306,1,20);
        helmet.setStack(a);mc.player.inventory.setCursorStack(b);
        mc.player.playerContainer.onSlotClick(5,0,false,mc.player);
        check(helmet.getStack()==b&&mc.player.inventory.getCursorStack()==a,"helmet swap failed");
      });
      test("armour slot still rejects wrong item type",()->{
        Slot helmet=(Slot)mc.player.playerContainer.slots.get(5);
        ItemStack a=helmet.getStack(),b=new ItemStack(267,1,15);mc.player.inventory.setCursorStack(b);
        mc.player.playerContainer.onSlotClick(5,0,false,mc.player);
        check(helmet.getStack()==a&&mc.player.inventory.getCursorStack()==b,"rejected slot changed");
      });
      test("crafting output cannot receive a swapped tool",()->{
        Slot output=(Slot)mc.player.playerContainer.slots.get(0);
        ItemStack saved=output.getStack(),a=new ItemStack(267,1,3),b=new ItemStack(267,1,7);
        try {
          output.setStack(a);mc.player.inventory.setCursorStack(b);
          mc.player.playerContainer.onSlotClick(0,0,false,mc.player);
          check(output.getStack()==a&&mc.player.inventory.getCursorStack()==b,"result slot changed");
        } finally {output.setStack(saved);}
      });
      test("Beta bows have no durability and retain vanilla behaviour",()->{
        ItemStack a=new ItemStack(261,1,0),b=new ItemStack(261,1,0);
        check(!a.isDamageable(),"unexpected bow durability in Beta");
        slot.setStack(a);mc.player.inventory.setCursorStack(b);
        mc.player.playerContainer.onSlotClick(36,0,false,mc.player);
        check(slot.getStack()==a&&mc.player.inventory.getCursorStack()==b,"bow semantics changed");
      });
      test("stackable blocks still merge normally",()->{
        slot.setStack(new ItemStack(1,20,0));mc.player.inventory.setCursorStack(new ItemStack(1,30,0));
        mc.player.playerContainer.onSlotClick(36,0,false,mc.player);
        check(slot.getStack().count==50&&mc.player.inventory.getCursorStack()==null,"ordinary merge changed");
      });
      test("disabled equipment swapping restores vanilla behaviour",()->{
        settings.swapEquipment=false;VisualConfig.preview(settings);
        ItemStack a=new ItemStack(267,1,3),b=new ItemStack(267,1,7);slot.setStack(a);mc.player.inventory.setCursorStack(b);
        mc.player.playerContainer.onSlotClick(36,0,false,mc.player);
        check(slot.getStack()==a&&mc.player.inventory.getCursorStack()==b,"disabled setting ignored");
      });
      test("light controls stay visible while off and link to all settings",()->{
        ConfigSession session=SettingsRegistry.open(mc);
        Setting key=find(session,"keys.powerbeta.lightOverlay");
        check(ControlLinks.enabled(session,key),"off state hides toggle");
        check(ControlLinks.settings(session,key).size()==10,"missing linked settings");
        check(key.defaultValue.getAsInt()==65,"F7 not default");
        check(find(session,"visual.swapEquipment").defaultValue.getAsBoolean(),"swap not default on");
      });
    } finally {
      mc.player.inventory.setCursorStack(null);mc.setScreen(previous);
      System.arraycopy(main,0,mc.player.inventory.main,0,main.length);
      System.arraycopy(armour,0,mc.player.inventory.armor,0,armour.length);
      mc.player.inventory.setCursorStack(cursor);VisualConfig.preview(before);
    }
  }

  public static void light(Minecraft mc,String action) throws Exception {
    if(action.equals("input")) {
      var state=new com.google.gson.JsonObject();
      state.addProperty("focus",mc.field_2778);
      state.addProperty("screen",mc.currentScreen==null?"none":mc.currentScreen.getClass().getName());
      state.addProperty("binding",GameplayKeys.LIGHT.code);
      state.addProperty("held",local.luke.power.input.Bindings.down(GameplayKeys.LIGHT));
      state.addProperty("enabled",LightConfig.current().enabled);
      for(int i=0;i<org.lwjgl.input.Mouse.getButtonCount();i++)
        state.addProperty("mouse"+i,org.lwjgl.input.Mouse.isButtonDown(i));
      Field listeners=local.luke.power.input.Bindings.class.getDeclaredField("mouseListeners");listeners.setAccessible(true);
      state.addProperty("listeners",((Map<?,?>)listeners.get(null)).keySet().toString());
      java.nio.file.Files.writeString(java.nio.file.Path.of("power-beta-overlay-input.json"),state.toString());
    } else if(action.equals("setup")) {
      mc.setScreen(null);
      // Disposable test world only. A level platform with a torch and a dark side.
      for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++) {
        mc.world.method_200(x,100,z,1);
        for(int y=101;y<=106;y++)mc.world.method_200(x,y,z,0);
      }
      mc.world.method_200(0,101,0,50);
      mc.player.method_1340(.5,102,-5.5);mc.player.yaw=0;mc.player.prevYaw=0;mc.player.pitch=55;mc.player.prevPitch=55;
      LightSettings s=new LightSettings();s.enabled=true;LightConfig.preview(s);
    } else if(action.equals("check")) {
      LightSettings s=LightConfig.copy();
      test("block-light numbers match engine at feet",()->{
        check(LightOverlay.sample(mc.world,s,1,100,0)==mc.world.method_164(class_56.BLOCK,1,101,0),"wrong sample coordinate");
        check(LightOverlay.sample(mc.world,s,1,100,0)>=8,"torch does not light nearby floor");
        check(LightOverlay.sample(mc.world,s,9,100,9)<=7,"dark floor incorrectly safe");
      });
      test("overlay skips unloaded chunks before accessing blocks",()->{
        int x=30000000,z=30000000;
        check(!mc.world.method_239(x,100,z),"test position is loaded");
        check(LightOverlay.sample(mc.world,s,x,100,z)==-1,"unloaded position sampled");
        check(!mc.world.method_239(x,100,z),"overlay loaded a chunk");
      });
      test("surface filtering rejects slabs, liquid and blocked headroom",()->{
        mc.world.method_200(5,100,0,44);
        check(LightOverlay.sample(mc.world,s,5,100,0)==-1,"half slab marked as full");
        mc.world.method_200(5,100,0,43);
        check(LightOverlay.sample(mc.world,s,5,100,0)>=0,"double slab omitted");
        mc.world.method_200(5,101,0,9);
        check(LightOverlay.sample(mc.world,s,5,100,0)==-1,"underwater floor included");
        mc.world.method_200(5,101,0,0);mc.world.method_200(5,102,0,1);
        s.spawnableOnly=true;
        check(LightOverlay.sample(mc.world,s,5,100,0)==-1,"blocked headroom included");
        s.spawnableOnly=false;
        check(LightOverlay.sample(mc.world,s,5,100,0)>=0,"full-block mode requires two blocks clearance");
        mc.world.method_200(5,102,0,0);mc.world.method_200(5,100,0,1);
      });
      test("combined light reads engine sky-darkened light",()->{
        s.lightSource=1;
        check(LightOverlay.sample(mc.world,s,9,100,9)==mc.world.method_158(9,101,9,false),"combined read wrong");s.lightSource=0;
      });
      test("rendering restores GL matrix and attribute state",()->{
        Field cache=LightOverlay.class.getDeclaredField("CACHE");cache.setAccessible(true);
        check(!((LightCache)cache.get(null)).cells().isEmpty(),"no labels to render");
        int depth=GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
        GL11.glEnable(GL11.GL_FOG);GL11.glDisable(GL11.GL_BLEND);
        LightOverlay.render(mc,0);
        check(GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH)==depth,"matrix leak");
        check(GL11.glIsEnabled(GL11.GL_FOG)&&!GL11.glIsEnabled(GL11.GL_BLEND),"attribute leak");
        check(GL11.glGetError()==GL11.GL_NO_ERROR,"OpenGL error");
      });
      test("disabling overlay clears every cached label",()->{
        s.enabled=false;LightConfig.preview(s);LightOverlay.tick(mc);
        Field f=LightOverlay.class.getDeclaredField("CACHE");f.setAccessible(true);
        check(((LightCache)f.get(null)).cells().isEmpty(),"cached labels retained");
        s.enabled=true;LightConfig.preview(s);
      });
    }
  }
}
