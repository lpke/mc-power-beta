package local.luke.power.validation;

import static local.luke.power.validation.Release240Checks.*;
import static local.luke.power.validation.Validation.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

final class Hud240Checks {
  static void run(Minecraft mc)throws Exception {
    MenuPreferences.update(v->v.autoApply=false);
    var options=new PowerOptionsScreen(null);open(mc,options,640,420);
    test("HUD search sections navigate to the correct tab while individual rows edit in place",()->{
      for(String query:List.of("crosshair","Info cluster 2","Info order","Version text")){
        set(options,"scroll",0d);UiChecks.search(options,query);((Set<?>)get(options,"collapsed")).clear();call(options,"layout");
        var rows=(List<?>)get(options,"rows");Object heading=rows.stream().filter(row->{try{return (boolean)call(row,"heading")&&((String)call(row,"group")).startsWith("Interface / HUD editor /");}catch(Exception e){throw new RuntimeException(e);}}).findFirst().orElseThrow();
        var match=(Setting)call(rows.get(rows.indexOf(heading)+1),"setting");
        check(((String)call(heading,"group")).endsWith(HudEditorSettings.tabLabel(HudEditorSettings.tab(match,1))),"wrong HUD search section");
        int y=n(options,"top")+n(heading,"y")+10;click(options,n(options,"left")+80,y,0);
        check(mc.currentScreen instanceof HudEditorScreen,"heading did not open HUD editor");
        var hud=(HudEditorScreen)mc.currentScreen;check(get(hud,"selected").equals(HudEditorSettings.tab(match,1)),"heading opened wrong tab");
        mc.setScreen(options);
      }
      set(options,"scroll",0d);UiChecks.search(options,"crosshair");((Set<?>)get(options,"collapsed")).clear();call(options,"layout");
      var rows=(List<?>)get(options,"rows");Object row=rows.stream().filter(r->{try{var s=(Setting)call(r,"setting");return s!=null&&s.id.equals("power_hud:config.disableCrosshair");}catch(Exception e){throw new RuntimeException(e);}}).findFirst().orElseThrow();
      var value=(Setting)call(row,"setting");boolean before=value.value.getAsBoolean();byte[] bytes=Files.readAllBytes(PowerConfig.path());
      click(options,((Number)call(options,"mainControlLeft",row)).intValue()+8,n(options,"top")+n(row,"y")+(n(row,"height")==40?24:9),0);
      check(mc.currentScreen==options,"search row opened "+mc.currentScreen.getClass().getName());check(value.value.getAsBoolean()!=before,"search row value did not change; scroll="+get(options,"scroll")+" rowY="+n(row,"y")+" top="+n(options,"top"));check(Arrays.equals(bytes,Files.readAllBytes(PowerConfig.path())),"manual search edit wrote before Apply");
      call(options,"save",false,false);check(setting(SettingsRegistry.open(mc),value.id).value.equals(value.value),"search Apply did not reach live state");
      value.value=new JsonPrimitive(before);options.changed(value);call(options,"save",false,false);
      UiChecks.search(options,"");
    });
    test("all responsive sizes show quiet sliders, shared bars and selected-only cluster outlines",()->{
      var hud=new HudEditorScreen(options,HudEditorSettings.ENTRY);open(mc,hud,640,420);
      check(!(boolean)get(hud,"background"),"background did not start Off");
      for(int[] size:new int[][]{{320,240},{640,420},{960,540},{1720,720},{540,960}}){
        shot(mc,hud,"hud-"+size[0]+"x"+size[1],size[0],size[1]);
        check(n(hud,"previewSpan")>=280&&n(hud,"previewBottom")-n(hud,"previewTop")>=48,"responsive preview lost space");
      }
      open(mc,hud,640,420);hud.render(-1,-1,0);
      var preview=get(hud,"preview");var elements=(List<?>)call(preview,"elements");
      check(elements.stream().anyMatch(e->{try{return call(e,"id").equals("chat");}catch(Exception ex){throw new RuntimeException(ex);}}),"Normal preview omitted chat");
      int x=n(hud,"previewLeft")+n(hud,"previewSpan")-5,y=n(hud,"previewTop")+5;
      call(hud,"select","tweaks");
      click(hud,x,y,0);check(get(hud,"selected").equals("layout"),"blank preview did not select General");
      var d=draft(hud);d.get("infoHud.enabled").value=new JsonPrimitive(false);call(hud,"select","cluster.2");call(hud,"afterEdit");
      shot(mc,hud,"selected-hidden-cluster",640,420);
      var hidden=(List<?>)call(preview,"elements");check(hidden.stream().filter(e->{try{return ((String)call(e,"id")).startsWith("cluster.")&&(boolean)call(e,"hidden");}catch(Exception ex){throw new RuntimeException(ex);}}).count()==4,"hidden bounds lost");
      check(!d.get("infoHud.enabled").value.getAsBoolean(),"selection enabled hidden HUD");
      call(hud,"select","layout");shot(mc,hud,"unselected-hidden-clusters",640,420);
      byte[] bytes=Files.readAllBytes(PowerConfig.path());var toggle=d.get("power_hud:config.disableCrosshair");int cy=expose(hud,toggle),clicksBefore=clicks;
      click(hud,n(hud,"controlX")+8,cy+7,0);check(clicks==clicksBefore+1,"HUD toggle omitted UI click");check(Arrays.equals(bytes,Files.readAllBytes(PowerConfig.path()))&&options.session().changes()==0,"HUD edit escaped its draft");
    });
    test("F3 Reorder is enlarged, local, undoable and supports gaps and outside Save",()->{
      var hud=new HudEditorScreen(options,"interface.debugOrder");open(mc,hud,960,540);var d=draft(hud);byte[] bytes=Files.readAllBytes(PowerConfig.path());var original=d.get("interface.debugOrder").value.deepCopy();
      var order=d.get("interface.debugOrder");int cy=expose(hud,order);click(hud,n(hud,"controlX")+8,cy+7,0);
      var reorder=get(hud,"reorder");check((boolean)call(reorder,"active"),"Reorder did not activate");hud.render(-1,-1,0);
      shot(mc,hud,"f3-reorder-active",640,420);
      int left=(int)get(reorder,"left"),top=(int)get(reorder,"top");
      click(hud,left+30,top+8,0);((ScreenInput)(Object)hud).power$release(left+30,top+5+4*12+5,0);
      check(d.get("interface.debugOrder").value.equals(original),"temporary reorder changed the HUD draft before Save");
      check(!Catalog.JSON.toJsonTree(get(reorder,"rows")).equals(original),"drag did not change temporary order");
      cy=expose(hud,order);click(hud,n(hud,"controlX")+8,cy+7,0);
      check(!(boolean)call(reorder,"active")&&!d.get(order.id).value.equals(original),"Save did not commit local order");
      check(Arrays.equals(bytes,Files.readAllBytes(PowerConfig.path()))&&options.session().changes()==0,"reorder wrote live settings");
      call(hud,"undo");check(d.get(order.id).value.equals(original),"Undo did not restore order");
      hud.beginReorder();hud.render(-1,-1,0);left=(int)get(reorder,"left");top=(int)get(reorder,"top");
      click(hud,left+30,top+8,0);((ScreenInput)(Object)hud).power$release(left+30,top+5+12*12+5,0);
      click(hud,5,24,0);check(!(boolean)call(reorder,"active"),"outside click did not Save reorder");
      check(d.get(order.id).value.getAsJsonArray().size()>=original.getAsJsonArray().size(),"empty-row drop lost rows");
      shot(mc,hud,"f3-reordered",960,540);key(hud,Keyboard.KEY_ESCAPE);check((boolean)call(get(hud,"exit"),"active"),"dirty F3 exit skipped warning");
      click(hud,hud.width/2,hud.height/2+18,0);check(mc.currentScreen==options&&Arrays.equals(bytes,Files.readAllBytes(PowerConfig.path())),"F3 Discard wrote settings");
    });
    test("title grid sliders reset and edit exact values with native click ownership",()->{
      var title=new TitleLetteringScreen(options);open(mc,title,640,420);var columns=(Setting)get(title,"columns");
      check((boolean)call(title,"usesSlider",columns)&&(boolean)call(title,"resettable",columns),"grid slider/R missing");
      var before=columns.value.deepCopy();int cy=(int)call(title,"controlY",columns),x=n(title,"controlX"),w=((Number)call(title,"controlWidth",columns)).intValue(),sounds=clicks;
      click(title,x+w/2,cy+7,0);((ScreenInput)(Object)title).power$release(x+w/2,cy+7,0);check(clicks==sounds,"slider played click sound");
      click(title,x+5,cy+7,1);check(clicks==sounds+1,"exact value entry omitted click");
      Object inline=get(title,"inline");((TextInput)get(inline,"input")).setText("50");key(title,Keyboard.KEY_RETURN);check(columns.value.getAsInt()==50,"grid exact value did not apply");
      click(title,n(title,"resetX")+8,cy+7,0);check(columns.value.equals(columns.defaultValue),"grid R did not restore default");
      int tabX=n(title,"left")+n(title,"tabWidth")+8;sounds=clicks;click(title,tabX,38,0);check(clicks==sounds+1,"title tab omitted click");
      shot(mc,title,"title-appearance",640,420);key(title,Keyboard.KEY_ESCAPE);click(title,title.width/2,title.height/2+18,0);
    });
  }
}
