package local.luke.power.chat;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ChatRetentionTest {
  @Test void limitsAreIndependentAndWrappedMessagesStayWhole() {
    var retention = new ChatRetention<Object>();
    Object newest = new Object(), own1 = new Object(), own2 = new Object(), old = new Object(), olderOwn = new Object();
    long own = retention.group();
    retention.add(own1, own, true); retention.add(own2, own, true);
    retention.add(olderOwn, retention.group(), true);
    retention.add(old, retention.group(), false); retention.add(newest, retention.group(), false);
    var lines = new ArrayList<>(List.of(newest, own1, own2, old, olderOwn));
    retention.trim(lines, 1, 1);
    assertEquals(List.of(newest, own1, own2), lines);
    retention.trim(lines, 1, 1); assertEquals(3, lines.size());
    retention.removeOwn(lines); assertEquals(List.of(newest), lines);
  }
}
