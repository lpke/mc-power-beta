package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ColorPickerSnapshotTest {
  @Test void rgbOpacityAndSwatchesShareOneDirtyBoundaryAndRevertingClearsIt() {
    var swatches = new ArrayList<>(List.of("#123456"));
    var original = new ColorPickerSnapshot(0xabcdef, 80, swatches);
    assertFalse(original.changed(0xabcdef, 80, swatches, false));
    assertTrue(original.changed(3, 80, swatches, false));
    assertTrue(original.changed(0xabcdef, 12.5, swatches, false));
    swatches.add("#000003"); assertTrue(original.changed(0xabcdef, 80, swatches, false));
    swatches.remove(1); assertFalse(original.changed(0xabcdef, 80, swatches, false));
    assertTrue(original.changed(0xabcdef, 80, swatches, true));
    assertEquals(List.of("#123456"), original.swatches());
  }
}
