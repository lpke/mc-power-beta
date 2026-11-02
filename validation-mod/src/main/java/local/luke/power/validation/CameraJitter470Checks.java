package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import local.luke.power.input.DetachedCamera;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.lwjgl.opengl.Display;

/** Native input handoff and camera observations. Only runs on explicit validation commands. */
public final class CameraJitter470Checks {
  private static final Path ROOT=Path.of("power-beta-data/reports/release-470/camera-jitter");
  private static final List<String> frames=new ArrayList<>();
  private static boolean recording;
  private static long start;
  private static boolean orbit, staged, savedBob, savedThird;
  private static net.minecraft.class_41 savedInput;
  private static double[] savedPose;
  private static String savedMode;
  private static double targetX,targetY,targetZ,angleX,angleY;
  private static final List<int[]> blocks=new ArrayList<>();
  private static final java.nio.FloatBuffer matrix=org.lwjgl.BufferUtils.createFloatBuffer(16);
  private static float savedSensitivity;


  static void run(Minecraft mc,String command) throws Exception {
    check(mc.world!=null && mc.player!=null && !mc.world.isRemote && Display.isActive()
        && !DetachedCamera.isActive(),"focus a disposable singleplayer world outside freecam");
    check(mc.options.soundVolume==0 && mc.options.musicVolume==0,"mute the disposable client");
    Files.createDirectories(ROOT);
    if(command.equals("stage")) { stage(mc);return; }
    if(command.equals("restore")) { restore(mc);return; }
    if(command.equals("orbit")) {
      check(staged,"stage the camera scene first");orbit=true;
      mc.player.field_161.field_2532=-1;
      command="record";
    }
    if(command.equals("record")) {
      check(!recording,"camera recording already active");frames.clear();start=System.nanoTime();recording=true;
      frames.add("ns,delta,x,z,prevX,prevZ,lastRenderX,lastRenderZ,yaw,prevYaw,pitch,prevPitch,mouseX,mouseY,targetAngleX,targetAngleY");
      log("CAMERA JITTER recording native frames");return;
    }
    if(command.equals("stop")) {
      recording=orbit=false;if(staged)mc.player.field_161.field_2532=0;Files.write(ROOT.resolve("frames.csv"),frames);log("CAMERA JITTER saved "+(frames.size()-1)+" native frames");return;
    }
    check(command.equals("raw"),"use camera-jitter-470 raw|stage|orbit|record|stop|restore");
    raw(mc);
  }

  public static void frame(Minecraft mc,float delta) {
    if(!recording || mc.player==null || frames.size()>=12000)return;
    var p=mc.player;
    frames.add((System.nanoTime()-start)+","+delta+","+p.x+","+p.z+","+p.prevX+","+p.prevZ+","
        +p.field_1637+","+p.field_1639+","+p.yaw+","+p.prevYaw+","+p.pitch+","+p.prevPitch+","
        +mc.field_2767.field_2586+","+mc.field_2767.field_2587+","+angleX+","+angleY);
  }

