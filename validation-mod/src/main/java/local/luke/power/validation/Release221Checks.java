package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.FreecamPreviewChecks.*;
import java.lang.reflect.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.input.*;
import local.luke.power.mixin.ItemHealthAccessor;
import local.luke.power.render.GhostHead;
import local.luke.power.rules.*;
import local.luke.power.storage.PowerConfig;
import net.minecraft.class_142;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/** Changes only disposable settings, entities, terrain and the test framebuffer. */
public final class Release221Checks {
  private static final String[] ACCEL = {"creative.acceleration", "creative.spectatorAcceleration", "power_camera:config.acceleration"};
  static void run(Minecraft mc, String action) throws Exception {
    if(action.equals("save") || action.equals("saved")) {
      var session=SettingsRegistry.open(mc);
      for(int i=0;i<ACCEL.length;i++) {
        var setting=find(session,ACCEL[i]);
        if(action.equals("saved"))check(Math.abs(setting.value.getAsDouble()-(i+1.25))<1e-5,"acceleration restart "+setting.id);
        setting.parse(action.equals("save")?Double.toString(i+1.25):i==2?"3.5":"5");
      }
      session.save(Path.of(".").toAbsolutePath());
      log("PASS acceleration persistence "+action);return;
    }
    failures=0;mc.setScreen(null);mc.paused=false;DeathChecks.alive(mc);active(false);
    var rules=AccessRules.copy(mc);var session=SettingsRegistry.open(mc);
    try {
      AccessRules.restore(mc,RuleSet.cheats());
      test("acceleration sliders use defaults, bounded drafts, previews and Cancel",()->{
        for(int i=0;i<ACCEL.length;i++) {
          var s=find(session,ACCEL[i]);
          check(s.kind==Setting.Kind.DECIMAL && s.min==0 && s.max==5,"acceleration slider "+s.id);
          check(s.defaultValue.getAsDouble()==(i==2?3.5:5),"acceleration default "+s.id);
          s.parse("2.5");session.preview(true);
        }
        session.discard();
        var restored=SettingsRegistry.open(mc);
        for(String id:ACCEL)check(find(restored,id).value.equals(find(restored,id).defaultValue),"Cancel changed "+id);
        restored.discard();
        for(String id:List.of("creative.glide","creative.spectatorGlide","power_camera:config.drag"))
          check(find(session,id).label.endsWith("deceleration"),"deceleration label "+id);
      });
      test("Creative Y follows the same feet/eye origin as Survival through flight, falling and landing",()->coordinates(mc));
      test("native flight accelerates and stops in all directions in both protected modes",()->nativeFlight(mc));
      test("detached motion responds on every axis and reverses without changing frame timing",()->freecam(mc));
      test("lava-protected items sink with unchanged data, water motion and server authority",()->lava(mc));
      test("native skin pixels and depth survive foreground terrain at both marker opacities",()->heads(mc));
    } finally { active(false);session.discard();AccessRules.restore(mc,rules); }
    log("RELEASE 221 FAILURES "+failures);
  }

