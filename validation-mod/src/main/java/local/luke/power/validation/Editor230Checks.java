package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.*;
import org.lwjgl.input.Keyboard;

/** Exercise real draft screens, input routing, native textures and persistence in a disposable clone. */
final class Editor230Checks {
  private static final Path REPORT = Path.of("power-beta-data/reports/editors-230");
  static Object get(Object target, String name) throws Exception {
    for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) try {
      Field f = type.getDeclaredField(name); f.setAccessible(true); return f.get(target);
    } catch (NoSuchFieldException ignored) {}
    throw new NoSuchFieldException(name);
  }
  static Object invoke(Object target, String name, Class<?>[] types, Object... args) throws Exception {
    for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) try {
      Method m = type.getDeclaredMethod(name, types); m.setAccessible(true); return m.invoke(target, args);
    } catch (NoSuchMethodException ignored) {}
    throw new NoSuchMethodException(name);
  }
  private static void set(Object target, String name, Object value) throws Exception {
    for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) try {
      Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(target, value); return;
    } catch (NoSuchFieldException ignored) {}
    throw new NoSuchFieldException(name);
  }
  private static int number(Object o, String method) throws Exception { return ((Number)invoke(o, method, new Class<?>[0])).intValue(); }
  private static void click(Screen screen, int x, int y, int button) { ((ScreenInput)(Object)screen).power$click(x, y, button); }
  private static void key(Screen screen, int key) { ((ScreenInput)(Object)screen).power$key((char)0, key); }
  private static void open(Minecraft mc, Screen screen, int w, int h) { mc.setScreen(screen); screen.init(mc, w, h); }
  private static SettingsDraft draft(Screen screen) throws Exception { return (SettingsDraft)get(screen, "draft"); }
  private static void refresh(Screen screen) throws Exception { invoke(screen, "afterEdit", new Class<?>[0]); }
  private static void screenshot(Minecraft mc, Screen screen, String name, int w, int h) throws Exception {
    screen.init(mc, w, h); SettingsSnapshot.render(mc, screen, w, h, REPORT.resolve(name + ".png"));
  }
  private static void done(Screen screen) throws Exception { invoke(screen, "save", new Class<?>[0]); }
  static void run(Minecraft mc, String action) throws Exception {
    if (action.equals("refinements")) { Refinement230Checks.run(mc); return; }
    if (action.equals("menus")) { HudMenu230Checks.run(mc); return; }
    if (action.equals("saved")) {
      var s = SettingsRegistry.open(mc);
      check(find(s, "logo.presets").value.getAsJsonArray().size() == 1, "title preset did not survive restart");
      check(find(s, "infoHud.clusters.2.offsetX").value.getAsInt() == 37, "HUD placement did not survive restart");
      check(find(s,"logo.swatches").value.getAsJsonArray().size()==2,"swatches lost across restart");
      check(find(s,"logo.selectedPreset").value.equals(find(s,"logo.presets").value.getAsJsonArray().get(0).getAsJsonObject().get("id")),"selected preset ID lost across restart");
      check(find(s,"logo.retroBase").value.getAsJsonObject().size()==TitleTiles.DESIGN_KEYS.size(),"retained retro base lost across restart");
      log("PASS 2.3 editor restart persistence"); return;
    }
    if (action.equals("save")) {
      MenuPreferences.update(v -> v.autoApply = true);
      var options = new PowerOptionsScreen(null); open(mc, options, 640, 420);
      var title = new TitleLetteringScreen(options); open(mc, title, 640, 420);
      var local = draft(title); JsonObject preset = new JsonObject(), values = new JsonObject();
      preset.addProperty("id", UUID.randomUUID().toString()); preset.addProperty("name", "Restart test");
      TitleTiles.DESIGN_KEYS.forEach(id -> values.add(id, local.get(id).value.deepCopy())); preset.add("values", values);
      local.get("logo.presets").value = new JsonArray(); local.get("logo.presets").value.getAsJsonArray().add(preset);
      local.get("logo.swatches").value=JsonParser.parseString("[1,45]");local.get("logo.selectedPreset").value=preset.get("id").deepCopy();
      local.get("logo.retroBase").value=values.deepCopy();done(title);
      check(mc.currentScreen == options, "title save did not return to Options");
      var hud = new HudEditorScreen(options, "infoHud.clusters.2.offsetX"); open(mc, hud, 640, 420);
      draft(hud).get("infoHud.clusters.2.offsetX").value = new JsonPrimitive(37); done(hud);
      check(mc.currentScreen == options, "HUD save did not return to Options");
      check(options.session().changes() == 0, "auto-apply left dirty settings"); log("PASS 2.3 editor durable saves"); return;
    }
    failures = 0; Files.createDirectories(REPORT); PowerConfig.flushPending();
    var document = PowerConfig.document(); boolean auto = MenuPreferences.current().autoApply;
    try {
      MenuPreferences.update(v -> v.autoApply = false);
      var options = new PowerOptionsScreen(null); open(mc, options, 640, 420);
      byte[] original = Files.readAllBytes(PowerConfig.path());
      test("one HUD entry replaces ordinary rows, search still finds nested settings", () -> {
        UiChecks.field(options, "page", "Interface"); ((Set<?>)UiChecks.field(options, "collapsed")).clear(); UiChecks.search(options, "");
        List<?> rows = (List<?>)UiChecks.field(options, "rows"); int hudRows = 0;
        for (Object row : rows) {
          Setting s = (Setting)UiChecks.call(row, "setting");
          if (s != null && HudEditorSettings.contains(s)) { hudRows++; check(s.id.equals(HudEditorSettings.ENTRY), "old HUD row exposed " + s.id); }
        }
        check(hudRows == 1, "missing HUD editor");
        UiChecks.search(options, "cluster 3");
        check(((List<?>)UiChecks.field(options, "rows")).size() > 0, "nested cluster search lost");
        UiChecks.search(options, "");
      });
      test("native HUD modes, sample situations and every editor view render at narrow and wide sizes", () -> {
        var hud = new HudEditorScreen(options, HudEditorSettings.ENTRY); open(mc, hud, 640, 420);
        for (int mode = 0; mode < 3; mode++) {
          draft(hud).get(HudEditorSettings.ENTRY).value = new JsonPrimitive(mode); refresh(hud);
          for (int scenario : new int[]{0,2}) {
            UiChecks.field(hud, "scenario", scenario);
            screenshot(mc, hud, "hud-mode-" + mode + "-" + scenario, 640, 420);
          }
        }
        for (String id : List.of("infoHud.clusters.0.position", "activeTweaks.enabled", "power_controls:userinterface.versionTextConfig.customVersionText", "interface.debugTextColor")) {
          hud = new HudEditorScreen(options, id); open(mc, hud, 640, 420);
          screenshot(mc, hud, "hud-view-" + get(hud, "selected") + "-wide", 640, 420);
          screenshot(mc, hud, "hud-view-" + get(hud, "selected") + "-narrow", 320, 240);
        }
        check(Arrays.equals(original, Files.readAllBytes(PowerConfig.path())), "HUD preview wrote config");
      });
      test("Audio volume categories retain indentation without a divider at both GUI sizes",()->{
        open(mc,options,640,420);UiChecks.field(options,"page","Audio");UiChecks.search(options,"");
        ((Set<?>)UiChecks.field(options,"collapsed")).clear();UiChecks.call(options,"layout");
        List<?> rows=(List<?>)UiChecks.field(options,"rows");boolean found=false;
        for(int i=0;i<rows.size();i++) {
          Setting s=(Setting)UiChecks.call(rows.get(i),"setting");
          if(s!=null&&s.id.equals("native.sound")) {
            check(!(boolean)UiChecks.call(rows.get(i+1),"separator"),"volume divider remains");found=true;
          }
          if(s!=null&&s.id.startsWith("audio.category."))check(Catalog.number(Catalog.metadata(s.id),"indent",0)==10,"volume child not indented");
        }
        check(found,"game-sounds volume row missing");screenshot(mc,options,"audio-volume-wide",640,420);screenshot(mc,options,"audio-volume-narrow",320,240);
      });
      test("HUD selection sits above settings and the divider resizes without losing either area",()->{
        var hud=new HudEditorScreen(options,HudEditorSettings.ENTRY);open(mc,hud,320,240);
        click(hud,number(hud,"left")+10,38,0);
        check((boolean)get(hud,"selecting"),"element menu did not open");screenshot(mc,hud,"hud-element-menu",320,240);
        var menu=javax.imageio.ImageIO.read(REPORT.resolve("hud-element-menu.png").toFile());
        int menuRows=number(hud,"selectorRows");
        for(int py=51*2;py<(51+menuRows*20)*2;py++)for(int px=number(hud,"left")*2;px<number(hud,"right")*2;px++) {
          int pixel=menu.getRGB(px,py),r=(pixel>>16)&255,g=(pixel>>8)&255,b=pixel&255;
          check(Math.abs(r-g)<=1&&Math.abs(g-b)<=1,"HUD dropdown contains coloured pixels");
        }
        key(hud,Keyboard.KEY_ESCAPE);check(!(boolean)get(hud,"selecting"),"Escape did not close element menu");
        int before=number(hud,"controlsBottom"),x=number(hud,"left")+10;
        click(hud,x,before+5,0);Object divider=get(hud,"divider");
        check((boolean)invoke(divider,"dragging",new Class<?>[0]),"divider did not own press");
        ((ScreenInput)(Object)hud).power$release(x,100,0);
        check(number(hud,"controlsBottom")<before,"quick divider drag did not use release position");
        check(!(boolean)invoke(divider,"dragging",new Class<?>[0]),"divider retained released button");
        screenshot(mc,hud,"hud-divider-narrow",320,240);
        check(number(hud,"previewBottom")-number(hud,"previewTop")>=48,"preview disappeared");
      });
      test("title appearance resets use individual defaults, preserve other values, support Undo and stay local until Done",()->{
        for(int[] size:new int[][]{{640,420},{320,240}}) {
          var ownOptions=new PowerOptionsScreen(null);open(mc,ownOptions,size[0],size[1]);
          var title=new TitleLetteringScreen(ownOptions);open(mc,title,size[0],size[1]);UiChecks.field(title,"tab",1);
          var local=draft(title);List<Setting> controls=(List<Setting>)invoke(title,"controls",new Class<?>[0]);
          int top=number(title,"controlsTop"),bottom=number(title,"controlsBottom");
          for(int index=0;index<controls.size();index++) {
            Setting s=controls.get(index);s.reset();s.cycle(1);
            invoke(title,"valueChanged",new Class<?>[]{Setting.class},s);
            check(!s.value.equals(s.defaultValue),"reset fixture did not change "+s.id);
            int scroll=Math.min(index*23,Math.max(0,controls.size()*23-(bottom-top)));
            set(title,"scroll",(double)scroll);
            int y=top+index*23-scroll,x=number(title,"controlX")+number(title,"controlWidth")-10;
            var before=local.snapshot();
            click(title,x-12,y+8,0);check(local.snapshot().equals(before),"gap next to reset changed a value");
            click(title,x,y+8,1);check(local.snapshot().equals(before),"right-click reset changed a value");
            click(title,x,y+8,0);check(s.value.equals(s.defaultValue),"reset missed "+s.id);
            String target=s.id.startsWith("titleEditor.rotation")?"logo.logo.rotation":s.id;
            for(var entry:before.entrySet())if(!Set.of(target,"logo.selectedPreset","logo.retroBase").contains(entry.getKey()))
              check(local.get(entry.getKey()).value.equals(entry.getValue()),"reset changed unrelated "+entry.getKey());
            if(s.id.startsWith("titleEditor.rotation")) {
              int axis=Integer.parseInt(s.id.substring("titleEditor.rotation".length()));
              check(local.get(target).value.getAsJsonArray().get(axis).equals(local.get(target).defaultValue.getAsJsonArray().get(axis)),"rotation preview did not reset");
            }
            var reset=local.snapshot();click(title,x,y+8,0);
            check(local.snapshot().equals(reset),"disabled reset changed settings");
            local.undo();refresh(title);check(local.snapshot().equals(before),"Undo lost reset state "+s.id);
            check(ownOptions.session().changes()==0,"reset leaked into parent before Done");
          }
          set(title,"scroll",0d);screenshot(mc,title,"title-default-buttons-"+size[0],size[0],size[1]);
          int x=number(title,"controlX")+number(title,"controlWidth")-10;
          click(title,number(title,"controlX")+8,top+8,1);
          ((TextInput)get(get(title,"inline"),"input")).setText("invalid");
          click(title,x,top+8,0);
          check(!(boolean)invoke(get(title,"inline"),"active",new Class<?>[0]),"reset kept stale inline input");
          check(local.get("logo.logo.rotation").value.getAsJsonArray().get(0).equals(local.get("logo.logo.rotation").defaultValue.getAsJsonArray().get(0)),"reset after inline input failed");
          done(title);check(mc.currentScreen==ownOptions,"reset draft Done failed");
          ownOptions.session().discard();
          check(Arrays.equals(original,Files.readAllBytes(PowerConfig.path())),"individual reset wrote config before Apply");
        }
      });
      test("drag uses GUI coordinates; Escape opens an input-blocking draft prompt and Discard leaves parent untouched", () -> {
        var hud = new HudEditorScreen(options, HudEditorSettings.ENTRY); open(mc, hud, 640, 420);
        draft(hud).get(HudEditorSettings.ENTRY).value = new JsonPrimitive(1); refresh(hud); UiChecks.field(hud, "snapping", false);
        screenshot(mc, hud, "hud-drag-before", 640, 420);
        Object preview = get(hud, "preview"); List<?> elements = (List<?>)invoke(preview, "elements", new Class<?>[0]);
        Object combined = elements.stream().filter(e -> { try { return invoke(e,"id",new Class<?>[0]).equals("combined"); } catch(Exception ex) { throw new RuntimeException(ex); } }).findFirst().orElseThrow();
        double scale = ((Number)invoke(hud,"scale",new Class<?>[0])).doubleValue();
        double cx = ((Number)invoke(hud,"canvasX",new Class<?>[0])).doubleValue(), cy = ((Number)invoke(hud,"canvasY",new Class<?>[0])).doubleValue();
        double ex = ((Number)invoke(combined,"x",new Class<?>[0])).doubleValue(), ey = ((Number)invoke(combined,"y",new Class<?>[0])).doubleValue();
        int x = (int)Math.round(cx + (ex + 170) * scale), y = (int)Math.round(cy + (ey + 10) * scale);
        var unselected=draft(hud).snapshot();
        click(hud,x,y,0);((ScreenInput)(Object)hud).power$release(x+12,y-10,0);
        check(draft(hud).snapshot().equals(unselected)&&get(hud,"dragging")==null,"selection press moved the HUD");
        click(hud, x, y, 0); invoke(hud,"move",new Class<?>[]{int.class,int.class},x+12,y-10);
        check(draft(hud).get("power_hud:hudpositions.SIMPLE_HUD_POSITION_CONFIG.horizontalPositionOffset").value.getAsInt() > 0,"drag did not move HUD");
        key(hud,Keyboard.KEY_ESCAPE); Object prompt=get(hud,"exit");
        check((boolean)invoke(prompt,"active",new Class<?>[0]),"Escape lost unsaved edits");
        var before=draft(hud).snapshot(); click(hud,20,80,0);key(hud,Keyboard.KEY_LEFT);check(draft(hud).snapshot().equals(before),"modal leaked input");
        screenshot(mc,hud,"hud-exit-warning",640,420);
        click(hud,hud.width/2,hud.height/2+15,0);
        check(mc.currentScreen==options,"Discard did not return");
        check(Arrays.equals(original,Files.readAllBytes(PowerConfig.path())),"discard wrote config");
      });
      test("title paint and erase, filtered instant brush selection, swatches and live sliders stay local",()->{
        var title=new TitleLetteringScreen(options);open(mc,title,640,420);var local=draft(title);
        int tile=number(title,"tileSize"),x=number(title,"gridLeft")+tile/2,y=number(title,"gridTop")+tile/2;
        JsonArray lines=local.get("logo.logo.lines").value.getAsJsonArray();JsonObject palette=local.get("logo.blocks.map").value.getAsJsonObject();
        click(title,x,y,0);invoke(title,"stopGesture",new Class<?>[0]);
        check(TitleTiles.block(lines,palette,0,0)==1,"left click did not paint");
        click(title,x,y,0);invoke(title,"stopGesture",new Class<?>[0]);check(TitleTiles.block(lines,palette,0,0)==1,"painting toggled an occupied tile");
        click(title,x,y,1);invoke(title,"stopGesture",new Class<?>[0]);check(TitleTiles.block(lines,palette,0,0)==0,"right click did not erase");
        click(title,x,y,0);((ScreenInput)(Object)title).power$release(x+2*tile,y,0);
        check(TitleTiles.block(lines,palette,2,0)==1,"quick paint drag lost final motion");
        click(title,x,y,1);((ScreenInput)(Object)title).power$release(x+2*tile,y,1);
        check(TitleTiles.block(lines,palette,0,0)==0&&TitleTiles.block(lines,palette,2,0)==0,"quick erase drag lost final motion");
        click(title,number(title,"controlX")+10,number(title,"controlsTop")+7,0);
        check(mc.currentScreen instanceof ItemPickerScreen,"brush button did not open picker");
        ItemPickerScreen picker=(ItemPickerScreen)mc.currentScreen;
        for(Object entry:(List<?>)get(picker,"shown"))check(!List.of(8,9,10,11).contains(((ItemCatalogue.Entry)entry).id()),"liquid admitted to brush picker");
        invoke(picker,"choose",new Class<?>[]{String.class},"45");check(mc.currentScreen==title,"one-click brush did not commit");
        check(((Setting)get(title,"brush")).value.getAsInt()==45,"brush selection missing");
        click(title,number(title,"controlX")+10,number(title,"controlsTop")+7,0);picker=(ItemPickerScreen)mc.currentScreen;
        invoke(picker,"custom",new Class<?>[0]);Screen custom=mc.currentScreen;((TextInput)get(custom,"input")).setText("8");done(custom);
        check(mc.currentScreen==custom&&((Setting)get(title,"brush")).value.getAsInt()==45,"custom liquid selection was accepted");
        ((TextInput)get(custom,"input")).setText("17");done(custom);check(mc.currentScreen==title&&((Setting)get(title,"brush")).value.getAsInt()==17,"custom instant selection did not return to editor");
        ((Setting)get(title,"brush")).value=new JsonPrimitive("45");
        local.get("logo.swatches").value=new JsonArray();int sy=number(title,"swatchTop");
        click(title,number(title,"left")+8,sy+8,0);check(local.get("logo.swatches").value.getAsJsonArray().size()==1,"brush swatch not saved");
        ((Setting)get(title,"brush")).value=new JsonPrimitive("1");click(title,number(title,"left")+8,number(title,"swatchTop")+8,0);
        check(((Setting)get(title,"brush")).value.getAsInt()==45,"swatch did not switch brush");
        UiChecks.field(title,"tab",1);int cy=number(title,"controlsTop");
        click(title,number(title,"controlX")+number(title,"controlWidth")/4,cy+7,0);
        ((ScreenInput)(Object)title).power$release(number(title,"controlX")+number(title,"controlWidth")*3/4,cy+7,0);
        check(local.get("logo.logo.rotation").value.getAsJsonArray().get(0).getAsDouble()>100,"rotation slider did not update preview");
        click(title,number(title,"controlX")+10,cy+7,1);check(mc.currentScreen==title,"precise rotation opened another screen");
        Object inputEditor=get(title,"inline");((TextInput)get(inputEditor,"input")).setText("22.5");key(title,Keyboard.KEY_RETURN);
        check(local.get("logo.logo.rotation").value.getAsJsonArray().get(0).getAsDouble()==22.5,"precise rotation lost fraction");
        var before=local.snapshot();invoke(title,"extraFooterAction",new Class<?>[0]);check(local.snapshot().equals(before),"replay changed settings");
        for(int tab=0;tab<3;tab++) {
          UiChecks.field(title,"tab",tab);screenshot(mc,title,"title-tab-"+tab+"-wide",640,420);
          for(int tick=0;tick<200;tick++)title.tick();
          screenshot(mc,title,"title-tab-"+tab+"-wide",640,420);screenshot(mc,title,"title-tab-"+tab+"-narrow",320,240);
        }
        local.get("logo.logo.lines").value=TitleTiles.resize(lines,80,12);local.get("logo.swatches").value=new JsonArray();
        var valid=(Map<Integer,?>)get(title,"blocks");for(int id:valid.keySet().stream().sorted().limit(16).toList())local.get("logo.swatches").value.getAsJsonArray().add(id);
        for(int[] size:new int[][]{{320,240},{640,420}}){open(mc,title,size[0],size[1]);
          int actualRows=(17+number(title,"swatchColumns")-1)/number(title,"swatchColumns");
          check(number(title,"swatchTop")+actualRows*22<=number(title,"previewBottom"),"maximum grid/swatches overlap footer");
        }
        check(Arrays.equals(original,Files.readAllBytes(PowerConfig.path())),"title preview wrote config");
      });
      test("title preset UI saves a copy, loads it, removes on right-click and supports Undo without disk writes",()->{
        var title=new TitleLetteringScreen(options);open(mc,title,640,420);var local=draft(title);
        local.get("logo.presets").value=new JsonArray();UiChecks.field(title,"tab",2);
        int left=number(title,"left"),span=number(title,"span"),top=number(title,"controlsTop");
        click(title,left+span*3/4,top+8,0);check(mc.currentScreen==title,"preset name left editor");
        ((TextInput)get(get(title,"inline"),"input")).setText("UI preset");key(title,Keyboard.KEY_RETURN);
        check(mc.currentScreen==title&&local.get("logo.presets").value.getAsJsonArray().size()==1,"preset not saved into draft");
        var saved=local.get("logo.logo.lines").value.deepCopy();
        TitleTiles.paint(local.get("logo.logo.lines").value.getAsJsonArray(),local.get("logo.blocks.map").value.getAsJsonObject(),0,0,45);
        click(title,left+10,top+32,0);check(local.get("logo.logo.lines").value.equals(saved),"preset did not restore lettering");
        click(title,left+10,top+32,1);check(local.get("logo.presets").value.getAsJsonArray().size()==0,"right-click did not remove preset");
        local.undo();check(local.get("logo.presets").value.getAsJsonArray().size()==1,"Undo lost removed preset");
        check(Arrays.equals(original,Files.readAllBytes(PowerConfig.path())),"preset draft wrote config");
      });
      test("Done stages both editors together and parent Discard restores without writes",()->{
        var title=new TitleLetteringScreen(options);open(mc,title,640,420);
        draft(title).get("logo.logo.shadow").value=new JsonPrimitive(!draft(title).get("logo.logo.shadow").value.getAsBoolean());done(title);
        check(mc.currentScreen==options,"title Done failed");check(find(options.session(),"logo.logo.shadow").changed(),"title draft not accepted");
        var hud=new HudEditorScreen(options,"infoHud.clusters.2.offsetX");open(mc,hud,640,420);
        draft(hud).get("infoHud.clusters.2.offsetX").value=new JsonPrimitive(draft(hud).get("infoHud.clusters.2.offsetX").value.getAsInt()+37);done(hud);
        check(mc.currentScreen==options&&options.session().changes()>=2,"HUD Done lost another draft");
        check(Arrays.equals(original,Files.readAllBytes(PowerConfig.path())),"manual Done saved config");
        options.session().discard();check(options.session().changes()==0,"parent Discard left edits");
      });
      test("main-menu panoramas cover Options and native lists; in-world menus remain world-owned",()->{
        var session=SettingsRegistry.open(mc);String prefix="power_controls:userinterface.panoramaConfig.";
        find(session,prefix+"panoramaFolder").value=new JsonPrimitive("b1_8_1");find(session,prefix+"onlyTitleScreen").value=new JsonPrimitive(false);
        session.preview(true);Object world=mc.world;mc.world=null;
        try {
          open(mc,options,640,420);check(MenuPanorama.enabled(mc),"menu panorama did not enable");screenshot(mc,options,"panorama-options",640,420);
          Screen worlds=new net.minecraft.client.gui.screen.world.SelectWorldScreen(new TitleScreen());open(mc,worlds,640,420);screenshot(mc,worlds,"panorama-worlds",640,420);
          Screen packs=new net.minecraft.client.gui.screen.pack.PackScreen(new TitleScreen());open(mc,packs,640,420);screenshot(mc,packs,"panorama-packs",640,420);
          find(session,prefix+"onlyTitleScreen").value=new JsonPrimitive(true);session.preview(true);check(!MenuPanorama.enabled(mc),"title-only preference ignored");
        } finally {mc.world=(net.minecraft.world.World)world;session.discard();}
        if(mc.world!=null)check(!MenuPanorama.enabled(mc),"panorama replaced world background");
      });
    } finally {
      mc.setScreen(null); PowerConfig.write(document); MenuPreferences.update(v->v.autoApply=auto);
    }
    log("EDITOR 2.3 FAILURES "+failures);
  }
}
