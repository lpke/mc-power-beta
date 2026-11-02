package local.luke.power.chat;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PreviousCommandTest {
  @AfterEach void clear() { PreviousCommand.clear(); }

  @Test void recallsLatestSlashCommandAfterOrdinaryChatWithoutChangingItsText() {
    Object world = new Object();
    List<String> dispatched = new ArrayList<>();
    assertEquals(PreviousCommand.Result.EMPTY, PreviousCommand.execute(world, dispatched::add));
    PreviousCommand.record(world, "/seed");
    PreviousCommand.record(world, "  //replace stone  glass  ");
    for (String text : new String[]{"hello", "", "/", "/   ", null, "/bad\ncommand", "/"+"x".repeat(100)})
      PreviousCommand.record(world, text);
    assertEquals(PreviousCommand.Result.EXECUTED, PreviousCommand.execute(world, dispatched::add));
    assertEquals(PreviousCommand.Result.EXECUTED, PreviousCommand.execute(world, dispatched::add));
    assertEquals(List.of("//replace stone  glass", "//replace stone  glass"), dispatched);
  }

  @Test void neverReplaysACommandFromAnotherWorldOrAnEndedVisit() {
    Object first = new Object(), second = new Object();
    PreviousCommand.record(first, "/seed");
    assertEquals(PreviousCommand.Result.EMPTY, PreviousCommand.execute(second, text -> fail(text)));
    assertEquals(PreviousCommand.Result.EMPTY, PreviousCommand.execute(first, text -> fail(text)));
    PreviousCommand.record(first, "/help"); PreviousCommand.clear();
    assertEquals(PreviousCommand.Result.EMPTY, PreviousCommand.execute(first, text -> fail(text)));
    PreviousCommand.record(null, "/seed");
    assertEquals(PreviousCommand.Result.EMPTY, PreviousCommand.execute(null, text -> fail(text)));
  }

  @Test void nestedDispatchCannotRecurseOrReplaceTheSubmittedCommand() {
    Object world = new Object();
    PreviousCommand.record(world, "/seed");
    List<String> dispatched = new ArrayList<>();
    PreviousCommand.execute(world, text -> {
      dispatched.add(text);
      PreviousCommand.record(world, "/different");
      assertEquals(PreviousCommand.Result.BUSY, PreviousCommand.execute(world, nested -> fail(nested)));
    });
    PreviousCommand.execute(world, dispatched::add);
    assertEquals(List.of("/seed", "/seed"), dispatched);
  }

  @Test void dispatcherFailureDoesNotStickTheGuardOrLoseTheCommand() {
    Object world = new Object();
    PreviousCommand.record(world, "/seed");
    assertThrows(IllegalStateException.class, () -> PreviousCommand.execute(world, text -> { throw new IllegalStateException("failed"); }));
    List<String> dispatched = new ArrayList<>();
    assertEquals(PreviousCommand.Result.EXECUTED, PreviousCommand.execute(world, dispatched::add));
    assertEquals(List.of("/seed"), dispatched);
  }

  @Test void consumedChatPressEndsAtItsOwnReleaseAndDoesNotHideTheNextTap() {
    PreviousCommand.consumePress(28);
    assertTrue(PreviousCommand.ownsPress(28));
    assertFalse(PreviousCommand.ownsPress(30));
    PreviousCommand.releasePress(30);
    assertTrue(PreviousCommand.ownsPress(28));
    PreviousCommand.releasePress(28);
    assertFalse(PreviousCommand.hasPress());
    PreviousCommand.consumePress(28); PreviousCommand.clear();
    assertFalse(PreviousCommand.ownsPress(28));
  }
}
