package local.luke.power.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudEditorToolbarTest {
  @Test void wrapsWithoutLosingControlsOrPreviewSpace() {
    for(int[] size:new int[][]{{320,240},{640,420},{960,540},{1720,720},{540,960}}) {
      int selector = HudEditorLayout.horizontal(size[0], size[1]) ? 360 : size[0] - 20;
      var bar=HudEditorToolbar.at(size[0],selector,s->s.length()*6,HudEditorLayout.compact(size[0],size[1]));
      assertEquals(6,bar.toggles().size());
      for(var toggle:bar.toggles()) {assertTrue(toggle.x()>=10);assertTrue(toggle.x()+toggle.width()<=size[0]-10);}
      var geometry=HudEditorLayout.at(size[0],size[1],size[1]/2,bar.controlsTop());
      assertTrue(geometry.controlsBottom()-geometry.controlsTop()>=23);
      assertTrue(geometry.previewBottom()-geometry.previewTop()>=48);
    }
  }
  @Test void wideScreensKeepFullLabelsAndSmallerScreensCompactBeforeWrapping() {
    var wide = HudEditorToolbar.at(1400,360,s->s.length()*6);
    assertEquals(55,wide.controlsTop());
    assertEquals(382,wide.labelX()); assertEquals(35,wide.labelY());
    assertTrue(wide.toggles().stream().allMatch(t -> t.y()==30));
    assertFalse(wide.compact()); assertEquals(360,wide.selectorWidth());
    var narrow = HudEditorToolbar.at(640,328,s->s.length()*6);
    assertTrue(narrow.compact()); assertEquals(-1,narrow.labelX());
    assertEquals(55,narrow.controlsTop());
    assertTrue(narrow.toggles().stream().allMatch(t -> t.y()==30));
  }
  @Test void compactControlsKeepEveryToggleOnOneRowAndCanExpandThePreview() {
    for (int[] size : new int[][]{{320,240},{480,270},{640,360},{640,420},{540,960}}) {
      assertTrue(HudEditorLayout.compact(size[0],size[1]));
      var bar=HudEditorToolbar.at(size[0],size[0]-42,s->s.length()*6,true);
      boolean inline = size[0]>=480;
      assertEquals(6,bar.toggles().size()); assertEquals(inline?55:79,bar.controlsTop());
      for (var toggle : bar.toggles()) {
        assertEquals(inline?30:54,toggle.y()); assertTrue(toggle.x()>=10);
        assertTrue(toggle.x()+toggle.width()<=size[0]-(inline?54:10));
        if(inline) assertTrue(toggle.x()>=10+bar.selectorWidth()+12);
      }
      var expanded=HudEditorLayout.at(size[0],size[1],size[1]/2,bar.controlsTop(),false);
      var collapsed=HudEditorLayout.at(size[0],size[1],size[1]/2,bar.controlsTop(),true);
      assertEquals(bar.controlsTop(),collapsed.previewTop());
      assertEquals(collapsed.controlsTop(),collapsed.controlsBottom());
      assertTrue(collapsed.previewWidth()*(collapsed.previewBottom()-collapsed.previewTop()) > expanded.previewWidth()*(expanded.previewBottom()-expanded.previewTop()));
    }
    for (int[] size : new int[][]{{800,600},{960,540},{1280,720},{1720,720},{900,1200}}) {
      assertFalse(HudEditorLayout.compact(size[0],size[1]));
      assertEquals(HudEditorLayout.at(size[0],size[1],360,55),HudEditorLayout.at(size[0],size[1],360,55,true));
    }
  }
  @Test void hidingPreviewGivesSettingsAllAvailableSpaceWithoutChangingTheSplit() {
    for (int[] size : new int[][]{{320,240},{640,360},{540,960}}) {
      var hidden=HudEditorLayout.at(size[0],size[1],150,55,false,true,size[1]-35);
      assertEquals(size[1]-35,hidden.controlsBottom());
      assertEquals(size[0]-20,hidden.controlsWidth());
      assertEquals(hidden.previewTop(),hidden.previewBottom());
      var restored=HudEditorLayout.at(size[0],size[1],150,55,false,false,size[1]-35);
      assertTrue(restored.previewBottom()>restored.previewTop());
    }
  }
}
