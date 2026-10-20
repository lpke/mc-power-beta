package local.luke.power.chat;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ChatSelectionTest {
  private final ChatSelection selection = new ChatSelection();
  private final Object screen = new Object(), font = new Object();
  private void lines() {
    selection.begin(screen);
    selection.line("\u00a7aNewest line", font, 2, 150, ChatSelectionTest::width);
    selection.line("\u00a7cOlder wrapped", font, 2, 141, ChatSelectionTest::width);
    selection.end();
  }
  private static int width(String text) { return text.replaceAll("\u00a7.", "").length() * 6; }

  @Test void dragUsesVisibleNativeRowsAndCopiesPlainTextInReadingOrder() {
    lines();
    assertTrue(selection.click(screen, 2 + 6 * 6, 142));
    selection.move(screen, 2 + 6 * 6, 151, true);
    assertEquals("wrapped\nNewest", selection.text());
    assertEquals(6, selection.start(1));
    assertEquals(13, selection.end(1));
    assertEquals(2 + 6 * 6, selection.x(1, 6));
    assertFalse(selection.dragging());
    lines();
    assertEquals("wrapped\nNewest", selection.text());
  }

  @Test void reverseDragAndOutsideReleaseClampToRenderedText() {
    lines();
    assertTrue(selection.click(screen, 2 + 6 * 6, 151));
    selection.move(screen, 2 + 6 * 6, 142, true);
    assertEquals("wrapped\nNewest", selection.text());
    assertTrue(selection.click(screen, 2, 142));
    selection.move(screen, 10000, 2000, true);
    assertEquals("Older wrapped\nNewest line", selection.text());
    selection.move(screen, 2, 142, false);
    assertEquals("Older wrapped\nNewest line", selection.text());
  }

  @Test void scrollChangesAndFocusOrScreenLossDiscardStaleSelection() {
    lines(); selection.click(screen, 2, 142); selection.move(screen, 30, 151, true);
    selection.begin(screen);
    selection.line("Scrolled history", font, 2, 150, ChatSelectionTest::width);
    selection.end(); assertFalse(selection.selected());
    lines(); selection.click(screen, 2, 142); selection.move(screen, 30, 151, false);
    selection.context(screen, false); assertFalse(selection.selected()); assertFalse(selection.dragging());
    lines(); selection.click(screen, 2, 142); selection.move(screen, 30, 151, false);
    selection.context(new Object(), true); assertFalse(selection.selected());
    assertFalse(selection.click(screen, 2, 151));
  }

  @Test void cachedRowsDoNotMeasureTextOnEveryFrameAndInputClicksClearHistorySelection() {
    lines(); selection.click(screen, 2, 142); selection.move(screen, 30, 151, true);
    selection.begin(screen);
    selection.line("\u00a7aNewest line", font, 2, 150, value -> { throw new AssertionError("measured unchanged row"); });
    selection.line("\u00a7cOlder wrapped", font, 2, 141, value -> { throw new AssertionError("measured unchanged row"); });
    selection.end();
    assertTrue(selection.selected());
    assertFalse(selection.click(screen, 2, 190));
    assertFalse(selection.selected());
  }

  @Test void logicalSelectionSurvivesIncomingMessagesScrollAndReflowWithoutCopyingWrapNewlines() {
    var message=new ChatTranscript.Message("Alpha Beta Gamma");
    var newer=new ChatTranscript.Message("New message");var layout=new ChatLayout();
    layout.update(java.util.List.of(message),font,36,ChatSelectionTest::width);
    paint(layout,java.util.List.of(message));
    assertTrue(selection.click(screen,2,150-(layout.rows().size()-1)*9));
    selection.move(screen,999,150,true);
    assertEquals(message.plain,selection.text());
    var source=java.util.List.of(newer,message);layout.update(source,font,24,ChatSelectionTest::width);paint(layout,source);
    assertEquals(message.plain,selection.text());
    selection.begin(screen);selection.messages(source);
    var row=layout.rows().get(0);selection.line(row.message(),row.from(),row.to(),row.text(),font,2,150,ChatSelectionTest::width);selection.end();
    assertEquals(message.plain,selection.text(),"offscreen selection lost its source");
    selection.messages(java.util.List.of(newer));assertFalse(selection.selected(),"removed source must not copy stale data");
  }
  private void paint(ChatLayout layout,java.util.List<ChatTranscript.Message> source) {
    selection.begin(screen);selection.messages(source);
    int i=0;for(var row:layout.rows())selection.line(row.message(),row.from(),row.to(),row.text(),font,2,150-i++*9,ChatSelectionTest::width);
    selection.end();
  }
}
