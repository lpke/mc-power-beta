package local.luke.power.map;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.Gson;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class MapToggleTest {
  @Test void allQuickSettingsHaveDistinctStableIdsAndPersistWithoutChangingOtherPreferences() {
    assertEquals(java.util.List.of("ITEMS","MOBS","WAYPOINTS","WORLD_WAYPOINTS","CAVES","HEADS","CHUNKS","SLIME","LIGHT","PATH"),MapToggle.ALL.stream().map(Enum::name).toList());
    var gson=new Gson();
    var fields=new HashSet<String>();
    for(var toggle:MapToggle.ALL) {
      assertTrue(fields.addAll(toggle.fields));
      var settings=new MapSettings();settings.zoom=2.5;
      boolean before=toggle.enabled(settings,false);
      MapToggle.apply(settings,toggle.changes(settings));
      assertEquals(!before,toggle.enabled(settings,false));
      assertEquals(2.5,settings.zoom);
      var restored=gson.fromJson(gson.toJson(settings),MapSettings.class);
      assertEquals(!before,toggle.enabled(restored,false));
      assertDoesNotThrow(restored::validate);
    }
  }
  @Test void partialMobFilterEnablesBothAndCaveToggleRetainsManualMode() {
    var settings=new MapSettings();settings.hostile=true;settings.animals=false;
    MapToggle.apply(settings,MapToggle.MOBS.changes(settings));
    assertTrue(settings.hostile&&settings.animals);
    MapToggle.apply(settings,MapToggle.MOBS.changes(settings));
    assertFalse(settings.hostile||settings.animals);
    settings.caveMode=2;
    MapToggle.apply(settings,MapToggle.CAVES.changes(settings));assertEquals(0,settings.caveMode);
    MapToggle.apply(settings,MapToggle.CAVES.changes(settings));assertEquals(2,settings.caveMode);
  }
  @Test void boundedScrollingReachesEveryRowAndRejectsHiddenRows() {
    for(int available:new int[]{88,128,168,208}) {
      var layout=MapTogglePanel.at(8,220,184,available,150,0);
      var reached=new HashSet<Integer>();
      for(int wheel=0;wheel<20;wheel++) {
        for(int i=layout.first();i<layout.first()+layout.visible();i++) {
          var row=layout.row(i);reached.add(i);
          assertEquals(i,layout.rowAt(row.x(),row.y()));
          assertEquals(i,layout.rowAt(row.x()+row.width()-1,row.y()+row.height()-1));
        }
        assertEquals(-1,layout.rowAt(layout.bounds().x(),layout.bounds().y()));
        assertEquals(-1,layout.rowAt(20,layout.bounds().y()+layout.bounds().height()));
        layout=MapTogglePanel.at(8,220,184,available,150,layout.scroll(-120));
      }
      assertEquals(10,reached.size());
      assertEquals(10-layout.visible(),layout.first());
      for(int wheel=0;wheel<20;wheel++)layout=MapTogglePanel.at(8,220,184,available,150,layout.scroll(120));
      assertEquals(0,layout.first());
    }
  }
}
