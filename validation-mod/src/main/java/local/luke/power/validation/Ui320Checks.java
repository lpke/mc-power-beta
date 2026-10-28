package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;

import com.google.gson.JsonElement;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import local.luke.power.audio.AudioController;
import local.luke.power.audio.ExtraSounds;
import local.luke.power.config.*;
import local.luke.power.map.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/** Native chrome, waypoint controls and modal ownership. Disposable clients only. */
final class Ui320Checks {
  private static final Path REPORT = Path.of("power-beta-data/reports/release-320/ui");
  private static final int WIDE = 960, SHORT = 270;

  private static Object invoke(Object target, String name, Class<?>[] types, Object... args) throws Exception {
    return Editor230Checks.invoke(target, name, types, args);
  }
  private static int number(Object target, String name, int value) throws Exception {
    return ((Number)invoke(target, name, new Class<?>[]{int.class}, value)).intValue();
  }
  private static void press(Screen screen, int x, int y) { ((ScreenInput)(Object)screen).power$click(x, y, 0); }
  private static void key(Screen screen, int code) { ((ScreenInput)(Object)screen).power$key('\0', code); }
  private static void open(Minecraft mc, Screen screen, int w, int h) { mc.setScreen(screen); screen.init(mc, w, h); }
  private static BufferedImage render(Minecraft mc, Screen screen, String name, int w, int h) throws Exception {
    Path file = REPORT.resolve(name + ".png");
    GL11.glClearColor(.45f, .5f, .6f, 1);
    SettingsSnapshot.render(mc, screen, w, h, file);
    return ImageIO.read(file.toFile());
  }
  private static int light(BufferedImage image, int x, int y) {
    int rgb = image.getRGB(x * 2, y * 2);
    return (rgb >> 16 & 255) + (rgb >> 8 & 255) + (rgb & 255);
  }
  private static void fullHeader(BufferedImage baseline, BufferedImage actual, String name) {
    for (int x : new int[]{1, WIDE - 2})
      check(light(actual, x, 3) < light(baseline, x, 3), name + " title bar missed screen edge " + x);
  }
  private static void rowPress(WaypointsScreen screen, Waypoint point, long time) throws Exception {
    invoke(screen, "rowClick", new Class<?>[]{Waypoint.class, long.class}, point, time);
  }
  private record SettingControl(int x, int y, int right) {}
  private static Object settingRow(PowerOptionsScreen options, Setting setting) throws Exception {
    return ((List<?>)field(options,"rows")).stream().filter(value->{
      try{return call(value,"setting")==setting;}catch(Exception failure){throw new RuntimeException(failure);}
    }).findFirst().orElseThrow();
  }
  private static SettingControl settingControl(PowerOptionsScreen options, Setting setting) throws Exception {
    Object row=settingRow(options,setting);
    int x=((Number)invoke(options,"mainControlLeft",new Class<?>[]{row.getClass()},row)).intValue();
    int right=((Number)invoke(options,"controlEnd",new Class<?>[]{Setting.class},setting)).intValue();
    int y=(int)call(options,"top")+(int)call(row,"y")-((Double)field(options,"scroll")).intValue()+((int)call(row,"height")==40?16:3);
    return new SettingControl(x,y,right);
  }
  private static Map<String,JsonElement> draftValues(PowerOptionsScreen options) {
    var values=new LinkedHashMap<String,JsonElement>();
    options.session().settings().forEach(setting->values.put(setting.id,setting.value.deepCopy()));
    return values;
  }

