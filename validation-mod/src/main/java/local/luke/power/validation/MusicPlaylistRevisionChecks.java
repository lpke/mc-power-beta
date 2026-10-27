package local.luke.power.validation;

import static local.luke.power.validation.UiChecks.*;
import static local.luke.power.validation.Validation.*;

import java.util.*;
import local.luke.power.audio.*;
import local.luke.power.config.Catalog;
import local.luke.power.ui.*;
import local.luke.power.validation.mixin.ScreenInput;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

/** Native regression checks for flat order, nested Add and the reserved Favorites draft. */
final class MusicPlaylistRevisionChecks {
  private static final String FIRST="music:calm1.ogg", SECOND="music:calm2.ogg", MISSING="music:playlist-fixture-missing.ogg";

  static void run(Minecraft mc) throws Exception {
    for(String view:List.of("grouped-scrolled","add-library-scrolled"))
      test("MUSIC460 scrolled small song controls stay visible without a sticky heading " + view, () -> {
        var options=MusicPlaylists460Checks.stage(mc,view,320,240);var library=(MusicLibraryScreen)field(options,"library");
        int top=(int)call(library,"listTop"),bottom=(int)call(library,"bottom");
        check(bottom-top<48,"small fixture no longer exercises sticky-header height guard");
        check(call(library,"stickyHeader")==null,"small song viewport pins a heading over its song");
        Object row=((List<?>)field(library,"rows")).stream().filter(r->{try{return FIRST.equals(call(r,"id"));}catch(Exception e){throw new RuntimeException(e);}}).findFirst().orElseThrow();
        int y=(int)call(library,"listTop")+(int)call(row,"y")-(int)call(library,"scroll");
        check(y>=top && y+24<=bottom,"scrolled song row is not fully visible");
        var geometry=(MusicRowLayout)call(library,"rowLayout");
        check(geometry.playX()>=(int)call(library,"left") && geometry.playX()+geometry.buttonWidth()<=(int)call(library,"right") && y+3>=top && y+21<=bottom,"small row Play bounds leave visible list");
        var input=(ScreenInput)(Object)options;input.power$click(geometry.volumeX()+8,y+8,1);
        check((boolean)call(field(options,"valueEditor"),"active"),"small visible volume control missed exact entry");
        input.power$key('\0',Keyboard.KEY_ESCAPE);
        check(!(boolean)call(field(options,"valueEditor"),"active") && mc.currentScreen==options && (boolean)call(options,"libraryVisible"),"small exact entry Escape navigated away");
        check(((MusicPresetDraft)call(field(library,"presets"),"draft")).id().equals("quiet"),"small exact entry changed playlist draft");
        options.session().discard();
      });
    test("MUSIC460 playlist header controls fit at the responsive row boundary", () -> {
      AudioConfig.preview(MusicPlaylists460Checks.seed());var options=MusicPlaylists460Checks.open(mc);
      var library=MusicPlaylists460Checks.library(options);MusicPlaylists460Checks.edit(options,library,"quiet");
      var panel=field(library,"presets");int low=320,high=854;
      while(low<high) {
        int width=(low+high)/2;options.init(mc,width,420);
        if((int)call(panel,"headerHeight")==22)high=width;else low=width+1;
      }
      int boundary=low;
      for(int width:new int[]{320,boundary-1,boundary,boundary+1,854}) {
        options.init(mc,width,420);options.render(-1,-1,0);
        int left=(int)call(panel,"left"),right=(int)call(panel,"right"),top=(int)call(panel,"top");
        int h=(int)call(panel,"headerHeight");
        int[][] bounds={
          {(int)call(panel,"nameX"),top,(int)call(panel,"nameWidth"),18},
          {(int)call(panel,"addX"),(int)call(panel,"actionY"),(int)call(panel,"addWidth"),18},
          {(int)call(panel,"groupedX"),(int)call(panel,"actionY"),(int)call(panel,"viewWidth"),18}
        };
        for(int i=0;i<bounds.length;i++) {
          int[] a=bounds[i];check(a[0]>=left && a[0]+a[2]<=right && a[1]>=top && a[1]+a[3]<=top+h,"header control clips at width " + width);
          for(int j=0;j<i;j++) {int[] b=bounds[j];check(a[0]+a[2]<=b[0] || b[0]+b[2]<=a[0] || a[1]+a[3]<=b[1] || b[1]+b[3]<=a[1],"header controls overlap at width " + width);}
        }
        check(h==(width<boundary?44:22),"header changes rows inconsistently around its boundary");
      }
      options.session().discard();
    });
    test("MUSIC460 song controls reserve right-click exact volume and expose ordered parent actions", () -> {
      MenuPreferences.update(v->v.autoApply=false);AudioConfig.preview(MusicPlaylists460Checks.seed());
      var options=MusicPlaylists460Checks.open(mc);var library=MusicPlaylists460Checks.library(options);
      library.showTrack(FIRST);int y=MusicPlaylists460Checks.trackY(library,FIRST);var row=(MusicRowLayout)call(library,"rowLayout");
      var input=(ScreenInput)(Object)options;
      input.power$click(row.volumeX()+8,y+8,1);
      check((boolean)call(field(options,"valueEditor"),"active") && !(boolean)call(field(options,"choices"),"active"),"volume right click opened song context instead of exact entry");
      input.power$key('\0',Keyboard.KEY_ESCAPE);
      var queue=MusicRequests.tracks();
      MusicPlaylists460Checks.context(options,library,FIRST,List.of("Add to queue","Remove favourite","Preview"));
      MusicPlaylists460Checks.choose(options,0);
      var expected=new ArrayList<>(queue);expected.add(FIRST);
      check(MusicRequests.tracks().equals(expected),"context Add to queue missed");
      library.showQueue();
      MusicPlaylists460Checks.context(options,library,FIRST,List.of("Add to queue","Remove favourite","Preview","Remove from queue"));
      MusicPlaylists460Checks.choose(options,3);
      check(MusicRequests.tracks().size()==queue.size(),"context Remove from queue missed one request");
      MusicRequests.edit(q->{q.tracks.clear();q.tracks.addAll(queue);});options.session().discard();
    });
    test("MUSIC460 queue-only missing IDs retain volume controls and remove the selected duplicate occurrence", () -> {
      String missing="music:queue-only-missing-fixture.ogg";
      AudioConfig.preview(MusicPlaylists460Checks.seed());var originalQueue=MusicRequests.tracks();
      MusicRequests.edit(q->{q.tracks.clear();q.tracks.addAll(List.of(missing,FIRST,missing,SECOND));});
      var options=MusicPlaylists460Checks.open(mc);var library=MusicPlaylists460Checks.library(options);library.showQueue();
      var input=(ScreenInput)(Object)options;var geometry=(MusicRowLayout)call(library,"rowLayout");
      int y=MusicPlaylists460Checks.trackY(library,missing);
      input.power$click(geometry.volumeX()+8,y+8,1);
      check((boolean)call(field(options,"valueEditor"),"active"),"queue-only missing ID lacks exact volume control");
      ((TextInput)field(field(options,"valueEditor"),"input")).setText("67");input.power$key('\0',Keyboard.KEY_RETURN);
      check(AudioConfig.current().sounds.get(missing)==67,"missing queue volume did not commit globally");
      options.render(-1,-1,0);
      Object occurrence=((List<?>)field(library,"rows")).stream().filter(r->{try{return (int)call(r,"index")==2;}catch(Exception e){throw new RuntimeException(e);}}).findFirst().orElseThrow();
      int rowY=(int)call(occurrence,"y");Dropdown450Screens.invoke(library,"scroll",new Class<?>[]{int.class},rowY);
      y=(int)call(library,"listTop")+rowY-(int)call(library,"scroll");
      input.power$click((int)call(library,"left")+22,y+8,1);
      var choices=field(options,"choices");
      check(field(choices,"labels").equals(List.of("Add to queue","Favourite","Preview","Remove from queue")),"missing queue context order changed");
      check(field(choices,"enabled").equals(List.of(true,true,false,true)),"missing track preview is enabled");
      MusicPlaylists460Checks.choose(options,3);
      check(MusicRequests.tracks().equals(List.of(missing,FIRST,SECOND)),"Remove from queue removed the first matching ID instead of selected occurrence: " + MusicRequests.tracks());
      options.session().discard();MusicRequests.edit(q->{q.tracks.clear();q.tracks.addAll(originalQueue);});
    });
    test("MUSIC460 From playlist appends only absent IDs in source order and retains Library gains", () -> {
      MenuPreferences.update(v->v.autoApply=false);AudioConfig.preview(MusicPlaylists460Checks.seed());
      var options=MusicPlaylists460Checks.open(mc);var library=MusicPlaylists460Checks.library(options);
      MusicPlaylists460Checks.edit(options,library,"building");var panel=field(library,"presets");
      var draft=(MusicPresetDraft)call(panel,"draft");var initial=draft.tracks();var gains=new TreeMap<>(AudioConfig.current().libraryGroupVolumes);
      String saved=Catalog.JSON.toJson(AudioConfig.current().playlists);
      click(options,(int)call(panel,"addX")+5,(int)call(panel,"actionY")+5);MusicPlaylists460Checks.choose(options,1);
      var choices=field(panel,"choices");var labels=(List<?>)field(choices,"labels");
      check(labels.contains("Beta") && labels.contains("Favorites") && !labels.contains("Building"),"From playlist source list omitted reserved or included destination");
      MusicPlaylists460Checks.choose(options,labels.indexOf("Quiet afternoons"));
      var expected=new ArrayList<>(initial);expected.addAll(List.of(SECOND,FIRST,MISSING));
      check(draft.tracks().equals(expected),"From playlist changed append order or dropped missing ID");
      check(AudioConfig.current().libraryGroupVolumes.equals(gains),"From playlist changed Library gains");
      click(options,(int)call(panel,"addX")+5,(int)call(panel,"actionY")+5);MusicPlaylists460Checks.choose(options,1);
      MusicPlaylists460Checks.choose(options,((List<?>)field(choices,"labels")).indexOf("Quiet afternoons"));
      check(draft.tracks().equals(expected),"repeated From playlist duplicated tracks");
      check(saved.equals(Catalog.JSON.toJson(AudioConfig.current().playlists)),"From playlist wrote saved playlist before Save");
      options.session().discard();
    });
    test("MUSIC460 Add group appends matching songs and filtered List retains full row indices without dragging", () -> {
      AudioConfig.preview(MusicPlaylists460Checks.seed());var options=MusicPlaylists460Checks.open(mc);
      var library=MusicPlaylists460Checks.library(options);MusicPlaylists460Checks.edit(options,library,"quiet");
      var panel=field(library,"presets");var draft=(MusicPresetDraft)call(panel,"draft");var initial=draft.tracks();
      MusicPlaylists460Checks.adding(options,library);
      int y=MusicGroupChecks.groupY(library,"Beta / Creative");
      click(options,(int)call(library,"right")-35,y+8);
      var expected=new ArrayList<>(initial);BuiltinMusic.TRACKS.stream().filter(t->t.group().equals("Beta / Creative"))
          .map(BuiltinMusic.Track::id).sorted(Comparator.comparing(AudioController::musicLabel,String.CASE_INSENSITIVE_ORDER).thenComparing(Comparator.naturalOrder()))
          .forEach(expected::add);
      check(draft.tracks().equals(expected),"group Add reordered existing IDs or missed its songs");
      check(AudioConfig.current().libraryGroupVolumes.getOrDefault("Alpha",100)==40,"group Add changed Library gain");
      click(options,(int)call(panel,"addX")+5,(int)call(panel,"actionY")+5);
      ((TextInput)field(library,"search")).setText("calm");call(library,"rebuild");
      for(Object row:(List<?>)field(library,"rows")) {
        String id=(String)call(row,"id");
        if(id!=null)check((int)call(row,"index")==expected.indexOf(id),"filtered row lost full playlist index");
      }
      y=MusicPlaylists460Checks.trackY(library,FIRST);int x=(int)call(library,"left")+22;
      ((ScreenInput)(Object)options).power$click(x,y+8,0);
      check(!(boolean)call(field(library,"reorder"),"active"),"filtered playlist enabled drag");
      ((ScreenInput)(Object)options).power$release(x,y+24,0);
      check(draft.tracks().equals(expected),"filtered pointer changed full order");options.session().discard();
    });
    test("MUSIC460 Favorites uses an isolated fixed-name draft with saved playback membership", () -> {
      var settings=MusicPlaylists460Checks.seed();MusicPlaylist.load(settings,MusicPlaylist.FAVORITES);
      MusicPlaylist.loadMenu(settings,MusicPlaylist.FAVORITES);AudioConfig.preview(settings);
      var options=MusicPlaylists460Checks.open(mc);var library=MusicPlaylists460Checks.library(options);
      MusicPlaylists460Checks.tab(options,library,"favorites");var panel=field(library,"presets");
      var draft=(MusicPresetDraft)call(panel,"draft");
      check(draft.favorites() && draft.name().equals("Favorites") && draft.tracks().equals(List.of(FIRST)),"Favorites draft has wrong identity or membership");
      ((TextInput)field(panel,"name")).setText("Forbidden rename");
      check(draft.snapshot("Forbidden rename").name().equals("Favorites"),"Favorites accepted rename");
      MusicPlaylists460Checks.context(options,library,FIRST,List.of("Add to queue","Remove favourite","Preview"));
      MusicPlaylists460Checks.choose(options,1);
      check(draft.tracks().isEmpty() && AudioConfig.current().favourites.contains(FIRST),"Favorites context bypassed draft");
      check(new ArrayList<>(AudioController.activeMusic(mc)).equals(List.of(FIRST)),"unsaved Favorites changed playback");
      MusicPlaylists460Checks.adding(options,library);
      MusicPlaylists460Checks.context(options,library,SECOND,List.of("Add to queue","Favourite","Preview","Add to playlist"));
      MusicPlaylists460Checks.choose(options,3);
      click(options,(int)call(panel,"addX")+5,(int)call(panel,"actionY")+5);
      AudioUi461Checks.savePlaylist(options);
      check(AudioConfig.current().favourites.equals(new LinkedHashSet<>(List.of(SECOND))),"Favorites Save lost draft membership");
      check(AudioConfig.current().playlist.equals(MusicPlaylist.FAVORITES) && AudioConfig.current().menuPlaylist.equals(MusicPlaylist.FAVORITES),"Favorites Save changed selection IDs");
      check(new ArrayList<>(AudioController.activeMusic(mc)).equals(List.of(SECOND)),"saved Favorites did not update playback");
      options.session().discard();check(AudioConfig.current().favourites.contains(FIRST),"Options Discard lost saved Favorites snapshot");
    });
    test("MUSIC460 Library group drag saves separate order without changing imported paths or playlist data", () -> {
      AudioConfig.preview(MusicPlaylists460Checks.seed());var options=MusicPlaylists460Checks.open(mc);
      var library=MusicPlaylists460Checks.library(options);MusicPlaylists460Checks.tab(options,library,"library");
      if(!library.sectionsCollapsed())library.toggleSections();
      var directories=List.copyOf(AudioConfig.current().musicDirectories);String playlists=Catalog.JSON.toJson(AudioConfig.current().playlists);
      var headings=((List<?>)field(library,"rows")).stream().filter(r->{try{return call(r,"id")==null;}catch(Exception e){throw new RuntimeException(e);}}).toList();
      check(headings.size()>1,"group reorder needs multiple headings");
      String first=(String)call(headings.get(0),"group"),second=(String)call(headings.get(1),"group");
      Dropdown450Screens.invoke(library,"scroll",new Class<?>[]{int.class},0);
      int top=(int)call(library,"listTop"),y=top+(int)call(headings.get(0),"y"),secondY=top+(int)call(headings.get(1),"y"),x=(int)call(library,"left")+22;
      ((ScreenInput)(Object)options).power$click(x,y+8,0);((ScreenInput)(Object)options).power$release(x,secondY+22,0);
      check(AudioConfig.current().libraryOrder.indexOf(second)<AudioConfig.current().libraryOrder.indexOf(first),"group drag did not save display order: " + AudioConfig.current().libraryOrder);
      check(AudioConfig.current().musicDirectories.equals(directories),"group drag changed source paths");
      check(Catalog.JSON.toJson(AudioConfig.current().playlists).equals(playlists),"group drag edited playlist order");
      options.session().discard();
    });
  }
}
