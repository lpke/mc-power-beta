package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class WarningTextTest {
  @Test void wrapsConsequencesWithoutLosingWordsOnCompactScreens() {
    String message = "Music files and other playlists stay unchanged.";
    var lines = WarningText.layout(296, 109, "Delete this playlist?", message, 0xaaaaaa, s -> s.length() * 6);
    assertEquals(message, String.join(" ", lines.stream().filter(line -> line.color() == 0xaaaaaa)
        .map(WarningText.Line::text).toList()));
    contained(lines, 296, 109);
  }

  @Test void boundsLongNamesAndPathsAboveButtons() {
    var lines = WarningText.layout(296, 109, "Remove \"" + "long-name".repeat(40) + "\"?",
        "Files and playlist entries stay unchanged.\n/" + "folder/".repeat(100),
        0xaaaaaa, s -> s.length() * 6);
    contained(lines, 296, 109);
    assertTrue(lines.stream().anyMatch(line -> line.text().equals("Files and playlist entries stay unchanged.")));
    assertTrue(lines.get(lines.size() - 1).text().endsWith("..."));
  }

  @Test void keepsErrorCopyAndExplicitParagraphsCenteredInTheSameBlock() {
    var lines = WarningText.layout(296, 109, "Delete this playlist?", "Playlist changed.\nCancel and select Delete again.",
        0xff8888, s -> s.length() * 6);
    assertEquals(List.of("Delete this playlist?", "Playlist changed.", "Cancel and select Delete again."),
        lines.stream().map(WarningText.Line::text).toList());
    assertEquals(0xff8888, lines.get(1).color());
    assertEquals(0xff8888, lines.get(2).color());
    contained(lines, 296, 109);
  }

  private void contained(List<WarningText.Line> lines, int span, int bottom) {
    for (var line : lines) {
      assertTrue(line.text().length() * 6 <= span, line.text());
      assertTrue(line.y() >= 12);
      assertTrue(line.y() + 8 <= bottom);
    }
  }
}
