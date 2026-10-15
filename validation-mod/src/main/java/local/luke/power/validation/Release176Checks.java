package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import java.lang.reflect.Method;
import java.nio.file.Path;
import local.luke.power.autowalk.AutoWalk;
import local.luke.power.autowalk.WalkToggle;
import local.luke.power.building.config.Config;
import local.luke.power.config.SettingsRegistry;
import local.luke.power.input.*;
import local.luke.power.permissions.CheatWorld;
import local.luke.power.render.*;
import local.luke.power.visual.VisualConfig;
import net.minecraft.block.Material;
import net.minecraft.class_555;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

final class Release176Checks {
  private static double walkStart;
  private static int zoomKey;
  static void run(Minecraft mc, String action) throws Exception {
    if(action.equals("zoom-check")) {
      var key=(net.minecraft.client.option.KeyBinding)Class.forName("local.luke.power.controls.tweaks.morekeybinds.KeyBindingListener").getField("zoom").get(null);
      try {
        check(Bindings.down(key),"hold F6 in the focused test window");
        check(Math.abs(view(mc,0)[5]-1/Math.tan(Math.toRadians(10)))<.0001,"custom 90-degree FOV, water reduction and zoom did not compose");
        log("PASS actual zoom key composes custom FOV and underwater narrowing");
      } finally {key.code=zoomKey;}
      return;
    }
    if(action.equals("focus-check")) {
      check(!org.lwjgl.opengl.Display.isActive(),"test window must be unfocused");
      check(AutoWalk.isWalking() && mc.player.z-walkStart>1,"unfocused inventory auto-flight stopped");
      check(mc.player.field_161.field_2532==0 && !mc.player.field_161.field_2535,"unfocused camera keys moved player");
      log("PASS unfocused inventory auto-flight displacement="+(mc.player.z-walkStart));AutoWalk.stop();return;
    }
    if (action.equals("save") || action.equals("saved")) {
      var s=SettingsRegistry.open(mc);
      if (action.equals("saved")) {
        check(VisualConfig.current().underwaterDarkness==23 && VisualConfig.current().underwaterDarknessOutside==81
            && !VisualConfig.current().narrowUnderwaterFov,"new preferences did not survive restart");
        find(s,"visual.underwaterDarknessOutside").parse("0");find(s,"visual.narrowUnderwaterFov").parse("true");s.preview(true);s.discard();
        check(VisualConfig.current().underwaterDarknessOutside==81 && !VisualConfig.current().narrowUnderwaterFov,"Cancel lost persisted values");
      } else {
        find(s,"visual.underwaterDarkness").parse("23");find(s,"visual.underwaterDarknessOutside").parse("81");
        find(s,"visual.narrowUnderwaterFov").parse("false");s.save(Path.of(".").toAbsolutePath());
      }
      log("PASS RELEASE 176 persistence "+action);return;
    }
    failures=0;mc.setScreen(null);mc.paused=false;DeathChecks.alive(mc);FreecamPreviewChecks.active(false);
    var session=SettingsRegistry.open(mc);var original=VisualConfig.copy();var building=Config.current().copy();
    var cheats=(CheatWorld)mc.world.method_262();boolean oldCheats=cheats.power$cheatsEnabled();
    boolean third=mc.options.thirdPerson,bob=mc.options.bobView,stereo=mc.options.anaglyph3d;
    try {
      cheats.power$cheatsEnabled(true);RenderControls.reset(mc);mc.field_2807=mc.player;
      find(session,"power_camera:config.enabled").parse("true");find(session,"native.fov").parse("70");session.preview(true);
      CreativeVehicleChecks.mode(mc,"CREATIVE");mc.player.getClass().getMethod("creative_setFlying",boolean.class).invoke(mc.player,true);
      mc.options.bobView=false;mc.options.anaglyph3d=false;mc.player.hurtTime=0;mc.player.field_504=mc.player.field_505=0;
      if(action.equals("flight")) flight(mc);
      else if(action.equals("camera")) camera(mc);
      else if(action.startsWith("scene")) {scene(mc,action);return;}
      else throw new IllegalArgumentException(action);
    } finally {
      if(!action.startsWith("scene")) {
        FreecamPreviewChecks.active(false);AutoWalk.stop();mc.player.method_140();mc.setScreen(null);session.discard();
        Config.preview(building);VisualConfig.preview(original);mc.options.thirdPerson=third;mc.options.bobView=bob;mc.options.anaglyph3d=stereo;
        CreativeVehicleChecks.mode(mc,"SURVIVAL");cheats.power$cheatsEnabled(oldCheats);RenderedView.clear();RenderControls.reset(mc);
      }
    }
    log("RELEASE 176 "+action.toUpperCase()+" FAILURES "+failures);
  }

