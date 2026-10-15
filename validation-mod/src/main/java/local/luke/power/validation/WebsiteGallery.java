package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.death.*;
import local.luke.power.ui.*;
import local.luke.power.visual.*;
import local.luke.power.worldedit.MinecraftWorld;
import local.luke.power.worldedit.core.*;
import local.luke.power.worldedit.carry.ContainerCarry;
import local.luke.power.validation.mixin.*;
import net.minecraft.*;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.*;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.input.*;

/** Anonymous website scenes. Never install in a personal instance. */
final class WebsiteGallery {
  static int x,y,z;
  static void setting(Minecraft mc,String id,String value) throws Exception {
    var session=SettingsRegistry.open(mc);find(session,id).parse(value);session.save(Path.of("."));
  }
  static void block(Minecraft mc,int dx,int dy,int dz,int id,int meta) {
    new MinecraftWorld(mc.world).set(new Pos(x+dx,y+dy,z+dz),new BlockValue(id,meta));
  }
  static void camera(Minecraft mc,double dx,double dy,double dz,float yaw,float pitch) throws Exception {
    mc.setScreen(null);mc.player.getClass().getMethod("creative_setFlying",boolean.class).invoke(mc.player,true);
    // Float eye height is slightly greater than decimal 1.62. Keep feet above floor tops.
    mc.player.method_1340(x+dx,y+dy+.001,z+dz);
    mc.player.prevX=mc.player.field_1637=mc.player.x;mc.player.prevY=mc.player.field_1638=mc.player.y;mc.player.prevZ=mc.player.field_1639=mc.player.z;
    mc.player.yaw=mc.player.prevYaw=yaw;mc.player.pitch=mc.player.prevPitch=pitch;
    mc.player.velocityX=mc.player.velocityY=mc.player.velocityZ=0;
  }
  static void menu(Minecraft mc,String page,String query) throws Exception {
    var screen=new PowerOptionsScreen(null);mc.setScreen(screen);
    field(screen,"page",page);field(screen,"scroll",0d);field(screen,"sideScroll",0d);
    field(screen,"libraryOpen",false);field(screen,"changedOnly",false);
    field(screen,"relatedIds",List.of());field(screen,"conflictIds",List.of());
    ((Set<?>)field(screen,"collapsed")).clear();((Deque<?>)field(screen,"history")).clear();
    search(screen,query);Mouse.setCursorPosition(0,0);
  }
  static void run(Minecraft mc,String action) throws Exception {
    if(action.startsWith("menu ")) {menu(mc,action.substring(5),"");return;}
    if(action.equals("search")) {menu(mc,"General","speed");return;}
    if(action.equals("freecam-view")) {
      var settings=Class.forName("local.luke.power.camera.FreecamConfig").getField("config").get(null);

      var controller=Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
      controller.getClass().getMethod("startFromView",Minecraft.class).invoke(controller,mc);
      controller.getClass().getMethod("setCameraPosition",double.class,double.class,double.class)
          .invoke(controller,mc.player.x-8,mc.player.y+4,mc.player.z+8);
      controller.getClass().getMethod("setActive",boolean.class).invoke(controller,true);return;
    }
    if(action.equals("modes")) {
      mc.setScreen((Screen)Class.forName("local.luke.power.creative.ui.ModePickerScreen").getConstructor().newInstance());
      Mouse.setCursorPosition(0,0);return;
    }
    if(action.equals("chat")) {
      mc.setScreen(new ChatScreen());((ChatInput)mc.currentScreen).power$text("//set st");
      Class.forName("local.luke.power.worldedit.chat.ChatAccess").getMethod("setText",String.class).invoke(null,"//set st");
      ((ChatInput)mc.currentScreen).power$type('\0',Keyboard.KEY_DOWN);Mouse.setCursorPosition(0,0);return;
    }
    if(action.equals("death")) {
      var s=DeathConfig.copy();s.enabled=true;DeathConfig.preview(s);
      int[] ids={276,278,279,5,4,50,260,325,345,264,265,266,331,35,20,17,262,280};
      for(int i=0;i<ids.length;i++) {
        var stack=new ItemStack(ids[i],1,0);stack.count=Math.min(stack.getMaxCount(),i<3?1:i<9?16:32);mc.player.inventory.main[i]=stack;
      }
      for(int i=0;i<4;i++)mc.player.inventory.armor[i]=new ItemStack(313-i,1,0);
      DeathChecks.record(mc,"Fall");var record=DeathChecks.latest(mc);DeathChecks.clear(mc);
      mc.setScreen(new DeathInventoryScreen(mc,record));Mouse.setCursorPosition(0,0);return;
    }
    if(action.equals("sign-editor")) {
      var sign=(SignBlockEntity)mc.world.method_1777(x,y,z);
      Arrays.fill(sign.texts, "");mc.player.method_489(sign);
      var input=(ScreenInput)mc.currentScreen;
      for(String line:List.of("Trailhead","Lake  <",">  Mountain","Bring a boat")) {
        for(char c:line.toCharArray())input.power$key(c,0);
        input.power$key('\r',Keyboard.KEY_RETURN);
      }
      Mouse.setCursorPosition(0,0);return;
    }
    if(action.equals("carry-pickup")) {
      mc.player.field_161.field_2536=true;mc.field_2823=new class_27(x,y,z,1,Vec3d.createCached(x+.5,y+.5,z+.5));
      check(ContainerCarry.click(mc,1),"gallery chest pickup failed");mc.player.field_161.field_2536=false;
      camera(mc,.5,2,-3,25,6);return;
    }
    if(action.equals("tnt-prime")) {
      var s=local.luke.power.gameplay.GameplayConfig.copy();s.limitTnt=true;local.luke.power.gameplay.GameplayConfig.preview(s);
      for(int dx=-4;dx<=4;dx+=2)for(int dz=-4;dz<=4;dz+=2) {
        var tnt=new class_365(mc.world,x+dx+.5,y+.5,z+dz+.5);tnt.field_1390=0;mc.world.method_210(tnt);
      }
      mc.world.method_227();
      mc.setScreen(new Screen(){@Override public boolean shouldPause(){return true;} @Override public void render(int mx,int my,float delta){}});
      return;
    }
    if(ContainerCarry.carrying()) {
      // Place the anonymous fixture chest before changing scenes.
      mc.player.field_161.field_2536=false;
      mc.field_2823=new class_27(x+2,y-1,z,1,Vec3d.createCached(x+2.5,y,z+.5));
      ContainerCarry.click(mc,1);
      check(!ContainerCarry.carrying(),"place gallery chest before travelling");
    }
    if(mc.player.dead||mc.player.health<=0||mc.player.field_1041>0)mc.method_2122(true,0);
    if(mc.player.method_943())mc.player.method_497(true,true,false);
    var detached=Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
    detached.getClass().getMethod("setActive",boolean.class).invoke(detached,false);
    (AccessRules.of(mc.world.method_262())).allAllowed(true);CreativeVehicleChecks.mode(mc,"CREATIVE");mc.player.health=20;mc.player.fire=0;
    mc.setScreen(null);mc.options.hideHud=true;mc.options.thirdPerson=false;
    var visuals=VisualConfig.copy();visuals.hideEntities=false;visuals.spectatorFullBright=false;
    visuals.containerCarry=false;visuals.oldCobble=visuals.oldBricks=false;visuals.redstonePowerLevels=false;VisualConfig.preview(visuals);
    var light=local.luke.power.light.LightConfig.copy();light.enabled=false;local.luke.power.light.LightConfig.preview(light);
    var status=local.luke.power.status.StatusConfig.copy();status.enabled=false;local.luke.power.status.StatusConfig.preview(status);
    setting(mc,"native.guiScale","2");setting(mc,"native.renderDistance","12");setting(mc,"native.brightness","55");setting(mc,"native.fov","75");
    java.util.Arrays.fill(mc.player.inventory.main,null);java.util.Arrays.fill(mc.player.inventory.armor,null);mc.player.inventory.setCursorStack(null);mc.player.inventory.selectedSlot=8;
    List<String> scenes=List.of("vanilla","menu-scene","controls-scene","rules-scene","preview","carry","sign","chat-scene","death-scene","modes-scene","building","textures","tnt","crafting-scene","freecam","castle");
    int index=scenes.indexOf(action);if(index<0)throw new IllegalArgumentException(action);
    x=1536+index*160;z=index%2==0?1216:-1216;
    for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)mc.world.method_214((x>>4)+dx,(z>>4)+dz);
    y=110;while(y>64&&mc.world.getBlockId(x,y-1,z)==0)y--;
    if(action.equals("building"))y=68;
    if(action.equals("castle"))y=80;
    ChatChecks.submit(mc,"/time set noon");ChatChecks.submit(mc,"/weather clear");
    camera(mc,0,12,0,135,12);
    if(Set.of("preview","carry","sign","building","textures","tnt").contains(action)) {
      for(int dx=-10;dx<=10;dx++)for(int dz=-8;dz<=8;dz++) {
        block(mc,dx,-1,dz,action.equals("tnt")?1:action.equals("preview")?5:2,0);
        for(int dy=0;dy<128-y;dy++)block(mc,dx,dy,dz,0,0);
      }
    }
    if(action.equals("vanilla")) {camera(mc,0,18,0,225,13);ChatChecks.submit(mc,"/time set 10500");}
    else if(action.equals("menu-scene")) {camera(mc,0,10,0,40,12);ChatChecks.submit(mc,"/time set 11200");}
    else if(action.equals("preview")||action.equals("carry")) {
      if(action.equals("preview")) {
        for(int dx=-4;dx<=4;dx++)for(int dz=-2;dz<=4;dz++)for(int dy=0;dy<=4;dy++)
          if(dy==4||Math.abs(dx)==4||dz==4)block(mc,dx,dy,dz,dy==4?5:Math.abs(dx)==4?17:dy==1||dy==2?20:5,0);
        block(mc,-2,0,2,58,0);block(mc,2,0,2,61,0);
      }
      block(mc,0,0,0,54,0);
      var chest=(Inventory)mc.world.method_1777(x,y,z);
      int[] ids={4,5,20,17,50,264,265,331,35,280,262,260,297,325,263,264,266,345};
      for(int i=0;i<ids.length;i++) { var stack=new ItemStack(ids[i],1,0);stack.count=Math.min(32,stack.getMaxCount());chest.setStack(i,stack); }
      visuals=VisualConfig.copy();visuals.containerCarry=true;VisualConfig.preview(visuals);
      camera(mc,.5,2.2,-2.7,0,29);mc.options.hideHud=false;
    } else if(action.equals("sign")) {
      for(int dx=-6;dx<=6;dx+=2){block(mc,dx,0,3,85,0);block(mc,dx,0,2,37,0);}
      block(mc,0,0,0,63,0);var sign=(SignBlockEntity)mc.world.method_1777(x,y,z);
      sign.texts[0]="Trailhead";sign.texts[1]="Lake  <";sign.texts[2]=">  Mountain";sign.texts[3]="Bring a boat";
      camera(mc,.5,2,-3,0,14);
    } else if(action.equals("building")) {
      for(int dz=-7;dz<=7;dz++)for(int dx=-2;dx<=2;dx++){block(mc,dx,0,dz,5,0);if(Math.abs(dx)==2)block(mc,dx,1,dz,85,0);}
      setting(mc,"tweaks.placement.enabled", "true");
      status=local.luke.power.status.StatusConfig.copy();status.enabled=true;local.luke.power.status.StatusConfig.preview(status);
      mc.player.inventory.main[0]=new ItemStack(5,64,0);mc.player.inventory.selectedSlot=0;camera(mc,4,4,-6,40,32);mc.options.hideHud=false;
    } else if(action.equals("textures")) {
      for(int dx=-5;dx<=5;dx++)for(int dy=0;dy<4;dy++)block(mc,dx,dy,2,dx<0?4:45,0);
      visuals=VisualConfig.copy();visuals.oldCobble=visuals.oldBricks=true;VisualConfig.preview(visuals);mc.worldRenderer.method_1537();camera(mc,1.5,3,-5,0,10);
    } else if(action.equals("tnt")) {
      for(int dx=-6;dx<=6;dx++)for(int dz=-6;dz<=6;dz++)block(mc,dx,0,dz,46,0);
      camera(mc,13,12,-15,41,33);
    } else if(action.equals("castle")) {
      for(int dx=-19;dx<=19;dx++)for(int dz=-19;dz<=19;dz++) {
        for(int dy=-5;dy<0;dy++)block(mc,dx,dy,dz,dy==-1?2:3,0);
        for(int dy=0;dy<128-y;dy++)block(mc,dx,dy,dz,0,0);
      }
      for(int dx=-12;dx<=12;dx++)for(int dz=-12;dz<=12;dz++) {
        if(Math.abs(dx)<2||Math.abs(dz)<2)block(mc,dx,-1,dz,13,0);
        if(Math.abs(dx)==10||Math.abs(dz)==10)for(int dy=0;dy<=6;dy++) {
          boolean gate=dz==-10&&Math.abs(dx)<=2&&dy<4;
          if(!gate&&(dy<6||(dx+dz)%2==0))block(mc,dx,dy,dz,4,0);
        }
      }
      for(int tx:new int[]{-10,10})for(int tz:new int[]{-10,10}) {
        for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++) {
          int radius=dx*dx+dz*dz;
          if(radius>16)continue;
          for(int dy=0;dy<=14;dy++) {
            boolean window=dy>=8&&dy<=10&&(dx==0||dz==0);
            if(dy==0||dy==6||dy==12||radius>=9&&dy<13&&!window||radius>=9&&dy==14&&(dx+dz)%2==0)
              block(mc,tx+dx,dy,tz+dz,4,0);
          }
        }
        for(int dy=13;dy<=19;dy++)block(mc,tx,dy,tz,85,0);
        for(int dx=1;dx<=3;dx++)for(int dy=17;dy<=19;dy++)block(mc,tx+dx,dy,tz,35,14);
      }
      for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)for(int dy=0;dy<9;dy++)
        if(Math.abs(dx)==4||Math.abs(dz)==4||dy==0||dy==8)block(mc,dx,dy,dz,dy>1&&dy<6&&(dx==0||dz==0)?20:5,0);
      for(int h=0;h<6;h++)for(int dx=-5+h;dx<=5-h;dx++)for(int dz=-5+h;dz<=5-h;dz++)block(mc,dx,9+h,dz,35,14);
      ChatChecks.submit(mc,"//pos1 "+(x-15)+","+(y-1)+","+(z-15));
      ChatChecks.submit(mc,"//pos2 "+(x+15)+","+(y+20)+","+(z+15));
      camera(mc,31,24,-33,44,24);
    } else if(action.equals("freecam")) {camera(mc,0,24,0,225,24);}
    mc.inGameHud.clearChat();Mouse.setCursorPosition(0,0);
    log("WEBSITE SCENE "+action);
  }
}
