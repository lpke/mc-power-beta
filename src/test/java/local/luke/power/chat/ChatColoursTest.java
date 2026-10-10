package local.luke.power.chat;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ChatColoursTest {
  @Test void plainTextHasNoColour() {
    assertEquals("", ChatColours.continuation("plain text", 6));
  }

  @Test void continuationUsesTheLastColourBeforeTheBreak() {
    String message = "§e[WE] §cCheats are disabled for this world. §aNext";
    assertEquals("§c", ChatColours.continuation(message, message.indexOf("world")));
    assertEquals("§a", ChatColours.continuation(message, message.length()));
  }

  @Test void allBetaColoursAndResetsSurvive() {
    for (char code : "0123456789abcdefABCDEFrz".toCharArray()) {
      String pair = "§" + code;
      assertEquals(pair, ChatColours.continuation("§cRed " + pair + "Next", 10));
    }
  }

  @Test void codesAfterTheBreakAndIncompletePairsDoNotChangeColour() {
    assertEquals("§c", ChatColours.continuation("§cRed §aGreen", 6));
    assertEquals("§c", ChatColours.continuation("§cRed §aGreen", 7));
    assertEquals("", ChatColours.continuation("plain §", 7));
    assertEquals("§§", ChatColours.continuation("§§cText", 4));
  }
}
