package local.luke.power.ui;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonPrimitive;
import java.util.List;
import local.luke.power.config.Setting;
import org.junit.jupiter.api.Test;

class PhysicalSliderGeometryTest {
  @Test void fractionalDisplayScalingRetainsExactThumbCenterEndpoints() {
    for (double scale : new double[]{1, 1.25, 1.5, 2, 1708d / 320, 1919d / 320})
      for (int left : new int[]{0, 106, 122, 451}) for (int width : new int[]{54, 70, 150, 160}) {
        var track = SliderGeometry.physicalTrack(left, width, scale);
        assertEquals(0, track.fraction((int)((left + 4) * scale)));
        assertEquals(1, track.fraction((int)((left + width - 4) * scale)));
        assertEquals(0, track.fraction(track.left() - 1));
        assertEquals(1, track.fraction(track.left() + track.span() + 1));
      }
  }
  @Test void physicalPointerEndpointsRetainNegativeAndWideRangesWithoutChangingPreciseEntry() {
    for (double[] bounds : new double[][]{{0,10000},{1,2048},{-4096,4096},{0,1000000}}) {
      var setting = new Setting("physical-slider", "test", "", "", "Numeric", "", Setting.Kind.INTEGER,
          new JsonPrimitive(137), new JsonPrimitive(137), bounds[0], bounds[1], 10, List.of(), false);
      var track = SliderGeometry.physicalTrack(122, 160, 1708d / 320);
      assertEquals(137, setting.value.getAsInt());
      setting.slide(track.fraction((int)(126 * (1708d / 320)))); assertEquals(bounds[0], setting.value.getAsDouble());
      setting.slide(track.fraction((int)(278 * (1708d / 320)))); assertEquals(bounds[1], setting.value.getAsDouble());
      setting.parse("137"); assertEquals(137, setting.value.getAsInt());
    }
  }
}
