package local.luke.power.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudEditorToolbarTest {
  @Test void wrapsWithoutLosingControlsOrPreviewSpace() {
    for(int[] size:new int[][]{{320,240},{640,420},{960,540},{1720,720},{540,960}}) {
      var bar=HudEditorToolbar.at(size[0],s->s.length()*6);
      assertEquals(6,bar.toggles().size());
      for(var toggle:bar.toggles()) {assertTrue(toggle.x()>=10);assertTrue(toggle.x()+toggle.width()<=size[0]-10);}
      var geometry=HudEditorLayout.at(size[0],size[1],size[1]/2,bar.controlsTop());
      assertTrue(geometry.controlsBottom()-geometry.controlsTop()>=23);
      assertTrue(geometry.previewBottom()-geometry.previewTop()>=48);
    }
  }
}
