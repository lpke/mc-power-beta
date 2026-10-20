package local.luke.power.chat;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class ChatLayoutTest {
  private static int width(String text){return text.replaceAll("\u00a7.","").length()*6;}
  @Test void reflowRetainsSourceOffsetsAndBetaColorsWithoutSplittingFormatting() {
    var message=new ChatTranscript.Message("\u00a7aAlpha\u00a7cBeta");var layout=new ChatLayout();Object font=new Object();
    assertTrue(layout.update(List.of(message),font,30,ChatLayoutTest::width));
    assertEquals("AlphaBeta",message.plain);
    assertEquals(2,layout.rows().size());assertEquals(5,layout.rows().get(0).from());
    assertEquals("\u00a7cBeta",layout.rows().get(0).text());
    assertEquals("Alpha",layout.rows().get(1).text().replaceAll("\u00a7.",""));
    var old=layout.rows().get(0);layout.update(List.of(message),font,18,ChatLayoutTest::width);
    assertEquals(message,layout.rows().get(layout.anchor(old,0)).message());
    assertTrue(layout.rows().get(layout.anchor(old,0)).from()<=5);
  }
  @Test void stableMessagesNeedNoFontWorkAndIncomingMessagesKeepScrollAnchor() {
    var first=new ChatTranscript.Message("Original message");var next=new ChatTranscript.Message("New message");
    var layout=new ChatLayout();Object font=new Object();var source=List.of(first);
    layout.update(source,font,48,ChatLayoutTest::width);var old=layout.rows().get(1);
    assertFalse(layout.update(source,font,48,s->{throw new AssertionError("unchanged layout measured");}));
    layout.update(List.of(next,first),font,48,ChatLayoutTest::width);
    assertSame(first,layout.rows().get(layout.anchor(old,1)).message());
    assertEquals(old.from(),layout.rows().get(layout.anchor(old,1)).from());
  }
  @Test void explicitNewlinesAndVeryNarrowWindowsAreBounded() {
    var message=new ChatTranscript.Message("a\n\nBC");var layout=new ChatLayout();
    layout.update(List.of(message),this,1,ChatLayoutTest::width);
    assertEquals(4,layout.rows().size());assertTrue(layout.rows().stream().anyMatch(r->r.from()==r.to()));
    assertEquals("a\n\nBC",message.plain);
  }
}