  private static void coordinates(Minecraft mc) throws Exception {
    boolean preference=local.luke.power.ui.MenuPreferences.useFeetYCoordinates();
    var flying=mc.player.getClass().getMethod("creative_setFlying",boolean.class);
    var isFlying=mc.player.getClass().getMethod("creative_isFlying");
    var command=Class.forName("local.luke.power.commands.util.CommandPosition").getMethod("of",net.minecraft.entity.player.PlayerEntity.class);
    var editor=Class.forName("local.luke.power.worldedit.WorldEditor");
    try {
      for(int x=31;x<=33;x++)for(int z=31;z<=33;z++)for(int y=109;y<=123;y++)
        mc.world.method_154(x,y,z,y==109?1:0,0);
      for(boolean feet:new boolean[]{true,false}) {
        local.luke.power.ui.MenuPreferences.previewFeetY(feet);
        for(String mode:List.of("SURVIVAL","CREATIVE"))for(boolean flight:new boolean[]{false,true}) {
          if(mode.equals("SURVIVAL")&&flight)continue;
          CreativeVehicleChecks.mode(mc,mode);flying.invoke(mc.player,flight);
          for(double eye:new double[]{111.62,112.12,119.25}) {
            mc.player.method_1340(-4.25,eye,5.75);
            double expected=feet?mc.player.boundingBox.minY:mc.player.y;
            check(local.luke.power.hud.HudCoordinates.y(mc.player)==expected,mode+" coordinate origin, flying="+flight+", feet="+feet);
            check(ReportedPosition.current(mc.player,0,0,0,0,0).y()==expected,"reported Y diverged");
            check(((Number)call(command.invoke(null,mc.player),"y")).doubleValue()==expected,"command Y diverged");
            editor.getMethod("command",Minecraft.class,String.class).invoke(null,mc,"//pos1");
            Object state=editor.getMethod("current").invoke(null);
            check(state.getClass().getField("pos1").get(state).toString().equals("-5,"+(int)Math.floor(expected)+",5"),"WorldEdit Y diverged");
          }
        }
        for(boolean slab:new boolean[]{false,true})for(boolean landFlying:new boolean[]{false,true}) {
          mc.world.method_154(32,110,32,slab?44:0,0);
          CreativeVehicleChecks.mode(mc,"CREATIVE");flying.invoke(mc.player,true);
          mc.player.method_1340(32.5,119.62,32.5);mc.player.field_1623=false;
          mc.player.field_161.field_2532=mc.player.field_161.field_2533=0;
          mc.player.field_161.field_2535=mc.player.field_161.field_2536=false;
          mc.player.velocityX=mc.player.velocityZ=0;mc.player.velocityY=-.5;
          if(!landFlying)flying.invoke(mc.player,false);
          for(int i=0;i<100&&!mc.player.field_1623;i++) {
            mc.player.method_945(0,0);
            double expected=feet?mc.player.boundingBox.minY:mc.player.y;
            check(local.luke.power.hud.HudCoordinates.y(mc.player)==expected,"fall/flight Y changed origin");
            // Maintain descent to reach the floor while native flight has no gravity.
            if((boolean)isFlying.invoke(mc.player))mc.player.velocityY=-.5;
          }
          check(mc.player.field_1623&&!(boolean)isFlying.invoke(mc.player),"flight did not end on landing");
          double creativeY=local.luke.power.hud.HudCoordinates.y(mc.player);
          CreativeVehicleChecks.mode(mc,"SURVIVAL");
          check(local.luke.power.hud.HudCoordinates.y(mc.player)==creativeY,"grounded Creative differs from Survival");
          check(Math.abs(mc.player.boundingBox.minY-(slab?110.5:110))<1e-5,"wrong landing surface");
        }
        CreativeVehicleChecks.mode(mc,"SPECTATOR");
        check(local.luke.power.hud.HudCoordinates.y(mc.player)==mc.player.y-mc.player.field_1631+HeadGeometry.BASE_ABOVE_FEET,"spectator lost head-base Y");
        active(true);pose(32.5,119.62,32.5,0,180);
        check(local.luke.power.hud.HudCoordinates.y(mc.player)==HeadGeometry.baseY(DetachedCamera.y()),"freecam lost head-base Y");active(false);
      }
    } finally {active(false);local.luke.power.ui.MenuPreferences.previewFeetY(preference);mc.world.method_154(32,110,32,0,0);}
  }

