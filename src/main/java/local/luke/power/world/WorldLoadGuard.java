package local.luke.power.world;

import local.luke.power.storage.PowerConfig;
import local.luke.power.ui.DataProblemScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

public final class WorldLoadGuard {
  private static String pending;
  private WorldLoadGuard() {}
  public static boolean allow(String folder) {
    if (!PowerConfig.problem().isEmpty()) { pending = PowerConfig.problem(); return false; }
    try { WorldRuleFiles.beforeOpen(FabricLoader.getInstance().getGameDir(), folder); return true; }
    catch (Exception e) { pending = "World " + folder + ": " + e.getMessage(); return false; }
  }
  public static boolean tick(Minecraft mc) {
    String configuration = PowerConfig.problem();
    if (!configuration.isEmpty()) {
      if (!(mc.currentScreen instanceof DataProblemScreen)) mc.setScreen(new DataProblemScreen(configuration, true));
      return true;
    }
    if (pending != null) { mc.setScreen(new DataProblemScreen(pending, false)); pending = null; }
    return false;
  }
}
