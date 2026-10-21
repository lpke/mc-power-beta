package local.luke.power.inventory.tools;

import java.lang.reflect.Method;
import local.luke.power.input.Bindings;
import net.minecraft.client.Minecraft;

/** The utility module has a separate mapping/build boundary; resolve its public methods once. */
final class HotbarRows {
  private static final Method ELIGIBLE, CONSUMES, CANCEL;
  private static boolean reported;
  static {
    Method eligible = null, consumes = null, cancel = null;
    try {
      Class<?> type = Class.forName("local.luke.power.building.hotbar.Hotbars");
      eligible = type.getMethod("eligible", Minecraft.class); consumes = type.getMethod("consumesKey", Minecraft.class, int.class);
      cancel = type.getMethod("cancelPending");
    } catch (ReflectiveOperationException unavailable) { report(unavailable); }
    ELIGIBLE = eligible; CONSUMES = consumes; CANCEL = cancel;
  }
  private HotbarRows() {}
  static boolean available() { return ELIGIBLE != null && CONSUMES != null && CANCEL != null; }
  static boolean owns(Minecraft mc, int key) {
    if (!available()) return true;
    try {
      if (!Boolean.TRUE.equals(ELIGIBLE.invoke(null, mc))) return false;
      return Boolean.TRUE.equals(CONSUMES.invoke(null, mc, key)) || Bindings.down("power_building.hotbar.scroll")
          || Bindings.down("power_building.hotbar.row1") || Bindings.down("power_building.hotbar.row2") || Bindings.down("power_building.hotbar.row3");
    } catch (ReflectiveOperationException unavailable) { report(unavailable); return true; }
  }
  static boolean cancel() {
    if (!available()) return false;
    try { CANCEL.invoke(null); return true; }
    catch (ReflectiveOperationException unavailable) { report(unavailable); return false; }
  }
  private static void report(Exception failure) {
    if (reported) return; reported = true;
    local.luke.power.PowerBeta.LOG.error("Saved Creative hotbars could not coordinate inventory row controls; original inventories and presets retained", failure);
  }
}
