package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import java.util.*;
import org.junit.jupiter.api.Test;

class CreativeTabsHistoryTest {
  private JsonObject document(int value) { var result = new JsonObject(); result.addProperty("value", value); return result; }
  @Test void snapshotsOwnTheirDocumentAndSelectionAndIgnoreNoOpEdits() {
    var history = new CreativeTabsHistory(); var original = document(1); var cells = new HashSet<>(Set.of(3, 9));
    var state = new CreativeTabsHistory.State(original, 2, 1, 18, cells, 3);
    history.record(state, original); assertFalse(history.available());
    history.record(state, document(2)); original.addProperty("value", 99); cells.clear();
    var restored = history.undo(); assertEquals(document(1), restored.document()); assertEquals(Set.of(3, 9), restored.cells());
    assertEquals(2, restored.selected()); assertEquals(1, restored.listScroll()); assertEquals(18, restored.gridScroll()); assertEquals(3, restored.anchor());
    assertFalse(history.available()); assertNull(history.undo());
  }
  @Test void repeatedEditsAndResetUndoInReverseOrderWithinBound() {
    var history = new CreativeTabsHistory();
    for (int value = 0; value < 40; value++) history.record(new CreativeTabsHistory.State(document(value), 0, 0, 0, Set.of(), -1), document(value + 1));
    for (int value = 39; value >= 8; value--) assertEquals(document(value), history.undo().document());
    assertNull(history.undo());
  }
}
