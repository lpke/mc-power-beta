package local.luke.power.worldedit.core;

import static org.junit.jupiter.api.Assertions.*;

import local.luke.power.worldedit.chat.*;
import org.junit.jupiter.api.Test;

class ChatTest {
  @Test
  void completionCyclesInBothDirectionsAndResetsAfterEditing() {
    var cycle = new CompletionCycle();
    assertEquals("//pos2", cycle.next("//pos", true));
    assertEquals("//pos1", cycle.next("//pos2", true));
    assertEquals("//pos2", cycle.next("//pos1", false));
    cycle.reset();
    assertEquals("//pos1", cycle.next("//pos", false));
    assertEquals("//set stone", cycle.next("//set ston", false));
    assertEquals("//unknown", cycle.next("//unknown", false));
  }

  @Test
  void doubleSlashNamesAndArgumentsComplete() {
    assertTrue(CommandCatalog.complete("//po").contains("//pos1"));
    assertTrue(CommandCatalog.complete("//set sto").contains("//set stone"));
    assertTrue(CommandCatalog.complete("//set ha").contains("//set hand"));
    assertTrue(CommandCatalog.complete("//replace !ha").contains("//replace !hand"));
    assertTrue(CommandCatalog.complete("//set 50%stone,50%ha").contains("//set 50%stone,50%hand"));
    assertTrue(
        CommandCatalog.complete("//set 50%stone,50%co").contains("//set 50%stone,50%cobblestone"));
    assertTrue(CommandCatalog.complete("//replace !ai").contains("//replace !air"));
    assertTrue(CommandCatalog.complete("//move 3 no").contains("//move 3 north"));
    assertTrue(CommandCatalog.complete("/remove items,mo").contains("/remove items,mobs"));
    assertTrue(CommandCatalog.complete("//set stone 3").isEmpty());
    assertFalse(CommandCatalog.complete("//set moving_").contains("//set moving_piston"));
  }

  @Test
  void ordinaryChatIsNotClaimed() {
    assertFalse(CommandCatalog.owns("hello"));
    assertFalse(CommandCatalog.owns("/give stone"));
    assertTrue(CommandCatalog.owns("//set stone"));
    assertTrue(CommandCatalog.owns("/remove items 20"));
  }

  @Test
  void fallbackHistoryRestoresUnsubmittedDraft() {
    ChatHistory.add("//pos1");
    ChatHistory.add("//pos2");
    ChatHistory.add("//pos2");
    var h = new ChatHistory();
    assertEquals("//pos2", h.move("//set sto", true));
    assertEquals("//pos1", h.move("//pos2", true));
    assertEquals("//pos2", h.move("//pos1", false));
    assertEquals("//set sto", h.move("//pos2", false));
  }
}
