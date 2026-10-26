package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.lwjgl.input.Keyboard;

class SettingsDropdownTest {
  private SettingsDropdown open(Object owner, int current, int reset, int height) {
    var popup = new SettingsDropdown();
    popup.open(owner, List.of("Edit playlists", "Beta", "World", "Menu"), List.of(), current, reset,
        40, 30, 160, height, 320, 240, index -> {});
    return popup;
  }

  private Object field(SettingsDropdown popup, String name) throws ReflectiveOperationException {
    Field field = SettingsDropdown.class.getDeclaredField(name);
    field.setAccessible(true);
    return field.get(popup);
  }

  @Test void ownerAndMarkersBelongToTheSpecificOpeningControl() throws Exception {
    var popup = open("playlist", 2, 1, 18);
    assertTrue(popup.editing("playlist"));
    assertFalse(popup.editing("other"));
    assertEquals(2, field(popup, "selected"));
    assertEquals(1, field(popup, "defaultIndex"));
    assertEquals(18, field(popup, "rowHeight"));
    popup.separatorsBefore(1, 2);
    assertEquals(2, field(popup, "selected"));
    assertEquals(1, field(popup, "defaultIndex"));
    popup.markers(3, 2);
    assertEquals(3, field(popup, "selected"));
    assertEquals(2, field(popup, "defaultIndex"));
    popup.close();
    assertFalse(popup.editing("playlist"));
  }

  @Test void keyboardFocusAppearsOnlyAfterNavigationAndNeverStartsOnTheFirstAction() throws Exception {
    var popup = open("actions", -1, -1, 18);
    assertEquals(-1, field(popup, "focus"));
    assertEquals(false, field(popup, "keyboardFocus"));
    popup.key(null, Keyboard.KEY_DOWN);
    assertEquals(0, field(popup, "focus"));
    assertEquals(true, field(popup, "keyboardFocus"));
    popup.key(null, Keyboard.KEY_UP);
    assertEquals(3, field(popup, "focus"));
    popup.key(null, Keyboard.KEY_HOME);
    assertEquals(0, field(popup, "focus"));
    popup.key(null, Keyboard.KEY_END);
    assertEquals(3, field(popup, "focus"));
    popup.wheel(1);
    assertEquals(false, field(popup, "keyboardFocus"));
  }

  @Test void contextActionsStartWithoutFocusAndKeyboardNavigationSkipsDisabledRows() throws Exception {
    var popup = new SettingsDropdown();
    var screen = new PopupStyleTest.Host();
    popup.openActions("queue", screen, List.of("Unavailable", "Add to queue", "Play"), List.of(false, true, true),
        319, 239, 320, 240, index -> {});
    assertTrue(popup.editing("queue"));
    assertEquals(true, field(popup, "context"));
    assertEquals(-1, field(popup, "focus"));
    assertEquals(false, field(popup, "keyboardFocus"));
    popup.key(screen, Keyboard.KEY_DOWN);
    assertEquals(1, field(popup, "focus"));
    popup.key(screen, Keyboard.KEY_UP);
    assertEquals(2, field(popup, "focus"));
  }

  @Test void separatorAndOutsideEdgeClicksCannotActivateRows() throws Exception {
    var popup = new SettingsDropdown();
    var screen = new PopupStyleTest.Host();
    var accepted = new AtomicInteger(-1);
    popup.open("playlist", List.of("Edit", "Beta", "World"), List.of(), 2, 1,
        40, 30, 160, 18, 320, 240, accepted::set);
    popup.separatorsBefore(1, 2);
    int x = (int)field(popup, "x"), y = (int)field(popup, "y");
    popup.press(screen, x + 5, y + 18, 0);
    assertEquals(-1, accepted.get());
    assertTrue(popup.active());
    popup.press(screen, x + 159, y + 19, 1);
    assertEquals(-1, accepted.get());
    assertTrue(popup.active());
    popup.press(screen, x + 159, y + 19, 0);
    assertEquals(1, accepted.get());
    assertEquals(1, screen.clicks);
    assertFalse(popup.active());
  }

  @Test void scrollingPreservesSeparatorHitBoundsAndOriginalCallbackIndices() throws Exception {
    for (int height : new int[]{18, 20}) {
      var popup = new SettingsDropdown();
      var screen = new PopupStyleTest.Host();
      var accepted = new AtomicInteger(-1);
      var options = java.util.stream.IntStream.range(0, 30).mapToObj(i -> "Track " + i).toList();
      popup.open("playlist", options, List.of(), 0, -1, 40, 30, 160, height, 320, 240, accepted::set);
      popup.separatorsBefore(1, 2, 5);
      popup.wheel(-1);
      var layout = (PopupLayout.Rows)field(popup, "layout");
      int x = (int)field(popup, "x"), y = (int)field(popup, "y"), scroll = ((Double)field(popup, "scroll")).intValue();
      int next = y + layout.top(5) - scroll;
      popup.press(screen, x + 5, next - 1, 0);
      assertEquals(-1, accepted.get());
      assertTrue(popup.active());
      popup.press(screen, x + 5, next, 0);
      assertEquals(5, accepted.get());
      assertEquals(1, screen.clicks);
    }
  }

  @Test void emptyScrollbarGutterDoesNotActivateAChoice() throws Exception {
    var popup = new SettingsDropdown();
    var screen = new PopupStyleTest.Host();
    var accepted = new AtomicInteger(-1);
    popup.open("playlist", java.util.Collections.nCopies(30, "Track"), List.of(), 0, -1,
        40, 30, 160, 18, 320, 240, accepted::set);
    int x = (int)field(popup, "x"), y = (int)field(popup, "y"), width = (int)field(popup, "width");
    popup.press(screen, x + width - 9, y + 5, 0);
    assertEquals(-1, accepted.get());
    assertTrue(popup.active());
    popup.press(screen, x + width - 11, y + 5, 0);
    assertEquals(0, accepted.get());
  }
}
