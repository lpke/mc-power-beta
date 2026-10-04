package local.luke.power.validation;

import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.Validation.*;
import com.google.gson.JsonPrimitive;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public final class FilteredNavigationChecks {
  private static TextInput search(PowerOptionsScreen s) throws Exception {
    return (TextInput)field(s,"search");
  }
  private static void query(PowerOptionsScreen s, String query) throws Exception {
    search(s).setText(""); search(s).focused = true;
    for (char c : query.toCharArray()) ((ScreenInput)(Object)s).power$key(c,0);
    check(search(s).cursor() == query.length(), "typed cursor mismatch");
    search(s).focused = false;
    call(s,"layout");
  }
  private static Object row(PowerOptionsScreen s, Setting setting) throws Exception {
    for (Object row : (List<?>)field(s,"rows")) if (call(row,"setting") == setting) return row;
    throw new AssertionError("Missing row " + setting.id);
  }
  private static void render(PowerOptionsScreen s) throws Exception {
    while (GL11.glGetError() != GL11.GL_NO_ERROR) {}
    s.render(-1,-1,0);
    check(GL11.glGetError() == GL11.GL_NO_ERROR,"render error");
  }
  private static void link(PowerOptionsScreen s, Setting setting, int dx) throws Exception {
    Object row=row(s,setting);
    field(s,"scroll",(double)(int)call(row,"y"));
    int x=(int)invoke(s,"controlLeft",row.getClass(),row)+dx;
    int y=(int)call(s,"top")+((int)call(row,"height")==40?16:2)+8;
    ((ScreenInput)(Object)s).power$click(x,y,0);
    render(s);
  }
  private static Object invoke(Object o,String name,Class<?> type,Object arg) throws Exception {
    var m=o.getClass().getDeclaredMethod(name,type);m.setAccessible(true);return m.invoke(o,arg);
  }
  public static void run(Minecraft mc) throws Exception {
    failures=0;
    var parent=mc.currentScreen;
    PowerOptionsScreen s=new PowerOptionsScreen(parent);mc.setScreen(s);
    boolean oldShow=(boolean)field(s,"showDisabled");
    try {
      field(s,"showDisabled",true);
      for (String id:List.of("keys.Fast place (toggle)","tweaks.placement.enabled"))
        test("typed search link renders and Back restores query: "+id,()->{
          query(s,"fast place");
          Setting setting=find(s.session(),id);
          link(s,setting,8);
          check(search(s).text().isEmpty(),"filter not cleared");
          check(!((List<?>)field(s,"relatedIds")).isEmpty(),"related list missing");
          call(s,"back");render(s);
          check(search(s).text().equals("fast place"),"query lost");
        });
      test("conflict link after typed search is safe and keeps a fixed result list",()->{
        Setting a=find(s.session(),"keys.Fast place (toggle)"),b=find(s.session(),"keys.Fake sneak (toggle)");
        var av=a.value;var bv=b.value;a.value=new JsonPrimitive(Keyboard.KEY_F8);b.value=a.value;
        try {
          query(s,"fast place");link(s,a,-6);
          List<?> ids=List.copyOf((List<?>)field(s,"conflictIds"));
          check(ids.contains(a.id)&&ids.contains(b.id),"missing conflict");
          b.value=new JsonPrimitive(0);call(s,"layout");render(s);
          check(field(s,"conflictIds").equals(ids),"fixed conflicts changed");
          call(s,"back");render(s);
        } finally {a.value=av;b.value=bv;}
      });
      test("search heading opens its expanded section and Back restores results",()->{
        query(s,"fast place");
        Object heading=((List<?>)field(s,"rows")).get(0);
        Setting first=(Setting)call(((List<?>)field(s,"rows")).get(1),"setting");
        field(s,"scroll",0d);
        check(call(heading,"setting")==null,"heading missing");
        ((ScreenInput)(Object)s).power$click((int)call(s,"left")+8,(int)call(s,"top")+8,0);
        render(s);
        check(search(s).text().isEmpty()&&field(s,"page").equals(first.page),"did not open section");
        call(s,"back");render(s);check(search(s).text().equals("fast place"),"search lost");
      });
      test("changing to unsaved-settings filter clears a typed cursor safely",()->{
        Setting changed=find(s.session(),"tweaks.placement.enabled");var old=changed.value;
        changed.value=new JsonPrimitive(!changed.original().getAsBoolean());
        try {
          query(s,"fast place");
          ((ScreenInput)(Object)s).power$click((int)call(s,"origin")+110,(int)call(s,"footerY")+8,0);
          render(s);check((boolean)field(s,"changedOnly"),"changed filter missing");
        } finally {changed.value=old;field(s,"changedOnly",false);}
      });
      test("off toggle states never hide their actions or modifiers",()->{
        for (String id:List.of("tweaks.placement.enabled","tweaks.sneak.enabled","tweaks.slabs.enabled","tweaks.flexible.enabled")) {
          Setting state=find(s.session(),id);var old=state.value;state.value=new JsonPrimitive(false);
          try {for(Setting key:ControlLinks.controls(s.session(),state)) check(ControlLinks.enabled(s.session(),key),"hidden state action "+key.id);}
          finally{state.value=old;}
        }
      });
      test("shared sprint control remains available for flight or freecam",()->{
        Setting flight=find(s.session(),"creative.sprintFlight"),camera=find(s.session(),"power_camera:config.enabled"),
            sprint=find(s.session(),"power_camera:config.sprint"),key=find(s.session(),"keys.power_creative.sprint");
        var a=flight.value;var b=camera.value;var c=sprint.value;
        try {
          for (int bits=0;bits<8;bits++) {
            flight.value=new JsonPrimitive((bits&1)!=0);camera.value=new JsonPrimitive((bits&2)!=0);sprint.value=new JsonPrimitive((bits&4)!=0);
            check(ControlLinks.enabled(s.session(),key)==((bits&1)!=0||(bits&6)==6),"shared action hidden incorrectly");
          }
        } finally {flight.value=a;camera.value=b;sprint.value=c;}
      });
      test("feature gates hide only their unavailable actions and Show Disabled exposes them",()->{
        for(String[] pair:List.of(
          new String[]{"tweaks.autoWalk","keys.Auto-walk (toggle)"},
          new String[]{"tweaks.freeLook","keys.key.powerbeta.free_look"},
          new String[]{"tweaks.hotbar.swap","keys.power_building.hotbar.base"},
          new String[]{"tweaks.hotbar.scroll","keys.power_building.hotbar.scroll"},
          new String[]{"creative.modePicker","keys.power_creative.picker"},
          new String[]{"visual.containerPreview","keys.powerbeta.containerPreview"},
          new String[]{"power_camera:config.enabled","keys.Toggle Freecam"})) {
          Setting gate=find(s.session(),pair[0]),key=find(s.session(),pair[1]);var old=gate.value;
          try {
            gate.value=new JsonPrimitive(false);check(!ControlLinks.enabled(s.session(),key),"gate ignored "+pair[0]);
            gate.value=new JsonPrimitive(true);check(ControlLinks.enabled(s.session(),key),"enabled gate hidden");
          } finally{gate.value=old;}
        }
      });
    } finally {field(s,"showDisabled",oldShow);s.session().discard();mc.setScreen(parent);}
    log("FILTER NAVIGATION FAILURES "+failures);
  }
}
