package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.*;
import local.luke.power.gameplay.*;
import local.luke.power.input.*;
import local.luke.power.rules.RuleSet;
import local.luke.power.sprint.SprintController;
import local.luke.power.storage.PowerConfig;
import local.luke.power.visual.VisualConfig;
import net.minecraft.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Box;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Real pose ticks and native item/block rays, without placing items or changing inventory. */
public final class CameraTarget448Checks {
  private record Ray(double x,double y,double z) {}
  private static int captureKind;
  private static Ray captured;

  public static boolean capture(int kind,Vec3d start) {
    if (captureKind!=kind) return false;
    captured=new Ray(start.x,start.y,start.z);
    return true;
  }

  static void run(Minecraft mc) throws Exception {
    check(mc.world!=null && !mc.world.isRemote && mc.player!=null && Display.isActive()
        && !DetachedCamera.isActive(),"focus a muted disposable singleplayer world outside freecam");
    check(mc.options.soundVolume==0 && mc.options.musicVolume==0,"mute the disposable client");
    failures=0;
    var owner=mc.player; var camera=mc.field_2807; var screen=mc.currentScreen; var hit=mc.field_2823;
    boolean paused=mc.paused,bob=mc.options.bobView,third=mc.options.thirdPerson,stereo=mc.options.anaglyph3d;
    var gameplay=GameplayConfig.copy(); var visual=VisualConfig.copy(); var rules=AccessRules.copy(mc);
    byte[] disk=Files.readAllBytes(PowerConfig.path());
    int x=((int)Math.floor(owner.x)>>4)*16+8,z=((int)Math.floor(owner.z)>>4)*16+8,y=112;
    check(mc.world.method_155(x-3,y-1,z-3,x+3,y+4,z+5),"camera scene requires loaded terrain");
    var blocks=new ArrayList<int[]>();
    for(int bx=x-2;bx<=x+2;bx++)for(int bz=z-2;bz<=z+4;bz++)for(int by=y-1;by<=y+3;by++) {
      check(mc.world.method_1777(bx,by,bz)==null,"camera scene intersects a block entity");
      blocks.add(new int[]{bx,by,bz,mc.world.getBlockId(bx,by,bz),mc.world.method_1778(bx,by,bz)});
    }
    var probe=new ClientPlayerEntity(mc,mc.world,mc.session,owner.dimensionId);
    try {
      mc.player=probe;mc.field_2807=probe;mc.setScreen(null);mc.paused=false;
      mc.options.bobView=mc.options.thirdPerson=mc.options.anaglyph3d=false;
      AccessRules.restore(mc,RuleSet.cheats());CreativeVehicleChecks.mode(mc,"SURVIVAL");
      for(boolean lower:new boolean[]{false,true})test("native logical eyes follow real crouch transitions, lower="+lower,()->{
        scene(mc,x,y,z);reset(mc,x,y,z);
        var settings=GameplayConfig.copy();settings.modernCrawling=true;settings.modernWaterMovement=true;
        settings.crouchCameraHeight=lower;GameplayConfig.preview(settings);
        for(int tick=0;tick<48;tick++) {
          probe.field_161.field_2536=tick<24;probe.tick();
          near(probe.boundingBox.minY,y,1e-5,"pose tick moved collision feet");
          for(float delta:new float[]{0,.25f,.5f,.75f,1})checkOrigins(mc,delta);
        }
      });
      test("forced crouch and dry compact transitions retain one native interaction eye",()->{
        scene(mc,x,y,z);reset(mc,x,y,z);
        var settings=GameplayConfig.copy();settings.modernCrawling=true;settings.crouchCameraHeight=false;
        GameplayConfig.preview(settings);
        mc.world.method_154(x,y,z,44,0);mc.world.method_154(x,y+2,z,1,0);
        probe.method_1341(x+.5,y+.5,z+.5,0,0);
        for(int tick=0;tick<24;tick++){probe.tick();checkOrigins(mc,.5f);}
        check(ModernSwimming.crouched(probe),"low roof did not force crouching");
        scene(mc,x,y,z);reset(mc,x,y,z);mc.world.method_154(x,y+1,z,96,0);
        for(int tick=0;tick<24;tick++){probe.tick();checkOrigins(mc,.5f);}
        check(ModernSwimming.compact(probe),"closed trapdoor did not force compact pose");
        mc.world.method_154(x,y+1,z,0,0);
        for(int tick=0;tick<24;tick++){probe.tick();checkOrigins(mc,.5f);}
      });
      test("native entity picks cover crouch and first compact frame without extending reach or passing solid blocks",()->{
        check(mc.interactionManager instanceof SingleplayerInteractionManager,"use native singleplayer entity targeting");
        var settings=GameplayConfig.copy();settings.modernCrawling=true;settings.crouchCameraHeight=false;
        GameplayConfig.preview(settings);
        scene(mc,x,y,z);reset(mc,x,y,z);probe.field_161.field_2536=true;
        for(int tick=0;tick<24;tick++)probe.tick();
        entityPick(mc,x,y,z,1,false);
        scene(mc,x,y,z);reset(mc,x,y,z);mc.world.method_154(x,y+1,z,96,0);probe.tick();
        check(ModernSwimming.compact(probe),"first compact-frame fixture did not resize");
        entityPick(mc,x,y,z,0,true);
        entityPick(mc,x,y,z,.5f,false);
      });
      test("native item fallbacks survive foreign, dead and multiplayer pose guards",()->{
        scene(mc,x,y,z);reset(mc,x,y,z);probe.field_161.field_2536=true;
        for(int tick=0;tick<24;tick++)probe.tick();
        var other=new ClientPlayerEntity(mc,mc.world,mc.session,owner.dimensionId);
        var state=((SwimPlayer)other).power$swim();state.crouched=true;state.resetCamera(1.62f);other.field_1631=1.27f;
        near(InteractionEye.offset(other,1,.7),.7,0,"foreign pose changed native fallback");
        near(InteractionEye.offset(null,1,.7),.7,0,"missing owner changed native fallback");
        probe.dead=true;try {checkNativeFallbacks(mc);}finally {probe.dead=false;}
        mc.world.isRemote=true;try {checkNativeFallbacks(mc);}finally {mc.world.isRemote=false;}
      });
      test("cold stereo projection uses rendered water state after the crouch camera transform",()->{
        scene(mc,x,y,z);reset(mc,x,y,z);
        var settings=GameplayConfig.copy();settings.modernCrawling=true;settings.crouchCameraHeight=false;
        GameplayConfig.preview(settings);probe.field_161.field_2536=true;
        for(int tick=0;tick<24;tick++)probe.tick();
        for(int bx=x-2;bx<=x+2;bx++)for(int bz=z-2;bz<=z+2;bz++)for(int by=y;by<=y+1;by++)
          mc.world.method_154(bx,by,bz,9,0);
        probe.method_1340(x+.5,y+1.9,z+.5);probe.prevX=probe.x;probe.prevY=probe.y;probe.prevZ=probe.z;
        probe.field_1637=probe.x;probe.field_1638=probe.y;probe.field_1639=probe.z;
        check(probe.isInFluid(net.minecraft.block.Material.WATER),"physical-eye water fixture missed the surface");
        mc.options.anaglyph3d=true;
        for(int eye=0;eye<2;eye++) {
          var options=VisualConfig.copy();options.narrowUnderwaterFov=false;VisualConfig.preview(options);
          float nativeProjection=projection(mc,eye);
          options.narrowUnderwaterFov=true;VisualConfig.preview(options);
          near(projection(mc,eye),nativeProjection,1e-6,"FOV narrowed at the submerged physical eye");
          check(RenderedView.y()>y+2,"rendered camera did not clear the water");
        }
      });
      test("camera checks preserve saved preferences",()->check(Arrays.equals(disk,Files.readAllBytes(PowerConfig.path())),"camera checks wrote preferences"));
    } finally {
      captureKind=0;captured=null;mc.world.isRemote=false;
      for(int[] b:blocks)mc.world.method_154(b[0],b[1],b[2],b[3],b[4]);
      GameplayConfig.preview(gameplay);VisualConfig.preview(visual);AccessRules.restore(mc,rules);
      mc.player=owner;mc.field_2807=camera;mc.field_2823=hit;mc.options.bobView=bob;mc.options.thirdPerson=third;
      mc.options.anaglyph3d=stereo;mc.paused=paused;mc.setScreen(screen);SprintController.reset();RenderedView.clear();
    }
    log("CAMERA TARGET 448 FAILURES "+failures);
  }

