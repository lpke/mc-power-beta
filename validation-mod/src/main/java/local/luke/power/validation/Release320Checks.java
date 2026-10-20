package local.luke.power.validation;

import net.minecraft.client.Minecraft;

/** Physical input drivers dispatch through this entry point in disposable clients only. */
final class Release320Checks {
  static void run(Minecraft mc,String mode) throws Exception {
    switch(mode) {
      case "ui" -> Ui320Checks.run(mc);
      case "map-ui" -> Map320Checks.run(mc);
      default -> {
        if(mode.startsWith("chat-"))Chat320Checks.run(mc,mode);
        else if(mode.startsWith("radar-"))Radar320Checks.run(mc,mode.substring(6));
        else throw new IllegalArgumentException("Use release-320 ui|map-ui|chat-<mode>|radar-<mode>");
      }
    }
  }
}
