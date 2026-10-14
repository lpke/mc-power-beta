package local.luke.power.ui;

import local.luke.power.chat.ChatBuffer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TextSelectionTest {
  @Test void pointerSharesTheRenderedRangeAndKeepsTheFinalReleasePosition() {
    var editor = new ChatBuffer(30); editor.set("abcdefghij"); editor.position(0, false);
    var pointer = new TextPointer(editor); Object screen = new Object();
    pointer.layout(screen, 100, 31, s -> s.length() * 6);
    pointer.click(106, false, () -> true);
    TextDrag.event(screen, 124, -1, false, true);
    assertEquals("bcd", editor.selection());
    TextDrag.event(screen, 118, 0, false, true);
    assertEquals("bc", editor.selection());
    TextDrag.event(screen, 100, -1, false, true);
    assertEquals("bc", editor.selection());
  }
  @Test void focusAndScreenChangesCancelUntilAnotherPress() {
    var editor = new ChatBuffer(20); editor.set("abcdef");
    var pointer = new TextPointer(editor); Object screen = new Object();
    pointer.layout(screen, 0, 100, s -> s.length() * 6); pointer.click(0, false, () -> true);
    TextDrag.context(screen, false);
    TextDrag.event(screen, 30, -1, false, true); assertFalse(editor.selected());
    pointer.click(0, false, () -> true); TextDrag.context(new Object(), true);
    TextDrag.event(screen, 30, -1, false, true); assertFalse(editor.selected());
  }
  @Test void keyboardWordSelectionAndDeletionShareChatSemantics() {
    var editor = new ChatBuffer(12); editor.set("one two");
    TextEditing.key(editor, '\0', 203, true, true, c -> c >= 32);
    assertEquals("two", editor.selection());
    TextEditing.key(editor, 'x', 45, false, false, c -> c >= 32);
    assertEquals("one x", editor.text());
    TextEditing.key(editor, '\0', 14, true, false, c -> c >= 32);
    assertEquals("one ", editor.text());
  }
}