  private static void checkOrigins(Minecraft mc,float delta) throws Exception {
    var p=mc.player;double raw=p.prevY+(p.y-p.prevY)*delta;
    near(p.method_931(delta).y,raw,1e-9,"render-position method changed its physical origin");
    double logical=raw+InteractionEye.offset(p,delta,0);
    near(logical,cameraY(mc,delta),2e-5,"interaction eye differs from native camera");
    captureKind=1;captured=null;
    try {p.method_929(4,delta);check(captured!=null,"native block ray was not observed");near(captured.y,logical,1e-6,"native block ray eye");}
    finally {captureKind=0;}
    checkItems(mc,p.y+InteractionEye.offset(p,1,0));
  }

  private static void checkNativeFallbacks(Minecraft mc) throws Exception {
    near(InteractionEye.offset(mc.player,1,.7),.7,0,"ineligible pose changed native fallback");
    checkItems(mc,mc.player.y+1.62-mc.player.field_1631);
  }

  private static void entityPick(Minecraft mc,int x,int y,int z,float delta,boolean aboveNativeBounds) throws Exception {
    var player=mc.player;double eyeY=player.prevY+(player.y-player.prevY)*delta+InteractionEye.offset(player,delta,0);
    var target=new class_113(mc.world) {@Override public float method_1369(){return 0;}};
    target.spacingXZ=.2f;target.spacingY=.02f;target.field_1631=target.field_1640=0;
    target.method_1340(player.x,eyeY-.01,player.z+3.09);
    if(aboveNativeBounds) {
      check(target.boundingBox.minY>player.boundingBox.maxY+1,"compact target still intersects the native candidate query");
      Box originalQuery=player.boundingBox.add(0,0,3).expand(1,1,1);
      check(!mc.world.getEntities(player,originalQuery).contains(target),"unregistered target leaked into native query");
    }
    check(mc.world.method_210(target),"loaded entity-target fixture failed to spawn");
    double[] body=bounds(player.boundingBox);
    try {
      if(aboveNativeBounds) {
        Box originalQuery=player.boundingBox.add(0,0,3).expand(1,1,1);
        check(!mc.world.getEntities(player,originalQuery).contains(target),"native physical query already included the airborne entity");
      }
      mc.field_2818.method_1838(delta);
      check(mc.field_2823!=null && mc.field_2823.field_1983==class_212.ENTITY && mc.field_2823.field_1989==target,
          "native eye ray omitted the entity just inside its three-block reach");
      target.method_1340(player.x,eyeY-.01,player.z+3.12);
      mc.field_2818.method_1838(delta);
      check(mc.field_2823==null || mc.field_2823.field_1989!=target,"query extension increased native entity reach");
      target.method_1340(player.x,eyeY-.01,player.z+3.09);
      int blockY=(int)Math.floor(eyeY),blockZ=z+2;
      int previous=mc.world.getBlockId(x,blockY,blockZ),metadata=mc.world.method_1778(x,blockY,blockZ);
      mc.world.method_154(x,blockY,blockZ,1,0);
      try {
        mc.field_2818.method_1838(delta);
        check(mc.field_2823!=null && mc.field_2823.field_1983==class_212.TILE
            && mc.field_2823.field_1984==x && mc.field_2823.field_1985==blockY && mc.field_2823.field_1986==blockZ,
            "native entity pick passed through a nearer solid block");
      } finally {mc.world.method_154(x,blockY,blockZ,previous,metadata);}
      check(Arrays.equals(body,bounds(player.boundingBox)),"entity query changed the physical player box");
    } finally {
      target.markDead();mc.world.method_199((int)Math.floor(target.x),(int)Math.floor(target.z)).method_877(target);
      mc.world.method_291().remove(target);
      Method unload=net.minecraft.world.World.class.getDeclaredMethod("method_226",Entity.class);
      unload.setAccessible(true);unload.invoke(mc.world,target);
    }
  }

