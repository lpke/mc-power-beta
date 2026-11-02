package local.luke.power.hud;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToIntFunction;
import org.junit.jupiter.api.Test;

class InfoHudFrameCacheTest {
  @Test void unchangedFramesRetainMeasuredTextAndIgnoreUnusedValues() {
    var cluster = new InfoHudSettings.Cluster(); cluster.coordinates = true;
    var data = InfoHudData.sample(); var cache = new InfoHudFrame.Cache();
    var measurements = new AtomicInteger();
    ToIntFunction<String> measure = text -> { measurements.incrementAndGet(); return text.length() * 6; };
    Object font = new Object();
    var first = cache.get(cluster, data, 320, 240, font, measure);
    int before = measurements.get();
    for (int i = 0; i < 1000; i++) assertSame(first, cache.get(cluster, data, 320, 240, font, measure));
    assertSame(first, cache.get(cluster, new InfoHudData(data.coordinates(), "other", "new biome", "new time", "99", "1/1", "999"), 320, 240, font, measure));
    assertEquals(before, measurements.get());
    assertNotSame(first, cache.get(cluster, new InfoHudData("1, 2, 3", "", "", "", "", "", ""), 320, 240, font, measure));
  }

  @Test void fontViewportAndInPlacePresentationChangesRefreshTheFrame() {
    var cluster = new InfoHudSettings.Cluster(); cluster.coordinates = true;
    var data = InfoHudData.sample(); var cache = new InfoHudFrame.Cache(); Object font = new Object();
    ToIntFunction<String> measure = text -> text.length() * 6;
    var first = cache.get(cluster, data, 320, 240, font, measure);
    var resized = cache.get(cluster, data, 640, 480, font, measure); assertNotSame(first, resized);
    var reloaded = cache.get(cluster, data, 640, 480, new Object(), measure); assertNotSame(resized, reloaded);
    cluster.offsetX = 30;
    var moved = cache.get(cluster, data, 640, 480, font, measure); assertNotSame(reloaded, moved);
    cluster.timeOfDay = true;
    var fields = cache.get(cluster, data, 640, 480, font, measure); assertNotSame(moved, fields); assertEquals(2, fields.lines().size());
    cluster.textColor = "#000000";
    var colored = cache.get(cluster, data, 640, 480, font, measure);
    assertEquals(0xff000000, colored.argb());
    cache.clear();
    assertNotSame(colored, cache.get(cluster, data, 640, 480, font, measure));
  }
}
