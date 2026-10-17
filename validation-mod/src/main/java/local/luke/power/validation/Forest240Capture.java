package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import net.minecraft.client.Minecraft;

/** Locate unedited native forest terrain. No terrain is constructed for the asset. */
final class Forest240Capture {
  static void run(Minecraft mc,String action)throws Exception{
    if(mc.world==null){
      mc.interactionManager=new net.minecraft.SingleplayerInteractionManager(mc);
      mc.method_2120("Power Beta 2.4 forest capture","Power Beta forest",17320261003L);
      net.minecraft.client.render.entity.EntityRenderDispatcher.field_2489.method_1917(mc.world,mc.textureManager,mc.textRenderer,mc.player,mc.options,0F);
    }
    AccessRules.of(mc.world.method_262()).allAllowed(true);CreativeVehicleChecks.mode(mc,"CREATIVE");
    WebsiteGallery.setting(mc,"native.renderDistance","12");WebsiteGallery.setting(mc,"native.fov","70");WebsiteGallery.setting(mc,"native.fogDensity","100");
    WebsiteGallery.setting(mc,"power_controls:userinterface.hideAchievementToast","false");
    int x=0,z=0,y=64;boolean found=false;
    outer:for(int radius=64;radius<=1024;radius+=64)for(int bx=-radius;bx<=radius;bx+=32)for(int bz=-radius;bz<=radius;bz+=32){
      boolean interior=true;
      for(int dx=-32;dx<=32;dx+=32)for(int dz=-32;dz<=32;dz+=32)
        if(!mc.world.method_1781().method_1787(bx+dx,bz+dz).field_888.equals("Forest"))interior=false;
      if(!interior)continue;
      for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)mc.world.method_214((bx>>4)+dx,(bz>>4)+dz);
      for(int by=95;by>=62;by--)if(mc.world.getBlockId(bx,by,bz)==2&&mc.world.getBlockId(bx,by+1,bz)==0&&mc.world.getBlockId(bx,by+2,bz)==0){
        boolean clear=true;int leaves=0;
        for(int dx=-12;dx<=12;dx+=4)for(int dz=-12;dz<=12;dz+=4)for(int dy=-3;dy<=12;dy++){
          int id=mc.world.getBlockId(bx+dx,by+dy,bz+dz);
          if(id==78||id==80)clear=false;
          if(id==18)leaves++;
          if(Math.abs(dx)<=4&&Math.abs(dz)<=4&&dy>=2&&dy<=4&&id!=0)clear=false;
        }
        if(clear&&leaves>=12){x=bx;y=by;z=bz;found=true;break outer;}
      }
    }
    check(found,"No natural forest clearing found");WebsiteGallery.x=x;WebsiteGallery.y=y;WebsiteGallery.z=z;
    ChatChecks.submit(mc,"/time set noon");ChatChecks.submit(mc,"/weather clear");
    mc.options.thirdPerson=false;mc.options.hideHud=true;mc.options.ao=true;
    WebsiteGallery.camera(mc,.5,2.62,.5,-35,3);mc.inGameHud.clearChat();
    log("NATURAL FOREST CAPTURE biome Forest, terrain unedited, 12 chunks, noon, clear, first person, HUD hidden; position "+x+","+y+","+z);
  }
}