  private static void pool(Minecraft mc) {
    for(int x=20;x<=44;x++)for(int z=20;z<=44;z++)for(int y=107;y<=122;y++)
      mc.world.method_154(x,y,z,y==107?1:y<=113?9:0,0);
    mc.player.method_1340(32.5,111.62,32.5);
    mc.player.prevX=mc.player.x;mc.player.prevY=mc.player.y;mc.player.prevZ=mc.player.z;
    mc.player.yaw=mc.player.prevYaw=0;mc.player.pitch=mc.player.prevPitch=0;
  }

  private static void camera(Minecraft mc) throws Exception {
    pool(mc);var s=VisualConfig.copy();s.underwaterFogDensity=100;s.underwaterDarkness=0;s.underwaterDarknessOutside=100;s.narrowUnderwaterFov=true;VisualConfig.preview(s);
    test("actual third-person camera chooses fog, FOV and darkness without changing distance",()->{
      mc.options.thirdPerson=true;
      for(double distance:new double[]{2,4,8})for(float pitch:new float[]{0,80}) {
        s.thirdPersonDistance=distance;VisualConfig.preview(s);mc.player.pitch=mc.player.prevPitch=pitch;
        float[] projection=view(mc,0);boolean wet=RenderedView.y()<114;
        check(RenderedView.valid(mc.world,mc.player),"missing camera pose");
        check(mc.player.isInFluid(Material.WATER),"fixture player left water");
        double measured=Math.sqrt(Math.pow(RenderedView.x()-mc.player.x,2)+Math.pow(RenderedView.y()-mc.player.y,2)+Math.pow(RenderedView.z()-mc.player.z,2));
        check(Math.abs(measured-distance)<.001,"third person distance changed: "+measured+" wanted "+distance);
        check(CameraEnvironment.inFluid(mc.player,Material.WATER)==wet,"camera material disagrees with pose");
        check(Math.abs(projection[5]-1/Math.tan(Math.toRadians((wet?60:70)/2.0)))<.0001,"wrong projection FOV");
        CameraEnvironment.updateLighting(mc);check(CameraEnvironment.darkness()==(wet?0:100),"wrong darkness slider");
        fog(mc);check(GL11.glGetInteger(GL11.GL_FOG_MODE)==(wet?GL11.GL_EXP:GL11.GL_LINEAR),"fog followed player instead of camera");
        s.narrowUnderwaterFov=false;VisualConfig.preview(s);projection=view(mc,0);
        check(Math.abs(projection[5]-1/Math.tan(Math.toRadians(35)))<.0001,"disabled narrowing still zooms");
        s.narrowUnderwaterFov=true;VisualConfig.preview(s);
      }
    });
    test("first person, stereo and detached camera use their own water state",()->{
      mc.options.thirdPerson=false;mc.player.pitch=mc.player.prevPitch=0;
      for(boolean stereo:new boolean[]{false,true})for(int eye:new int[]{0,1}) {
        mc.options.anaglyph3d=stereo;check(Math.abs(view(mc,eye)[5]-1/Math.tan(Math.toRadians(30)))<.0001,"native first-person water FOV changed");
      }
      mc.options.anaglyph3d=false;FreecamPreviewChecks.active(true);
      Object c=Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
      for(double y:new double[]{116,111}) {
        c.getClass().getMethod("setCameraPosition",double.class,double.class,double.class).invoke(c,32.5,y,32.5);
        view(mc,0);view(mc,0);check(Math.abs(RenderedView.y()-y)<.001,"detached rendered position wrong");
        CameraEnvironment.updateLighting(mc);fog(mc);
        check(GL11.glGetInteger(GL11.GL_FOG_MODE)==(y<114?GL11.GL_EXP:GL11.GL_LINEAR),"detached fog followed body");
        check(CameraEnvironment.darkness()==(y<114?0:100),"detached darkness followed body");
      }
      FreecamPreviewChecks.active(false);
    });
    test("free look crosses the water surface independently of the player",()->{
      var old=Config.current().copy();var b=old.copy();b.freeLook=true;Config.preview(b);
      try {
        mc.options.thirdPerson=true;s.thirdPersonDistance=8;VisualConfig.preview(s);mc.player.pitch=mc.player.prevPitch=0;
        local.luke.power.building.camera.FreeLook.begin(mc);
        local.luke.power.building.camera.FreeLook.turn(mc.player,0,-500);
        view(mc,0);fog(mc);boolean wet=CameraEnvironment.inFluid(mc.player,Material.WATER);
        check(mc.player.pitch==0,"free look moved player pitch");
        check(GL11.glGetInteger(GL11.GL_FOG_MODE)==(wet?GL11.GL_EXP:GL11.GL_LINEAR),"free-look fog mismatch");
        check(RenderedView.y()>114,"free-look fixture did not raise camera: "+RenderedView.y());
      } finally {local.luke.power.building.camera.FreeLook.reset(mc);Config.preview(old);}
    });
    test("flowing-water surface and unloaded terrain are sampled without loading",()->{
      mc.world.method_154(32,120,32,9,7);
      check(CameraEnvironment.fluid(mc.world,32.5,120.1,32.5)==Material.WATER,"flowing water bottom missed");
      check(CameraEnvironment.fluid(mc.world,32.5,120.9,32.5)==null,"air above flowing water counted wet");
      check(CameraEnvironment.fluid(mc.world,30000000,100,30000000)==null,"unloaded terrain returned water");
    });
    test("outside slider and narrowing preview and Cancel are independent",()->{
      var session=SettingsRegistry.open(mc);var setting=find(session,"visual.underwaterDarknessOutside");
      check(setting.slider() && setting.defaultValue.getAsInt()==100,"outside slider metadata");
      setting.parse("39");find(session,"visual.narrowUnderwaterFov").parse("false");session.preview(true);
      check(VisualConfig.current().underwaterDarknessOutside==39 && VisualConfig.current().underwaterDarkness==0,"preview changed inside value");
      session.discard();check(VisualConfig.current().underwaterDarknessOutside==100 && VisualConfig.current().narrowUnderwaterFov,"Cancel failed");
    });
    test("water crossings reuse terrain meshes and do not duplicate queued work",()->{
      mc.options.thirdPerson=true;mc.player.pitch=mc.player.prevPitch=0;view(mc,0);CameraEnvironment.updateLighting(mc);
      Object meshes=field(mc.worldRenderer,"field_1809");
      mc.player.pitch=mc.player.prevPitch=80;view(mc,0);CameraEnvironment.updateLighting(mc);
      check(field(mc.worldRenderer,"field_1809")==meshes,"camera crossing recreated terrain meshes");
      var pending=(java.util.List<?>)field(mc.worldRenderer,"field_1807");int count=pending.size();
      for(int i=0;i<100;i++)CameraEnvironment.updateLighting(mc);
      check(pending.size()==count,"unchanged camera requeued terrain");
    });
    check(GL11.glGetError()==0,"camera GL error");
  }

