package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import java.nio.file.*;
import local.luke.power.config.*;
import local.luke.power.light.*;
import local.luke.power.visual.*;
import local.luke.power.worldedit.*;
import local.luke.power.worldedit.core.*;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

/** Separate real-renderer scenes for the release gallery. Disposable worlds only. */
final class Release150Gallery {
  private static int x,y,z;
  private static void setting(Minecraft mc,String id,String value) throws Exception {
    var session=SettingsRegistry.open(mc);find(session,id).parse(value);session.save(Path.of(".").toAbsolutePath());
  }
  private static void block(Minecraft mc,int dx,int dy,int dz,int id,int meta) {
    new MinecraftWorld(mc.world).set(new Pos(x+dx,y+dy,z+dz),new BlockValue(id,meta));
  }
  private static void camera(Minecraft mc,double dx,double dy,double dz,float yaw,float pitch) throws Exception {
    mc.setScreen(null);mc.player.getClass().getMethod("creative_setFlying",boolean.class).invoke(mc.player,true);
    mc.player.method_1340(x+dx,y+dy,z+dz);
    mc.player.prevX=mc.player.field_1637=mc.player.x;mc.player.prevY=mc.player.field_1638=mc.player.y;mc.player.prevZ=mc.player.field_1639=mc.player.z;
    mc.player.yaw=mc.player.prevYaw=yaw;mc.player.pitch=mc.player.prevPitch=pitch;
    mc.player.velocityX=mc.player.velocityY=mc.player.velocityZ=0;
  }
  static void run(Minecraft mc,String action) throws Exception {
    if(action.startsWith("capture ")) {
      Path folder=Path.of("power-beta-data/reports/gallery-150");Files.createDirectories(folder);
      String result=class_260.method_908(folder.toFile(),mc.displayWidth,mc.displayHeight);
      Path latest;
      try(var files=Files.list(folder.resolve("screenshots"))) {latest=files.max(java.util.Comparator.comparing(p->p.getFileName().toString())).orElseThrow();}
      Files.move(latest,folder.resolve(action.substring(8)+".png"),StandardCopyOption.REPLACE_EXISTING);
      log("GALLERY CAPTURE "+result);return;
    }
    if(action.equals("inventory")) {
      mc.setScreen(new class_585(mc.player));field(mc.currentScreen,"creative_normalGUI",false);org.lwjgl.input.Mouse.setCursorPosition(0,0);return;
    }
    if(action.equals("music")) {
      var audio=local.luke.power.audio.AudioConfig.copy();audio.master=70;local.luke.power.audio.AudioConfig.preview(audio);
      mc.options.musicVolume=.2f;mc.soundManager.method_2008();local.luke.power.audio.AudioController.next();
      local.luke.power.ui.PowerOptionsScreen.openMusic(mc,null,false,null);org.lwjgl.input.Mouse.setCursorPosition(0,0);return;
    }
    if(action.equals("isometric")) {
      for(int pass=0;pass<4096 && !mc.worldRenderer.method_1549(mc.field_2807,false);pass++) {}
      var type=Class.forName("local.luke.power.capture.IsometricScreenshotRenderer");
      var renderer=type.getConstructor(Minecraft.class,java.io.File.class).newInstance(mc,Path.of(".").toFile());
      type.getMethod("doRender").invoke(renderer);return;
    }
    (AccessRules.of(mc.world.method_262())).allAllowed(true);CreativeVehicleChecks.mode(mc,"CREATIVE");
    mc.setScreen(null);mc.options.hideHud=false;mc.options.thirdPerson=false;
    var visuals=VisualConfig.copy();visuals.hideEntities=false;visuals.spectatorFullBright=false;visuals.fullBrightMessages=visuals.entityRenderingMessages=false;visuals.redstonePowerLevels=false;VisualConfig.preview(visuals);
    var light=LightConfig.copy();light.enabled=false;LightConfig.preview(light);
    var status=local.luke.power.status.StatusConfig.copy();status.enabled=false;local.luke.power.status.StatusConfig.preview(status);
    // Moving to a new scene leaves the previous selection outside the camera.
    setting(mc,"native.guiScale","2");setting(mc,"native.renderDistance","8");setting(mc,"native.brightness","60");
    setting(mc,"native.fov","75");
    java.util.Arrays.fill(mc.player.inventory.main,null);
    mc.player.inventory.main[0]=new ItemStack(1,64,0);mc.player.inventory.main[1]=new ItemStack(5,64,0);
    mc.player.inventory.main[2]=new ItemStack(20,64,0);mc.player.inventory.main[3]=new ItemStack(50,64,0);
    mc.player.inventory.selectedSlot=8;
    x=switch(action) {case "creative-scene"->192;case "music-scene"->384;case "bridge"->576;case "redstone"->768;case "light"->960;case "cavern"->1152;default->1344;};
    z=switch(action) {case "creative-scene"->192;case "music-scene"->-192;case "bridge"->384;case "redstone"->-384;case "light"->576;case "cavern"->-576;default->768;};
    for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)mc.world.method_214((x>>4)+dx,(z>>4)+dz);
    y=100;while(y>62&&mc.world.getBlockId(x,y-1,z)==0)y--;
    ChatChecks.submit(mc,"/time set noon");ChatChecks.submit(mc,"/weather clear");
    if(action.equals("creative-scene")) {camera(mc,0,10,0,125,12);mc.options.hideHud=true;}
    else if(action.equals("music-scene")) {camera(mc,0,6,0,235,6);mc.options.hideHud=true;ChatChecks.submit(mc,"/time set 11500");}
    else if(action.equals("bridge")) {
      for(int dx=-14;dx<=14;dx++)for(int dz=-9;dz<=9;dz++) {
        block(mc,dx,-2,dz,12,0);block(mc,dx,-1,dz,9,0);
        for(int dy=0;dy<9;dy++)block(mc,dx,dy,dz,0,0);
      }
      for(int dx=-10;dx<=10;dx++)for(int dz=-2;dz<=2;dz++) {
        int h=2+(int)Math.round(2*Math.cos(dx*Math.PI/20));
        block(mc,dx,h,dz,4,0);if(Math.abs(dz)==2)block(mc,dx,h+1,dz,85,0);
        if(dx%5==0)for(int dy=0;dy<h;dy++)block(mc,dx,dy,dz,4,0);
      }
      ChatChecks.submit(mc,"//pos1 "+(x-10)+","+y+","+(z-2));ChatChecks.submit(mc,"//pos2 "+(x+10)+","+(y+6)+","+(z+2));
      camera(mc,18,12,-20,42,22);mc.options.hideHud=true;
    } else if(action.equals("redstone")) {
      for(int dx=-10;dx<=10;dx++)for(int dz=-5;dz<=5;dz++) {
        block(mc,dx,-1,dz,24,0);for(int dy=0;dy<7;dy++)block(mc,dx,dy,dz,0,0);
      }
      for(int dx=-7;dx<=7;dx++)block(mc,dx,0,0,55,0);
      mc.world.method_201(x-8,y,z,76,5);mc.world.method_244(x-8,y,z,76);block(mc,4,0,1,55,0);block(mc,4,0,2,55,0);
      visuals=VisualConfig.copy();visuals.redstonePowerLevels=true;VisualConfig.preview(visuals);mc.worldRenderer.method_1537();
      camera(mc,-2,4,-4,-20,50);mc.options.hideHud=true;
    } else if(action.equals("light")||action.equals("cavern")) {
      y=38;
      for(int dx=-10;dx<=10;dx++)for(int dz=-12;dz<=12;dz++)for(int dy=-1;dy<=7;dy++) {
        boolean shell=Math.abs(dx)==10||Math.abs(dz)==12||dy==-1||dy==7;
        block(mc,dx,dy,dz,shell ? 1 : 0,0);
      }
      for(int dz=-9;dz<=9;dz+=6)for(int dy=0;dy<=6;dy++) {block(mc,-8,dy,dz,4,0);block(mc,8,dy,dz,4,0);}
      if(action.equals("light")) {
        block(mc,-4,0,2,50,0);light=LightConfig.copy();light.enabled=true;light.radius=12;light.verticalRange=4;light.textSize=.55;LightConfig.preview(light);
        camera(mc,4,4,-7,15,35);
      } else {
        block(mc,0,0,9,56,0);block(mc,1,0,9,56,0);block(mc,-4,0,8,14,0);block(mc,3,0,8,16,0);
        camera(mc,0,2,-8,0,3);visuals=VisualConfig.copy();visuals.spectatorFullBright=true;VisualConfig.preview(visuals);
        CreativeVehicleChecks.mode(mc,"SPECTATOR");local.luke.power.render.RenderControls.tick(mc);
        status=local.luke.power.status.StatusConfig.copy();status.enabled=true;local.luke.power.status.StatusConfig.preview(status);
      }
    } else {camera(mc,0,30,0,35,28);mc.options.hideHud=true;}
    mc.inGameHud.clearChat();
    log("GALLERY SCENE "+action+" at "+x+","+y+","+z);
  }
}
