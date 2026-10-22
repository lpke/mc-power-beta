package local.luke.power.validation;

import static local.luke.power.validation.Validation.*;
import java.util.*;
import local.luke.power.map.MapProjection;
import net.minecraft.client.Minecraft;

public final class MapHot421Checks {
  private MapHot421Checks() {}
  public static void run(Minecraft mc) throws Exception {
    var random = new Random(421);
    for (int n = 0; n < 1000; n++) {
      double left = random.nextInt(200) - 100, top = random.nextInt(200) - 100;
      double width = 16 + random.nextInt(600), height = 16 + random.nextInt(600);
      var before = new LegacyMapProjection421(-100, 200, 1.25, left, top, width, height, .7);
      var after = new MapProjection(-100, 200, 1.25, left, top, width, height, .7);
      double x = random.nextInt(1000) - 500, y = random.nextInt(1000) - 500;
      double size = 1 + random.nextInt(1200);
      var a = List.of(new LegacyMapProjection421.Point(x, y, 0, 0),
          new LegacyMapProjection421.Point(x + size, y, 1, 0),
          new LegacyMapProjection421.Point(x + size, y + size, 1, 1),
          new LegacyMapProjection421.Point(x, y + size, 0, 1));
      var b = a.stream().map(p -> new MapProjection.Point(p.x(), p.y(), p.u(), p.v())).toList();
      for (boolean circle : new boolean[]{false, true}) {
        var old = before.clip(a, circle); var next = after.clip(b, circle);
        check(old.size() == next.size(), "clip point count changed");
        for (int i = 0; i < old.size(); i++) {
          var p = old.get(i); var q = next.get(i);
          check(Double.doubleToLongBits(p.x()) == Double.doubleToLongBits(q.x())
              && Double.doubleToLongBits(p.y()) == Double.doubleToLongBits(q.y())
              && Double.doubleToLongBits(p.u()) == Double.doubleToLongBits(q.u())
              && Double.doubleToLongBits(p.v()) == Double.doubleToLongBits(q.v()), "clip coordinates/UV changed");
        }
      }
    }
    log("MAP HOT 421 PASS: 2000 clipped polygons retain exact coordinate/UV bits");
  }
}