  private static void stage(Minecraft mc) throws Exception {
    check(!staged,"restore the previous camera scene first");
    var p=mc.player;
    int x=((int)Math.floor(p.x)>>4)*16+8,z=((int)Math.floor(p.z)>>4)*16+8,y=112;
    check(mc.world.method_155(x-12,y-1,z-12,x+12,y+5,z+12),"camera scene must be loaded");
    blocks.clear();
    for(int bx=x-12;bx<=x+12;bx++)for(int bz=z-12;bz<=z+12;bz++)for(int by=y-1;by<=y+4;by++) {
      check(mc.world.method_1777(bx,by,bz)==null,"camera scene overlaps a block entity");
      blocks.add(new int[]{bx,by,bz,mc.world.getBlockId(bx,by,bz),mc.world.method_1778(bx,by,bz)});
    }
    savedPose=new double[]{p.x,p.boundingBox.minY,p.z,p.yaw,p.pitch,p.velocityX,p.velocityY,p.velocityZ};
    var mode=(local.luke.power.input.PlayerMode)p;
    savedMode=mode.power$spectator()?"SPECTATOR":mode.power$creative()?"CREATIVE":"SURVIVAL";
    savedInput=p.field_161;savedBob=mc.options.bobView;savedThird=mc.options.thirdPerson;
    savedSensitivity=mc.options.mouseSensitivity;
    CreativeVehicleChecks.mode(mc,"SURVIVAL");
    for(int[] b:blocks)mc.world.method_154(b[0],b[1],b[2],b[1]==y-1?1:0,0);
    for(int by=y;by<=y+3;by++)mc.world.method_154(x,by,z,17,0);
    mc.world.method_202(x-12,y-1,z-12,x+12,y+4,z+12);
    p.method_1341(x+.5,y,z+4.5,180,0);p.prevX=p.field_1637=p.x;p.prevY=p.field_1638=p.y;p.prevZ=p.field_1639=p.z;
    p.prevYaw=p.yaw;p.prevPitch=p.pitch;p.velocityX=p.velocityY=p.velocityZ=0;
    p.field_161=new net.minecraft.class_41();mc.options.bobView=mc.options.thirdPerson=false;
    mc.options.mouseSensitivity=.40140846f;
    targetX=x+.5;targetY=p.y;targetZ=z+.5;staged=true;
    log("CAMERA JITTER staged native Survival strafe around loaded log pillar at "+targetX+","+targetY+","+targetZ);
  }

  private static void restore(Minecraft mc) throws Exception {
    recording=orbit=false;if(!staged)return;
    var p=mc.player;p.field_161=savedInput;
    for(int[] b:blocks)mc.world.method_154(b[0],b[1],b[2],b[3],b[4]);
    int[] first=blocks.get(0),last=blocks.get(blocks.size()-1);
    mc.world.method_202(first[0],first[1],first[2],last[0],last[1],last[2]);blocks.clear();
    CreativeVehicleChecks.mode(mc,savedMode);
    p.method_1341(savedPose[0],savedPose[1],savedPose[2],(float)savedPose[3],(float)savedPose[4]);
    p.prevX=p.field_1637=p.x;p.prevY=p.field_1638=p.y;p.prevZ=p.field_1639=p.z;
    p.prevYaw=p.yaw;p.prevPitch=p.pitch;p.velocityX=savedPose[5];p.velocityY=savedPose[6];p.velocityZ=savedPose[7];
    mc.options.bobView=savedBob;mc.options.thirdPerson=savedThird;mc.options.mouseSensitivity=savedSensitivity;
    staged=false;log("CAMERA JITTER restored scene and player");
  }

  public static void aim(Minecraft mc,float delta) {
    if(!orbit || mc.player==null)return;
    var p=mc.player;double x=p.field_1637+(p.x-p.field_1637)*delta,z=p.field_1639+(p.z-p.field_1639)*delta;
    double desired=Math.toDegrees(Math.atan2(-(targetX-x),targetZ-z));
    double current=p.prevYaw+(p.yaw-p.prevYaw)*delta,change=(desired-current)%360;
    if(change>180)change-=360;if(change< -180)change+=360;
    p.method_1362((float)(change/.15),(float)(p.pitch/.15));
  }

  public static void camera(Minecraft mc,float delta) {
    if(!recording || !staged || mc.player==null)return;
    var p=mc.player;matrix.clear();org.lwjgl.opengl.GL11.glGetFloat(org.lwjgl.opengl.GL11.GL_MODELVIEW_MATRIX,matrix);
    double x=targetX-p.field_1637-(p.x-p.field_1637)*delta;
    double y=targetY-p.field_1638-(p.y-p.field_1638)*delta;
    double z=targetZ-p.field_1639-(p.z-p.field_1639)*delta;
    double cx=matrix.get(0)*x+matrix.get(4)*y+matrix.get(8)*z+matrix.get(12);
    double cy=matrix.get(1)*x+matrix.get(5)*y+matrix.get(9)*z+matrix.get(13);
    double cz=matrix.get(2)*x+matrix.get(6)*y+matrix.get(10)*z+matrix.get(14);
    angleX=Math.toDegrees(Math.atan2(cx,-cz));angleY=Math.toDegrees(Math.atan2(cy,-cz));
  }

