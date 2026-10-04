package local.luke.power.chat;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class HelpOutputTest {
  @Test
  void pagingAndFullOutputRetainEveryLineExactlyOnce() {
    var rows =
        java.util.stream.IntStream.range(0, 19).mapToObj(i -> "/test" + i + " [value]").toList();
    List<String> output = new ArrayList<>();
    HelpOutput.print(output::add, "Commands", rows, 1, 6, true);
    assertEquals(20, output.size());
    assertEquals(20, new HashSet<>(output).size());
    assertTrue(output.get(19).contains("/test18"));
    output.clear();
    HelpOutput.print(output::add, "Commands", rows, 4, 6, false);
    assertEquals(2, output.size());
    assertTrue(output.get(0).contains("4/4"));
    assertTrue(output.get(1).contains("/test18"));
    assertThrows(
        IllegalArgumentException.class,
        () -> HelpOutput.print(output::add, "Commands", rows, 0, 6, false));
    assertThrows(
        IllegalArgumentException.class,
        () -> HelpOutput.print(output::add, "Commands", rows, 5, 6, false));
  }

  @Test
  void commandAndDescriptionColoursAreIndependent() {
    String line = HelpOutput.line("Usage: /tp <target> | /teleport <target>");
    assertTrue(line.contains("§b/tp§7 <target>"));
    assertTrue(line.contains("§b/teleport§7"));
    assertFalse(line.contains("Usage"));
    assertFalse(line.contains("[WE]"));
  }
}