  private static float[] view(Minecraft mc,int eye) throws Exception {
    Method method=class_555.class.getDeclaredMethod("method_1840",float.class,int.class);method.setAccessible(true);
    method.invoke(mc.field_2818,1F,eye);
    var b=BufferUtils.createFloatBuffer(16);GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX,b);float[] p=new float[16];b.get(p);return p;
  }
  private static void fog(Minecraft mc) throws Exception {
    for(String name:new String[]{"method_1852","method_1842"}) {
      Method m=name.equals("method_1852")?class_555.class.getDeclaredMethod(name,float.class):class_555.class.getDeclaredMethod(name,int.class,float.class);
      m.setAccessible(true);if(name.equals("method_1852"))m.invoke(mc.field_2818,1F);else m.invoke(mc.field_2818,1,1F);
    }
  }

  private static void flight(Minecraft mc) throws Exception {
    var settings=Config.current().copy();settings.autoWalk=true;settings.autoWalkCancelOnLostFocus=false;settings.inventoryWhileMoving=true;Config.preview(settings);
    AutoWalk.tick(mc);var f=AutoWalk.class.getDeclaredField("TOGGLE");f.setAccessible(true);var toggle=(WalkToggle)f.get(null);
    for(String mode:new String[]{"CREATIVE","SPECTATOR"})for(boolean backwards:new boolean[]{false,true})
      test(mode+" auto-flight continues in freecam, backwards="+backwards,()->{
        FreecamPreviewChecks.active(false);mc.setScreen(null);AutoWalk.stop();CreativeVehicleChecks.mode(mc,mode);
        mc.player.getClass().getMethod("creative_setFlying",boolean.class).invoke(mc.player,true);
        mc.player.method_1340(32.5,122,32.5);mc.player.yaw=mc.player.prevYaw=0;mc.player.velocityX=mc.player.velocityY=mc.player.velocityZ=0;
        toggle.update(false,true,false,0,350,backwards);toggle.update(true,true,false,0,350,backwards);toggle.update(false,true,false,0,350,backwards);
        Class.forName("local.luke.power.creative.ClientRuntime").getMethod("tick",Minecraft.class).invoke(null,mc);
        FreecamPreviewChecks.active(true);double start=mc.player.z,y=mc.player.y;
        double cx=DetachedCamera.x(),cy=DetachedCamera.y(),cz=DetachedCamera.z();
        for(int i=0;i<30;i++) {
          // Deliberately stale manual input must not leak into the player's flight.
          mc.player.field_161.field_2532=1;mc.player.field_161.field_2533=backwards?1:-1;
          mc.player.field_161.field_2535=true;mc.player.field_161.field_2536=false;
          mc.player.method_945(1,-1);
        }
        check((mc.player.z-start)*(backwards?-1:1)>1,"player did not actually fly");
        check(mc.player.x==32.5 && mc.player.y==y,"camera keys moved player sideways/vertically");
        check(DetachedCamera.x()==cx && DetachedCamera.y()==cy && DetachedCamera.z()==cz,"auto-flight moved camera");
        mc.setScreen(new ChatScreen());start=mc.player.z;mc.player.method_945(1,1);
        check(mc.player.z==start && !AutoWalk.isWalking(),"chat left auto-flight moving");
        mc.setScreen(null);FreecamPreviewChecks.active(false);mc.player.method_140();
      });
  }

  private static void scene(Minecraft mc,String action) throws Exception {
    pool(mc);mc.player.method_1340(32.5,113.8,32.5);mc.player.prevY=mc.player.y;
    mc.options.thirdPerson=true;mc.player.pitch=mc.player.prevPitch=action.endsWith("above")?12:0;
    var s=VisualConfig.copy();s.thirdPersonDistance=4;s.underwaterDarkness=0;s.underwaterDarknessOutside=100;s.underwaterFogDensity=100;s.narrowUnderwaterFov=false;
    s.removeUnderwaterOverlay=true;VisualConfig.preview(s);
    if(action.equals("scene-zoom")) {
      s.narrowUnderwaterFov=true;VisualConfig.preview(s);
      var session=SettingsRegistry.open(mc);find(session,"native.fov").parse("90");session.preview(true);
      var key=(net.minecraft.client.option.KeyBinding)Class.forName("local.luke.power.controls.tweaks.morekeybinds.KeyBindingListener").getField("zoom").get(null);
      zoomKey=key.code;key.code=64;Bindings.configure(java.util.Map.of());
    }
    if(action.equals("scene-walk")) {
      var settings=Config.current().copy();settings.autoWalk=true;settings.autoWalkCancelOnLostFocus=false;settings.inventoryWhileMoving=true;Config.preview(settings);
      mc.player.method_1340(32.5,122,32.5);mc.player.velocityX=mc.player.velocityY=mc.player.velocityZ=0;
      AutoWalk.tick(mc);var f=AutoWalk.class.getDeclaredField("TOGGLE");f.setAccessible(true);var toggle=(WalkToggle)f.get(null);
      toggle.stop();toggle.update(false,true);toggle.update(true,true);toggle.update(false,true);
      FreecamPreviewChecks.active(true);mc.setScreen(new net.minecraft.class_585(mc.player));walkStart=mc.player.z;
    }
    log("SCENE "+action);
  }
}
