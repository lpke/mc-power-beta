package local.luke.power.validation;

import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.Validation.*;

import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.ui.*;
import net.minecraft.client.Minecraft;

final class StickyGroupChecks {
  private static boolean group(MusicLibraryScreen library, String name) throws Exception {
    for (Object row : (List<?>)field(library,"rows"))
      if (call(row,"id") == null && name.equals(call(row,"group"))) return true;
    return false;
  }

  static void run(Minecraft mc) throws Exception {
    failures = 0;
    var before = AudioConfig.copy();
    var queueBefore = MusicRequests.tracks();
    boolean auto = MenuPreferences.current().autoApply;
    MenuPreferences.update(v -> v.autoApply = false);
    try {
      var settings = new AudioSettings();
      settings.musicMode = AudioSettings.MusicMode.ALL_MINECRAFT;
      AudioConfig.preview(settings);
      var options = MenuUpdateChecks.open(mc,"General");
      options.init(mc,640,420);
      test("pinned settings heading consumes clicks without changing covered settings", () -> {
        field(options,"scroll",30d);
        var sticky = (StickyHeader)call(options,"stickyHeader");
        check(sticky != null,"no settings heading");
        String name = (String)call(((List<?>)field(options,"rows")).get(sticky.index()),"group");
        long changes = options.session().changes();
        click(options,(int)call(options,"right")-80,(int)call(options,"top")+8);
        check(((Set<?>)field(options,"collapsed")).contains("General/"+name),"pinned collapse missed");
        check(options.session().changes()==changes,"heading clicked a covered setting");
      });
      test("search headings keep their section-navigation action when pinned", () -> {
        field(options,"scroll",0d);search(options,"volume");
        field(options,"scroll",30d);
        var sticky=(StickyHeader)call(options,"stickyHeader");
        check(sticky != null,"no search header");
        click(options,(int)call(options,"left")+10,(int)call(options,"top")+8);
        check(((TextInput)field(options,"search")).text().isEmpty(),"pinned search did not open section");
      });
      field(options,"page","Audio");field(options,"libraryOpen",true);
      var library = (MusicLibraryScreen)field(options,"library");
      library.restore(new MusicLibraryScreen.State(false,"","",0,0));
      library.showTrack("music:calm1.ogg");
      test("pinned music volume and inclusion target the group, not covered tracks", () -> {
        field(library,"trackScroll",30);
        var sticky=(StickyHeader)call(library,"stickyHeader");
        check(sticky != null,"no music sticky header");
        var volumes=new TreeMap<>(AudioConfig.current().sounds);
        int x=(int)call(library,"groupVolumeX"),w=(int)call(library,"groupVolumeWidth");
        click(options,x+w/2,sticky.y()+8);options.render(-1,-1,0);
        check(Math.abs(AudioConfig.current().groupVolumes.get("Alpha")-50)<=1,"pinned group slider missed");
        check(AudioConfig.current().sounds.equals(volumes),"group changed track volume");
        click(options,(int)call(library,"right")-10,sticky.y()+8);
        check(AudioConfig.current().disabledTracks.contains("music:calm1.ogg"),"pinned include missed");
        check(group(library,"Alpha"),"excluded group hidden by default");
      });
      test("library Hide excluded changes visibility only and survives view restore", () -> {
        var excluded=Set.copyOf(AudioConfig.current().disabledTracks);
        click(options,(int)call(library,"hideX")+5,(int)call(library,"hideY")+8);
        check(!group(library,"Alpha"),"excluded group remains visible");
        check(AudioConfig.current().disabledTracks.equals(excluded),"visibility changed rotation");
        var state=library.state();library.restore(state);call(library,"rebuild");
        check(state.hideExcluded()&&!group(library,"Alpha"),"visibility choice lost");
        click(options,(int)call(library,"hideX")+5,(int)call(library,"hideY")+8);
        check(group(library,"Alpha"),"Show excluded missed group");
        check(AudioConfig.current().disabledTracks.equals(excluded),"Show changed rotation");
      });
      test("library filter and bulk controls never overlap at supported sizes", () -> {
        for(int[] size:new int[][]{{320,240},{427,240},{640,420},{854,480}}) {
          options.init(mc,size[0],size[1]);options.render(-1,-1,0);
          int hy=(int)call(library,"hideY"),hx=(int)call(library,"hideX");
          int by=(int)call(library,"bulkTop"),ix=(int)call(library,"includeX");
          check(hx>=(int)call(library,"left"),"Hide clipped");
          if(hy==by)check(hx+102<ix,"Hide overlaps Include all");
          var tabs=(MusicFilters.Layout)call(library,"filters");
          for(var tab:tabs.tabs()) if(tab.y()==hy) check(tab.x()+tab.width()<hx,"Hide overlaps tabs");
          check((int)call(library,"listTop")>=by+18,"rows overlap bulk controls");
          check(org.lwjgl.opengl.GL11.glGetError()==0,"GL error");
        }
        mc.setScreen(options);
      });
      test("empty queue hides its shortcut while Back still closes the queue", () -> {
        MusicRequests.edit(q -> { q.tracks.clear(); q.add("music:calm1.ogg"); });
        musicClick(options, AudioToolbar.Action.QUEUE);
        check(library.queueVisible(), "Queue did not open");
        MusicRequests.edit(q -> q.tracks.clear());
        check(((List<AudioToolbar.Button>)call(options,"musicButtons")).stream()
            .noneMatch(v -> v.action()==AudioToolbar.Action.QUEUE), "empty Queue shortcut visible");
        click(options,(int)call(options,"left")+10,35);
        check(!(boolean)call(options,"libraryVisible"), "empty queue trapped navigation");
      });
      options.session().discard();
    } finally {
      MusicRequests.edit(q -> { q.tracks.clear(); q.tracks.addAll(queueBefore); });
      AudioConfig.preview(before);
      MenuPreferences.update(v -> v.autoApply=auto);
      mc.setScreen(null);
    }
    log("STICKY GROUP FAILURES "+failures);
  }
}
