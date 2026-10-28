package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MusicFolderWarningTest {
  @Test void pathImmediatelyFollowsTitleBeforeTheConsequenceParagraph() {
    var lines = MusicFolderWarning.layout(296, 121, "/music/import", "", s -> s.length() * 6);
    assertEquals("Remove folder?", lines.get(0).text());
    assertEquals("/music/import", lines.get(1).text());
    assertEquals(11, lines.get(1).y() - lines.get(0).y());
    assertEquals(15, lines.get(2).y() - lines.get(1).y());
    assertEquals(MusicFolderWarning.CONSEQUENCE,
        String.join(" ", lines.subList(2, lines.size()).stream().map(WarningText.Line::text).toList()));
  }

  @Test void longPathsLeaveTheFullAvailabilityWarningVisibleAt320By240() {
    var lines = MusicFolderWarning.layout(296, 121, "/" + "folder/".repeat(100), "", s -> s.length() * 6);
    assertTrue(lines.stream().anyMatch(line -> line.text().endsWith("...")));
    assertTrue(String.join(" ", lines.stream().map(WarningText.Line::text).toList())
        .endsWith(MusicFolderWarning.CONSEQUENCE));
    for (var line : lines) {
      assertTrue(line.text().length() * 6 <= 296);
      assertTrue(line.y() >= 12);
      assertTrue(line.y() + 8 <= 121);
    }
  }

  @Test void errorsKeepTheFolderIdentityAndUseErrorColor() {
    var lines = MusicFolderWarning.layout(296, 121, "/music/import", "Folders changed. Cancel and select Remove again.", s -> s.length() * 6);
    assertEquals(0xaaaaaa, lines.get(1).color());
    assertTrue(lines.subList(2, lines.size()).stream().allMatch(line -> line.color() == 0xff8888));
  }
}