  private static void nativeFlight(Minecraft mc) throws Exception {
    var session=SettingsRegistry.open(mc);
    try {
      for(String mode:List.of("CREATIVE","SPECTATOR"))for(double level:new double[]{0,3.5,5}) {
        CreativeVehicleChecks.mode(mc,mode);
        mc.player.getClass().getMethod("creative_setFlying",boolean.class).invoke(mc.player,true);
        find(session,mode.equals("CREATIVE")?ACCEL[0]:ACCEL[1]).parse(Double.toString(level));
        find(session,mode.equals("CREATIVE")?"creative.glide":"creative.spectatorGlide").parse("0");session.preview(true);
        Class.forName("local.luke.power.creative.ClientRuntime").getMethod("tick",Minecraft.class).invoke(null,mc);
        for(int side=-1;side<=1;side++)for(int forward=-1;forward<=1;forward++)for(int vertical=-1;vertical<=1;vertical++) {
          mc.player.method_1340(32.5,122,32.5);mc.player.yaw=0;mc.player.velocityX=mc.player.velocityY=mc.player.velocityZ=0;
          var input=mc.player.field_161;input.field_2532=vertical<0?side*.3f:side;input.field_2533=vertical<0?forward*.3f:forward;
          input.field_2535=vertical>0;input.field_2536=vertical<0;
          var expected=FlightMotion.tick(0,0,0,side,forward,vertical,0,.05,1,level,0);
          mc.player.method_945(0,0);
          check(Math.abs(mc.player.x-32.5-expected.x())<1e-6 && Math.abs(mc.player.y-122-expected.y())<1e-6
              && Math.abs(mc.player.z-32.5-expected.z())<1e-6,mode+" native acceleration "+level+" "+side+","+forward+","+vertical);
          input.field_2532=input.field_2533=0;input.field_2535=input.field_2536=false;
          double x=mc.player.x,y=mc.player.y,z=mc.player.z;mc.player.method_945(0,0);
          check(mc.player.x==x&&mc.player.y==y&&mc.player.z==z,"native zero deceleration "+mode);
        }
      }
    } finally {session.discard();}
  }

  private static void freecam(Minecraft mc) throws Exception {
    var session=SettingsRegistry.open(mc);
    Object camera=Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
    Method move=Arrays.stream(mc.field_2818.getClass().getDeclaredMethods()).filter(m->m.getName().contains("moveCamera")&&m.getParameterCount()==0).findFirst().orElseThrow();move.setAccessible(true);
    Field time=Arrays.stream(mc.field_2818.getClass().getDeclaredFields()).filter(f->f.getType()==long.class&&f.getName().contains("lastTime")).findFirst().orElseThrow();time.setAccessible(true);
    try {
      CreativeVehicleChecks.mode(mc,"SPECTATOR");active(true);
      find(session,ACCEL[2]).parse("0");find(session,"power_camera:config.drag").parse("0");session.preview(true);
      for(int side=-1;side<=1;side++)for(int forward=-1;forward<=1;forward++)for(int vertical=-1;vertical<=1;vertical++) {
        pose(32.5,122,32.5,0,180);camera.getClass().getMethod("clearMovementInput").invoke(camera);
        camera.getClass().getField("strafe").setFloat(camera,side);camera.getClass().getField("move").setFloat(camera,forward);
        camera.getClass().getField("jumping").setBoolean(camera,vertical>0);camera.getClass().getField("sneaking").setBoolean(camera,vertical<0);
        for(String axis:List.of("X","Y","Z"))camera.getClass().getField("velocity"+axis).setDouble(camera,0);
        time.setLong(mc.field_2818,System.nanoTime()-100_000_000);move.invoke(mc.field_2818);
        double x=DetachedCamera.x(),y=DetachedCamera.y(),z=DetachedCamera.z();
        check((side==0||Math.abs(x-32.5)>.4)&&(forward==0||Math.abs(z-32.5)>.4)
            &&(vertical==0||(y-122)*vertical>.5),"detached direction "+side+","+forward+","+vertical);
        camera.getClass().getMethod("clearMovementInput").invoke(camera);
        time.setLong(mc.field_2818,System.nanoTime()-100_000_000);move.invoke(mc.field_2818);
        check(DetachedCamera.x()==x&&DetachedCamera.y()==y&&DetachedCamera.z()==z,"detached release");
      }
    } finally {active(false);session.discard();}
  }