  private static double[] bounds(Box box) {
    return new double[]{box.minX,box.minY,box.minZ,box.maxX,box.maxY,box.maxZ};
  }

  private static void checkItems(Minecraft mc,double expectedY) throws Exception {
    for(Item item:new Item[]{Item.field_449,Item.field_450,Item.field_405}) {
      ItemStack stack=new ItemStack(item,1);captureKind=2;captured=null;
      try {
        item.method_451(stack,mc.world,mc.player);
        check(captured!=null,"native bucket/boat ray was not observed");
        near(captured.y,expectedY,1e-6,"native bucket/boat eye");check(stack.count==1,"miss consumed an item");
      } finally {captureKind=0;}
    }
  }

  private static double cameraY(Minecraft mc,float delta) throws Exception {
    GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();
    try {
      GL11.glLoadIdentity();Method method=class_555.class.getDeclaredMethod("method_1851",float.class);
      method.setAccessible(true);method.invoke(mc.field_2818,delta);
      var m=BufferUtils.createFloatBuffer(16);GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX,m);
      return mc.player.prevY+(mc.player.y-mc.player.prevY)*delta-(m.get(4)*m.get(12)+m.get(5)*m.get(13)+m.get(6)*m.get(14));
    } finally {GL11.glPopMatrix();}
  }

  private static float projection(Minecraft mc,int eye) throws Exception {
    GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPushMatrix();GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();
    try {
      RenderedView.clear();Method method=class_555.class.getDeclaredMethod("method_1840",float.class,int.class);
      method.setAccessible(true);method.invoke(mc.field_2818,1f,eye);
      var matrix=BufferUtils.createFloatBuffer(16);GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX,matrix);return matrix.get(5);
    } finally {
      GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }
  }

  private static void reset(Minecraft mc,int x,int y,int z) {
    var p=mc.player;var state=((SwimPlayer)p).power$swim();state.swimming=state.compact=state.crouched=state.lowCeiling=false;
    state.resetCamera(1.62f);p.spacingXZ=.6f;p.spacingY=1.8f;p.field_1631=1.62f;p.field_1640=0;
    p.field_161=new class_41();p.method_1341(x+.5,y,z+.5,0,0);p.velocityX=p.velocityZ=0;p.velocityY=-.08*.98f;p.field_1623=true;
    p.prevX=p.x;p.prevY=p.y;p.prevZ=p.z;p.prevYaw=p.yaw;p.prevPitch=p.pitch;
    p.hurtTime=0;p.field_1041=0;p.field_504=p.field_505=0;SprintController.reset();
  }

  private static void scene(Minecraft mc,int x,int y,int z) {
    for(int bx=x-2;bx<=x+2;bx++)for(int bz=z-2;bz<=z+4;bz++)for(int by=y-1;by<=y+3;by++)
      mc.world.method_154(bx,by,bz,by==y-1?1:0,0);
  }

  private static void near(double actual,double expected,double tolerance,String label) {
    check(Math.abs(actual-expected)<=tolerance,label+": "+actual+" expected "+expected);
  }
}
