package local.luke.power.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EditorSplitTest {
  @Test void dragRetainsAtLeastOneControlAndThePreviewAtEverySupportedSize() {
    for (int height : new int[]{240, 300, 420, 600}) {
      EditorSplit split = new EditorSplit(); int top = 80, bottom = height - 53;
      for (int pointer : new int[]{-100, 0, 120, 300, 10000}) {
        split.start(pointer, top, bottom);
        assertTrue(split.dragging()); assertTrue(split.bottom(top, bottom) >= top + 41);
        assertTrue(bottom - split.bottom(top, bottom) - 12 >= 48);
        split.stop(); assertFalse(split.dragging());
      }
    }
  }
  @Test void resizingRetainsTheSplitProportionWithoutSavingASetting() {
    EditorSplit split = new EditorSplit(); split.start(220, 80, 547); split.stop();
    int old = split.bottom(80, 547), small = split.bottom(80, 367);
    assertTrue(small < old); assertEquals(old, split.bottom(80, 547));
  }
}
