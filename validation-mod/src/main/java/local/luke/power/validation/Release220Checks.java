package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.FreecamPreviewChecks.*;
import com.google.gson.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.config.*;
import local.luke.power.hud.HudCoordinates;
import local.luke.power.input.*;
import local.luke.power.render.GhostHead;
import local.luke.power.rules.*;
import local.luke.power.ui.*;
import local.luke.power.world.*;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NbtCompound;
import org.lwjgl.opengl.GL11;

/** Native-client checks confined to a muted disposable world. */
public final class Release220Checks {
  private static boolean capture;
  private static final List<double[]> heads = new ArrayList<>();
  public static void head(double x,double y,double z,float alpha) {
    if(capture)heads.add(new double[]{x,y,z,alpha});
  }
  private static Object camera() throws Exception {
    return Class.forName("local.luke.power.camera.Freecam").getField("freecamController").get(null);
  }
  private static Object cameraSettings() throws Exception {
    return Class.forName("local.luke.power.camera.FreecamConfig").getField("config").get(null);
  }
  private static List<?> rows(PowerOptionsScreen s) throws Exception { return (List<?>)field(s,"rows"); }
  private static List<Setting> visible(PowerOptionsScreen s) throws Exception {
    var result=new ArrayList<Setting>();for(Object row:rows(s))if(call(row,"setting") instanceof Setting v)result.add(v);return result;
  }
  private static void page(PowerOptionsScreen s,String page) throws Exception {
    field(s,"page",page);field(s,"scroll",0d);search(s,"");
  }
  static void run(Minecraft mc) throws Exception {
    failures=0;var rules=AccessRules.copy(mc);var saved=new NbtCompound();mc.player.write(saved);
    var screen=mc.currentScreen;boolean third=mc.options.thirdPerson,feet=MenuPreferences.useFeetYCoordinates();
    Object config=cameraSettings();String originalConfig=Catalog.JSON.toJson(config);boolean auto=MenuPreferences.current().autoApply;
    Path report=Path.of("power-beta-data/reports/220");Files.createDirectories(report);
    try {
      mc.setScreen(null);mc.paused=false;DeathChecks.alive(mc);active(false);AccessRules.restore(mc,RuleSet.cheats());
      test("flight rows share units, order and defaults without retired switches",()->{
        var session=SettingsRegistry.open(mc);var ordered=SettingsLayout.ordered(session.settings());
        for(String[] ids:new String[][]{
          {"creative.flightSpeed","creative.sprintMultiplier","creative.acceleration","creative.glide","creative.sprintToggle"},
          {"creative.spectatorSpeed","creative.spectatorSprintMultiplier","creative.spectatorAcceleration","creative.spectatorGlide","creative.spectatorSprintToggle"},
          {"power_camera:config.speed","power_camera:config.sprintMultiplier","power_camera:config.acceleration","power_camera:config.drag","power_camera:config.sprintToggle"}}) {
          Setting first=find(session,ids[0]);var group=ordered.stream().filter(v->v.page.equals(first.page)&&v.group.equals(first.group)).toList();
          check(group.subList(0,5).stream().map(v->v.id).toList().equals(List.of(ids)),"flight order "+group.stream().map(v->v.id).toList());
          check(first.label.endsWith("speed %"),"speed units");
          check(!first.description.contains("blocks per second"),"tooltip uses another unit");
          check(first.description.contains("40%"),"walking baseline missing");
        }
        for(String id:List.of("creative.sprintFlight","creative.spectatorSprintFlight","power_camera:config.sprint","power_camera:config.classicMovement"))
          check(session.settings().stream().noneMatch(s->s.id.equals(id)),"retired "+id);
        var speed=find(session,"power_camera:config.speed");check(speed.display(speed.defaultValue).equals("110"),"freecam default percentage");
        check(find(session,"power_camera:config.drag").defaultValue.getAsFloat()==3,"freecam drift default");
        check(find(session,"creative.glide").defaultValue.getAsInt()==5&&find(session,"creative.spectatorGlide").defaultValue.getAsInt()==5,"native drift defaults");
        check(ControlLinks.enabled(session,find(session,"keys.power_creative.sprint")),"sprint key hidden");
        for(Setting setting:session.settings())check(!setting.label.contains(", %"),"percentage punctuation "+setting.id);
        session.discard();
      });
      MenuPreferences.update(v->v.autoApply=false);
      var options=new PowerOptionsScreen(null);mc.setScreen(options);((Set<?>)field(options,"collapsed")).clear();
      test("anaglyph and OpenGL use the requested groups and initial collapse",()->{
        var anaglyph=find(options.session(),"native.anaglyph");check(anaglyph.page.equals("Advanced")&&anaglyph.group.equals("Miscellaneous"),"anaglyph group");
        var groups=SettingsLayout.groups("Advanced");check(groups.get(groups.size()-1).equals("Miscellaneous"),"misc not last");
        check(SettingsLayout.initiallyCollapsed().contains("Advanced/Miscellaneous"),"misc starts open");
        page(options,"Video");check(visible(options).stream().filter(s->s.group.equals("Performance")).findFirst().orElseThrow().id.equals("native.opengl"),"OpenGL not first");
      });
      test("general search excludes tracks but retains individual sounds and library matches",()->{
        for(String query:List.of("music","rotation","sweden","calm")) {
          search(options,query);
          check(visible(options).stream().noneMatch(s->s.group.equals("Individual music tracks")||s.group.equals("Track rotation")||s.id.startsWith("audio.sound.music:")),"music leaked into search");
        }
        search(options,"sound");check(visible(options).stream().anyMatch(s->s.group.equals("Individual sounds")),"individual sounds lost");
        page(options,"Audio");var library=(MusicLibraryScreen)field(options,"library");library.showTracks();
        library.restore(new MusicLibraryScreen.State(false,"","sweden",0,0));
        check(((List<?>)field(library,"rows")).stream().anyMatch(r->{try{return call(r,"id")!=null;}catch(Exception e){throw new RuntimeException(e);}}),"library search lost tracks");
      });
      test("audio divider shares layout at narrow and wide sizes without becoming a heading",()->{
        for(int[] size:new int[][]{{640,420},{320,240},{320,300}}) {
          options.init(mc,size[0],size[1]);page(options,"Audio");((Set<?>)field(options,"collapsed")).clear();call(options,"layout");
          var rows=rows(options);int index=-1;
          for(int i=0;i<rows.size();i++)if((boolean)call(rows.get(i),"separator")){check(index<0,"duplicate divider");index=i;}
          check(index>0&&index+1<rows.size(),"divider missing");Object divider=rows.get(index),before=rows.get(index-1),after=rows.get(index+1);
          check(((Setting)call(before,"setting")).id.equals("native.sound"),"divider location");
          check(call(after,"setting")!=null&&!(boolean)call(divider,"heading"),"divider treated as heading");
          check((int)call(divider,"y")==((int)call(before,"y")+(int)call(before,"height")),"gap before divider");
          check((int)call(after,"y")==((int)call(divider,"y")+(int)call(divider,"height")),"gap after divider");
          int top=(int)call(options,"top"),dy=(int)call(divider,"y");field(options,"scroll",(double)Math.max(0,dy-(size[0]==320?40:65)));
          var values=options.session().settings().stream().map(s->s.value.deepCopy()).toList();
          click(options,(int)call(options,"left")+20,top+dy-((Double)field(options,"scroll")).intValue()+6);
          check(values.equals(options.session().settings().stream().map(s->s.value).toList()),"divider click edited setting");
          SettingsSnapshot.render(mc,options,size[0],size[1],report.resolve("audio-divider-"+size[0]+"-"+size[1]+".png"));
          ((Set<String>)field(options,"collapsed")).add("Audio/Volume");call(options,"layout");
          check(rows(options).stream().noneMatch(r->{try{return (boolean)call(r,"separator");}catch(Exception e){throw new RuntimeException(e);}}),"collapsed divider remains");
          ((Set<?>)field(options,"collapsed")).clear();search(options,"game sounds");
          check(rows(options).stream().noneMatch(r->{try{return (boolean)call(r,"separator");}catch(Exception e){throw new RuntimeException(e);}}),"search divider remains");
          Method open=PowerOptionsScreen.class.getDeclaredMethod("openGroup",Setting.class);open.setAccessible(true);open.invoke(options,find(options.session(),"native.sound"));
          check(field(options,"page").equals("Audio")&&((TextInput)field(options,"search")).text().isEmpty(),"search jump destination");
          check(rows(options).stream().anyMatch(r->{try{return (boolean)call(r,"separator");}catch(Exception e){throw new RuntimeException(e);}}),"jump did not restore divider");
        }
      });
      options.session().discard();mc.setScreen(null);
      test("WorldEdit selects the reported block for each mode and coordinate preference",()->{
        var editorType=Class.forName("local.luke.power.worldedit.WorldEditor");
        for(String mode:List.of("SURVIVAL","CREATIVE","SPECTATOR"))for(boolean useFeet:new boolean[]{true,false}) {
          active(false);CreativeVehicleChecks.mode(mc,mode);MenuPreferences.previewFeetY(useFeet);
          mc.player.method_1340(-4.25,111.25,5.75);
          int y=(int)Math.floor(HudCoordinates.y(mc.player));
          editorType.getMethod("command",Minecraft.class,String.class).invoke(null,mc,"//pos1");
          editorType.getMethod("command",Minecraft.class,String.class).invoke(null,mc,"//pos2");
          Object editor=editorType.getMethod("current").invoke(null);
          for(String name:List.of("pos1","pos2"))check(editor.getClass().getField(name).get(editor).toString().equals("-5,"+y+",5"),mode+" "+useFeet+" "+name);
          var commandPose=Class.forName("local.luke.power.commands.util.CommandPosition").getMethod("of",net.minecraft.entity.player.PlayerEntity.class).invoke(null,mc.player);
          check(((Number)call(commandPose,"y")).doubleValue()==HudCoordinates.y(mc.player),"relative command origin");
        }
        active(true);pose(31.25,112.5,35.75,-30,120);
        var reported=ReportedPosition.current(mc.player,0,0,0,0,0);
        check(reported.x()==DetachedCamera.x()&&reported.y()==HudCoordinates.y(mc.player)&&reported.z()==DetachedCamera.z(),"detached origin");
        active(false);
      });
      test("real camera displacement respects Survival collisions and bypasses protected modes",()->{
        FreecamPreviewChecks.scene(mc);config.getClass().getField("collision").set(config,true);config.getClass().getField("drag").set(config,5F);
        Object camera=camera();Method move=Arrays.stream(mc.field_2818.getClass().getDeclaredMethods()).filter(m->m.getName().contains("moveCamera")&&m.getParameterCount()==0).findFirst().orElseThrow();move.setAccessible(true);
        Field time=Arrays.stream(mc.field_2818.getClass().getDeclaredFields()).filter(f->f.getType()==long.class&&f.getName().contains("lastTime")).findFirst().orElseThrow();time.setAccessible(true);
        for(String mode:List.of("SURVIVAL","CREATIVE","SPECTATOR")) {
          active(false);CreativeVehicleChecks.mode(mc,mode);active(true);pose(35.5,109.12+HeadGeometry.EYE_ABOVE_BASE,35.5,0,0);
          camera.getClass().getMethod("clearMovementInput").invoke(camera);
          camera.getClass().getField("velocityY").setDouble(camera,-10);
          time.setLong(mc.field_2818,System.nanoTime()-200_000_000);move.invoke(mc.field_2818);
          double y=HudCoordinates.y(mc.player);check(mode.equals("SURVIVAL")?y>=109&&y<109.01:y<108.9,"collision "+mode+" y="+y);
          check((Boolean)config.getClass().getField("collision").get(config),"collision preference changed");
        }
        active(false);
      });
      test("spectator body stays opaque alongside the translucent detached head",()->{
        CreativeVehicleChecks.mode(mc,"SPECTATOR");active(false);mc.player.method_1340(32.5,112.62,38.5);active(true);pose(32.5,112.62,34.5,0,180);
        for(boolean thirdPerson:new boolean[]{false,true}) {
          mc.options.thirdPerson=thirdPerson;heads.clear();capture=true;GhostHead.render(mc,1);capture=false;
          check(heads.size()==(thirdPerson?2:1),"head count "+thirdPerson+" "+heads.size());
          check(heads.get(0)[3]==1&&heads.get(0)[0]==mc.player.x&&heads.get(0)[2]==mc.player.z,"body alpha or position");
          if(thirdPerson)check(Math.abs(heads.get(1)[3]-.65)<.00001&&heads.get(1)[2]==DetachedCamera.z(),"camera marker alpha or position");
        }
        active(false);heads.clear();capture=true;GhostHead.render(mc,1);capture=false;
        check(heads.size()==1&&heads.get(0)[3]==1,"spectator third-person alpha");
        check(GL11.glGetError()==0,"head GL error");
      });
    } finally {
      capture=false;active(false);AccessRules.restore(mc,rules);mc.player.read(saved);
      Object restored=Catalog.JSON.fromJson(originalConfig,config.getClass());Class.forName("local.luke.power.camera.FreecamConfig").getField("config").set(null,restored);
      mc.options.thirdPerson=third;MenuPreferences.previewFeetY(feet);MenuPreferences.update(v->v.autoApply=auto);mc.setScreen(screen);
    }
    log("RELEASE 220 FAILURES "+failures);
  }
}
