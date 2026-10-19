package local.luke.power.validation;

import static local.luke.power.validation.Release240Checks.*;
import static local.luke.power.validation.Validation.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import local.luke.power.config.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.world.WorldRuleFiles;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.TitleScreen;
import net.modificationstation.stationapi.api.client.gui.screen.EditWorldScreen;
import org.lwjgl.input.Keyboard;

final class Menu240Checks {
  static void run(Minecraft mc)throws Exception {
    check(mc.world==null,"Run title before menu checks");
    test("title version follows metadata and logo cycling never waits for a stalled settings disk",()->{
      var title=new TitleScreen();open(mc,title,640,420);
      try {
        captureTitle=true;titleText.clear();MenuPreferences.previewPowerVersion(false);shot(mc,title,"main-menu-version",640,420);
        check(titleText.stream().anyMatch(text->text.equals("Minecraft Beta 1.7.3 ("+PackVersion.label()+")")),"native title did not draw pack version suffix");
        titleText.clear();MenuPreferences.previewPowerVersion(true);shot(mc,title,"main-menu-hidden-version",640,420);
        check(titleText.contains("Minecraft Beta 1.7.3")&&titleText.stream().noneMatch(text->text.contains(PackVersion.label())),"hide version ignored");
      } finally {captureTitle=false;MenuPreferences.previewPowerVersion(false);}
      var disk=PowerConfig.class.getDeclaredField("DISK");disk.setAccessible(true);Object lock=disk.get(null);
      CountDownLatch held=new CountDownLatch(1),release=new CountDownLatch(1);
      Thread worker=new Thread(()->{synchronized(lock){held.countDown();try{release.await(5,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}}},"2.4 stalled settings fixture");worker.start();check(held.await(2,TimeUnit.SECONDS),"disk fixture did not start");
      long max=0;double clock=local.luke.power.input.PanoramaRenderer.animationTicks();
      try{for(int i=0;i<10;i++){long start=System.nanoTime();click(title,title.width/2,50,i<5?0:1);max=Math.max(max,System.nanoTime()-start);check(TitleSelection.error().isEmpty(),"title selection failed");}}
      finally{release.countDown();worker.join(2000);}
      check(max<150_000_000L,"logo click waited for durable I/O: "+max/1e6+"ms");check(local.luke.power.input.PanoramaRenderer.animationTicks()>=clock,"panorama clock reset");PowerConfig.flushPending();
      log("TITLE CLICK MAX "+max/1e6+"ms with durable writer stalled");
    });
    test("Interface groups, defaults, retro Customize and screenshot rows follow the requested order",()->{
      var options=new PowerOptionsScreen(null);open(mc,options,640,420);set(options,"page","Interface");UiChecks.search(options,"");
      ((Set<?>)get(options,"collapsed")).clear();call(options,"layout");
      var rows=(List<?>)get(options,"rows");List<String> main=new ArrayList<>();
      for(Object row:rows){var s=(Setting)call(row,"setting");if(s!=null)main.add(s.id);}
      var general=List.of(HudEditorSettings.ENTRY,"station-api-configuration:config.loadingScreenOption","power_client_fixes:config.disableServerListIpAddresses","power_controls:userinterface.photoModeConfig.enablePhotoModeButton","power_controls:userinterface.showQuitButton","power_controls:userinterface.clearTextFieldWithRightClick","interface.blinkingUiCursor");
      check(main.subList(0,general.size()).equals(general),"General order differs: "+main);check(!main.contains("logo.logo.lines"),"old Title lettering row remains");
      check(options.session().settings().stream().noneMatch(s->s.id.endsWith("achievementBackToMenu")),"removed achievement toggle remains");
      var style=setting(options.session(),"logo.style");style.value=new JsonPrimitive(0);call(options,"layout");
      Object row=((List<?>)get(options,"rows")).stream().filter(r->{try{return call(r,"setting")==style;}catch(Exception e){throw new RuntimeException(e);}}).findFirst().orElseThrow();
      int y=n(options,"top")+n(row,"y")+8;set(options,"scroll",Math.max(0,n(row,"y")-40d));y=n(options,"top")+n(row,"y")-((Number)get(options,"scroll")).intValue()+8;
      shot(mc,options,"interface-wide",640,420);click(options,((Number)call(options,"controlLeft",row)).intValue()+12,y,0);check(mc.currentScreen instanceof TitleLetteringScreen,"Customize did not open title painter");
      check(!style.choices.get(0).contains("customisable"),"old retro suffix remains");
      for(String id:List.of("power_controls:userinterface.panoramaConfig.onlyTitleScreen","interface.hidePowerBetaVersion"))check(!setting(options.session(),id).defaultValue.getAsBoolean(),"wrong new default "+id);
      check(setting(options.session(),"power_environment:config.WEATHER_CONFIG.sleepOnlyResetsWeatherWhenRaining").defaultValue.getAsBoolean(),"sleep default not On");
      check(setting(options.session(),"power_controls:userinterface.versionTextConfig.customVersionText").defaultValue.getAsString().equals(PackVersion.label()),"custom version default differs from metadata");
    });
    test("world Edit title, inline selected name, cancel, Enter and check preserve native metadata",()->{
      var select=new net.minecraft.client.gui.screen.world.SelectWorldScreen(new TitleScreen());open(mc,select,640,420);
      var worlds=(List<net.minecraft.class_591>)get(select,"field_2436");check(!worlds.isEmpty(),"Create a disposable world before running menus");
      var data=worlds.get(0);var menu=new EditWorldScreen(select,data);open(mc,menu,640,420);shot(mc,menu,"world-edit-before",640,420);
      var control=((WorldEditOwner)menu).power$worldControls();check(control.title(menu).equals("Edit "+data.method_1958()),"Edit title omitted world name");
      var list=get(menu,"buttonMenu");var entries=((WorldEditButtons)list).power$editButtons();int rename=-1,rules=-1,convert=-1;
      for(int i=0;i<entries.size();i++){String label=entries.get(i).button().text;if(label.equals(net.minecraft.class_629.method_2049("selectWorld.rename")))rename=i;if(label.equals("Edit game rules"))rules=i;if(label.toLowerCase(Locale.ROOT).contains("mcregion"))convert=i;}
      check(rename>=0&&rules>=0&& (convert<0||convert==rules+1),"world action order differs");
      var button=entries.get(rename).button();Path level=Path.of("saves",data.method_1956(),"level.dat"),old=level.resolveSibling("level.dat_old");byte[] before=Files.readAllBytes(level),previous=Files.exists(old)?Files.readAllBytes(old):null;
      NbtCopy player=NbtCopy.of(WorldRuleFiles.read(level).getCompound("Data").getCompound("Player"));
      set(list,"mouseX",button.x+8);set(list,"mouseY",button.y+8);call(list,"entryClicked",rename,false);((ScreenInput)(Object)menu).power$release(button.x+8,button.y+8,0);
      check(control.active()&&mc.currentScreen==menu,"Rename opened a separate menu");check(control.hides(button)&&entries.stream().filter(e->e.button()!=button).noneMatch(e->control.hides(e.button())),"inline editor did not exclusively hide Rename");Object inline=get(control,"inline");var input=(TextInput)get(inline,"input");
      check(input.text().equals(data.method_1958())&&input.start()==0&&input.end()==input.text().length(),"existing name not pre-selected");
      ((ScreenInput)(Object)menu).power$key('N',Keyboard.KEY_N);check(input.text().equals("N"),"typing did not replace selection");key(menu,Keyboard.KEY_ESCAPE);check(!control.active()&&mc.currentScreen==menu&&Arrays.equals(before,Files.readAllBytes(level)),"Rename Escape wrote or closed world editor");
      call(list,"entryClicked",rename,false);((ScreenInput)(Object)menu).power$release(button.x+8,button.y+8,0);((TextInput)get(get(control,"inline"),"input")).setText("2.4 renamed world");key(menu,Keyboard.KEY_RETURN);check(WorldRuleFiles.read(level).getCompound("Data").getString("LevelName").equals("2.4 renamed world"),"Enter did not persist rename");
      check(control.title(menu).equals("Edit 2.4 renamed world"),"title did not update after rename");
      call(list,"entryClicked",rename,false);((ScreenInput)(Object)menu).power$release(button.x+8,button.y+8,0);shot(mc,menu,"world-rename-inline",640,420);click(menu,button.x+190,button.y+8,0);check(!control.active(),"inline check did not commit");
      check(player.equals(NbtCopy.of(WorldRuleFiles.read(level).getCompound("Data").getCompound("Player"))),"Rename changed native player data");if(previous!=null)check(Arrays.equals(previous,Files.readAllBytes(old)),"Rename changed previous metadata");
      key(menu,Keyboard.KEY_ESCAPE);check(mc.currentScreen==select,"Edit Escape did not return to world list");
    });
  }
  static void gameplay(Minecraft mc)throws Exception {
    check(mc.world!=null&&mc.player!=null,"Load the disposable world before gameplay checks");
    test("live F3 uses the saved left-side order, visibility, gaps and scale without moving memory",()->{
      boolean debug=mc.options.debugHud;var old=MenuPreferences.debugOrder();double size=MenuPreferences.debugTextSize();boolean terrain=MenuPreferences.debugLine("terrain");
      try {
        var order=new ArrayList<>(DebugOrder.defaults());order.remove("biome");order.add(0,"biome");order.add(1,"");
        MenuPreferences.previewDebugOrder(order);MenuPreferences.previewDebugSize(1.6);MenuPreferences.previewDebugLine("terrain",false);
        mc.options.debugHud=true;mc.inGameHud.render(0F,true,0,0);
        var lines=DebugOverlay.lastLayout();check(lines.stream().noneMatch(line->line.id().equals("terrain")),"disabled terrain line rendered");
        var biome=lines.stream().filter(line->line.id().equals("biome")).findFirst().orElseThrow();var version=lines.stream().filter(line->line.id().equals("version")).findFirst().orElseThrow();
        check(biome.y()==2&&version.y()==34,"live F3 ignored saved order/gap/scale");
        check(lines.stream().anyMatch(line->line.id().isEmpty()&&line.y()==2&&line.text().startsWith("Used memory:")),"right memory layout changed");
      } finally {mc.options.debugHud=debug;MenuPreferences.previewDebugOrder(old);MenuPreferences.previewDebugSize(size);MenuPreferences.previewDebugLine("terrain",terrain);}
    });
    test("native achievements expand to available space and Done and Escape return to pause",()->{
      if(mc.player==null||mc.field_2773==null){log("ACHIEVEMENTS require a loaded player; run gameplay checks separately");return;}
      var screen=new net.minecraft.class_609(mc.field_2773);open(mc,screen,640,420);
      check((int)get(screen,"field_2618")>256&&(int)get(screen,"field_2619")>202,"achievement viewport stayed fixed");
      shot(mc,screen,"achievements-expanded",640,420);key(screen,Keyboard.KEY_ESCAPE);check(mc.currentScreen instanceof net.minecraft.class_525,"achievement Escape unpaused");
      open(mc,screen,640,420);var done=((ScreenInput)(Object)screen).power$buttons().stream().filter(b->b.id==1).findFirst().orElseThrow();click(screen,done.x+8,done.y+8,0);check(mc.currentScreen instanceof net.minecraft.class_525,"achievement Done unpaused");
    });
  }
  private record NbtCopy(byte[] bytes){
    static NbtCopy of(net.minecraft.nbt.NbtCompound value)throws Exception{var out=new java.io.ByteArrayOutputStream();net.minecraft.nbt.NbtIo.writeCompressed(value,out);return new NbtCopy(out.toByteArray());}
    public boolean equals(Object o){return o instanceof NbtCopy n&&Arrays.equals(bytes,n.bytes);}
    public int hashCode(){return Arrays.hashCode(bytes);}
  }
}
