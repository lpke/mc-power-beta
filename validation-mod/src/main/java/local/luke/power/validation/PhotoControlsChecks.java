package local.luke.power.validation;

import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.Validation.*;
import com.google.gson.*;
import java.nio.file.*;
import local.luke.power.world.WorldCycles;
import local.luke.power.input.PhotoControlsLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import local.luke.power.validation.mixin.ScreenInput;

/** State for physical photo-mode controls in a disposable client. */
final class PhotoControlsChecks {
  private static Screen photo;
  private static long daylight;
  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("open")) {
      mc.setScreen(null);
      daylight = ((WorldCycles)mc.world.method_262()).power$daylightTime();
      photo = (Screen)Class.forName("local.luke.power.controls.tweaks.photomode.PhotoModeScreen")
          .getConstructor(Screen.class).newInstance(new net.minecraft.class_525());
      mc.setScreen(photo);
    } else if (action.equals("layout")) {
      int width=photo.width,height=photo.height;
      try {
        int wrap=320;
        while(wrap<854&&PhotoControlsLayout.at(wrap,240,mc.textRenderer::getWidth).rotationY()<216)wrap++;
        for(int[] size:new int[][]{{320,240},{359,240},{360,240},{427,240},{wrap-1,240},{wrap,240},{854,480}}) {
          photo.init(mc,size[0],size[1]);
          var buttons=((ScreenInput)photo).power$buttons();
          for(int i=0;i<buttons.size();i++) {
            var a=buttons.get(i);var ab=(local.luke.power.validation.mixin.ButtonBounds)a;
            check(a.x>=8&&a.y>=6&&a.x+ab.power$width()<=photo.width-8&&a.y+ab.power$height()<=photo.height-6,"clipped photo control");
            check(ab.power$height()==18,"photo control height differs from maps");
            for(int j=i+1;j<buttons.size();j++) {
              var b=buttons.get(j);var bb=(local.luke.power.validation.mixin.ButtonBounds)b;
              check(a.x+ab.power$width()<=b.x||b.x+bb.power$width()<=a.x||a.y+ab.power$height()<=b.y||b.y+bb.power$height()<=a.y,"overlapping photo controls "+a.id+" "+b.id+" width="+size[0]);
            }
          }
        }
      } finally {photo.init(mc,width,height);}
      log("PASS photo controls remain separate at narrow and wide sizes");
    } else if (action.equals("resize")) {
      photo.init(mc, photo.width, photo.height);
    } else if (action.equals("closed")) {
      check(mc.currentScreen != photo,"photo mode stayed open");
      check(((WorldCycles)mc.world.method_262()).power$daylightTime() - daylight < 20, "preview time leaked after exit");
      check(Class.forName("local.luke.power.controls.util.ModOptions").getField("photoModeFogMultiplier").getFloat(null)==1,"fog override leaked");
      log("PASS photo mode restores time and fog on exit");
    } else if (!action.equals("state")) throw new IllegalArgumentException(action);
    JsonObject out = new JsonObject();
    out.addProperty("open",mc.currentScreen==photo);
    out.addProperty("screen",mc.currentScreen==null?"none":mc.currentScreen.getClass().getName());
    out.addProperty("focused",org.lwjgl.opengl.Display.isActive());
    out.addProperty("grabbed",org.lwjgl.input.Mouse.isGrabbed());
    out.addProperty("daylight",((WorldCycles)mc.world.method_262()).power$daylightTime());
    out.addProperty("originalDaylight",daylight);
    out.addProperty("width",photo.width);out.addProperty("height",photo.height);
    out.addProperty("displayWidth",mc.displayWidth);out.addProperty("displayHeight",mc.displayHeight);
    for (String name : new String[]{"rotationGoal","zoomGoal","tiltGoal","cameraX","cameraY"})
      out.addProperty(name,(Number)field(photo,name));
    out.addProperty("renderPlayer",(boolean)field(photo,"renderPlayer"));
    out.addProperty("dragButton",(int)field(photo,"dragButton"));
    JsonObject buttons = new JsonObject();
    for (String name : new String[]{"exitButton","centerButton","rotateLeftButton","rotateRightButton","screenshotButton","renderPlayerButton","tiltSlider","timeSlider"}) {
      var button=(ButtonWidget)field(photo,name);
      JsonObject b=new JsonObject();b.addProperty("x",button.x);b.addProperty("y",button.y);
      var bounds=(local.luke.power.validation.mixin.ButtonBounds)button;
      b.addProperty("width",bounds.power$width());b.addProperty("height",bounds.power$height());buttons.add(name,b);
    }
    out.add("buttons",buttons);
    Files.writeString(Path.of("power-beta-photo-state.json"),out.toString());
  }
}
