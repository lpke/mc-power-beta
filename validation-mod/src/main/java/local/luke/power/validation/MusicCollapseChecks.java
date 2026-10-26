package local.luke.power.validation;

import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.Validation.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.ui.*;

final class MusicCollapseChecks {
  static void run(net.minecraft.client.Minecraft mc) throws Exception {
    failures=0;var before=AudioConfig.copy();var parent=mc.currentScreen;
    PowerOptionsScreen screen=null;
    try {
      var settings=new AudioSettings();settings.musicMode=AudioSettings.MusicMode.ALL_MINECRAFT;
      settings.favourites=new TreeSet<>(Set.of("music:calm1.ogg","music:creative1.ogg"));
      settings.playlists.add(new MusicPlaylist("collapse-fixture","Collapse fixture",new ArrayList<>(settings.favourites),Map.of()));
      AudioConfig.preview(settings);
      screen=MenuUpdateChecks.open(mc,"Audio");field(screen,"libraryOpen",true);call(screen,"layout");
      var options=screen;var library=(MusicLibraryScreen)field(screen,"library");
      var select=MusicLibraryScreen.class.getDeclaredMethod("selectFilter",String.class);select.setAccessible(true);
      byte[] config=Files.readAllBytes(local.luke.power.storage.PowerConfig.path());
      String audio=local.luke.power.config.Catalog.JSON.toJson(AudioConfig.current());
      for(String filter:List.of("active","","favourites","presets"))
        for(int[] size:new int[][]{{320,240},{854,480}})
          test("music collapse overrides individual groups only in current view "+filter+" "+size[0],()->{
            library.showTracks();options.init(mc,size[0],size[1]);
            field(library,"hideExcluded",false);((TextInput)field(library,"search")).setText("");
            select.invoke(library,filter);
            if(filter.equals("presets")){field(library,"selectedPreset","collapse-fixture");call(library,"rebuild");}
            check(library.hasSections(),"filter has no sections");
            var groups=List.copyOf((List<String>)field(library,"sectionKeys"));
            var collapsed=(Set<String>)field(library,"collapsed");collapsed.clear();collapsed.add("unrelated-group");
            if(groups.size()>1)collapsed.add(groups.get(0));
            call(library,"rebuildRows");options.render(-1,-1,0);
            click(options,(int)call(options,"sectionButtonX")+8,35);
            check(library.sectionsCollapsed(),"mixed groups not all collapsed");
            check(((List<?>)field(library,"rows")).stream().allMatch(row->{try{return call(row,"id")==null;}catch(Exception e){throw new RuntimeException(e);}}),"tracks still visible");
            click(options,(int)call(options,"sectionButtonX")+8,35);
            check(!library.sectionsCollapsed()&&collapsed.equals(Set.of("unrelated-group")),"expand changed unrelated group");
            check(((List<?>)field(library,"rows")).size()>groups.size(),"tracks not restored");
          });
      test("music search supports collapse and clearing keeps ordinary group state",()->{
        select.invoke(library,"");var query=(TextInput)field(library,"search");query.setText("calm");call(library,"rebuild");
        check(library.hasSections(),"missing search groups");
        library.toggleSections();check(library.sectionsCollapsed(),"search collapse ignored");
        check(query.text().equals("calm"),"query changed");
        library.toggleSections();check(!library.sectionsCollapsed(),"search expand ignored");
        library.clearQuery();check(((Set<?>)field(library,"collapsed")).contains("unrelated-group"),"normal state lost");
      });
      test("queue has no groups and collapse never changes playback or settings",()->{
        library.showQueue();check(!library.hasSections(),"queue has collapse control");
        library.toggleSections();check(Arrays.equals(config,Files.readAllBytes(local.luke.power.storage.PowerConfig.path())),"view saved config");
        check(audio.equals(local.luke.power.config.Catalog.JSON.toJson(AudioConfig.current())),"view changed audio");
      });
    } finally {if(screen!=null)screen.session().discard();AudioConfig.preview(before);mc.setScreen(parent);}
    log("MUSIC COLLAPSE FAILURES "+failures);
  }
}
