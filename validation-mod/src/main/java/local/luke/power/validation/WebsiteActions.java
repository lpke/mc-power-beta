package local.luke.power.validation;

import static local.luke.power.validation.WebsiteGallery.*;
import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.Validation.*;
import java.util.*;
import local.luke.power.flexible.Keys;
import local.luke.power.visual.*;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.Inventory;
import org.lwjgl.input.Mouse;

/** Gameplay demonstrations use native inventories, vehicles, blocks and interaction paths. */
final class WebsiteActions {
  static void run(Minecraft mc,String action) throws Exception {
    if(action.startsWith("placement ")) {placement(mc,action.substring(10));return;}
    if(action.startsWith("roof-row ")) {roofRow(mc,Integer.parseInt(action.substring(9)));return;}
    if(action.startsWith("texture ")) {texture(mc,action.substring(8));return;}
    switch(action) {
      case "sponge" -> {
        WebsiteGallery.run(mc,"building");setting(mc,"power_gold:config.enableSpongeSoaksUpWater","true");
        for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++) {
          block(mc,dx,-1,dz,24,0);block(mc,dx,0,dz,Math.abs(dx)==4||Math.abs(dz)==4?24:9,0);
          for(int dy=1;dy<=3;dy++)block(mc,dx,dy,dz,0,0);
        }
        mc.player.inventory.main[0]=new ItemStack(19,64,0);mc.player.inventory.selectedSlot=0;
        aim(mc,.5,4,-3,.5,-.001,.5);mc.options.hideHud=false;
      }
      case "ledge" -> {
        WebsiteGallery.run(mc,"building");setting(mc,"tweaks.sneak.enabled","true");
        for(int dz=-7;dz<=8;dz++)block(mc,0,6,dz,5,0);
        CreativeVehicleChecks.mode(mc,"SURVIVAL");camera(mc,.5,8.62,-4,0,15);
        mc.player.getClass().getMethod("creative_setFlying",boolean.class).invoke(mc.player,false);
        mc.options.thirdPerson=true;mc.options.hideHud=false;
      }
      case "animals" -> {
        WebsiteGallery.run(mc,"building");
        for(int dx=-5;dx<=5;dx++)for(int dz=-5;dz<=5;dz++) {
          block(mc,dx,0,dz,2,0);block(mc,dx,1,dz,Math.abs(dx)==5||Math.abs(dz)==5?85:0,0);
        }
        String[] mobs={"cow","pig","sheep","chicken"};
        for(int i=0;i<8;i++)ChatChecks.submit(mc,"/summon "+mobs[i%4]+" "+(x-3+(i%4)*2)+" "+(y+1)+" "+(z-2+(i/4)*3));
        camera(mc,8,7,-10,37,25);mc.options.hideHud=true;mc.inGameHud.clearChat();
      }
      case "roof" -> roof(mc);
      case "roof-overview" -> {camera(mc,10,10,-13,39,28);mc.options.hideHud=true;}
      case "boat", "minecart" -> vehicle(mc,action.equals("boat"));
      case "dismount" -> {mc.player.method_1376(null);mc.options.thirdPerson=false;}
      case "inventory" -> {
        WebsiteGallery.run(mc,"preview");CreativeVehicleChecks.mode(mc,"SURVIVAL");camera(mc,.5,2,-2.5,0,12);mc.options.hideHud=false;
        var building=local.luke.power.building.config.Config.current().copy();building.inventoryWhileMoving=true;local.luke.power.building.config.Config.preview(building);
        for(int n=0;n<27;n++)mc.player.inventory.main[n+9]=new ItemStack(new int[]{1,5,4,20,17,35,50,44,260}[n%9],32,0);
        mc.player.inventory.main[0]=new ItemStack(257,1,30);mc.player.inventory.main[1]=new ItemStack(267,1,100);
        mc.player.inventory.main[2]=new ItemStack(306,1,12);mc.player.inventory.armor[3]=new ItemStack(306,1,80);
        mc.setScreen(new class_585(mc.player));Mouse.setCursorPosition(0,0);
      }
      case "equipment-before" -> {
        var v=VisualConfig.copy();v.swapEquipment=true;VisualConfig.preview(v);
        mc.player.inventory.setCursorStack(new ItemStack(306,1,12));
        mc.player.inventory.armor[3]=new ItemStack(306,1,80);Mouse.setCursorPosition(460,440);
      }
      case "equipment-swap" -> mc.player.playerContainer.onSlotClick(5,0,false,mc.player);
      case "transfer" -> {
        WebsiteGallery.run(mc,"preview");CreativeVehicleChecks.mode(mc,"SURVIVAL");camera(mc,.5,2,-2.5,0,12);
        Inventory chest=(Inventory)mc.world.method_1777(x,y,z);
        for(int i=0;i<27;i++)chest.setStack(i,new ItemStack(new int[]{1,5,4,20,17,35,50,44,260}[i%9],64,0));
        mc.player.method_486(chest);Mouse.setCursorPosition(0,0);
      }
      case "transfer-stack" -> {for(int i=0;i<9;i++)mc.player.container.onSlotClick(i,0,true,mc.player);}
      case "leaf" -> {
        WebsiteGallery.run(mc,"building");setting(mc,"power_controls:features.fastLeafDecay.enableFastLeafDecay","false");
        x=8496;y=72;z=2304;
        for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)mc.world.method_214((x>>4)+dx,(z>>4)+dz);
        for(int dx=-10;dx<=10;dx++)for(int dz=-10;dz<=10;dz++) {
          block(mc,dx,-1,dz,2,0);for(int dy=0;dy<15;dy++)block(mc,dx,dy,dz,0,0);
        }
        setting(mc,"power_controls:features.fastLeafDecay.minimumDecayTime","0.05");setting(mc,"power_controls:features.fastLeafDecay.maximumDecayTime","0.1");
        for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)for(int dy=3;dy<=5;dy++)
          if(Math.abs(dx)+Math.abs(dz)<4)block(mc,dx,dy,dz,18,0);
        for(int dy=0;dy<5;dy++)block(mc,0,dy,0,17,0);
        camera(mc,-6,3,-7,-40,0);mc.options.hideHud=true;
      }
      case "leaf-cut" -> {
        setting(mc,"power_controls:features.fastLeafDecay.enableFastLeafDecay","true");
        for(int dy=0;dy<5;dy++)mc.interactionManager.method_1716(x,y+dy,z,2);
        // Start one ordinary leaf tick; the installed feature schedules the cascade.
        mc.world.method_216(x,y+5,z,18,1);
      }
      case "gold" -> {
        WebsiteGallery.run(mc,"building");CreativeVehicleChecks.mode(mc,"SURVIVAL");
        setting(mc,"power_gold:config.enableGoldPickaxeSilkTouch","true");
        for(int dx=-3;dx<=3;dx++)for(int dz=0;dz<=2;dz++)block(mc,dx,0,dz,dx<0?56:dx<2?73:14,0);
        mc.player.inventory.main[0]=new ItemStack(285,1,0);mc.player.inventory.selectedSlot=0;
        aim(mc,.5,2.7,-2,.5,.5,.01);mc.options.hideHud=false;
      }
      case "gold-mine" -> {mc.interactionManager.method_1716(x,y,z,2);}
      case "water" -> {
        WebsiteGallery.run(mc,"building");setting(mc,"power_mechanics:config.MOB_CONFIG.waterNegatesFallDamage","true");
        for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)block(mc,dx,0,dz,9,0);
        setting(mc,"tweaks.sneak.enabled","false");setting(mc,"tweaks.placement.enabled","false");
        block(mc,0,32,-1,5,0);
        CreativeVehicleChecks.mode(mc,"SURVIVAL");mc.player.health=20;
        // Start on a diving ledge so recording includes the fall, not only the landing.
        camera(mc,.5,34.62,-.5,0,65);mc.player.inventory.selectedSlot=8;mc.options.hideHud=false;
      }
      case "water-drop" -> mc.player.getClass().getMethod("creative_setFlying",boolean.class).invoke(mc.player,false);
      case "fire" -> {WebsiteGallery.run(mc,"building");CreativeVehicleChecks.mode(mc,"SURVIVAL");mc.player.health=20;mc.player.fire=400;mc.options.hideHud=false;}
      case "explosion-items" -> {
        WebsiteDetails.run(mc,"items");var v=local.luke.power.gameplay.GameplayConfig.copy();AccessRules.set(mc, local.luke.power.rules.GameRule.EXPLOSIONS_DESTROY_ITEMS, false);local.luke.power.gameplay.GameplayConfig.preview(v);
        camera(mc,1,4,-7,0,30);mc.options.hideHud=true;
      }
      case "explosion-start" -> {var tnt=new class_365(mc.world,x+.5,y+.5,z-1.5);tnt.field_1390=25;mc.world.method_210(tnt);}
      default -> throw new IllegalArgumentException(action);
    }
  }
  static void aim(Minecraft mc,double px,double py,double pz,double tx,double ty,double tz) throws Exception {
    camera(mc,px,py,pz,(float)Math.toDegrees(Math.atan2(-(tx-px),tz-pz)),(float)Math.toDegrees(Math.atan2(py-ty,Math.hypot(tx-px,tz-pz))));
  }
  private static void texture(Minecraft mc,String kind) throws Exception {
    if(kind.equals("scene")) {WebsiteGallery.run(mc,"textures");setting(mc,"native.texturePack","Default");camera(mc,1.5,2,-3,0,0);return;}
    var session=local.luke.power.config.SettingsRegistry.open(mc);
    for(String id:List.of("oldCobble","oldBricks","softRain","softSnow","lowFire"))find(session,"visual."+id).parse(String.valueOf(kind.equals("old")&&(id.equals("oldCobble")||id.equals("oldBricks"))||kind.equals("soft")&&(id.equals("softRain")||id.equals("softSnow"))||kind.equals("low")&&id.equals("lowFire")));
    session.save(java.nio.file.Path.of("."));TextureOverrides.clear();mc.textureManager.method_1096();mc.worldRenderer.method_1537();
  }
  private static void placement(Minecraft mc,String kind) throws Exception {
    if(kind.equals("offset")) {WebsiteDetails.run(mc,"placement");return;}
    WebsiteGallery.run(mc,"building");
    var settings=local.luke.power.building.config.Config.current().copy();settings.placement.enabled=false;settings.flexible.enabled=settings.flexible.showOverlay=settings.flexible.showPreview=true;local.luke.power.building.config.Config.preview(settings);
    for(int i=1;i<Keys.ALL.length;i++)Keys.ALL[i].code=0;
    int index=switch(kind){case "adjacent"->2;case "rotation"->3;case "reverse"->4;case "into"->5;default->throw new IllegalArgumentException(kind);};Keys.ALL[index].code=34;
    if(kind.equals("adjacent")) {
      for(int dx=-3;dx<=3;dx++)for(int dz=0;dz<=5;dz++)if(!(dx==1&&dz==0))block(mc,dx,1,dz,5,0);
      for(int dz=1;dz<=5;dz++){block(mc,-3,2,dz,85,0);block(mc,3,2,dz,85,0);}
      mc.player.inventory.main[0]=new ItemStack(5,64,0);aim(mc,2.3,4,-2.5,.95,2.001,.5);
    } else {
      for(int dx=-3;dx<=3;dx++)for(int dz=0;dz<=3;dz++)block(mc,dx,0,dz,1,0);
      int item=kind.equals("reverse")?53:33;
      for(int dx=-3;dx<=3;dx+=2) {block(mc,dx,1,2,item,kind.equals("into")?0:kind.equals("reverse")?3:5);if(item==33)block(mc,dx,0,3,76,5);}
      mc.player.inventory.main[0]=new ItemStack(item,64,0);
      if(kind.equals("into"))aim(mc,.5,3.4,-2,.5,1.001,.5);
      else {block(mc,0,1,0,1,0);aim(mc,.5,3,-2.5,.95,1.5,-.001);}
    }
    mc.player.inventory.selectedSlot=0;mc.options.hideHud=false;
  }
  private static void roof(Minecraft mc) throws Exception {
    WebsiteGallery.run(mc,"building");
    for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)for(int dy=0;dy<=4;dy++)
      if(dy==0||Math.abs(dx)==4||Math.abs(dz)==4)block(mc,dx,dy,dz,dy==0||dy==4?5:(dy==2||dy==3)&&Math.abs(dx)!=4?20:17,0);
    for(int dz=-3;dz<=3;dz++)block(mc,4,4,dz,0,0);
    setting(mc,"creative.blockReach","10");
    var s=local.luke.power.building.config.Config.current().copy();s.placement.enabled=true;s.placement.attemptsPerTick=8;s.placement.restrictionEnabled=true;s.placement.restrictionMode=local.luke.power.fastplace.RestrictionMode.LAYER;s.flexible.enabled=false;local.luke.power.building.config.Config.preview(s);
    mc.player.inventory.main[0]=new ItemStack(5,64,0);mc.player.inventory.selectedSlot=0;mc.options.hideHud=false;
    var status=local.luke.power.status.StatusConfig.copy();status.enabled=true;local.luke.power.status.StatusConfig.preview(status);roofRow(mc,0);
  }
  private static void roofRow(Minecraft mc,int n) throws Exception {
    // Aim at a roof edge; native fast placement fills each row while Use is held.
    double dz=-3+n;aim(mc,5.5,4.6,dz+.5,-3.001,4.6,dz+.5);
  }
  private static void vehicle(Minecraft mc,boolean boat) throws Exception {
    WebsiteGallery.run(mc,"building");x=boat?4608:4864;z=boat?1536:1792;y=104;
    for(Object value:new ArrayList<>(mc.world.field_198)) {
      if(value instanceof net.minecraft.entity.Entity entity && (entity instanceof class_113 || entity instanceof class_549))entity.markDead();
    }
    for(int dx=-2;dx<=2;dx++)for(int dz=-7;dz<=7;dz++)mc.world.method_214((x>>4)+dx,(z>>4)+dz);
    int half=boat?8:3;
    for(int dx=-half;dx<=half;dx++)for(int dz=-90;dz<=90;dz++) {
      block(mc,dx,-1,dz,boat?12:4,0);block(mc,dx,0,dz,boat?9:5,0);
      for(int dy=1;dy<128-y;dy++)block(mc,dx,dy,dz,0,0);
      if(!boat&&dx==0)block(mc,dx,1,dz,27,8);
      if(!boat&&dx==1&&dz%6==0)block(mc,dx,1,dz,76,5);
      if(Math.abs(dx)==half){block(mc,dx,0,dz,boat?4:5,0);block(mc,dx,1,dz,85,0);
        if(dz%12==0)block(mc,dx,2,dz,50,5);
        if(dz%18==0)for(int dy=-2;dy>=-24;dy--)block(mc,dx,dy,dz,4,0);
      }
    }
    var s=local.luke.power.building.config.Config.current().copy();s.boatSpeed=s.boatProtection=s.boatSteering=s.fastMinecarts=true;local.luke.power.building.config.Config.preview(s);
    setting(mc,"native.clouds","false");
    mc.player.inventory.selectedSlot=8;
    var status=local.luke.power.status.StatusConfig.copy();status.enabled=false;local.luke.power.status.StatusConfig.preview(status);
    camera(mc,.5,3,-65.5,0,8);mc.player.getClass().getMethod("creative_setFlying",boolean.class).invoke(mc.player,false);
    net.minecraft.entity.Entity vehicle=boat?new class_113(mc.world,x+.5,y+.5,z-65.5):new class_549(mc.world,x+.5,y+1.5,z-65.5,0);
    vehicle.yaw=vehicle.prevYaw=boat?-90:90;mc.world.method_210(vehicle);mc.player.method_1376(vehicle);
    mc.options.hideHud=false;mc.options.thirdPerson=true;
  }
}
