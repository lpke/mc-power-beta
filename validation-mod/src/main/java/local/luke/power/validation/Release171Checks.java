package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.FreecamPreviewChecks.*;
import java.lang.reflect.*;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.concurrent.*;
import local.luke.power.building.hotbar.Hotbars;
import local.luke.power.building.hotbar.InventoryRows;
import local.luke.power.building.hotbar.RecoveryJournal;
import local.luke.power.hud.*;
import local.luke.power.input.*;
import local.luke.power.render.GhostHead;
import local.luke.power.ui.MenuPreferences;
import local.luke.power.ui.DebugOverlay;
import net.minecraft.class_555;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/** Deliberately changes only the disposable validation world's inventory and terrain. */
final class Release171Checks {
  private static Class<?> transaction() throws Exception {
    return Class.forName("local.luke.power.building.hotbar.HotbarTransaction");
  }
  private static Object state(String name) throws Exception {
    Field field=transaction().getDeclaredField(name);field.setAccessible(true);return field.get(null);
  }
  private static void state(String name,Object value) throws Exception {
    Field field=transaction().getDeclaredField(name);field.setAccessible(true);field.set(null,value);
  }
  private static void tick(Minecraft mc) throws Exception {
    Method method=transaction().getDeclaredMethod("tick",Minecraft.class);method.setAccessible(true);method.invoke(null,mc);
  }
  private static void drain(Minecraft mc) throws Exception {
    long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
    while(state("pending")!=null && System.nanoTime()<deadline) {tick(mc);Thread.sleep(1);}
    check(state("pending")==null,"journal did not finish");
  }
  private static final class BlockedWriter implements AutoCloseable {
    final CountDownLatch release=new CountDownLatch(1);
    BlockedWriter() throws Exception {
      Field field=Class.forName("local.luke.power.building.hotbar.JournalWriter").getDeclaredField("IO");field.setAccessible(true);
      CountDownLatch started=new CountDownLatch(1);
      ((ExecutorService)field.get(null)).submit(()->{started.countDown();release.await(10,TimeUnit.SECONDS);return null;});
      check(started.await(5,TimeUnit.SECONDS),"journal worker did not start");
    }
    public void close(){release.countDown();}
  }
  static void run(Minecraft mc,String action) throws Exception {
    if(action.equals("normal-scene")) {
      scene(mc);(AccessRules.of(mc.world.method_262())).allAllowed(true);
      CreativeVehicleChecks.mode(mc,"CREATIVE");mc.player.method_1340(32.5,110.62,32.5);
      for(int y=109;y<=112;y++)mc.world.method_154(32,y,32,1,0);
      mc.options.thirdPerson=true;return;
    }
    if(action.equals("perspective-held")) {
      check(mc.options.thirdPerson && GhostHead.visible(mc),"native frame reset third person/head");
      log("PASS native frames preserve spectator/freecam third person and ghost");return;
    }
    if(action.equals("normal-perspective")) {
      check(!mc.options.thirdPerson,"normal in-block camera behaviour changed");log("PASS normal in-block first-person reset remains");return;
    }
    if(action.equals("head-result")){check(GhostProbe.samples>400 && GhostProbe.changed>400,"head did not draw visible skin pixels");log(GhostProbe.details+" samples="+GhostProbe.samples+" changed="+GhostProbe.changed);GhostProbe.save();return;}
    if(action.equals("head-probe")){GhostProbe.capture=true;return;}
    if(action.equals("head-state")) {
      Field texture=GhostHead.class.getDeclaredField("skinTexture");texture.setAccessible(true);
      Field parts=GhostHead.class.getDeclaredField("parts");parts.setAccessible(true);
      log("HEAD texture="+texture.get(null)+" fallback="+mc.player.method_1314()+" source="+mc.player.skinUrl+" parts="+Arrays.deepToString((Object[])parts.get(null)));
      GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPushMatrix();GL11.glLoadIdentity();
      org.lwjgl.util.glu.GLU.gluPerspective(70,mc.displayWidth/(float)mc.displayHeight,.05F,100);
      GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();GL11.glLoadIdentity();GL11.glTranslatef(0,0,-4);
      GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glDisable(GL11.GL_FOG);GL11.glDisable(GL11.GL_SCISSOR_TEST);
      int query=org.lwjgl.opengl.GL15.glGenQueries();org.lwjgl.opengl.GL15.glBeginQuery(org.lwjgl.opengl.GL15.GL_SAMPLES_PASSED,query);
      GhostHead.render(mc,1);
      org.lwjgl.opengl.GL15.glEndQuery(org.lwjgl.opengl.GL15.GL_SAMPLES_PASSED);
      int pixels=org.lwjgl.opengl.GL15.glGetQueryObjectui(query,org.lwjgl.opengl.GL15.GL_QUERY_RESULT);
      org.lwjgl.opengl.GL15.glDeleteQueries(query);GL11.glPopAttrib();GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_MODELVIEW);
      log("HEAD samples="+pixels+" GL="+GL11.glGetError());return;
    }
    if(action.equals("camera-state")) {
      log("CAMERA third="+mc.options.thirdPerson+" ghost="+GhostHead.visible(mc)+" spectator="+((PlayerMode)mc.player).power$spectator()+" detached="+DetachedCamera.isActive()+" matrix="+Arrays.toString(matrix(mc)));return;
    }
    if(action.equals("picker")) {PickerChecks.run(mc);return;}
    if(action.startsWith("picker-")) {PickerChecks.scene(mc,action.substring(7));return;}
    if(action.equals("hud")) {hud(mc);return;}
    if(action.equals("hotbar")) {hotbars(mc);return;}
    if(action.equals("failure")) {failure(mc);return;}
    if(action.equals("camera")) {cameras(mc);return;}
    if(action.equals("fire")) {fire(mc);return;}
    if(action.equals("spectator-scene")) {
      scene(mc);(AccessRules.of(mc.world.method_262())).allAllowed(true);
      CreativeVehicleChecks.mode(mc,"SPECTATOR");mc.player.method_1340(32.5,110.62,32.5);
      mc.player.prevX=mc.player.x;mc.player.prevY=mc.player.y;mc.player.prevZ=mc.player.z;
      mc.player.yaw=mc.player.prevYaw=180;mc.player.pitch=mc.player.prevPitch=0;
      for(int y=109;y<=112;y++)mc.world.method_154(32,y,32,1,0);
      mc.options.thirdPerson=true;mc.options.hideHud=false;mc.options.debugHud=true;return;
    }
    if(action.equals("freecam-floor")) {
      scene(mc);(AccessRules.of(mc.world.method_262())).allAllowed(true);
      CreativeVehicleChecks.mode(mc,"SURVIVAL");pose(32.5,109+HeadGeometry.EYE_ABOVE_BASE,35.5,0,0);active(true);
      mc.options.thirdPerson=true;mc.options.hideHud=false;mc.options.debugHud=true;return;
    }
    throw new IllegalArgumentException(action);
  }

  private static void hud(Minecraft mc) throws Exception {
    failures=0;active(false);mc.setScreen(null);scene(mc);
    Object settings=Class.forName("local.luke.power.camera.FreecamConfig").getField("config").get(null);
    Field flag=settings.getClass().getField("hideHudOnEntry");boolean original=(boolean)flag.get(settings);
    try {
      check(original==local.luke.power.storage.PowerConfig.section("power_camera:config").get("hideHudOnEntry").getAsBoolean(),"HUD preference lost on restart");
      test("freecam HUD preference preserves entry state and manual F1 in both directions",()->{
        for(boolean hide:new boolean[]{false,true})for(boolean entry:new boolean[]{false,true}){
          flag.set(settings,hide);mc.options.hideHud=entry;active(true);
          check(mc.options.hideHud==(entry||hide),"wrong entry HUD");
          mc.options.hideHud=!mc.options.hideHud;active(false);check(mc.options.hideHud==entry,"entry HUD not restored");
        }
      });
      flag.set(settings,original);
      test("freecam HUD option defaults on and Cancel restores a preview",()->{
        var session=local.luke.power.config.SettingsRegistry.open(mc);
        var option=find(session,"power_camera:config.hideHudOnEntry");
        check(option.defaultValue.getAsBoolean(),"default is not on");
        boolean before=(boolean)flag.get(settings);option.value=new com.google.gson.JsonPrimitive(!before);
        session.preview(true);check((boolean)flag.get(Class.forName("local.luke.power.camera.FreecamConfig").getField("config").get(null))!=before,"preview did not update");session.discard();
        check((boolean)flag.get(Class.forName("local.luke.power.camera.FreecamConfig").getField("config").get(null))==before,"Cancel did not restore HUD option");
      });
    }finally{active(false);flag.set(Class.forName("local.luke.power.camera.FreecamConfig").getField("config").get(null),original);mc.options.hideHud=false;}
    log("RELEASE 171 HUD FAILURES "+failures);
  }

  private static void hotbars(Minecraft mc) throws Exception {
    failures=0;active(false);mc.setScreen(null);mc.paused=false;DeathChecks.alive(mc);
    (AccessRules.of(mc.world.method_262())).allAllowed(true);
    CreativeVehicleChecks.mode(mc,"SURVIVAL");mc.field_2807=mc.player;
    var inventory=mc.player.inventory;var oldMain=inventory.main;var oldArmor=inventory.armor;
    int selected=inventory.selectedSlot;inventory.main=new ItemStack[36];inventory.armor=new ItemStack[4];
    for(int i=0;i<36;i++) {
      inventory.main[i]=new ItemStack(35,i+1,i%16);
      DeathChecks.tag(inventory.main[i]).putString("payload","slot-"+i);
    }
    inventory.armor[0]=new ItemStack(301,1,8);
    try {
      Hotbars.tick(mc);check(Hotbars.eligible(mc),"focused gameplay required");
      test("prepared preview remains inert until release and a ready plan publishes immediately",()->{
        var before=inventory.main.clone();
        Method preview=transaction().getDeclaredMethod("preview",Minecraft.class,int.class);preview.setAccessible(true);
        preview.invoke(null,mc,1);
        ((Future<?>)UiChecks.field(state("pending"),"write")).get(5,TimeUnit.SECONDS);tick(mc);
        check(InventoryRows.identical(before,inventory.main),"preview changed inventory before release");
        long start=System.nanoTime();Hotbars.swap(mc,1);
        check(state("pending")==null&&InventoryRows.identical(InventoryRows.swapped(before,1),inventory.main),"ready preview waited after release");
        log("HOTBAR prepared release ms="+(System.nanoTime()-start)/1e6);
        Hotbars.swap(mc,1);drain(mc);
      });
      test("changing a prepared preview row never publishes an unrequested swap",()->{
        var before=inventory.main.clone();
        Method preview=transaction().getDeclaredMethod("preview",Minecraft.class,int.class);preview.setAccessible(true);
        preview.invoke(null,mc,0);((Future<?>)UiChecks.field(state("pending"),"write")).get(5,TimeUnit.SECONDS);tick(mc);
        preview.invoke(null,mc,2);tick(mc);((Future<?>)UiChecks.field(state("pending"),"write")).get(5,TimeUnit.SECONDS);tick(mc);
        check(InventoryRows.identical(before,inventory.main),"row preview published inventory");
        Hotbars.swap(mc,2);check(InventoryRows.identical(InventoryRows.swapped(before,2),inventory.main),"latest preview row not committed");
        Hotbars.swap(mc,2);drain(mc);
      });
      test("hotbar publishes only after durable flush and preserves native stacks",()->{
        ItemStack[] before=inventory.main.clone();
        try(var blocked=new BlockedWriter()) {
          long start=System.nanoTime();Hotbars.swap(mc,1);tick(mc);
          double millis=(System.nanoTime()-start)/1e6;
          check(millis<150,"request blocked game thread: "+millis+" ms");
          check(state("pending")!=null && InventoryRows.identical(before,inventory.main),"published before flush");
          log("HOTBAR delayed-disk submit-and-tick ms="+millis);
        }
        drain(mc);check(InventoryRows.identical(InventoryRows.swapped(before,1),inventory.main),"wrong permutation");
        for(int i=0;i<36;i++)check(DeathChecks.tag(before[i]).getString("payload").equals("slot-"+i)
            && before[i].count==i+1 && before[i].getDamage()==i%16,"native data changed");
        check(inventory.armor[0].getDamage()==8,"armor changed");
      });
      test("rapid swaps retain request order and latest hotbar selection",()->{
        ItemStack[] expected=inventory.main.clone();
        try(var blocked=new BlockedWriter()) {
          for(int row:new int[]{0,1,2,1,0,2,2,0}) {Hotbars.swap(mc,row);expected=InventoryRows.swapped(expected,row);}
          inventory.selectedSlot=7;
        }
        drain(mc);check(InventoryRows.identical(expected,inventory.main),"rapid requests lost or reordered");
        check(inventory.selectedSlot==7,"selected slot overwritten");
      });
      for(String change:List.of("count","damage","nbt","array","armor","cursor","screen","focus","world","player")) {
        test("pending swap cancels safely after "+change+" changes",()->{
          try(var blocked=new BlockedWriter()) {
            Hotbars.swap(mc,2);Hotbars.swap(mc,1);
            switch(change) {
              case "count" -> inventory.main[0].count--;
              case "damage" -> inventory.main[0].setDamage(inventory.main[0].getDamage()+1);
              case "nbt" -> DeathChecks.tag(inventory.main[0]).putString("payload","edited");
              case "array" -> inventory.main=inventory.main.clone();
              case "armor" -> inventory.armor[0].setDamage(inventory.armor[0].getDamage()+1);
              case "cursor" -> {inventory.setCursorStack(new ItemStack(1,1,0));tick(mc);inventory.setCursorStack(null);}
              case "screen" -> {mc.setScreen(new Screen(){});mc.setScreen(null);}
              case "focus" -> {mc.field_2778=false;tick(mc);mc.field_2778=true;}
              case "world" -> {var world=mc.world;mc.world=null;tick(mc);mc.world=world;}
              case "player" -> {var player=mc.player;mc.player=null;tick(mc);mc.player=player;}
            }
          }
          ItemStack[] expected=inventory.main.clone();drain(mc);
          check(InventoryRows.identical(expected,inventory.main),"stale swap applied");
          check(!(boolean)state("failed"),"normal edit disabled future swaps");
        });
      }
      test("pending input queue stays bounded under stalled storage",()->{
        try(var blocked=new BlockedWriter()) {
          for(int i=0;i<1000;i++)Hotbars.swap(mc,i%3);
          check(((ArrayDeque<?>)state("rows")).size()==16,"unbounded queue");
          Hotbars.cancelPending();
        }
        drain(mc);check(((ArrayDeque<?>)state("rows")).isEmpty(),"cancel left queued actions");
      });
      test("repeated hotbar swaps keep per-tick work small",()->{
        double maxSubmit=0,maxCommit=0;
        for(int i=0;i<60;i++) {
          ItemStack[] before=inventory.main.clone();long start=System.nanoTime();Hotbars.swap(mc,i%3);
          maxSubmit=Math.max(maxSubmit,(System.nanoTime()-start)/1e6);
          Future<?> write=(Future<?>)UiChecks.field(state("pending"),"write");write.get(5,TimeUnit.SECONDS);
          start=System.nanoTime();tick(mc);maxCommit=Math.max(maxCommit,(System.nanoTime()-start)/1e6);
          check(InventoryRows.identical(InventoryRows.swapped(before,i%3),inventory.main),"stress swap mismatch");
        }
        log("HOTBAR 60 swaps max submit_ms="+maxSubmit+" max commit_ms="+maxCommit);
      });
    } finally {Hotbars.cancelPending();drain(mc);inventory.main=oldMain;inventory.armor=oldArmor;inventory.selectedSlot=selected;}
    log("RELEASE 171 HOTBAR FAILURES "+failures);
  }

  private static void failure(Minecraft mc) throws Exception {
    failures=0;mc.setScreen(null);active(false);DeathChecks.alive(mc);Hotbars.tick(mc);
    test("journal failure preserves inventory and stops all later requests",()->{
      Hotbars.swap(mc,0);drain(mc);var before=mc.player.inventory.main.clone();
      var path=java.nio.file.Path.of("power-beta-data/hotbar-validation-full.journal");
      java.nio.file.Files.deleteIfExists(path);var journal=new RecoveryJournal(path,RecoveryJournal.HEADER+1);journal.append(new byte[]{1});
      state("journal",journal);Hotbars.swap(mc,1);drain(mc);
      check((boolean)state("failed"),"failed writer did not disable swaps");
      check(InventoryRows.identical(before,mc.player.inventory.main),"failure changed inventory");
      Hotbars.swap(mc,2);check(state("pending")==null,"failure allowed another request");
      check(java.nio.file.Files.size(path)==RecoveryJournal.HEADER+1,"prior recovery data changed");
    });
    log("RELEASE 171 FAILURE FAILURES "+failures);
  }

  private static float[] matrix(Minecraft mc) throws Exception {
    GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();
    try {
      GL11.glLoadIdentity();Method method=class_555.class.getDeclaredMethod("method_1851",float.class);method.setAccessible(true);method.invoke(mc.field_2818,1F);
      FloatBuffer buffer=BufferUtils.createFloatBuffer(16);GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX,buffer);
      float[] result=new float[16];buffer.get(result);return result;
    } finally {GL11.glPopMatrix();}
  }
  private static void cameras(Minecraft mc) throws Exception {
    failures=0;scene(mc);mc.paused=false;
    Object camera=Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
    Object settings=Class.forName("local.luke.power.camera.FreecamConfig").getField("config").get(null);
    settings.getClass().getField("collision").set(settings,true);
    (AccessRules.of(mc.world.method_262())).allAllowed(true);
    CreativeVehicleChecks.mode(mc,"SURVIVAL");
    test("freecam floor collision reports the same Y as standing feet, including slabs",()->{
      for(int x:new int[]{32,-32})for(int block:new int[]{1,44}) {
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)mc.world.method_154(x+dx,108,35+dz,block,0);
        double floor=block==44?108.5:109;
        for(boolean feet:new boolean[]{false,true}) {
          active(false);pose(x+.5,floor+HeadGeometry.EYE_ABOVE_BASE+.03,35.5,0,0);active(true);
          camera.getClass().getMethod("frameRendered").invoke(camera);
          camera.getClass().getField("velocityY").setDouble(camera,-10);
          // Force a real frame movement with a known elapsed interval.
          Field time=Arrays.stream(mc.field_2818.getClass().getDeclaredFields()).filter(f->f.getType()==long.class && f.getName().contains("lastTime")).findFirst().orElseThrow();
          time.setAccessible(true);time.setLong(mc.field_2818,System.nanoTime()-100_000_000);
          mc.field_2818.method_1844(1F);MenuPreferences.previewFeetY(feet);
          double reported=HudCoordinates.y(mc.player);
          check(reported>=floor && reported<floor+.01,"floor collision reports "+reported+" expected "+floor);
          mc.player.method_1340(x+.5,floor+mc.player.field_1631,35.5);
          check(Math.floor(reported)==Math.floor(mc.player.boundingBox.minY+.000001),"head/feet block Y differs");
          mc.options.hideHud=false;mc.options.debugHud=true;mc.inGameHud.render(1,false,0,0);
          check(DebugOverlay.lastLayout().stream().anyMatch(line->line.text().equals("y: "+reported)),"F3 did not follow floor Y");
        }
      }
      active(false);MenuPreferences.previewFeetY(false);
    });
    test("spectator keeps full third-person distance inside terrain and with free look",()->{
      active(false);mc.player.method_1340(32.5,111.62,32.5);mc.player.prevX=mc.player.x;mc.player.prevY=mc.player.y;mc.player.prevZ=mc.player.z;
      mc.player.yaw=mc.player.prevYaw=180;mc.player.pitch=mc.player.prevPitch=0;mc.options.thirdPerson=true;
      for(int y=109;y<=113;y++)mc.world.method_154(32,y,32,1,0);
      CreativeVehicleChecks.mode(mc,"SURVIVAL");float[] clipped=matrix(mc);
      CreativeVehicleChecks.mode(mc,"SPECTATOR");float[] unclipped=matrix(mc);
      check(Math.abs(unclipped[14]-clipped[14])>1,"spectator camera still clipped");
      check(Math.abs(Math.abs(unclipped[14])-CameraView.distance())<.01,"wrong spectator distance "+unclipped[14]);
      check(GhostHead.visible(mc),"head hidden inside terrain");
      var old=local.luke.power.building.config.Config.current().copy();var config=old.copy();
      config.freeLookPerspective=local.luke.power.building.camera.Perspective.THIRD_PERSON;
      local.luke.power.building.config.Config.preview(config);
      try {local.luke.power.building.camera.FreeLook.begin(mc);local.luke.power.building.camera.FreeLook.turn(mc.player,200,100);
        float[] looked=matrix(mc);check(!Arrays.equals(unclipped,looked),"free look ignored");check(GhostHead.visible(mc),"free look hid ghost");
        boolean depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST);GhostHead.render(mc,1);check(GL11.glIsEnabled(GL11.GL_DEPTH_TEST)==depth,"head leaked depth state");
      } finally {local.luke.power.building.camera.FreeLook.reset(mc);local.luke.power.building.config.Config.preview(old);}
      CreativeVehicleChecks.mode(mc,"SURVIVAL");check(Math.abs(matrix(mc)[14]-clipped[14])<.01,"normal camera clipping did not return");
    });
    active(false);mc.options.thirdPerson=false;mc.player.method_1340(24.5,110.62,24.5);
    check(GL11.glGetError()==0,"camera GL error");log("RELEASE 171 CAMERA FAILURES "+failures);
  }

  private static void fire(Minecraft mc) throws Exception {
    failures=0;active(false);mc.setScreen(null);DeathChecks.alive(mc);
    (AccessRules.of(mc.world.method_262())).allAllowed(true);
    test("creative clicks extinguish every fire face and preserve its support",()->{
      CreativeVehicleChecks.mode(mc,"CREATIVE");
      for(int face=0;face<6;face++) {
        int x=28+face*2,y=115,z=28;
        int fx=x+(face==5?1:face==4?-1:0),fy=y+(face==1?1:face==0?-1:0),fz=z+(face==3?1:face==2?-1:0);
        mc.world.method_154(x,y,z,5,0);mc.world.method_154(fx,fy,fz,51,0);
        check(mc.world.getBlockId(fx,fy,fz)==51,"fixture fire missing");
        mc.interactionManager.method_1707(x,y,z,face);
        check(mc.world.getBlockId(fx,fy,fz)==0 && mc.world.getBlockId(x,y,z)==5,"fire click broke support, face "+face);
        mc.interactionManager.method_1721(x,y,z,face);check(mc.world.getBlockId(x,y,z)==5,"same-tick held mining broke support");
        mc.interactionManager.method_1707(x,y,z,face);check(mc.world.getBlockId(x,y,z)==0,"next click did not break normally");
      }
    });
    test("held creative mining extinguishes newly targeted fire first",()->{
      mc.world.method_154(30,115,30,5,0);mc.world.method_154(30,116,30,51,0);
      mc.interactionManager.method_1721(30,115,30,1);
      check(mc.world.getBlockId(30,116,30)==0 && mc.world.getBlockId(30,115,30)==5,"held fire broke support");
    });
    test("survival and spectator retain their normal fire permissions",()->{
      mc.world.method_154(30,115,30,5,0);mc.world.method_154(30,116,30,51,0);
      CreativeVehicleChecks.mode(mc,"SPECTATOR");mc.interactionManager.method_1707(30,115,30,1);
      check(mc.world.getBlockId(30,116,30)==51 && mc.world.getBlockId(30,115,30)==5,"spectator changed terrain");
      CreativeVehicleChecks.mode(mc,"SURVIVAL");mc.interactionManager.method_1707(30,115,30,1);
      check(mc.world.getBlockId(30,116,30)==0 && mc.world.getBlockId(30,115,30)==5,"survival fire changed");
    });
    log("RELEASE 171 FIRE FAILURES "+failures);
  }
}
