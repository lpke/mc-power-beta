package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;
import java.util.List;
import local.luke.power.config.Setting;
import org.junit.jupiter.api.Test;

class TitleGridDimensionsTest {
  private Setting lines(String... rows) {
    JsonArray grid = new JsonArray();
    for (String row : rows) grid.add(row);
    return new Setting("logo.logo.lines", "logo", "", "", "Lines", "", Setting.Kind.LIST,
        grid, grid, 0, 0, 1, List.of(), false);
  }

  @Test void draggingEitherDimensionThroughPaintedCellsRetainsTheDraftAndRestoresTheControl() {
    Setting lines = lines("   *", " *  ");
    var original = lines.value.deepCopy();
    var dimensions = new TitleGridDimensions(lines);
    for (Setting control : List.of(dimensions.columns, dimensions.rows)) {
      control.slide(0);
      String error = assertDoesNotThrow(() -> dimensions.apply(control));
      assertEquals("Erase occupied tiles before shrinking the grid", error);
      assertEquals(original, lines.value);
      assertEquals(4, dimensions.columns.value.getAsInt());
      assertEquals(2, dimensions.rows.value.getAsInt());
    }
  }

  @Test void everySliderSizeEitherFitsOrLeavesPaintIntact() {
    for (boolean columns : new boolean[]{true, false})
      for (int size = 1; size <= (columns ? 80 : 12); size++) {
        Setting lines = lines("  * ", " *  ");
        var dimensions = new TitleGridDimensions(lines);
        Setting control = columns ? dimensions.columns : dimensions.rows;
        control.slide((size - 1d) / (control.max - 1));
        String error = assertDoesNotThrow(() -> dimensions.apply(control));
        boolean fits = size >= (columns ? 3 : 2);
        assertEquals(fits, error.isEmpty());
        assertEquals(fits ? size : columns ? 4 : 2, control.value.getAsInt());
        assertEquals('*', lines.value.getAsJsonArray().get(0).getAsString().charAt(2));
        assertEquals('*', lines.value.getAsJsonArray().get(1).getAsString().charAt(1));
      }
  }

  @Test void individualDimensionResetCannotClipPaintOutsideTheDefaultSize() {
    Setting lines = lines(" ".repeat(39) + "*", "", "", "", "", "*");
    var original = lines.value.deepCopy();
    var dimensions = new TitleGridDimensions(lines);
    for (Setting control : List.of(dimensions.columns, dimensions.rows)) {
      control.reset();
      assertFalse(assertDoesNotThrow(() -> dimensions.apply(control)).isEmpty());
      assertEquals(original, lines.value);
      assertEquals(40, dimensions.columns.value.getAsInt());
      assertEquals(6, dimensions.rows.value.getAsInt());
    }
  }

  @Test void exactEntryRejectsClippingFractionsAndBoundsBeforeChangingEitherControl() {
    Setting lines = lines("   *", " *  ");
    var original = lines.value.deepCopy();
    var dimensions = new TitleGridDimensions(lines);
    for (Setting control : List.of(dimensions.columns, dimensions.rows)) {
      var old = control.value.deepCopy();
      for (String text : List.of("1", "0", "81", "2.5")) {
        assertThrows(RuntimeException.class, () -> control.parse(text));
        assertEquals(old, control.value);
        assertEquals(original, lines.value);
      }
    }
    dimensions.columns.parse("6");
    assertEquals("", dimensions.apply(dimensions.columns));
    assertEquals("   *  ", lines.value.getAsJsonArray().get(0).getAsString());
    dimensions.rows.parse("3");
    assertEquals("", dimensions.apply(dimensions.rows));
    assertEquals("      ", lines.value.getAsJsonArray().get(2).getAsString());
  }

  @Test void successfulResizeStaysInTheEditorDraftAndUndoRestoresItsDimensions() {
    Setting source = lines(" * ", "   ");
    var draft = new SettingsDraft(List.of(source), value -> true);
    var dimensions = new TitleGridDimensions(draft.get(source.id));
    draft.checkpoint();
    dimensions.columns.parse("2");
    assertEquals("", dimensions.apply(dimensions.columns));
    assertEquals(3, TitleTiles.width(source.value.getAsJsonArray()));
    assertTrue(draft.changed());
    draft.undo(); dimensions.sync();
    assertFalse(draft.changed());
    assertEquals(3, dimensions.columns.value.getAsInt());
    assertEquals(2, dimensions.rows.value.getAsInt());
  }
}
