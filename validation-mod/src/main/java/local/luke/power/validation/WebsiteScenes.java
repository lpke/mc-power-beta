package local.luke.power.validation;

import static local.luke.power.validation.WebsiteGallery.*;
import static local.luke.power.validation.WebsiteActions.aim;
import static local.luke.power.validation.Validation.*;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import java.util.Arrays;

/** Focused native gameplay scenes for website illustrations, never shipped. */
final class WebsiteScenes {
  static void run(Minecraft mc,String action) throws Exception {
    if(action.startsWith("place ")) {
      String[] p=action.substring(6).split(" ");
      boolean placed=mc.interactionManager.method_1713(mc.player,mc.world,mc.player.inventory.getSelectedItem(),x+Integer.parseInt(p[0]),y+Integer.parseInt(p[1]),z+Integer.parseInt(p[2]),Integer.parseInt(p[3]));
      check(placed,"native block placement failed");return;
    }
    if(action.startsWith("view ")) {
      String[] p=action.substring(5).split(" ");mc.player.yaw=mc.player.prevYaw=Float.parseFloat(p[0]);mc.player.pitch=mc.player.prevPitch=Float.parseFloat(p[1]);return;
    }
    if(action.startsWith("pose ")) {
      String[] p=action.substring(5).split(" ");
      camera(mc,Double.parseDouble(p[0]),Double.parseDouble(p[1]),Double.parseDouble(p[2]),Float.parseFloat(p[3]),Float.parseFloat(p[4]));return;
    }
    if(action.equals("state")) {
      var mount=mc.player.field_1595;
      log("SCENE STATE origin="+x+","+y+","+z+" player="+mc.player.x+","+mc.player.y+","+mc.player.z+" bbox="+mc.player.boundingBox+" eye="+mc.player.field_1631+" sleeping="+mc.player.method_943()+" dead="+mc.player.dead+" health="+mc.player.health+" focused="+org.lwjgl.opengl.Display.isActive()+" view="+(mc.field_2807==mc.player)+" bed="+mc.world.getBlockId(x,y,z)+" target="+mc.field_2823+" mount="+(mount==null?"none":mount.x+","+mount.z+" velocity="+mount.velocityX+","+mount.velocityZ));return;
    }
    if(action.equals("pause")) {pause(mc);return;}
    if(action.equals("resume")) {mc.setScreen(null);return;}
    if(action.equals("two-chests")) {
      WebsiteGallery.run(mc,"preview");block(mc,0,0,0,0,0);block(mc,-2,0,0,54,0);block(mc,2,0,0,54,0);
      for(int side:new int[]{-2,2}) {
        var inventory=(net.minecraft.inventory.Inventory)mc.world.method_1777(x+side,y,z);
        for(int i=0;i<18;i++)inventory.setStack(i,new ItemStack(side<0?new int[]{1,5,4,20,50,17}[i%6]:new int[]{264,265,266,331,260,345}[i%6],i%6==5?1:32,0));
      }
      setting(mc,"keys.powerbeta.containerPreview","34");setting(mc,"keys.power_building.hotbar.base","0");
      setting(mc,"tweaks.freeLook","false");setting(mc,"native.guiScale","3");camera(mc,.5,2.2,-3,0,20);return;
    }
    if(action.equals("preview-left")||action.equals("preview-right")) {
      aim(mc,.5,2.2,-3,action.endsWith("left")?-1.5:2.5,.5,.5);return;
    }
    if(action.startsWith("roof-row ")) {
      int row=Integer.parseInt(action.substring(9));aim(mc,6.5,5.7,-4+row+.5,-2.001,5.7,-4+row+.5);
      local.luke.power.building.camera.FreeLook.reset(mc);
      local.luke.power.building.camera.FreeLook.begin(mc);
      local.luke.power.building.camera.FreeLook.turn(mc.player,0,-180);return;
    }
    if(action.equals("spectator-speed")) {
      CreativeVehicleChecks.mode(mc,"SPECTATOR");
      mc.options.hideHud=false;setting(mc,"native.guiScale","3");setting(mc,"creative.spectatorScroll","true");
      setting(mc,"creative.showSpectatorSpeed","true");
      Class.forName("local.luke.power.creative.FlightController").getMethod("scroll",Minecraft.class,int.class).invoke(null,mc,360);
      return;
    }
    if(action.equals("light-torch")) {run(mc,"place 0 -1 -2 1");return;}
    if(action.equals("vehicle-wait")) {
      for(Object value:new java.util.ArrayList<>(mc.world.field_198)) {
        if(value instanceof net.minecraft.entity.Entity entity && entity!=mc.player.field_1595
            && (entity instanceof class_113 || entity instanceof class_549))entity.markDead();
      }
      pause(mc);return;
    }
    if(action.equals("vehicle-start")) {
      mc.setScreen(null);if(mc.player.field_1595 instanceof class_549)mc.player.field_1595.velocityZ=.01;return;
    }
    if(action.equals("bed-enter")) {
      ChatChecks.submit(mc,"//desel");
      mc.player.method_495(x-1,y,z);check(mc.player.method_943(),"could not enter bed");
      mc.player.prevX=mc.player.field_1637=mc.player.x;mc.player.prevY=mc.player.field_1638=mc.player.y;mc.player.prevZ=mc.player.field_1639=mc.player.z;
      mc.inGameHud.clearChat();pause(mc);return;
    }
    if(action.equals("gold-glass-break")) {mc.interactionManager.method_1716(x,y,z,2);return;}
    if(action.equals("light")) {
      Release150Gallery.run(mc,"light");
      // This fixture's original chamber is centred at 960,38,576.
      x=960;y=38;z=576;mc.player.inventory.main[0]=new ItemStack(50,64,0);mc.player.inventory.selectedSlot=0;
      mc.options.hideHud=false;return;
    }
    if(action.equals("forest")) {forest(mc);return;}
    WebsiteGallery.run(mc,"building");
    local.luke.power.building.camera.FreeLook.reset(mc);setting(mc,"tweaks.freeLook","false");
    setting(mc,"native.guiScale","3");
    setting(mc,"tweaks.placement.enabled","false");setting(mc,"tweaks.flexible.enabled","false");
    setting(mc,"tweaks.sneak.enabled","false");
    String[] names={"gold-glass","hotbar","edge","slab","mining","sleep","stacking","roof","plants","blocks","sponge","hud","photo","culling"};
    int index=Arrays.asList(names).indexOf(action);check(index>=0,"unknown website scene "+action);
    x=6208+index*96;z=2304;y=72;
    for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)mc.world.method_214((x>>4)+dx,(z>>4)+dz);
    for(int dx=-12;dx<=12;dx++)for(int dz=-12;dz<=12;dz++) {
      block(mc,dx,-1,dz,2,0);for(int dy=0;dy<128-y;dy++)block(mc,dx,dy,dz,0,0);
    }
    mc.options.thirdPerson=false;mc.options.hideHud=false;camera(mc,.5,2.62,-3,0,18);
    switch(action) {
      case "gold-glass" -> {
        setting(mc,"power_gold:config.enableGoldPickaxeSilkTouch","true");
        for(int dx=-2;dx<=2;dx++)for(int dy=0;dy<=2;dy++)block(mc,dx,dy,1,20,0);
        block(mc,0,0,0,20,0);CreativeVehicleChecks.mode(mc,"SURVIVAL");
        mc.player.inventory.main[0]=new ItemStack(285,1,0);mc.player.inventory.selectedSlot=0;
        aim(mc,.5,1.62,-2.0,.5,.55,0);break;
      }
      case "hotbar" -> {
        var s=local.luke.power.building.config.Config.current().copy();s.hotbar.swap=s.hotbar.overlay=true;
        s.hotbar.alignment=local.luke.power.building.hotbar.HotbarSettings.Alignment.CENTER;
        local.luke.power.building.config.Config.preview(s);
        setting(mc,"keys.power_building.hotbar.base","34");setting(mc,"keys.powerbeta.containerPreview","0");
        int[][] rows={{1,5,20,4,17,35,44,50,45},{278,277,279,276,261,262,260,325,345},{264,265,266,331,263,295,338,353,351},{35,35,35,35,35,35,35,35,35}};
        for(int i=0;i<36;i++){var stack=new ItemStack(rows[i/9][i%9],1,i>=27?i%9:0);stack.count=Math.min(stack.getMaxCount(),i<9?32:64);mc.player.inventory.main[i]=stack;}
        camera(mc,.5,2.62,-3,10,10);break;
      }
      case "edge" -> {
        for(int dx=-12;dx<=12;dx++)for(int dz=-12;dz<=12;dz++)for(int dy=-24;dy<0;dy++)block(mc,dx,dy,dz,0,0);
        for(int dz=-8;dz<=8;dz++)block(mc,0,20,dz,5,0);
        setting(mc,"tweaks.sneak.enabled","true");CreativeVehicleChecks.mode(mc,"SURVIVAL");
        camera(mc,.5,22.62,-3.5,-20,52);mc.player.getClass().getMethod("creative_setFlying",boolean.class).invoke(mc.player,false);
        status(mc);break;
      }
      case "slab" -> {
        for(int dx=-3;dx<=3;dx++)for(int dy=0;dy<=4;dy++)block(mc,dx,dy,2,4,0);
        for(int dx=-2;dx<=2;dx++){block(mc,dx,2,0,44,0);block(mc,dx,3,0,4,0);}
        for(int dy=0;dy<=3;dy++){block(mc,-3,dy,0,4,0);block(mc,3,dy,0,4,0);}
        setting(mc,"tweaks.slabs.enabled","true");mc.player.inventory.main[0]=new ItemStack(44,64,0);mc.player.inventory.selectedSlot=0;
        aim(mc,.5,2.2,-2.4,.5,2.72,2);status(mc);break;
      }
      case "mining" -> {
        for(int dx=-3;dx<=3;dx++)for(int dz=-5;dz<=10;dz++)for(int dy=-1;dy<=4;dy++)block(mc,dx,dy,dz,1,0);
        for(int dx=-1;dx<=1;dx++)for(int dz=-5;dz<7;dz++)for(int dy=0;dy<3;dy++)block(mc,dx,dy,dz,0,0);
        for(int dz=-4;dz<=5;dz+=3)block(mc,-1,0,dz,50,5);
        for(int dz=1;dz<=5;dz+=2){block(mc,2,1,dz,15,0);block(mc,2,2,dz,16,0);}
        block(mc,1,1,7,14,0);setting(mc,"native.brightness","85");
        CreativeVehicleChecks.mode(mc,"SURVIVAL");mc.player.inventory.main[0]=new ItemStack(278,1,0);mc.player.inventory.selectedSlot=0;
        camera(mc,.5,1.62,.5,-8,6);break;
      }
      case "sleep" -> {
        y=104;
        for(int dx=-3;dx<=3;dx++)for(int dz=-2;dz<=4;dz++) {
          block(mc,dx,-1,dz,5,0);for(int dy=0;dy<4;dy++)block(mc,dx,dy,dz,0,0);
        }
        block(mc,-1,0,-1,26,0);block(mc,-1,0,0,26,8);
        setting(mc,"power_environment:config.SLEEP_CONFIG.bedsSpeedUpNightRatherThanSkipIt","true");
        setting(mc,"power_environment:config.SLEEP_CONFIG.bedsSpeedUpNightTickRate","1");
        ChatChecks.submit(mc,"/time set 13500");CreativeVehicleChecks.mode(mc,"SURVIVAL");camera(mc,.5,1.62,-1.0,0,0);break;
      }
      case "stacking" -> {
        CreativeVehicleChecks.mode(mc,"SURVIVAL");
        for(String id:new String[]{"enableModernWoodDoorStackSize","enableModernIronDoorStackSize","enableModernSignStackSize","stackableEmptyBuckets"})
          setting(mc,"power_compat:config.STACK_SIZE_CONFIG."+id,"true");
        int[] ids={325,323,324,330,1,5,50,20,35,326,327,335};
        for(int i=0;i<ids.length;i++){var stack=new ItemStack(ids[i],1,0);stack.count=stack.getMaxCount();mc.player.inventory.main[i+9]=stack;}
        camera(mc,.5,1.62,-3,10,8);mc.setScreen(new class_585(mc.player));break;
      }
      case "roof" -> {
        for(int dx=-5;dx<=5;dx++)for(int dz=-5;dz<=5;dz++) {
          block(mc,dx,-1,dz,5,0);
          if(Math.abs(dx)==5||Math.abs(dz)==5)for(int dy=0;dy<=4;dy++)block(mc,dx,dy,dz,dy==4?5:17,0);
          if(dx<=-2)block(mc,dx,5,dz,5,0);
        }
        setting(mc,"creative.blockReach","10");setting(mc,"tweaks.placement.enabled","true");setting(mc,"tweaks.placement.attemptsPerTick","1");
        setting(mc,"tweaks.freeLook","true");setting(mc,"tweaks.freeLookToggle","false");setting(mc,"keys.key.powerbeta.free_look","34");
        mc.options.thirdPerson=true;
        var s=local.luke.power.building.config.Config.current().copy();s.freeLookFollowThirdPerson=true;s.placement.restrictionEnabled=true;s.placement.restrictionMode=local.luke.power.fastplace.RestrictionMode.LAYER;local.luke.power.building.config.Config.preview(s);
        mc.player.inventory.main[0]=new ItemStack(5,64,0);mc.player.inventory.selectedSlot=0;
        aim(mc,4.5,10,-6,-1.5,5.8,-4.5);break;
      }
      case "plants" -> {
        for(int dx=-7;dx<=7;dx++)for(int dz=0;dz<=6;dz++){block(mc,dx,-1,dz,60,7);block(mc,dx,0,dz,59,7);}
        for(int dz=0;dz<=6;dz++)block(mc,0,-1,dz,9,0);
        mc.player.inventory.main[0]=new ItemStack(292,1,0);mc.player.inventory.selectedSlot=0;camera(mc,6,3,-4,32,22);break;
      }
      case "blocks" -> {
        for(int dx=-4;dx<=4;dx++)block(mc,dx,0,0,85,0);
        block(mc,-2,1,0,72,0);block(mc,2,1,0,70,0);block(mc,0,2,1,96,4);
        camera(mc,5,3,-6,36,20);break;
      }
      case "sponge" -> {
        setting(mc,"power_gold:config.enableSpongeSoaksUpWater","true");
        for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)block(mc,dx,0,dz,Math.abs(dx)==4||Math.abs(dz)==4?24:9,0);
        mc.player.inventory.main[0]=new ItemStack(19,64,0);mc.player.inventory.selectedSlot=0;aim(mc,.5,4,-3,.5,-.01,.5);break;
      }
      case "hud" -> {status(mc);mc.player.inventory.main[0]=new ItemStack(257,1,0);mc.player.inventory.selectedSlot=0;camera(mc,0,5,0,135,15);break;}
      case "photo", "culling" -> {WebsiteGallery.run(mc,"castle");camera(mc,20,15,-24,40,22);mc.options.hideHud=true;break;}
    }
    mc.inGameHud.clearChat();org.lwjgl.input.Mouse.setCursorPosition(0,0);
  }
  private static void status(Minecraft mc) {var s=local.luke.power.status.StatusConfig.copy();s.enabled=true;local.luke.power.status.StatusConfig.preview(s);}
  private static void pause(Minecraft mc) {
    mc.setScreen(new net.minecraft.client.gui.screen.Screen(){
      @Override public boolean shouldPause(){return true;}
      @Override public void render(int mx,int my,float delta){}
    });
  }
  private static void forest(Minecraft mc) throws Exception {
    WebsiteGallery.run(mc,"building");x=8096;y=72;z=2624;
    for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=3;dz++)mc.world.method_214((x>>4)+dx,(z>>4)+dz);
    for(int dx=-10;dx<=10;dx++)for(int dz=-10;dz<=35;dz++) {
      block(mc,dx,-1,dz,Math.abs(dx)<=1?13:2,0);for(int dy=0;dy<12;dy++)block(mc,dx,dy,dz,0,0);
    }
    for(int dx:new int[]{-7,-4,4,7})for(int dz=-4;dz<=30;dz+=7) {
      for(int dy=0;dy<6;dy++)block(mc,dx,dy,dz,17,0);
      for(int ax=-2;ax<=2;ax++)for(int az=-2;az<=2;az++)for(int dy=4;dy<=7;dy++)if(Math.abs(ax)+Math.abs(az)<4)block(mc,dx+ax,dy,dz+az,18,4);
    }
    setting(mc,"tweaks.freeLook","true");setting(mc,"tweaks.freeLookToggle","false");setting(mc,"keys.key.powerbeta.free_look","34");
    setting(mc,"keys.power_building.hotbar.base","0");setting(mc,"keys.powerbeta.containerPreview","0");
    var settings=local.luke.power.building.config.Config.current().copy();settings.freeLookFollowThirdPerson=true;local.luke.power.building.config.Config.preview(settings);
    CreativeVehicleChecks.mode(mc,"SURVIVAL");camera(mc,.5,1.62,-3,0,0);mc.options.thirdPerson=true;mc.options.hideHud=false;
    mc.inGameHud.clearChat();
  }
}