  static void run(Minecraft mc) throws Exception {
    failures = 0;
    check(mc.world != null && mc.player != null, "open a disposable world first");
    check(org.lwjgl.opengl.Display.getWidth() >= WIDE * 2 && org.lwjgl.opengl.Display.getHeight() >= 960,
        "resize disposable client to at least 1920 x 960 before UI checks");
    Files.createDirectories(REPORT);
    Screen previous = mc.currentScreen;
    boolean auto = MenuPreferences.current().autoApply;
    PowerConfig.flushPending(); byte[] config = Files.readAllBytes(PowerConfig.path());
    var storeField = Maps.class.getDeclaredField("waypoints"); storeField.setAccessible(true);
    WaypointStore previousStore = (WaypointStore)storeField.get(null);
    Path data = Files.createTempDirectory(REPORT, "waypoints-");
    WaypointStore store = new WaypointStore(data, message -> { throw new AssertionError(message); });
    for (int i = 0; i < 2000 && !store.ready(); i++) { store.tick(); Thread.sleep(1); }
    check(store.ready(), "isolated waypoint fixture did not load");
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    try {
      MenuPreferences.current().autoApply = false; storeField.set(null, store);
      var point = new Waypoint(); point.name = "UI waypoint"; point.abbreviation = "UI";
      point.x = -57; point.y = 81; point.z = 113; point.colour = 0x46acf2;
      store.createSet("UI set"); point.set = "UI set"; store.put(point);

      test("all dark title bars cover both edges of an ultrawide GUI", () -> {
        UiScreen empty = new UiScreen() { public void render(int x, int y, float delta) { renderBackground(); } };
        open(mc, empty, WIDE, SHORT); BufferedImage baseline = render(mc, empty, "background", WIDE, SHORT);
        var options = new PowerOptionsScreen(null); open(mc, options, WIDE, SHORT);
        field(options, "page", "Map"); call(options, "layout");
        BufferedImage image = render(mc, options, "options-wide", WIDE, SHORT);
        fullHeader(baseline, image, "Options");
        for (int x : new int[]{1, WIDE - 2})
          check(light(image, x, SHORT - 2) < light(baseline, x, SHORT - 2), "Options footer missed screen edge " + x);
        var screens = new LinkedHashMap<String, Screen>();
        screens.put("waypoints", new WaypointsScreen(options));
        screens.put("waypoint-editor", new WaypointEditorScreen(options, point));
        screens.put("map-coordinates", new MapCoordinatesScreen(options, 0, 0, (x, z) -> {}));
        screens.put("defaults-setup", new DefaultsSetupScreen(options));
        screens.put("hud-editor", new HudEditorScreen(options, HudEditorSettings.ENTRY));
        screens.put("title-lettering", new TitleLetteringScreen(options));
        var constructor = RuleDefaultsScreen.class.getDeclaredConstructor(Screen.class, RuleDefaults.Review.class, Runnable.class, Runnable.class, String.class);
        constructor.setAccessible(true);
        screens.put("rule-review", constructor.newInstance(options, RuleDefaults.review(), (Runnable)() -> {}, (Runnable)() -> {}, "General settings"));
        for (var entry : screens.entrySet()) {
          open(mc, entry.getValue(), WIDE, SHORT);
          fullHeader(baseline, render(mc, entry.getValue(), entry.getKey() + "-wide", WIDE, SHORT), entry.getKey());
        }
        open(mc, options, WIDE, SHORT); field(options, "savedWorldName", "UI audit"); call(options, "layout");
        fullHeader(baseline, render(mc, options, "saved-rules-wide", WIDE, SHORT), "Saved world rules");
        var rules = new local.luke.power.rules.CreationRules(local.luke.power.rules.RulePresets.survival(), local.luke.power.rules.RulePresets.cheats());
        PowerOptionsScreen.openCreationRules(mc, options, rules); mc.currentScreen.init(mc, WIDE, SHORT);
        fullHeader(baseline, render(mc, mc.currentScreen, "creation-rules-wide", WIDE, SHORT), "Creation rules");
      });

      test("embedded chrome stays in the sidebar and Apply retains padding at every width", () -> {
        var map = new WorldMapScreen(); open(mc, map, WIDE, SHORT);
        var panel = (PowerOptionsScreen)invokeStatic(PowerOptionsScreen.class, "mapPanel", new Class<?>[]{WorldMapScreen.class}, map);
        panel.init(mc, WIDE, SHORT);
        invoke(panel, "panelSize", new Class<?>[]{int.class}, 360);
        var blank = new UiScreen() { public void render(int x, int y, float delta) {} };
        open(mc, blank, WIDE, SHORT); BufferedImage baseline = render(mc, blank, "panel-background", WIDE, SHORT);
        BufferedImage image = render(mc, panel, "panel-only", WIDE, SHORT);
        for (int[] p : new int[][]{{361,3}, {WIDE-2,3}, {361,SHORT-2}, {WIDE-2,SHORT-2}, {361,SHORT/2}})
          check(image.getRGB(p[0]*2,p[1]*2) == baseline.getRGB(p[0]*2,p[1]*2), "panel chrome covered map at " + Arrays.toString(p));
        for (int width : new int[]{200, 240, 280, 320, 360, 469, 470, 640}) {
          invoke(panel, "panelSize", new Class<?>[]{int.class}, width);
          int apply = number(panel,"actionWidth",1), x = number(panel,"actionX",1);
          check(apply >= mc.textRenderer.getWidth("Auto apply") + 16, "Apply padding lost at " + width);
          check(number(panel,"actionX",0) >= 0 && x+apply+4 == number(panel,"actionX",2), "footer actions overlap at " + width);
        }
        panel.session().discard();
      });

      test("waypoint footer aligns to content and its eye is centered", () -> {
        var screen = new WaypointsScreen(previous);
        for (int[] size : new int[][]{{320,240},{480,270},{960,270},{320,480}}) {
          open(mc, screen, size[0], size[1]); var bounds = call(screen,"layout");
          int left=(int)call(bounds,"left"), right=(int)call(bounds,"right"), footer=(int)call(bounds,"footerWidth");
          check((int)call(bounds,"newX")==left && (int)call(bounds,"doneX")+footer==right, "waypoint footer lost content edges");
          render(mc,screen,"waypoints-"+size[0]+"x"+size[1],size[0],size[1]);
        }
        open(mc, screen, 480, 270); var image=render(mc,screen,"waypoints-eye",480,270);
        int x=(int)call(screen,"left")+6, y=77, minX=18,maxX=-1,minY=18,maxY=-1;
        for(int dy=2;dy<16;dy++)for(int dx=2;dx<16;dx++)if((image.getRGB((x+dx)*2,(y+dy)*2)&0xffffff)==0xffffff){
          minX=Math.min(minX,dx);maxX=Math.max(maxX,dx);minY=Math.min(minY,dy);maxY=Math.max(maxY,dy);
        }
        check(minX==4&&maxX==12&&minY==5&&maxY==11,"eye glyph is off center: "+List.of(minX,maxX,minY,maxY));
      });

      test("Options mob colors toggle inheritance and keep nested color edits in the session", () -> {
        var options = new PowerOptionsScreen(previous); open(mc,options,640,360);
        try {
          Setting color=options.session().settings().stream().filter(s->s.id.equals("maps.radarCreeperColor")).findFirst().orElseThrow();
          Setting category=options.session().settings().stream().filter(s->s.id.equals("maps.radarHostileColor")).findFirst().orElseThrow();
          category.value=new com.google.gson.JsonPrimitive(0x123456); options.changed(category);
          color.value=new com.google.gson.JsonPrimitive(-1); options.changed(color);
          field(options,"conflictIds",List.of(color.id)); field(options,"scroll",0d); call(options,"layout");
          SettingControl control=settingControl(options,color);
          press(options,control.x()+3,control.y()+3);
          check(color.value.getAsInt()==0x123456,"Custom did not copy the session category color");
          check(mc.currentScreen==options,"mode toggle opened an editor");
          var geometry=invokeStatic(Class.forName("local.luke.power.ui.InheritedColorControl"),"at",new Class<?>[]{int.class,int.class,boolean.class},control.x(),control.right()-control.x(),true);
          press(options,(int)call(geometry,"colorX")+3,control.y()+3);
          check(mc.currentScreen instanceof ColourScreen,"custom swatch did not open the shared picker");
          key(mc.currentScreen,Keyboard.KEY_ESCAPE); check(mc.currentScreen==options,"nested picker lost its Options session");
          check(color.value.getAsInt()==0x123456,"canceling the picker changed the session color");
          // setScreen restores the parent at the native GUI size, rebuilding these rows.
          control=settingControl(options,color);
          press(options,control.x()+3,control.y()+3); check(color.value.getAsInt()==-1,"Custom mode did not return to Inherit");
          options.init(mc,640,360);
          render(mc,options,"inherited-mob-color",640,360);
          check(Arrays.equals(config,Files.readAllBytes(PowerConfig.path())),"color mode wrote a manual session");
        } finally { options.session().discard(); }
      });

      test("row double-click edits while eye, keyboard and modal input cancel it", () -> {
        var screen = new WaypointsScreen(previous); open(mc, screen, 480, 270);
        rowPress(screen,point,1_000_000_000L); ((ScreenInput)(Object)screen).power$release(200,80,0);
        rowPress(screen,point,1_200_000_000L);
        check(mc.currentScreen instanceof WaypointEditorScreen,"double-click did not open waypoint editor");
        check(((Waypoint)field(mc.currentScreen,"initial")).id.equals(point.id),"double-click opened a different waypoint");
        open(mc,screen,480,270); rowPress(screen,point,2_000_000_000L);
        ((ScreenInput)(Object)screen).power$release(200,80,0); press(screen,(int)call(screen,"left")+10,80);
        rowPress(screen,point,2_100_000_000L); check(mc.currentScreen==screen,"eye click completed an edit gesture");
        key(screen,Keyboard.KEY_LEFT); rowPress(screen,point,2_200_000_000L);
        check(mc.currentScreen==screen,"keyboard input retained double-click ownership");
      });

      test("all extra-sound speakers preview and stop without changing toggles or manual drafts", () -> {
        var ids=List.of("audio.redstonePlacementSound","audio.seedPlantingSounds","audio.hoeTillingSounds","audio.bucketSounds","audio.boneMealSounds");
        var options=new PowerOptionsScreen(previous);open(mc,options,640,360);
        try {
          Setting invert=options.session().settings().stream().filter(setting->setting.id.equals("native.invert")).findFirst().orElseThrow();
          invert.value=new com.google.gson.JsonPrimitive(!invert.value.getAsBoolean());options.changed(invert);
          check(options.session().changes()>0,"sound preview needs an existing manual draft");
          field(options,"page","Audio");field(options,"libraryOpen",false);field(options,"conflictIds",ids);
          ((TextInput)field(options,"search")).setText("");field(options,"scroll",0d);options.init(mc,640,360);
          long changes=options.session().changes();var draft=draftValues(options);
          render(mc,options,"extra-sound-speakers",640,360);
          for(String id:ids) {
            Setting setting=options.session().settings().stream().filter(value->value.id.equals(id)).findFirst().orElseThrow();
            JsonElement enabled=setting.value.deepCopy();String preview=ExtraSounds.previewId(id);
            check(preview!=null&&preview.equals(invoke(options,"previewId",new Class<?>[]{Setting.class},setting)),"speaker advertises wrong sound: "+id);
            check(!AudioController.previewing(preview),"sound was already previewing before its button click: "+id);
            Object row=settingRow(options,setting);SettingControl control=settingControl(options,setting);
            int speaker=((Number)invoke(options,"controlLeft",new Class<?>[]{row.getClass()},row)).intValue();
            check(control.x()-speaker==22,"speaker has no separate render/hit bounds: "+id);
            press(options,speaker+9,control.y()+9);
            check(AudioController.previewing(preview),"Options speaker did not start native preview: "+id);
            check(setting.value.equals(enabled)&&options.session().changes()==changes&&draftValues(options).equals(draft),"preview changed a toggle or manual draft: "+id);
            render(mc,options,"extra-sound-preview-"+id.substring("audio.".length()),640,360);
            press(options,speaker+9,control.y()+9);
            check(!AudioController.previewing(preview),"second speaker click did not stop preview: "+id);
            check(setting.value.equals(enabled)&&options.session().changes()==changes&&draftValues(options).equals(draft),"stopping preview changed a toggle or manual draft: "+id);
          }
          check(Arrays.equals(config,Files.readAllBytes(PowerConfig.path())),"extra-sound previews wrote a manual settings session");
        } finally {
          for(String id:ids){String preview=ExtraSounds.previewId(id);if(AudioController.previewing(preview))AudioController.previewSound(preview);}
          options.session().discard();
        }
      });

      test("set deletion confirms, isolates modal input and keeps all waypoint data", () -> {
        var screen=new WaypointsScreen(previous); open(mc,screen,480,270); store.select("UI set");
        var bounds=call(screen,"layout"); press(screen,(int)call(bounds,"deleteX")+4,55);
        check(field(screen,"deletingSet").equals("UI set"),"Delete set skipped confirmation");
        key(screen,Keyboard.KEY_RETURN); check(store.sets().contains("UI set"),"Enter accepted destructive confirmation");
        press(screen,(int)call(bounds,"newX")+4,250); check(mc.currentScreen==screen,"modal input opened waypoint editor");
        key(screen,Keyboard.KEY_ESCAPE); check(store.sets().contains("UI set"),"Escape deleted set");
        press(screen,(int)call(bounds,"deleteX")+4,55); store.allSets(true);
        press(screen,480/2-108,270/2+24); check(store.sets().contains("UI set"),"stale confirmation deleted a changed set");
        store.select("UI set"); press(screen,(int)call(bounds,"deleteX")+4,55);
        render(mc,screen,"delete-set-confirm",480,270);
        Waypoint before=store.points().get(0); before.set="Default";
        press(screen,480/2-108,270/2+24);
        check(!store.sets().contains("UI set")&&store.selectedSet().equals("Default"),"set deletion did not choose Default");
        check(Catalog.JSON.toJson(before).equals(Catalog.JSON.toJson(store.points().get(0))),"set deletion changed waypoint data");
        press(screen,(int)call(bounds,"deleteX")+4,55); check(field(screen,"deletingSet").equals(""),"Default deletion was offered");
        store.finish(); check(WaypointStore.decode(Files.readAllBytes(data.resolve("waypoints.json"))).waypoints.get(0).id.equals(point.id),"set deletion did not persist point identity");
      });
      check(Arrays.equals(config, Files.readAllBytes(PowerConfig.path())), "UI checks published settings drafts");
    } finally {
      store.finish(); storeField.set(null,previousStore); MenuPreferences.current().autoApply=auto;
      mc.setScreen(previous); GL11.glPopAttrib();
      try(var files=Files.walk(data)){for(Path path:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}
    }
    log("UI320 COMPLETE failures="+failures);
  }

  private static Object invokeStatic(Class<?> type,String name,Class<?>[] parameters,Object... args)throws Exception {
    var method=type.getDeclaredMethod(name,parameters);method.setAccessible(true);return method.invoke(null,args);
  }
}
