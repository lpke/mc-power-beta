package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.*;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.util.Arrays;
import local.luke.power.autowalk.AutoWalk;
import local.luke.power.building.config.Config;
import local.luke.power.input.MovementOwnership;
import local.luke.power.input.Bindings;
import local.luke.power.storage.PowerConfig;
import net.minecraft.class_555;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

final class FreecamHandoffChecks {
  static void run(Minecraft mc) throws Exception {
    check(org.lwjgl.opengl.Display.isActive(),"test window must have focus");
    failures=0;
    Object camera=Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
    Object options=Class.forName("local.luke.power.camera.FreecamConfig").getField("config").get(null);
    Field enabled=options.getClass().getField("enabled"); Object oldEnabled=enabled.get(options);
    var before=Config.current().copy(); var walking=before.copy(); walking.autoWalk=true; Config.preview(walking);
    boolean bob=mc.options.bobView, third=mc.options.thirdPerson, stereo=mc.options.anaglyph3d;
    byte[] saved=Files.readAllBytes(PowerConfig.path());
    Object keys=Class.forName("local.luke.power.camera.listener.KeyPressedListener").getConstructor().newInstance();
    KeyBinding binding=(KeyBinding)Class.forName("local.luke.power.camera.registry.KeyBindingRegistry").getField("freecamKeybinding").get(null);
    int oldCode=binding.code; var oldModifiers=Bindings.modifiers();
    binding.code=Keyboard.KEY_C; Bindings.configure(java.util.Map.of());
    Method press=keys.getClass().getDeclaredMethod("press",int.class); press.setAccessible(true);
    Method active=camera.getClass().getMethod("setActive",boolean.class);
    Method start=camera.getClass().getMethod("startFromView",Minecraft.class);
    mc.setScreen(null); mc.field_2807=mc.player; enabled.set(options,true); AutoWalk.tick(mc);
    Field toggle=AutoWalk.class.getDeclaredField("TOGGLE"); toggle.setAccessible(true);
    var state=(local.luke.power.autowalk.WalkToggle)toggle.get(null);
    state.stop(); state.update(false,true); state.update(true,true); state.update(false,true);
    try {
      for(boolean viewBob:new boolean[]{false,true}) test("hotkey keeps walking perspective with bobbing="+viewBob, () -> {
        mc.options.bobView=viewBob; mc.options.thirdPerson=false; mc.options.anaglyph3d=false;
        for(float delta:new float[]{0,.17F,.5F,.9F,1}) for(float yaw:new float[]{0,89.9F,90,173,270}) {
          active.invoke(camera,false); walking(mc,yaw,23,1.62F);
          float[] normal=matrix(mc,delta,0);
          press.invoke(keys,Keyboard.KEY_C); check(MovementOwnership.cameraControlsMovement(),"hotkey did not activate");
          mc.player.x+=.27; mc.player.y-=.09; mc.player.z+=.18;
          equal(normal,matrix(mc,.6F,0)); active.invoke(camera,false);
        }
      });
      test("sneaking, third person and vertical views keep their rendered pose", () -> {
        mc.options.bobView=true;
        for(boolean thirdPerson:new boolean[]{false,true}) for(float eye:new float[]{1.54F,1.62F})
          for(float pitch:new float[]{-90,0,90}) {
            active.invoke(camera,false); mc.options.thirdPerson=thirdPerson; walking(mc,63,pitch,eye);
            float[] normal=matrix(mc,.4F,0); press.invoke(keys,Keyboard.KEY_C);
            equal(normal,matrix(mc,.4F,0));
          }
      });
      test("both anaglyph eyes retain their separate view offsets", () -> {
        mc.options.anaglyph3d=true; mc.options.thirdPerson=false;
        for(int eye:new int[]{0,1}) {
          active.invoke(camera,false); walking(mc,155,27,1.62F);
          float[] normal=matrix(mc,.37F,eye); press.invoke(keys,Keyboard.KEY_C);
          equal(normal,matrix(mc,.37F,eye));
        }
      });
      test("stale velocity and timing cannot move the first detached frame", () -> {
        active.invoke(camera,false); mc.options.anaglyph3d=false; walking(mc,0,0,1.62F); matrix(mc,.5F,0);
        for(String name:new String[]{"velocityX","velocityY","velocityZ","accelerationX","accelerationY","accelerationZ"}) camera.getClass().getField(name).setDouble(camera,8);
        press.invoke(keys,Keyboard.KEY_C); double[] position=position(camera);
        camera.getClass().getField("move").setFloat(camera,1);
        mc.field_2818.method_1837(); check(Arrays.equals(position,position(camera)),"pre-render movement changed pose");
        for(String name:new String[]{"velocityX","velocityY","velocityZ","accelerationX","accelerationY","accelerationZ"}) check(camera.getClass().getField(name).getDouble(camera)==0,"stale "+name);
        matrix(mc,.5F,0); camera.getClass().getField("move").setFloat(camera,0);
      });
      test("auto-walk moves the player while the detached world view stays still", () -> {
        check(AutoWalk.isWalking(),"auto-walk stopped during handoff");
        mc.player.field_161.method_1942(mc.player); check(mc.player.field_161.field_2533==1,"auto-walk input lost");
        double[] cameraBefore=position(camera); double x=mc.player.x,z=mc.player.z;
        for(int i=0;i<5;i++) mc.player.method_945(0,mc.player.field_161.field_2533);
        check(mc.player.x!=x || mc.player.z!=z,"player did not move");
        check(Arrays.equals(cameraBefore,position(camera)),"auto-walk moved detached camera");
        press.invoke(keys,Keyboard.KEY_C); check(!MovementOwnership.cameraControlsMovement() && AutoWalk.isWalking(),"exit changed ownership/walking");
        float[] first=matrix(mc,.5F,0); active.invoke(camera,false); equal(first,matrix(mc,.5F,0));
      });
      test("missing/cleared snapshots use the current entity and settings stay untouched", () -> {
        camera.getClass().getMethod("clearRenderedPosition").invoke(camera); walking(mc,35,12,1.62F);
        start.invoke(camera,mc); double[] pose=position(camera);
        check(pose[0]==mc.player.x && pose[1]==mc.player.y && pose[2]==mc.player.z,"stale snapshot reused");
        check(Arrays.equals(saved,Files.readAllBytes(PowerConfig.path())),"handoff wrote settings");
        check(GL11.glGetError()==GL11.GL_NO_ERROR,"OpenGL error");
      });
    } finally {
      active.invoke(camera,false); AutoWalk.stop(); Config.preview(before); enabled.set(options,oldEnabled);
      binding.code=oldCode; Bindings.configure(oldModifiers);
      mc.options.bobView=bob; mc.options.thirdPerson=third; mc.options.anaglyph3d=stereo;
      mc.player.field_1631=1.62F; mc.player.method_140();
    }
    log("FREECAM HANDOFF FAILURES "+failures);
  }

