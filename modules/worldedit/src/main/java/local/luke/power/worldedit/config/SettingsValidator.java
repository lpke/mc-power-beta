package local.luke.power.worldedit.config;

public final class SettingsValidator {
  private SettingsValidator() {}

  public static void validate(Settings s) {
    if (s.wandItem < 1
        || s.wandItem > 32767
        || s.blockLimit < 1
        || s.blockLimit > 262144
        || s.blocksPerTick < 64
        || s.blocksPerTick > 8192
        || s.historySize < 1
        || s.historySize > 100
        || s.color < 0
        || s.color > 3
        || s.opacity < 10
        || s.opacity > 100
        || s.lineWidth < 1
        || s.lineWidth > 4) throw new IllegalArgumentException("Invalid WorldEdit Beta settings.");
  }
}
