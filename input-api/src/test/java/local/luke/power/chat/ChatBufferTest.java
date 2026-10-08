package local.luke.power.chat;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatBufferTest {
  @Test void selectionReplacementClipboardAndLimits() {
    ChatBuffer b = new ChatBuffer(12); b.set("hello world");
    b.move(-1, true, true); assertEquals("world", b.selection());
    b.write("Beta", c -> c >= 32); assertEquals("hello Beta", b.text());
    b.all(); b.write("\n", c -> c >= 32); assertEquals("hello Beta", b.text());
    b.write("abcdefghijklmnop", c -> c >= 32); assertEquals("abcdefghijkl", b.text());
    b.position(5, false); b.move(-1, true, false); b.delete(1, false);
    assertEquals("abcdfghijkl", b.text()); assertEquals(4, b.cursor());
  }
  @Test void historyKeepsArrowsAndRestoresDraftUntilEdited() {
    ChatBuffer b = new ChatBuffer(100); b.set("draft");
    var history = List.of("//set stone", "//set rail");
    b.history(history, true); assertEquals("//set rail", b.text()); assertTrue(b.browsing());
    b.history(history, true); assertEquals("//set stone", b.text());
    b.history(history, false); b.history(history, false); assertEquals("draft", b.text());
    assertTrue(b.browsing()); b.write("!", c -> true); assertFalse(b.browsing());
    b.history(history, true); b.history(history, false); assertEquals("draft!", b.text());
  }
  @Test void completionInsertsAtCursorAndPreservesSuffix() {
    ChatBuffer b = new ChatBuffer(100); b.set("//set st  tail"); b.position(8, false);
    b.complete("one"); assertEquals("//set stone  tail", b.text()); assertEquals(11, b.cursor());
    b.set("//set stone  tail"); b.position(8, false); b.complete("one");
    assertEquals("//set stone  tail", b.text());
  }
  @Test void viewportAndHitTestingShareCharacterWidths() {
    ChatBuffer b = new ChatBuffer(100); b.set("abcdefghij");
    var v = b.view(25, s -> s.length() * 6);
    assertEquals(6, v.start()); assertEquals(10, v.end());
    assertEquals(8, v.hit(b.text(), 12, s -> s.length() * 6));
    b.position(8, false); assertEquals(6, b.view(25, s -> s.length() * 6).start());
    b.position(0, false); v = b.view(25, s -> s.length() * 6);
    assertEquals(0, v.start()); assertEquals(4, v.end());
  }
}
