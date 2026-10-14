package local.luke.power.ui;

import local.luke.power.config.Setting;

/** Contextual labels leave the stored catalogue and ordinary rows unchanged. */
public final class SettingLabels {
  private static final String CLUSTER = "infoHud.clusters.";
  private SettingLabels() {}

  public static String display(Setting setting, boolean searching) {
    if (searching && setting.id.startsWith(CLUSTER)) {
      int index = CLUSTER.length();
      if (setting.id.length() > index + 1 && setting.id.charAt(index + 1) == '.') {
        int cluster = setting.id.charAt(index) - '0';
        if (cluster >= 0 && cluster < 4) return setting.label + " (cluster " + (cluster + 1) + ")";
      }
    }
    return setting.label;
  }
}
