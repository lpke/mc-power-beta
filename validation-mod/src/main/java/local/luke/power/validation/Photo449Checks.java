package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.field;

import java.lang.reflect.Method;
import local.luke.power.input.PhotoProjection;
import net.minecraft.class_555;
import net.minecraft.class_66;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/** Native camera checks and screenshot controls for a disposable world with loaded water. */
final class Photo449Checks {
  static void run(Minecraft mc, String action) throws Exception {
    check(mc.world!=null&&mc.player!=null&&!mc.world.isRemote&&org.lwjgl.opengl.Display.isActive(),"focus a disposable singleplayer world");
    check(mc.options.musicVolume==0&&mc.options.soundVolume==0,"mute the disposable client");
    if (action.equals("open")) {
      mc.setScreen(create(mc.currentScreen));
      log("PHOTO449 opened native photo preview");
      return;
    }
    if (action.equals("capture")) {
      check(isPhoto(mc.currentScreen), "photo preview must be open");
      field(mc.currentScreen, "shouldScreenshot", true);
      return;
    }
    if (action.equals("close")) {
      check(isPhoto(mc.currentScreen), "photo preview must be open");
      mc.setScreen((Screen) field(mc.currentScreen, "previousScreen"));
      return;
    }
    if (action.startsWith("view ")) {
      check(isPhoto(mc.currentScreen), "photo preview must be open");
      String[] values = action.substring(5).split(" ");
      set(mc.currentScreen, "rotation", Float.parseFloat(values[0]));
      set(mc.currentScreen, "tilt", Float.parseFloat(values[1]));
      if (values.length > 2) set(mc.currentScreen, "zoom", Float.parseFloat(values[2]));
      return;
    }
    if (!action.equals("check")) throw new IllegalArgumentException(action);

    failures = 0;
    Screen previous = mc.currentScreen, photo = create(previous);
    double x = mc.player.x, y = mc.player.y, z = mc.player.z;
    boolean thirdPerson = mc.options.thirdPerson;
    Object chunks = field(mc.worldRenderer, "field_1809");
    int transparent = transparentSections((class_66[]) chunks);
    int minX = (int) field(mc.worldRenderer, "field_1776"), minZ = (int) field(mc.worldRenderer, "field_1778");
    int maxX = (int) field(mc.worldRenderer, "field_1779"), maxZ = (int) field(mc.worldRenderer, "field_1781");
    GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix();
    GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix();
    try {
      mc.setScreen(photo);
      test("photo 4.4.9 retains native sky background color", () -> {
        Method colors=class_555.class.getDeclaredMethod("method_1852",float.class);
        colors.setAccessible(true);
        var cycles=(local.luke.power.world.WorldCycles)mc.world.method_262();
        long daylight=cycles.power$daylightTime();
        var old=BufferUtils.createFloatBuffer(16);
        GL11.glGetFloat(GL11.GL_COLOR_CLEAR_VALUE,old);
        String[] fogFields={"field_2346","field_2347","field_2348"};
        float[] fog=new float[3];
        for(int i=0;i<3;i++)fog[i]=(float)field(mc.field_2818,fogFields[i]);
        try {
          cycles.power$daylightTime(6000);
          mc.currentScreen=null;
          colors.invoke(mc.field_2818,1F);
          var nativeColor=BufferUtils.createFloatBuffer(16);
          GL11.glGetFloat(GL11.GL_COLOR_CLEAR_VALUE,nativeColor);
          mc.currentScreen=photo;
          colors.invoke(mc.field_2818,1F);
          var photoColor=BufferUtils.createFloatBuffer(16);
          GL11.glGetFloat(GL11.GL_COLOR_CLEAR_VALUE,photoColor);
          for(int i=0;i<4;i++)check(Math.abs(nativeColor.get(i)-photoColor.get(i))<1e-6,"photo replaced native sky color");
          check(photoColor.get(2)>.2F,"daytime background is dark; use an outdoor fixture");
        } finally {
          mc.currentScreen=photo;cycles.power$daylightTime(daylight);
          for(int i=0;i<3;i++)field(mc.field_2818,fogFields[i],fog[i]);
          GL11.glClearColor(old.get(0),old.get(1),old.get(2),old.get(3));
        }
      });
      test("photo 4.4.9 uses existing finite renderer bounds", () -> {
        check(bounds(mc, "spanX") == maxX - minX + 16, "wrong X footprint");
        check(bounds(mc, "spanZ") == maxZ - minZ + 16, "wrong Z footprint");
        check(bounds(mc, "centerX") == ((double) minX + maxX + 16) / 2, "wrong X center");
        check(bounds(mc, "centerZ") == ((double) minZ + maxZ + 16) / 2, "wrong Z center");
      });
      test("photo 4.4.9 native projection preserves reference zoom at every tilt", () -> {
        Method camera = class_555.class.getDeclaredMethod("method_1840", float.class, int.class);
        camera.setAccessible(true);
        for (boolean third : new boolean[]{false, true}) {
          mc.options.thirdPerson = third;
          for (float rotation : new float[]{0, .5f, 1, 2}) for (float tilt : new float[]{0, 30, 90})
              for (float zoom : new float[]{.01F, 1.25F, 12F}) {
            set(photo, "rotation", rotation); set(photo, "tilt", tilt); set(photo,"zoom",zoom);
            camera.invoke(mc.field_2818, 1F, 0);
            var matrix = BufferUtils.createFloatBuffer(16);
            GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, matrix);
            double height = PhotoProjection.height() / PhotoProjection.zoomFactor((float)field(photo,"zoom"));
            double width = height * mc.displayWidth / mc.displayHeight;
            double expected = 2 * Math.cos(Math.toRadians(45+90*rotation)) / width;
            check(Math.abs(matrix.get(0)-expected) < 1e-5, "native projection changed reference zoom scale");
            for (int i=0;i<16;i++) check(Float.isFinite(matrix.get(i)), "invalid matrix at tilt "+tilt);
            check((float)field(photo,"tilt") == tilt, "projection altered selected tilt");
          }
        }
      });
      test("photo 4.4.9 owns camera while preserving active freecam", () -> freecam(mc, photo));
      test("photo 4.4.9 leaves terrain layers and player untouched", () -> {
        check(field(mc.worldRenderer,"field_1809") == chunks, "camera replaced renderer grid");
        check(transparentSections((class_66[])chunks) == transparent, "camera changed translucent terrain");
        check(mc.player.x==x && mc.player.y==y && mc.player.z==z, "photo mode moved player");
        check((int)field(mc.worldRenderer,"field_1776")==minX && (int)field(mc.worldRenderer,"field_1778")==minZ,
            "photo camera moved renderer grid");
      });
      log("PHOTO449 translucent terrain sections "+transparent+"; verify visible water in native screenshot");
    } finally {
      mc.options.thirdPerson = thirdPerson;
      mc.setScreen(previous);
      GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPopMatrix();
      GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }
    log("PHOTO449 FAILURES "+failures);
    check(failures==0,"photo-449 failures="+failures);
  }

  private static Screen create(Screen previous) throws Exception {
    return (Screen)Class.forName("local.luke.power.controls.tweaks.photomode.PhotoModeScreen")
        .getConstructor(Screen.class).newInstance(previous);
  }
  private static boolean isPhoto(Screen screen) {
    return screen != null && screen.getClass().getSimpleName().equals("PhotoModeScreen");
  }
  private static void set(Screen photo, String name, float value) throws Exception {
    field(photo,name,value); field(photo,name+"Goal",value);
  }
  private static double bounds(Minecraft mc, String name) throws Exception {
    return ((Number)mc.worldRenderer.getClass().getMethod("powerPhoto$"+name).invoke(mc.worldRenderer)).doubleValue();
  }
  private static int transparentSections(class_66[] chunks) {
    int count=0;
    for (class_66 chunk:chunks) if (chunk!=null && !chunk.field_244[1]) count++;
    return count;
  }

  private static void freecam(Minecraft mc, Screen photo) throws Exception {
    Object controller=Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
    Class<?> type=controller.getClass();
    Method active=type.getMethod("setActive",boolean.class), pose=type.getMethod("getCameraPosition");
    Method position=type.getMethod("setCameraPositionAndRotation",double.class,double.class,double.class,float.class,float.class,float.class);
    boolean wasActive=(boolean)type.getMethod("isActive").invoke(controller);
    boolean movement=type.getField("allowPlayerMovement").getBoolean(controller);
    Object before=pose.invoke(controller).getClass().getMethod("clone").invoke(pose.invoke(controller));
    try {
      // The first matrix is the photo camera before detaching or moving freecam.
      set(photo,"rotation",0);set(photo,"tilt",30);set(photo,"zoom",1.25F);
      float[] photoBefore=modelView(mc);
      mc.setScreen(null);
      active.invoke(controller,true);
      position.invoke(controller,mc.player.x+4096,mc.player.y+19,mc.player.z-4096,37F,123F,7F);
      float[] detachedBefore=modelView(mc);
      Object detached=pose.invoke(controller).getClass().getMethod("clone").invoke(pose.invoke(controller));
      mc.setScreen(photo);
      sameMatrix(photoBefore,modelView(mc),"freecam changed photo transform");
      check((boolean)type.getMethod("isActive").invoke(controller),"photo disabled freecam");
      samePose(detached,pose.invoke(controller));
      photoEntities(mc,photo);
      mc.setScreen(null);
      sameMatrix(detachedBefore,modelView(mc),"closing photo did not restore detached transform");
      samePose(detached,pose.invoke(controller));
      check((boolean)type.getMethod("isActive").invoke(controller),"closing photo disabled freecam");
    } finally {
      active.invoke(controller,wasActive);
      position.invoke(controller,field(before,"x"),field(before,"y"),field(before,"z"),
          field(before,"pitch"),field(before,"yaw"),field(before,"roll"));
      type.getField("allowPlayerMovement").setBoolean(controller,movement);
      mc.setScreen(photo);
    }
  }

  private static float[] modelView(Minecraft mc) throws Exception {
    Method camera=class_555.class.getDeclaredMethod("method_1840",float.class,int.class);
    camera.setAccessible(true);camera.invoke(mc.field_2818,1F,0);
    var buffer=BufferUtils.createFloatBuffer(16);GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX,buffer);
    float[] matrix=new float[16];buffer.get(matrix);return matrix;
  }
  private static void sameMatrix(float[] expected,float[] actual,String message) {
    for(int i=0;i<16;i++)check(Math.abs(expected[i]-actual[i])<1e-5,message+" element "+i);
  }
  private static void samePose(Object expected,Object actual) throws Exception {
    for(String name:new String[]{"x","y","z","pitch","yaw","roll"})
      check(field(expected,name).equals(field(actual,name)),"photo changed freecam "+name);
  }

  @SuppressWarnings("unchecked")
  private static void photoEntities(Minecraft mc,Screen photo) throws Exception {
    var visual=local.luke.power.visual.VisualConfig.copy();
    boolean third=mc.options.thirdPerson,renderPlayer=(boolean)field(photo,"renderPlayer");
    var blocks=(java.util.List<net.minecraft.block.entity.BlockEntity>)field(mc.worldRenderer,"field_1795");
    var previous=new java.util.ArrayList<>(blocks);
    var sign=new net.minecraft.block.entity.SignBlockEntity();
    sign.world=mc.world;sign.x=(int)Math.floor(mc.player.x)+1;
    sign.y=Math.max(1,Math.min(126,(int)Math.floor(mc.player.y)));sign.z=(int)Math.floor(mc.player.z);
    try {
      var shown=local.luke.power.visual.VisualConfig.copy();shown.hideEntities=false;
      local.luke.power.visual.VisualConfig.preview(shown);
      blocks.clear();blocks.add(sign);
      for(boolean perspective:new boolean[]{false,true}) for(boolean body:new boolean[]{false,true}) {
        mc.options.thirdPerson=perspective;field(photo,"renderPlayer",body);
        FreecamDistanceChecks.render(mc);
        check(FreecamDistanceChecks.entities.contains(mc.player)==body,
            "far freecam changed photo Render Player "+body+" third="+perspective);
        check(FreecamDistanceChecks.blocks.contains(sign),"far freecam hid photo block entity");
        check(!local.luke.power.render.GhostHead.visible(mc),"freecam ghost head leaked into photo");
        local.luke.power.render.GhostHead.render(mc,1F);
      }
      log("PASS photo 4.4.9 native player On/Off, nearby block entity and no ghost heads with far freecam");
    } finally {
      FreecamDistanceChecks.recording=false;
      FreecamDistanceChecks.entities.clear();FreecamDistanceChecks.blocks.clear();
      blocks.clear();blocks.addAll(previous);
      local.luke.power.visual.VisualConfig.preview(visual);
      mc.options.thirdPerson=third;field(photo,"renderPlayer",renderPlayer);
    }
  }
}