  private static void raw(Minecraft mc) throws Exception {
    Class<?> handler=Class.forName("local.luke.power.controls.tweaks.rawinput.RawInputHandler");
    Field running=handler.getField("runInputThread"),threadField=handler.getField("inputThread");
    boolean wasRunning=running.getBoolean(null);Thread thread=(Thread)threadField.get(null);
    running.setBoolean(null,false);
    if(thread!=null) { thread.join(2000);check(!thread.isAlive(),"raw polling thread did not stop for isolated handoff check"); }
    Class<?> helperClass=Class.forName("local.luke.power.controls.tweaks.rawinput.RawMouseHelper");
    var mouse=(net.minecraft.class_596)helperClass.getConstructor(java.awt.Component.class).newInstance(mc.canvas);
    Class<?> motionClass=null;
    try {motionClass=Class.forName("local.luke.power.controls.tweaks.rawinput.RawMouseMotion");} catch(ClassNotFoundException legacy) {}
    Object motion=motionClass==null?null:handler.getField("motion").get(null);
    Method add=motionClass==null?null:motionClass.getMethod("add",int.class,int.class);
    Method clear=motionClass==null?null:motionClass.getMethod("clear");
    Field dx=motionClass==null?handler.getField("dx"):null,dy=motionClass==null?handler.getField("dy"):null;
    var screen=mc.currentScreen;mc.currentScreen=null;
    try {
      int expected=200000;long totalError=0;
      for(int trial=0;trial<8;trial++) {
        if(clear!=null)clear.invoke(motion);else {dx.setInt(null,0);dy.setInt(null,0);}
        AtomicBoolean done=new AtomicBoolean();AtomicReference<Throwable> failure=new AtomicReference<>();
        CountDownLatch ready=new CountDownLatch(1);
        Object target=motion;Method offer=add;Field x=dx,y=dy;
        Thread producer=new Thread(()->{
          try {
            ready.await();
            for(int i=0;i<expected;i++) {
              if(offer!=null)offer.invoke(target,1,-1);
              else {x.setInt(null,x.getInt(null)+1);y.setInt(null,y.getInt(null)-1);}
            }
          }catch(Throwable problem){failure.set(problem);}finally{done.set(true);}
        },"power-validation-raw-producer");
        producer.start();ready.countDown();long observedX=0,observedY=0;int reads=0;
        while(!done.get()) {
          mouse.method_1972();observedX+=mouse.field_2586;observedY+=mouse.field_2587;reads++;
        }
        producer.join();mouse.method_1972();observedX+=mouse.field_2586;observedY+=mouse.field_2587;
        if(failure.get()!=null)throw new RuntimeException(failure.get());
        totalError+=Math.abs(expected-observedX)+Math.abs(expected-observedY);
        log("CAMERA RAW "+(motionClass==null?"legacy":"atomic")+" trial="+trial+" produced="+expected
            +" x="+observedX+" y="+observedY+" drains="+reads);
      }
      if(motionClass!=null)check(totalError==0,"native raw helper lost or duplicated motion: "+totalError);
      log("CAMERA RAW total count error="+totalError+" implementation="+(motionClass==null?"legacy":"atomic"));
      Files.writeString(ROOT.resolve(motionClass==null?"raw-before.txt":"raw-after.txt"),"Eight trials, 200000 paired motion samples each; absolute count error="+totalError+"\n");
    } finally {
      mc.currentScreen=screen;
      if(clear!=null)clear.invoke(motion);else {dx.setInt(null,0);dy.setInt(null,0);}
      if(wasRunning)handler.getMethod("startInputThread").invoke(null);
    }
  }
}
