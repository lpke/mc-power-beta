package local.luke.power.validation;
import net.minecraft.client.Minecraft;
final class CheatsChecks {
  static void run(Minecraft mc) throws Exception { WorldRulesChecks.permissions(mc); }
  static void creation(Minecraft mc) throws Exception { WorldSettingsChecks.run(mc, "create"); }
}
