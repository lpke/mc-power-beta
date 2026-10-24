package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

class TextInputTest {
  @AfterEach void resetPreference() { TextEditing.rightClickClear(() -> false); TextDrag.cancel(); }

  @Test void rightClickOnlyClearsTheFocusedFieldWhenEnabled() {
    var field = new TextInput("selected search", 128); field.selectAll();
    TextEditing.rightClickClear(() -> true);
    field.click(0, 1);
    assertEquals("selected search", field.text());
    field.focused = true;
    TextEditing.rightClickClear(() -> false); field.click(0, 1);
    assertEquals("selected search", field.text()); assertEquals(0, field.start()); assertEquals(15, field.end());
    TextEditing.rightClickClear(() -> true); field.click(0, 1);
    assertEquals("", field.text()); assertEquals(0, field.cursor()); assertEquals(0, field.start()); assertEquals(0, field.end());
    assertTrue(field.focused);
  }

  @Test
  void clearingTypedOrSelectedSearchResetsEveryRenderIndex() {
    TextInput input = new TextInput("controls", 128);
    for (boolean selected : new boolean[] {false, true}) {
      input.setText("controls");
      if (selected) input.selectAll();
      input.setText("");
      assertEquals(0, input.cursor());
      assertEquals(0, input.start());
      assertEquals(0, input.end());
      assertEquals("", input.text().substring(0, input.cursor()));
    }
  }

  @Test
  void replacingSelectedPathWithShorterValueLeavesSelectionWithinText() {
    TextInput input = new TextInput("a/long/music/directory", 1024);
    input.selectAll();
    input.setText("music");
    assertEquals(5, input.cursor());
    assertEquals(5, input.start());
    assertEquals(5, input.end());
    input.selectAll();
    assertEquals("music", input.text().substring(input.start(), input.end()));
  }
}