  private static void lava(Minecraft mc) throws Exception {
    boolean remote=mc.world.isRemote;
    try {
      for(int block:new int[]{8,9,10,11})for(boolean destroys:new boolean[]{false,true}) {
        for(int y=108;y<=114;y++)mc.world.method_154(4,y,4,block,0);
        AccessRules.set(mc,GameRule.LAVA_DESTROYS_ITEMS,destroys);
        var stack=new ItemStack(267,17,37);DeathChecks.tag(stack).putString("lava-sinking","retained");var nbt=local.luke.power.death.DeathItems.encode(stack);
        var item=new class_142(mc.world,4.5,113.4,4.5,stack);item.velocityX=item.velocityY=item.velocityZ=0;
        item.tick();boolean protectedLava=block>=10&&!destroys;
        check(block<10||protectedLava?item.y<113.4&&item.velocityY<0:item.velocityY>0,"fluid bounce "+block+" "+destroys);
        if(protectedLava) {
          double previous=item.y;
          for(int i=0;i<10;i++){item.tick();check(item.y<previous,"lava did not keep sinking");previous=item.y;}
          check(!item.dead&&item.fire<=0&&((ItemHealthAccessor)item).power$health()==5,"sinking item damaged");
          check(local.luke.power.death.DeathItems.encode(stack).equals(nbt),"sinking altered item data");
        }
        item.markDead();
      }
      AccessRules.set(mc,GameRule.LAVA_DESTROYS_ITEMS,false);mc.world.isRemote=true;
      var item=new class_142(mc.world,4.5,113.4,4.5,new ItemStack(1,1,0));item.velocityY=0;item.tick();
      check(item.velocityY>0,"client suppressed server-owned bounce");item.markDead();
    } finally {mc.world.isRemote=remote;for(int y=108;y<=114;y++)mc.world.method_154(4,y,4,0,0);}
  }

  private static int pixel() {
    var pixel=BufferUtils.createByteBuffer(4);GL11.glReadPixels(160,120,1,1,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixel);return pixel.getInt(0);
  }
  private static void heads(Minecraft mc) throws Exception {
    Method draw=GhostHead.class.getDeclaredMethod("draw",Minecraft.class,float.class,double.class,double.class,double.class,float.class,float.class,float.class,boolean.class);draw.setAccessible(true);
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPushMatrix();GL11.glLoadIdentity();GL11.glOrtho(-1,1,-.75,.75,.1,100);
    GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();GL11.glLoadIdentity();GL11.glTranslatef(0,-.2F,-4);
    try {
      GL11.glViewport(0,0,320,240);GL11.glDisable(GL11.GL_SCISSOR_TEST);GL11.glDisable(GL11.GL_FOG);
      GL11.glColorMask(true,true,true,true);GL11.glDepthMask(true);GL11.glClearColor(.1F,.2F,.9F,1);
      for(float alpha:new float[]{1,.65F}) {
        GL11.glClearDepth(.4);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glDepthRange(0,1);GL11.glDepthFunc(GL11.GL_LESS);GL11.glEnable(GL11.GL_DEPTH_TEST);
        int background=pixel();
        draw.invoke(null,mc,1F,mc.field_2807.x,mc.field_2807.y,mc.field_2807.z,180F,0F,alpha,true);
        int skin=pixel();
        check(skin!=background,"no skin pixel");
        var depth=BufferUtils.createFloatBuffer(1);GL11.glReadPixels(160,120,1,1,GL11.GL_DEPTH_COMPONENT,GL11.GL_FLOAT,depth);
        check(depth.get(0)<.001,"head did not write foreground depth");
        var range=BufferUtils.createDoubleBuffer(16);GL11.glGetDouble(GL11.GL_DEPTH_RANGE,range);
        check(range.get(0)==0&&range.get(1)==1&&GL11.glGetInteger(GL11.GL_DEPTH_FUNC)==GL11.GL_LESS,"head leaked depth state");
        // A later distant terrain face must fail against the head's own depth.
        GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glDisable(GL11.GL_BLEND);GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glColor4f(0,1,0,1);GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3d(-1,-1,-45);GL11.glVertex3d(1,-1,-45);GL11.glVertex3d(1,1,-45);GL11.glVertex3d(-1,1,-45);GL11.glEnd();
        check(pixel()==skin,"terrain cut through head alpha="+alpha);
      }
      check(GL11.glGetError()==0,"head GL error");
    } finally {
      GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopAttrib();
    }
  }
}