  private static void walking(Minecraft mc,float yaw,float pitch,float eye) {
    mc.player.method_1340(.5,103,.5); mc.player.prevX=.22; mc.player.prevY=103.06; mc.player.prevZ=.17;
    mc.player.yaw=yaw; mc.player.prevYaw=yaw-2; mc.player.pitch=pitch; mc.player.prevPitch=pitch;
    mc.player.field_1631=eye; mc.player.field_1634=3.1F; mc.player.field_1635=3.4F;
    mc.player.field_524=.05F; mc.player.field_525=.08F; mc.player.field_1043=.1F; mc.player.field_1044=.2F;
    mc.player.hurtTime=0; mc.player.health=20; mc.player.field_1041=0;
    mc.player.field_504=mc.player.field_505=0;
  }
  private static double[] position(Object camera) throws Exception {
    Object p=camera.getClass().getMethod("getCameraPosition").invoke(camera);
    return new double[]{p.getClass().getField("x").getDouble(p),p.getClass().getField("y").getDouble(p),p.getClass().getField("z").getDouble(p)};
  }
  private static float[] matrix(Minecraft mc,float delta,int eye) throws Exception {
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      Method view=class_555.class.getDeclaredMethod("method_1840",float.class,int.class); view.setAccessible(true); view.invoke(mc.field_2818,delta,eye);
      double x=mc.player.prevX+(mc.player.x-mc.player.prevX)*delta;
      double y=mc.player.prevY+(mc.player.y-mc.player.prevY)*delta;
      double z=mc.player.prevZ+(mc.player.z-mc.player.prevZ)*delta;
      GL11.glTranslated(-x,-y,-z);
      FloatBuffer buffer=BufferUtils.createFloatBuffer(16); GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX,buffer);
      float[] result=new float[16]; buffer.get(result); return result;
    } finally { GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix(); GL11.glMatrixMode(GL11.GL_MODELVIEW); }
  }
  private static void equal(float[] before,float[] after) {
    for(int i=0;i<16;i++) check(Math.abs(before[i]-after[i])<.0001F,"view changed at "+i+": "+before[i]+" -> "+after[i]);
  }
}
