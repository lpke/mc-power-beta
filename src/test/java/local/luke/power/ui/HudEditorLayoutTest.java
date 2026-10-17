package local.luke.power.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HudEditorLayoutTest {
  @Test void forestCaptureIsSharpAndFitsTheNativeTextureUploadBuffer() throws Exception {
    try (var in = getClass().getResourceAsStream(HudPreviewBackground.TEXTURE)) {
      assertNotNull(in);
      var image = javax.imageio.ImageIO.read(in);
      assertEquals(2560, image.getWidth()); assertEquals(1440, image.getHeight());
      assertTrue((long)image.getWidth() * image.getHeight() * 4 <= 16 * 1024 * 1024);
    }
  }
  @Test void commonSizesKeepControlsReadableAndPreviewCoordinatesInvertible() {
    for (int[] size : new int[][] {{320,240},{480,270},{640,360},{640,420},{800,600},{960,540},
        {1280,720},{1720,720},{1920,540},{540,960},{900,1200}}) {
      int w = size[0], h = size[1];
      for (int split : new int[] {-500, 100, 360, 10000}) {
        var layout = HudEditorLayout.at(w,h,split);
        assertTrue(layout.controlsWidth() >= 280);
        assertTrue(layout.controlsBottom() - layout.controlsTop() >= 47);
        assertTrue(layout.previewWidth() >= 280);
        assertTrue(layout.previewBottom() - layout.previewTop() >= 48);
        assertTrue(layout.previewLeft() + layout.previewWidth() <= w - 10);
        if (layout.horizontal()) assertTrue(layout.controlsLeft() + layout.controlsWidth() < layout.previewLeft());
        else assertTrue(layout.controlsBottom() < layout.previewTop());
        double scale = layout.scale(w,h), x = layout.canvasX(w,h), y = layout.canvasY(w,h);
        assertTrue(scale > 0);
        assertTrue(x >= layout.previewLeft() && y >= layout.previewTop());
        assertTrue(x + w*scale <= layout.previewLeft() + layout.previewWidth() + .001);
        assertTrue(y + h*scale <= layout.previewBottom() + .001);
        assertEquals(w*.73, (x + w*.73*scale - x)/scale, .0001);
        assertEquals(h*.61, (y + h*.61*scale - y)/scale, .0001);
      }
    }
    assertFalse(HudEditorLayout.horizontal(320,240));
    assertFalse(HudEditorLayout.horizontal(540,960));
    assertFalse(HudEditorLayout.horizontal(900,1200));
    assertTrue(HudEditorLayout.horizontal(800,600));
    assertTrue(HudEditorLayout.horizontal(1720,720));
  }
  @Test void bothDividerOrientationsStayBoundedAndRetainTheirIndependentPositions() {
    EditorSplit split = new EditorSplit(); split.start(150,55,367);split.stop();int vertical = split.bottom(55,367);
    for (int x : new int[] {-100,300,2000}) {
      split.startSide(x,10,1280);assertTrue(split.sideways());assertTrue(split.sideWidth() >= 280 && split.sideWidth() <= 440);split.stop();
      assertEquals(vertical,split.bottom(55,367));
    }
  }
}
