package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import com.google.gson.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.*;
import local.luke.power.map.*;
import local.luke.power.mixin.SoundManagerAccessor;
import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import paulscode.sound.SoundSystemConfig;

/** Version-specific native checks. Runs only in a muted disposable client. */
public final class Release321Checks {
  private static final Path REPORT = Path.of("power-beta-data/reports/release-321");
  private static MapSettings zoomBefore;
  private static final List<double[]> zoomFrames=new ArrayList<>();
  private static java.lang.reflect.Field displayedField;
  public static void frame(Minecraft mc) {
    if(zoomBefore==null||zoomFrames.size()>=600)return;
    try {zoomFrames.add(new double[]{System.nanoTime()/1e6,MapConfig.current().zoom,displayedField.getDouble(null)});}
    catch(Exception e){throw new RuntimeException(e);}
  }
  static void run(Minecraft mc, String phase) throws Exception {
    failures = 0; Files.createDirectories(REPORT);
    if(phase.equals("zoom-watch")) {
      check(zoomBefore==null,"zoom watcher already armed");zoomBefore=MapConfig.copy();zoomFrames.clear();
      displayedField=MinimapBuffer.class.getDeclaredField("displayedZoom");displayedField.setAccessible(true);mc.setScreen(null);
      log("ZOOM WATCH ARMED press physical I once, then zoom-watch-check");return;
    }
    if(phase.equals("zoom-watch-check")) {
      check(zoomBefore!=null,"zoom watcher not armed");
      try {
        var unique=new HashSet<Double>();double from=zoomBefore.zoom;
        double target=MapConfig.current().zoom;check(target!=from,"physical zoom key did not change target");
        for(var frame:zoomFrames)if(frame[1]==target&&frame[2]>Math.min(from,target)+1e-8&&frame[2]<Math.max(from,target)-1e-8)unique.add(frame[2]);
        Files.writeString(REPORT.resolve("zoom-native-frames.json"),Catalog.JSON.toJson(zoomFrames));
        check(unique.size()>=3,"zoom has fewer than three distinct intermediate rendered frames: "+unique.size());
        log("PASS physical zoom key animates through "+unique.size()+" distinct intermediate native frames, sampled="+zoomFrames.size());
      } finally {MapConfig.queued(zoomBefore);zoomBefore=null;}
      return;
    }
    if (phase.equals("zoom-write")) {
      Files.writeString(REPORT.resolve("zoom-before.json"),Catalog.JSON.toJson(MapConfig.copy()));
      var settings=MapConfig.copy();settings.zoomSteps=new double[]{.3,.75,1.25,2.27,3.9};MapConfig.save(settings);
      log("PASS exact custom zoom steps durably saved for restart");return;
    }
    if (phase.equals("zoom-read")) {
      check(Arrays.equals(MapConfig.current().zoomSteps,new double[]{.3,.75,1.25,2.27,3.9}),"exact steps changed after native restart");
      MapConfig.save(Catalog.JSON.fromJson(Files.readString(REPORT.resolve("zoom-before.json")),MapSettings.class));
      log("PASS exact custom zoom steps survived native restart and original preferences restored");return;
    }
    if (phase.equals("ui")) ui(mc);
    else if (phase.equals("audio")) audio(mc);
    else if (phase.equals("steps")) steps(mc);
    else if (phase.equals("zoom")) MinimapHandoffChecks.zoom(mc);
    else throw new IllegalArgumentException("Use release-321 ui|audio|steps|zoom");
    log("RELEASE321 " + phase + " FAILURES " + failures);
  }
  private static Object field(Object target, String name) throws Exception { return Editor230Checks.get(target,name); }
  private static void field(Object target, String name, Object value) throws Exception {
    for(Class<?> type=target.getClass();type!=null;type=type.getSuperclass())try {
      var f=type.getDeclaredField(name);f.setAccessible(true);f.set(target,value);return;
    }catch(NoSuchFieldException ignored){}
    throw new NoSuchFieldException(name);
  }
  private static Object invoke(Object target, String name, Class<?>[] types, Object... args) throws Exception {
    return Editor230Checks.invoke(target,name,types,args);
  }
  private static void activate(PowerOptionsScreen options, String id) throws Exception {
    invoke(options,"activate",new Class<?>[]{Setting.class,int.class},find(options.session(),id),1);
  }
  private static void render(Minecraft mc, Screen screen, String name) throws Exception {
    SettingsSnapshot.render(mc, screen, screen.width, screen.height, REPORT.resolve(name+".png"));
  }
  private static void open(Minecraft mc, Screen screen, int w, int h) { mc.setScreen(screen);screen.init(mc,w,h); }
  private static void press(Screen screen,int x,int y,int button) { ((ScreenInput)(Object)screen).power$click(x,y,button); }
  private static void key(Screen screen,int code) { ((ScreenInput)(Object)screen).power$key('\0',code); }
  private static void ui(Minecraft mc) throws Exception {
    var previous=mc.currentScreen;boolean auto=MenuPreferences.current().autoApply;
    var before=MapConfig.copy();PowerConfig.flushPending();var document=PowerConfig.document();byte[] bytes=Files.readAllBytes(PowerConfig.path());
    try {
      MenuPreferences.current().autoApply=false;
      test("Minimap Edit borrows manual Options drafts and returns without saving",()->{
        var owner=new PowerOptionsScreen(null);open(mc,owner,960,540);
        var unrelated=find(owner.session(),"gameplay.boatPlacementFacing");unrelated.cycle(1);owner.changed(unrelated);
        var original=unrelated.value.deepCopy();var size=find(owner.session(),"maps.size");var oldSize=size.value.deepCopy();
        activate(owner,"maps.hudEditor");check(mc.currentScreen instanceof WorldMapScreen,"did not open World Map");
        var map=(WorldMapScreen)mc.currentScreen;map.init(mc,960,540);
        var panel=(PowerOptionsScreen)field(map,"settingsPanel");
        check(panel.session()==owner.session(),"Options draft was replaced");
        check(field(panel,"mapView").equals("Minimap")&&field(panel,"highlightedId").equals(""),"wrong initial Minimap view/highlight");
        size.value=new JsonPrimitive(oldSize.getAsInt()+1);panel.changed(size);
        check(MapConfig.current().size==size.value.getAsInt(),"Minimap did not preview live");
        render(mc,map,"minimap-sidebar-wide");
        map.init(mc,640,360);check(panel.session()==owner.session(),"resize replaced owner");
        map.init(mc,960,540);check(size.value.getAsInt()==oldSize.getAsInt()+1,"resize lost draft");
        call(panel,"cancelBorrowedMap");check(size.value.equals(oldSize),"Cancel did not restore map entry values");
        check(unrelated.value.equals(original)&&unrelated.changed(),"Cancel erased unrelated manual draft");
        call(map,"requestClose");check(mc.currentScreen==owner,"close did not return to Options");
        owner.session().discard();check(Arrays.equals(bytes,Files.readAllBytes(PowerConfig.path())),"navigation/Cancel saved config");
      });
      test("borrowed Escape, reopen and toolbar actions retain the original manual draft",()->{
        var owner=new PowerOptionsScreen(null);open(mc,owner,960,540);
        var unrelated=find(owner.session(),"gameplay.boatPlacementFacing");unrelated.cycle(1);owner.changed(unrelated);var original=unrelated.value.deepCopy();
        activate(owner,"maps.hudEditor");var map=(WorldMapScreen)mc.currentScreen;map.init(mc,960,540);
        var panel=(PowerOptionsScreen)field(map,"settingsPanel");var size=find(owner.session(),"maps.size");size.value=new JsonPrimitive(91);panel.changed(size);
        key(map,Keyboard.KEY_ESCAPE);key(map,Keyboard.KEY_ESCAPE);
        check(field(map,"settingsPanel")==null,"Escape did not close borrowed sidebar");
        check(!((Boolean)field(panel,"confirmClose")),"Escape offered whole-session Discard");
        Class action=Class.forName("local.luke.power.ui.WorldMapControls$Action");
        invoke(map,"toggleOverlay",new Class<?>[]{action},Enum.valueOf(action,"SLIME"));
        check(find(owner.session(),"maps.slimeChunks").value.getAsBoolean()==MapConfig.current().slimeChunks,"toolbar bypassed manual draft");
        call(map,"toggleSettings");panel=(PowerOptionsScreen)field(map,"settingsPanel");check(panel.session()==owner.session(),"reopen replaced session");
        call(map,"closeSettings");map.init(mc,640,360);call(map,"toggleSettings");
        check(mc.currentScreen==map&&((PowerOptionsScreen)field(map,"settingsPanel")).session()==owner.session(),"narrow reopen lost owner");
        call(map,"closeSettings");key(map,Keyboard.KEY_ESCAPE);check(mc.currentScreen==owner,"native Escape did not return owner");
        check(unrelated.value.equals(original)&&unrelated.changed(),"navigation erased unrelated manual draft");
        check(Arrays.equals(bytes,Files.readAllBytes(PowerConfig.path())),"manual navigation persisted values");owner.session().discard();
      });
      test("Cancel retains pre-entry map drafts even when sidebar edits return to original",()->{
        var owner=new PowerOptionsScreen(null);open(mc,owner,960,540);var size=find(owner.session(),"maps.size");var original=size.value.deepCopy();
        size.value=new JsonPrimitive(original.getAsInt()+1);owner.changed(size);activate(owner,"maps.hudEditor");
        var map=(WorldMapScreen)mc.currentScreen;map.init(mc,960,540);var panel=(PowerOptionsScreen)field(map,"settingsPanel");
        size.value=original;panel.changed(size);call(panel,"panelRelease");check(owner.session().changes()==0,"fixture did not return to original");
        call(panel,"cancelBorrowedMap");check(size.value.getAsInt()==original.getAsInt()+1,"Cancel lost pre-entry map draft");
        call(map,"requestClose");owner.session().discard();
      });
      test("borrowed Apply refreshes Cancel baseline and held sliders flush only on release",()->{
        var owner=new PowerOptionsScreen(null);open(mc,owner,960,540);activate(owner,"maps.hudEditor");
        var map=(WorldMapScreen)mc.currentScreen;map.init(mc,960,540);var panel=(PowerOptionsScreen)field(map,"settingsPanel");var size=find(owner.session(),"maps.size");
        size.value=new JsonPrimitive(92);panel.changed(size);invoke(panel,"save",new Class<?>[]{boolean.class,boolean.class},false,false);
        size.value=new JsonPrimitive(93);panel.changed(size);call(panel,"cancelBorrowedMap");check(size.value.getAsInt()==92&&MapConfig.current().size==92,"Cancel restored pre-Apply value");
        call(map,"toggleSettings");panel=(PowerOptionsScreen)field(map,"settingsPanel");MenuPreferences.current().autoApply=true;
        byte[] applied=Files.readAllBytes(PowerConfig.path());field(panel,"dragging",size);size.value=new JsonPrimitive(94);panel.changed(size);
        check(Arrays.equals(applied,Files.readAllBytes(PowerConfig.path())),"held slider wrote settings");
        call(panel,"panelRelease");check(PowerConfig.section("maps").get("size").getAsInt()==94,"release did not flush once");
        MenuPreferences.current().autoApply=false;call(map,"requestClose");
        PowerConfig.write(document);MapConfig.preview(before);
      });
      test("Minimap Edit falls back to the HUD editor for small windows or disabled world map",()->{
        for(boolean disabled:new boolean[]{false,true}) {
          var owner=new PowerOptionsScreen(null);open(mc,owner,disabled?960:640,360);
          if(disabled){var enabled=find(owner.session(),"maps.worldMap");enabled.value=new JsonPrimitive(false);owner.changed(enabled);}
          activate(owner,"maps.hudEditor");check(mc.currentScreen instanceof HudEditorScreen,"fallback did not open HUD editor");
          check(field(mc.currentScreen,"selected").equals("minimap"),"fallback selected wrong element");
          check(field(mc.currentScreen,"highlightedSetting").equals(""),"fallback highlighted a setting");
          render(mc,mc.currentScreen,disabled?"hud-world-map-off":"hud-small");owner.session().discard();
        }
      });
      test("Zoom-step exact entry, Undo, Defaults and dirty exits stay isolated",()->{
        var owner=new PowerOptionsScreen(null);open(mc,owner,640,360);
        var setting=find(owner.session(),"maps.zoomSteps");var original=setting.value.deepCopy();
        activate(owner,"maps.zoomSteps");check(mc.currentScreen instanceof ZoomStepsScreen,"wrong step editor");
        var editor=mc.currentScreen;editor.init(mc,320,240);render(mc,editor,"zoom-steps-small");
        int valueX=(int)call(editor,"valueX"), valueWidth=(int)call(editor,"valueWidth");
        press(editor,valueX+4,78,0);((ScreenInput)(Object)editor).power$release(valueX+valueWidth-4,78,0);
        check(((List<?>)field(editor,"values")).get(0).equals(4d),"quick slider release lost final motion");
        key(editor,Keyboard.KEY_ESCAPE);check((int)field(editor,"dragging")==-1,"exit retained drag ownership");key(editor,Keyboard.KEY_ESCAPE);
        // Restore defaults before testing a valid exact level.
        press(editor,(int)call(editor,"left")+100,editor.height-44,0);
        invoke(editor,"exact",new Class<?>[]{int.class},0);render(mc,editor,"zoom-steps-exact");
        Object inline=field(editor,"inline");((TextInput)field(inline,"input")).setText("0.3");key(editor,Keyboard.KEY_RETURN);
        check(setting.value.equals(original),"inline draft leaked into Options");
        key(editor,Keyboard.KEY_ESCAPE);check((boolean)call(field(editor,"exit"),"active"),"dirty exit has no prompt");
        key(editor,Keyboard.KEY_ESCAPE);call(editor,"save");check(mc.currentScreen==owner,"Done did not return");
        check(setting.value.getAsJsonArray().get(0).getAsDouble()==.3,"exact decimal was not committed");
        check(MapConfig.current().zoomSteps[0]==.3,"accepted steps did not preview");
        activate(owner,"maps.zoomSteps");editor=mc.currentScreen;editor.init(mc,640,360);
        render(mc,editor,"zoom-steps-wide");
        int l=(int)call(editor,"left");press(editor,l+100,editor.height-44,0);
        check(((List<?>)field(editor,"values")).get(0).equals(.25),"Defaults did not use selected base");
        press(editor,l+170,editor.height-44,0);check(((List<?>)field(editor,"values")).get(0).equals(.3),"Undo lost previous levels");
        owner.session().discard();
      });
    } finally {PowerConfig.write(document);MapConfig.preview(before);MenuPreferences.current().autoApply=auto;mc.setScreen(previous);}
  }
  private static void audio(Minecraft mc) throws Exception {
    var document=PowerConfig.document();var audio=AudioConfig.copy();float volume=mc.options.soundVolume;
    if (SoundManagerAccessor.power$system()==null) {
      // This clone starts muted. Native Beta skips sound-system creation when both sliders are zero.
      mc.options.soundVolume=1;mc.soundManager.method_2008();mc.options.soundVolume=volume;
      PowerConfig.write(document);AudioConfig.preview(audio);
    }
    check(SoundManagerAccessor.power$system()!=null,"silent native sound system did not initialize");
    var manager=(SoundManagerAccessor)mc.soundManager;
    var options=new PowerOptionsScreen(null);options.init(mc,640,360);
    test("every Extra sounds row previews a registered native sound, including disabled toggles",()->{
      int count=0;
      for(var setting:options.session().settings()) if(setting.group.equals("Extra sounds")) {
        String id=(String)invoke(options,"previewId",new Class<?>[]{Setting.class},setting);
        check(id!=null,"missing preview for "+setting.id);
        check(manager.power$sounds().method_958(id)!=null,"unregistered preview "+id);
        AudioController.previewSound(id);check(AudioController.previewing(id),"preview did not start "+id);
        AudioController.previewSound(id);check(!AudioController.previewing(id),"preview did not stop "+id);count++;
      }
      check(count==13,"expected all thirteen Extra sounds rows, got "+count);
    });
    test("all current bucket variants decode completely through the actual game codec",()->{
      var manifest=JsonParser.parseReader(new java.io.InputStreamReader(Release321Checks.class.getResourceAsStream("/assets/powerbeta/extra-sounds.json"))).getAsJsonObject();
      int count=0;
      for(var value:manifest.getAsJsonArray("assets")) {
        var asset=value.getAsJsonObject();if(!asset.get("event").getAsString().contains("bucket"))continue;
        String file=asset.get("file").getAsString();var url=Release321Checks.class.getResource("/"+file);check(url!=null,file);
        byte[] data;try(var in=url.openStream()){data=in.readAllBytes();}
        int last=-1;for(int i=0;i<data.length-27;i++)if(data[i]=='O'&&data[i+1]=='g'&&data[i+2]=='g'&&data[i+3]=='S')last=i;
        check(last>=0,"missing Ogg page");long samples=ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getLong(last+6);
        var codec=SoundSystemConfig.getCodec(file);check(codec!=null&&codec.initialize(url),"codec initialization failed "+file);
        try {
          var decoded=codec.readAll();check(decoded!=null&&decoded.audioData!=null,"codec returned no samples");
          double seconds=decoded.audioData.length/(double)decoded.audioFormat.getFrameSize()/decoded.audioFormat.getSampleRate();
          double duration=samples/48000d;check(Math.abs(seconds-duration)<.06,"clipped decode "+file+" expected "+duration+" actual "+seconds);
          log("BUCKET DECODE "+file.substring(file.lastIndexOf('/')+1)+" seconds="+seconds+" source="+duration);count++;
        } finally {codec.cleanup();}
      }
      check(count==12,"did not decode all twelve variants");
    });
  }
  private static void steps(Minecraft mc) throws Exception {
    var player=mc.player;var previous=mc.currentScreen;mc.setScreen(null);
    double x=player.x,y=player.y,z=player.z;float distance=player.field_1635;
    int threshold=(int)field(player,"field_1611");boolean remote=mc.world.isRemote;
    var rules=AccessRules.copy(mc);String mode=invoke(player,"power_mode",new Class<?>[0]).toString();
    boolean flying=(boolean)invoke(player,"creative_isFlying",new Class<?>[0]);
    int override=(int)invoke(player,"power_flightOverride",new Class<?>[0]);
    try {
      test("one native ground step discards accumulated footstep distance",()->{
        player.method_1340(x,110,z);mc.world.method_154((int)Math.floor(x),(int)Math.floor(player.boundingBox.minY-.2),(int)Math.floor(z),1,0);
        player.field_1635=100;field(player,"field_1611",0);player.move(.05,0,0);
        check((int)field(player,"field_1611")==101,"native footstep threshold retained backlog");
        player.move(.05,0,0);check((int)field(player,"field_1611")==101,"next tick consumed backlog");
      });
      test("Creative, Spectator and permitted Survival flight discard silent travel distance",()->{
        AccessRules.set(mc,local.luke.power.rules.GameRule.ALLOW_FLIGHT,true);
        for(String target:List.of("CREATIVE","SPECTATOR","SURVIVAL")) {
          CreativeVehicleChecks.mode(mc,target);invoke(player,"power_flightOverride",new Class<?>[]{int.class},1);
          invoke(player,"creative_setFlying",new Class<?>[]{boolean.class},true);
          player.method_1340(x,120,z);player.field_1635=100;field(player,"field_1611",0);player.move(2,0,0);
          if(target.equals("SPECTATOR")) check(player.field_1635==100,"no-clip Spectator unexpectedly accumulated footsteps");
          else check((int)field(player,"field_1611")==((int)Math.floor(player.field_1635)+1),target+" flight kept backlog");
        }
        CreativeVehicleChecks.mode(mc,"SURVIVAL");invoke(player,"creative_setFlying",new Class<?>[]{boolean.class},false);
        player.method_1340(x,110,z);
      });
      test("freecam is included by default and follows actual controller state",()->{
        Object camera=Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
        boolean active=(boolean)invoke(camera,"isActive",new Class<?>[0]);
        try {
          check(new local.luke.power.status.StatusSettings().freecam,"freecam inclusion defaults off");
          invoke(camera,"setActive",new Class<?>[]{boolean.class},true);
          check("Freecam".equals(local.luke.power.input.TweakIndicators.label(local.luke.power.input.TweakIndicators.Tweak.FREECAM)),"active camera missing from HUD provider");
          invoke(camera,"setActive",new Class<?>[]{boolean.class},false);
          check(local.luke.power.input.TweakIndicators.label(local.luke.power.input.TweakIndicators.Tweak.FREECAM)==null,"inactive camera still listed");
        } finally {invoke(camera,"setActive",new Class<?>[]{boolean.class},active);}
      });
      test("multiplayer footstep thresholds remain native",()->{
        CreativeVehicleChecks.mode(mc,"SURVIVAL");invoke(player,"creative_setFlying",new Class<?>[]{boolean.class},false);
        player.method_1340(x,110,z);
        mc.world.isRemote=true;player.field_1635=100;field(player,"field_1611",0);player.move(.05,0,0);
        check((int)field(player,"field_1611")==1,"changed remote threshold");
      });
    } finally {mc.world.isRemote=remote;CreativeVehicleChecks.mode(mc,mode);invoke(player,"power_flightOverride",new Class<?>[]{int.class},override);invoke(player,"creative_setFlying",new Class<?>[]{boolean.class},flying);AccessRules.restore(mc,rules);player.field_1635=distance;field(player,"field_1611",threshold);player.method_1340(x,y,z);mc.setScreen(previous);}
  }
}
