package local.luke.power.validation;

import static local.luke.power.validation.Release240Checks.*;
import static local.luke.power.validation.Validation.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import local.luke.power.building.hotbar.*;

/** Native-event, persistence and preview fixtures for an explicitly muted disposable client. */
public final class Release241Checks {
  public static void captureMobTag(String text,int x,int y) { Forest241Capture.tag(text,x,y); }
  public static boolean captureTags;
  public static int tags;
  private static HudEditorScreen nativeEditor;
  private static Object nativeReorder;
  private static DebugColumns expected;
  private static boolean hotbarArmed, hotbarHeld, hotbarReady, hotbarPassed;
  private static net.minecraft.item.ItemStack[] hotbarBefore;
  public static void observe(Minecraft mc) throws Exception {
    if (!hotbarArmed || mc.player == null) return;
    boolean down=local.luke.power.input.Bindings.down(Hotbars.SCROLL);
    if(down) {
      hotbarHeld=true;check(InventoryRows.identical(hotbarBefore,mc.player.inventory.main),"native preview changed inventory before release");
      var pending=staticField(Class.forName("local.luke.power.building.hotbar.HotbarTransaction"),"pending");
      if(pending!=null&&((java.util.concurrent.Future<?>)get(pending,"write")).isDone()&&!hotbarReady){hotbarReady=true;log("NATIVE HOTBAR PREPARED");}
    } else if(hotbarHeld) {
      hotbarArmed=false;check(hotbarReady,"native release preceded prepared snapshot");
      check(InventoryRows.identical(InventoryRows.swapped(hotbarBefore,1),mc.player.inventory.main),"native prepared swap did not commit on first release tick");
      hotbarPassed=true;log("PASS native prepared hotbar swap commits on first release tick");
    }
  }
  private static final Path REPORT=Path.of("power-beta-data/reports/release-241");
  private static Object staticField(Class<?> type,String name)throws Exception {var f=type.getDeclaredField(name);f.setAccessible(true);return f.get(null);}
  private static void staticField(Class<?> type,String name,Object value)throws Exception {var f=type.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
  private static void select(Object hud,String id)throws Exception {call(hud,"select",id);}
  private static void toggle(HudEditorScreen hud,String id)throws Exception {
    Object toolbar=call(hud,"toolbar");
    for(Object button:(List<?>)call(toolbar,"toggles"))if(call(button,"id").equals(id)) {click(hud,n(button,"x")+5,n(button,"y")+5,0);return;}
    throw new AssertionError("Missing preview toggle "+id);
  }
  private static void footer(Screen screen,String label)throws Exception {
    for(Object button:(List<?>)call(screen,"footer"))if(call(button,"label").equals(label)) {click(screen,n(button,"x")+5,screen.height-24,0);return;}
    throw new AssertionError("Missing footer "+label);
  }
  private static void shot(Minecraft mc,Screen screen,String name)throws Exception {SettingsSnapshot.render(mc,screen,screen.width,screen.height,REPORT.resolve(name+".png"));}
  static void run(Minecraft mc,String action)throws Exception {
    Files.createDirectories(REPORT);
    if(action.equals("hotbar-setup")) {
      mc.setScreen(null);mc.method_2133();AccessRules.of(mc.world.method_262()).allAllowed(true);
      var session=SettingsRegistry.open(mc);
      setting(session,"keys."+Hotbars.SCROLL.translationKey).value=new JsonPrimitive(Keyboard.KEY_F8);
      session.settings().stream().filter(s->s.id.endsWith("hotbar.selectedRow")).forEach(s->s.value=new JsonPrimitive(1));session.save(Path.of("."));
      for(int i=0;i<36;i++)mc.player.inventory.main[i]=new net.minecraft.item.ItemStack(35,i+1,i%16);
      hotbarBefore=mc.player.inventory.main.clone();hotbarArmed=true;hotbarHeld=hotbarReady=hotbarPassed=false;log("NATIVE HOTBAR hold F8 then release after PREPARED");return;
    }
    if(action.equals("hotbar-result")){check(hotbarPassed,"native hotbar release not observed");log("PASS native hotbar result");return;}
    if(action.equals("live")) {
      check(mc.world!=null,"Load world first");boolean debug=mc.options.debugHud;
      try {mc.options.debugHud=true;mc.inGameHud.render(0F,true,0,0);var lines=DebugOverlay.lastLayout();
        var version=lines.stream().filter(line->line.id().equals("version")).findFirst().orElseThrow();
        var memory=lines.stream().filter(line->line.id().equals("usedMemory")).findFirst().orElseThrow();
        check(version.x()>memory.x()&&memory.x()==2,"live renderer ignored saved columns");log("PASS native live F3 renders the saved cross-column order");
      }finally{mc.options.debugHud=debug;}return;
    }
    if(action.equals("ui")){ui(mc);return;}
    if(action.equals("gameplay")){gameplay(mc);return;}
    if(action.equals("forest")){Forest241Capture.scene(mc);return;}
    if(action.startsWith("cow-pose ")){Forest241Capture.pose(action.substring(9));return;}
    if(action.equals("tag-project")){mc.options.hideHud=false;Forest241Capture.project(true);return;}
    if(action.equals("tag-hide")){mc.options.debugHud=false;mc.options.hideHud=true;Forest241Capture.project(false);return;}
    if(action.equals("saved")) {
      check(MenuPreferences.current().debugRightOrder.contains("version")&&!MenuPreferences.current().debugOrder.contains("version"),"cross-column order lost on restart");
      check(MenuPreferences.current().debugOrder.contains("usedMemory")&&!MenuPreferences.current().debugRightOrder.contains("usedMemory"),"memory column lost on restart");
      log("PASS 2.4.1 two-column order survives restart");return;
    }
    if(action.equals("drag-setup")) {
      MenuPreferences.update(v->v.autoApply=false);var options=new PowerOptionsScreen(null);mc.setScreen(options);
      nativeEditor=new HudEditorScreen(options,"interface.debugOrder");mc.setScreen(nativeEditor);
      var draft=draft(nativeEditor);draft.get("interface.debugOrder").value=Catalog.JSON.toJsonTree(DebugOrder.defaults());
      draft.get("interface.debugRightOrder").value=Catalog.JSON.toJsonTree(DebugColumns.defaultRight());
      nativeEditor.beginReorder();nativeEditor.render(-1,-1,0);nativeReorder=get(nativeEditor,"reorder");
      expected=(DebugColumns)get(nativeReorder,"rows");coordinates(mc);return;
    }
    if(action.equals("drag-check")) {
      DebugColumns actual=(DebugColumns)get(nativeReorder,"rows");
      check(!actual.equals(expected),"native release failed to reorder");check(get(nativeReorder,"dragging")==null,"native release did not end drag");
      expected=actual;log("PASS native drag/drop changed order: left="+actual.left()+" right="+actual.right());coordinates(mc);return;
    }
    if(action.equals("drag-save")) {
      call(nativeEditor,"finishReorder");var draft=draft(nativeEditor);
      check(DebugOrder.read(draft.get("interface.debugOrder").value).equals(expected.left()),"Save omitted left order");
      check(DebugOrder.read(draft.get("interface.debugRightOrder").value).equals(expected.right()),"Save omitted right order");
      footer(nativeEditor,"Done");var options=(PowerOptionsScreen)mc.currentScreen;call(options,"save",false,false);
      check(MenuPreferences.current().debugOrder.equals(expected.left())&&MenuPreferences.current().debugRightOrder.equals(expected.right()),"Apply lost column layout");
      log("PASS native reorder saved into live and durable settings");return;
    }
    throw new IllegalArgumentException(action);
  }
  private static void coordinates(Minecraft mc)throws Exception {
    int left=(int)get(nativeReorder,"left"),top=(int)get(nativeReorder,"top"),span=(int)get(nativeReorder,"span");
    double sx=mc.displayWidth/(double)nativeEditor.width,sy=mc.displayHeight/(double)nativeEditor.height;
    JsonObject out=new JsonObject();out.addProperty("left",left);out.addProperty("top",top);out.addProperty("span",span);out.addProperty("sx",sx);out.addProperty("sy",sy);
    Files.writeString(REPORT.resolve("native-drag.json"),out.toString());log("NATIVE DRAG "+out);
  }
  private static void ui(Minecraft mc)throws Exception {
    failures=0;MenuPreferences.update(v->v.autoApply=false);var options=new PowerOptionsScreen(null);open(mc,options,640,420);
    test("native selected-world fill stays opaque black over the panorama",()->{
      check(mc.world==null,"Run title before UI checks");
      var panorama=get(staticField(Class.forName("local.luke.power.controls.ControlFeatures"),"USER_INTERFACE_CONFIG"),"panoramaConfig");
      boolean enabled=(boolean)get(panorama,"enablePanorma"),only=(boolean)get(panorama,"onlyTitleScreen");
      var folder=get(panorama,"panoramaFolder");
      try {
        set(panorama,"enablePanorma",true);set(panorama,"onlyTitleScreen",false);set(panorama,"panoramaFolder","1_0");
        var select=new net.minecraft.client.gui.screen.world.SelectWorldScreen(new net.minecraft.client.gui.screen.TitleScreen());open(mc,select,640,420);
        set(select,"field_2435",0);check(MenuPanorama.enabled(mc),"panorama fixture unavailable");shot(mc,select,"selected-world-black");
        var image=javax.imageio.ImageIO.read(REPORT.resolve("selected-world-black.png").toFile());
        var list=get(select,"field_2437");
        int rowY=((Number)get(list,"field_1535")).intValue()+4-((Number)get(list,"field_1540")).intValue()+((Number)get(list,"field_1545")).intValue();
        check((image.getRGB(428*2,(rowY+27)*2)&0xffffff)==0,"selected-world fill is translucent");
      } finally {set(panorama,"enablePanorma",enabled);set(panorama,"onlyTitleScreen",only);set(panorama,"panoramaFolder",folder);mc.setScreen(options);}
    });
    test("Interface top rows are ungrouped with a divider and HUD entry search belongs to Interface",()->{
      set(options,"page","Interface");UiChecks.search(options,"");call(options,"layout");var rows=(List<?>)get(options,"rows");
      Object first=rows.get(0);check(call(first,"setting")!=null&&((Setting)call(first,"setting")).id.equals(HudEditorSettings.ENTRY),"Interface starts with a group heading");
      check(rows.stream().anyMatch(row->{try{return (boolean)call(row,"separator");}catch(Exception e){throw new RuntimeException(e);}}),"HUD divider missing");
      UiChecks.search(options,"HUD editor");rows=(List<?>)get(options,"rows");
      for(Object row:rows){Setting setting=(Setting)call(row,"setting");if(setting!=null&&setting.id.equals(HudEditorSettings.ENTRY))check(call(row,"group").equals("Interface"),"HUD entry search group wrong");}
      UiChecks.search(options,"");shot(mc,options,"interface");
    });
    test("preview toggles default On, wrap across common sizes and never change settings",()->{
      var hud=new HudEditorScreen(options,HudEditorSettings.ENTRY);open(mc,hud,640,420);var before=draft(hud).snapshot();byte[] disk=Files.readAllBytes(PowerConfig.path());
      check((boolean)get(hud,"background"),"Bg did not default On");Object preview=get(hud,"preview");
      for(String key:List.of("showToast","showChat","showTweaks"))check((boolean)get(preview,key),"preview toggle not On");
      for(int[] size:new int[][]{{320,240},{640,420},{960,540},{1720,720},{540,960}}) {hud.init(mc,size[0],size[1]);shot(mc,hud,"hud-"+size[0]+"x"+size[1]);check(n(hud,"previewBottom")-n(hud,"previewTop")>=48,"preview too short");}
      hud.init(mc,640,420);
      for(String id:List.of("background","toast","chat","tweaks","snap","f3")){toggle(hud,id);toggle(hud,id);}
      check(before.equals(draft(hud).snapshot())&&Arrays.equals(disk,Files.readAllBytes(PowerConfig.path())),"preview toggles changed settings");
    });
    test("editor remembers section and scroll, while order resets link both columns and remain undoable",()->{
      var hud=new HudEditorScreen(options,"interface.debugOrder");open(mc,hud,640,420);set(hud,"scroll",85d);mc.setScreen(options);
      var reopened=new HudEditorScreen(options,HudEditorSettings.ENTRY);open(mc,reopened,640,420);
      check(get(reopened,"selected").equals("debug")&&((Number)get(reopened,"scroll")).doubleValue()==85,"editor location lost");
      var draft=draft(reopened);var left=new ArrayList<>(DebugOrder.defaults());left.remove("version");var right=new ArrayList<>(DebugColumns.defaultRight());right.add(0,"version");
      draft.get("interface.debugOrder").value=Catalog.JSON.toJsonTree(left);draft.get("interface.debugRightOrder").value=Catalog.JSON.toJsonTree(right);var before=draft.snapshot();
      var setting=draft.get("interface.debugOrder");int y=expose(reopened,setting);click(reopened,n(reopened,"controlX")+n(reopened,"controlWidth")-10,y+7,0);
      check(DebugOrder.read(setting.value).equals(DebugOrder.defaults())&&DebugOrder.read(draft.get("interface.debugRightOrder").value).equals(DebugColumns.defaultRight()),"individual order reset omitted a column");
      call(reopened,"undo");check(before.equals(draft.snapshot()),"Undo omitted a column");
      check(((List<Setting>)call(reopened,"positionSettings")).stream().allMatch(s->s.id.startsWith("interface.debug")),"current F3 position reset touched another HUD");
      setting.reset();var rightOnly=new ArrayList<>(DebugColumns.defaultRight());Collections.reverse(rightOnly);draft.get("interface.debugRightOrder").value=Catalog.JSON.toJsonTree(rightOnly);
      check(!(boolean)call(reopened,"atDefault",setting),"right-only change disabled Info order reset");
      y=expose(reopened,setting);click(reopened,n(reopened,"controlX")+n(reopened,"controlWidth")-10,y+7,0);
      check(DebugOrder.read(draft.get("interface.debugRightOrder").value).equals(DebugColumns.defaultRight()),"right-only reset failed");
      draft.restore(before);call(reopened,"performReset",true,false);
      check(DebugOrder.read(setting.value).equals(DebugOrder.defaults())&&DebugOrder.read(draft.get("interface.debugRightOrder").value).equals(DebugColumns.defaultRight()),"position reset omitted a column");
      call(reopened,"undo");check(before.equals(draft.snapshot()),"position reset was not fully undoable");
    });
    test("Clear logo preserves grid/palette, remains local and Undo restores every tile",()->{
      var title=new TitleLetteringScreen(options);open(mc,title,640,420);var draft=draft(title);var before=draft.snapshot();
      footer(title,"Clear logo");var lines=draft.get("logo.logo.lines").value.getAsJsonArray();
      check(java.util.stream.StreamSupport.stream(lines.spliterator(),false).allMatch(line->line.getAsString().isBlank()),"Clear retained painted tiles");
      check(draft.get("logo.blocks.map").value.equals(before.get("logo.blocks.map")),"Clear changed palette");
      call(title,"undo");check(draft.snapshot().equals(before),"Clear was not fully undoable");shot(mc,title,"title-clear-button");
    });
    test("F3 entity tags use the positive draft setting and render only when enabled",()->{
      var hud=new HudEditorScreen(options,"interface.debugOrder");open(mc,hud,640,420);var setting=draft(hud).get("power_controls:userinterface.disableDebugEntityIdTags");
      check(setting.label.equals("Show F3 entity id tags")&&setting.defaultValue.getAsBoolean(),"entity tag label/default wrong");
      captureTags=true;
      try {setting.value=new JsonPrimitive(true);tags=0;shot(mc,hud,"entity-tags-on");check(tags>0,"entity ID tag not drawn");
        setting.value=new JsonPrimitive(false);tags=0;shot(mc,hud,"entity-tags-off");check(tags==0,"disabled entity ID tag drawn");}
      finally {captureTags=false;}
    });
    mc.setScreen(options);log("RELEASE 2.4.1 UI FAILURES "+failures);
  }
  private static void gameplay(Minecraft mc)throws Exception {
    check(mc.world!=null,"Load disposable world first");failures=0;var audio=AudioConfig.copy();var world=mc.world;
    try {
      test("explicit music pause survives every dimension policy and custom context resets",()->{
        for(var menu:AudioSettings.MenuMusic.values())for(var policy:AudioSettings.DimensionMusic.values()) {
          var config=AudioConfig.copy();config.master=100;config.menuMusic=menu;config.dimensionMusic=policy;AudioConfig.preview(config);
          AudioController.tick(mc);AudioController.playNow("music:calm1.ogg");AudioController.pause();
          AudioController.changingDimension();check((boolean)staticField(AudioController.class,"paused"),"dimension policy revoked pause");
          mc.world=null;AudioController.tick(mc);mc.world=world;AudioController.tick(mc);
          check((boolean)staticField(AudioController.class,"paused")&&!AudioController.musicPlaying(),"context reset revoked pause");
        }
        mc.method_2139();AudioController.tick(mc);check((boolean)staticField(AudioController.class,"paused"),"actual dimension travel revoked pause");mc.method_2139();AudioController.tick(mc);
      });
      test("paused music preview survives dimension travel without restoring old jukeboxes",()->{
        var config=AudioConfig.copy();config.menuMusic=AudioSettings.MenuMusic.MIX;config.dimensionMusic=AudioSettings.DimensionMusic.STOP_ALL;AudioConfig.preview(config);
        AudioController.tick(mc);var preview=AudioController.class.getDeclaredMethod("previewMusic",String.class);preview.setAccessible(true);preview.invoke(null,"music:calm1.ogg");AudioController.pause();
        mc.method_2139();AudioController.tick(mc);
        check((boolean)staticField(Class.forName("local.luke.power.audio.MusicPreview"),"paused")&&!AudioController.musicPlaying(),"dimension travel revoked preview pause");
        mc.method_2139();AudioController.tick(mc);
      });
      test("photo mode opens closer and resize preserves its initial zoom",()->{
        var type=Class.forName("local.luke.power.controls.tweaks.photomode.PhotoModeScreen");var screen=(Screen)type.getConstructor(Screen.class).newInstance(mc.currentScreen);open(mc,screen,640,420);
        check(((Number)get(screen,"zoom")).doubleValue()==1.25&&((Number)get(screen,"zoomGoal")).doubleValue()==1.25,"photo mode initial zoom wrong");
        screen.init(mc,960,540);check(((Number)get(screen,"zoomGoal")).doubleValue()==1.25,"photo resize reset zoom");mc.setScreen(null);
      });
    } finally {if(mc.world==null)mc.world=world;AudioController.quiet();AudioConfig.preview(audio);mc.setScreen(null);}
    log("RELEASE 2.4.1 GAMEPLAY FAILURES "+failures);
  }
}
