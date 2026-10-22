package local.luke.power.input;

import java.util.HashSet;
import java.util.Set;
import java.util.function.IntSupplier;

/** Own debug presses until their release, including release of the modifier first. */
public final class DebugInput {
  private static final Set<Integer> claimed = new HashSet<>();
  private static final Set<Integer> alreadyHeld = new HashSet<>();
  private static final Set<Integer> dispatched = new HashSet<>();
  private static IntSupplier preflight = () -> 0;
  private static boolean enabled, held, used, overlay, prepared;
  private static int passthrough, pollingModifier;
  private static long entryGeneration;
  private DebugInput() {}
  public static boolean action(String id) { return id.startsWith("powerbeta.debug.") && !id.equals("powerbeta.debug.modifier"); }
  public static void context(boolean active) { enabled = active; if (!active) clear(); }
  /** Return the eligible, physically held debug modifier, or zero. Never dispatch actions here. */
  public static void preflight(IntSupplier modifier) { preflight = java.util.Objects.requireNonNull(modifier); }
  public static void dispatched(int code, boolean down) { if (down) dispatched.add(code); else dispatched.remove(code); }
  public static void press(boolean sameOverlay) { press(sameOverlay, dispatched); }
  public static void press(boolean sameOverlay, Set<Integer> priorHeld) {
    if (!prepared) { entryGeneration++; used = false; alreadyHeld.clear(); alreadyHeld.addAll(priorHeld); }
    held = true; overlay = sameOverlay; prepared = false;
  }
  public static boolean release() { boolean toggle = held && overlay && !used; held = overlay = used = prepared = false; pollingModifier = 0; return toggle; }
  public static boolean held() { return enabled && held; }
  public static boolean actionHeld() { return enabled && (held || prepared && pollingModifier != 0); }
  /** Retain completed short holds until polling owners have observed their cancellation boundary. */
  public static long entryGeneration() { return entryGeneration; }
  public static boolean managesOverlay() { return enabled && held && overlay; }
  public static void claim(int code) { used = true; claimed.add(code); }
  public static void picker(int code) { claim(code); passthrough = code; }
  public static boolean suppressNative(int code) { return claimed(code) && code != passthrough; }
  public static void released(int code) { claimed.remove(code); alreadyHeld.remove(code); if (passthrough == code) passthrough = 0; }
  public static boolean claimed(int code) { return claimed.contains(code); }
  public static boolean allows(String id, int code) { return allows(id, code, true); }
  public static boolean allows(String id, int code, boolean physicallyDown) {
    if (id.equals("powerbeta.debug.modifier")) return enabled;
    if (action(id)) return actionHeld();
    if (id.equals("power_creative.picker") || id.equals("power_creative.modifier")) return true;
    pollingModifier = preflight.getAsInt();
    if (pollingModifier == 0 && !held && prepared) { prepared = false; used = false; alreadyHeld.clear(); }
    if (pollingModifier != 0 && !held && !prepared) {
      entryGeneration++; prepared = true; used = false; alreadyHeld.clear(); alreadyHeld.addAll(dispatched);
    }
    if (managesOverlay() && id.equals("key.power_controls.debug_hud")) return false;
    if (enabled && (held || pollingModifier != 0) && !alreadyHeld.contains(code)) {
      if (physicallyDown && code != pollingModifier) claim(code);
      return false;
    }
    return !claimed(code);
  }
  public static void clear() { claimed.clear(); alreadyHeld.clear(); dispatched.clear(); held = used = overlay = prepared = false; passthrough = pollingModifier = 0; }
}
