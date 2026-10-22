package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.input.*;
import local.luke.power.inventory.tools.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.*;
import org.lwjgl.opengl.*;

final class Sorting403Checks {
  static final Path REPORT=Path.of("power-beta-data/reports/sorting-403");
  private Sorting403Checks() {}
  static void run(Minecraft mc)throws Exception {
    check(mc.world!=null&&mc.player!=null&&Display.isActive(),"focus a disposable loaded world");
    check(mc.options.musicVolume==0&&mc.options.soundVolume==0,"mute the disposable client");
    failures=0;Files.createDirectories(REPORT);
    try(var s=new SortingLayer402Checks.State(mc)) {
      test("SORTING403 hidden native body and same-container Escape at both widths",()->{
        for(int[] size:new int[][]{{320,240},{640,360}}) {
          s.open(size);String items=s.inventory();var editor=s.editor();
          check((boolean)UiChecks.field(editor,"immediate"),"hotkey editor is still a manual draft");
          int grid=(int)UiChecks.call(editor,"gridTop"),row=controls(editor),presets=(int)UiChecks.call(editor,"presetsTop");
          check(row-grid-66==10&&presets-row-18==20,"button gaps are not 10 and 20 pixels");
          capture(mc,s.screen,"layout-"+size[0],true);
          s.key('\0',Keyboard.KEY_ESCAPE);s.closed();s.unchanged(items);
          capture(mc,s.screen,"restored-"+size[0],false);
        }
      });
      test("SORTING403 protection publishes before Escape without a dirty prompt",()->{
        s.open(new int[]{640,360});String items=s.inventory();var editor=s.editor();int x=left(editor)+10,y=(int)UiChecks.call(editor,"gridTop")+10;
        JsonObject before=PowerConfig.document().deepCopy();s.press(x,y,1);s.release(x,y,1);
        check(InventoryToolsConfig.current().protectedSlots.equals(List.of(9)),"right-click did not apply immediately");
        owned(before,Set.of(9),Map.of());s.key('\0',Keyboard.KEY_ESCAPE);s.closed();s.unchanged(items);
        s.key('\0',Keyboard.KEY_F10);check(locked(s.editor()).equals(Set.of(9)),"reopening lost the applied layout");
        s.key('\0',Keyboard.KEY_ESCAPE);s.closed();
      });
      test("SORTING403 Item picker accepts once and immediately publishes",()->{
        s.open(new int[]{640,360});String items=s.inventory();var editor=s.editor();
        s.press(left(editor)+10,controls(editor)+9,0);check(s.page() instanceof ItemPickerScreen,"Item did not open the picker");
        var picker=(ItemPickerScreen)s.page();@SuppressWarnings("unchecked") var shown=(List<ItemCatalogue.Entry>)UiChecks.field(picker,"shown");
        String chosen=shown.get(0).value();s.press((int)UiChecks.call(picker,"left")+12,78,0);
        check(s.page()==editor&&chosen.equals(InventoryToolsConfig.current().preferredItems.get(9)),"selection waited for Done");
        s.key('\0',Keyboard.KEY_ESCAPE);s.closed();s.unchanged(items);
      });
      test("SORTING403 same-slot double-click dispatch opens the picker",()->{
        s.open(new int[]{640,360});String items=s.inventory();var editor=s.editor();
        var clicks=(SlotDoubleClick)UiChecks.field(editor,"clicks");clicks.press(9,0,Mouse.getEventNanoseconds()-100_000_000L);
        s.press(left(editor)+10,(int)UiChecks.call(editor,"gridTop")+10,0);
        check(s.page() instanceof ItemPickerScreen&&s.hostOpen(),"double-click did not open an embedded preferred-item picker");
        s.key('\0',Keyboard.KEY_ESCAPE);check(s.page()==editor,"picker Escape did not return to the same editor");
        s.key('\0',Keyboard.KEY_ESCAPE);s.closed();s.unchanged(items);
      });
      test("SORTING403 clearing and Reset publish without changing native items",()->{
        s.open(new int[]{640,360});String items=s.inventory();var editor=s.editor();locked(editor).add(9);preferred(editor).put(9,"35:4");UiChecks.call(editor,"applyNow");
        s.press(left(editor)+10,(int)UiChecks.call(editor,"gridTop")+10,1);s.release(left(editor)+10,(int)UiChecks.call(editor,"gridTop")+10,1);
        check(InventoryToolsConfig.current().protectedSlots.equals(List.of(9))&&InventoryToolsConfig.current().preferredItems.isEmpty(),"clearing changed protection");
        preferred(editor).put(9,"35:4");UiChecks.call(editor,"applyNow");s.press(left(editor)+75,controls(editor)+9,0);
        check(InventoryToolsConfig.current().preferredItems.isEmpty(),"Clear did not apply");
        s.press(left(editor)+145,controls(editor)+9,0);
        check(InventoryToolsConfig.current().protectedSlots.isEmpty()&&InventoryToolsConfig.current().preferredItems.isEmpty(),"Reset waited for Done");
        s.key('\0',Keyboard.KEY_ESCAPE);s.closed();s.unchanged(items);
      });
      test("SORTING403 presets apply on Load and persist named edits immediately",()->{
        s.open(new int[]{640,360});String items=s.inventory();var editor=s.editor();var data=SortingLayoutsData.read(new JsonObject());
        for(int i=0;i<3;i++){var locked=new JsonArray();locked.add(9+i);var preferred=new JsonObject();preferred.addProperty("12","35:4");data=data.save("Immediate "+i,locked,preferred);}
        UiChecks.field(editor,"presets",data);UiChecks.call(editor,"applyNow");int top=(int)UiChecks.call(editor,"presetsTop");
        s.press(left(editor)+10,top+9,0);s.key('\0',Keyboard.KEY_END);s.key('\0',Keyboard.KEY_RETURN);
        check(InventoryToolsConfig.current().protectedSlots.isEmpty(),"choosing a preset loaded it implicitly");
        s.press(left(editor)+120,top+9,0);check(InventoryToolsConfig.current().protectedSlots.equals(List.of(11)),"Load did not apply");
        s.press(left(editor)+165,top+9,0);check(SortingLayoutsData.read(SortingLayoutPresets.snapshot().getAsJsonObject()).size()==2,"Delete did not persist");
        ((TextInput)UiChecks.field(editor,"presetName")).setText("Saved immediately");s.press(left(editor)+160,top+29,0);
        check(SortingLayoutsData.read(PowerConfig.section("sortingLayouts")).size()==3,"Save did not persist");
        s.press(left(editor)+10,top+9,0);capture(mc,s.screen,"dropdown-640",true);s.key('\0',Keyboard.KEY_ESCAPE);s.key('\0',Keyboard.KEY_ESCAPE);s.closed();s.unchanged(items);
      });
      test("SORTING403 menu editor keeps its ordinary draft and Discard",()->{
        s.open(new int[]{640,360});s.key('\0',Keyboard.KEY_ESCAPE);s.closed();String items=s.inventory();JsonObject before=PowerConfig.document().deepCopy();
        var session=new ConfigSession();InventoryToolsBackend.register(session);var editor=new SortingPreferencesScreen(s.screen,session,()->{});
        mc.currentScreen=editor;editor.init(mc,640,360);int x=left(editor)+10,y=(int)UiChecks.call(editor,"gridTop")+10;
        editor.layerPress(x,y,1);editor.layerRelease(x,y,1);check(before.equals(PowerConfig.document()),"ordinary menu edit published without Done");
        capture(mc,editor,"menu-draft-640",false);editor.layerKey('\0',Keyboard.KEY_ESCAPE);
        check((boolean)UiChecks.call(UiChecks.field(editor,"exit"),"active"),"ordinary draft lost its exit prompt");
        int width=Math.min(76,Math.max(10,(editor.width-32)/3));editor.layerPress((editor.width-(width*3+8))/2+width+4+width/2,editor.height/2+18,0);
        check(mc.currentScreen==s.screen&&before.equals(PowerConfig.document()),"Discard changed saved settings");s.unchanged(items);
      });
    }
    log("SORTING403 failures="+failures+" captures="+REPORT);check(failures==0,"Sorting403 failures="+failures);
  }
  private static void owned(JsonObject before,Set<Integer> locked,Map<Integer,String> preferred) {
    var expected=before.deepCopy().getAsJsonObject("settings").getAsJsonObject("inventoryTools");
    if(!expected.has("toolWhitelist"))expected.add("toolWhitelist",Catalog.JSON.toJsonTree(new InventoryToolsSettings().toolWhitelist));
    expected.add("protectedSlots",Catalog.JSON.toJsonTree(locked));expected.add("preferredItems",Catalog.JSON.toJsonTree(preferred));
    var after=PowerConfig.document();check(expected.equals(after.getAsJsonObject("settings").get("inventoryTools")),"layout changed unrelated inventory settings");
    before.getAsJsonObject("settings").add("inventoryTools",expected);check(before.equals(after),"layout changed another configuration section");
  }
  static void capture(Minecraft mc,Screen host,String name,boolean hidden)throws Exception {
    Inventory403Signals.backgrounds=0;Inventory403Signals.count=true;
    int mode=GL11.glGetInteger(GL11.GL_MATRIX_MODE);GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
    GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPushMatrix();GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();
    try{SettingsSnapshot.render(mc,host,host.width,host.height,REPORT.resolve(name+".png"));}
    finally{
      Inventory403Signals.count=false;
      GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();GL11.glPopAttrib();GL11.glMatrixMode(mode);
    }
    if(hidden)check(Inventory403Signals.backgrounds==0,"native inventory/model was still rendered under the layer");
    else if(host instanceof net.minecraft.client.gui.screen.container.ContainerScreen)check(Inventory403Signals.backgrounds>0,"Escape did not restore native rendering");
  }
  private static int left(SortingPreferencesScreen editor)throws Exception{return (int)UiChecks.call(editor,"left");}
  private static int controls(SortingPreferencesScreen editor)throws Exception{return (int)UiChecks.call(editor,"controlsTop");}
  @SuppressWarnings("unchecked") private static Set<Integer> locked(SortingPreferencesScreen editor)throws Exception{return (Set<Integer>)UiChecks.field(editor,"protectedSlots");}
  @SuppressWarnings("unchecked") private static Map<Integer,String> preferred(SortingPreferencesScreen editor)throws Exception{return (Map<Integer,String>)UiChecks.field(editor,"preferredItems");}
}
