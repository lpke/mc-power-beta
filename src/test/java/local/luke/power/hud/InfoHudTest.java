package local.luke.power.hud;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

class InfoHudTest {
  @Test void clockUsesDaylightTicksAndWrapsAtMidnight() {
    assertEquals("6:00 AM", HudText.time(0));
    assertEquals("12:00 PM", HudText.time(6000));
    assertEquals("6:00 PM", HudText.time(12000));
    assertEquals("12:00 AM", HudText.time(18000));
    assertEquals("5:59 AM", HudText.time(-1));
    assertEquals("6:00 AM", HudText.time(24000));
    assertEquals(HudText.time(Math.floorMod(Long.MAX_VALUE, 24000L)), HudText.time(Long.MAX_VALUE));
  }
  @Test void clusterDataSurvivesGson289AndCopiesIndependently() {
    var gson = new Gson();
    var settings = gson.fromJson("{}", InfoHudSettings.class);
    assertFalse(settings.enabled);
    for (var c : settings.clusters) assertTrue(c.hideWithDebug);
    settings.clusters[1].textSize = 1.7;
    settings.clusters[1].textColor = "#123456";
    settings.clusters[1].opacity = 50;
    settings.clusters[1].offsetX = -20;
    var copy = gson.fromJson(gson.toJson(settings), InfoHudSettings.class);
    copy.validate();
    assertEquals(1.7, copy.clusters[1].textSize);
    assertEquals(0x80123456, copy.clusters[1].argb());
    copy.clusters[0].coordinates = false;
    assertTrue(settings.clusters[0].coordinates);
    assertEquals(-20, copy.clusters[1].offsetX);
    copy.clusters[1].textSize = Double.NaN;
    assertThrows(IllegalArgumentException.class, copy::validate);
    copy.clusters[1].textSize = 1;
    copy.clusters[1].offsetY = Integer.MIN_VALUE;
    assertThrows(IllegalArgumentException.class, copy::validate);
    assertThrows(IllegalArgumentException.class, () -> gson.fromJson("{\"clusters\":[null]}", InfoHudSettings.class).validate());
  }
  @Test void scaledClustersAndOffsetsStayWithinAllNineAnchors() {
    var c = new InfoHudSettings.Cluster();
    for (int width : new int[]{320, 427, 854}) for (int height : new int[]{240, 450})
      for (double scale : new double[]{.5, 1, 3}) for (int anchor = 0; anchor < 9; anchor++)
        for (int offset : new int[]{-4096, 0, 4096}) {
          c.position = anchor; c.textSize = scale; c.offsetX = c.offsetY = offset;
          int textWidth = (int)Math.min(180, (width - 8) / scale);
          var layout = InfoHudLayout.at(width, height, textWidth, 7, c);
          assertTrue(layout.x() >= 2 && layout.x() + textWidth * scale <= width - 2);
          assertTrue(layout.y() >= 2 && layout.y() + 70 * scale <= height - 2);
          assertTrue(layout.lineX(textWidth, 20) >= 0);
          assertTrue(layout.lineX(textWidth, 20) + 20 <= textWidth);
        }
  }
  @Test void facingWrapsThroughNegativeYaw() {
    assertEquals("( Z, S)", HudText.direction(0));
    assertEquals("(-X, W)", HudText.direction(90));
    assertEquals("(-Z, N)", HudText.direction(180));
    assertEquals("( X, E)", HudText.direction(-90));
    assertEquals(HudText.direction(45), HudText.direction(405));
    assertEquals("( Z, S)", HudText.direction(44.99F));
    assertEquals("(-X, W)", HudText.direction(45));
  }
  @Test void coordinatesAndDirectionShareOneLineOnlyWhenBothAreSelected() {
    String coordinates = HudText.coordinates(-123, 64, 456);
    assertEquals("-123, 64, 456", coordinates);
    assertEquals(coordinates, HudText.location(coordinates, HudText.direction(90), true, false));
    assertEquals("-123, 64, 456 (-X, W)", HudText.location(coordinates, HudText.direction(90), true, true));
    assertEquals("-123, 64, 456 ( X, E)", HudText.location(coordinates, HudText.direction(-90), true, true));
    assertEquals("(-Z, N)", HudText.location(coordinates, HudText.direction(180), false, true));
    assertEquals("", HudText.location(coordinates, HudText.direction(0), false, false));
  }
}
