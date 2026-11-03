package local.luke.power.validation;

import com.google.gson.JsonObject;
import java.nio.IntBuffer;

/** Explicit, one-shot validation fault at the native XQueryPointer result boundary. */
public final class GuiCursor472StaleQueryFault {
  private static boolean armed;
  private static int x, y, applications, realX, realY;
  private GuiCursor472StaleQueryFault() {}

  public static void arm(int preReleaseX, int preReleaseY) {
    if (armed) throw new IllegalStateException("stale-query fault already armed");
    x = preReleaseX; y = preReleaseY; applications = 0;
    armed = true;
  }

  /** Keep the actual native query, warp and event queue, replacing only one returned position. */
  public static void queried(boolean grabbed, IntBuffer result) {
    if (!armed || grabbed) return;
    armed = false;
    realX = result.get(2); realY = result.get(3);
    result.put(0, result.get(0) + x - realX);
    result.put(1, result.get(1) + y - realY);
    result.put(2, x); result.put(3, y);
    applications++;
  }

  public static void disarm() { armed = false; }

  public static JsonObject report() {
    var value = new JsonObject();
    value.addProperty("mode", "validation-only stale XQueryPointer result fault injection");
    value.addProperty("applications", applications);
    value.addProperty("realQueryX", realX); value.addProperty("realQueryY", realY);
    value.addProperty("substitutedPreReleaseX", x); value.addProperty("substitutedPreReleaseY", y);
    value.addProperty("armed", armed);
    value.addProperty("xwaylandWarpDelayReproduced", false);
    return value;
  }
}
