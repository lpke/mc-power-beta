package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class IntegerArrayDraftTest {
  private IntegerArrayDraft draft() {
    return new IntegerArrayDraft(new IntegerArrayDraft.Rules(2, 32, 16, true), List.of(12, 8, 4, 2));
  }

  @Test void editingNeverMutatesTheOriginalOrReturnedList() {
    var original = new ArrayList<>(List.of(12, 8, 4, 2));
    var draft = new IntegerArrayDraft(new IntegerArrayDraft.Rules(2, 32, 16, true), original);
    draft.set(0, 32);
    assertEquals(List.of(12, 8, 4, 2), original);
    assertThrows(UnsupportedOperationException.class, () -> draft.values().set(0, 2));
  }

  @Test void duplicatesAreEditableButCannotBeAccepted() {
    var draft = draft();
    draft.set(0, 8);
    assertFalse(draft.error().isEmpty());
    draft.set(1, 32);
    assertEquals("", draft.error());
  }

  @Test void badRangeAndResetLeaveDraftIntact() {
    var draft = draft();
    assertThrows(IllegalArgumentException.class, () -> draft.set(0, 33));
    assertThrows(IllegalArgumentException.class, () -> draft.reset(List.of(16, 1)));
    assertThrows(IllegalArgumentException.class, () -> draft.reset(List.of()));
    assertEquals(List.of(12, 8, 4, 2), draft.values());
  }

  @Test void additionUsesUnusedValuesAndStopsAtLimit() {
    var draft = draft();
    while (draft.add()) assertTrue(draft.size() <= 16);
    assertEquals(16, draft.size());
    assertEquals("", draft.error());
    assertFalse(draft.add());
  }

  @Test void occupiedRangeStopsAddition() {
    var draft = new IntegerArrayDraft(new IntegerArrayDraft.Rules(2, 3, 16, true), List.of(3, 2));
    assertFalse(draft.add());
    draft.remove(0);
    assertTrue(draft.add());
    assertEquals(List.of(2, 3), draft.values());
  }

  @Test void reorderAndRemovalKeepValidEntriesAndAtLeastOne() {
    var draft = draft();
    draft.move(0, -1);
    draft.move(3, 1);
    draft.move(0, 2);
    assertEquals(List.of(12, 8, 4, 2), draft.values());
    draft.move(0, 1);
    assertEquals(List.of(8, 12, 4, 2), draft.values());
    for (int i = 0; i < 10; i++) draft.remove(0);
    assertEquals(List.of(2), draft.values());
    draft.remove(-1);
    draft.remove(50);
    assertEquals(List.of(2), draft.values());
  }
}
