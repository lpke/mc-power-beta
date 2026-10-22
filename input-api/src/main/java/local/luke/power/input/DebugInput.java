package local.luke.power.input;

import java.util.HashSet;
import java.util.Set;

/** Own debug presses until their release, including release of the modifier first. */
public final class DebugInput {
  private static final Set<Integer> claimed = new HashSet<>();
  private static boolean enabled, held, used, overlay;
  private static int passthrough;
  private DebugInput() {}
  public static boolean action(String id) { return id.startsWith("powerbeta.debug.") && !id.equals("powerbeta.debug.modifier"); }
  public static void context(boolean active) { enabled = active; if (!active) clear(); }
  public static void press(boolean sameOverlay) { held = true; used = false; overlay = sameOverlay; }
  public static boolean release() { boolean toggle = held && overlay && !used; held = overlay = used = false; return toggle; }
  public static boolean held() { return enabled && held; }
  public static boolean managesOverlay() { return enabled && held && overlay; }
  public static void claim(int code) { used = true; claimed.add(code); }
  public static void picker(int code) { claim(code); passthrough = code; }
  public static boolean suppressNative(int code) { return claimed(code) && code != passthrough; }
  public static void released(int code) { claimed.remove(code); if (passthrough == code) passthrough = 0; }
  public static boolean claimed(int code) { return claimed.contains(code); }
  public static boolean allows(String id, int code) {
    if (id.equals("powerbeta.debug.modifier")) return enabled;
    if (action(id)) return held();
    if (id.equals("power_creative.picker") || id.equals("power_creative.modifier")) return true;
    if (managesOverlay() && id.equals("key.power_controls.debug_hud")) return false;
    return !claimed(code);
  }
  public static void clear() { claimed.clear(); held = used = overlay = false; passthrough = 0; }
}
