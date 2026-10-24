package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import local.luke.power.chat.ChatBuffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RightClickClearTest {
  @AfterEach void reset() { TextEditing.rightClickClear(() -> false); TextDrag.cancel(); }

  @Test void preferenceIsLiveAndOffRetainsTextSelectionAndHistory() {
    boolean[] enabled = {false};
    TextEditing.rightClickClear(() -> enabled[0]);
    var editor = new ChatBuffer(12); editor.history(List.of("recalled"), true); editor.position(0, true);
    assertFalse(TextEditing.clearOnRightClick(editor, 1));
    assertEquals("recalled", editor.selection());
    assertTrue(editor.browsing());
    assertFalse(TextEditing.clearOnRightClick(editor, 0));
    assertFalse(TextEditing.clearOnRightClick(editor, 2));
    enabled[0] = true;
    assertTrue(TextEditing.clearOnRightClick(editor, 1));
    assertEquals("", editor.text());
    assertEquals(0, editor.cursor()); assertEquals(0, editor.start()); assertEquals(0, editor.end());
    assertFalse(editor.browsing());
    editor.write("abcdefghijklmnop", value -> true);
    assertEquals("abcdefghijkl", editor.text());
  }

  @Test void clearingCancelsThePreviousPointerGesture() {
    TextEditing.rightClickClear(() -> true);
    var editor = new ChatBuffer(30); editor.set("first field");
    var pointer = new TextPointer(editor); Object screen = new Object();
    pointer.layout(screen, 0, 100, text -> text.length() * 6); pointer.click(0, false, () -> true);
    var other = new ChatBuffer(30); other.set("second field");
    assertTrue(TextEditing.clearOnRightClick(other, 1));
    TextDrag.event(screen, 42, -1, false, true);
    assertEquals(0, editor.cursor());
    assertFalse(editor.selected());
  }
}
